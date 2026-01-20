from fastapi import FastAPI, Depends, HTTPException, status, UploadFile, File, Form
from sqlalchemy.orm import Session
from Modelos import db
from Modelos.Usuario import Usuario
from Modelos.Roles import Rol
from Servicios.Usuario_Servicio import Usuario_Servicio
from Controladores.Usuario_Controlador import router as usuario_router
from Controladores.Catalogos_Controlador import router as catalogos_router
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional
import uvicorn
import os

# Dependencia para obtener la sesión de base de datos
from Base_de_Datos.db_session import get_db

# Inicialización de FastAPI
app = FastAPI(title="IntelliHome API", description="API para autenticación y gestión de propiedades")

# Configuración de CORS (opcional, útil para desarrollo móvil)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Endpoint de prueba
@app.get("/")
def read_root():
    return {"mensaje": "¡FastAPI funcionando!"}


# Registrar el router modular de usuarios
app.include_router(usuario_router)
app.include_router(catalogos_router)

# Para correr: uvicorn app:app --reload
if __name__ == "__main__":
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)
