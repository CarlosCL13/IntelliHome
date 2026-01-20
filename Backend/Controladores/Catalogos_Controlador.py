from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from Base_de_Datos.db_session import get_db
from Modelos.Hobby import Hobby
from Modelos.TipoCasa import TipoCasa
from Modelos.PreguntaRecuperacion import PreguntaRecuperacion

router = APIRouter(prefix="/catalogos", tags=["catálogos"])

# Endpoint para obtener la lista de hobbies
@router.get("/hobbies")
def get_hobbies(db: Session = Depends(get_db)):
    return [{"id": h.id, "nombre": h.nombre} for h in db.query(Hobby).all()]

# Endpoint para obtener la lista de tipos de casa
@router.get("/tipos-casa")
def get_tipos_casa(db: Session = Depends(get_db)):
    return [{"id": t.id, "nombre": t.nombre} for t in db.query(TipoCasa).all()]

# Endpoint para obtener la lista de preguntas de recuperación
@router.get("/preguntas-recuperacion")
def get_preguntas_recuperacion(db: Session = Depends(get_db)):
    return [{"id": p.id, "texto": p.texto} for p in db.query(PreguntaRecuperacion).all()]
