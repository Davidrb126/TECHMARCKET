package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import com.techmarket.app.MetodoPago

/**
 * DAO encargado de administrar los métodos
 * de pago de los usuarios de TechMarket.
 */
class PagoDAO(context: Context) {

    private val dbLectura = DatabaseProvider.obtenerLectura(context)
    private val dbEscritura = DatabaseProvider.obtenerEscritura(context)

    // GUARDAR MÉTODO DE PAGO
    fun guardarMetodoPago(
        documentoUsuario: String,
        metodoPago: MetodoPago
    ): Long {

        var nuevoId = -1L

        dbEscritura.beginTransaction()

        try {

            val cursor = dbLectura.rawQuery(
                """
                SELECT COUNT(*)
                FROM ${DatabaseContract.MetodosPago.TABLA}
                WHERE ${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?
                """.trimIndent(),
                arrayOf(documentoUsuario.trim())
            )

            val esPrimerMetodo = cursor.use {
                it.moveToFirst() && it.getInt(0) == 0
            }

            val establecerPredeterminado =
                metodoPago.predeterminado || esPrimerMetodo

            if (establecerPredeterminado) {
                dbEscritura.execSQL(
                    """
                    UPDATE ${DatabaseContract.MetodosPago.TABLA}
                    SET ${DatabaseContract.MetodosPago.PREDETERMINADO} = 0
                    WHERE ${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?
                    """.trimIndent(),
                    arrayOf(documentoUsuario.trim())
                )
            }

            val valores = ContentValues().apply {
                put(
                    DatabaseContract.MetodosPago.DOCUMENTO_USUARIO,
                    documentoUsuario.trim()
                )
                put(
                    DatabaseContract.MetodosPago.TITULAR,
                    metodoPago.titular.trim()
                )
                put(
                    DatabaseContract.MetodosPago.ULTIMOS_CUATRO,
                    metodoPago.ultimosCuatro.trim()
                )
                put(
                    DatabaseContract.MetodosPago.VENCIMIENTO,
                    metodoPago.vencimiento.trim()
                )
                put(
                    DatabaseContract.MetodosPago.PREDETERMINADO,
                    if (establecerPredeterminado) 1 else 0
                )
            }

            nuevoId = dbEscritura.insertOrThrow(
                DatabaseContract.MetodosPago.TABLA,
                null,
                valores
            )

            dbEscritura.setTransactionSuccessful()

        } finally {
            dbEscritura.endTransaction()
        }

        return nuevoId
    }

    // CONSULTAR MÉTODOS DE PAGO
    fun obtenerMetodosPago(
        documentoUsuario: String
    ): List<MetodoPago> {

        val metodos = mutableListOf<MetodoPago>()

        val cursor = dbLectura.query(
            DatabaseContract.MetodosPago.TABLA,
            null,
            "${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?",
            arrayOf(documentoUsuario.trim()),
            null,
            null,
            "${DatabaseContract.MetodosPago.PREDETERMINADO} DESC, " +
                    "${DatabaseContract.MetodosPago.ID} DESC"
        )

        cursor.use {

            while (it.moveToNext()) {

                metodos.add(
                    MetodoPago(
                        id = it.getLong(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.MetodosPago.ID
                            )
                        ),
                        titular = it.getString(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.MetodosPago.TITULAR
                            )
                        ),
                        ultimosCuatro = it.getString(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.MetodosPago.ULTIMOS_CUATRO
                            )
                        ),
                        vencimiento = it.getString(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.MetodosPago.VENCIMIENTO
                            )
                        ),
                        predeterminado = it.getInt(
                            it.getColumnIndexOrThrow(
                                DatabaseContract.MetodosPago.PREDETERMINADO
                            )
                        ) == 1
                    )
                )
            }
        }

        return metodos
    }

    // ESTABLECER MÉTODO PREDETERMINADO
    fun establecerMetodoPagoPredeterminado(
        documentoUsuario: String,
        idMetodo: Long
    ): Boolean {

        dbEscritura.beginTransaction()

        try {

            dbEscritura.execSQL(
                """
                UPDATE ${DatabaseContract.MetodosPago.TABLA}
                SET ${DatabaseContract.MetodosPago.PREDETERMINADO} = 0
                WHERE ${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?
                """.trimIndent(),
                arrayOf(documentoUsuario)
            )

            val valores = ContentValues().apply {
                put(DatabaseContract.MetodosPago.PREDETERMINADO, 1)
            }

            val resultado = dbEscritura.update(
                DatabaseContract.MetodosPago.TABLA,
                valores,
                "${DatabaseContract.MetodosPago.ID} = ? AND " +
                        "${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?",
                arrayOf(idMetodo.toString(), documentoUsuario)
            )

            if (resultado > 0) {
                dbEscritura.setTransactionSuccessful()
            }

            return resultado > 0

        } finally {
            dbEscritura.endTransaction()
        }
    }

    // ELIMINAR MÉTODO DE PAGO
    fun eliminarMetodoPago(
        documentoUsuario: String,
        idMetodo: Long
    ): Boolean {

        dbEscritura.beginTransaction()

        try {

            val cursor = dbLectura.query(
                DatabaseContract.MetodosPago.TABLA,
                arrayOf(DatabaseContract.MetodosPago.PREDETERMINADO),
                "${DatabaseContract.MetodosPago.ID} = ? AND " +
                        "${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?",
                arrayOf(idMetodo.toString(), documentoUsuario),
                null,
                null,
                null
            )

            val eraPredeterminado = cursor.use {
                it.moveToFirst() && it.getInt(0) == 1
            }

            val resultado = dbEscritura.delete(
                DatabaseContract.MetodosPago.TABLA,
                "${DatabaseContract.MetodosPago.ID} = ? AND " +
                        "${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?",
                arrayOf(idMetodo.toString(), documentoUsuario)
            )

            if (resultado > 0 && eraPredeterminado) {

                dbEscritura.execSQL(
                    """
                    UPDATE ${DatabaseContract.MetodosPago.TABLA}
                    SET ${DatabaseContract.MetodosPago.PREDETERMINADO} = 1
                    WHERE ${DatabaseContract.MetodosPago.ID} = (
                        SELECT ${DatabaseContract.MetodosPago.ID}
                        FROM ${DatabaseContract.MetodosPago.TABLA}
                        WHERE ${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?
                        ORDER BY ${DatabaseContract.MetodosPago.ID} DESC
                        LIMIT 1
                    )
                    AND ${DatabaseContract.MetodosPago.DOCUMENTO_USUARIO} = ?
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