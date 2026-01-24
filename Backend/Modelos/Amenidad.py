from Modelos import db

class Amenidad(db.Model):
    """
    Tabla para las amenidades disponibles en las propiedades.
    """
    __tablename__ = 'amenidades'
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(50), nullable=False, unique=True)

class PropiedadAmenidad(db.Model):
    """
    Tabla intermedia para la relación muchas a muchas entre propiedades y amenidades.
    """
    __tablename__ = 'propiedad_amenidades'
    propiedad_id = db.Column(db.Integer, db.ForeignKey('propiedad.id'), nullable=False, primary_key=True)
    amenidad_id = db.Column(db.Integer, db.ForeignKey('amenidades.id'), nullable=False, primary_key=True)