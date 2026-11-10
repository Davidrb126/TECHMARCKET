package com.techmarket.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.DetalleTiendaPedido
import com.techmarket.app.database.PedidoDAO
import java.text.NumberFormat
import java.util.Locale

class DetalleCompraActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NUMERO_PEDIDO = "numero_pedido"
    }

    private lateinit var pedidoDAO: PedidoDAO

    private lateinit var tvNumeroPedido: TextView
    private lateinit var tvFechaPedido: TextView
    private lateinit var tvEstadoPedido: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvEnvio: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvDireccion: TextView
    private lateinit var tvMetodoPago: TextView

    private lateinit var contenedorProductos: LinearLayout
    private lateinit var contenedorTiendas: LinearLayout

    private val formatoMoneda: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_compra)

        inicializarVistas()

        pedidoDAO = PedidoDAO(this)

        findViewById<ImageView>(R.id.btnVolverDetalleCompra).setOnClickListener {
            finish()
        }

        cargarDetallePedido()
    }

    private fun inicializarVistas() {
        tvNumeroPedido = findViewById(R.id.tvNumeroPedidoDetalle)
        tvFechaPedido = findViewById(R.id.tvFechaPedidoDetalle)
        tvEstadoPedido = findViewById(R.id.tvEstadoPedidoDetalle)

        tvSubtotal = findViewById(R.id.tvSubtotalDetalle)
        tvEnvio = findViewById(R.id.tvEnvioDetalle)
        tvTotal = findViewById(R.id.tvTotalDetalle)

        tvDireccion = findViewById(R.id.tvDireccionDetalle)
        tvMetodoPago = findViewById(R.id.tvMetodoPagoDetalle)

        contenedorProductos = findViewById(R.id.contenedorProductosDetalle)
        contenedorTiendas = findViewById(R.id.contenedorTiendasDetalle)

        // Tamaños de texto de la información general del pedido
        tvNumeroPedido.textSize = 18f
        tvFechaPedido.textSize = 13f
        tvEstadoPedido.textSize = 14f

        // Resumen de pago
        tvSubtotal.textSize = 15f
        tvEnvio.textSize = 15f
        tvTotal.textSize = 18f

        // Información de entrega y pago
        tvDireccion.textSize = 14f
        tvMetodoPago.textSize = 14f
    }

    private fun cargarDetallePedido() {
        val numeroPedido = intent.getStringExtra(EXTRA_NUMERO_PEDIDO)

        if (numeroPedido.isNullOrBlank()) {
            Toast.makeText(
                this,
                "No se recibió el número del pedido.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val documentoComprador =
            SessionManager.obtenerDocumento(this).orEmpty()

        if (documentoComprador.isBlank()) {
            Toast.makeText(
                this,
                "No se pudo identificar al comprador.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val pedido = pedidoDAO.obtenerDetallePedido(
            numeroPedido,
            documentoComprador
        )

        if (pedido == null) {
            Toast.makeText(
                this,
                "No se encontró el pedido o no pertenece a esta cuenta.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        mostrarInformacionPedido(pedido)

        val tiendas = pedidoDAO.obtenerTiendasPedido(
            pedido.numeroPedido
        )

        mostrarProductos(pedido.productos)
        mostrarTiendas(tiendas)
    }

    private fun mostrarInformacionPedido(pedido: Pedido) {
        tvNumeroPedido.text = "Pedido #${pedido.numeroPedido}"
        tvFechaPedido.text = "Fecha: ${pedido.fecha}"

        tvEstadoPedido.text = pedido.estado
        actualizarColorEstado(tvEstadoPedido, pedido.estado)

        tvSubtotal.text = "Subtotal: ${formatearMoneda(pedido.subtotal)}"
        tvEnvio.text = "Envío: ${formatearMoneda(pedido.envio)}"
        tvTotal.text = "Total: ${formatearMoneda(pedido.total)}"

        tvDireccion.text = pedido.direccion
        tvMetodoPago.text = pedido.metodoPago
    }

    private fun mostrarProductos(productos: List<CarritoItem>) {
        contenedorProductos.removeAllViews()

        if (productos.isEmpty()) {
            agregarTexto(
                contenedorProductos,
                "No hay productos registrados en este pedido."
            )
            return
        }

        productos.forEachIndexed { index, item ->

            val producto = item.producto

            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(12), dp(8), dp(12))
            }

            val nombre = TextView(this).apply {
                text = producto.nombre
                textSize = 17f
                setTextColor(android.graphics.Color.rgb(23, 32, 51))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val cantidad = TextView(this).apply {
                text = "Cantidad: ${item.cantidad}"
                textSize = 15f
                setTextColor(android.graphics.Color.rgb(102, 113, 134))
            }

            val precio = TextView(this).apply {
                text = "Precio unitario: ${producto.precio}"
                textSize = 15f
                setTextColor(android.graphics.Color.rgb(102, 113, 134))
            }

            val subtotalProducto = TextView(this).apply {
                text = "Total: ${calcularSubtotalProducto(producto.precio, item.cantidad)}"
                textSize = 16f
                setTextColor(android.graphics.Color.rgb(10, 84, 213))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            fila.addView(nombre)
            fila.addView(cantidad)
            fila.addView(precio)
            fila.addView(subtotalProducto)

            contenedorProductos.addView(fila)

            if (index < productos.lastIndex) {
                agregarSeparador(contenedorProductos)
            }
        }
    }

    private fun mostrarTiendas(tiendas: List<DetalleTiendaPedido>) {
        contenedorTiendas.removeAllViews()

        if (tiendas.isEmpty()) {
            agregarTexto(
                contenedorTiendas,
                "No hay información de seguimiento disponible."
            )
            return
        }

        tiendas.forEach { tienda ->
            agregarTarjetaTienda(tienda)
        }
    }

    private fun agregarTarjetaTienda(tienda: DetalleTiendaPedido) {

        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundResource(R.drawable.bg_tarjeta_pedido)

            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            parametros.bottomMargin = dp(12)

            layoutParams = parametros
        }

        val nombreTienda = TextView(this).apply {
            text = tienda.nombreTienda
            textSize = 18f
            setTextColor(android.graphics.Color.rgb(23, 32, 51))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val estado = TextView(this).apply {
            text = "Estado: ${tienda.estado}"
            textSize = 15f
            setPadding(0, dp(10), 0, dp(10))
            setTextColor(android.graphics.Color.rgb(10, 84, 213))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        tarjeta.addView(nombreTienda)
        tarjeta.addView(estado)

        actualizarColorEstado(estado, tienda.estado)

        val tituloProductos = crearSubtitulo("Productos de esta tienda")
        tarjeta.addView(tituloProductos)

        if (tienda.productos.isEmpty()) {
            agregarTexto(tarjeta, "No hay productos registrados.")
        } else {
            tienda.productos.forEach { item ->
                val texto = TextView(this).apply {
                    text = "• ${item.producto.nombre} x${item.cantidad}"
                    textSize = 15f
                    setTextColor(android.graphics.Color.rgb(86, 97, 118))
                    setPadding(0, dp(6), 0, dp(6))
                }
                tarjeta.addView(texto)
            }
        }

        tarjeta.addView(crearSubtitulo("Historial de seguimiento"))

        if (tienda.historial.isEmpty()) {
            agregarTexto(tarjeta, "Todavía no hay movimientos registrados.")
        } else {
            tienda.historial.forEach { movimiento ->

                val textoHistorial = TextView(this).apply {
                    text = buildString {
                        append("• ${movimiento.estadoNuevo}")
                        append("\nFecha: ${movimiento.fechaHora}")
                        append("\nResponsable: ${movimiento.responsable}")

                        if (!movimiento.motivo.isNullOrBlank()) {
                            append("\nMotivo: ${movimiento.motivo}")
                        }
                    }

                    textSize = 14f
                    setTextColor(android.graphics.Color.rgb(86, 97, 118))
                    setPadding(0, dp(9), 0, dp(9))
                    setLineSpacing(dp(2).toFloat(), 1f)
                }

                tarjeta.addView(textoHistorial)
            }
        }

        contenedorTiendas.addView(tarjeta)
    }

    private fun crearSubtitulo(texto: String): TextView {
        return TextView(this).apply {
            text = texto
            textSize = 15f
            setTextColor(android.graphics.Color.rgb(23, 32, 51))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(7))
        }
    }

    private fun agregarTexto(
        contenedor: LinearLayout,
        texto: String
    ) {
        val vista = TextView(this).apply {
            text = texto
            textSize = 15f
            setTextColor(android.graphics.Color.rgb(102, 113, 134))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        contenedor.addView(vista)
    }

    private fun agregarSeparador(contenedor: LinearLayout) {
        val separador = View(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(230, 234, 241))
        }

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(1)
        )

        contenedor.addView(separador, parametros)
    }

    private fun actualizarColorEstado(
        vista: TextView,
        estado: String
    ) {
        val color = when (estado) {
            "Entregado" -> android.graphics.Color.rgb(22, 163, 74)
            "Cancelado" -> android.graphics.Color.rgb(220, 38, 38)
            "Enviado" -> android.graphics.Color.rgb(29, 78, 216)
            else -> android.graphics.Color.rgb(10, 84, 213)
        }

        vista.setTextColor(color)
    }

    private fun calcularSubtotalProducto(
        precio: String,
        cantidad: Int
    ): String {
        val valor = precio
            .replace("$", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
            .toDoubleOrNull()

        return if (valor != null) {
            formatearMoneda((valor * cantidad).toLong())
        } else {
            "No disponible"
        }
    }

    private fun formatearMoneda(valor: Long): String {
        return formatoMoneda.format(valor)
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}
