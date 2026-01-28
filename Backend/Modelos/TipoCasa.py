from Modelos import db

class UsuarioPreferencia(db.Model):
    """
    Tabla de asociación entre usuarios y tipos de casa (Preferencias).
    """
    __tablename__ = 'usuario_tipos_casa'
    __table_args__ = {'extend_existing': True} 
    # --------------------------

    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), primary_key=True)
    tipo_casa_id = db.Column(db.Integer, db.ForeignKey('tipos_casa.id'), primary_key=True)

class TipoCasa(db.Model):
    __tablename__ = 'tipos_casa'
    
    id = db.Column(db.Integer, primary_key=True)
    nombre = db.Column(db.String(100), nullable=False, unique=True)
