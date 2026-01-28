from fastapi import APIRouter, HTTPException, Depends
from Servicios.Casa_Servicio import CasaServicio
from Base_de_Datos.db_session import get_db
from sqlalchemy.orm import Session

router = APIRouter(prefix="/casa", tags=["casa"])

@router.post("/led")
def cambiar_led(propiedad_id: int, habitacion: str = None, accion: str = None, db: Session = Depends(get_db)):
    """
    Cambia el estado de un LED de una habitación o todos.
    accion: "encender", "apagar", "todos_encender", "todos_apagar"
    """
    resultado = CasaServicio.cambiar_led(db, propiedad_id, habitacion, accion)
    if "error" in resultado:
        if resultado["error"] == "Acción no válida":
            raise HTTPException(status_code=400, detail=resultado["error"])
        else:
            raise HTTPException(status_code=404, detail=resultado["error"])
    return resultado

@router.get("/estado_leds")
def obtener_estado_leds(propiedad_id: int, db: Session = Depends(get_db)):
    """
    Devuelve el estado actual de todos los LEDs por habitación.
    """
    return CasaServicio.obtener_estado_leds(db, propiedad_id)
