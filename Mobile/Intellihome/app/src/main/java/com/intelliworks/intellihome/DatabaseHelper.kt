package com.intelliworks.intellihome

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

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

    fun isFingerprintEnabled(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USUARIO,
            arrayOf(COLUMN_FINGERPRINT),
            "${COLUMN_USERNAME} = ?",
            arrayOf(username),
            null,
            null,
            null
        )
        var enabled = false
        if (cursor.moveToFirst()) {
            enabled = cursor.getInt(0) == 1
        }
        cursor.close()
        return enabled
    }

    fun readUser(username: String, contrasena: String): Boolean {
        val db = this.readableDatabase
        val selection = "${COLUMN_USERNAME} = ? AND ${COLUMN_CONTRASENA} = ?"
        val selectionArgs = arrayOf(username, contrasena)
        val cursor = db.query(TABLE_USUARIO, null, selection, selectionArgs, null, null, null)
        val userExists = cursor.count > 0
        cursor.close()
        return userExists
    }
}