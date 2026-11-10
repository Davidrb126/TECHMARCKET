package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import com.techmarket.app.Notificacion

/**
 * DAO encargado de administrar las notificaciones
 * de los usuarios de TechMarket.
 */
class NotificacionDAO(context: Context) {

    private val dbLectura = DatabaseProvider.obtenerLectura(context)
    private val dbEscritura = DatabaseProvider.obtenerEscritura(context)

    // GUARDAR NOTIFICACIÓN
    fun guardarNotificacion(
        documentoUsuario: String,
        titulo: String,
        mensaje: String,
        fecha: String
    ): Long {

        if (
            documentoUsuario.isBlank() ||
            titulo.isBlank() ||
            mensaje.isBlank() ||
            fecha.isBlank()
        ) {
            return -1L
        }

        val valores = ContentValues().apply {
            put(
                DatabaseContract.Notificaciones.DOCUMENTO_USUARIO,
                documentoUsuario.trim()
            )
            put(
                DatabaseContract.Notificaciones.TITULO,
                titulo.trim()
            )
            put(
                DatabaseContract.Notificaciones.MENSAJE,
                mensaje.trim()
            )
            put(
                DatabaseContract.Notificaciones.FECHA,
                fecha.trim()
            )
            put(DatabaseContract.Notificaciones.LEIDA, 0)
        }

        return dbEscritura.insert(
            DatabaseContract.Notificaciones.TABLA,
            null,
            valores
        )
    }

    // CONSULTAR NOTIFICACIONES
    fun obtenerNotificaciones(
        documentoUsuario: String
    ): List<Notificacion> {

        val notificaciones = mutableListOf<Notificacion>()

        val cursor = dbLectura.query(
            DatabaseContract.Notificaciones.TABLA,
            null,
            "${DatabaseContract.Notificaciones.DOCUMENTO_USUARIO} = ?",
            arrayOf(documentoUsuario.trim()),
            null,
            null,
            "${DatabaseContract.Notificaciones.ID} DESC"
        )

        cursor.use {

            while (it.moveToNext()) {

                notificaciones.add(
                    Notificacion(
                        id = it.getLong(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.Notificaciones.ID
                            )
                        ),
                        titulo = it.getString(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.Notificaciones.TITULO
                            )
                        ),
                        mensaje = it.getString(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.Notificaciones.MENSAJE
                            )
                        ),
                        fecha = it.getString(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.Notificaciones.FECHA
                            )
                        ),
                        leida = it.getInt(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.Notificaciones.LEIDA
                            )
                        ) == 1
                    )
                )
            }
        }

        return notificaciones
    }

    // MARCAR UNA NOTIFICACIÓN COMO LEÍDA
    fun marcarNotificacionLeida(
        documentoUsuario: String,
        idNotificacion: Long
    ): Boolean {

        val valores = ContentValues().apply {
            put(DatabaseContract.Notificaciones.LEIDA, 1)
        }

        val resultado = dbEscritura.update(
            DatabaseContract.Notificaciones.TABLA,
            valores,
            "${DatabaseContract.Notificaciones.ID} = ? AND " +
                    "${DatabaseContract.Notificaciones.DOCUMENTO_USUARIO} = ?",
            arrayOf(
                idNotificacion.toString(),
                documentoUsuario.trim()
            )
        )

        return resultado > 0
    }

    // MARCAR TODAS LAS NOTIFICACIONES COMO LEÍDAS
    fun marcarTodasNotificacionesLeidas(
        documentoUsuario: String
    ): Int {

        val valores = ContentValues().apply {
            put(DatabaseContract.Notificaciones.LEIDA, 1)
        }

        return dbEscritura.update(
            DatabaseContract.Notificaciones.TABLA,
            valores,
            "${DatabaseContract.Notificaciones.DOCUMENTO_USUARIO} = ? AND " +
                    "${DatabaseContract.Notificaciones.LEIDA} = 0",
            arrayOf(documentoUsuario.trim())
        )
    }

    // ELIMINAR NOTIFICACIÓN
    fun eliminarNotificacion(
        documentoUsuario: String,
        idNotificacion: Long
    ): Boolean {

        val resultado = dbEscritura.delete(
            DatabaseContract.Notificaciones.TABLA,
            "${DatabaseContract.Notificaciones.ID} = ? AND " +
                    "${DatabaseContract.Notificaciones.DOCUMENTO_USUARIO} = ?",
            arrayOf(
                idNotificacion.toString(),
                documentoUsuario.trim()
            )
        )

        return resultado > 0
    }

    // CONTAR NOTIFICACIONES NO LEÍDAS
    fun contarNotificacionesNoLeidas(
        documentoUsuario: String
    ): Int {

        val cursor = dbLectura.rawQuery(
            """
            SELECT COUNT(*)
            FROM ${DatabaseContract.Notificaciones.TABLA}
            WHERE ${DatabaseContract.Notificaciones.DOCUMENTO_USUARIO} = ?
              AND ${DatabaseContract.Notificaciones.LEIDA} = 0
            """.trimIndent(),
            arrayOf(documentoUsuario.trim())
        )

        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }
}