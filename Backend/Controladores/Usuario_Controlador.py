from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException
from sqlalchemy.orm import Session
from Servicios.Usuario_Servicio import Usuario_Servicio
from typing import Optional
from Base_de_Datos.db_session import get_db

router = APIRouter(prefix="/usuarios", tags=["usuarios"])

@router.post("/registro")
def registrar_usuario(
    nombre: str = Form(...),
    apellidos: str = Form(...),
    username: str = Form(...),
    correo: str = Form(...),
    telefono: str = Form(...),
    fecha_nacimiento: str = Form(...),
    domicilio: str = Form(...),
    contraseña: str = Form(...),
    imagen_perfil: UploadFile = File(...),
    hobbies_ids: str = Form(...),  # Recibe una cadena separada por comas
    tipos_casa_ids: str = Form(...),  # Recibe una cadena separada por comas
    pregunta_recuperacion_id: int = Form(...),
    respuesta_recuperacion: str = Form(...),
    permitir_huella: int = Form(...),
    nombre_titular: str = Form(...),
    numero_tarjeta: str = Form(...),
    fecha_expiracion: str = Form(...),
    db: Session = Depends(get_db)
):
    # Convertir cadenas separadas por comas a listas de enteros
    hobbies_ids_list = [int(i) for i in hobbies_ids.split(",")] if hobbies_ids else []
    tipos_casa_ids_list = [int(i) for i in tipos_casa_ids.split(",")] if tipos_casa_ids else []
    resultado = Usuario_Servicio.registrar_usuario(
        db=db,
        nombre=nombre,
        apellidos=apellidos,
        username=username,
        correo=correo,
        telefono=telefono,
        fecha_nacimiento=fecha_nacimiento,
        domicilio=domicilio,
        contraseña=contraseña,
        imagen_perfil=imagen_perfil,
        hobbies_ids=hobbies_ids_list,
        tipos_casa_ids=tipos_casa_ids_list,
        pregunta_recuperacion_id=pregunta_recuperacion_id,
        respuesta_recuperacion=respuesta_recuperacion,
        permitir_huella=permitir_huella,
        nombre_titular=nombre_titular,
        numero_tarjeta=numero_tarjeta,
        fecha_expiracion=fecha_expiracion
    )
    if 'errores' in resultado:
        raise HTTPException(status_code=422, detail=resultado['errores'])
    return resultado
