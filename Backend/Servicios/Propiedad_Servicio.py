from passlib.context import CryptContext
import os
import re
import uuid
import shutil
from fastapi import UploadFile
from sqlalchemy.orm import Session
from Modelos.Usuario import Usuario
from Modelos.Hobby import Hobby
from Modelos.TipoCasa import TipoCasa
from Modelos.Amenidad import Amenidad
from Modelos.Propiedad import Propiedad
from Modelos.FotoPropiedad import FotoPropiedad
from Modelos.Dispositivo import Dispositivo
from Modelos.Dispositivo import EstadoDispositivo

class Propiedad_Servicio:

    # Hashing password context (CRÍTICO: NO BORRAR)
    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

    # Diccionario de palabras obscenas
    PALABRAS_OBSCENAS = {
        'sexo', 'puta', 'puto', 'mierda', 'coño', 'cabron',
        'pendejo', 'picha', 'marica', 'pinga','verga', 'maricon',
        'nazi', 'fuck', 'nigga', 'fucker', 'perra'
    }

    # Diccionario de extensiones permitidas para imágenes
    EXTENSIONES_PERMITIDAS = {'png', 'jpg', 'jpeg', 'gif', 'svg'}

    TAM_MAX_IMAGEN = 1 * 1024 * 1024

    # Directorio de subidas (De la V2)
    UPLOAD_DIR = os.path.join(os.getcwd(), 'uploads')

    #================================= Lógica Endpoints ================================= #

#Registro de una nueva propiedad
    @staticmethod
    def registrar_propiedad(
        db: Session,
        usuario_id: int,
        tipo_casa_id: int,
        latitud: float,
        longitud: float,
        titulo_publicacion: str,
        descripcion_publicacion: str,
        fotos_propiedad: list[UploadFile],
        precio_noche: float = 0.0,
        huespedes: int = 0,
        habitaciones: int = 0,
        camas: int = 0,
        banos: int = 0,
        hobbies_ids: list = None,
        amenidades_ids: list = None,
        reglas_uso: str = None,
        estado: str = 'disponible'
    ):
        errores = {}
        nueva_propiedad = None
        try:
            #validaciones
            Propiedad_Servicio._validar_tipo_casa(db, tipo_casa_id, errores)
            Propiedad_Servicio._validar_titulo_publicacion(titulo_publicacion, errores)
            Propiedad_Servicio._validar_descripcion_publicacion(descripcion_publicacion, errores)
            Propiedad_Servicio._validar_huespedes(huespedes, errores)
            hobbies = Propiedad_Servicio._validar_hobbies(db, hobbies_ids, errores)
            amenidades = Propiedad_Servicio._validar_amenidades(db, amenidades_ids, errores)
            Propiedad_Servicio._validar_reglas_uso(reglas_uso, errores)
            Propiedad_Servicio._validar_fotos_propiedad(fotos_propiedad, errores)

            if errores:
                return {'errores': errores}

            # Se registra la propiedad
            nueva_propiedad = Propiedad(
                usuario_id=usuario_id,
                tipo_casa_id=tipo_casa_id,
                latitud=latitud,
                longitud=longitud,
                titulo_publicacion=titulo_publicacion,
                descripcion_publicacion=descripcion_publicacion,
                precio_noche=precio_noche,
                huespedes=huespedes,
                habitaciones=habitaciones,
                camas=camas,
                banos=banos,
                hobbies=hobbies,
                amenidades=amenidades,
                reglas_uso=reglas_uso,
                estado=estado
            )
            db.add(nueva_propiedad)
            db.commit()

            # Crear luces predeterminadas para la propiedad
            Propiedad_Servicio.crear_luces_predeterminadas(db, nueva_propiedad.id)

            # Se almacenan las fotos de la propiedad
            for foto in fotos_propiedad:
                foto_path = Propiedad_Servicio._guardar_foto_propiedad(foto)
                nueva_foto = FotoPropiedad(
                    propiedad_id=nueva_propiedad.id,
                    url_foto=foto_path
                )
                db.add(nueva_foto)
            db.commit()

            return {"mensaje": "Propiedad registrada exitosamente"}
        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()

    #================================= VALIDACIONES ================================= #

    # Validación de tipo de casa
    @staticmethod
    def _validar_tipo_casa(db, tipo_casa_id, errores):
        if tipo_casa_id:
            tipo_casa = db.query(TipoCasa).filter(TipoCasa.id == tipo_casa_id).first()
            if not tipo_casa:
                errores["tipo_casa"] = "El tipo de casa no existe."
            return tipo_casa

    # Validar titulo
    @staticmethod
    def _validar_titulo_publicacion(titulo: str, errores: dict):
        if len(titulo) > 150:
            errores["titulo_publicacion"] = "El título de la publicación debe tener menos de 150 caracteres."
        if any(palabra in titulo.lower() for palabra in Propiedad_Servicio.PALABRAS_OBSCENAS):
            errores["titulo_publicacion"] = "El título de la publicación contiene palabras inapropiadas."

    # Validar descripción
    @staticmethod
    def _validar_descripcion_publicacion(descripcion: str, errores: dict):
        if any(palabra in descripcion.lower() for palabra in Propiedad_Servicio.PALABRAS_OBSCENAS):
            errores["descripcion_publicacion"] = "La descripción de la publicación contiene palabras inapropiadas."

    # Validar huespedes
    @staticmethod
    def _validar_huespedes(huespedes: int, errores: dict):
        if huespedes <= 0:
            errores["huespedes"] = "El número de huéspedes debe ser mayor que cero."

    # Validar hobbies
    @staticmethod
    def _validar_hobbies(db: Session, hobbies_ids: list, errores: dict):
        hobbies = []
        if hobbies_ids:
            hobbies = db.query(Hobby).filter(Hobby.id.in_(hobbies_ids)).all()
            if len(hobbies) != len(set(hobbies_ids)):
                errores["hobbies"] = "Uno o más hobbies no existen."
        return hobbies
    
    # Validar amenidades
    @staticmethod
    def _validar_amenidades(db: Session, amenidades_ids: list, errores: dict):
        amenidades = []
        if amenidades_ids:
            amenidades = db.query(Amenidad).filter(Amenidad.id.in_(amenidades_ids)).all()
            if len(amenidades) != len(set(amenidades_ids)):
                errores["amenidades"] = "Una o más amenidades no existen."
        return amenidades

    # Validar reglas
    @staticmethod
    def _validar_reglas_uso(reglas_uso: str, errores: dict):
        if reglas_uso and any(palabra in reglas_uso.lower() for palabra in Propiedad_Servicio.PALABRAS_OBSCENAS):
            errores["reglas_uso"] = "Las reglas de uso contienen palabras inapropiadas."
    
    # Validar fotos (Implementación V2 Mejorada)
    @staticmethod
    def _validar_fotos_propiedad(fotos_propiedad: list, errores: dict):
        """
        Validación V2: Verifica extensión y tamaño real de bytes de forma segura.
        """
        if not fotos_propiedad or len(fotos_propiedad) == 0:
            errores["fotos_propiedad"] = "Se requiere al menos una foto de la propiedad."
            return
        if len(fotos_propiedad) > 10:
            errores["fotos_propiedad"] = "No se permiten más de 10 fotos de la propiedad."
            return

        for imagen in fotos_propiedad:
            filename = imagen.filename
            extension = filename.rsplit('.', 1)[-1].lower() if '.' in filename else ''
            
            if extension not in Propiedad_Servicio.EXTENSIONES_PERMITIDAS:
                errores["fotos_propiedad"] = f"Archivo '{filename}' no permitido. Use: PNG, JPG, JPEG, GIF, SVG."
                return

            try:
                imagen.file.seek(0, os.SEEK_END)
                size = imagen.file.tell()
                imagen.file.seek(0)
                
                if size > Propiedad_Servicio.TAM_MAX_IMAGEN:
                    errores["fotos_propiedad"] = f"La foto '{filename}' excede el tamaño máximo (5MB)."
                    return
            except Exception:
                errores["fotos_propiedad"] = "Error al leer el archivo de imagen."
                return

    #================================= UTILIDADES ================================= #

    @staticmethod
    def _guardar_foto_propiedad(imagen_propiedad):
        """
        Guarda la foto con UUID (V2) para evitar sobrescritura.
        """
        try:
            os.makedirs(Propiedad_Servicio.UPLOAD_DIR, exist_ok=True)
            
            # Generar UUID
            ext = imagen_propiedad.filename.rsplit('.', 1)[-1].lower()
            nombre_unico = f"{uuid.uuid4()}.{ext}"
            
            ruta_destino = os.path.join(Propiedad_Servicio.UPLOAD_DIR, nombre_unico)
            
            # Guardado eficiente con shutil
            with open(ruta_destino, "wb") as buffer:
                shutil.copyfileobj(imagen_propiedad.file, buffer)
            
            return nombre_unico
        except Exception as e:
            print(f"Error guardando foto: {e}")
            return None
    
    @staticmethod
    def crear_luces_predeterminadas(db, propiedad_id):
        """
        Crea luces LED predeterminadas (Lista original completa).
        """
        habitaciones = [
            "Sala",
            "Cocina",
            "Habitacion 1",
            "Habitacion 2",
            "Habitacion 3",
            "Bano 1",
            "Bano 2",
            "Garaje"
        ]
        for nombre_habitacion in habitaciones:
            dispositivo = Dispositivo(
                nombre=f"Luz {nombre_habitacion}",
                tipo="led",
                propiedad_id=propiedad_id,
                habitacion=nombre_habitacion
            )
            db.add(dispositivo)
            db.flush()
            estado = EstadoDispositivo(
                dispositivo_id=dispositivo.id,
                estado="apagado"
            )
            db.add(estado)
        # El commit lo hace registrar_propiedad
