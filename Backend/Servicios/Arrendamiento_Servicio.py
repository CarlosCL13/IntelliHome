from passlib.context import CryptContext
import os
import re

from sqlalchemy.orm import Session
from Modelos.Arrendamiento import Arrendamiento
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

            #Se registra el arrendamiento
            nuevo_arrendamiento = Arrendamiento(
                propiedad_id=propiedad_id,
                inquilino_id=inquilino_id,
                fecha_inicio=fecha_inicio_aux,
                fecha_fin=fecha_fin_aux
            )
            db.add(nuevo_arrendamiento)
            db.commit()
            
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