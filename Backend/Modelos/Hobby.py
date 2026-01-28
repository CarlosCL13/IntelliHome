from Modelos import db

class Hobby(db.Model):
    __tablename__ = 'hobbies'
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(100), nullable=False, unique=True)

class UsuarioHobby(db.Model):
    __tablename__ = 'usuario_hobbies'
    __table_args__ = {'extend_existing': True}
    
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), primary_key=True)
    hobby_id = db.Column(db.Integer, db.ForeignKey('hobbies.id'), primary_key=True)

class PropiedadHobby(db.Model):
    __tablename__ = 'propiedad_hobbies'
    __table_args__ = {'extend_existing': True}
    
    propiedad_id = db.Column(db.Integer, db.ForeignKey('propiedad.id'), primary_key=True)
    hobby_id = db.Column(db.Integer, db.ForeignKey('hobbies.id'), primary_key=True)
