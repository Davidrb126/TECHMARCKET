package com.techmarket.app

data class MetodoPago(
    val id: Long = 0L,
    val titular: String,
    val ultimosCuatro: String,
    val vencimiento: String,
    val predeterminado: Boolean = false
)