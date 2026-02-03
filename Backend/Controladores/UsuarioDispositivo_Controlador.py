from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, Request
from sqlalchemy.orm import Session
from datetime import date
from Base_de_Datos.db_session import get_db
from Modelos.UsuarioDispositivo import UsuarioDispositivo
from Servicios.UsuarioDispositivo_Servicio import UsuarioDispositivo_Servicio

# Configuración del router para los endpoints de propiedades
router = APIRouter(prefix="/usuarios-dispositivos", tags=["usuariosDispositivos"])

# -----------------------------------------------------------------------------
# Endpoint: Vincular Dispositivo al Usuario
# -----------------------------------------------------------------------------
@router.post("/usuario-dispositivo")
def vincular_usuario_dispositivo(
        usuario_id: int = Form(...), 
        fcm_token: str = Form(...), 
        db: Session = Depends(get_db)
    ):
    """
    Vincula un dispositivo (token) al usuario actual que inicio sesión.
    """

    # Servicio para vincular el dispositivo al usuario
    resultado = UsuarioDispositivo_Servicio.vincular_usuario_dispositivo(
        db=db,
        usuario_id=usuario_id,
        fcm_token=fcm_token
    )

    if 'errores' in resultado:
        raise HTTPException(status_code=422, detail=resultado['errores'])
    
    return resultado

# -----------------------------------------------------------------------------
# Endpoint: eliminar Dispositivo del Usuario
# -----------------------------------------------------------------------------
from fastapi import Query

@router.delete("/usuario-dispositivo")
def eliminar_usuario_dispositivo(
        usuario_id: int,
        fcm_token: str,
        db: Session = Depends(get_db)
    ):
    """
    Elimina la vinculación de un dispositivo (token) al usuario actual.
    """

    # Servicio para eliminar el dispositivo al usuario
    resultado = UsuarioDispositivo_Servicio.eliminar_usuario_dispositivo(
        db=db,
        usuario_id=usuario_id,
        fcm_token=fcm_token
    )

    if 'errores' in resultado:
        raise HTTPException(status_code=422, detail=resultado['errores'])
    
    return resultado
