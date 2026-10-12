package com.senati.mitiemposemanal.jponce.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.senati.mitiemposemanal.jponce.model.Categoria
import com.senati.mitiemposemanal.jponce.model.Tarea

class Repository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // ==================== CATEGORÍAS ====================

    fun insertarCategoria(nombre: String): Pair<Boolean, String> {
        val nombreTrimmed = nombre.trim()
        if (nombreTrimmed.isEmpty()) {
            return Pair(false, "El nombre de la categoría no puede estar vacío")
        }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CATEGORIA_NOMBRE, nombreTrimmed)
        }
        return try {
            val id = db.insert(DatabaseHelper.TABLE_CATEGORIAS, null, values)
            if (id != -1L) {
                Pair(true, "Categoría creada exitosamente")
            } else {
                Pair(false, "Ya existe una categoría con este nombre")
            }
        } catch (e: SQLiteConstraintException) {
            Pair(false, "Ya existe una categoría con este nombre")
        }
    }

    fun obtenerCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CATEGORIAS,
            arrayOf(DatabaseHelper.COLUMN_CATEGORIA_ID, DatabaseHelper.COLUMN_CATEGORIA_NOMBRE),
            null, null, null, null,
            "${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} ASC"
        )
        cursor.use {
            val idIndex = it.getColumnIndex(DatabaseHelper.COLUMN_CATEGORIA_ID)
            val nombreIndex = it.getColumnIndex(DatabaseHelper.COLUMN_CATEGORIA_NOMBRE)
            while (it.moveToNext()) {
                val id = it.getInt(idIndex)
                val nombreStr = it.getString(nombreIndex)
                lista.add(Categoria(id, nombreStr))
            }
        }
        return lista
    }

    fun obtenerCantidadCategorias(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_CATEGORIAS}", null)
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    fun actualizarCategoria(id: Int, nombre: String): Pair<Boolean, String> {
        val nombreTrimmed = nombre.trim()
        if (nombreTrimmed.isEmpty()) {
            return Pair(false, "El nombre de la categoría no puede estar vacío")
        }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_CATEGORIA_NOMBRE, nombreTrimmed)
        }
        return try {
            val rows = db.update(
                DatabaseHelper.TABLE_CATEGORIAS,
                values,
                "${DatabaseHelper.COLUMN_CATEGORIA_ID} = ?",
                arrayOf(id.toString())
            )
            if (rows > 0) {
                Pair(true, "Categoría actualizada exitosamente")
            } else {
                Pair(false, "No se encontró la categoría a actualizar")
            }
        } catch (e: SQLiteConstraintException) {
            Pair(false, "Ya existe una categoría con este nombre")
        }
    }

    fun obtenerCantidadTareasPorCategoria(categoriaId: Int): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_TAREAS} WHERE ${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID} = ?",
            arrayOf(categoriaId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    fun eliminarCategoria(id: Int): Pair<Boolean, String> {
        val db = dbHelper.writableDatabase
        return try {
            val rows = db.delete(
                DatabaseHelper.TABLE_CATEGORIAS,
                "${DatabaseHelper.COLUMN_CATEGORIA_ID} = ?",
                arrayOf(id.toString())
            )
            if (rows > 0) {
                Pair(true, "Categoría eliminada exitosamente")
            } else {
                Pair(false, "No se pudo eliminar la categoría")
            }
        } catch (e: SQLiteConstraintException) {
            Pair(false, "Esta categoría tiene tareas asociadas. Elimina o reasigna esas tareas primero.")
        }
    }

    // ==================== TAREAS ====================

    fun insertarTarea(nombre: String, categoriaId: Int): Pair<Boolean, String> {
        val nombreTrimmed = nombre.trim()
        if (nombreTrimmed.isEmpty()) {
            return Pair(false, "El nombre de la tarea no puede estar vacío")
        }
        if (categoriaId <= 0) {
            return Pair(false, "Debes seleccionar una categoría válida")
        }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TAREA_NOMBRE, nombreTrimmed)
            put(DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID, categoriaId)
        }
        return try {
            val id = db.insert(DatabaseHelper.TABLE_TAREAS, null, values)
            if (id != -1L) {
                Pair(true, "Tarea guardada exitosamente")
            } else {
                Pair(false, "Ya existe una tarea con este nombre")
            }
        } catch (e: SQLiteConstraintException) {
            Pair(false, "Ya existe una tarea con este nombre")
        }
    }

    fun obtenerTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        val db = dbHelper.readableDatabase
        
        val query = """
            SELECT t.${DatabaseHelper.COLUMN_TAREA_ID}, 
                   t.${DatabaseHelper.COLUMN_TAREA_NOMBRE}, 
                   t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID}, 
                   c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} AS categoria_nombre
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            JOIN ${DatabaseHelper.TABLE_CATEGORIAS} c 
            ON t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID} = c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
            ORDER BY t.${DatabaseHelper.COLUMN_TAREA_NOMBRE} ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            val idIndex = it.getColumnIndex(DatabaseHelper.COLUMN_TAREA_ID)
            val nombreIndex = it.getColumnIndex(DatabaseHelper.COLUMN_TAREA_NOMBRE)
            val catIdIndex = it.getColumnIndex(DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID)
            val catNombreIndex = it.getColumnIndex("categoria_nombre")

            while (it.moveToNext()) {
                val id = it.getInt(idIndex)
                val nombreStr = it.getString(nombreIndex)
                val categoriaId = it.getInt(catIdIndex)
                val categoriaNombre = it.getString(catNombreIndex) ?: ""
                lista.add(Tarea(id, nombreStr, categoriaId, categoriaNombre))
            }
        }
        return lista
    }

    fun obtenerCantidadTareas(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_TAREAS}", null)
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    fun actualizarTarea(id: Int, nombre: String, categoriaId: Int): Pair<Boolean, String> {
        val nombreTrimmed = nombre.trim()
        if (nombreTrimmed.isEmpty()) {
            return Pair(false, "El nombre de la tarea no puede estar vacío")
        }
        if (categoriaId <= 0) {
            return Pair(false, "Debes seleccionar una categoría válida")
        }
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TAREA_NOMBRE, nombreTrimmed)
            put(DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID, categoriaId)
        }
        return try {
            val rows = db.update(
                DatabaseHelper.TABLE_TAREAS,
                values,
                "${DatabaseHelper.COLUMN_TAREA_ID} = ?",
                arrayOf(id.toString())
            )
            if (rows > 0) {
                Pair(true, "Tarea actualizada exitosamente")
            } else {
                Pair(false, "No se encontró la tarea a actualizar")
            }
        } catch (e: SQLiteConstraintException) {
            Pair(false, "Ya existe una tarea con este nombre")
        }
    }

    fun eliminarTarea(id: Int): Pair<Boolean, String> {
        val db = dbHelper.writableDatabase
        return try {
            val rows = db.delete(
                DatabaseHelper.TABLE_TAREAS,
                "${DatabaseHelper.COLUMN_TAREA_ID} = ?",
                arrayOf(id.toString())
            )
            if (rows > 0) {
                Pair(true, "Tarea eliminada exitosamente")
            } else {
                Pair(false, "No se pudo eliminar la tarea")
            }
        } catch (e: SQLiteConstraintException) {
            Pair(false, "Error al eliminar la tarea")
        }
    }
}
