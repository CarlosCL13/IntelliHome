from passlib.context import CryptContext
from twilio.rest import Client
from credenciales_Twilio import TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_WHATSAPP_NUMBER_FROM
from sqlalchemy.orm import Session
from Modelos.Arrendamiento import Arrendamiento
from Modelos.Propiedad import Propiedad
from Modelos.Usuario import Usuario
from datetime import datetime


class Arrendamiento_Servicio:

    #================================= Lógica Endpoints ================================= #

    # Registro de un arrendamiento o alquiler de propiedad
    @staticmethod
    def registrar_arrendamiento(
            db: Session,
            propiedad_id: int,
            inquilino_id: int,
            fecha_inicio: str,
            fecha_fin: str
        ):
        errores = {}
        nuevo_arrendamiento = None

        try:
            #validaciones
            fecha_inicio_aux, fecha_fin_aux = Arrendamiento_Servicio._validar_fechas(fecha_inicio, fecha_fin, errores)
            
            if errores:
                return {'errores': errores}

            #Se registra el arrendamiento
            nuevo_arrendamiento = Arrendamiento(
                propiedad_id=propiedad_id,
                inquilino_id=inquilino_id,
                fecha_inicio=fecha_inicio_aux,
                fecha_fin=fecha_fin_aux
            )
            db.add(nuevo_arrendamiento)
            db.commit()

            # Enviar notificación vía WhatsApp al inquilino
            Arrendamiento_Servicio._enviar_notificacion_whatsapp(
                db,
                propiedad_id,
                inquilino_id,
                nuevo_arrendamiento.id,
                errores
            )

            if errores:
                return {'errores': errores}
            
            return {"mensaje": "Arrendamiento registrado exitosamente."}

        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()

    #================================= VALIDACIONES ================================= #

    # Validación de fechas
    @staticmethod
    def _validar_fechas(fecha_inicio, fecha_fin, errores):
        """
        Verifica que las fechas de inicio y fin tengan el formato correcto YYYY-MM-DD.
        """
        fecha_inicio_aux = None
        fecha_fin_aux = None
        if fecha_inicio and fecha_fin:
            try:
                fecha_inicio_aux = datetime.strptime(fecha_inicio, '%Y-%m-%d').date()
                fecha_fin_aux = datetime.strptime(fecha_fin, '%Y-%m-%d').date()

                # Se comprueba que la fecha de fin sea posterior a la de inicio
                if fecha_fin_aux <= fecha_inicio_aux:
                    errores['fechas'] = 'La fecha de fin debe ser posterior a la fecha de inicio.'

                return fecha_inicio_aux, fecha_fin_aux
            except Exception:
                errores['fechas'] = 'El formato de las fechas debe ser YYYY-MM-DD.'
        else:
            errores['fechas'] = 'Las fechas de inicio y fin son obligatorias.'
            return None, None

    #================================= UTILIDADES ================================= #

    # Enviar mensaje de notificación vía  Whatsapp
    @staticmethod
    def _enviar_notificacion_whatsapp(db, propiedad_id, inquilino_id, arrendamiento_id, errores):
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

            # Fecha de arrendamiento de la propiedad
            arrendamiento = db.query(Arrendamiento).filter(Arrendamiento.id == arrendamiento_id).first()
            fecha_inicio = arrendamiento.fecha_inicio
            fecha_fin = arrendamiento.fecha_fin

            # Precio del arrendamiento
            precio_noche = propiedad_arrendar.precio_noche
            noches_arrendamiento = (fecha_fin - fecha_inicio).days  # Se obtiene las NOCHES totales del arrendamiento
            total_precio = precio_noche * noches_arrendamiento

            # Número de teléfono del inquilino
            inquilino = db.query(Usuario).filter(Usuario.id == inquilino_id).first()
            numero_telefono = inquilino.telefono

            # Nombre del inquilino
            nombre_inquilino = inquilino.nombre + ' ' + inquilino.apellidos

            # Se formatean las fechas para el mensaje
            fecha_inicio_str = fecha_inicio.strftime('%d/%m/%Y')
            fecha_fin_str = fecha_fin.strftime('%d/%m/%Y')

            # Texto del mensaje a enviar con la información anterior
            mensaje_texto = (
                "IntelliHome 🏡\n\n"
                f"¡Hola {nombre_inquilino}!\n"
                f"Confirmamos que tu reserva de la propiedad \"{titulo_propiedad}\" ha sido registrada exitosamente.\n\n"
                f"Detalles de tu reserva:\n"
                f"• Fechas: {fecha_inicio_str} al {fecha_fin_str} ({noches_arrendamiento} noches)\n"
                f"• Monto total: ₡{total_precio:.2f}\n\n"
                "¡Gracias por confiar en nosotros! Te deseamos una excelente estadía.\n"
            )
            
            # Se envia el mensaje
            mensaje = client.messages.create(
                body=mensaje_texto,
                from_=TWILIO_WHATSAPP_NUMBER_FROM,
                to=f'whatsapp:+506{numero_telefono}'
            )
            return True
        
        except Exception as e:
            db.rollback()
            errores['notificacion'] = f'No se pudo enviar la notificación WhatsApp: {str(e)}'
            return False
        finally:
            db.close()

        

