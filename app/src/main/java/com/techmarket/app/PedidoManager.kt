package com.techmarket.app

import android.content.Context
import com.techmarket.app.database.PedidoDAO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Pedido(
    val numeroPedido: String,
    val productos: List<CarritoItem>,
    val direccion: String,
    val metodoPago: String,
    val subtotal: Long,
    val envio: Long,
    val total: Long,
    val fecha: String,
    val estado: String = "En proceso"
)

object PedidoManager {

    private var pedidoDAO: PedidoDAO? = null

    fun inicializar(context: Context) {
        if (pedidoDAO == null) {
            pedidoDAO = PedidoDAO(context.applicationContext)
        }
    }

    private fun obtenerDAO(): PedidoDAO {
        return pedidoDAO
            ?: throw IllegalStateException("PedidoManager no ha sido inicializado.")
    }

    fun registrarPedido(
        productos: List<CarritoItem>,
        datosEntrega: DatosEntrega,
        metodoPago: String,
        subtotal: Long,
        envio: Long,
        documentoComprador: String
    ): Pedido {

        require(documentoComprador.isNotBlank()) {
            "No se encontró el documento del comprador."
        }

        val numero = "TM${System.currentTimeMillis().toString().takeLast(8)}"

        val formatoFecha = SimpleDateFormat(
            "dd 'de' MMMM 'de' yyyy",
            Locale("es", "CO")
        )

        val fechaActual = formatoFecha.format(Date())

        val pedido = Pedido(
            numeroPedido = numero,
            productos = productos.map { it.copy() },
            direccion = "${datosEntrega.direccion}, ${datosEntrega.ciudad}, ${datosEntrega.departamento}",
            metodoPago = metodoPago,
            subtotal = subtotal,
            envio = envio,
            total = subtotal + envio,
            fecha = fechaActual,
            estado = "En proceso"
        )

        obtenerDAO().guardarPedido(pedido, documentoComprador)

        return pedido
    }

    fun obtenerPedidos(): List<Pedido> {
        return obtenerDAO().obtenerPedidos()
    }

    fun obtenerUltimoPedido(): Pedido? {
        return obtenerPedidos().firstOrNull()
    }
}
