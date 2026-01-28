from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from sqlalchemy.orm import Session
from Servicios.Usuario_Servicio import Usuario_Servicio
from typing import Optional
from Base_de_Datos.db_session import get_db
from Modelos.Usuario import Usuario
from Modelos.Hobby import Hobby, UsuarioHobby
from Modelos.TipoCasa import TipoCasa, UsuarioPreferencia
import os
import socket

router = APIRouter(prefix="/usuarios", tags=["usuarios"])

# -----------------------------------------------------------------------------
# Endpoint: Registro de Nuevo Usuario
# -----------------------------------------------------------------------------
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
    hobbies_ids: str = Form(...),  
    tipos_casa_ids: str = Form(...),  
    pregunta_recuperacion_id: int = Form(...),
    respuesta_recuperacion: str = Form(...),
    permitir_huella: int = Form(...),
    nombre_titular: str = Form(...),
    numero_tarjeta: str = Form(...),
    fecha_expiracion: str = Form(...),
    token_publico: Optional[str] = Form(None),
    db: Session = Depends(get_db)
    ):
    
    # Conversión de "1,2,3" a lista [1, 2, 3]
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

# -----------------------------------------------------------------------------
# Endpoint: Login
# -----------------------------------------------------------------------------
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

# -----------------------------------------------------------------------------
# Endpoint: Recuperación de Contraseña
# -----------------------------------------------------------------------------
@router.post("/recuperar-contrasena")
def recuperar_contrasena(
    identificador: str = Form(...),
    db: Session = Depends(get_db)
    ):
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
    
@router.post("/buscar-por-token")
def buscar_usuario_por_token(token_publico: str = Form(...), db: Session = Depends(get_db)):
    resultado = Usuario_Servicio.buscar_por_token_publico(db=db, token_publico=token_publico)
    if 'errores' in resultado:
        raise HTTPException(status_code=404, detail=resultado['errores'])
    return resultado

@router.get("/tarjeta/ultimos4/{user_id}", summary="Obtener los últimos 4 dígitos de la tarjeta de un usuario")
def get_ultimos4_tarjeta(user_id: int, db: Session = Depends(get_db)):
    usuario = db.query(Usuario).filter(Usuario.id == user_id).first()
    if not usuario:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    ultimos_4 = getattr(usuario, 'ultimos_4', None)
    return {"user_id": user_id, "ultimos_4": ultimos_4}

# -----------------------------------------------------------------------------
# NUEVO ENDPOINT: Obtener Perfil Completo (Para ProfileFragment)
# -----------------------------------------------------------------------------
@router.get("/perfil/{user_id}")
def get_perfil_usuario(user_id: int, request: Request, db: Session = Depends(get_db)):
    usuario = db.query(Usuario).filter(Usuario.id == user_id).first()
    if not usuario:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    # Obtener IDs de Hobbies
    hobbies_rel = db.query(UsuarioHobby).filter(UsuarioHobby.usuario_id == user_id).all()
    # Enviamos una lista de números: ej. [1, 3, 5]
    hobbies_ids = [h.hobby_id for h in hobbies_rel] 

    # Obtener IDs de Preferencias (TipoCasa)
    prefs_rel = db.query(UsuarioPreferencia).filter(UsuarioPreferencia.usuario_id == user_id).all()
    preferencias_ids = [p.tipo_casa_id for p in prefs_rel]
    
    imagen_url = None
    if usuario.imagen_perfil:
        nombre_archivo = os.path.basename(usuario.imagen_perfil)
        
        base_url = str(request.base_url).rstrip('/')
        if "localhost" in base_url or "127.0.0.1" in base_url:
            ip = get_local_ip() 
            base_url = base_url.replace("localhost", ip).replace("127.0.0.1", ip)
        imagen_url = f"{base_url}/uploads/{nombre_archivo}"

    return {
        "nombre_completo": f"{usuario.nombre} {usuario.apellidos}".strip(),
        "nombre_usuario": usuario.username,
        "correo": usuario.correo,
        "domicilio": usuario.domicilio,
        "hobbies_ids": hobbies_ids,
        "preferencias_ids": preferencias_ids,
        "imagen": imagen_url
    }
    
def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('10.255.255.255', 1))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip
