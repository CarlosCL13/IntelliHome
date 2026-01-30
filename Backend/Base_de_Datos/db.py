from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

# Configuración de la base de datos SQLite
DATABASE_URL = "sqlite:///C:/Users/Carlos CL/Desktop/Proyecto_Modelado/IntelliHome/Backend/Base_de_Datos/intellihome.db"
engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
