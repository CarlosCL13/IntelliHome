from fastapi import APIRouter, Depends, Form, HTTPException, Request
from sqlalchemy.orm import Session
from sqlalchemy import desc
import os
import socket

# Importaciones del Proyecto
from Base_de_Datos.db_session import get_db
from Servicios.Arrendamiento_Servicio import Arrendamiento_Servicio
from Modelos.Arrendamiento import Arrendamiento
from Modelos.Propiedad import Propiedad
from Modelos.FotoPropiedad import FotoPropiedad

# Importamos la utilidad get_local_ip desde Propiedad_Controlador
# (Esto funciona bien ahora que eliminamos la importación circular en el otro archivo)
from Controladores.Propiedad_Controlador import get_local_ip

# Configuración del Router
router = APIRouter(prefix="/arrendamientos", tags=["arrendamientos"])

"""
Módulo: Arrendamiento_Controlador
Descripción: Gestiona los endpoints HTTP relacionados con el ciclo de vida de los alquileres.
Incluye registro de nuevos contratos y consulta de historial para inquilinos.
"""


@router.post("/arrendamiento")
def registrar_arrendamiento(
    propiedad_id: int = Form(...),
    inquilino_id: int = Form(...),
    fecha_inicio: str = Form(...),
    fecha_fin: str = Form(...),
    subtotal: float = Form(...),
    iva: float = Form(...),
    comision: float = Form(...),
    db: Session = Depends(get_db)
):
    """
    Registra un nuevo contrato de arrendamiento en el sistema.

    Delega la lógica de validación de fechas y disponibilidad al servicio de dominio.

    Args:
        propiedad_id (int): ID de la propiedad a rentar.
        inquilino_id (int): ID del usuario que realiza la renta.
        fecha_inicio (str): Fecha de inicio en formato YYYY-MM-DD.
        fecha_fin (str): Fecha de fin en formato YYYY-MM-DD.
        subtotal (float): Subtotal del arrendamiento.
        iva (float): IVA calculado.
        comision (float): Comisión calculada.
        db (Session): Sesión de base de datos inyectada.

    Returns:
        dict: Objeto JSON con el mensaje de éxito o detalles del arrendamiento creado.

    Raises:
        HTTPException (400): Si el servicio retorna errores de validación (ej. fechas ocupadas).
    """
    resultado = Arrendamiento_Servicio.registrar_arrendamiento(
        db=db,
        propiedad_id=propiedad_id,
        inquilino_id=inquilino_id,
        fecha_inicio=fecha_inicio,
        fecha_fin=fecha_fin,
        subtotal=subtotal,
        iva=iva,
        comision=comision
    )

    if 'errores' in resultado:
        raise HTTPException(status_code=400, detail=resultado["errores"])
    
    return resultado


@router.get("/alquiladas/{user_id}", summary="Obtener historial de rentas de un usuario")
def get_propiedades_alquiladas(user_id: int, request: Request, db: Session = Depends(get_db)):
    """
    Recupera el historial completo de propiedades alquiladas por un inquilino específico.

    El listado incluye rentas pasadas, presentes y futuras. Los resultados se ordenan
    cronológicamente de forma descendente (la renta más reciente o futura aparece primero).
    Incluye los campos de fecha_inicio y fecha_fin para visualización en Frontend.

    Args:
        user_id (int): ID del inquilino.
        request (Request): Objeto de solicitud para construir URLs absolutas de imágenes.
        db (Session): Sesión de base de datos.

    Returns:
        list[dict]: Lista de objetos que combinan información de la Propiedad y del Arrendamiento.
    """
    
    # Consulta a la tabla Arrendamiento filtrando por inquilino.
    # Se utiliza 'desc(Arrendamiento.fecha_inicio)' para que lo más nuevo salga primero.
    arrendamientos = db.query(Arrendamiento)\
        .filter(Arrendamiento.inquilino_id == user_id)\
        .order_by(desc(Arrendamiento.fecha_inicio))\
        .all()

    propiedades_rentadas = []
    
    # Configuración de URL base para recursos estáticos (imágenes)
    # Se ajusta para dispositivos físicos si la petición viene de localhost
    base_url = str(request.base_url).rstrip('/')
    if "localhost" in base_url or "127.0.0.1" in base_url:
        try:
            ip = get_local_ip()
            base_url = base_url.replace("localhost", ip).replace("127.0.0.1", ip)
        except Exception:
            pass # Si falla obtener IP, mantenemos localhost

    for arr in arrendamientos:
        # Recuperación de la entidad Propiedad asociada al contrato
        prop = db.query(Propiedad).filter(Propiedad.id == arr.propiedad_id).first()
        
        if prop:
            # Recuperación de la imagen de portada
            foto = db.query(FotoPropiedad).filter(FotoPropiedad.propiedad_id == prop.id).first()
            foto_url = None
            if foto:
                nombre_archivo = os.path.basename(foto.url_foto)
                foto_url = f"{base_url}/uploads/{nombre_archivo}"
            
            # Construcción del DTO de respuesta.
            # Se combinan datos estáticos de la propiedad con datos temporales del arrendamiento.
            propiedades_rentadas.append({
                # Metadatos de la Propiedad
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
                "longitud": prop.longitud,
                
                # Metadatos del Contrato (CRUCIALES PARA EL HISTORIAL)
                "arrendamiento_id": arr.id,
                "fecha_inicio": str(arr.fecha_inicio),
                "fecha_fin": str(arr.fecha_fin)
            })
            
    return propiedades_rentadas

@router.post("/cotizar", summary="Cotizar arrendamiento sin guardar")
def cotizar_arrendamiento(
    propiedad_id: int = Form(...),
    fecha_inicio: str = Form(...),
    fecha_fin: str = Form(...),
    db: Session = Depends(get_db)
):
    """
    Calcula el desglose de un arrendamiento (subtotal, IVA, comisión, total) sin guardar en BD.
    Usa la fecha de inicio como fecha de reserva para el cálculo de la comisión.
    """
    resultado = Arrendamiento_Servicio.calcular_cotizacion(
        db=db,
        propiedad_id=propiedad_id,
        fecha_inicio=fecha_inicio,
        fecha_fin=fecha_fin,
        fecha_reserva=fecha_inicio
    )
    if 'errores' in resultado:
        raise HTTPException(status_code=400, detail=resultado["errores"])
    return resultado

# Endpoint para obtener el desglose del último arrendamiento de un usuario en una propiedad
@router.get("/desglose/{propiedad_id}/{usuario_id}", summary="Obtener desglose del último arrendamiento de un usuario en una propiedad")
def get_desglose_ultimo_arrendamiento(propiedad_id: int, usuario_id: int, db: Session = Depends(get_db)):
    """
    Devuelve el desglose del último arrendamiento hecho por el usuario a la propiedad indicada.
    """
    ultimo_arrendamiento = db.query(Arrendamiento).filter(
        Arrendamiento.propiedad_id == propiedad_id,
        Arrendamiento.inquilino_id == usuario_id
    ).order_by(Arrendamiento.fecha_inicio.desc()).first()

    if not ultimo_arrendamiento:
        raise HTTPException(status_code=404, detail="No existe arrendamiento para ese usuario y propiedad")

    desglose_arrendamiento = {
        "subtotal": float(ultimo_arrendamiento.subtotal),
        "iva": float(ultimo_arrendamiento.iva),
        "comision": float(ultimo_arrendamiento.comision),
        "total": float(ultimo_arrendamiento.subtotal) + float(ultimo_arrendamiento.iva) + float(ultimo_arrendamiento.comision)
    }
    return desglose_arrendamiento

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