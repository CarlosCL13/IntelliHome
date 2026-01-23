from Modelos import db

class Propiedad(db.Model):
    """
    Tabla para las propiedades.
    """
    __tablename__ = 'propiedades'
    id = db.Column(db.Integer, primary_key=True)
    usuario_id = db.Column(db.Integer, db.ForeignKey('usuario.id'), nullable=False) # propietario id
    tipo_casa_id = db.Column(db.Integer, db.ForeignKey('tipo_casa.id'), nullable=False)
    coordenadas = db.Column(db.String(100), nullable=False) # Falta de definir el formato
    titulo_publicacion = db.Column(db.String(150), nullable=False)
    descripcion_publicacion = db.Column(db.Text, nullable=True)
    precio_noche = db.Column(db.Float, nullable=False)
    huespedes = db.Column(db.Integer, nullable=False)
    habitaciones = db.Column(db.Integer, nullable=False)
    camas = db.Column(db.Integer, nullable=False)
    banos = db.Column(db.Integer, nullable=False)
    cocina = db.Column(db.Boolean, nullable=False) # Si o no
    reglas_uso = db.Column(db.Text, nullable=True)
    vehiculos = db.Column(db.Integer, nullable=True)
    estado = db.Column(db.String(20), nullable=False, default='disponible') # puede ser 'disponible' o 'no disponible'

# ANOTACIONES:
# Columnas de una propiedad: id, TipoCasa_id, Hobbie_id, coordenadas, titulo_publicacion, descripcion_publicacion, precio_noche, 
#   huespedes, habitaciones, camas, baños, cocina (si,no), reglas de uso, vehiculos (cantidad), estado. LISTO
# Se necesita una tabla de Hobbies de una propiedad (muchas a muchas) LISTO
# Se necesita una tabla de fotos de una propiedad (muchas a una) (1 min, 10 max para una propiedad) LISTO
# Se necesita una tabla extra para las casa alquiladas y los inquilinos (un historial), tiene que tener fecha de inicio y fin de alquiler - LISTO
# Se necesita una tabla extra para las amenidades de cada propiedad (muchas a muchas) LISTO
