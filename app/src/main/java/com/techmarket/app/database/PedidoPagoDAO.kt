package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modelo que representa un intento de pago asociado a un pedido.
 */
data class PedidoPago(
    val id: Long = 0,
    val numeroPedido: String,
    val proveedor: String = PROVEEDOR_MERCADO_PAGO,
    val estado: String = ESTADO_PENDIENTE,
    val preferenceId: String? = null,
    val paymentId: String? = null,
    val externalReference: String,
    val monto: Long,
    val moneda: String = "COP",
    val initPoint: String? = null,
    val sandboxInitPoint: String? = null,
    val fechaCreacion: String,
    val fechaActualizacion: String
) {
    companion object {
        const val PROVEEDOR_MERCADO_PAGO = "MERCADO_PAGO"

        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_EN_PROCESO = "EN_PROCESO"
        const val ESTADO_APROBADO = "APROBADO"
        const val ESTADO_RECHAZADO = "RECHAZADO"
        const val ESTADO_CANCELADO = "CANCELADO"
        const val ESTADO_EXPIRADO = "EXPIRADO"
    }
}

/**
 * DAO encargado de administrar los intentos de pago
 * de los pedidos, independientemente de su estado logístico.
 */
class PedidoPagoDAO(context: Context) {

    private val dbLectura = DatabaseProvider.obtenerLectura(context)
    private val dbEscritura = DatabaseProvider.obtenerEscritura(context)

    companion object {
        private const val TABLA = "pedido_pagos"

        private const val COL_ID = "id"
        private const val COL_NUMERO_PEDIDO = "numero_pedido"
        private const val COL_PROVEEDOR = "proveedor"
        private const val COL_ESTADO = "estado"
        private const val COL_PREFERENCE_ID = "preference_id"
        private const val COL_PAYMENT_ID = "payment_id"
        private const val COL_EXTERNAL_REFERENCE = "external_reference"
        private const val COL_MONTO = "monto"
        private const val COL_MONEDA = "moneda"
        private const val COL_INIT_POINT = "init_point"
        private const val COL_SANDBOX_INIT_POINT = "sandbox_init_point"
        private const val COL_FECHA_CREACION = "fecha_creacion"
        private const val COL_FECHA_ACTUALIZACION = "fecha_actualizacion"
    }

    private fun fechaActual(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

    // ==========================================
    // REGISTRAR INTENTO DE PAGO
    // ==========================================

    /**
     * Registra un intento de pago pendiente.
     * El pedido debe existir previamente.
     */
    fun registrarPagoPendiente(
        numeroPedido: String,
        externalReference: String,
        monto: Long,
        moneda: String = "COP"
    ): Long {

        require(numeroPedido.isNotBlank()) {
            "El número del pedido es obligatorio."
        }

        require(externalReference.isNotBlank()) {
            "La referencia externa del pago es obligatoria."
        }

        require(monto > 0) {
            "El monto del pago debe ser mayor que cero."
        }

        val fecha = fechaActual()

        val valores = ContentValues().apply {
            put(COL_NUMERO_PEDIDO, numeroPedido)
            put(COL_PROVEEDOR, PedidoPago.PROVEEDOR_MERCADO_PAGO)
            put(COL_ESTADO, PedidoPago.ESTADO_PENDIENTE)
            putNull(COL_PREFERENCE_ID)
            putNull(COL_PAYMENT_ID)
            put(COL_EXTERNAL_REFERENCE, externalReference)
            put(COL_MONTO, monto)
            put(COL_MONEDA, moneda)
            putNull(COL_INIT_POINT)
            putNull(COL_SANDBOX_INIT_POINT)
            put(COL_FECHA_CREACION, fecha)
            put(COL_FECHA_ACTUALIZACION, fecha)
        }

        return dbEscritura.insertOrThrow(
            TABLA,
            null,
            valores
        )
    }

    // ==========================================
    // ACTUALIZAR DATOS DE LA PREFERENCIA
    // ==========================================

    /**
     * Guarda los identificadores y enlaces devueltos
     * por el backend al crear la preferencia.
     */
    fun actualizarPreferencia(
        externalReference: String,
        preferenceId: String,
        initPoint: String?,
        sandboxInitPoint: String?
    ): Boolean {

        if (
            externalReference.isBlank() ||
            preferenceId.isBlank()
        ) {
            return false
        }

        val valores = ContentValues().apply {
            put(COL_PREFERENCE_ID, preferenceId)
            put(COL_INIT_POINT, initPoint)
            put(COL_SANDBOX_INIT_POINT, sandboxInitPoint)
            put(COL_FECHA_ACTUALIZACION, fechaActual())
        }

        return dbEscritura.update(
            TABLA,
            valores,
            "$COL_EXTERNAL_REFERENCE = ?",
            arrayOf(externalReference)
        ) > 0
    }

    // ==========================================
    // ACTUALIZAR ESTADO DEL PAGO
    // ==========================================

    /**
     * Actualiza el estado del intento de pago.
     * No modifica el estado logístico del pedido.
     */
    fun actualizarEstado(
        externalReference: String,
        nuevoEstado: String,
        paymentId: String? = null
    ): Boolean {

        val estadosValidos = listOf(
            PedidoPago.ESTADO_PENDIENTE,
            PedidoPago.ESTADO_EN_PROCESO,
            PedidoPago.ESTADO_APROBADO,
            PedidoPago.ESTADO_RECHAZADO,
            PedidoPago.ESTADO_CANCELADO,
            PedidoPago.ESTADO_EXPIRADO
        )

        if (
            externalReference.isBlank() ||
            nuevoEstado !in estadosValidos
        ) {
            return false
        }

        val valores = ContentValues().apply {
            put(COL_ESTADO, nuevoEstado)

            if (!paymentId.isNullOrBlank()) {
                put(COL_PAYMENT_ID, paymentId)
            }

            put(COL_FECHA_ACTUALIZACION, fechaActual())
        }

        return dbEscritura.update(
            TABLA,
            valores,
            "$COL_EXTERNAL_REFERENCE = ?",
            arrayOf(externalReference)
        ) > 0
    }

    // ==========================================
    // CONSULTAR PAGOS DE UN PEDIDO
    // ==========================================

    fun obtenerPagosPorPedido(
        numeroPedido: String
    ): List<PedidoPago> {

        val pagos = mutableListOf<PedidoPago>()

        if (numeroPedido.isBlank()) return pagos

        val cursor = dbLectura.query(
            TABLA,
            null,
            "$COL_NUMERO_PEDIDO = ?",
            arrayOf(numeroPedido),
            null,
            null,
            "$COL_ID DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                pagos.add(construirPedidoPago(it))
            }
        }

        return pagos
    }

    // ==========================================
    // CONSULTAR PAGO POR REFERENCIA EXTERNA
    // ==========================================

    fun obtenerPagoPorReferencia(
        externalReference: String
    ): PedidoPago? {

        if (externalReference.isBlank()) return null

        val cursor = dbLectura.query(
            TABLA,
            null,
            "$COL_EXTERNAL_REFERENCE = ?",
            arrayOf(externalReference),
            null,
            null,
            null,
            "1"
        )

        cursor.use {
            return if (it.moveToFirst()) {
                construirPedidoPago(it)
            } else {
                null
            }
        }
    }

    // ==========================================
    // CONSULTAR PAGO POR IDENTIFICADOR
    // ==========================================

    fun obtenerPagoPorPreferenceId(
        preferenceId: String
    ): PedidoPago? {

        if (preferenceId.isBlank()) return null

        val cursor = dbLectura.query(
            TABLA,
            null,
            "$COL_PREFERENCE_ID = ?",
            arrayOf(preferenceId),
            null,
            null,
            null,
            "1"
        )

        cursor.use {
            return if (it.moveToFirst()) {
                construirPedidoPago(it)
            } else {
                null
            }
        }
    }

    // ==========================================
    // CONSULTAR PAGOS PENDIENTES
    // ==========================================

    fun obtenerPagosPendientes(): List<PedidoPago> {

        val pagos = mutableListOf<PedidoPago>()

        val cursor = dbLectura.query(
            TABLA,
            null,
            "$COL_ESTADO IN (?, ?)",
            arrayOf(
                PedidoPago.ESTADO_PENDIENTE,
                PedidoPago.ESTADO_EN_PROCESO
            ),
            null,
            null,
            "$COL_ID DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                pagos.add(construirPedidoPago(it))
            }
        }

        return pagos
    }

    // ==========================================
    // CONSTRUIR MODELO DE PAGO
    // ==========================================

    private fun construirPedidoPago(
        cursor: Cursor
    ): PedidoPago {

        fun obtenerTexto(columna: String): String? {
            val indice = cursor.getColumnIndexOrThrow(columna)

            return if (cursor.isNull(indice)) {
                null
            } else {
                cursor.getString(indice)
            }
        }

        return PedidoPago(
            id = cursor.getLong(
                cursor.getColumnIndexOrThrow(COL_ID)
            ),
            numeroPedido = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_NUMERO_PEDIDO)
            ),
            proveedor = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_PROVEEDOR)
            ),
            estado = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_ESTADO)
            ),
            preferenceId = obtenerTexto(COL_PREFERENCE_ID),
            paymentId = obtenerTexto(COL_PAYMENT_ID),
            externalReference = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_EXTERNAL_REFERENCE)
            ),
            monto = cursor.getLong(
                cursor.getColumnIndexOrThrow(COL_MONTO)
            ),
            moneda = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_MONEDA)
            ),
            initPoint = obtenerTexto(COL_INIT_POINT),
            sandboxInitPoint = obtenerTexto(COL_SANDBOX_INIT_POINT),
            fechaCreacion = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_FECHA_CREACION)
            ),
            fechaActualizacion = cursor.getString(
                cursor.getColumnIndexOrThrow(COL_FECHA_ACTUALIZACION)
            )
        )
    }
}
