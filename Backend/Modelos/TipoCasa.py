from sqlalchemy import Column, Integer, String, ForeignKey
from Base_de_Datos.db import Base

class TipoCasa(Base):
    """
    Catálogo de tipos de casa (Ej: Apartamento, Cabaña, Mansión).
    """
    __tablename__ = 'tipos_casa'
    
    id = Column(Integer, primary_key=True)
    nombre = Column(String(100), nullable=False, unique=True)
