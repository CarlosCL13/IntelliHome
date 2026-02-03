from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from sqlalchemy.orm import Session

from Servicios.Notificacion_Servicio import Notificacion_Servicio
from Base_de_Datos.db_session import get_db


# Configuración del router para los endpoints de notificaciones de eventos
router = APIRouter(prefix="/notificaciones_eventos", tags=["notificaciones_eventos"])

# -----------------------------------------------------------------------------
# Endpoint: Notifiación de evento
# -----------------------------------------------------------------------------
@router.post("/notificacion-evento")
def notificar_evento(
    propiedad_id: int = Form(...),
    evento: str = Form(...),
    db: Session = Depends(get_db)
    ):
    
    resultado = Notificacion_Servicio.notificar_evento(
        db=db,
        propiedad_id=propiedad_id,
        evento=evento
    )
    
    if 'errores' in resultado:
        raise HTTPException(status_code=400, detail=resultado['errores'])
    
    return {"mensaje": "Notificación de evento enviada exitosamente."}