from sqlalchemy import Column, Integer, String, Float, Text, ForeignKey
from sqlalchemy.orm import relationship
from Base_de_Datos.db import Base

class Propiedad(Base):
    """
    Tabla para las propiedades.
    """
    __tablename__ = 'propiedad'

    id = Column(Integer, primary_key=True)
    
    # Llaves foráneas
    usuario_id = Column(Integer, ForeignKey('usuario.id'), nullable=False)
    tipo_casa_id = Column(Integer, ForeignKey('tipos_casa.id'), nullable=False)

    # Coordenadas y Detalles
    latitud = Column(Float, nullable=False)
    longitud = Column(Float, nullable=False)
    titulo_publicacion = Column(String(150), nullable=False)
    descripcion_publicacion = Column(Text, nullable=True)
    precio_noche = Column(Float, nullable=False)
    
    # Capacidad
    huespedes = Column(Integer, nullable=False)
    habitaciones = Column(Integer, nullable=False)
    camas = Column(Integer, nullable=False)
    banos = Column(Integer, nullable=False)
    
    # Reglas y Estado
    reglas_uso = Column(Text, nullable=True)
    estado = Column(String(20), nullable=False, default='disponible')

    # --- RELACIONES ---
    
    # Relación con el Dueño (Usuario)
    # Esto permite que desde una propiedad puedas acceder a propiedad.propietario.nombre
    propietario = relationship("Usuario", backref="mis_propiedades")

    # Relación con Tipo de Casa
    tipo_casa = relationship("TipoCasa", backref="propiedades")

    # Relaciones Muchos a Muchos (Hobbies y Amenidades)
    # Tablas intermedias 'propiedad_hobbies' y 'propiedad_amenidades'
    hobbies = relationship('Hobby', secondary='propiedad_hobbies', backref='propiedades')
    amenidades = relationship('Amenidad', secondary='propiedad_amenidades', backref='propiedades')
