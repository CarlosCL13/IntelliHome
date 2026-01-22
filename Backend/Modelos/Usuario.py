from Modelos import db
from Modelos.UsuarioTipoCasa import UsuarioTipoCasa  # <-- Agrega esta línea
from Modelos.Hobby import Hobby  # <-- Agrega esta línea
from Modelos.PreguntaRecuperacion import PreguntaRecuperacion  # <-- Ag

class Usuario(db.Model):
    """
    Modelo de Usuario que representa a los usuarios del sistema.
    """
    id = db.Column(db.Integer, primary_key=True)
    rol_id = db.Column(db.Integer, db.ForeignKey('roles.id'), nullable=False)
    imagen_perfil = db.Column(db.String(255), nullable=False)
    nombre = db.Column(db.String(80), nullable=False)
    apellidos = db.Column(db.String(120), nullable=False)
    correo = db.Column(db.String(120), unique=True, nullable=False)
    username = db.Column(db.String(80), unique=True, nullable=False)
    contrasena = db.Column(db.String(120), nullable=False)
    telefono = db.Column(db.String(20), nullable=False)
    fecha_nacimiento = db.Column(db.Date, nullable=False)
    hobbies = db.relationship('Hobby', secondary='usuario_hobbies', backref='usuarios')
    domicilio = db.Column(db.String(255), nullable=False)
    tipos_casa = db.relationship('TipoCasa', secondary='usuario_tipos_casa', backref='usuarios')
    pregunta_recuperacion_id = db.Column(db.Integer, db.ForeignKey('preguntas_recuperacion.id'), nullable=False)
    respuesta_recuperacion = db.Column(db.String(255), nullable=False)
    permitir_huella = db.Column(db.Integer, default=0, nullable=False)  # 0 = no permite, 1 = sí permite
    token_publico = db.Column(db.String(255), nullable=True)  # Token biométrico público
    intentos_fallidos = db.Column(db.Integer, default=0, nullable=False)
    estado_cuenta = db.Column(db.String(20), default='activo', nullable=False)  # valores: 'activo', 'bloqueado'

    # Información de tarjetas de crédito asociadas al usuario
    nombre_titular = db.Column(db.String(120), nullable=False)
    numero_encriptado = db.Column(db.String(255), nullable=False)
    fecha_expiracion = db.Column(db.String(7), nullable=False)  # formato MM/YYYY
    marca = db.Column(db.String(20), nullable=False)  # Visa, Mastercard, etc.
    ultimos_4 = db.Column(db.String(4), nullable=False)