from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from sqlalchemy.orm import Session, joinedload
from Servicios.Propiedad_Servicio import Propiedad_Servicio
from Modelos.Propiedad import Propiedad
from Modelos.FotoPropiedad import FotoPropiedad
from Modelos.Amenidad import Amenidad, PropiedadAmenidad
from Modelos.TipoCasa import TipoCasa
from Modelos.Usuario import Usuario
from Modelos.Hobby import Hobby, PropiedadHobby
from typing import Optional
from Base_de_Datos.db_session import get_db
import os
import socket

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

# Endpoint para obtener todas las propiedades con detalles resumidos y una imagen
@router.get("/todas", summary="Obtener todas las propiedades con detalles resumidos y una imagen")
def get_todas_propiedades(request: Request, db: Session = Depends(get_db)):
    propiedades = db.query(Propiedad).all()
    resultado = []
    base_url = str(request.base_url).rstrip('/')
    # Reemplazar localhost o 127.0.0.1 por la IP local
    if "localhost" in base_url or "127.0.0.1" in base_url:
        ip = get_local_ip()
        base_url = base_url.replace("localhost", ip).replace("127.0.0.1", ip)
    for prop in propiedades:
        # Obtener solo la primera foto
        foto = db.query(FotoPropiedad).filter(FotoPropiedad.propiedad_id == prop.id).first()
        foto_url = None
        if foto:
            nombre_archivo = os.path.basename(foto.url_foto)
            foto_url = f"{base_url}/uploads/{nombre_archivo}"
        resultado.append({
            "id": prop.id,
            "titulo_publicacion": prop.titulo_publicacion,
            "precio_noche": prop.precio_noche,
            "huespedes": prop.huespedes,
            "habitaciones": prop.habitaciones,
            "camas": prop.camas,
            "banos": prop.banos,
            "imagen": foto_url
        })
    return resultado

# Endpoint para obtener una propiedad por id, incluyendo fotos (URL completa) y amenidades
@router.get("/{propiedad_id}", summary="Obtener una propiedad por id con fotos y amenidades")
def get_propiedad_por_id(propiedad_id: int, request: Request, db: Session = Depends(get_db)):
    prop = db.query(Propiedad).filter(Propiedad.id == propiedad_id).first()
    if not prop:
        raise HTTPException(status_code=404, detail="Propiedad no encontrada")
    base_url = str(request.base_url).rstrip('/')
    if "localhost" in base_url or "127.0.0.1" in base_url:
        ip = get_local_ip()
        base_url = base_url.replace("localhost", ip).replace("127.0.0.1", ip)
    # Fotos
    fotos = db.query(FotoPropiedad).filter(FotoPropiedad.propiedad_id == prop.id).all()
    fotos_urls = []
    for foto in fotos:
        nombre_archivo = os.path.basename(foto.url_foto)
        url = f"{base_url}/uploads/{nombre_archivo}"
        fotos_urls.append(url)
    # Amenidades
    amenidades_ids = db.query(PropiedadAmenidad.amenidad_id).filter(PropiedadAmenidad.propiedad_id == prop.id).all()
    amenidades = db.query(Amenidad).filter(Amenidad.id.in_([a[0] for a in amenidades_ids])).all()
    amenidades_list = [{"id": a.id, "nombre": a.nombre} for a in amenidades]
    # Hobbies
    hobbies_ids = db.query(PropiedadHobby.hobby_id).filter(PropiedadHobby.propiedad_id == prop.id).all()
    hobbies = db.query(Hobby).filter(Hobby.id.in_([h[0] for h in hobbies_ids])).all()
    hobbies_list = [{"id": h.id, "nombre": h.nombre} for h in hobbies]
    # Nombres relacionados
    tipo_casa = db.query(TipoCasa).filter(TipoCasa.id == prop.tipo_casa_id).first()
    usuario = db.query(Usuario).filter(Usuario.id == prop.usuario_id).first()
    # Imagen de perfil del usuario
    imagen_perfil_url = None
    if usuario and hasattr(usuario, 'imagen_perfil') and usuario.imagen_perfil:
        nombre_archivo = os.path.basename(usuario.imagen_perfil)
        base_url_img = base_url
        # Si la imagen está en la carpeta uploads, servirla por /uploads
        if 'uploads' in usuario.imagen_perfil:
            imagen_perfil_url = f"{base_url_img}/uploads/{nombre_archivo}"
        else:
            imagen_perfil_url = usuario.imagen_perfil
    resultado = {
        "id": prop.id,
        "usuario": usuario.nombre if usuario else None,
        "usuario_nombre_completo": f"{usuario.nombre} {usuario.apellidos}".strip() if usuario and hasattr(usuario, 'nombre') and hasattr(usuario, 'apellidos') else None,
        "usuario_telefono": usuario.telefono if usuario and hasattr(usuario, 'telefono') else None,
        "usuario_imagen_perfil": imagen_perfil_url,
        "tipo_casa": tipo_casa.nombre if tipo_casa else None,
        "latitud": prop.latitud,
        "longitud": prop.longitud,
        "titulo_publicacion": prop.titulo_publicacion,
        "descripcion_publicacion": prop.descripcion_publicacion,
        "precio_noche": prop.precio_noche,
        "huespedes": prop.huespedes,
        "habitaciones": prop.habitaciones,
        "camas": prop.camas,
        "banos": prop.banos,
        "cocina": prop.cocina,
        "reglas_uso": prop.reglas_uso,
        "vehiculos": prop.vehiculos,
        "estado": prop.estado,
        "fotos": fotos_urls,
        "amenidades": amenidades_list,
        "hobbies": hobbies_list
    }
    return resultado

# Endpoint para obtener todas las propiedades de un usuario por su user id, incluyendo fotos, amenidades y hobbies
@router.get("/usuario/{user_id}", summary="Obtener todas las propiedades de un usuario por su id (resumido)")
def get_propiedades_por_usuario(user_id: int, request: Request, db: Session = Depends(get_db)):
    propiedades = db.query(Propiedad).filter(Propiedad.usuario_id == user_id).all()
    resultado = []
    base_url = str(request.base_url).rstrip('/')
    if "localhost" in base_url or "127.0.0.1" in base_url:
        ip = get_local_ip()
        base_url = base_url.replace("localhost", ip).replace("127.0.0.1", ip)
    for prop in propiedades:
        # Obtener solo la primera foto
        foto = db.query(FotoPropiedad).filter(FotoPropiedad.propiedad_id == prop.id).first()
        foto_url = None
        if foto:
            nombre_archivo = os.path.basename(foto.url_foto)
            foto_url = f"{base_url}/uploads/{nombre_archivo}"
        resultado.append({
            "id": prop.id,
            "titulo_publicacion": prop.titulo_publicacion,
            "precio_noche": prop.precio_noche,
            "huespedes": prop.huespedes,
            "habitaciones": prop.habitaciones,
            "camas": prop.camas,
            "banos": prop.banos,
            "imagen": foto_url
        })
    return resultado

# Función para obtener la IP local de la máquina
def get_local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # No importa si no hay internet, solo queremos la IP local
        s.connect(('10.255.255.255', 1))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip