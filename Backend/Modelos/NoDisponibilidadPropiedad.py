from sqlalchemy import Column, Integer, Date, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from Base_de_Datos.db import Base

class NoDisponibilidadPropiedad(Base):
    """
    Tabla para la No disponibilidad de una propiedad en ciertas fechas.
    """
    __tablename__ = 'no_disponibilidad_propiedad'

    id = Column(Integer, primary_key=True)
    propiedad_id = Column(Integer, ForeignKey('propiedad.id'), nullable=False)
    fecha_noDisponible = Column(Date, nullable=False)

    # Relación inversa con Propiedad
    propiedad = relationship("Propiedad", backref="no_disponibilidad_propiedad")
