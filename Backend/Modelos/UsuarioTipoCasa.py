from sqlalchemy import Column, Integer, ForeignKey
from Base_de_Datos.db import Base

class UsuarioTipoCasa(Base):
    """
    Tabla de asociación entre usuarios y tipos de casa.
    """
    __tablename__ = 'usuario_tipos_casa'
    usuario_id = Column(Integer, ForeignKey('usuario.id'), primary_key=True)
    tipo_casa_id = Column(Integer, ForeignKey('tipos_casa.id'), primary_key=True)
