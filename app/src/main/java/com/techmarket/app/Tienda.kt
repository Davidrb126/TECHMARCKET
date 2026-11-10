package com.techmarket.app

data class Tienda(
    val id: Long = 0L,
    val usuarioId: Long,
    val nombre: String,
    val descripcion: String,
    val logo: String = "",
    val direccion: String = "",
    val sitioWeb: String = ""
)