from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from flask import request
from sqlalchemy.orm import Session, joinedload
from Controladores.Propiedad_Controlador import get_local_ip
from Servicios.Arrendamiento_Servicio import Arrendamiento_Servicio
from Base_de_Datos.db_session import get_db
import os
from Modelos.Arrendamiento import Arrendamiento
from Modelos.Propiedad import Propiedad
from Modelos.FotoPropiedad import FotoPropiedad
import socket

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

@router.get("/alquiladas/{user_id}", summary="Obtener propiedades alquiladas por un usuario (inquilino)")
def get_propiedades_alquiladas(user_id: int, request: Request, db: Session = Depends(get_db)):
    arrendamientos = db.query(Arrendamiento).filter(Arrendamiento.inquilino_id == user_id).all()
    propiedades = []
    base_url = str(request.base_url).rstrip('/')
    if "localhost" in base_url or "127.0.0.1" in base_url:
        ip = get_local_ip()
        base_url = base_url.replace("localhost", ip).replace("127.0.0.1", ip)
    for arr in arrendamientos:
        prop = db.query(Propiedad).filter(Propiedad.id == arr.propiedad_id).first()
        if prop:
            # Obtener solo la primera foto
            foto = db.query(FotoPropiedad).filter(FotoPropiedad.propiedad_id == prop.id).first()
            foto_url = None
            if foto:
                nombre_archivo = os.path.basename(foto.url_foto)
                foto_url = f"{base_url}/uploads/{nombre_archivo}"
            propiedades.append({
                "id": prop.id,
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
    return propiedades


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
