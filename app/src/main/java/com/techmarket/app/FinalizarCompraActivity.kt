package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finalizar_compra)

        // Inicializar el administrador de pedidos y SQLite.
        PedidoManager.inicializar(this)

        inicializarComponentes()
        configurarEventos()
        cargarResumenCompra()
    }

    private fun inicializarComponentes() {

        tvDireccion = findViewById(R.id.tvDireccionFinalizar)
        tvCiudad = findViewById(R.id.tvCiudadFinalizar)
        tvMetodoPago = findViewById(R.id.tvMetodoPagoFinalizar)
        tvCostoEnvio = findViewById(R.id.tvCostoEnvio)
        tvSubtotal = findViewById(R.id.tvSubtotalFinalizar)
        tvEnvio = findViewById(R.id.tvEnvioFinalizar)
        tvTotal = findViewById(R.id.tvTotalFinalizar)

        btnConfirmarCompra = findViewById(R.id.btnConfirmarCompra)
    }

    private fun configurarEventos() {

        findViewById<ImageButton>(R.id.btnVolverFinalizar)
            .setOnClickListener {
                finish()
            }

        findViewById<TextView>(R.id.tvCambiarDireccion)
            .setOnClickListener {
                finish()
            }

        btnConfirmarCompra.setOnClickListener {
            confirmarCompra()
        }
    }

    private fun cargarResumenCompra() {

        val datosEntrega = DatosEntregaManager.obtener()
        val metodoPago = MetodoPagoManager.obtener()
        val subtotal = CarritoManager.obtenerSubtotal()

        if (datosEntrega == null) {
            Toast.makeText(
                this,
                "No se encontraron los datos de entrega.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        if (metodoPago.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "No se encontró el método de pago.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        if (CarritoManager.obtenerProductos().isEmpty()) {
            Toast.makeText(
                this,
                "El carrito está vacío.",
                Toast.LENGTH_LONG
            ).show()

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

    private fun confirmarCompra() {

        val productos = CarritoManager.obtenerProductos()
        val datosEntrega = DatosEntregaManager.obtener()
        val metodoPago = MetodoPagoManager.obtener()

        // Validar que exista una sesión autenticada.
        val documentoComprador =
            SessionManager.obtenerDocumento(this).orEmpty()

        if (documentoComprador.isBlank()) {
            Toast.makeText(
                this,
                "Debes iniciar sesión para realizar una compra.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        // Validar que existan productos.
        if (productos.isEmpty()) {
            Toast.makeText(
                this,
                "No hay productos para confirmar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Validar los datos de entrega.
        if (datosEntrega == null) {
            Toast.makeText(
                this,
                "No se encontraron los datos de entrega.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Validar el método de pago.
        if (metodoPago.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Selecciona un método de pago.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val subtotal = CarritoManager.obtenerSubtotal()

        try {

            // Inicializar SQLite.
            PedidoManager.inicializar(this)

            // Registrar el pedido asociado al comprador autenticado.
            PedidoManager.registrarPedido(
                productos = productos,
                datosEntrega = datosEntrega,
                metodoPago = metodoPago,
                subtotal = subtotal,
                envio = costoEnvio,
                documentoComprador = documentoComprador
            )

            // Limpiar los datos de la compra finalizada.
            CarritoManager.vaciar()
            DatosEntregaManager.limpiar()
            MetodoPagoManager.limpiar()

            // Abrir la confirmación y limpiar las pantallas
            // anteriores del proceso de compra.
            val intent = Intent(
                this,
                ConfirmacionCompraActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No fue posible guardar el pedido: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun formatearPrecio(valor: Long): String {

        val numero = String.format("%,d", valor)
            .replace(",", ".")

        return "$ $numero"
    }
}
