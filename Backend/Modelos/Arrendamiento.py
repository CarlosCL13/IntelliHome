from sqlalchemy import Column, Integer, Date, ForeignKey, Numeric
from sqlalchemy.orm import relationship
from Base_de_Datos.db import Base

class Arrendamiento(Base):
    """
    Tabla para los arrendamientos o alquileres.
    """
    __tablename__ = 'arrendamientos'
    
    id = Column(Integer, primary_key=True)
    
    # Claves foráneas
    propiedad_id = Column(Integer, ForeignKey('propiedad.id'), nullable=False)
    inquilino_id = Column(Integer, ForeignKey('usuario.id'), nullable=False)
    
    fecha_inicio = Column(Date, nullable=False)
    fecha_fin = Column(Date, nullable=False)

    subtotal = Column(Numeric(precision=12, scale=2), nullable=False)
    iva = Column(Numeric(precision=12, scale=2), nullable=False)
    comision = Column(Numeric(precision=12, scale=2), nullable=False)

    # --- Relaciones (Opcionales pero muy útiles) ---
    # Esto te permite hacer: arrendamiento.inquilino.nombre
    propiedad = relationship("Propiedad", backref="contratos_alquiler")
    inquilino = relationship("Usuario", backref="mis_alquileres")
