package com.techmarket.app

data class Producto(
    val nombre: String,
    val precio: String,
    val calificacion: String,
    val imagenResId: Int,
    val descripcion: String = "Producto de excelente calidad, diseñado para ofrecer una gran experiencia de uso.",
    val activo: Boolean = true,
    val publicado: Boolean = true,
    val idProducto: Long? = null,
    val imagenUri: String? = null,
    val categoria: String = ""
)