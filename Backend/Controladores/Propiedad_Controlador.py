from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from sqlalchemy.orm import Session
from datetime import date
from typing import Optional
import os
import socket

# Importación de Servicios y Base de Datos
from Servicios.Propiedad_Servicio import Propiedad_Servicio
from Base_de_Datos.db_session import get_db

# Importación de Modelos
from Modelos.Propiedad import Propiedad
from Modelos.FotoPropiedad import FotoPropiedad
from Modelos.Amenidad import Amenidad, PropiedadAmenidad
from Modelos.TipoCasa import TipoCasa
from Modelos.Usuario import Usuario
from Modelos.Hobby import Hobby, PropiedadHobby
from Modelos.Arrendamiento import Arrendamiento
from Modelos.NoDisponibilidadPropiedad import NoDisponibilidadPropiedad

# Configuración del router para los endpoints de propiedades
router = APIRouter(prefix="/propiedades", tags=["propiedades"])


# -----------------------------------------------------------------------------
# Helper: Obtener IP Local (Definida aquí mismo, por eso no se importa)
# -----------------------------------------------------------------------------
def get_local_ip():
    """
    Intenta obtener la IP de la red local para sustituir localhost.
    Útil para pruebas desde dispositivos físicos (celulares) para que puedan ver las imágenes.
    """
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Se intenta conectar a una IP externa (no envía datos reales)
        s.connect(('10.255.255.255', 1))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip


# -----------------------------------------------------------------------------
# Helpers para Arrendamientos (Lógica interna)
# -----------------------------------------------------------------------------
def obtener_inquilino_actual(propiedad_id: int, db: Session) -> Optional[Usuario]:
    """
    Determina si la propiedad está ocupada hoy y devuelve al inquilino.
    """
    hoy = date.today()
    # Se obtiene el arrendamiento actual
    arrendamiento_actual = db.query(Arrendamiento).filter(
        Arrendamiento.propiedad_id == propiedad_id,
        Arrendamiento.fecha_inicio <= hoy,
        Arrendamiento.fecha_fin >= hoy
    ).first()

    if arrendamiento_actual:
        # Se obtiene el usuario inquilino asociado a dicho arrendamiento
        inquilino = db.query(Usuario).filter(Usuario.id == arrendamiento_actual.inquilino_id).first()
        return inquilino
    return None


def obtener_arrendamientos_futuros(propiedad_id: int, db: Session):
    """
    Obtiene la lista de fechas reservadas a futuro para bloquear el calendario.
    """
    arrendamientos = db.query(Arrendamiento).filter(
        Arrendamiento.propiedad_id == propiedad_id
    ).all()

    futuros_arrendamientos = []
    hoy = date.today()
    for arrendamientos_aux in arrendamientos:
        # Se omiten los arrendamientos que ya hayan finalizado hasta hoy
        # Se incluyen los que están activos o son futuros
        if arrendamientos_aux.fecha_fin >= hoy:
            futuros_arrendamientos.append({
                "arrendamiento_id": arrendamientos_aux.id,
                "fecha_inicio": arrendamientos_aux.fecha_inicio,
                "fecha_fin": arrendamientos_aux.fecha_fin
            })
    
    return futuros_arrendamientos


# -----------------------------------------------------------------------------
# Endpoint: Registro de Propiedad
# -----------------------------------------------------------------------------
@router.post("/registro-propiedad")
def registrar_propiedad(
    usuario_id: int = Form(...),
    tipo_casa_id: int = Form(...),
    hobbies_ids: str = Form(""),
    amenidades_ids: str = Form(""),
    dias_disponibles: str = Form(...),
    latitud: float = Form(...),
    longitud: float = Form(...),
    titulo_publicacion: str = Form(...),
    descripcion_publicacion: str = Form(...),
    precio_noche: float = Form(...),
    huespedes: int = Form(...),
    habitaciones: int = Form(...),
    camas: int = Form(...),
    banos: int = Form(...),
    reglas_uso: str = Form(None),
    estado: str = Form('disponible'),
    fotos_propiedad: list[UploadFile] = File(...),
    db: Session = Depends(get_db)
    ):
    """
    Registra una nueva propiedad en el sistema.
    Recibe datos del formulario (Multipart) y archivos de imagen.
    """
    # Conversión de strings CSV a listas de enteros
    hobbies_ids_list = [int(i) for i in hobbies_ids.split(",")] if hobbies_ids else []
    amenidades_ids_list = [int(i) for i in amenidades_ids.split(",")] if amenidades_ids else []
    dias_list = [d.strip() for d in dias_disponibles.split(",")] if dias_disponibles else []
    
    # Delegación de la lógica de negocio al servicio
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
        hobbies_ids=hobbies_ids_list,
        amenidades_ids=amenidades_ids_list,
        dias_disponibles=dias_list,
        reglas_uso=reglas_uso,
        estado=estado
    )

    if 'errores' in resultado:
        raise HTTPException(status_code=422, detail=resultado['errores'])
    
    return resultado


# -----------------------------------------------------------------------------
# Endpoint: Obtener Todas las Propiedades (Resumen)
# -----------------------------------------------------------------------------
@router.get("/todas", summary="Obtener todas las propiedades con detalles resumidos y una imagen")
def get_todas_propiedades(request: Request, db: Session = Depends(get_db)):
    """
    Devuelve un listado de todas las propiedades registradas.
    Incluye latitud y longitud para mostrar en mapas, y la primera foto como portada.
    """
    propiedades = db.query(Propiedad).all()
    resultado = []
    
    # Lógica para determinar la URL base (IP local vs Localhost)
    base_url = str(request.base_url).rstrip('/')
    if "localhost" in base_url or "127.0.0.1" in base_url:
        ip = get_local_ip()
        base_url = base_url.replace("localhost", ip).replace("127.0.0.1", ip)

    
    for prop in propiedades:
        # Recuperar solo la primera foto para la vista de tarjeta/resumen
        foto = db.query(FotoPropiedad).filter(FotoPropiedad.propiedad_id == prop.id).first()
        foto_url = None
        if foto:
            nombre_archivo = os.path.basename(foto.url_foto)
            foto_url = f"{base_url}/uploads/{nombre_archivo}"

        resultado.append({
            "id": prop.id,
            "usuario_id": prop.usuario_id, # IMPORTANTE: ID del dueño real
            "titulo_publicacion": prop.titulo_publicacion,
            "precio_noche": prop.precio_noche,
            "huespedes": prop.huespedes,
            "habitaciones": prop.habitaciones,
            "camas": prop.camas,
            "banos": prop.banos,
            "imagen": foto_url,
            "latitud": prop.latitud,
            "longitud": prop.longitud
        })
    
    return resultado


# -----------------------------------------------------------------------------
# Endpoint: Obtener Detalle de Propiedad por ID
# -----------------------------------------------------------------------------
@router.get("/{propiedad_id}", summary="Obtener una propiedad por id con fotos y amenidades")
def get_propiedad_por_id(propiedad_id: int, request: Request, db: Session = Depends(get_db)):
    """
    Devuelve el detalle completo de una propiedad específica por su ID.
    Incluye fotos, amenidades, hobbies, y datos del propietario.
    """
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

    # Imagen de perfil del usuario (Host)
    imagen_perfil_url = None
    if usuario and getattr(usuario, 'imagen_perfil', None):
        img_str = usuario.imagen_perfil
        if img_str.startswith("http"):
            imagen_perfil_url = img_str
        else:
            nombre_archivo = os.path.basename(img_str)
            imagen_perfil_url = f"{base_url}/uploads/{nombre_archivo}"

    # Se obtiene el Inquilino actual de la propiedad (hoy) para la lógica de IoT
    inquilino_actual = obtener_inquilino_actual(propiedad_id, db)
    inquilino_actual_id = None
    if inquilino_actual:
        inquilino_actual_id = inquilino_actual.id

    # Se obtiene los arrendamientos futuros de la propiedad para bloquear el calendario
    futuros_arrendamientos = obtener_arrendamientos_futuros(propiedad_id, db)
    
    # --- Procesar días disponibles ---
    dias_disponibles_list = []
    if prop.dias_disponibles:
        # Convertimos "Lunes,Martes" a ["Lunes", "Martes"]
        dias_disponibles_list = prop.dias_disponibles.split(",")
    else:
        # Si es nulo (propiedades viejas), asumimos todos los días
        dias_disponibles_list = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"]

    resultado = {
        "id": prop.id,
        "usuario_id": prop.usuario_id,
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
        "reglas_uso": prop.reglas_uso,
        "estado": prop.estado,
        "fotos": fotos_urls,
        "amenidades": amenidades_list,
        "hobbies": hobbies_list,
        "dias_disponibles": dias_disponibles_list,
        "inquilino_actual_id": inquilino_actual_id,
        "futuros_arrendamientos": futuros_arrendamientos
    }
    return resultado


# -----------------------------------------------------------------------------
# Endpoint: Obtener Propiedades por Usuario (Resumen - Mis Publicaciones)
# -----------------------------------------------------------------------------
@router.get("/usuario/{user_id}", summary="Obtener todas las propiedades publicadas por un usuario")
def get_propiedades_por_usuario(user_id: int, request: Request, db: Session = Depends(get_db)):
    """
    Devuelve las propiedades pertenecientes a un usuario específico (Dueño).
    Formato resumido (tarjetas).
    """
    propiedades = db.query(Propiedad).filter(Propiedad.usuario_id == user_id).all()
    resultado = []
    
    # Ajuste de URL base
    base_url = str(request.base_url).rstrip('/')
    if "localhost" in base_url or "127.0.0.1" in base_url:
        ip = get_local_ip()
        base_url = base_url.replace("localhost", ip).replace("127.0.0.1", ip)

    for prop in propiedades:
        foto = db.query(FotoPropiedad).filter(FotoPropiedad.propiedad_id == prop.id).first()
        foto_url = None
        if foto:
            nombre_archivo = os.path.basename(foto.url_foto)
            foto_url = f"{base_url}/uploads/{nombre_archivo}"
        
        resultado.append({
            "id": prop.id,
            "usuario_id": prop.usuario_id,
            "titulo_publicacion": prop.titulo_publicacion,
            "precio_noche": prop.precio_noche,
            "huespedes": prop.huespedes,
            "habitaciones": prop.habitaciones,
            "camas": prop.camas,
            "banos": prop.banos,
            "imagen": foto_url,
            "latitud": prop.latitud,
            "longitud": prop.longitud
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

# Función para obtener el inquilino actual (hoy) de una propiedad
def obtener_inquilino_actual(propiedad_id: int, db: Session) -> Optional[Usuario]:
    """
    Determina si la propiedad está ocupada hoy y devuelve al inquilino.
    """
    hoy = date.today()
    # Se obtiene el arrendamiento actual
    arrendamiento_actual = db.query(Arrendamiento).filter(
        Arrendamiento.propiedad_id == propiedad_id,
        Arrendamiento.fecha_inicio <= hoy,
        Arrendamiento.fecha_fin >= hoy
    ).first()

    if arrendamiento_actual:
        # Se obtiene el usuario inquilino asociado a dicho arrendamiento
        inquilino = db.query(Usuario).filter(Usuario.id == arrendamiento_actual.inquilino_id).first()
        return inquilino
    return None

# Función para obtener los arrendamientos futuros de una propiedad
def obtener_arrendamientos_futuros(propiedad_id: int, db: Session):
    """
    Obtiene la lista de fechas reservadas a futuro para bloquear el calendario.
    """
    arrendamientos = db.query(Arrendamiento).filter(
        Arrendamiento.propiedad_id == propiedad_id
    ).all()

    futuros_arrendamientos = []
    hoy = date.today()
    for arrendamientos_aux in arrendamientos:
        # Se omiten los arredamientos que ya hayan finalizado hasta hoy
        if arrendamientos_aux.fecha_fin >= hoy:
            futuros_arrendamientos.append({
                "arrendamiento_id": arrendamientos_aux.id,
                "fecha_inicio": arrendamientos_aux.fecha_inicio,
                "fecha_fin": arrendamientos_aux.fecha_fin
            })
    
    return futuros_arrendamientos
