from Modelos import db

class Arrendamiento(db.Model):
    """
    Tabla para los arrendamientos o alquileres.
    """
    __tablename__ = 'arrendamientos'
    id = db.Column(db.Integer, primary_key=True)
    propiedad_id = db.Column(db.Integer, db.ForeignKey('propiedad.id'), nullable=False)
    inquilino_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), nullable=False)
    fecha_inicio = db.Column(db.Date, nullable=False)
    fecha_fin = db.Column(db.Date, nullable=False)
    subtotal = db.Column(db.Numeric(precision=12, scale=2), nullable=False)
    iva = db.Column(db.Numeric(precision=12, scale=2), nullable=False)
    comision = db.Column(db.Numeric(precision=12, scale=2), nullable=False)