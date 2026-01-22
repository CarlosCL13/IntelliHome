
from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException
from sqlalchemy.orm import Session
from Servicios.Usuario_Servicio import Usuario_Servicio
from typing import Optional
from Base_de_Datos.db_session import get_db

router = APIRouter(prefix="/usuarios", tags=["usuarios"])

# Endpoint para el registro de un nuevo usuario

@router.post("/registro")
def registrar_usuario(
    nombre: str = Form(...),
    apellidos: str = Form(...),
    username: str = Form(...),
    correo: str = Form(...),
    telefono: str = Form(...),
    fecha_nacimiento: str = Form(...),
    domicilio: str = Form(...),
    contrasena: str = Form(...),
    imagen_perfil: UploadFile = File(...),
    hobbies_ids: str = Form(...),  # Recibe una cadena separada por comas
    tipos_casa_ids: str = Form(...),  # Recibe una cadena separada por comas
    pregunta_recuperacion_id: int = Form(...),
    respuesta_recuperacion: str = Form(...),
    permitir_huella: int = Form(...),
    nombre_titular: str = Form(...),
    numero_tarjeta: str = Form(...),
    fecha_expiracion: str = Form(...),
    token_publico: Optional[str] = Form(None),
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
        contrasena=contrasena,
        imagen_perfil=imagen_perfil,
        hobbies_ids=hobbies_ids_list,
        tipos_casa_ids=tipos_casa_ids_list,
        pregunta_recuperacion_id=pregunta_recuperacion_id,
        respuesta_recuperacion=respuesta_recuperacion,
        permitir_huella=permitir_huella,
        nombre_titular=nombre_titular,
        numero_tarjeta=numero_tarjeta,
        fecha_expiracion=fecha_expiracion,
        token_publico=token_publico
    )
    if 'errores' in resultado:
        raise HTTPException(status_code=422, detail=resultado['errores'])
    return resultado

@router.post("/login")
def login_usuario(
    identificador: str = Form(...),
    contrasena: str = Form(...),
    db: Session = Depends(get_db)
    ):
    resultado = Usuario_Servicio.login_usuario(db=db, identificador=identificador, contrasena=contrasena)
    if "errores" in resultado:
        raise HTTPException(status_code=401, detail=resultado["errores"])
    return resultado

@router.post("/recuperar-contrasena")
def recuperar_contrasena(
    identificador: str = Form(...),
    db: Session = Depends(get_db)
    ):
    # respuesta con la pregunta de recuperación
    resultado = Usuario_Servicio.obtener_pregunta_recuperacion(db=db, identificador=identificador)

    if "errores" in resultado:
        raise HTTPException(status_code=400, detail=resultado["errores"])
    return resultado

@router.post("/restablecer-contrasena")
def restablecer_contrasena(
    identificador: str = Form(...),
    nueva_contrasena: str = Form(...),
    respuesta_recuperacion: str = Form(...),
    db: Session = Depends(get_db)
    ):
    resultado = Usuario_Servicio.restablecer_contrasena(
        db=db,
        identificador=identificador,
        nueva_contrasena=nueva_contrasena,
        respuesta_recuperacion=respuesta_recuperacion
    )
    if "errores" in resultado:
        raise HTTPException(status_code=400, detail=resultado["errores"])
    return resultado
    
# Endpoint para buscar usuario por token público
@router.post("/buscar-por-token")
def buscar_usuario_por_token(token_publico: str = Form(...), db: Session = Depends(get_db)):
    resultado = Usuario_Servicio.buscar_por_token_publico(db=db, token_publico=token_publico)
    if 'errores' in resultado:
        raise HTTPException(status_code=404, detail=resultado['errores'])
    return resultado
