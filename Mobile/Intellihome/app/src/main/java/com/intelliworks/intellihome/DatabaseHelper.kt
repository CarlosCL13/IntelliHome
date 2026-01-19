package com.intelliworks.intellihome

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.intelliworks.intellihome.model.User

/**
 * Gestiona el ciclo de vida de la base de datos y provee interfaces de acceso
 * para las operaciones de persistencia de la entidad usuario.
 */
class DatabaseHelper(private val contexto: Context):
    SQLiteOpenHelper(contexto, NOMBRE_BASE_DATOS, null, VERSION_BASE_DATOS) {

    companion object {
        private const val NOMBRE_BASE_DATOS = "UserDatabase.db"
        private const val VERSION_BASE_DATOS = 3

        // Estructura de la tabla usuario
        private const val TABLA_USUARIO = "usuario"
        private const val COLUMNA_ID = "id"
        private const val COLUMNA_ROL_ID = "rol_id"
        private const val COLUMNA_IMAGEN_PERFIL = "imagen_perfil"
        private const val COLUMNA_NOMBRE = "nombre"
        private const val COLUMNA_APELLIDOS = "apellidos"
        private const val COLUMNA_CORREO = "correo"
        private const val COLUMNA_USERNAME = "username"
        private const val COLUMNA_CONTRASENA = "contrasena"
        private const val COLUMNA_TELEFONO = "telefono"
        private const val COLUMNA_FECHA_NACIMIENTO = "fecha_nacimiento"
        private const val COLUMNA_DOMICILIO = "domicilio"
        private const val COLUMNA_PREGUNTA_RECUPERACION_ID = "pregunta_recuperacion_id"
        private const val COLUMNA_RESPUESTA_RECUPERACION = "respuesta_recuperacion"
        private const val COLUMNA_HUELLA_DIGITAL = "fingerprintEnabled"
        private const val COLUMNA_INTENTOS_FALLIDOS = "intentos_fallidos"
        private const val COLUMNA_ESTADO_CUENTA = "estado_cuenta"
        private const val COLUMNA_NOMBRE_TITULAR = "nombre_titular"
        private const val COLUMNA_NUMERO_ENCRIPTADO = "numero_encriptado"
        private const val COLUMNA_FECHA_EXPIRACION = "fecha_expiracion"
        private const val COLUMNA_MARCA = "marca"
        private const val COLUMNA_ULTIMOS_4 = "ultimos_4"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Definición de esquema para la tabla de usuarios
        val crearTablaUsuario = """
        CREATE TABLE $TABLA_USUARIO (
            $COLUMNA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMNA_ROL_ID INTEGER NOT NULL,
            $COLUMNA_IMAGEN_PERFIL TEXT NOT NULL,
            $COLUMNA_NOMBRE TEXT NOT NULL,
            $COLUMNA_APELLIDOS TEXT NOT NULL,
            $COLUMNA_CORREO TEXT NOT NULL UNIQUE,
            $COLUMNA_USERNAME TEXT NOT NULL UNIQUE,
            $COLUMNA_CONTRASENA TEXT NOT NULL,
            $COLUMNA_TELEFONO TEXT NOT NULL,
            $COLUMNA_FECHA_NACIMIENTO TEXT NOT NULL,
            $COLUMNA_DOMICILIO TEXT NOT NULL,
            $COLUMNA_PREGUNTA_RECUPERACION_ID INTEGER NOT NULL,
            $COLUMNA_RESPUESTA_RECUPERACION TEXT NOT NULL,
            $COLUMNA_HUELLA_DIGITAL INTEGER NOT NULL,
            $COLUMNA_INTENTOS_FALLIDOS INTEGER NOT NULL,
            $COLUMNA_ESTADO_CUENTA TEXT NOT NULL,
            $COLUMNA_NOMBRE_TITULAR TEXT NOT NULL,
            $COLUMNA_NUMERO_ENCRIPTADO TEXT NOT NULL,
            $COLUMNA_FECHA_EXPIRACION TEXT NOT NULL,
            $COLUMNA_MARCA TEXT NOT NULL,
            $COLUMNA_ULTIMOS_4 TEXT NOT NULL
        )
        """.trimIndent()
        db?.execSQL(crearTablaUsuario)
    }

    override fun onUpgrade(db: SQLiteDatabase?, versionAnterior: Int, versionNueva: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLA_USUARIO")
        onCreate(db)
    }

    /**
     * Registra un nuevo perfil de usuario en el sistema.
     */
    fun insertUser(
        rolId: Int, imagenPerfil: String, nombre: String, apellidos: String,
        correo: String, username: String, contrasena: String, telefono: String,
        fechaNacimiento: String, domicilio: String, preguntaId: Int,
        respuesta: String, huellaActiva: Boolean, intentos: Int,
        estado: String, titular: String, numeroEnc: String,
        expiracion: String, marca: String, ultimos4: String
    ): Long {
        val valores = ContentValues().apply {
            put(COLUMNA_ROL_ID, rolId)
            put(COLUMNA_IMAGEN_PERFIL, imagenPerfil)
            put(COLUMNA_NOMBRE, nombre)
            put(COLUMNA_APELLIDOS, apellidos)
            put(COLUMNA_CORREO, correo)
            put(COLUMNA_USERNAME, username)
            put(COLUMNA_CONTRASENA, contrasena)
            put(COLUMNA_TELEFONO, telefono)
            put(COLUMNA_FECHA_NACIMIENTO, fechaNacimiento)
            put(COLUMNA_DOMICILIO, domicilio)
            put(COLUMNA_PREGUNTA_RECUPERACION_ID, preguntaId)
            put(COLUMNA_RESPUESTA_RECUPERACION, respuesta)
            put(COLUMNA_HUELLA_DIGITAL, if (huellaActiva) 1 else 0)
            put(COLUMNA_INTENTOS_FALLIDOS, intentos)
            put(COLUMNA_ESTADO_CUENTA, estado)
            put(COLUMNA_NOMBRE_TITULAR, titular)
            put(COLUMNA_NUMERO_ENCRIPTADO, numeroEnc)
            put(COLUMNA_FECHA_EXPIRACION, expiracion)
            put(COLUMNA_MARCA, marca)
            put(COLUMNA_ULTIMOS_4, ultimos4)
        }
        val db = writableDatabase
        return db.insert(TABLA_USUARIO, null, valores)
    }

    /**
     * Verifica si el usuario tiene habilitada la autenticación biométrica.
     */
    fun isFingerprintEnabled(identificador: String): Boolean {
        val db = this.readableDatabase
        val seleccion = "($COLUMNA_USERNAME = ? OR $COLUMNA_CORREO = ? OR $COLUMNA_TELEFONO = ?) AND $COLUMNA_HUELLA_DIGITAL = 1"
        val argumentos = arrayOf(identificador, identificador, identificador)

        val cursor = db.query(TABLA_USUARIO, null, seleccion, argumentos, null, null, null)
        val estaActivo = cursor.count > 0
        cursor.close()
        return estaActivo
    }

    /**
     * Valida las credenciales de acceso comparando identificador y contraseña.
     */
    fun readUser(identificador: String, contrasena: String): Boolean {
        val db = this.readableDatabase
        val seleccion = "($COLUMNA_USERNAME = ? OR $COLUMNA_CORREO = ? OR $COLUMNA_TELEFONO = ?) AND $COLUMNA_CONTRASENA = ?"
        val argumentos = arrayOf(identificador, identificador, identificador, contrasena)

        val cursor = db.query(TABLA_USUARIO, null, seleccion, argumentos, null, null, null)
        val existeUsuario = cursor.count > 0
        cursor.close()
        return existeUsuario
    }

    /**
     * Recupera un objeto User completo basado en el nombre de usuario único.
     */
    fun getUserByUsername(nombreUsuario: String): User? {
        val db = readableDatabase
        val cursor = db.query(TABLA_USUARIO, null, "$COLUMNA_USERNAME = ?", arrayOf(nombreUsuario), null, null, null)

        var usuario: User? = null
        if (cursor.moveToFirst()) {
            usuario = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNA_ID)),
                rolId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNA_ROL_ID)),
                imagenPerfil = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_IMAGEN_PERFIL)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_NOMBRE)),
                apellidos = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_APELLIDOS)),
                correo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_CORREO)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_USERNAME)),
                contrasena = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_CONTRASENA)),
                telefono = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_TELEFONO)),
                fechaNacimiento = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_FECHA_NACIMIENTO)),
                domicilio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_DOMICILIO)),
                preguntaRecuperacionId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNA_PREGUNTA_RECUPERACION_ID)),
                respuestaRecuperacion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_RESPUESTA_RECUPERACION)),
                fingerprintEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNA_HUELLA_DIGITAL)) == 1,
                intentosFallidos = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMNA_INTENTOS_FALLIDOS)),
                estadoCuenta = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_ESTADO_CUENTA)),
                nombreTitular = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_NOMBRE_TITULAR)),
                numeroEncriptado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_NUMERO_ENCRIPTADO)),
                fechaExpiracion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_FECHA_EXPIRACION)),
                marca = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_MARCA)),
                ultimos4 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMNA_ULTIMOS_4))
            )
        }
        cursor.close()
        return usuario
    }

    /**
     * Resuelve el nombre de usuario asociado a un correo, teléfono o nombre de usuario.
     */
    fun getActualUsername(identificador: String): String {
        val db = this.readableDatabase
        val seleccion = "$COLUMNA_USERNAME = ? OR $COLUMNA_CORREO = ? OR $COLUMNA_TELEFONO = ?"
        val argumentos = arrayOf(identificador, identificador, identificador)

        val cursor = db.query(TABLA_USUARIO, arrayOf(COLUMNA_USERNAME), seleccion, argumentos, null, null, null)
        var nombreUsuarioReal = identificador
        if (cursor.moveToFirst()) {
            nombreUsuarioReal = cursor.getString(0)
        }
        cursor.close()
        return nombreUsuarioReal
    }

    /**
     * Recupera el texto de la pregunta de seguridad desde recursos mediante su identificador numérico.
     */
    fun getRecoveryQuestion(identificador: String): String? {
        val db = this.readableDatabase
        var preguntaId: Int = -1

        val seleccionSql = "SELECT $COLUMNA_PREGUNTA_RECUPERACION_ID FROM $TABLA_USUARIO WHERE $COLUMNA_USERNAME = ? OR $COLUMNA_CORREO = ? OR $COLUMNA_TELEFONO = ?"

        try {
            val cursor = db.rawQuery(seleccionSql, arrayOf(identificador, identificador, identificador))
            if (cursor.moveToFirst()) {
                preguntaId = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            return null
        }

        if (preguntaId == -1) return null

        return try {
            val listaPreguntas = contexto.resources.getStringArray(R.array.preguntas_recuperacion)
            val indice = preguntaId - 1
            if (indice in listaPreguntas.indices) listaPreguntas[indice] else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Valida la respuesta de seguridad ignorando discrepancias de mayúsculas y minúsculas.
     */
    fun verifyRecoveryAnswer(identificador: String, respuesta: String): Boolean {
        val db = this.readableDatabase
        val seleccion = "($COLUMNA_USERNAME = ? OR $COLUMNA_CORREO = ? OR $COLUMNA_TELEFONO = ?) AND LOWER($COLUMNA_RESPUESTA_RECUPERACION) = LOWER(?)"
        val cursor = db.query(TABLA_USUARIO, null, seleccion, arrayOf(identificador, identificador, identificador, respuesta.trim()), null, null, null)
        val esCorrecta = cursor.count > 0
        cursor.close()
        return esCorrecta
    }

    /**
     * Realiza la actualización de la contraseña del usuario.
     */
    fun updatePassword(identificador: String, nuevaContrasena: String): Boolean {
        val db = this.writableDatabase
        val valores = ContentValues().apply {
            put(COLUMNA_CONTRASENA, nuevaContrasena)
        }
        val clausulaDonde = "$COLUMNA_USERNAME = ? OR $COLUMNA_CORREO = ? OR $COLUMNA_TELEFONO = ?"
        val filasAfectadas = db.update(TABLA_USUARIO, valores, clausulaDonde, arrayOf(identificador, identificador, identificador))
        return filasAfectadas > 0
    }
}