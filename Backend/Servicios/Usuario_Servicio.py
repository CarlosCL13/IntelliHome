import os
from Modelos import db
from Modelos.Usuario import Usuario

import re
from Modelos.Roles import Rol

class Usuario_Servicio:
    PALABRAS_OBSCENAS = {'obsceno1', 'obsceno2', 'obsceno3'}  # Agrega palabras reales
    EXTENSIONES_PERMITIDAS = {'png', 'jpg', 'jpeg', 'gif'}
    TAM_MAX_IMAGEN = 1 * 1024 * 1024  # 1 MB

    @staticmethod
    def registrar_usuario(args):
        errores = {}



        # Validar unicidad de correo y username
        if Usuario.query.filter_by(correo=args['correo']).first():
            errores['correo'] = 'El correo ya está registrado.'
        if Usuario.query.filter_by(username=args['username']).first():
            errores['username'] = 'El nombre de usuario ya está registrado.'

        # Validar contraseña
        contraseña = args['contraseña']
        if len(contraseña) < 8 or not re.search(r'[A-Za-z]', contraseña) or not re.search(r'\d', contraseña):
            errores['contraseña'] = 'La contraseña debe tener al menos 8 caracteres y ser alfanumérica.'

        # Validar nombres obscenos
        for campo in ['nombre', 'apellidos', 'username']:
            valor = args[campo].lower()
            if any(pal in valor for pal in Usuario_Servicio.PALABRAS_OBSCENAS):
                errores[campo] = 'El valor contiene palabras no permitidas.'

        # Validar imagen
        imagen = args.get('imagen_perfil')
        imagen_path = None
        if imagen:
            filename = imagen.filename
            ext = filename.rsplit('.', 1)[-1].lower() if '.' in filename else ''
            if ext not in Usuario_Servicio.EXTENSIONES_PERMITIDAS:
                errores['imagen_perfil'] = 'Formato de imagen inválido. Permitidos: PNG, JPG, JPEG, GIF.'
            imagen.seek(0, os.SEEK_END)
            size = imagen.tell()
            imagen.seek(0)
            if size > Usuario_Servicio.TAM_MAX_IMAGEN:
                errores['imagen_perfil'] = 'La imagen excede el tamaño máximo de 1 MB.'

        if errores:
            return {'errores': errores}, 400

        # Guardar imagen si es válida
        if imagen:
            uploads_dir = os.path.join(os.getcwd(), 'uploads')
            os.makedirs(uploads_dir, exist_ok=True)
            imagen_path = os.path.join(uploads_dir, imagen.filename)
            imagen.save(imagen_path)


        # Asignar rol "usuario" por defecto (id=2)
        rol_id = 2

        # Convertir fecha_nacimiento a tipo date si es proporcionada
        fecha_nacimiento = args.get('fecha_nacimiento')
        from datetime import datetime
        fecha_nacimiento_date = None
        if fecha_nacimiento:
            try:
                fecha_nacimiento_date = datetime.strptime(fecha_nacimiento, '%Y-%m-%d').date()
            except Exception:
                errores['fecha_nacimiento'] = 'El formato de la fecha debe ser YYYY-MM-DD.'
                return {'errores': errores}, 400

        usuario = Usuario(
            imagen_perfil=imagen_path,
            nombre=args['nombre'],
            apellidos=args['apellidos'],
            username=args['username'],
            correo=args['correo'],
            telefono=args.get('telefono'),
            fecha_nacimiento=fecha_nacimiento_date,
            domicilio=args.get('domicilio'),
            contraseña=contraseña,
            rol_id=rol_id
        )
        db.session.add(usuario)
        db.session.commit()
        return {'mensaje': 'Usuario registrado exitosamente'}, 201
