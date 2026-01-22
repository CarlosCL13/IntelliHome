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
        contrasena: str = None,
        imagen_perfil: UploadFile = None,
        hobbies_ids: list = None,
        tipos_casa_ids: list = None,
        pregunta_recuperacion_id: int = None,
        respuesta_recuperacion: str = None,
        permitir_huella: int = 0,
        nombre_titular: str = None,
        numero_tarjeta: str = None,
        fecha_expiracion: str = None,
        token_publico: str = None
    ):
        errores = {}
        usuario = None
        try:
            Usuario_Servicio._validar_unicidad(db, correo, username, telefono, errores)
            Usuario_Servicio._validar_contrasena_registro(contrasena, errores)
            Usuario_Servicio._validar_nombres_obscenos(nombre, apellidos, username, errores)
            Usuario_Servicio._validar_telefono(telefono, errores)
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
            hashed_password = Usuario_Servicio.pwd_context.hash(contrasena)
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
                contrasena=hashed_password,
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
                ultimos_4=ultimos_4,
                token_publico=token_publico
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
    def login_usuario(db: Session, identificador: str, contrasena: str):
        """
        Permite iniciar sesión usando nombre de usuario, correo o teléfono y contraseña.
        """
        errores = {}
        usuario = None

        try:
            # Buscar usuario por nombre de usuario, correo o teléfono
            usuario = Usuario_Servicio._validar_identificador(db, identificador, errores)
            
            if errores:
                return {'errores': errores}
            
            # si la cuenta está bloqueada se retorna error
            if usuario.estado_cuenta == 'bloqueado':
                errores['cuenta'] = 'La cuenta está bloqueada. Contacte al administrador.'
                return {'errores': errores}
            
            validacion_contrasena = Usuario_Servicio._validar_contrasena_login(contrasena, usuario.contrasena)

            # validar si la contraseña es correcta
            if not validacion_contrasena:
                # Incrementar intentos fallidos
                usuario.intentos_fallidos += 1
                # Bloquear cuenta si supera 3 intentos
                if usuario.intentos_fallidos >= 3:
                    usuario.estado_cuenta = 'bloqueado'
                db.commit()
                errores['contrasena'] = 'Contraseña incorrecta.'

                return {'errores': errores}
            
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
        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()

    # Obtención de la pregunta de recuperación de contraseña
    @staticmethod
    def obtener_pregunta_recuperacion(db: Session, identificador: str):
        """
        Permite obtener la pregunta de recuperación de contraseña usando nombre de usuario, correo o teléfono.
        """
        errores = {}
        usuario = None

        try:
            # Buscar usuario por nombre de usuario, correo o teléfono
            usuario = Usuario_Servicio._validar_identificador(db, identificador, errores)
            
            if errores:
                return {'errores': errores}

            #Se obtiene la información de la pregunta de recuperación asociada al usuario
            PreguntaRecuperacion_inf = db.query(PreguntaRecuperacion).filter_by(id=usuario.pregunta_recuperacion_id).first()
            
            return {
                "identificador": identificador,
                "pregunta_id": PreguntaRecuperacion_inf.id,
                "pregunta": PreguntaRecuperacion_inf.texto
            }
        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()

    # Restablecimiento de contraseña
    @staticmethod
    def restablecer_contrasena(db: Session, identificador: str, nueva_contrasena: str, respuesta_recuperacion: str):
        """
        Permite restablecer la contraseña si la respuesta de recuperación es correcta.
        """

        errores = {}
        usuario = None

        try:
            # Buscar usuario por nombre de usuario, correo o teléfono
            usuario = Usuario_Servicio._validar_identificador(db, identificador, errores)
            
            if errores:
                return {'errores': errores}
            
            # Validar respuesta de recuperación
            if usuario.respuesta_recuperacion.lower() != respuesta_recuperacion.lower():
                errores['respuesta_recuperacion'] = 'Respuesta de recuperación incorrecta.'
                return {'errores': errores}
            
            # Validar nueva contraseña
            Usuario_Servicio._validar_contrasena_registro(nueva_contrasena, errores)
            if errores:
                return {'errores': errores}
            
            # Se encripta la nueva contraseña y se actualiza
            hashed_password = Usuario_Servicio.pwd_context.hash(nueva_contrasena)
            usuario.contrasena = hashed_password

            # Resetear intentos fallidos y estado de cuenta
            usuario.intentos_fallidos = 0
            usuario.estado_cuenta = 'activo'

            db.commit()
            return {'mensaje': 'Contraseña restablecida exitosamente'}
        
        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()

    #================================= VALIDACIONES ================================= #

    # Validación de unicidad de correo y username
    @staticmethod
    def _validar_unicidad(db, correo, username, telefono, errores):
        """
        Verifica que el correo y el nombre de usuario sean únicos en la base de datos.
        """
        if db.query(Usuario).filter_by(correo=correo).first():
            errores['correo'] = 'El correo ya está registrado.'
        if db.query(Usuario).filter_by(username=username).first():
            errores['username'] = 'El nombre de usuario ya está registrado.'
        if db.query(Usuario).filter_by(telefono=telefono).first():
            errores['telefono'] = 'El teléfono ya está registrado.'

    # Validación de contraseña (registro)
    @staticmethod
    def _validar_contrasena_registro(contrasena, errores):
        """
        Verifica que la contraseña cumpla con los requisitos mínimos.
        """
        if len(contrasena) < 8 or not re.search(r'[A-Za-z]', contrasena) or not re.search(r'\d', contrasena):
            errores['contrasena'] = 'La contraseña debe tener al menos 8 caracteres y ser alfanumérica.'

    # Validación de nombres obscenos
    @staticmethod
    def _validar_nombres_obscenos(nombre, apellidos, username, errores):
        """
        Verifica que el nombre, apellidos y username no contengan palabras obscenas.
        """
        for campo, valor in [('nombre', nombre), ('apellidos', apellidos), ('username', username)]:
            if any(pal in valor.lower() for pal in Usuario_Servicio.PALABRAS_OBSCENAS):
                errores[campo] = 'El valor contiene palabras no permitidas.'

    # Validación de hobbies
    @staticmethod
    def _validar_hobbies(db, hobbies_ids, errores):
        """
        Verifica que los hobbies proporcionados existan en la base de datos.
        """
        hobbies = []
        if hobbies_ids:
            hobbies = db.query(Hobby).filter(Hobby.id.in_(hobbies_ids)).all()
            if len(hobbies) != len(hobbies_ids):
                errores['hobbies'] = 'Uno o más hobbies no existen.'
        return hobbies

    # Validación de tipos de casa
    @staticmethod
    def _validar_tipos_casa(db, tipos_casa_ids, errores):
        """
        Verifica que los tipos de casa proporcionados existan en la base de datos.
        """
        tipos_casa = []
        if tipos_casa_ids:
            tipos_casa = db.query(TipoCasa).filter(TipoCasa.id.in_(tipos_casa_ids)).all()
            if len(tipos_casa) != len(tipos_casa_ids):
                errores['tipos_casa'] = 'Uno o más tipos de casa no existen.'
        return tipos_casa

    # Validación de pregunta de recuperación
    @staticmethod
    def _validar_pregunta_recuperacion(db, pregunta_recuperacion_id, errores):
        """
        Determina si la pregunta de recuperación existe en la base de datos.
        """
        pregunta = db.query(PreguntaRecuperacion).filter_by(id=pregunta_recuperacion_id).first()
        if not pregunta:
            errores['pregunta_recuperacion'] = 'La pregunta de recuperación no existe.'
        return pregunta

    # Validación de respuesta de recuperación
    @staticmethod
    def _validar_respuesta_recuperacion(respuesta_recuperacion, errores):
        """
        Verifica que la respuesta de recuperación no esté vacía.
        """
        if not respuesta_recuperacion:
            errores['respuesta_recuperacion'] = 'La respuesta de recuperación es obligatoria.'

    # Validación de imagen de perfil
    @staticmethod
    def _validar_imagen(imagen_perfil, errores):
        """
        Verifica que la imagen de perfil cumpla con los requisitos de formato y tamaño.
        """
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
    def _validar_telefono(telefono, errores):
        """
        Verifica que el teléfono contenga solo caracteres numéricos.
        """
        if telefono is None or not str(telefono).isdigit():
            errores['telefono'] = 'El teléfono debe contener solo caracteres numéricos.'

    
    # Validación de fecha de nacimiento
    @staticmethod
    def _validar_fecha_nacimiento(fecha_nacimiento, errores):
        """
        Verifica que la fecha de nacimiento tenga el formato correcto YYYY-MM-DD.
        """
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
        """
        Determina si los datos de la tarjeta de crédito son válidos.
        """
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
    
    #validación de identificador (correo, telefono o nombre de usuario)  (login)
    @staticmethod
    def _validar_identificador(db, identificador, errores):
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
    def _validar_contrasena_login(contrasena_plana: str, contrasena_hash: str) -> bool:
        """
        Verifica si la contraseña plana coincide con el hash almacenado.
        """

        # Se encripta la contraseña plana y se compara con el hash almacenado        
        validacion = Usuario_Servicio.pwd_context.verify(contrasena_plana, contrasena_hash)

        return validacion

    #================================= UTILIDADES ================================= #

    # Calcular marca y últimos 4 dígitos de la tarjeta
    @staticmethod
    def _calcular_marca_y_ultimos4(numero_tarjeta):
        """
        Determina la marca de la tarjeta y obtiene los últimos 4 dígitos.
        """
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

    # Validación de contraseña
    @staticmethod
    def verificar_contrasena(contrasena_plana: str, contrasena_hash: str) -> bool:
        """
        Verifica si la contraseña plana coincide con el hash almacenado.
        """
        return Usuario_Servicio.pwd_context.verify(contrasena_plana, contrasena_hash)
    
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
        """
        Guarda la imagen de perfil en el sistema de archivos y devuelve la ruta.
        """
        if imagen_perfil:
            uploads_dir = os.path.join(os.getcwd(), 'uploads')
            os.makedirs(uploads_dir, exist_ok=True)
            imagen_path = os.path.join(uploads_dir, imagen_perfil.filename)
            with open(imagen_path, "wb") as buffer:
                buffer.write(imagen_perfil.file.read())
            return imagen_path
        return None
# Buscar usuario por token público
    @staticmethod
    def buscar_por_token_publico(db: Session, token_publico: str):
        """
        Busca un usuario por token público y devuelve la info tipo login.
        """
        errores = {}
        try:
            usuario = db.query(Usuario).filter_by(token_publico=token_publico).first()
            if not usuario:
                errores['token_publico'] = 'No existe usuario con ese token.'
                return {'errores': errores}
            if usuario.estado_cuenta == 'bloqueado':
                errores['cuenta'] = 'La cuenta está bloqueada. Contacte al administrador.'
                return {'errores': errores}
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
        except Exception as e:
            db.rollback()
            return {'errores': {'internal': f'Error interno: {str(e)}'}}
        finally:
            db.close()
