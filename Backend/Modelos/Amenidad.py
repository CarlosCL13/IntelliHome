from sqlalchemy import Column, Integer, String, ForeignKey
from Base_de_Datos.db import Base

class Amenidad(Base):
    """
    Tabla para las amenidades disponibles en las propiedades.
    """
    __tablename__ = 'amenidades'

    id = Column(Integer, primary_key=True)
    nombre = Column(String(50), nullable=False, unique=True)

class PropiedadAmenidad(Base):
    """
    Tabla intermedia para la relación muchas a muchas entre propiedades y amenidades.
    """
    __tablename__ = 'propiedad_amenidades'

    # Claves foráneas que actúan como Primary Key compuesta
    propiedad_id = Column(Integer, ForeignKey('propiedad.id'), nullable=False, primary_key=True)
    amenidad_id = Column(Integer, ForeignKey('amenidades.id'), nullable=False, primary_key=True)
