package com.techmarket.app

data class Notificacion(

    val id: Long = 0L,

    val documentoUsuario: String = "",

    val titulo: String,

    val mensaje: String,

    val fecha: String,

    val leida: Boolean = false

)
