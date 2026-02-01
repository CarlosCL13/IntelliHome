from sqlalchemy import Column, Integer, String, Date, ForeignKey
from sqlalchemy.orm import relationship
from Base_de_Datos.db import Base

class Usuario(Base):
    __tablename__ = "usuario"

    id = Column(Integer, primary_key=True)
    rol_id = Column(Integer, ForeignKey('roles.id'), nullable=False)
    imagen_perfil = Column(String(255), nullable=False)
    nombre = Column(String(80), nullable=False)
    apellidos = Column(String(120), nullable=False)
    correo = Column(String(120), unique=True, nullable=False)
    username = Column(String(80), unique=True, nullable=False)
    contrasena = Column(String(120), nullable=False)
    telefono = Column(String(20), nullable=False)
    fecha_nacimiento = Column(Date, nullable=False)
    
    # Relaciones Many-to-Many
    # Asegúrate de que las tablas intermedias 'usuario_hobbies' y 'usuario_tipos_casa' existan
    hobbies = relationship('Hobby', secondary='usuario_hobbies', backref='usuarios')
    domicilio = Column(String(255), nullable=False)
    tipos_casa = relationship('TipoCasa', secondary='usuario_tipos_casa', backref='usuarios')

    pregunta_recuperacion_id = Column(Integer, ForeignKey('preguntas_recuperacion.id'), nullable=False)
    respuesta_recuperacion = Column(String(255), nullable=False)
    
    permitir_huella = Column(Integer, default=0, nullable=False) # 0 = no, 1 = sí
    token_publico = Column(String(255), nullable=True) # Token biométrico (Obsoleto si usamos la nueva tabla, pero déjalo si quieres compatibilidad)
    
    intentos_fallidos = Column(Integer, default=0, nullable=False)
    estado_cuenta = Column(String(20), default='activo', nullable=False) # 'activo', 'bloqueado'

    # Información de tarjetas de crédito
    nombre_titular = Column(String(120), nullable=False)
    numero_encriptado = Column(String(255), nullable=False)
    fecha_expiracion = Column(String(7), nullable=False) # formato MM/YYYY
    marca = Column(String(20), nullable=False) # Visa, Mastercard, etc.
    ultimos_4 = Column(String(4), nullable=False)
