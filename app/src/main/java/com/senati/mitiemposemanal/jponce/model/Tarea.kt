package com.senati.mitiemposemanal.jponce.model

data class Tarea(
    val id: Int = 0,
    val nombre: String,
    val categoriaId: Int,
    val categoriaNombre: String = ""
)
