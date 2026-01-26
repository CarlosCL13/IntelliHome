from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from sqlalchemy.orm import Session, joinedload
from Servicios.Arrendamiento_Servicio import Arrendamiento_Servicio
from Base_de_Datos.db_session import get_db
import os

router = APIRouter(prefix="/arrendamientos", tags=["arrendamientos"])

# Endpoint para el registro de un nuevo arrendamiento o alquiler de propiedad
@router.post("/arrendamiento")
def registrar_arrendamiento(
    propiedad_id: int = Form(...),
    inquilino_id: int = Form(...),
    fecha_inicio: str = Form(...),
    fecha_fin: str = Form(...),
    db: Session = Depends(get_db)
    ):

    # Se registra el arrendamiento usando el servicio
    resultado = Arrendamiento_Servicio.registrar_arrendamiento(
        db=db,
        propiedad_id=propiedad_id,
        inquilino_id=inquilino_id,
        fecha_inicio=fecha_inicio,
        fecha_fin=fecha_fin
    )

    if 'errores' in resultado:
        raise HTTPException(status_code=400, detail=resultado["errores"])
    return resultado