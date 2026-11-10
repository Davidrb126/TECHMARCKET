package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import com.techmarket.app.DireccionEntrega

/**
 * DAO encargado de administrar las direcciones
 * de entrega de los usuarios de TechMarket.
 */
class DireccionDAO(context: Context) {

    private val dbLectura =
        DatabaseProvider.obtenerLectura(context)

    private val dbEscritura =
        DatabaseProvider.obtenerEscritura(context)

    // ==========================================
    // GUARDAR DIRECCIÓN
    // ==========================================

    fun guardarDireccion(
        documentoUsuario: String,
        direccion: DireccionEntrega
    ): Long {

        var nuevoId = -1L

        dbEscritura.beginTransaction()

        try {

            val cursor = dbEscritura.rawQuery(
                """
                SELECT COUNT(*) FROM ${DatabaseContract.Direcciones.TABLA}
                WHERE ${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?
                """.trimIndent(),
                arrayOf(documentoUsuario)
            )

            val esPrimeraDireccion = cursor.use {
                it.moveToFirst() && it.getInt(0) == 0
            }

            val establecerPredeterminada =
                direccion.predeterminada || esPrimeraDireccion

            if (establecerPredeterminada) {
                dbEscritura.execSQL(
                    """
                    UPDATE ${DatabaseContract.Direcciones.TABLA}
                    SET ${DatabaseContract.Direcciones.PREDETERMINADA} = 0
                    WHERE ${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?
                    """.trimIndent(),
                    arrayOf(documentoUsuario)
                )
            }

            val valores = ContentValues().apply {
                put(DatabaseContract.Direcciones.DOCUMENTO_USUARIO, documentoUsuario)
                put(DatabaseContract.Direcciones.ALIAS, direccion.alias.trim())
                put(DatabaseContract.Direcciones.NOMBRE_DESTINATARIO, direccion.nombre.trim())
                put(DatabaseContract.Direcciones.TELEFONO, direccion.telefono.trim())
                put(DatabaseContract.Direcciones.DEPARTAMENTO, direccion.departamento.trim())
                put(DatabaseContract.Direcciones.CIUDAD, direccion.ciudad.trim())
                put(DatabaseContract.Direcciones.DIRECCION_COMPLETA, direccion.direccion.trim())
                put(
                    DatabaseContract.Direcciones.INFORMACION_ADICIONAL,
                    direccion.informacionAdicional.trim()
                )
                put(
                    DatabaseContract.Direcciones.PREDETERMINADA,
                    if (establecerPredeterminada) 1 else 0
                )
            }

            nuevoId = dbEscritura.insertOrThrow(
                DatabaseContract.Direcciones.TABLA,
                null,
                valores
            )

            dbEscritura.setTransactionSuccessful()

        } finally {
            dbEscritura.endTransaction()
        }

        return nuevoId
    }

    // ==========================================
    // CONSULTAR DIRECCIONES DE UN USUARIO
    // ==========================================

    fun obtenerDirecciones(
        documentoUsuario: String
    ): List<DireccionEntrega> {

        val direcciones = mutableListOf<DireccionEntrega>()

        val cursor = dbLectura.query(
            DatabaseContract.Direcciones.TABLA,
            null,
            "${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?",
            arrayOf(documentoUsuario),
            null,
            null,
            "${DatabaseContract.Direcciones.PREDETERMINADA} DESC, " +
                    "${DatabaseContract.Direcciones.ID} DESC"
        )

        cursor.use {

            while (it.moveToNext()) {

                val direccion = DireccionEntrega(
                    id = it.getLong(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.ID
                        )
                    ),
                    alias = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.ALIAS
                        )
                    ),
                    nombre = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.NOMBRE_DESTINATARIO
                        )
                    ),
                    telefono = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.TELEFONO
                        )
                    ),
                    departamento = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.DEPARTAMENTO
                        )
                    ),
                    ciudad = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.CIUDAD
                        )
                    ),
                    direccion = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.DIRECCION_COMPLETA
                        )
                    ),
                    informacionAdicional = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.INFORMACION_ADICIONAL
                        )
                    ) ?: "",
                    predeterminada = it.getInt(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Direcciones.PREDETERMINADA
                        )
                    ) == 1
                )

                direcciones.add(direccion)
            }
        }

        return direcciones
    }

    // ==========================================
    // ACTUALIZAR DIRECCIÓN
    // ==========================================

    fun actualizarDireccion(
        documentoUsuario: String,
        direccion: DireccionEntrega
    ): Boolean {

        dbEscritura.beginTransaction()

        try {

            if (direccion.predeterminada) {
                dbEscritura.execSQL(
                    """
                    UPDATE ${DatabaseContract.Direcciones.TABLA}
                    SET ${DatabaseContract.Direcciones.PREDETERMINADA} = 0
                    WHERE ${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?
                    """.trimIndent(),
                    arrayOf(documentoUsuario)
                )
            }

            val valores = ContentValues().apply {
                put(DatabaseContract.Direcciones.ALIAS, direccion.alias.trim())
                put(DatabaseContract.Direcciones.NOMBRE_DESTINATARIO, direccion.nombre.trim())
                put(DatabaseContract.Direcciones.TELEFONO, direccion.telefono.trim())
                put(DatabaseContract.Direcciones.DEPARTAMENTO, direccion.departamento.trim())
                put(DatabaseContract.Direcciones.CIUDAD, direccion.ciudad.trim())
                put(DatabaseContract.Direcciones.DIRECCION_COMPLETA, direccion.direccion.trim())
                put(
                    DatabaseContract.Direcciones.INFORMACION_ADICIONAL,
                    direccion.informacionAdicional.trim()
                )
                put(
                    DatabaseContract.Direcciones.PREDETERMINADA,
                    if (direccion.predeterminada) 1 else 0
                )
            }

            val resultado = dbEscritura.update(
                DatabaseContract.Direcciones.TABLA,
                valores,
                "${DatabaseContract.Direcciones.ID} = ? AND " +
                        "${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?",
                arrayOf(
                    direccion.id.toString(),
                    documentoUsuario
                )
            )

            dbEscritura.setTransactionSuccessful()

            return resultado > 0

        } finally {
            dbEscritura.endTransaction()
        }
    }

    // ==========================================
    // ESTABLECER DIRECCIÓN PREDETERMINADA
    // ==========================================

    fun establecerDireccionPredeterminada(
        documentoUsuario: String,
        idDireccion: Long
    ): Boolean {

        dbEscritura.beginTransaction()

        try {

            dbEscritura.execSQL(
                """
                UPDATE ${DatabaseContract.Direcciones.TABLA}
                SET ${DatabaseContract.Direcciones.PREDETERMINADA} = 0
                WHERE ${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?
                """.trimIndent(),
                arrayOf(documentoUsuario)
            )

            val valores = ContentValues().apply {
                put(DatabaseContract.Direcciones.PREDETERMINADA, 1)
            }

            val resultado = dbEscritura.update(
                DatabaseContract.Direcciones.TABLA,
                valores,
                "${DatabaseContract.Direcciones.ID} = ? AND " +
                        "${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?",
                arrayOf(
                    idDireccion.toString(),
                    documentoUsuario
                )
            )

            if (resultado > 0) {
                dbEscritura.setTransactionSuccessful()
            }

            return resultado > 0

        } finally {
            dbEscritura.endTransaction()
        }
    }

    // ==========================================
    // ELIMINAR DIRECCIÓN
    // ==========================================

    fun eliminarDireccion(
        documentoUsuario: String,
        idDireccion: Long
    ): Boolean {

        dbEscritura.beginTransaction()

        try {

            val cursor = dbEscritura.query(
                DatabaseContract.Direcciones.TABLA,
                arrayOf(DatabaseContract.Direcciones.PREDETERMINADA),
                "${DatabaseContract.Direcciones.ID} = ? AND " +
                        "${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?",
                arrayOf(
                    idDireccion.toString(),
                    documentoUsuario
                ),
                null,
                null,
                null
            )

            val eraPredeterminada = cursor.use {
                it.moveToFirst() && it.getInt(0) == 1
            }

            val resultado = dbEscritura.delete(
                DatabaseContract.Direcciones.TABLA,
                "${DatabaseContract.Direcciones.ID} = ? AND " +
                        "${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?",
                arrayOf(
                    idDireccion.toString(),
                    documentoUsuario
                )
            )

            if (resultado > 0 && eraPredeterminada) {

                dbEscritura.execSQL(
                    """
                    UPDATE ${DatabaseContract.Direcciones.TABLA}
                    SET ${DatabaseContract.Direcciones.PREDETERMINADA} = 1
                    WHERE ${DatabaseContract.Direcciones.ID} = (
                        SELECT ${DatabaseContract.Direcciones.ID}
                        FROM ${DatabaseContract.Direcciones.TABLA}
                        WHERE ${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?
                        ORDER BY ${DatabaseContract.Direcciones.ID} DESC
                        LIMIT 1
                    )
                    AND ${DatabaseContract.Direcciones.DOCUMENTO_USUARIO} = ?
                    """.trimIndent(),
                    arrayOf(documentoUsuario, documentoUsuario)
                )
            }

            dbEscritura.setTransactionSuccessful()

            return resultado > 0

        } finally {
            dbEscritura.endTransaction()
        }
    }
}