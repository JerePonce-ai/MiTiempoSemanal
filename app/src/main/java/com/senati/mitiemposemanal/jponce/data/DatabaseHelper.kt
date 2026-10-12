package com.senati.mitiemposemanal.jponce.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MiTiempoSemanal.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_CATEGORIAS = "categorias"
        const val COLUMN_CATEGORIA_ID = "id"
        const val COLUMN_CATEGORIA_NOMBRE = "nombre"

        const val TABLE_TAREAS = "tareas"
        const val COLUMN_TAREA_ID = "id"
        const val COLUMN_TAREA_NOMBRE = "nombre"
        const val COLUMN_TAREA_CATEGORIA_ID = "categoria_id"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableCategorias = """
            CREATE TABLE $TABLE_CATEGORIAS (
                $COLUMN_CATEGORIA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CATEGORIA_NOMBRE TEXT NOT NULL UNIQUE COLLATE NOCASE
            )
        """.trimIndent()

        val createTableTareas = """
            CREATE TABLE $TABLE_TAREAS (
                $COLUMN_TAREA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TAREA_NOMBRE TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COLUMN_TAREA_CATEGORIA_ID INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_TAREA_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CATEGORIA_ID) ON DELETE RESTRICT
            )
        """.trimIndent()

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)

        // Insertar 4 categorías por defecto
        val categoriasDefault = arrayOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (cat in categoriasDefault) {
            db.execSQL("INSERT INTO $TABLE_CATEGORIAS ($COLUMN_CATEGORIA_NOMBRE) VALUES (?)", arrayOf(cat))
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAREAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIAS")
        onCreate(db)
    }
}
