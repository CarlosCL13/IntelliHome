from Modelos import db

class Arrendamiento(db.Model):
    """
    Tabla para los arrendamientos o alquileres.
    """
    __tablename__ = 'arrendamientos'
    id = db.Column(db.Integer, primary_key=True)
    propiedad_id = db.Column(db.Integer, db.ForeignKey('propiedades.id'), nullable=False)
    inquilino_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), nullable=False)
    fecha_inicio = db.Column(db.Date, nullable=False)
    fecha_fin = db.Column(db.Date, nullable=False)