from flask_restx import Namespace, Resource, reqparse
from werkzeug.datastructures import FileStorage
from datetime import date
from Servicios.Usuario_Servicio import Usuario_Servicio
from flask import request

api = Namespace('usuarios', description='Operaciones relacionadas con usuarios')

registro_parser = reqparse.RequestParser()
registro_parser.add_argument('imagen_perfil', location='files', type=FileStorage, required=False)
registro_parser.add_argument('nombre', required=True)
registro_parser.add_argument('apellidos', required=True)
registro_parser.add_argument('username', required=True)
registro_parser.add_argument('correo', required=True)
registro_parser.add_argument('telefono', required=False)
registro_parser.add_argument('fecha_nacimiento', required=False)
registro_parser.add_argument('domicilio', required=False)
registro_parser.add_argument('contraseña', required=True)
registro_parser.add_argument('fecha_nacimiento', type=str, required=False)

@api.route('/registro')
class UsuarioRegistro(Resource):
    @api.expect(registro_parser)
    def post(self):
        args = registro_parser.parse_args()
        resultado = Usuario_Servicio.registrar_usuario(args)
        return resultado
