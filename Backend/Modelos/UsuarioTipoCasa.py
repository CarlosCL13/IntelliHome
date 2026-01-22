from Modelos import db

class UsuarioTipoCasa(db.Model):
    """
    Tabla de asociación entre usuarios y tipos de casa.
    """
    __tablename__ = 'usuario_tipos_casa'
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), primary_key=True)
    tipo_casa_id = db.Column(db.Integer, db.ForeignKey('tipos_casa.id'), primary_key=True)
