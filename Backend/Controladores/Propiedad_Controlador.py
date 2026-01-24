from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException
from sqlalchemy.orm import Session
from Servicios.Propiedad_Servicio import Propiedad_Servicio
from typing import Optional
from Base_de_Datos.db_session import get_db

router = APIRouter(prefix="/propiedades", tags=["propiedades"])

# Endpoint para el registro de una nueva propiedad
@router.post("/registro-propiedad")
def registrar_propiedad(
    usuario_id: int = Form(...),
    tipo_casa_id: int = Form(...),
    hobbies_ids: str = Form(...),
    latitud: float = Form(...),
    longitud: float = Form(...),
    titulo_publicacion: str = Form(...),
    descripcion_publicacion: str = Form(...),
    precio_noche: float = Form(...),
    huespedes: int = Form(...),
    habitaciones: int = Form(...),
    camas: int = Form(...),
    banos: int = Form(...),
    cocina: bool = Form(...),
    reglas_uso: str = Form(None),
    vehiculos: Optional[int] = Form(None),
    estado: str = Form('disponible'),
    fotos_propiedad: list[UploadFile] = File(...),
    db: Session = Depends(get_db)
    ):
    hobbies_ids_list = [int(i) for i in hobbies_ids.split(",")] if hobbies_ids else []
    resultado = Propiedad_Servicio.registrar_propiedad(
        db=db,
        usuario_id=usuario_id,
        tipo_casa_id=tipo_casa_id,
        latitud=latitud,
        longitud=longitud,
        titulo_publicacion=titulo_publicacion,
        descripcion_publicacion=descripcion_publicacion,
        fotos_propiedad=fotos_propiedad,
        precio_noche=precio_noche,
        huespedes=huespedes,
        habitaciones=habitaciones,
        camas=camas,
        banos=banos,
        cocina=cocina,
        hobbies_ids=hobbies_ids_list,
        reglas_uso=reglas_uso,
        vehiculos=vehiculos,
        estado=estado
    )
    if 'errores' in resultado:
        raise HTTPException(status_code=422, detail=resultado['errores'])
    return resultado