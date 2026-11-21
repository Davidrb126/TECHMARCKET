package com.techmarket.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.PedidoPagoDAO
import java.util.UUID

class FinalizarCompraActivity : AppCompatActivity() {

    private lateinit var tvDireccion: TextView
    private lateinit var tvCiudad: TextView
    private lateinit var tvMetodoPago: TextView
    private lateinit var tvCostoEnvio: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvEnvio: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnConfirmarCompra: Button

    private val costoEnvio = 10000L

    private lateinit var pedidoPagoDAO: PedidoPagoDAO

    private var pedidoOnline: Pedido? = null
    private var referenciaPago: String? = null

    private var solicitudEnCurso = false
    private var checkoutAbierto = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_finalizar_compra)

        PedidoManager.inicializar(this)
        pedidoPagoDAO = PedidoPagoDAO(this)

        inicializarComponentes()
        configurarEventos()
        cargarResumenCompra()
    }

    // ==========================================
    // INICIALIZAR COMPONENTES
    // ==========================================

    private fun inicializarComponentes() {

        tvDireccion = findViewById(
            R.id.tvDireccionFinalizar
        )

        tvCiudad = findViewById(
            R.id.tvCiudadFinalizar
        )

        tvMetodoPago = findViewById(
            R.id.tvMetodoPagoFinalizar
        )

        tvCostoEnvio = findViewById(
            R.id.tvCostoEnvio
        )

        tvSubtotal = findViewById(
            R.id.tvSubtotalFinalizar
        )

        tvEnvio = findViewById(
            R.id.tvEnvioFinalizar
        )

        tvTotal = findViewById(
            R.id.tvTotalFinalizar
        )

        btnConfirmarCompra = findViewById(
            R.id.btnConfirmarCompra
        )
    }

    // ==========================================
    // EVENTOS
    // ==========================================

    private fun configurarEventos() {

        findViewById<ImageButton>(
            R.id.btnVolverFinalizar
        ).setOnClickListener {
            finish()
        }

        findViewById<TextView>(
            R.id.tvCambiarDireccion
        ).setOnClickListener {
            finish()
        }

        btnConfirmarCompra.setOnClickListener {
            confirmarCompra()
        }
    }

    // ==========================================
    // RESUMEN DE COMPRA
    // ==========================================

    private fun cargarResumenCompra() {

        val datosEntrega = DatosEntregaManager.obtener()
        val metodoPago = MetodoPagoManager.obtener()
        val subtotal = CarritoManager.obtenerSubtotal()

        if (datosEntrega == null) {

            mostrarMensaje(
                "No se encontraron los datos de entrega."
            )

            finish()
            return
        }

        if (metodoPago.isNullOrEmpty()) {

            mostrarMensaje(
                "No se encontró el método de pago."
            )

            finish()
            return
        }

        if (CarritoManager.obtenerProductos().isEmpty()) {

            mostrarMensaje(
                "El carrito está vacío."
            )

            finish()
            return
        }

        tvDireccion.text = datosEntrega.direccion

        tvCiudad.text =
            "${datosEntrega.ciudad}, ${datosEntrega.departamento}"

        tvMetodoPago.text = metodoPago

        tvCostoEnvio.text = formatearPrecio(costoEnvio)
        tvSubtotal.text = formatearPrecio(subtotal)
        tvEnvio.text = formatearPrecio(costoEnvio)

        val total = subtotal + costoEnvio

        tvTotal.text = formatearPrecio(total)
    }

    // ==========================================
    // CONFIRMAR COMPRA
    // ==========================================

    private fun confirmarCompra() {

        if (solicitudEnCurso) return

        val metodoPago = MetodoPagoManager.obtener()

        if (metodoPago.isNullOrBlank()) {

            mostrarMensaje(
                "Selecciona un método de pago."
            )

            return
        }

        when (metodoPago) {

            "Pago contra entrega" -> {
                confirmarPagoContraEntrega()
            }

            "Tarjeta de crédito o débito",
            "PSE" -> {
                iniciarPagoMercadoPago()
            }

            else -> {
                mostrarMensaje(
                    "El método de pago seleccionado no es válido."
                )
            }
        }
    }

    // ==========================================
    // PAGO CONTRA ENTREGA
    // ==========================================

    private fun confirmarPagoContraEntrega() {

        val productos = CarritoManager.obtenerProductos()
        val datosEntrega = DatosEntregaManager.obtener()
        val metodoPago = MetodoPagoManager.obtener()

        val documentoComprador =
            SessionManager.obtenerDocumento(this).orEmpty()

        if (documentoComprador.isBlank()) {

            mostrarMensaje(
                "Debes iniciar sesión para realizar una compra."
            )

            return
        }

        if (productos.isEmpty()) {

            mostrarMensaje(
                "No hay productos para confirmar."
            )

            return
        }

        if (datosEntrega == null) {

            mostrarMensaje(
                "No se encontraron los datos de entrega."
            )

            return
        }

        if (metodoPago.isNullOrBlank()) {

            mostrarMensaje(
                "Selecciona un método de pago."
            )

            return
        }

        val subtotal = CarritoManager.obtenerSubtotal()

        try {

            PedidoManager.inicializar(this)

            PedidoManager.registrarPedido(
                productos = productos,
                datosEntrega = datosEntrega,
                metodoPago = metodoPago,
                subtotal = subtotal,
                envio = costoEnvio,
                documentoComprador = documentoComprador
            )

            finalizarCompraLocal()

        } catch (e: Exception) {

            mostrarMensaje(
                "No fue posible guardar el pedido: ${e.message}"
            )
        }
    }

    // ==========================================
    // INICIAR PAGO ELECTRÓNICO
    // ==========================================

    private fun iniciarPagoMercadoPago() {

        if (checkoutAbierto) {
            mostrarMensaje(
                "Ya se abrió Mercado Pago para este pedido."
            )
            return
        }

        val documentoComprador =
            SessionManager.obtenerDocumento(this).orEmpty()

        if (documentoComprador.isBlank()) {

            mostrarMensaje(
                "Debes iniciar sesión para realizar una compra."
            )

            return
        }

        val productos = CarritoManager.obtenerProductos()
        val datosEntrega = DatosEntregaManager.obtener()
        val metodoPago = MetodoPagoManager.obtener()

        if (productos.isEmpty()) {

            mostrarMensaje(
                "El carrito está vacío."
            )

            return
        }

        if (datosEntrega == null || metodoPago.isNullOrBlank()) {

            mostrarMensaje(
                "Faltan datos para completar la compra."
            )

            return
        }

        try {

            if (pedidoOnline == null) {

                val subtotal = CarritoManager.obtenerSubtotal()

                // Registrar primero el pedido local.
                val pedido = PedidoManager.registrarPedido(
                    productos = productos,
                    datosEntrega = datosEntrega,
                    metodoPago = metodoPago,
                    subtotal = subtotal,
                    envio = costoEnvio,
                    documentoComprador = documentoComprador
                )

                // Generar referencia única del pago.
                val referencia =
                    "TM-${pedido.numeroPedido}-${UUID.randomUUID()}"

                // Registrar el intento de pago en SQLite.
                pedidoPagoDAO.registrarPagoPendiente(
                    numeroPedido = pedido.numeroPedido,
                    externalReference = referencia,
                    monto = pedido.total,
                    moneda = "COP"
                )

                pedidoOnline = pedido
                referenciaPago = referencia
            }

            crearPreferenciaMercadoPago()

        } catch (e: Exception) {

            mostrarMensaje(
                "No fue posible preparar el pago: ${e.message}"
            )
        }
    }

    // ==========================================
    // CREAR PREFERENCIA
    // ==========================================

    private fun crearPreferenciaMercadoPago() {

        val pedido = pedidoOnline ?: return
        val referencia = referenciaPago ?: return

        if (solicitudEnCurso || checkoutAbierto) return

        solicitudEnCurso = true
        btnConfirmarCompra.isEnabled = false

        MercadoPagoApi.crearPreferencia(
            numeroPedido = pedido.numeroPedido,
            monto = pedido.total,
            descripcion = "Compra TechMarket ${pedido.numeroPedido}",
            externalReference = referencia,

            callback = object :
                MercadoPagoApi.Callback<MercadoPagoApi.PreferenciaPago> {

                override fun onSuccess(
                    result: MercadoPagoApi.PreferenciaPago
                ) {

                    runOnUiThread {

                        solicitudEnCurso = false

                        try {

                            val actualizado =
                                pedidoPagoDAO.actualizarPreferencia(
                                    externalReference = referencia,
                                    preferenceId = result.preferenceId,
                                    initPoint = result.initPoint,
                                    sandboxInitPoint = result.sandboxInitPoint
                                )

                            if (!actualizado) {

                                btnConfirmarCompra.isEnabled = true

                                mostrarMensaje(
                                    "No fue posible actualizar el registro local del pago."
                                )

                                return@runOnUiThread
                            }

                            // Utilizar el enlace de pruebas.
                            val urlCheckout =
                                result.sandboxInitPoint

                            if (urlCheckout.isNullOrBlank()) {

                                btnConfirmarCompra.isEnabled = true

                                mostrarMensaje(
                                    "Mercado Pago no devolvió el enlace de prueba."
                                )

                                return@runOnUiThread
                            }

                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(urlCheckout)
                            )

                            startActivity(intent)

                            checkoutAbierto = true

                            mostrarMensaje(
                                "Completa el pago en Mercado Pago."
                            )

                        } catch (e: Exception) {

                            btnConfirmarCompra.isEnabled = true

                            mostrarMensaje(
                                "Error al guardar la preferencia: ${e.message}"
                            )
                        }
                    }
                }

                override fun onError(message: String) {

                    runOnUiThread {

                        solicitudEnCurso = false
                        btnConfirmarCompra.isEnabled = true

                        mostrarMensaje(
                            "No fue posible iniciar el pago: $message"
                        )
                    }
                }
            }
        )
    }

    // ==========================================
    // FINALIZAR COMPRA LOCAL
    // ==========================================

    private fun finalizarCompraLocal() {

        CarritoManager.vaciar()
        DatosEntregaManager.limpiar()
        MetodoPagoManager.limpiar()

        val intent = Intent(
            this,
            ConfirmacionCompraActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)

        finish()
    }

    // ==========================================
    // UTILIDADES
    // ==========================================

    private fun mostrarMensaje(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun formatearPrecio(valor: Long): String {

        val numero = String.format("%,d", valor)
            .replace(",", ".")

        return "$ $numero"
    }
}
