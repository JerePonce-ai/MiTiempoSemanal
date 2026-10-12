package com.senati.mitiemposemanal.jponce.model

import java.time.Duration

data class CategoriaEstadistica(
    val categoriaNombre: String,
    val duracionTotal: Duration,
    val porcentaje: Float
)
