from flask import Flask
from flask_restx import Api
from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
from Modelos import db

from Modelos.Usuario import Usuario
from Modelos.Roles import Rol
from Controladores.Usuario_Controlador import api as usuario_ns


app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///C:/Users/Carlos CL/Desktop/Proyecto_Modelado/IntelliHome/Backend/Base_de_Datos/intellihome.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
db.init_app(app)
migrate = Migrate(app, db)


api = Api(app, doc='/docs', title='IntelliHome API', description='API para autenticación y gestión de propiedades')
api.add_namespace(usuario_ns, path='/usuarios')

if __name__ == '__main__':
    app.run(debug=True)