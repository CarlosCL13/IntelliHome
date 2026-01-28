import os
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
from Modelos.Dispositivo import Dispositivo, EstadoDispositivo

class Propiedad_Servicio:
    """
    Capa de Servicios de Dominio para la gestión de Propiedades.
    Encapsula la lógica de negocio, validaciones y transacciones de base de datos.
    """

    # -------------------------------------------------------------------------
    # CONFIGURACIÓN Y CONSTANTES
    # -------------------------------------------------------------------------

    # Conjunto de palabras prohibidas para moderación de contenido (Set para búsqueda O(1))
    PALABRAS_OBSCENAS = {
        'sexo', 'puta', 'puto', 'mierda', 'coño', 'cabron',
        'pendejo', 'picha', 'marica', 'pinga', 'verga', 'maricon',
        'nazi', 'fuck', 'nigga', 'fucker', 'perra'
    }

    # Extensiones de archivo permitidas (Lista blanca)
    EXTENSIONES_PERMITIDAS = {'png', 'jpg', 'jpeg', 'gif', 'svg', 'webp'}

    # Tamaño máximo de archivo: 5 MB (5 * 1024 * 1024 bytes)
    TAM_MAX_IMAGEN = 5 * 1024 * 1024 

    # Directorio base para almacenamiento de medios
    UPLOAD_DIR = os.path.join(os.getcwd(), 'uploads')

    # -------------------------------------------------------------------------
    # LÓGICA DE NEGOCIO (ENDPOINTS)
    # -------------------------------------------------------------------------

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
    ) -> dict:
        """
        Orquesta el registro completo de una propiedad.
        Realiza validaciones, persiste la entidad principal, gestiona archivos físicos
        y crea relaciones dependientes (IoT, Fotos) dentro de una transacción atómica.
        """
        errores = {}
        
        # Normalización de listas para evitar errores de tipo None
        hobbies_ids = hobbies_ids or []
        amenidades_ids = amenidades_ids or []

        try:
            # 1. EJECUCIÓN DE VALIDACIONES DE NEGOCIO
            # Se acumulan todos los errores posibles antes de interactuar con la BD.
            Propiedad_Servicio._validar_tipo_casa(db, tipo_casa_id, errores)
            Propiedad_Servicio._validar_titulo_publicacion(titulo_publicacion, errores)
            Propiedad_Servicio._validar_descripcion_publicacion(descripcion_publicacion, errores)
            Propiedad_Servicio._validar_huespedes(huespedes, errores)
            hobbies = Propiedad_Servicio._validar_hobbies(db, hobbies_ids, errores)
            amenidades = Propiedad_Servicio._validar_amenidades(db, amenidades_ids, errores)
            Propiedad_Servicio._validar_reglas_uso(reglas_uso, errores)
            Propiedad_Servicio._validar_fotos_propiedad(fotos_propiedad, errores)

            # Si existen errores de validación, se aborta el proceso retornando el detalle.
            if errores:
                return {'errores': errores}

            # 2. PERSISTENCIA DE LA ENTIDAD PRINCIPAL
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
                hobbies=hobbies,       # SQLAlchemy gestiona la tabla intermedia automáticamente
                amenidades=amenidades, # SQLAlchemy gestiona la tabla intermedia automáticamente
                reglas_uso=reglas_uso,
                estado=estado
            )
            db.add(nueva_propiedad)
            
            # 'flush' envía la instrucción SQL a la BD para obtener el ID autogenerado
            # sin cerrar la transacción. Necesario para las relaciones siguientes.
            db.flush() 

            # 3. CREACIÓN DE DISPOSITIVOS IOT POR DEFECTO
            Propiedad_Servicio.crear_luces_predeterminadas(db, nueva_propiedad.id)

            # 4. GESTIÓN DE ARCHIVOS Y PERSISTENCIA DE METADATOS DE FOTOS
            for foto in fotos_propiedad:
                # Guardado físico con UUID para evitar colisiones de nombre
                nombre_archivo = Propiedad_Servicio._guardar_foto_propiedad(foto)
                
                if nombre_archivo:
                    # Guardado de la referencia (ruta relativa) en BD
                    nueva_foto = FotoPropiedad(
                        propiedad_id=nueva_propiedad.id,
                        url_foto=os.path.join('uploads', nombre_archivo) # Guardamos ruta relativa standard
                    )
                    db.add(nueva_foto)

            # 5. CONFIRMACIÓN DE LA TRANSACCIÓN
            db.commit()
            return {"mensaje": "Propiedad registrada exitosamente", "id_propiedad": nueva_propiedad.id}

        except Exception as e:
            # En caso de cualquier error técnico, revertimos todos los cambios en BD
            db.rollback()
            print(f"[ERROR CRÍTICO] Registrar Propiedad: {str(e)}") # Log interno
            return {'errores': {'internal': 'Ocurrió un error interno al procesar la solicitud.'}}

    # -------------------------------------------------------------------------
    # VALIDACIONES
    # -------------------------------------------------------------------------

    @staticmethod
    def _validar_tipo_casa(db, tipo_casa_id, errores):
        """Verifica la existencia de la categoría de vivienda."""
        if tipo_casa_id:
            tipo_casa = db.query(TipoCasa).filter(TipoCasa.id == tipo_casa_id).first()
            if not tipo_casa:
                errores["tipo_casa"] = "El tipo de casa seleccionado no es válido."

    @staticmethod
    def _validar_titulo_publicacion(titulo: str, errores: dict):
        """Valida longitud y contenido inapropiado en el título."""
        if not titulo: return
        if len(titulo) > 150:
            errores["titulo_publicacion"] = "El título excede el límite de 150 caracteres."
        if Propiedad_Servicio._contiene_obscenidades(titulo):
            errores["titulo_publicacion"] = "El título contiene lenguaje inapropiado."

    @staticmethod
    def _validar_descripcion_publicacion(descripcion: str, errores: dict):
        """Valida contenido inapropiado en la descripción."""
        if not descripcion: return
        if Propiedad_Servicio._contiene_obscenidades(descripcion):
            errores["descripcion_publicacion"] = "La descripción contiene lenguaje inapropiado."

    @staticmethod
    def _validar_huespedes(huespedes: int, errores: dict):
        """Valida lógica de negocio básica para capacidad."""
        if huespedes <= 0:
            errores["huespedes"] = "La capacidad debe ser de al menos 1 huésped."

    @staticmethod
    def _validar_hobbies(db: Session, hobbies_ids: list, errores: dict):
        """Valida que todos los IDs de hobbies existan en el catálogo."""
        if not hobbies_ids: return []
        hobbies = db.query(Hobby).filter(Hobby.id.in_(hobbies_ids)).all()
        # Compara cantidad encontrada vs cantidad solicitada (usando set para únicos)
        if len(hobbies) != len(set(hobbies_ids)): 
            errores["hobbies"] = "Se han seleccionado hobbies inválidos o inexistentes."
        return hobbies
    
    @staticmethod
    def _validar_amenidades(db: Session, amenidades_ids: list, errores: dict):
        """Valida que todos los IDs de amenidades existan en el catálogo."""
        if not amenidades_ids: return []
        amenidades = db.query(Amenidad).filter(Amenidad.id.in_(amenidades_ids)).all()
        if len(amenidades) != len(set(amenidades_ids)):
            errores["amenidades"] = "Se han seleccionado amenidades inválidas o inexistentes."
        return amenidades

    @staticmethod
    def _validar_reglas_uso(reglas_uso: str, errores: dict):
        """Valida contenido inapropiado en las reglas."""
        if reglas_uso and Propiedad_Servicio._contiene_obscenidades(reglas_uso):
            errores["reglas_uso"] = "Las reglas de uso contienen lenguaje inapropiado."
    
    @staticmethod
    def _validar_fotos_propiedad(fotos_propiedad: list, errores: dict):
        """
        Validación exhaustiva de archivos multimedia.
        Verifica: Cantidad, Extensiones y Tamaño real en bytes.
        """
        if not fotos_propiedad:
            errores["fotos_propiedad"] = "Es obligatorio subir al menos una fotografía."
            return

        if len(fotos_propiedad) > 10:
            errores["fotos_propiedad"] = "Se ha excedido el límite de 10 fotografías."
            return

        for imagen in fotos_propiedad:
            filename = imagen.filename
            
            # 1. Validación de Extensión
            ext = filename.rsplit(".", 1)[-1].lower() if "." in filename else ""
            if ext not in Propiedad_Servicio.EXTENSIONES_PERMITIDAS:
                errores["fotos_propiedad"] = f"Archivo '{filename}' no soportado. Use: JPG, PNG, WEBP."
                return

            # 2. Validación de Tamaño (Lectura de stream segura)
            try:
                # Movemos el cursor al final para leer el tamaño
                imagen.file.seek(0, os.SEEK_END)
                size_bytes = imagen.file.tell()
                
                # CRÍTICO: Rebobinamos el cursor al inicio. 
                # Si no se hace esto, al intentar guardar el archivo después, se guardará vacío (0 bytes).
                imagen.file.seek(0) 
                
                if size_bytes > Propiedad_Servicio.TAM_MAX_IMAGEN:
                    errores["fotos_propiedad"] = f"La imagen '{filename}' excede el límite de 5MB."
                    return
            except Exception:
                errores["fotos_propiedad"] = f"Error al leer el archivo '{filename}'."
                return

    # -------------------------------------------------------------------------
    # UTILIDADES INTERNAS
    # -------------------------------------------------------------------------

    @staticmethod
    def _contiene_obscenidades(texto: str) -> bool:
        """
        Analiza un texto buscando coincidencias con la lista negra de palabras.
        Utiliza intersección de conjuntos para máxima eficiencia.
        """
        palabras_texto = set(texto.lower().split())
        return bool(palabras_texto & Propiedad_Servicio.PALABRAS_OBSCENAS)

    @staticmethod
    def _guardar_foto_propiedad(imagen_propiedad: UploadFile) -> str:
        """
        Guarda el archivo físico en el servidor asegurando unicidad en el nombre.
        
        Retorna:
            str: El nombre único del archivo generado (ej: 'a1b2-c3d4.jpg').
            None: Si ocurre un error de I/O.
        """
        try:
            # Asegurar que el directorio existe
            os.makedirs(Propiedad_Servicio.UPLOAD_DIR, exist_ok=True)
            
            # Generación de nombre único mediante UUID v4 para evitar colisiones
            ext = imagen_propiedad.filename.rsplit(".", 1)[-1].lower()
            nombre_unico = f"{uuid.uuid4()}.{ext}"
            
            ruta_destino = os.path.join(Propiedad_Servicio.UPLOAD_DIR, nombre_unico)
            
            # Escritura eficiente del stream binario
            with open(ruta_destino, "wb") as buffer:
                shutil.copyfileobj(imagen_propiedad.file, buffer)
                
            return nombre_unico
        except Exception as e:
            print(f"[ERROR IO] Guardando imagen: {str(e)}")
            return None
    
    @staticmethod
    def crear_luces_predeterminadas(db: Session, propiedad_id: int):
        """
        Inicializa la configuración IoT de la propiedad creando dispositivos virtuales
        (Luces) para las habitaciones estándar.
        """
        habitaciones_estandar = ["Sala", "Cocina", "Habitacion 1", "Bano 1", "Garaje"]
        
        # Preparación de objetos para inserción por lotes (Bulk Insert)
        dispositivos = [
            Dispositivo(
                nombre=f"Luz {hab}",
                tipo="led",
                propiedad_id=propiedad_id,
                habitacion=hab
            ) for hab in habitaciones_estandar
        ]
        
        db.add_all(dispositivos)
        db.flush() # Necesario para obtener los IDs de los dispositivos recién creados

        # Inicialización de estados (Apagado por defecto)
        estados = [
            EstadoDispositivo(
                dispositivo_id=disp.id,
                estado="apagado"
            ) for disp in dispositivos
        ]
        
        db.add_all(estados)
        # Nota: No se hace commit aquí, se delega al método principal para mantener atomicidad.
