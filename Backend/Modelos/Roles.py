from Modelos import db

class Rol(db.Model):
    """
    Modelo de Rol para definir diferentes roles de usuario en el sistema.
    """
    __tablename__ = 'roles'
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(50), unique=True, nullable=False)
    descripcion = db.Column(db.String(255))
    usuarios = db.relationship('Usuario', backref='rol', lazy=True)