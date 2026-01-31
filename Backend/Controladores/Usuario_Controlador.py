from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from sqlalchemy.orm import Session
from Servicios.Usuario_Servicio import Usuario_Servicio
from typing import Optional
from Base_de_Datos.db import get_db
from Modelos.Usuario import Usuario
from Modelos.Hobby import Hobby, UsuarioHobby
from Modelos.TipoCasa import TipoCasa
from Modelos.UsuarioTipoCasa import UsuarioTipoCasa
from pydantic import BaseModel
import os
import socket
from Modelos.UsuarioDispositivo import UsuarioDispositivo 


router = APIRouter(prefix="/usuarios", tags=["usuarios"])


class DispositivoToken(BaseModel):
    fcm_token: str

# DTO simple para recibir el token
class TokenUpdateDto(BaseModel):
    fcm_token: str

@router.put("/usuarios/{usuario_id}/dispositivos")
def registrar_dispositivo(
    usuario_id: int, 
    datos: DispositivoToken, 
    db: Session = Depends(get_db)
):
    # 1. Verificar si el token ya existe (para no duplicarlo)
    dispositivo_existente = db.query(UsuarioDispositivo).filter(
        UsuarioDispositivo.fcm_token == datos.fcm_token
    ).first()

    if dispositivo_existente:
        # Si ya existe, solo actualizamos el dueño (por si cambió de usuario)
        dispositivo_existente.usuario_id = usuario_id
        db.commit()
        return {"mensaje": "Dispositivo actualizado correctamente"}

    # 2. Si es nuevo, lo creamos
    nuevo_dispositivo = UsuarioDispositivo(
        usuario_id=usuario_id,
        fcm_token=datos.fcm_token,
        tipo_dispositivo="android_postman" # Valor por defecto
    )
    
    db.add(nuevo_dispositivo)
    db.commit()
    db.refresh(nuevo_dispositivo)
    
    return {"mensaje": "Nuevo dispositivo agregado exitosamente"}

@router.delete("/usuarios/{usuario_id}/dispositivos/{token}")
def eliminar_dispositivo(usuario_id: int, token: str, db: Session = Depends(get_db)):
    """
    Endpoint para el LOGOUT. Deja de enviar notificaciones a este celular específico.
    """
    db.query(UsuarioDispositivo).filter(
        UsuarioDispositivo.usuario_id == usuario_id,
        UsuarioDispositivo.fcm_token == token
    ).delete()
    db.commit()
    return {"mensaje": "Dispositivo desvinculado"}


@router.put("/usuarios/{usuario_id}/fcm-token")
def actualizar_token_fcm(usuario_id: int, token_data: TokenUpdateDto, db: Session = Depends(get_db)):
    """
    Registra el token del dispositivo actual para el usuario logueado.
    IMPLEMENTA SEGURIDAD: Desvincula este mismo token de cualquier otro usuario previo.
    """
    
    # --- PASO 1: LIMPIEZA DE SEGURIDAD ---
    # Buscamos si este celular (token) estaba asignado a otra persona (ej. hermano, anterior dueño)
    usuarios_anteriores = db.query(Usuario).filter(
        Usuario.fcm_token == token_data.fcm_token,
        Usuario.id != usuario_id # Que no sea yo mismo
    ).all()

    if usuarios_anteriores:
        for u_viejo in usuarios_anteriores:
            u_viejo.fcm_token = None # Le quitamos el token
            print(f"[SEGURIDAD] Token desvinculado del usuario ID {u_viejo.id} por conflicto de dispositivo.")

    # --- PASO 2: ASIGNACIÓN ---
    usuario_actual = db.query(Usuario).filter(Usuario.id == usuario_id).first()
    
    if not usuario_actual:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    usuario_actual.fcm_token = token_data.fcm_token
    db.commit()
    
    return {"mensaje": "Dispositivo registrado exitosamente para notificaciones"}



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
    prefs_rel = db.query(UsuarioTipoCasa).filter(UsuarioTipoCasa.usuario_id == user_id).all()
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
