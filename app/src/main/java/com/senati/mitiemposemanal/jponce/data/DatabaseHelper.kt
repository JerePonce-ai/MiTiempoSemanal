package com.senati.mitiemposemanal.jponce.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "MiTiempoSemanal.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_CATEGORIAS = "categorias"
        const val COLUMN_CATEGORIA_ID = "id"
        const val COLUMN_CATEGORIA_NOMBRE = "nombre"

        const val TABLE_TAREAS = "tareas"
        const val COLUMN_TAREA_ID = "id"
        const val COLUMN_TAREA_NOMBRE = "nombre"
        const val COLUMN_TAREA_CATEGORIA_ID = "categoria_id"

        const val TABLE_EVENTOS = "eventos"
        const val COLUMN_EVENTO_ID = "id"
        const val COLUMN_EVENTO_TAREA_ID = "tarea_id"
        const val COLUMN_EVENTO_INICIO = "inicio"
        const val COLUMN_EVENTO_FIN = "fin"
        const val COLUMN_EVENTO_DURACION_SEGUNDOS = "duracion_segundos"

        const val TABLE_REPORTES = "reportes"
        const val COLUMN_REPORTE_ID = "id"
        const val COLUMN_REPORTE_SEMANA_INICIO = "semana_inicio"
        const val COLUMN_REPORTE_SEMANA_FIN = "semana_fin"
        const val COLUMN_REPORTE_CATEGORIA_ID = "categoria_id"
        const val COLUMN_REPORTE_DURACION_SEGUNDOS = "duracion_segundos"
        const val COLUMN_REPORTE_PORCENTAJE = "porcentaje"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableCategorias = """
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIAS (
                $COLUMN_CATEGORIA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CATEGORIA_NOMBRE TEXT NOT NULL UNIQUE COLLATE NOCASE
            )
        """.trimIndent()

        val createTableTareas = """
            CREATE TABLE IF NOT EXISTS $TABLE_TAREAS (
                $COLUMN_TAREA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TAREA_NOMBRE TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COLUMN_TAREA_CATEGORIA_ID INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_TAREA_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CATEGORIA_ID) ON DELETE RESTRICT
            )
        """.trimIndent()

        val createTableEventos = """
            CREATE TABLE IF NOT EXISTS $TABLE_EVENTOS (
                $COLUMN_EVENTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EVENTO_TAREA_ID INTEGER NOT NULL,
                $COLUMN_EVENTO_INICIO TEXT NOT NULL,
                $COLUMN_EVENTO_FIN TEXT,
                $COLUMN_EVENTO_DURACION_SEGUNDOS INTEGER,
                FOREIGN KEY($COLUMN_EVENTO_TAREA_ID) REFERENCES $TABLE_TAREAS($COLUMN_TAREA_ID) ON DELETE RESTRICT
            )
        """.trimIndent()

        val createTableReportes = """
            CREATE TABLE IF NOT EXISTS $TABLE_REPORTES (
                $COLUMN_REPORTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_REPORTE_SEMANA_INICIO TEXT NOT NULL,
                $COLUMN_REPORTE_SEMANA_FIN TEXT NOT NULL,
                $COLUMN_REPORTE_CATEGORIA_ID INTEGER NOT NULL,
                $COLUMN_REPORTE_DURACION_SEGUNDOS INTEGER NOT NULL,
                $COLUMN_REPORTE_PORCENTAJE REAL NOT NULL,
                FOREIGN KEY($COLUMN_REPORTE_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CATEGORIA_ID) ON DELETE RESTRICT,
                UNIQUE($COLUMN_REPORTE_SEMANA_INICIO, $COLUMN_REPORTE_CATEGORIA_ID) ON CONFLICT IGNORE
            )
        """.trimIndent()

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)
        db.execSQL(createTableEventos)
        db.execSQL(createTableReportes)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_eventos_tarea_inicio ON $TABLE_EVENTOS($COLUMN_EVENTO_TAREA_ID, $COLUMN_EVENTO_INICIO)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reportes_semana ON $TABLE_REPORTES($COLUMN_REPORTE_SEMANA_INICIO)")

        // Insertar 4 categorías por defecto si recién se crea la base de datos
        val categoriasDefault = arrayOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (cat in categoriasDefault) {
            db.execSQL("INSERT OR IGNORE INTO $TABLE_CATEGORIAS ($COLUMN_CATEGORIA_NOMBRE) VALUES (?)", arrayOf(cat))
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createTableEventos = """
                CREATE TABLE IF NOT EXISTS $TABLE_EVENTOS (
                    $COLUMN_EVENTO_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_EVENTO_TAREA_ID INTEGER NOT NULL,
                    $COLUMN_EVENTO_INICIO TEXT NOT NULL,
                    $COLUMN_EVENTO_FIN TEXT,
                    $COLUMN_EVENTO_DURACION_SEGUNDOS INTEGER,
                    FOREIGN KEY($COLUMN_EVENTO_TAREA_ID) REFERENCES $TABLE_TAREAS($COLUMN_TAREA_ID) ON DELETE RESTRICT
                )
            """.trimIndent()

            db.execSQL(createTableEventos)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_eventos_tarea_inicio ON $TABLE_EVENTOS($COLUMN_EVENTO_TAREA_ID, $COLUMN_EVENTO_INICIO)")
        }

        if (oldVersion < 3) {
            val createTableReportes = """
                CREATE TABLE IF NOT EXISTS $TABLE_REPORTES (
                    $COLUMN_REPORTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_REPORTE_SEMANA_INICIO TEXT NOT NULL,
                    $COLUMN_REPORTE_SEMANA_FIN TEXT NOT NULL,
                    $COLUMN_REPORTE_CATEGORIA_ID INTEGER NOT NULL,
                    $COLUMN_REPORTE_DURACION_SEGUNDOS INTEGER NOT NULL,
                    $COLUMN_REPORTE_PORCENTAJE REAL NOT NULL,
                    FOREIGN KEY($COLUMN_REPORTE_CATEGORIA_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CATEGORIA_ID) ON DELETE RESTRICT,
                    UNIQUE($COLUMN_REPORTE_SEMANA_INICIO, $COLUMN_REPORTE_CATEGORIA_ID) ON CONFLICT IGNORE
                )
            """.trimIndent()

            db.execSQL(createTableReportes)
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_reportes_semana ON $TABLE_REPORTES($COLUMN_REPORTE_SEMANA_INICIO)")
        }
    }
}
