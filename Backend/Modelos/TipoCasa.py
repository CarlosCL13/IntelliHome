from Modelos import db

class TipoCasa(db.Model):
    """
    Tabla para los tipos de casa.
    """
    __tablename__ = 'tipos_casa'
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(100), nullable=False, unique=True)
