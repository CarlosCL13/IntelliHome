from sqlalchemy import Column, Integer, String
from sqlalchemy.orm import relationship
from Base_de_Datos.db import Base

class Rol(Base):
    __tablename__ = "roles"

    id = Column(Integer, primary_key=True)
    nombre = Column(String(50), unique=True, nullable=False)
    descripcion = Column(String(255))

    # Relación inversa: Esto permite hacer usuario.rol
    # Usamos string "Usuario" para evitar imports circulares
    usuarios = relationship("Usuario", backref="rol")
