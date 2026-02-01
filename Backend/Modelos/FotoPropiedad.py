from sqlalchemy import Column, Integer, String, ForeignKey
from Base_de_Datos.db import Base

class FotoPropiedad(Base):
    """
    Tabla para las fotos de las propiedades.
    """
    __tablename__ = 'fotos_propiedades'

    id = Column(Integer, primary_key=True)
    
    # Clave foránea hacia la tabla 'propiedad'
    propiedad_id = Column(Integer, ForeignKey('propiedad.id'), nullable=False)
    
    url_foto = Column(String(255), nullable=False)  # ruta de la foto almacenada
