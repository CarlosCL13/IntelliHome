from sqlalchemy.orm import Session
from Modelos.NoDisponibilidadPropiedad import NoDisponibilidadPropiedad
from datetime import datetime


class NoDisponibilidadPropiedad_Servicio:

    #================================= Lógica Endpoints ================================= #

    # Registro de una no disponibilidad de una propiedad
    @staticmethod
    def registrar_no_disponibilidad_propiedad(
            db: Session,
            propiedad_id: int,
            fechas_noDisponible: list
        ):
        """
        Registra fechas en la que una propiedad no tendra disponibilidad de arrendamiento.
        """
        errores = {}
        nueva_no_disponibilidad = None

        try:
            #validaciones
            fechas_noDisponibles_aux = NoDisponibilidadPropiedad_Servicio._validar_fechas(fechas_noDisponible, errores)
            
            if errores:
                return {'errores': errores}

            #Se registran las fechas en las que la propiedad no estará disponible
            for fecha in fechas_noDisponibles_aux:
                nueva_no_disponibilidad = NoDisponibilidadPropiedad(
                    propiedad_id=propiedad_id,
                    fecha_noDisponible=fecha
                )
                db.add(nueva_no_disponibilidad)
            db.commit()

            return {'mensaje': 'No disponibilidad de la propiedad registrada exitosamente.'}

        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}

        finally:
            db.close()


    #================================= Validaciones ================================= #

    # Validación de fechas
    @staticmethod
    def _validar_fechas(fechas, errores):
        """
        Verifica que la fecha tenga el formato YYYY-MM-DD.
        """

        fechas_lista = []
        
        if fechas:
            try:
                # Se comprueba el formato de las fechas
                for fechas_indice in fechas:
                    fecha_aux = datetime.strptime(fechas_indice, '%Y-%m-%d').date()

                    # Se comprueba que la fecha sea posterior a la fecha actual
                    if fecha_aux <= datetime.now().date():
                        errores['fechas'] = 'La fecha debe ser posterior a la fecha actual.'
                        return None

                    fechas_lista.append(fecha_aux)

                return fechas_lista
            except Exception:
                errores['fechas'] = 'El formato de las fechas debe ser YYYY-MM-DD.'
                return None
        else:
            errores['fechas'] = 'Se debe al menos ingresar una fecha.'
            return None
        