from Modelos import db

class FotoPropiedad(db.Model):
    """
    Tabla para las fotos de las propiedades.
    """
    __tablename__ = 'fotos_propiedades'
    id = db.Column(db.Integer, primary_key=True)
    propiedad_id = db.Column(db.Integer, db.ForeignKey('propiedades.id'), nullable=False)
    url_foto = db.Column(db.String(255), nullable=False)  # ruta de la foto almacenada