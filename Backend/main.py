from fastapi import FastAPI, Depends, HTTPException, status, UploadFile, File, Form
from sqlalchemy.orm import Session
from Modelos import db
from Modelos.Usuario import Usuario
from Modelos.Roles import Rol
from Servicios.Usuario_Servicio import Usuario_Servicio

# --- IMPORTACIONES DE CONTROLADORES ---
from Controladores.Usuario_Controlador import router as usuario_router
from Controladores.Catalogos_Controlador import router as catalogos_router
from Controladores.Propiedad_Controlador import router as propiedad_router
from Controladores.Casa_Controlador import router as casa_router
from Controladores.Arrendamiento_Controlador import router as arrendamiento_router
from Controladores.NotificacionEvento_Controlador import router as notificacionEvento_router
from Controladores.UsuarioDispositivo_Controlador import router as usuarioDispositivo_router
from Controladores.NoDisponibilidadPropiedad_Controlador import router as noDisponibilidadPropiedad_router


from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from typing import Optional
import uvicorn
import os

# Dependencia para obtener la sesión de base de datos
from Base_de_Datos.db_session import get_db

# Inicialización de FastAPI
app = FastAPI(title="IntelliHome API", description="API para autenticación y gestión de propiedades")

# Servir archivos estáticos
uploads_dir = os.path.join(os.getcwd(), "uploads")
if not os.path.exists(uploads_dir):
    os.makedirs(uploads_dir)
app.mount("/uploads", StaticFiles(directory=uploads_dir), name="uploads")

# Configuración de CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {"mensaje": "¡FastAPI funcionando!"}

# --- REGISTRO DE ROUTERS ---
app.include_router(usuario_router)
app.include_router(catalogos_router)
app.include_router(propiedad_router)
app.include_router(casa_router)
app.include_router(arrendamiento_router)
app.include_router(notificacionEvento_router)
app.include_router(usuarioDispositivo_router)
app.include_router(noDisponibilidadPropiedad_router)
# Para correr: uvicorn app:app --reload
if __name__ == "__main__":
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)
