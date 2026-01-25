from Modelos import db

class Dispositivo(db.Model):
    __tablename__ = 'dispositivo'
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(50), nullable=False)
    tipo = db.Column(db.String(30), nullable=False, default='led')
    propiedad_id = db.Column(db.Integer, db.ForeignKey('propiedad.id'), nullable=False)
    habitacion = db.Column(db.String(50), nullable=False)
    estados = db.relationship('EstadoDispositivo', backref='dispositivo', lazy=True)

class EstadoDispositivo(db.Model):
    __tablename__ = 'estado_dispositivo'
    id = db.Column(db.Integer, primary_key=True)
    dispositivo_id = db.Column(db.Integer, db.ForeignKey('dispositivo.id'), nullable=False)
    estado = db.Column(db.String(20), nullable=False, default='apagado')
