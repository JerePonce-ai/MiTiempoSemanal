@file:Suppress("NewApi")

package com.senati.mitiemposemanal.jponce.model

import java.time.Duration
import java.time.LocalDateTime

data class Evento(
    val id: Int,
    val tareaId: Int,
    val tareaNombre: String,
    val categoriaNombre: String,
    val inicio: LocalDateTime,
    val fin: LocalDateTime?,
    val duracion: Duration?
)
