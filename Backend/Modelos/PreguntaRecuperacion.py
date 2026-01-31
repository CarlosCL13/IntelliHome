from sqlalchemy import Column, Integer, String
from Base_de_Datos.db import Base

class PreguntaRecuperacion(Base):
    """
    Tabla para las preguntas de recuperación.
    """
    __tablename__ = 'preguntas_recuperacion'

    id = Column(Integer, primary_key=True)
    texto = Column(String(255), nullable=False, unique=True)
