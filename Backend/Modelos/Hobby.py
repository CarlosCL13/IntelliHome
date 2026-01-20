from Modelos import db

class Hobby(db.Model):
    """
    Tabla para los hobbies.
    """
    __tablename__ = 'hobbies'
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(100), nullable=False, unique=True)

class UsuarioHobby(db.Model):
    """
    Tabla de asociación entre usuarios y hobbies.
    """
    __tablename__ = 'usuario_hobbies'
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), primary_key=True)
    hobby_id = db.Column(db.Integer, db.ForeignKey('hobbies.id'), primary_key=True)
