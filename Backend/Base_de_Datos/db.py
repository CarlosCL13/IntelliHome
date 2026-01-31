from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

# Configuración de la base de datos SQLite
DATABASE_URL = "sqlite:///C:/Users/Carlos CL/Desktop/Proyecto_Modelado/IntelliHome/Backend/Base_de_Datos/intellihome.db"
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# 3. Base para los modelos
Base = declarative_base()

# 4. Función de Dependencia
def get_db():
    """
    Crea una sesión de base de datos nueva para cada petición y la cierra al terminar.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
