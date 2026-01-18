
from Base_de_Datos.db import SessionLocal

# Dependencia para obtener la sesión de base de datos

def get_db():
    """
    Proporciona una sesión de base de datos a través de una dependencia de FastAPI.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
