package com.senati.mitiemposemanal.jponce.model

data class SemanaReporte(
    val semanaInicio: String,
    val semanaFin: String,
    val etiqueta: String
) {
    override fun toString(): String = etiqueta
}
