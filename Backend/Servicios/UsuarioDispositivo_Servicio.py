from sqlalchemy.orm import Session
from Modelos.UsuarioDispositivo import UsuarioDispositivo


class UsuarioDispositivo_Servicio:

    #================================= Lógica Endpoints ================================= #

    # Vincular Dispositivo al Usuario
    @staticmethod
    def vincular_usuario_dispositivo(
            db: Session,
            usuario_id: int,
            fcm_token: str
        ):
        """
        Vincula un dispositivo (token) al usuario actual que inicio sesión.
        """
        errores = {}

        try:
            # Validaciones
            UsuarioDispositivo_Servicio._validar_token_unico(db, fcm_token, errores)
            
            if errores:
                return {'errores': errores}

            # Vincular el dispositivo al usuario
            nuevo_dispositivo = UsuarioDispositivo(
                usuario_id=usuario_id,
                fcm_token=fcm_token
            )
            db.add(nuevo_dispositivo)
            db.commit()

            return {"mensaje": "Dispositivo vinculado exitosamente."}

        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()
    
    # Eliminar Dispositivo del Usuario
    @staticmethod
    def eliminar_usuario_dispositivo(
            db: Session,
            usuario_id: int,
            fcm_token: str
        ):
        """
        Elimina la vinculación de un dispositivo (token) al usuario actual.
        """
        errores = {}

        try:
            # Eliminar el dispositivo vinculado al usuario
            db.query(UsuarioDispositivo).filter(
                UsuarioDispositivo.usuario_id == usuario_id,
                UsuarioDispositivo.fcm_token == fcm_token
            ).delete()
            db.commit()

            return {"mensaje": "Dispositivo desvinculado exitosamente."}

        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()


    #================================= VALIDACIONES ================================= #

    # Validar si el token ya está vinculado a otro usuario
    @staticmethod
    def _validar_token_unico(
            db: Session,
            token: str,
            errores: dict
        ):
        dispositivo_existente = db.query(UsuarioDispositivo).filter(UsuarioDispositivo.fcm_token == token).first()
        if dispositivo_existente:
            errores['token'] = 'El token ya está vinculado a otro usuario.'



