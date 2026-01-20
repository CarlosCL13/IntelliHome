from Modelos import db

class PreguntaRecuperacion(db.Model):
    __tablename__ = 'preguntas_recuperacion'
    id = db.Column(db.Integer, primary_key=True)
    texto = db.Column(db.String(255), nullable=False, unique=True)
