from passlib.context import CryptContext
import os
import re

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

    # Hashing password context
    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

    # Diccionario de palabras obscenas
    PALABRAS_OBSCENAS = {'sexo', 'puta', 'puto',
                        'mierda', 'coño', 'cabron',
                        'pendejo', 'picha', 'marica',
                        'pinga','verga', 'maricon',
                        'nazi', 'fuck', 'nigga', 'fucker'
                        'perra',
                        }

    # Diccionario de extensiones permitidas para imágenes
    EXTENSIONES_PERMITIDAS = {'png', 'jpg', 'jpeg', 'gif', 'svg'}

    # Tamaño máximo permitido para imágenes
    TAM_MAX_IMAGEN = 1 * 1024 * 1024  # 1 MB

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
        cocina: bool = False,
        hobbies_ids: list = None,
        amenidades_ids: list = None,
        reglas_uso: str = None,
        vehiculos: int = None,
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
                cocina=cocina,
                hobbies=hobbies,
                amenidades=amenidades,
                reglas_uso=reglas_uso,
                vehiculos=vehiculos,
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
        """
        Verifica que los tipos de casa proporcionados existan en la base de datos.
        """
        if tipo_casa_id:
            tipo_casa = db.query(TipoCasa).filter(TipoCasa.id == tipo_casa_id).first()
            if not tipo_casa:
                errores["tipo_casa"] = "El tipo de casa no existe."
            return tipo_casa

    # Validar titulo de la publicación de una propiedad
    @staticmethod
    def _validar_titulo_publicacion(titulo: str, errores: dict):
        if len(titulo) > 150:
            errores["titulo_publicacion"] = "El título de la publicación debe tener menos de 150 caracteres."
        if any(palabra in titulo.lower() for palabra in Propiedad_Servicio.PALABRAS_OBSCENAS):
            errores["titulo_publicacion"] = "El título de la publicación contiene palabras inapropiadas."

    # Validar descripción de la publicación de una propiedad
    @staticmethod
    def _validar_descripcion_publicacion(descripcion: str, errores: dict):
        if any(palabra in descripcion.lower() for palabra in Propiedad_Servicio.PALABRAS_OBSCENAS):
            errores["descripcion_publicacion"] = "La descripción de la publicación contiene palabras inapropiadas."

    # Validar numero de huéspedes en la propiedad
    @staticmethod
    def _validar_huespedes(huespedes: int, errores: dict):
        """
        Verifica que el número de huéspedes sea mayor que cero.
        """
        if huespedes <= 0:
            errores["huespedes"] = "El número de huéspedes debe ser mayor que cero."

    # Validar hobbies relacionados a la propiedad
    @staticmethod
    def _validar_hobbies(db: Session, hobbies_ids: list, errores: dict):
        """
        Verifica que los hobbies proporcionados existan en la base de datos.
        """
        hobbies = []
        if hobbies_ids:
            hobbies = db.query(Hobby).filter(Hobby.id.in_(hobbies_ids)).all()
            if len(hobbies) != len(hobbies_ids):
                errores["hobbies"] = "Uno o más hobbies no existen."
        return hobbies
    
    # Validar amenidades relacionadas a la propiedad
    @staticmethod
    def _validar_amenidades(db: Session, amenidades_ids: list, errores: dict):
        """
        Verifica que las amenidades proporcionadas existan en la base de datos.
        """
        amenidades = []
        if amenidades_ids:
            amenidades = db.query(Amenidad).filter(Amenidad.id.in_(amenidades_ids)).all()
            if len(amenidades) != len(amenidades_ids):
                errores["amenidades"] = "Una o más amenidades no existen."
        return amenidades

    # validar reglas de uso de la propiedad
    @staticmethod
    def _validar_reglas_uso(reglas_uso: str, errores: dict):
        if any(palabra in reglas_uso.lower() for palabra in Propiedad_Servicio.PALABRAS_OBSCENAS):
            errores["reglas_uso"] = "Las reglas de uso contienen palabras inapropiadas."
    
    # validar fotos de propiedad
    @staticmethod
    def _validar_fotos_propiedad(fotos_propiedad: list, errores: dict):
        """
        Verifica que la lista de fotos de la propiedad cumpla con los requisitos, de formato, 
        de tamaño y que sea al menos una imagen y maximo diez imágenes.
        """
        imagen_path = None
        if not fotos_propiedad or len(fotos_propiedad) == 0:
            errores["fotos_propiedad"] = "Se requiere al menos una foto de la propiedad."
            return imagen_path
        if len(fotos_propiedad) > 10:
            errores["fotos_propiedad"] = "No se permiten más de 10 fotos de la propiedad."
            return imagen_path

        for imagen in fotos_propiedad:
            filename = imagen.filename
            extension = filename.rsplit('.', 1)[-1].lower() if '.' in filename else ''
            if extension not in Propiedad_Servicio.EXTENSIONES_PERMITIDAS:
                errores["fotos_propiedad"] = "Formato de imagen inválido. Permitidos: PNG, JPG, JPEG, GIF, SVG."
                continue
            imagen.file.seek(0, os.SEEK_END) # Mover al final del archivo
            size = imagen.file.tell() # se obtiene el tamaño
            imagen.file.seek(0) # Regresar al inicio del archivo
            if size > Propiedad_Servicio.TAM_MAX_IMAGEN:
                errores["fotos_propiedad"] = "La foto excede el tamaño máximo de 1 MB."
        return

    #================================= UTILIDADES ================================= #

    # Guardar imagen de perfil
    @staticmethod
    def _guardar_foto_propiedad(imagen_propiedad):
        """
        Guarda las fotos de una propiedad.
        """
        if imagen_propiedad:
            uploads_dir = os.path.join(os.getcwd(), 'uploads')
            os.makedirs(uploads_dir, exist_ok=True)
            imagen_path = os.path.join(uploads_dir, imagen_propiedad.filename)
            with open(imagen_path, "wb") as buffer:
                buffer.write(imagen_propiedad.file.read())
            return imagen_path
        return None
    
    # Crear luces predeterminadas para una propiedad
    @staticmethod
    def crear_luces_predeterminadas(db, propiedad_id):
        """
        Crea luces LED predeterminadas para una propiedad en habitaciones comunes.
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
            db.flush()  # Para obtener el id antes del commit
            estado = EstadoDispositivo(
                dispositivo_id=dispositivo.id,
                estado="apagado"
            )
            db.add(estado)
        db.commit()