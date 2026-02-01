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
from datetime import datetime

class Notificacion_Servicio:
    
    # Incializa la App de Firebase (Solo una vez)
    if not firebase_admin._apps:
        cred = credentials.Certificate("llaveServicio.json") 
        firebase_admin.initialize_app(cred)

    #================================= Lógica Endpoints ================================= #

    #Notificación de evento
    @staticmethod
    def notificar_evento(db: Session, propiedad_id: int, evento: str):
        """
        Envía una notificación de evento a los dispositivos.
        """
        errores = {}

        try:
            # Validar si la propiedad existe (prevención)
            propiedad_evento = Notificacion_Servicio._validar_propiedad(db, propiedad_id, errores)
            if errores:
                return {'errores': errores}
            
            # Buscar arrendamiento activo de la propiedad
            arrendamiento_activo = Notificacion_Servicio._validar_arrendamiento_activo(db, propiedad_id, errores)
            inquilino_id = None
            if arrendamiento_activo is None:
                inquilino_id = propiedad_evento.propietario_id # Se le notifica al propietario
                return
            else:
                inquilino_id = arrendamiento_activo.inquilino_id
            
            # Validar token de dispositivo del inquilino
            dispositivos_inquilino = Notificacion_Servicio._validar_usuario_dispositivo(db, inquilino_id, errores)
            if errores:
                return {'errores': errores}

            # Se extraen los tokens de cada dispositivo
            tokens = []
            for dispositivo in dispositivos_inquilino:
                tokens.append(dispositivo.fcm_token)

            # Enviar notificación según el evento
            if evento.lower() == "incendio":   
                Notificacion_Servicio.enviar_alerta_fuego(tokens, errores)
                titulo= "🔥 ¡ALERTA DE INCENDIO! 🔥"
                Notificacion_Servicio._enviar_notificacion_whatsapp(db,propiedad_id, titulo, errores)

            elif evento.lower() == "sismo":
                Notificacion_Servicio.enviar_alerta_sismo(tokens, errores)
                titulo= "🌎 ¡ALERTA DE SISMO! 🌎"
                Notificacion_Servicio._enviar_notificacion_whatsapp(db,propiedad_id, titulo, errores)

            else:
                errores['notificacion'] = 'Evento no reconocido.'
            
            if errores:
                return {'errores': errores}
            
            return {"mensaje": "Notificación enviada exitosamente."}

        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()

    #================================= VALIDACIONES ================================= #

    # Validar que la propiedad exista
    @staticmethod
    def _validar_propiedad(db: Session, propiedad_id: int, errores: dict):
        propiedad = db.query(Propiedad).filter(Propiedad.id == propiedad_id).first()
        if not propiedad:
            errores['propiedad'] = 'La propiedad no existe.'
            return None
        return propiedad
    
    # Validar si hay un arrendamiento activo de dicha propiedad
    @staticmethod
    def _validar_arrendamiento_activo(db: Session, propiedad_id: int, errores: dict):
        hoy = datetime.now().date()
        arrendamiento = db.query(Arrendamiento).filter(
            Arrendamiento.propiedad_id == propiedad_id,
            Arrendamiento.fecha_inicio <= hoy,
            Arrendamiento.fecha_fin >= hoy
        ).first()
        if not arrendamiento:
            # Si no hay arrendamiento se retorna None (se le notificará al propietario en este caso)
            return None
        return arrendamiento

    # Validar que el usuario (inquilino) tenga un token de dispositivo
    @staticmethod
    def _validar_usuario_dispositivo(db: Session, inquilino_id: int, errores):
        # Se obtienen los dispositivos vinculados al usuario
        dispositivos = db.query(UsuarioDispositivo).filter(UsuarioDispositivo.usuario_id == inquilino_id).all()
        if not dispositivos:
            errores['dispositivo'] = 'El usuario (inquilino) no tiene un dispositivo activo (sesión iniciada) para notificaciones.'
            return None
        return dispositivos

    #================================= UTILIDADES ================================= #
    
    # Notificación de alerta de fuego
    @staticmethod
    def enviar_alerta_fuego(tokens: list, errores: dict):
        """
        Envía una notificación (alta prioridad) de fuego a todos los dispositivos.
        """
        try:
            # Construcción del mensaje
            mensaje = messaging.MulticastMessage(
                notification=messaging.Notification(
                    title="🔥 ¡ALERTA DE INCENDIO! 🔥",
                    body="Se ha detectado humo en la casa. Revise inmediatamente.",
                ),
                tokens = tokens, # Lista de tokens
                android=messaging.AndroidConfig(
                    ttl=0, # Entrega inmediata (Time To Live 0)
                    priority='high', # Despierta al dispositivo
                    notification=messaging.AndroidNotification(
                        icon='stock_ticker_update', # Icono predeterminado
                        color='#f45342', # Rojo alerta
                        sound='default',
                        default_vibrate_timings=True,
                        channel_id='canal_emergencias_intelli' # Debe coincidir con Android
                    ),
                ),
            )

            # Enviar
            response = messaging.send_each_for_multicast(mensaje)
            print(f"Alerta enviada exitosamente: {response}")
            return True
        except Exception as e:
            print(f"Error enviando alerta: {e}")
            errores['notificacion'] = 'Error enviando alerta de fuego.'
            return False
        
    
    # Notificación de alerta de sismo
    @staticmethod
    def enviar_alerta_sismo(tokens: list, errores: dict):
        """
        Envía una notificación (alta prioridad) de sismo a todos los dispositivos.
        """
        try:
            # Construcción del mensaje
            mensaje = messaging.MulticastMessage(
                notification=messaging.Notification(
                    title="🌎 ¡ALERTA DE SISMO! 🌎",
                    body="Se ha detectado actividad sísmica. Tome precauciones.",
                ),
                tokens = tokens, # lista de tokens
                android=messaging.AndroidConfig(
                    ttl=0, # Entrega inmediata (Time To Live 0)
                    priority='high', # Despierta al dispositivo
                    notification=messaging.AndroidNotification(
                        icon='stock_ticker_update', # Icono predeterminado
                        color='#f4b842', # Naranja alerta
                        sound='default',
                        default_vibrate_timings=True,
                        channel_id='canal_emergencias_intelli' # Debe coincidir con Android
                    ),
                ),
            )

            # Enviar
            response = messaging.send_each_for_multicast(mensaje)
            print(f"Alerta enviada exitosamente: {response}")
            return True
        except Exception as e:
            print(f"Error enviando alerta: {e}")
            errores['notificacion'] = 'Error enviando alerta de sismo.'
            return False


     # Enviar mensaje de notificacion de evento via WhatsApp
    @staticmethod
    def _enviar_notificacion_whatsapp(db, propiedad_id, titulo, errores):
        """
        Envía un mensaje de notificación vía WhatsApp al número proporcionado.
        """
        try:
            # Credenciales de Twilio
            account_sid = TWILIO_ACCOUNT_SID
            auth_token = TWILIO_AUTH_TOKEN
            client = Client(account_sid, auth_token)

            # Titulo de la propiedad (asunto del mensaje)
            propiedad_arrendar = db.query(Propiedad).filter(Propiedad.id == propiedad_id).first()
            titulo_propiedad = propiedad_arrendar.titulo_publicacion

            # Número de teléfono del inquilino
            arrendamiento = db.query(Arrendamiento).filter(Arrendamiento.propiedad_id == propiedad_id).first()
            inquilino_id = arrendamiento.inquilino_id
            inquilino = db.query(Usuario).filter(Usuario.id == inquilino_id).first()
            numero_telefono = inquilino.telefono

            # Se formatean las fechas para el mensaje
            fecha_hoy = datetime.now().strftime('%d/%m/%Y')
            hora_hoy = datetime.now().strftime('%H:%M:%S')

            # Texto del mensaje a enviar con la información anterior
            mensaje_texto = (
                "IntelliHome 🏡\n\n"
                f"{titulo}.\n"
                f"Propiedad: {titulo_propiedad}\n"
                f"• Fecha: {fecha_hoy}\n"
                f"• Hora: {hora_hoy}\n\n"
                "!Comuniquese con las autoridades de emergencia en caso de ser necesario!\n"
            )
            
            # Se envia el mensaje
            mensaje = client.messages.create(
                body=mensaje_texto,
                from_=TWILIO_WHATSAPP_NUMBER_FROM,
                to=f'whatsapp:+506{numero_telefono}'
            )

            print("Numero de telefono:", numero_telefono)

            return True
        
        except Exception as e:
            db.rollback()
            errores['notificacion'] = f'No se pudo enviar la notificación WhatsApp: {str(e)}'
            return False
        finally:
            db.close()