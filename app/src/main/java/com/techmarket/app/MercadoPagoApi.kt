package com.techmarket.app

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

object MercadoPagoApi {

    private const val BASE_URL =
        "https://techmarket-payments.davidrb126.workers.dev"

    private val executor = Executors.newSingleThreadExecutor()

    data class PreferenciaPago(
        val preferenceId: String,
        val initPoint: String,
        val sandboxInitPoint: String?,
        val externalReference: String,
        val estado: String
    )

    data class EstadoPago(
        val numeroPedido: String,
        val estado: String,
        val preferenceId: String?,
        val paymentId: String?,
        val monto: Long,
        val moneda: String
    )

    interface Callback<T> {
        fun onSuccess(result: T)
        fun onError(message: String)
    }

    // ==========================================
    // CREAR PREFERENCIA DE PAGO
    // ==========================================

    fun crearPreferencia(
        numeroPedido: String,
        monto: Long,
        descripcion: String,
        externalReference: String,
        callback: Callback<PreferenciaPago>
    ) {

        executor.execute {

            try {

                require(numeroPedido.isNotBlank()) {
                    "El número del pedido es obligatorio."
                }

                require(externalReference.isNotBlank()) {
                    "La referencia externa es obligatoria."
                }

                require(monto > 0) {
                    "El monto debe ser mayor que cero."
                }

                val body = JSONObject().apply {
                    put("numeroPedido", numeroPedido)
                    put("externalReference", externalReference)
                    put("monto", monto)
                    put("moneda", "COP")
                    put("descripcion", descripcion)
                }

                val response = ejecutarSolicitud(
                    ruta = "/api/payments/preference",
                    metodo = "POST",
                    body = body.toString()
                )

                val preference = JSONObject(response)

                val resultado = PreferenciaPago(
                    preferenceId = preference.getString(
                        "preference_id"
                    ),
                    initPoint = preference.getString(
                        "init_point"
                    ),
                    sandboxInitPoint = preference.optString(
                        "sandbox_init_point"
                    ).takeIf {
                        it.isNotBlank() && it != "null"
                    },
                    externalReference = preference.getString(
                        "external_reference"
                    ),
                    estado = preference.optString(
                        "estado",
                        "PENDIENTE"
                    )
                )

                callback.onSuccess(resultado)

            } catch (e: Exception) {

                callback.onError(
                    e.message ?: "No fue posible crear el pago."
                )
            }
        }
    }

    // ==========================================
    // CONSULTAR ESTADO DEL PAGO
    // ==========================================

    fun consultarEstado(
        externalReference: String,
        callback: Callback<EstadoPago>
    ) {

        executor.execute {

            try {

                require(externalReference.isNotBlank()) {
                    "La referencia externa es obligatoria."
                }

                val referenciaCodificada =
                    URLEncoder.encode(
                        externalReference,
                        "UTF-8"
                    )

                val ruta =
                    "/api/payments/status?external_reference=$referenciaCodificada"

                val response = ejecutarSolicitud(
                    ruta = ruta,
                    metodo = "GET"
                )

                val json = JSONObject(response)

                val resultado = EstadoPago(
                    numeroPedido = json.getString(
                        "numero_pedido"
                    ),
                    estado = json.getString(
                        "estado"
                    ),
                    preferenceId = json.optString(
                        "preference_id"
                    ).takeIf {
                        it.isNotBlank() && it != "null"
                    },
                    paymentId = json.optString(
                        "payment_id"
                    ).takeIf {
                        it.isNotBlank() && it != "null"
                    },
                    monto = json.getLong("monto"),
                    moneda = json.optString(
                        "moneda",
                        "COP"
                    )
                )

                callback.onSuccess(resultado)

            } catch (e: Exception) {

                callback.onError(
                    e.message ?: "No fue posible consultar el pago."
                )
            }
        }
    }

    // ==========================================
    // EJECUTAR SOLICITUD HTTP
    // ==========================================

    private fun ejecutarSolicitud(
        ruta: String,
        metodo: String,
        body: String? = null
    ): String {

        val connection = (
                URL("$BASE_URL$ruta").openConnection()
                        as HttpURLConnection
                )

        try {

            connection.requestMethod = metodo
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (body != null) {

                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )

                connection.outputStream.use { output ->
                    output.write(
                        body.toByteArray(Charsets.UTF_8)
                    )
                }
            }

            val status = connection.responseCode

            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val response = stream?.use {
                BufferedReader(
                    InputStreamReader(it)
                ).readText()
            }.orEmpty()

            if (status !in 200..299) {

                val error = try {
                    JSONObject(response).optString(
                        "error",
                        "Error HTTP $status"
                    )
                } catch (_: Exception) {
                    "Error HTTP $status"
                }

                throw Exception(error)
            }

            return response

        } finally {
            connection.disconnect()
        }
    }
}
