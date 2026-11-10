package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException

/**
 * DAO encargado de las operaciones relacionadas
 * con los usuarios de TechMarket.
 */
class UsuarioDAO(context: Context) {

    private val dbLectura =
        DatabaseProvider.obtenerLectura(context)

    private val dbEscritura =
        DatabaseProvider.obtenerEscritura(context)

    // ==========================================
    // REGISTRAR USUARIO
    // ==========================================

    fun registrarUsuario(
        tipoDocumento: String,
        documento: String,
        nombres: String,
        apellidos: String,
        correo: String,
        passwordHash: String,
        rol: String = "CLIENTE",
        telefono: String = ""
    ): Long {

        val valores = ContentValues().apply {
            put(DatabaseContract.Usuarios.TIPO_DOCUMENTO, tipoDocumento)
            put(DatabaseContract.Usuarios.DOCUMENTO, documento)
            put(DatabaseContract.Usuarios.NOMBRES, nombres)
            put(DatabaseContract.Usuarios.APELLIDOS, apellidos)
            put(DatabaseContract.Usuarios.CORREO, correo)
            put(DatabaseContract.Usuarios.TELEFONO, telefono)
            put(DatabaseContract.Usuarios.PASSWORD_HASH, passwordHash)
            put(DatabaseContract.Usuarios.ROL, rol)
            put(DatabaseContract.Usuarios.ESTADO, 1)
        }

        return try {
            dbEscritura.insertOrThrow(
                DatabaseContract.Usuarios.TABLA,
                null,
                valores
            )
        } catch (_: SQLException) {
            -1L
        }
    }

    // ==========================================
    // CONSULTAR USUARIO POR DOCUMENTO
    // ==========================================

    fun consultarUsuario(documento: String): Cursor {

        return dbLectura.query(
            DatabaseContract.Usuarios.TABLA,
            null,
            "${DatabaseContract.Usuarios.DOCUMENTO} = ?",
            arrayOf(documento),
            null,
            null,
            null
        )
    }

    // ==========================================
    // ACTUALIZAR DATOS DEL USUARIO
    // ==========================================

    fun actualizarDatosUsuario(
        documento: String,
        nombres: String,
        apellidos: String,
        correo: String
    ): Boolean {

        val valores = ContentValues().apply {
            put(DatabaseContract.Usuarios.NOMBRES, nombres)
            put(DatabaseContract.Usuarios.APELLIDOS, apellidos)
            put(DatabaseContract.Usuarios.CORREO, correo)
        }

        return dbEscritura.update(
            DatabaseContract.Usuarios.TABLA,
            valores,
            "${DatabaseContract.Usuarios.DOCUMENTO} = ?",
            arrayOf(documento)
        ) > 0
    }

    // ==========================================
    // ACTUALIZAR CONTRASEÑA
    // ==========================================

    fun actualizarContrasena(
        documento: String,
        passwordHash: String
    ): Boolean {

        val valores = ContentValues().apply {
            put(DatabaseContract.Usuarios.PASSWORD_HASH, passwordHash)
        }

        return dbEscritura.update(
            DatabaseContract.Usuarios.TABLA,
            valores,
            "${DatabaseContract.Usuarios.DOCUMENTO} = ?",
            arrayOf(documento)
        ) > 0
    }
}
