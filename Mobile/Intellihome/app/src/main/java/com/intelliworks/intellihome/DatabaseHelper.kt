package com.intelliworks.intellihome

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.intelliworks.intellihome.model.User

class DatabaseHelper(private val context: Context):
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 3 // Incrementado para forzar recreación
        // Tabla usuario
        private const val TABLE_USUARIO = "usuario"
        private const val COLUMN_ID = "id"
        private const val COLUMN_ROL_ID = "rol_id"
        private const val COLUMN_IMAGEN_PERFIL = "imagen_perfil"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_APELLIDOS = "apellidos"
        private const val COLUMN_CORREO = "correo"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_CONTRASENA = "contrasena"
        private const val COLUMN_TELEFONO = "telefono"
        private const val COLUMN_FECHA_NACIMIENTO = "fecha_nacimiento"
        private const val COLUMN_DOMICILIO = "domicilio"
        private const val COLUMN_PREGUNTA_RECUPERACION_ID = "pregunta_recuperacion_id"
        private const val COLUMN_RESPUESTA_RECUPERACION = "respuesta_recuperacion"
        private const val COLUMN_FINGERPRINT = "fingerprintEnabled"
        private const val COLUMN_INTENTOS_FALLIDOS = "intentos_fallidos"
        private const val COLUMN_ESTADO_CUENTA = "estado_cuenta"
        private const val COLUMN_NOMBRE_TITULAR = "nombre_titular"
        private const val COLUMN_NUMERO_ENCRIPTADO = "numero_encriptado"
        private const val COLUMN_FECHA_EXPIRACION = "fecha_expiracion"
        private const val COLUMN_MARCA = "marca"
        private const val COLUMN_ULTIMOS_4 = "ultimos_4"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Solo tabla de usuario
        val createUsuarioTable = """
        CREATE TABLE usuario (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            rol_id INTEGER NOT NULL,
            imagen_perfil TEXT NOT NULL,
            nombre TEXT NOT NULL,
            apellidos TEXT NOT NULL,
            correo TEXT NOT NULL UNIQUE,
            username TEXT NOT NULL UNIQUE,
            contrasena TEXT NOT NULL,
            telefono TEXT NOT NULL,
            fecha_nacimiento TEXT NOT NULL,
            domicilio TEXT NOT NULL,
            pregunta_recuperacion_id INTEGER NOT NULL,
            respuesta_recuperacion TEXT NOT NULL,
            fingerprintEnabled INTEGER NOT NULL,
            intentos_fallidos INTEGER NOT NULL,
            estado_cuenta TEXT NOT NULL,
            nombre_titular TEXT NOT NULL,
            numero_encriptado TEXT NOT NULL,
            fecha_expiracion TEXT NOT NULL,
            marca TEXT NOT NULL,
            ultimos_4 TEXT NOT NULL
        )
        """.trimIndent()
        db?.execSQL(createUsuarioTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIO")
        onCreate(db)
    }

    fun insertUser(
        rol_id: Int,
        imagen_perfil: String,
        nombre: String,
        apellidos: String,
        correo: String,
        username: String,
        contrasena: String,
        telefono: String,
        fecha_nacimiento: String,
        domicilio: String,
        pregunta_recuperacion_id: Int,
        respuesta_recuperacion: String,
        fingerprintEnabled: Boolean,
        intentos_fallidos: Int,
        estado_cuenta: String,
        nombre_titular: String,
        numero_encriptado: String,
        fecha_expiracion: String,
        marca: String,
        ultimos_4: String
    ): Long {
        val values = ContentValues().apply {
            put(COLUMN_ROL_ID, rol_id)
            put(COLUMN_IMAGEN_PERFIL, imagen_perfil)
            put(COLUMN_NOMBRE, nombre)
            put(COLUMN_APELLIDOS, apellidos)
            put(COLUMN_CORREO, correo)
            put(COLUMN_USERNAME, username)
            put(COLUMN_CONTRASENA, contrasena)
            put(COLUMN_TELEFONO, telefono)
            put(COLUMN_FECHA_NACIMIENTO, fecha_nacimiento)
            put(COLUMN_DOMICILIO, domicilio)
            put(COLUMN_PREGUNTA_RECUPERACION_ID, pregunta_recuperacion_id)
            put(COLUMN_RESPUESTA_RECUPERACION, respuesta_recuperacion)
            put(COLUMN_FINGERPRINT, if (fingerprintEnabled) 1 else 0)
            put(COLUMN_INTENTOS_FALLIDOS, intentos_fallidos)
            put(COLUMN_ESTADO_CUENTA, estado_cuenta)
            put(COLUMN_NOMBRE_TITULAR, nombre_titular)
            put(COLUMN_NUMERO_ENCRIPTADO, numero_encriptado)
            put(COLUMN_FECHA_EXPIRACION, fecha_expiracion)
            put(COLUMN_MARCA, marca)
            put(COLUMN_ULTIMOS_4, ultimos_4)
        }
        val db = writableDatabase
        return db.insert(TABLE_USUARIO, null, values)
    }

    fun isFingerprintEnabled(identificador: String): Boolean {
        val db = this.readableDatabase
        // Buscamos en las tres columnas si la huella está activa (1)
        val selection = "($COLUMN_USERNAME = ? OR correo = ? OR telefono = ?) AND fingerprint_enabled = 1"
        val selectionArgs = arrayOf(identificador, identificador, identificador)

        val cursor = db.query(TABLE_USUARIO, null, selection, selectionArgs, null, null, null)
        val enabled = cursor.count > 0
        cursor.close()
        return enabled
    }

    fun readUser(identificador: String, contrasena: String): Boolean {
        val db = this.readableDatabase

        // Usamos OR para verificar si el identificador coincide con cualquiera de los tres campos
        val selection = "($COLUMN_USERNAME = ? OR correo = ? OR telefono = ?) AND $COLUMN_CONTRASENA = ?"

        // El identificador se repite tres veces en los argumentos para cubrir las tres columnas
        val selectionArgs = arrayOf(identificador, identificador, identificador, contrasena)

        val cursor = db.query(TABLE_USUARIO, null, selection, selectionArgs, null, null, null)
        val userExists = cursor.count > 0
        cursor.close()
        return userExists
    }
    fun getUserByUsername(username: String): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USUARIO,
            null, // selecciona todas las columnas
            "$COLUMN_USERNAME = ?",
            arrayOf(username),
            null,
            null,
            null
        )

        var user: User? = null
        if (cursor.moveToFirst()) {
            user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                rolId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ROL_ID)),
                imagenPerfil = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGEN_PERFIL)),
                nombre = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE)),
                apellidos = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APELLIDOS)),
                correo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CORREO)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                contrasena = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTRASENA)),
                telefono = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TELEFONO)),
                fechaNacimiento = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA_NACIMIENTO)),
                domicilio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOMICILIO)),
                preguntaRecuperacionId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PREGUNTA_RECUPERACION_ID)),
                respuestaRecuperacion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RESPUESTA_RECUPERACION)),
                fingerprintEnabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FINGERPRINT)) == 1,
                intentosFallidos = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INTENTOS_FALLIDOS)),
                estadoCuenta = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ESTADO_CUENTA)),
                nombreTitular = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRE_TITULAR)),
                numeroEncriptado = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMERO_ENCRIPTADO)),
                fechaExpiracion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA_EXPIRACION)),
                marca = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MARCA)),
                ultimos4 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ULTIMOS_4))
            )
        }
        cursor.close()
        return user
    }
    fun getActualUsername(identificador: String): String {
        val db = this.readableDatabase
        val selection = "$COLUMN_USERNAME = ? OR correo = ? OR telefono = ?"
        val selectionArgs = arrayOf(identificador, identificador, identificador)

        val cursor = db.query(TABLE_USUARIO, arrayOf(COLUMN_USERNAME), selection, selectionArgs, null, null, null)

        var usernameReal = identificador
        if (cursor.moveToFirst()) {
            usernameReal = cursor.getString(0)
        }
        cursor.close()
        return usernameReal
    }
    // Obtener el texto de la pregunta de seguridad asignada al usuario
    fun getRecoveryQuestion(identificador: String): String? {
        val db = this.readableDatabase
        var preguntaId: Int = -1

        val query = "SELECT $COLUMN_PREGUNTA_RECUPERACION_ID FROM $TABLE_USUARIO WHERE $COLUMN_USERNAME = ? OR $COLUMN_CORREO = ? OR $COLUMN_TELEFONO = ?"

        try {
            val cursor = db.rawQuery(query, arrayOf(identificador, identificador, identificador))
            if (cursor.moveToFirst()) {
                preguntaId = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        // Si no se encontró el usuario o el ID es inválido
        if (preguntaId == -1) return null

        // CARGAR DESDE ARRAYS.XML
        // Nota: Si en el Spinner el ID 1 es la primera pregunta, en el array es el índice 0.
        return try {
            val preguntasArray = context.resources.getStringArray(R.array.preguntas_recuperacion)

            // Ajuste de índice: Si tus IDs empiezan en 1, restamos 1.
            // Si el ID 0 era un mensaje tipo "Seleccione una pregunta", ajusta según tu lógica.
            val indice = preguntaId - 1

            if (indice in preguntasArray.indices) {
                preguntasArray[indice]
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Validar si la respuesta es correcta
    fun verifyRecoveryAnswer(identificador: String, respuesta: String): Boolean {
        val db = this.readableDatabase
        // Usamos LOWER() en SQL para comparar sin importar mayúsculas
        val selection = "($COLUMN_USERNAME = ? OR $COLUMN_CORREO = ? OR $COLUMN_TELEFONO = ?) AND LOWER($COLUMN_RESPUESTA_RECUPERACION) = LOWER(?)"
        val cursor = db.query(TABLE_USUARIO, null, selection, arrayOf(identificador, identificador, identificador, respuesta.trim()), null, null, null)
        val success = cursor.count > 0
        cursor.close()
        return success
    }

    // Actualizar la contraseña
    fun updatePassword(identificador: String, nuevaContrasena: String): Boolean {
        val db = this.writableDatabase
        val values = android.content.ContentValues().apply {
            put(COLUMN_CONTRASENA, nuevaContrasena)
        }
        val whereClause = "$COLUMN_USERNAME = ? OR correo = ? OR telefono = ?"
        val rows = db.update(TABLE_USUARIO, values, whereClause, arrayOf(identificador, identificador, identificador))
        return rows > 0
    }

}