from Modelos import db

class Usuario(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    imagen_perfil = db.Column(db.String(255))
    nombre = db.Column(db.String(80), nullable=False)
    apellidos = db.Column(db.String(120), nullable=False)
    username = db.Column(db.String(80), unique=True, nullable=False)
    correo = db.Column(db.String(120), unique=True, nullable=False)
    telefono = db.Column(db.String(20))
    fecha_nacimiento = db.Column(db.Date)
    domicilio = db.Column(db.String(255))
    #hobbies = db.Column(db.String(255))
    #tipo_casa = db.Column(db.String(80))
    contraseña = db.Column(db.String(120), nullable=False)
    #datos_tarjeta = db.Column(db.String(255))
    rol_id = db.Column(db.Integer, db.ForeignKey('roles.id'), nullable=False)