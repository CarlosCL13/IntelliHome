from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from sqlalchemy.orm import Session
from Base_de_Datos.db_session import get_db
from Modelos.NoDisponibilidadPropiedad import NoDisponibilidadPropiedad
from Servicios.NoDisponibilidadPropiedad_Servicio import NoDisponibilidadPropiedad_Servicio



# Configuración del router para los endpoints de propiedades
router = APIRouter(prefix="/no-disponibilidades-propiedades", tags=["noDisponibilidadesPropiedades"])

@router.post("/no-disponibilidad-propiedad")
def registrar_no_disponibilidad_propiedad(
        propiedad_id: int = Form(...), 
        fechas_noDisponible: str = Form(...),
        db: Session = Depends(get_db)
    ):
    """
    Registra una no disponibilidad de una propiedad en cierta fecha.
    """
    
    # Se colocan en una lista las fechas no disponibles de la propiedad
    fechas_noDisponibles_aux = []
    for fecha in fechas_noDisponible.split(","):
        fechas_noDisponibles_aux.append(fecha.strip())

    resultado = NoDisponibilidadPropiedad_Servicio.registrar_no_disponibilidad_propiedad(
        db=db,
        propiedad_id=propiedad_id,
        fechas_noDisponible=fechas_noDisponibles_aux
    )

    if 'errores' in resultado:
        raise HTTPException(status_code=422, detail=resultado['errores'])
    
    return resultado
