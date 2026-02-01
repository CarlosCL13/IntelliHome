from sqlalchemy import Column, Integer, String, ForeignKey
from sqlalchemy.orm import relationship
from Base_de_Datos.db import Base

class Dispositivo(Base):
    __tablename__ = 'dispositivo'

    id = Column(Integer, primary_key=True)
    nombre = Column(String(50), nullable=False)
    tipo = Column(String(30), nullable=False, default='led')
    
    # Clave foránea
    propiedad_id = Column(Integer, ForeignKey('propiedad.id'), nullable=False)
    
    habitacion = Column(String(50), nullable=False)

    # Relación uno a muchos (Un dispositivo tiene muchos estados/historial)
    estados = relationship('EstadoDispositivo', backref='dispositivo')

class EstadoDispositivo(Base):
    __tablename__ = 'estado_dispositivo'

    id = Column(Integer, primary_key=True)
    
    # Clave foránea hacia Dispositivo
    dispositivo_id = Column(Integer, ForeignKey('dispositivo.id'), nullable=False)
    
    estado = Column(String(20), nullable=False, default='apagado')
