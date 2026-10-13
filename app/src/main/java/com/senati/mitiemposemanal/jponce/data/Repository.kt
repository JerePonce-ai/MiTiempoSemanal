@file:Suppress("NewApi")

package com.senati.mitiemposemanal.jponce.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.senati.mitiemposemanal.jponce.model.Categoria
import com.senati.mitiemposemanal.jponce.model.Evento
import com.senati.mitiemposemanal.jponce.model.ReporteItem
import com.senati.mitiemposemanal.jponce.model.ResumenCategoria
import com.senati.mitiemposemanal.jponce.model.SemanaReporte
import com.senati.mitiemposemanal.jponce.model.Tarea
import com.senati.mitiemposemanal.jponce.util.SemanaUtil
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
        val cantidadTareas = obtenerCantidadTareasPorCategoria(id)
        if (cantidadTareas > 0) {
            return Pair(false, "Esta categoría tiene tareas asociadas. Elimina o reasigna esas tareas primero.")
        }

        val db = dbHelper.writableDatabase
        val cursorRep = db.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTES} WHERE ${DatabaseHelper.COLUMN_REPORTE_CATEGORIA_ID} = ?",
            arrayOf(id.toString())
        )
        var tieneReportes = false
        cursorRep.use {
            if (it.moveToFirst() && it.getInt(0) > 0) {
                tieneReportes = true
            }
        }
        if (tieneReportes) {
            return Pair(false, "Esta categoría se encuentra registrada en reportes semanales anteriores. No se puede eliminar.")
        }

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
            Pair(false, "Esta categoría se encuentra registrada en reportes semanales anteriores. No se puede eliminar.")
        } catch (e: Exception) {
            Pair(false, "No se pudo eliminar la categoría")
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

    fun buscarTareas(texto: String): List<Tarea> {
        val textoClean = texto.trim()
        if (textoClean.length < 2) return emptyList()

        val textoEscaped = textoClean
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

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
            WHERE t.${DatabaseHelper.COLUMN_TAREA_NOMBRE} LIKE '%' || ? || '%' ESCAPE '\'
            ORDER BY CASE WHEN LOWER(t.${DatabaseHelper.COLUMN_TAREA_NOMBRE}) LIKE LOWER(?) || '%' ESCAPE '\' THEN 0 ELSE 1 END,
                     t.${DatabaseHelper.COLUMN_TAREA_NOMBRE} ASC
            LIMIT 8
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(textoEscaped, textoEscaped))
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

    fun obtenerTareaPorNombre(nombre: String): Tarea? {
        val nombreClean = nombre.trim()
        if (nombreClean.isEmpty()) return null

        val db = dbHelper.readableDatabase
        val query = """
            SELECT t.${DatabaseHelper.COLUMN_TAREA_ID}, 
                   t.${DatabaseHelper.COLUMN_TAREA_NOMBRE}, 
                   t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID}, 
                   c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} AS categoria_nombre
            FROM ${DatabaseHelper.TABLE_TAREAS} t
            JOIN ${DatabaseHelper.TABLE_CATEGORIAS} c 
            ON t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID} = c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
            WHERE LOWER(t.${DatabaseHelper.COLUMN_TAREA_NOMBRE}) = LOWER(?)
            LIMIT 1
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(nombreClean))
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TAREA_ID))
                val nombreStr = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TAREA_NOMBRE))
                val categoriaId = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID))
                val categoriaNombre = it.getString(it.getColumnIndexOrThrow("categoria_nombre")) ?: ""
                return Tarea(id, nombreStr, categoriaId, categoriaNombre)
            }
        }
        return null
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

    fun obtenerCantidadEventosPorTarea(tareaId: Int): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_EVENTOS} WHERE ${DatabaseHelper.COLUMN_EVENTO_TAREA_ID} = ?",
            arrayOf(tareaId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    fun eliminarTarea(id: Int): Pair<Boolean, String> {
        val cantidadEventos = obtenerCantidadEventosPorTarea(id)
        if (cantidadEventos > 0) {
            return Pair(false, "No se puede eliminar: esta tarea tiene $cantidadEventos evento(s) registrado(s)")
        }

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
            Pair(false, "No se puede eliminar: esta tarea tiene eventos asociados")
        }
    }

    // ==================== EVENTOS ====================

    fun obtenerEventoEnCurso(): Evento? {
        val db = dbHelper.readableDatabase
        val query = """
            SELECT e.${DatabaseHelper.COLUMN_EVENTO_ID}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_TAREA_ID}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_INICIO}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_FIN}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS}, 
                   t.${DatabaseHelper.COLUMN_TAREA_NOMBRE} AS tarea_nombre, 
                   c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} AS categoria_nombre
            FROM ${DatabaseHelper.TABLE_EVENTOS} e
            JOIN ${DatabaseHelper.TABLE_TAREAS} t ON e.${DatabaseHelper.COLUMN_EVENTO_TAREA_ID} = t.${DatabaseHelper.COLUMN_TAREA_ID}
            JOIN ${DatabaseHelper.TABLE_CATEGORIAS} c ON t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID} = c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVENTO_FIN} IS NULL
            LIMIT 1
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_ID))
                val tareaId = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_TAREA_ID))
                val inicioStr = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_INICIO))
                val tareaNombre = it.getString(it.getColumnIndexOrThrow("tarea_nombre")) ?: ""
                val categoriaNombre = it.getString(it.getColumnIndexOrThrow("categoria_nombre")) ?: ""

                val inicio = try { LocalDateTime.parse(inicioStr) } catch (e: Exception) { LocalDateTime.now() }

                return Evento(
                    id = id,
                    tareaId = tareaId,
                    tareaNombre = tareaNombre,
                    categoriaNombre = categoriaNombre,
                    inicio = inicio,
                    fin = null,
                    duracion = null
                )
            }
        }
        return null
    }

    fun iniciarEvento(tareaId: Int): Pair<Boolean, String> {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try {
            val activo = obtenerEventoEnCurso()
            if (activo != null) {
                Pair(false, "Ya hay un evento en curso: ${activo.tareaNombre}")
            } else {
                val ahora = LocalDateTime.now().toString()
                val values = ContentValues().apply {
                    put(DatabaseHelper.COLUMN_EVENTO_TAREA_ID, tareaId)
                    put(DatabaseHelper.COLUMN_EVENTO_INICIO, ahora)
                    putNull(DatabaseHelper.COLUMN_EVENTO_FIN)
                    putNull(DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS)
                }

                val id = db.insert(DatabaseHelper.TABLE_EVENTOS, null, values)
                if (id != -1L) {
                    db.setTransactionSuccessful()
                    Pair(true, "Evento iniciado correctamente")
                } else {
                    Pair(false, "No se pudo iniciar el evento")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Error al iniciar el evento: ${e.localizedMessage}")
        } finally {
            db.endTransaction()
        }
    }

    fun iniciarEventoConTareaNueva(nombreTarea: String, categoriaId: Int): Pair<Boolean, String> {
        val nombreClean = nombreTarea.trim()
        if (nombreClean.isEmpty()) {
            return Pair(false, "El nombre de la tarea no puede estar vacío")
        }
        if (categoriaId <= 0) {
            return Pair(false, "Debes seleccionar una categoría válida")
        }

        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try {
            val activo = obtenerEventoEnCurso()
            if (activo != null) {
                Pair(false, "Ya hay un evento en curso: ${activo.tareaNombre}")
            } else {
                val valuesTarea = ContentValues().apply {
                    put(DatabaseHelper.COLUMN_TAREA_NOMBRE, nombreClean)
                    put(DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID, categoriaId)
                }
                val newTaskId = db.insert(DatabaseHelper.TABLE_TAREAS, null, valuesTarea)
                if (newTaskId == -1L) {
                    Pair(false, "Ya existe una tarea con el nombre '$nombreClean'")
                } else {
                    val ahora = LocalDateTime.now().toString()
                    val valuesEvento = ContentValues().apply {
                        put(DatabaseHelper.COLUMN_EVENTO_TAREA_ID, newTaskId.toInt())
                        put(DatabaseHelper.COLUMN_EVENTO_INICIO, ahora)
                        putNull(DatabaseHelper.COLUMN_EVENTO_FIN)
                        putNull(DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS)
                    }

                    val newEventId = db.insert(DatabaseHelper.TABLE_EVENTOS, null, valuesEvento)
                    if (newEventId != -1L) {
                        db.setTransactionSuccessful()
                        Pair(true, "Tarea y evento iniciados correctamente")
                    } else {
                        Pair(false, "Error al iniciar el evento para la nueva tarea")
                    }
                }
            }
        } catch (e: Exception) {
            Pair(false, "Error al crear la tarea e iniciar el evento")
        } finally {
            db.endTransaction()
        }
    }

    fun finalizarEvento(): Pair<Boolean, String> {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try {
            val activo = obtenerEventoEnCurso()
            if (activo == null) {
                Pair(false, "No hay ningún evento en curso")
            } else {
                val ahora = LocalDateTime.now()
                val duracion = Duration.between(activo.inicio, ahora)
                val duracionSegundos = maxOf(0L, duracion.seconds)

                val values = ContentValues().apply {
                    put(DatabaseHelper.COLUMN_EVENTO_FIN, ahora.toString())
                    put(DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS, duracionSegundos)
                }

                val rows = db.update(
                    DatabaseHelper.TABLE_EVENTOS,
                    values,
                    "${DatabaseHelper.COLUMN_EVENTO_ID} = ?",
                    arrayOf(activo.id.toString())
                )

                if (rows > 0) {
                    db.setTransactionSuccessful()
                    val formatoDuracion = formatearDuracionLegible(duracionSegundos)
                    verificarYGenerarCierresSemanales()
                    Pair(true, "Registraste $formatoDuracion en ${activo.tareaNombre}")
                } else {
                    Pair(false, "No se pudo finalizar el evento")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Error al finalizar el evento: ${e.localizedMessage}")
        } finally {
            db.endTransaction()
        }
    }

    fun obtenerEventos(): List<Evento> {
        val lista = mutableListOf<Evento>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT e.${DatabaseHelper.COLUMN_EVENTO_ID}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_TAREA_ID}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_INICIO}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_FIN}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS}, 
                   t.${DatabaseHelper.COLUMN_TAREA_NOMBRE} AS tarea_nombre, 
                   c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} AS categoria_nombre
            FROM ${DatabaseHelper.TABLE_EVENTOS} e
            JOIN ${DatabaseHelper.TABLE_TAREAS} t ON e.${DatabaseHelper.COLUMN_EVENTO_TAREA_ID} = t.${DatabaseHelper.COLUMN_TAREA_ID}
            JOIN ${DatabaseHelper.TABLE_CATEGORIAS} c ON t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID} = c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVENTO_FIN} IS NOT NULL
            ORDER BY e.${DatabaseHelper.COLUMN_EVENTO_INICIO} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_ID)
            val tareaIdIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_TAREA_ID)
            val inicioIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_INICIO)
            val finIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_FIN)
            val duracionIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS)
            val tareaNomIdx = it.getColumnIndexOrThrow("tarea_nombre")
            val catNomIdx = it.getColumnIndexOrThrow("categoria_nombre")

            while (it.moveToNext()) {
                val id = it.getInt(idIdx)
                val tareaId = it.getInt(tareaIdIdx)
                val inicioStr = it.getString(inicioIdx)
                val finStr = it.getString(finIdx)
                val duracionSeg = it.getLong(duracionIdx)
                val tareaNombre = it.getString(tareaNomIdx) ?: ""
                val categoriaNombre = it.getString(catNomIdx) ?: ""

                val inicio = try { LocalDateTime.parse(inicioStr) } catch (e: Exception) { LocalDateTime.now() }
                val fin = if (finStr != null) try { LocalDateTime.parse(finStr) } catch (e: Exception) { null } else null
                val duracion = Duration.ofSeconds(duracionSeg)

                lista.add(
                    Evento(
                        id = id,
                        tareaId = tareaId,
                        tareaNombre = tareaNombre,
                        categoriaNombre = categoriaNombre,
                        inicio = inicio,
                        fin = fin,
                        duracion = duracion
                    )
                )
            }
        }
        return lista
    }

    fun obtenerCantidadEventos(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_EVENTOS} WHERE ${DatabaseHelper.COLUMN_EVENTO_FIN} IS NOT NULL", null)
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    // ==================== RESUMEN SEMANAL (SPRINT 3) ====================

    fun obtenerResumenSemanal(desde: String, hasta: String): List<ResumenCategoria> {
        val lista = mutableListOf<ResumenCategoria>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT c.${DatabaseHelper.COLUMN_CATEGORIA_ID} AS cat_id,
                   c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} AS cat_nombre,
                   COALESCE(SUM(e.${DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS}), 0) AS total_seg
            FROM ${DatabaseHelper.TABLE_CATEGORIAS} c
            LEFT JOIN ${DatabaseHelper.TABLE_TAREAS} t ON t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID} = c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
            LEFT JOIN ${DatabaseHelper.TABLE_EVENTOS} e ON e.${DatabaseHelper.COLUMN_EVENTO_TAREA_ID} = t.${DatabaseHelper.COLUMN_TAREA_ID}
                 AND e.${DatabaseHelper.COLUMN_EVENTO_FIN} IS NOT NULL
                 AND e.${DatabaseHelper.COLUMN_EVENTO_INICIO} >= ?
                 AND e.${DatabaseHelper.COLUMN_EVENTO_INICIO} < ?
            GROUP BY c.${DatabaseHelper.COLUMN_CATEGORIA_ID}, c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE}
            ORDER BY c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} COLLATE NOCASE ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(desde, hasta))
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("cat_id")
            val nomIdx = it.getColumnIndexOrThrow("cat_nombre")
            val segIdx = it.getColumnIndexOrThrow("total_seg")

            while (it.moveToNext()) {
                val catId = it.getInt(idIdx)
                val catNombre = it.getString(nomIdx) ?: ""
                val totalSeg = it.getLong(segIdx)

                lista.add(
                    ResumenCategoria(
                        categoriaId = catId,
                        nombre = catNombre,
                        segundos = totalSeg
                    )
                )
            }
        }
        return lista
    }

    // ==================== CIERRE SEMANAL AUTOMÁTICO (HU-07 SPRINT 4) ====================

    fun verificarYGenerarCierresSemanales(): Int {
        val hoy = LocalDate.now()
        val lunesActual = SemanaUtil.inicioDeSemana(hoy)
        val db = dbHelper.writableDatabase

        val queryMin = """
            SELECT MIN(${DatabaseHelper.COLUMN_EVENTO_INICIO}) 
            FROM ${DatabaseHelper.TABLE_EVENTOS} 
            WHERE ${DatabaseHelper.COLUMN_EVENTO_FIN} IS NOT NULL 
              AND ${DatabaseHelper.COLUMN_EVENTO_INICIO} < ?
        """.trimIndent()

        var minInicioStr: String? = null
        val cursorMin = db.rawQuery(queryMin, arrayOf(SemanaUtil.desdeISO(lunesActual)))
        cursorMin.use {
            if (it.moveToFirst() && !it.isNull(0)) {
                minInicioStr = it.getString(0)
            }
        }

        if (minInicioStr.isNullOrEmpty()) return 0

        val primerFechaEvento = try {
            LocalDateTime.parse(minInicioStr).toLocalDate()
        } catch (e: Exception) {
            return 0
        }

        var lunesEvaluado = SemanaUtil.inicioDeSemana(primerFechaEvento)
        var cierresGenerados = 0

        while (lunesEvaluado.isBefore(lunesActual)) {
            val desdeIso = SemanaUtil.desdeISO(lunesEvaluado)
            val hastaIso = SemanaUtil.hastaISO(lunesEvaluado)
            val semanaInicioStr = lunesEvaluado.toString() // yyyy-MM-dd
            val semanaFinStr = lunesEvaluado.plusDays(6).toString() // yyyy-MM-dd

            val queryExiste = """
                SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_REPORTES}
                WHERE ${DatabaseHelper.COLUMN_REPORTE_SEMANA_INICIO} = ?
            """.trimIndent()

            var existeReporte = false
            val cursorExiste = db.rawQuery(queryExiste, arrayOf(semanaInicioStr))
            cursorExiste.use {
                if (it.moveToFirst() && it.getInt(0) > 0) {
                    existeReporte = true
                }
            }

            if (!existeReporte) {
                // AJUSTE PENDIENTE 1: No generar el reporte si hay un evento en curso que empezó en esa semana
                val queryEventoEnCursoEnSemana = """
                    SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_EVENTOS}
                    WHERE ${DatabaseHelper.COLUMN_EVENTO_FIN} IS NULL
                      AND ${DatabaseHelper.COLUMN_EVENTO_INICIO} >= ?
                      AND ${DatabaseHelper.COLUMN_EVENTO_INICIO} < ?
                """.trimIndent()

                var hayEventoEnCurso = false
                val cursorEnCurso = db.rawQuery(queryEventoEnCursoEnSemana, arrayOf(desdeIso, hastaIso))
                cursorEnCurso.use {
                    if (it.moveToFirst() && it.getInt(0) > 0) {
                        hayEventoEnCurso = true
                    }
                }

                val queryEventos = """
                    SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_EVENTOS}
                    WHERE ${DatabaseHelper.COLUMN_EVENTO_FIN} IS NOT NULL
                      AND ${DatabaseHelper.COLUMN_EVENTO_INICIO} >= ?
                      AND ${DatabaseHelper.COLUMN_EVENTO_INICIO} < ?
                """.trimIndent()

                var hayEventos = false
                val cursorEventos = db.rawQuery(queryEventos, arrayOf(desdeIso, hastaIso))
                cursorEventos.use {
                    if (it.moveToFirst() && it.getInt(0) > 0) {
                        hayEventos = true
                    }
                }

                if (hayEventos && !hayEventoEnCurso) {
                    db.beginTransaction()
                    try {
                        val queryTotales = """
                            SELECT c.${DatabaseHelper.COLUMN_CATEGORIA_ID} AS cat_id,
                                   COALESCE(SUM(e.${DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS}), 0) AS total_seg
                            FROM ${DatabaseHelper.TABLE_CATEGORIAS} c
                            LEFT JOIN ${DatabaseHelper.TABLE_TAREAS} t ON t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID} = c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
                            LEFT JOIN ${DatabaseHelper.TABLE_EVENTOS} e ON e.${DatabaseHelper.COLUMN_EVENTO_TAREA_ID} = t.${DatabaseHelper.COLUMN_TAREA_ID}
                                 AND e.${DatabaseHelper.COLUMN_EVENTO_FIN} IS NOT NULL
                                 AND e.${DatabaseHelper.COLUMN_EVENTO_INICIO} >= ?
                                 AND e.${DatabaseHelper.COLUMN_EVENTO_INICIO} < ?
                            GROUP BY c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
                        """.trimIndent()

                        val cursorTotales = db.rawQuery(queryTotales, arrayOf(desdeIso, hastaIso))
                        cursorTotales.use {
                            val catIdIdx = it.getColumnIndexOrThrow("cat_id")
                            val segIdx = it.getColumnIndexOrThrow("total_seg")

                            while (it.moveToNext()) {
                                val catId = it.getInt(catIdIdx)
                                val seg = it.getLong(segIdx)
                                val pct = SemanaUtil.porcentaje(seg)

                                val values = ContentValues().apply {
                                    put(DatabaseHelper.COLUMN_REPORTE_SEMANA_INICIO, semanaInicioStr)
                                    put(DatabaseHelper.COLUMN_REPORTE_SEMANA_FIN, semanaFinStr)
                                    put(DatabaseHelper.COLUMN_REPORTE_CATEGORIA_ID, catId)
                                    put(DatabaseHelper.COLUMN_REPORTE_DURACION_SEGUNDOS, seg)
                                    put(DatabaseHelper.COLUMN_REPORTE_PORCENTAJE, pct)
                                }
                                db.insertWithOnConflict(
                                    DatabaseHelper.TABLE_REPORTES,
                                    null,
                                    values,
                                    SQLiteDatabase.CONFLICT_IGNORE
                                )
                            }
                        }
                        db.setTransactionSuccessful()
                        cierresGenerados++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        db.endTransaction()
                    }
                }
            }

            lunesEvaluado = lunesEvaluado.plusWeeks(1)
        }

        return cierresGenerados
    }

    // ==================== REPORTES (HU-08 SPRINT 4) ====================

    fun obtenerSemanasConReporte(): List<SemanaReporte> {
        val lista = mutableListOf<SemanaReporte>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT DISTINCT ${DatabaseHelper.COLUMN_REPORTE_SEMANA_INICIO}, ${DatabaseHelper.COLUMN_REPORTE_SEMANA_FIN}
            FROM ${DatabaseHelper.TABLE_REPORTES}
            ORDER BY ${DatabaseHelper.COLUMN_REPORTE_SEMANA_INICIO} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        cursor.use {
            val inicioIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORTE_SEMANA_INICIO)
            val finIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPORTE_SEMANA_FIN)

            while (it.moveToNext()) {
                val inicioStr = it.getString(inicioIdx) ?: ""
                val finStr = it.getString(finIdx) ?: ""

                val inicioDate = try { LocalDate.parse(inicioStr) } catch (e: Exception) { null }
                val finDate = try { LocalDate.parse(finStr) } catch (e: Exception) { null }

                val etiqueta = if (inicioDate != null && finDate != null) {
                    "${inicioDate.format(dateFormatter)} – ${finDate.format(dateFormatter)}"
                } else {
                    "$inicioStr – $finStr"
                }

                lista.add(
                    SemanaReporte(
                        semanaInicio = inicioStr,
                        semanaFin = finStr,
                        etiqueta = etiqueta
                    )
                )
            }
        }
        return lista
    }

    fun obtenerDetalleReporte(semanaInicio: String): List<ReporteItem> {
        val lista = mutableListOf<ReporteItem>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT r.${DatabaseHelper.COLUMN_REPORTE_CATEGORIA_ID} AS cat_id,
                   c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} AS cat_nombre,
                   r.${DatabaseHelper.COLUMN_REPORTE_DURACION_SEGUNDOS} AS total_seg,
                   r.${DatabaseHelper.COLUMN_REPORTE_PORCENTAJE} AS pct
            FROM ${DatabaseHelper.TABLE_REPORTES} r
            JOIN ${DatabaseHelper.TABLE_CATEGORIAS} c ON r.${DatabaseHelper.COLUMN_REPORTE_CATEGORIA_ID} = c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
            WHERE r.${DatabaseHelper.COLUMN_REPORTE_SEMANA_INICIO} = ?
            ORDER BY c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} COLLATE NOCASE ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(semanaInicio))
        cursor.use {
            val catIdIdx = it.getColumnIndexOrThrow("cat_id")
            val nomIdx = it.getColumnIndexOrThrow("cat_nombre")
            val segIdx = it.getColumnIndexOrThrow("total_seg")
            val pctIdx = it.getColumnIndexOrThrow("pct")

            while (it.moveToNext()) {
                val catId = it.getInt(catIdIdx)
                val catNombre = it.getString(nomIdx) ?: ""
                val seg = it.getLong(segIdx)
                val pct = it.getDouble(pctIdx)

                lista.add(
                    ReporteItem(
                        categoriaId = catId,
                        nombre = catNombre,
                        segundos = seg,
                        porcentaje = pct
                    )
                )
            }
        }
        return lista
    }

    fun obtenerEventosDeSemana(semanaInicio: String): List<Evento> {
        val lunes = try { LocalDate.parse(semanaInicio) } catch (e: Exception) { return emptyList() }
        val desdeIso = SemanaUtil.desdeISO(lunes)
        val hastaIso = SemanaUtil.hastaISO(lunes)

        val lista = mutableListOf<Evento>()
        val db = dbHelper.readableDatabase

        val query = """
            SELECT e.${DatabaseHelper.COLUMN_EVENTO_ID}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_TAREA_ID}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_INICIO}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_FIN}, 
                   e.${DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS}, 
                   t.${DatabaseHelper.COLUMN_TAREA_NOMBRE} AS tarea_nombre, 
                   c.${DatabaseHelper.COLUMN_CATEGORIA_NOMBRE} AS categoria_nombre
            FROM ${DatabaseHelper.TABLE_EVENTOS} e
            JOIN ${DatabaseHelper.TABLE_TAREAS} t ON e.${DatabaseHelper.COLUMN_EVENTO_TAREA_ID} = t.${DatabaseHelper.COLUMN_TAREA_ID}
            JOIN ${DatabaseHelper.TABLE_CATEGORIAS} c ON t.${DatabaseHelper.COLUMN_TAREA_CATEGORIA_ID} = c.${DatabaseHelper.COLUMN_CATEGORIA_ID}
            WHERE e.${DatabaseHelper.COLUMN_EVENTO_FIN} IS NOT NULL
              AND e.${DatabaseHelper.COLUMN_EVENTO_INICIO} >= ?
              AND e.${DatabaseHelper.COLUMN_EVENTO_INICIO} < ?
            ORDER BY e.${DatabaseHelper.COLUMN_EVENTO_INICIO} DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(desdeIso, hastaIso))
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_ID)
            val tareaIdIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_TAREA_ID)
            val inicioIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_INICIO)
            val finIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_FIN)
            val duracionIdx = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENTO_DURACION_SEGUNDOS)
            val tareaNomIdx = it.getColumnIndexOrThrow("tarea_nombre")
            val catNomIdx = it.getColumnIndexOrThrow("categoria_nombre")

            while (it.moveToNext()) {
                val id = it.getInt(idIdx)
                val tareaId = it.getInt(tareaIdIdx)
                val inicioStr = it.getString(inicioIdx)
                val finStr = it.getString(finIdx)
                val duracionSeg = it.getLong(duracionIdx)
                val tareaNombre = it.getString(tareaNomIdx) ?: ""
                val categoriaNombre = it.getString(catNomIdx) ?: ""

                val inicio = try { LocalDateTime.parse(inicioStr) } catch (e: Exception) { LocalDateTime.now() }
                val fin = if (finStr != null) try { LocalDateTime.parse(finStr) } catch (e: Exception) { null } else null
                val duracion = Duration.ofSeconds(duracionSeg)

                lista.add(
                    Evento(
                        id = id,
                        tareaId = tareaId,
                        tareaNombre = tareaNombre,
                        categoriaNombre = categoriaNombre,
                        inicio = inicio,
                        fin = fin,
                        duracion = duracion
                    )
                )
            }
        }
        return lista
    }

    fun obtenerCantidadReportes(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(DISTINCT ${DatabaseHelper.COLUMN_REPORTE_SEMANA_INICIO}) FROM ${DatabaseHelper.TABLE_REPORTES}", null)
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
    }

    companion object {
        fun formatearDuracionLegible(duracionSegundos: Long): String {
            val horas = duracionSegundos / 3600
            val minutos = (duracionSegundos % 3600) / 60
            val segundos = duracionSegundos % 60

            return when {
                horas > 0 -> {
                    if (minutos > 0) "${horas} h ${minutos} min" else "${horas} h"
                }
                minutos > 0 -> "${minutos} min"
                else -> "${segundos} s"
            }
        }
    }
}
