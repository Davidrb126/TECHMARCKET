package com.techmarket.app

data class DireccionEntrega(
    val id: Long = 0L,
    val alias: String,
    val nombre: String,
    val telefono: String,
    val departamento: String,
    val ciudad: String,
    val direccion: String,
    val informacionAdicional: String = "",
    val predeterminada: Boolean = false
)
