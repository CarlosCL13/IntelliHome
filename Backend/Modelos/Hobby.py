from sqlalchemy import Column, Integer, String, ForeignKey
from Base_de_Datos.db import Base

class Hobby(Base):
    __tablename__ = 'hobbies'

    id = Column(Integer, primary_key=True)
    nombre = Column(String(100), nullable=False, unique=True)

# --- Tablas Intermedias (Asociaciones) ---

class UsuarioHobby(Base):
    __tablename__ = 'usuario_hobbies'
    
    usuario_id = Column(Integer, ForeignKey('usuario.id'), primary_key=True)
    hobby_id = Column(Integer, ForeignKey('hobbies.id'), primary_key=True)

class PropiedadHobby(Base):
    __tablename__ = 'propiedad_hobbies'
    
    propiedad_id = Column(Integer, ForeignKey('propiedad.id'), primary_key=True)
    hobby_id = Column(Integer, ForeignKey('hobbies.id'), primary_key=True)
