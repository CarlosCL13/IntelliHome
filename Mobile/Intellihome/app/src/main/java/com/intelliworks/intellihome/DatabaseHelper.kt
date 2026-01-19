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
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "data"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_FINGERPRINT = "fingerprint"

    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_USERNAME TEXT,
            $COLUMN_PASSWORD TEXT,
            $COLUMN_FINGERPRINT INTEGER
        )
    """.trimIndent()

        db?.execSQL(createTableQuery)
    }


    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val dropTableQuery = "DROP TABLE IF EXISTS $TABLE_NAME"
        db?.execSQL(dropTableQuery)
        onCreate(db)
    }

    fun insertUser(
        username: String,
        password: String,
        fingerprintEnabled: Boolean
    ): Long {
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_FINGERPRINT, if (fingerprintEnabled) 1 else 0)
        }
        val db = writableDatabase
        return db.insert(TABLE_NAME, null, values)
    }

    fun isFingerprintEnabled(username: String): Boolean {
        val db = readableDatabase

        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_FINGERPRINT),
            "$COLUMN_USERNAME = ?",
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

    fun readUser(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val selection = "$COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val selectionArgs = arrayOf(username, password)
        val cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null)

        val userExists = cursor.count > 0
        cursor.close()
        return userExists
    }
    fun getUserByUsername(username: String): User? {
        val db = readableDatabase

        val cursor = db.query(
            TABLE_NAME,
            null,
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
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                fingerprintEnabled = cursor.getInt(
                    cursor.getColumnIndexOrThrow(COLUMN_FINGERPRINT)
                ) == 1
            )
        }

        cursor.close()
        return user
    }




}