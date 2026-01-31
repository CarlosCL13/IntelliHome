from sqlalchemy import Column, Integer, String, ForeignKey
from Base_de_Datos.db import Base

class UsuarioDispositivo(Base):
    """
    Representa un dispositivo físico (Celular/Tablet) asociado a un usuario.
    Permite notificaciones push multi-dispositivo.
    """
    __tablename__ = "usuario_dispositivos"

    id = Column(Integer, primary_key=True)
    usuario_id = Column(Integer, ForeignKey("usuario.id"), nullable=False)
    fcm_token = Column(String(255), unique=True, nullable=False)