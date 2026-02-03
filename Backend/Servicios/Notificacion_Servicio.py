import firebase_admin
from firebase_admin import credentials, messaging
from sqlalchemy.orm import Session
from Modelos.Propiedad import Propiedad
from datetime import datetime
from Modelos.Arrendamiento import Arrendamiento
from Modelos.UsuarioDispositivo import UsuarioDispositivo
from credenciales_Twilio import TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_NUMBER_FROM
from twilio.rest import Client
from Modelos.Usuario import Usuario

class Notificacion_Servicio:
    
    if not firebase_admin._apps:
        cred = credentials.Certificate("llaveServicio.json") 
        firebase_admin.initialize_app(cred)

    # =========================================================================
    # LÓGICA PRINCIPAL
    # =========================================================================

    @staticmethod
    def notificar_evento(db: Session, propiedad_id: int, evento: str):
        errores = {}

        try:
            # 1. Validar propiedad
            propiedad_evento = Notificacion_Servicio._validar_propiedad(db, propiedad_id, errores)
            if errores: return {'errores': errores}
            
            # Guardamos el nombre para usarlo en el título
            nombre_propiedad = propiedad_evento.titulo_publicacion 
            
            # 2. Determinar destinatario
            arrendamiento_activo = Notificacion_Servicio._validar_arrendamiento_activo(db, propiedad_id, errores)
            
            destinatario_id = None
            telefono_destino = None

            if arrendamiento_activo is None:
                # Casa vacía -> Dueño
                destinatario_id = propiedad_evento.usuario_id
                dueno = db.query(Usuario).filter(Usuario.id == destinatario_id).first()
                if dueno: telefono_destino = dueno.telefono
            else:
                # Casa rentada -> Inquilino
                destinatario_id = arrendamiento_activo.inquilino_id
                inquilino = db.query(Usuario).filter(Usuario.id == destinatario_id).first()
                if inquilino: telefono_destino = inquilino.telefono
            
            # 3. Obtener Tokens
            dispositivos_destinatario = Notificacion_Servicio._validar_usuario_dispositivo(db, destinatario_id, errores)
            if errores: return {'errores': errores}

            tokens = [d.fcm_token for d in dispositivos_destinatario]

            # 4. Enviar Notificaciones
            titulo_alerta = ""
            
            if evento.lower() == "incendio":   
                titulo_alerta = f"🔥 {nombre_propiedad}: ¡INCENDIO! 🔥"
                Notificacion_Servicio.enviar_alerta_fuego(tokens, errores, nombre_propiedad)

            elif evento.lower() == "sismo":
                titulo_alerta = f"🌎 {nombre_propiedad}: ¡SISMO! 🌎"
                Notificacion_Servicio.enviar_alerta_sismo(tokens, errores, nombre_propiedad)

            else:
                errores['notificacion'] = 'Evento no reconocido.'
                return {'errores': errores}

            # 5. Enviar WhatsApp (También incluye el nombre)
            if telefono_destino:
                Notificacion_Servicio._enviar_notificacion_whatsapp(
                    titulo_propiedad=nombre_propiedad,
                    titulo_alerta=titulo_alerta,
                    numero_telefono=telefono_destino,
                    errores=errores
                )

            if errores: return {'errores': errores}
            
            return {"mensaje": "Notificación enviada exitosamente."}

        except Exception as e:
            db.rollback()
            print(f"[ERROR CRITICO] {str(e)}")
            return {'errores': {'internal': f'Error interno: {str(e)}'}}

    # =========================================================================
    # MÉTODOS DE VALIDACIÓN
    # =========================================================================

    @staticmethod
    def _validar_propiedad(db: Session, propiedad_id: int, errores: dict):
        propiedad = db.query(Propiedad).filter(Propiedad.id == propiedad_id).first()
        if not propiedad:
            errores['propiedad'] = 'La propiedad no existe.'
            return None
        return propiedad
    
    @staticmethod
    def _validar_arrendamiento_activo(db: Session, propiedad_id: int, errores: dict):
        hoy = datetime.now().date()
        return db.query(Arrendamiento).filter(
            Arrendamiento.propiedad_id == propiedad_id,
            Arrendamiento.fecha_inicio <= hoy,
            Arrendamiento.fecha_fin >= hoy
        ).first()

    @staticmethod
    def _validar_usuario_dispositivo(db: Session, usuario_id: int, errores):
        dispositivos = db.query(UsuarioDispositivo).filter(UsuarioDispositivo.usuario_id == usuario_id).all()
        if not dispositivos:
            errores['dispositivo'] = 'El usuario no tiene dispositivos registrados.'
            return None
        return dispositivos

    # =========================================================================
    # UTILIDADES DE ENVÍO
    # =========================================================================
    
    @staticmethod
    def enviar_alerta_fuego(tokens: list, errores: dict, nombre_propiedad: str):
        try:
            mensaje = messaging.MulticastMessage(
                data={
                    "tipo": "incendio",
                    "titulo": f"🔥 {nombre_propiedad}", 
                    "cuerpo": "¡ALERTA DE INCENDIO! Se ha detectado humo. Revise inmediatamente.",
                    "prioridad": "alta"
                },
                tokens=tokens,
                android=messaging.AndroidConfig(
                    ttl=0, 
                    priority='high'
                )
            )
            response = messaging.send_each_for_multicast(mensaje)
            print(f"[FIREBASE] Data Message (Fuego) enviado. Éxito: {response.success_count}")
            return True
        except Exception as e:
            print(f"[FIREBASE ERROR] {e}")
            errores['notificacion'] = 'Error enviando alerta de fuego.'
            return False
        
    @staticmethod
    def enviar_alerta_sismo(tokens: list, errores: dict, nombre_propiedad: str):
        try:
            mensaje = messaging.MulticastMessage(
                data={
                    "tipo": "sismo",
                    # AQUÍ INYECTAMOS EL NOMBRE DE LA PROPIEDAD
                    "titulo": f"🌎 {nombre_propiedad}",
                    "cuerpo": "¡ALERTA DE SISMO! Movimiento detectado. Tome precauciones.",
                    "prioridad": "alta"
                },
                tokens=tokens,
                android=messaging.AndroidConfig(
                    ttl=0,
                    priority='high'
                )
            )
            response = messaging.send_each_for_multicast(mensaje)
            print(f"[FIREBASE] Data Message (Sismo) enviado. Éxito: {response.success_count}")
            return True
        except Exception as e:
            print(f"[FIREBASE ERROR] {e}")
            errores['notificacion'] = 'Error enviando alerta de sismo.'
            return False

    @staticmethod
    def _enviar_notificacion_whatsapp(titulo_propiedad, titulo_alerta, numero_telefono, errores):
        try:
            client = Client(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)

            fecha_hoy = datetime.now().strftime('%d/%m/%Y')
            hora_hoy = datetime.now().strftime('%H:%M:%S')

            mensaje_texto = (
                f"IntelliHome 🏡\n\n"
                f"{titulo_alerta}\n" # Esto ya traerá el nombre gracias a la lógica de arriba
                f"• Fecha: {fecha_hoy}\n"
                f"• Hora: {hora_hoy}\n\n"
                f"¡Comuníquese con las autoridades de emergencia en caso de ser necesario!\n"
            )
            
            to_number = f'whatsapp:+506{numero_telefono}'
            
            client.messages.create(
                body=mensaje_texto,
                from_=TWILIO_WHATSAPP_NUMBER_FROM,
                to=to_number
            )
            print(f"[TWILIO] WhatsApp enviado a: {numero_telefono}")
            return True
        
        except Exception as e:
            print(f"[TWILIO ERROR] {e}")
            errores['whatsapp'] = f'No se pudo enviar WhatsApp: {str(e)}'
            return False
