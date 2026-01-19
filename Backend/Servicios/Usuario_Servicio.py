from passlib.context import CryptContext
import os
import re
from cryptography.fernet import Fernet

from fastapi import UploadFile
from sqlalchemy.orm import Session
from Modelos.Usuario import Usuario
from Modelos.Hobby import Hobby
from Modelos.TipoCasa import TipoCasa
from Modelos.PreguntaRecuperacion import PreguntaRecuperacion

class Usuario_Servicio:
    # Password hashing context
    pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

    # Ruta para la clave de encriptación
    KEY_PATH = os.path.join(os.getcwd(), 'clave_fernet.key')

    # Diccionario de palabras obscenas
    PALABRAS_OBSCENAS = {'sexo', 'puta', 'puto',
                        'mierda', 'coño', 'cabron',
                        'pendejo', 'picha', 'marica',
                        'pinga','verga', 'maricon',
                        'nazi', 'fuck', 'nigga', 'fucker'
                        'perra',
                        }
    
    # Diccionario de extensiones permitidas para imágenes
    EXTENSIONES_PERMITIDAS = {'png', 'jpg', 'jpeg', 'gif'}

    # Tamaño máximo permitido para imágenes
    TAM_MAX_IMAGEN = 1 * 1024 * 1024  # 1 MB

    #================================= Lógica Endpoints ================================= #

    # Registro de usuario
    @staticmethod
    def registrar_usuario(
        db: Session,
        nombre: str,
        apellidos: str,
        username: str,
        correo: str,
        telefono: str = None,
        fecha_nacimiento: str = None,
        domicilio: str = None,
        contraseña: str = None,
        imagen_perfil: UploadFile = None,
        hobbies_ids: list = None,
        tipos_casa_ids: list = None,
        pregunta_recuperacion_id: int = None,
        respuesta_recuperacion: str = None,
        permitir_huella: int = 0,
        nombre_titular: str = None,
        numero_tarjeta: str = None,
        fecha_expiracion: str = None
    ):
        errores = {}
        usuario = None
        try:
            Usuario_Servicio._validar_unicidad(db, correo, username, errores)
            Usuario_Servicio._validar_contraseña_registro(contraseña, errores)
            Usuario_Servicio._validar_nombres_obscenos(nombre, apellidos, username, errores)
            Usuario_Servicio._validar_telefono_registro(telefono, errores)
            hobbies = Usuario_Servicio._validar_hobbies(db, hobbies_ids, errores)
            tipos_casa = Usuario_Servicio._validar_tipos_casa(db, tipos_casa_ids, errores)
            Usuario_Servicio._validar_pregunta_recuperacion(db, pregunta_recuperacion_id, errores)
            Usuario_Servicio._validar_respuesta_recuperacion(respuesta_recuperacion, errores)
            Usuario_Servicio._validar_imagen(imagen_perfil, errores)
            fecha_nacimiento_date = Usuario_Servicio._validar_fecha_nacimiento(fecha_nacimiento, errores)
            Usuario_Servicio._validar_tarjeta(numero_tarjeta, fecha_expiracion, nombre_titular, errores)

            # Calcular marca y últimos 4 dígitos
            marca, ultimos_4 = Usuario_Servicio._calcular_marca_y_ultimos4(numero_tarjeta)

            if errores:
                return {'errores': errores}

            imagen_path = Usuario_Servicio._guardar_imagen(imagen_perfil)
            rol_id = 2
            hashed_password = Usuario_Servicio.pwd_context.hash(contraseña)
            # Encriptar el número de tarjeta antes de guardarlo
            numero_encriptado = Usuario_Servicio._encriptar_tarjeta(numero_tarjeta) if numero_tarjeta else None
            usuario = Usuario(
                imagen_perfil=imagen_path,
                nombre=nombre,
                apellidos=apellidos,
                username=username,
                correo=correo,
                telefono=telefono,
                fecha_nacimiento=fecha_nacimiento_date,
                domicilio=domicilio,
                contraseña=hashed_password,
                rol_id=rol_id,
                hobbies=hobbies,
                tipos_casa=tipos_casa,
                pregunta_recuperacion_id=pregunta_recuperacion_id,
                respuesta_recuperacion=respuesta_recuperacion,
                permitir_huella=permitir_huella,
                nombre_titular=nombre_titular,
                numero_encriptado=numero_encriptado,
                fecha_expiracion=fecha_expiracion,
                marca=marca,
                ultimos_4=ultimos_4
            )
            db.add(usuario)
            db.commit()
            return {'mensaje': 'Usuario registrado exitosamente'}
        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()
    
    # Login de usuario
    @staticmethod
    def login_usuario(db: Session, identificador: str, contraseña: str):
        """
        Permite iniciar sesión usando nombre de usuario, correo o teléfono y contraseña.
        """
        # Buscar usuario por nombre de usuario, correo o teléfono
        usuario = Usuario_Servicio._validar_identificador_login(db, identificador, errores={})

        # Si no se encuentra el usuario se retorna error
        if not usuario:
            return {"error": "Usuario no encontrado."}
        
        # si la cuenta está bloqueada se retorna error
        if usuario.estado_cuenta == 'bloqueado':
            return {"error": "La cuenta está bloqueada. Contacte al administrador."}
        
        validacion_contrasena = Usuario_Servicio._validar_contraseña_login(contraseña, usuario.contraseña)

        # validar si la contraseña es correcta
        if not validacion_contrasena:
            # Incrementar intentos fallidos
            usuario.intentos_fallidos += 1
            # Bloquear cuenta si supera 3 intentos
            if usuario.intentos_fallidos >= 3:
                usuario.estado_cuenta = 'bloqueado'
            db.commit()
            return {"error": "Contraseña incorrecta."}
        
        # Resetear intentos fallidos si login exitoso
        usuario.intentos_fallidos = 0
        db.commit()
        # Puedes retornar solo los datos necesarios
        return {
            "id": usuario.id,
            "username": usuario.username,
            "correo": usuario.correo,
            "telefono": usuario.telefono,
            "nombre": usuario.nombre,
            "apellidos": usuario.apellidos,
            "rol_id": usuario.rol_id,
            "estado_cuenta": usuario.estado_cuenta
        }

    #================================= VALIDACIONES ================================= #

    # Validación de unicidad de correo y username
    @staticmethod
    def _validar_unicidad(db, correo, username, errores):
        if db.query(Usuario).filter_by(correo=correo).first():
            errores['correo'] = 'El correo ya está registrado.'
        if db.query(Usuario).filter_by(username=username).first():
            errores['username'] = 'El nombre de usuario ya está registrado.'

    # Validación de contraseña (registro)
    @staticmethod
    def _validar_contraseña_registro(contraseña, errores):
        if len(contraseña) < 8 or not re.search(r'[A-Za-z]', contraseña) or not re.search(r'\d', contraseña):
            errores['contraseña'] = 'La contraseña debe tener al menos 8 caracteres y ser alfanumérica.'

    # Validación de nombres obscenos
    @staticmethod
    def _validar_nombres_obscenos(nombre, apellidos, username, errores):
        for campo, valor in [('nombre', nombre), ('apellidos', apellidos), ('username', username)]:
            if any(pal in valor.lower() for pal in Usuario_Servicio.PALABRAS_OBSCENAS):
                errores[campo] = 'El valor contiene palabras no permitidas.'

    # Validación de hobbies
    @staticmethod
    def _validar_hobbies(db, hobbies_ids, errores):
        hobbies = []
        if hobbies_ids:
            hobbies = db.query(Hobby).filter(Hobby.id.in_(hobbies_ids)).all()
            if len(hobbies) != len(hobbies_ids):
                errores['hobbies'] = 'Uno o más hobbies no existen.'
        return hobbies

    # Validación de tipos de casa
    @staticmethod
    def _validar_tipos_casa(db, tipos_casa_ids, errores):
        tipos_casa = []
        if tipos_casa_ids:
            tipos_casa = db.query(TipoCasa).filter(TipoCasa.id.in_(tipos_casa_ids)).all()
            if len(tipos_casa) != len(tipos_casa_ids):
                errores['tipos_casa'] = 'Uno o más tipos de casa no existen.'
        return tipos_casa

    # Validación de pregunta de recuperación
    @staticmethod
    def _validar_pregunta_recuperacion(db, pregunta_recuperacion_id, errores):
        pregunta = db.query(PreguntaRecuperacion).filter_by(id=pregunta_recuperacion_id).first()
        if not pregunta:
            errores['pregunta_recuperacion'] = 'La pregunta de recuperación no existe.'
        return pregunta

    # Validación de respuesta de recuperación
    @staticmethod
    def _validar_respuesta_recuperacion(respuesta_recuperacion, errores):
        if not respuesta_recuperacion:
            errores['respuesta_recuperacion'] = 'La respuesta de recuperación es obligatoria.'

    # Validación de imagen de perfil
    @staticmethod
    def _validar_imagen(imagen_perfil, errores):
        imagen_path = None
        if not imagen_perfil:
            errores['imagen_perfil'] = 'La imagen de perfil es obligatoria.'
            return imagen_path
        filename = imagen_perfil.filename
        ext = filename.rsplit('.', 1)[-1].lower() if '.' in filename else ''
        if ext not in Usuario_Servicio.EXTENSIONES_PERMITIDAS:
            errores['imagen_perfil'] = 'Formato de imagen inválido. Permitidos: PNG, JPG, JPEG, GIF.'
        imagen_perfil.file.seek(0, os.SEEK_END)
        size = imagen_perfil.file.tell()
        imagen_perfil.file.seek(0)
        if size > Usuario_Servicio.TAM_MAX_IMAGEN:
            errores['imagen_perfil'] = 'La imagen excede el tamaño máximo de 1 MB.'
        return imagen_path
    
    # Validación de teléfono (registro)
    @staticmethod
    def _validar_telefono_registro(telefono, errores):
        # se verifica si el telefono es numérico o si no fue dado
        if telefono is None or not str(telefono).isdigit():
            errores['telefono'] = 'El teléfono debe contener solo caracteres numéricos.'

    
    # Validación de fecha de nacimiento
    @staticmethod
    def _validar_fecha_nacimiento(fecha_nacimiento, errores):
        from datetime import datetime
        fecha_nacimiento_date = None
        if fecha_nacimiento:
            try:
                fecha_nacimiento_date = datetime.strptime(fecha_nacimiento, '%Y-%m-%d').date()
            except Exception:
                errores['fecha_nacimiento'] = 'El formato de la fecha debe ser YYYY-MM-DD.'
        return fecha_nacimiento_date

    # Validación de tarjeta de crédito
    @staticmethod
    def _validar_tarjeta(numero_tarjeta, fecha_expiracion, nombre_titular, errores):
        # Validar nombre del titular
        if not nombre_titular or not nombre_titular.strip():
            errores['nombre_titular'] = 'El nombre del titular es obligatorio.'
        # Validar número de tarjeta (debe ser 16 dígitos numéricos)
        if not numero_tarjeta or not numero_tarjeta.isdigit() or len(numero_tarjeta) != 16:
            errores['numero_tarjeta'] = 'El número de tarjeta debe tener 16 dígitos numéricos.'
        # Validar fecha de expiración (formato MM/YYYY y fecha futura)
        import re, datetime
        if not fecha_expiracion or not re.match(r'^(0[1-9]|1[0-2])/\d{4}$', fecha_expiracion):
            errores['fecha_expiracion'] = 'La fecha de expiración debe tener formato MM/YYYY.'
        else:
            mes, anio = fecha_expiracion.split('/')
            try:
                exp_date = datetime.date(int(anio), int(mes), 1)
                today = datetime.date.today().replace(day=1)
                if exp_date < today:
                    errores['fecha_expiracion'] = 'La fecha de expiración debe ser futura.'
            except Exception:
                errores['fecha_expiracion'] = 'La fecha de expiración no es válida.'
    
    # Validación de teléfono (login)
    @staticmethod
    def _validar_telefono_login(db, telefono, errores):
        """
        Verifica si el teléfono existe en la base de datos de usuarios.
        Si no existe, agrega un error en el diccionario de errores.
        """
        usuario = db.query(Usuario).filter(Usuario.telefono == str(telefono)).first()
        if not usuario:
            errores['telefono'] = 'El número de teléfono no está asociado a ningún usuario.'
    
    # validación de correo electrónico (login)
    @staticmethod
    def _validar_correo_login(db, correo, errores):
        """
        Verifica si el correo electrónico existe en la base de datos de usuarios.
        Si no existe, agrega un error en el diccionario de errores.
        """
        usuario = db.query(Usuario).filter(Usuario.correo == correo).first()
        if not usuario:
            errores['correo'] = 'El correo electrónico no está asociado a ningún usuario.'
    
    #validación de nombre de usuario (login)
    @staticmethod
    def _validar_usuario_login(db, nombre_usuario, errores):
        """
        Verifica si el nombre de usuario existe en la base de datos de usuarios.
        Si no existe, agrega un error en el diccionario de errores.
        """
        usuario = db.query(Usuario).filter(Usuario.username == nombre_usuario).first()
        if not usuario:
            errores['username'] = 'El nombre de usuario no está asociado a ningún usuario.'

    #validación de identificador (correo, telefono o nombre de usuario)  (login)
    @staticmethod
    def _validar_identificador_login(db, identificador, errores):
        """
        Verifica si el identificador (correo, teléfono o nombre de usuario) existe en la base de datos de usuarios.
        Si no existe, agrega un error en el diccionario de errores.
        """
        usuario = db.query(Usuario).filter(
            (Usuario.username == identificador) |
            (Usuario.correo == identificador) |
            (Usuario.telefono == identificador)
        ).first()
        if not usuario:
            errores['identificador'] = 'El identificador (correo, teléfono o nombre de usuario) no está asociado a ningún usuario.'
        return usuario

    # Validación de contraseña
    @staticmethod
    def _validar_contraseña_login(contraseña_plana: str, contraseña_hash: str) -> bool:
        """
        Verifica si la contraseña plana coincide con el hash almacenado.
        """

        # Se encripta la contraseña plana y se compara con el hash almacenado        
        validacion = Usuario_Servicio.pwd_context.verify(contraseña_plana, contraseña_hash)

        return validacion

    #================================= UTILIDADES ================================= #

    # Calcular marca y últimos 4 dígitos de la tarjeta
    @staticmethod
    def _calcular_marca_y_ultimos4(numero_tarjeta):
        # Determinar marca por el prefijo
        if numero_tarjeta.startswith('4'):
            marca = 'Visa'
        elif numero_tarjeta.startswith(('51', '52', '53', '54', '55')):
            marca = 'Mastercard'
        elif numero_tarjeta.startswith('34') or numero_tarjeta.startswith('37'):
            marca = 'American Express'
        else:
            marca = 'Desconocida'
        ultimos_4 = numero_tarjeta[-4:] if numero_tarjeta and len(numero_tarjeta) >= 4 else ''
        return marca, ultimos_4

    # Encriptación de tarjeta de crédito
    @staticmethod
    def _get_fernet():
        """
        Obtiene el objeto Fernet usando una clave persistente en disco.
        Si la clave no existe, la crea y la guarda.
        """
        if not os.path.exists(Usuario_Servicio.KEY_PATH):
            key = Fernet.generate_key()
            with open(Usuario_Servicio.KEY_PATH, 'wb') as f:
                f.write(key)
        else:
            with open(Usuario_Servicio.KEY_PATH, 'rb') as f:
                key = f.read()
        return Fernet(key)

    # Encriptar número de tarjeta
    @staticmethod
    def _encriptar_tarjeta(numero_tarjeta: str) -> str:
        """
        Encripta el número de tarjeta usando Fernet.
        """
        fernet = Usuario_Servicio._get_fernet()
        return fernet.encrypt(numero_tarjeta.encode()).decode()
    
    # Guardar imagen de perfil
    @staticmethod
    def _guardar_imagen(imagen_perfil):
        if imagen_perfil:
            uploads_dir = os.path.join(os.getcwd(), 'uploads')
            os.makedirs(uploads_dir, exist_ok=True)
            imagen_path = os.path.join(uploads_dir, imagen_perfil.filename)
            with open(imagen_path, "wb") as buffer:
                buffer.write(imagen_perfil.file.read())
            return imagen_path
        return None