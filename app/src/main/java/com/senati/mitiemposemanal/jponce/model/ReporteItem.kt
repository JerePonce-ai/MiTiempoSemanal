package com.senati.mitiemposemanal.jponce.model

data class ReporteItem(
    val categoriaId: Int,
    val nombre: String,
    val segundos: Long,
    val porcentaje: Double
)
