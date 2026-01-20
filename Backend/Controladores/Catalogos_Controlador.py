from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from Base_de_Datos.db_session import get_db
from Modelos.Hobby import Hobby
from Modelos.TipoCasa import TipoCasa
from Modelos.PreguntaRecuperacion import PreguntaRecuperacion

router = APIRouter(prefix="/catalogos", tags=["catálogos"])

@router.get("/hobbies")
def get_hobbies(db: Session = Depends(get_db)):
    return [{"id": h.id, "nombre": h.nombre} for h in db.query(Hobby).all()]

@router.get("/tipos-casa")
def get_tipos_casa(db: Session = Depends(get_db)):
    return [{"id": t.id, "nombre": t.nombre} for t in db.query(TipoCasa).all()]

@router.get("/preguntas-recuperacion")
def get_preguntas_recuperacion(db: Session = Depends(get_db)):
    return [{"id": p.id, "texto": p.texto} for p in db.query(PreguntaRecuperacion).all()]
