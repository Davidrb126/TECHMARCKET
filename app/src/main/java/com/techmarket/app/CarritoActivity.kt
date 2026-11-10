package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CarritoActivity : AppCompatActivity() {

    private lateinit var rvCarrito: RecyclerView
    private lateinit var contenedorCarritoVacio: LinearLayout
    private lateinit var tvCantidadCarrito: TextView
    private lateinit var tvSubtotalCarrito: TextView
    private lateinit var tvTotalCarrito: TextView
    private lateinit var btnContinuarCompra: Button

    // Recibir el resultado del inicio de sesión.
    private val loginLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->

            if (resultado.resultCode == RESULT_OK) {

                // Comprobar que la sesión se haya guardado.
                if (SessionManager.haySesionActiva(this)) {
                    continuarProcesoCompra()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo iniciar la sesión.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        inicializarComponentes()
        configurarEventos()

        rvCarrito.layoutManager = LinearLayoutManager(this)

        actualizarCarrito()
    }

    // --------------------------------------------------
    // INICIALIZAR COMPONENTES
    // --------------------------------------------------

    private fun inicializarComponentes() {

        rvCarrito = findViewById(R.id.rvCarrito)

        contenedorCarritoVacio =
            findViewById(R.id.contenedorCarritoVacio)

        tvCantidadCarrito =
            findViewById(R.id.tvCantidadCarrito)

        tvSubtotalCarrito =
            findViewById(R.id.tvSubtotalCarrito)

        tvTotalCarrito =
            findViewById(R.id.tvTotalCarrito)

        btnContinuarCompra =
            findViewById(R.id.btnContinuarCompra)
    }

    // --------------------------------------------------
    // CONFIGURAR EVENTOS
    // --------------------------------------------------

    private fun configurarEventos() {

        findViewById<ImageButton>(R.id.btnVolverCarrito)
            .setOnClickListener {
                finish()
            }

        btnContinuarCompra.setOnClickListener {

            val productos = CarritoManager.obtenerProductos()

            if (productos.isEmpty()) {
                Toast.makeText(
                    this,
                    "Agrega productos antes de continuar.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            verificarSesion()
        }
    }

    // --------------------------------------------------
    // VERIFICAR SESIÓN DEL COMPRADOR
    // --------------------------------------------------

    private fun verificarSesion() {

        if (SessionManager.haySesionActiva(this)) {

            continuarProcesoCompra()

        } else {

            val intent = Intent(
                this,
                MainActivity::class.java
            ).apply {

                putExtra(
                    MainActivity.EXTRA_ABRIR_LOGIN,
                    true
                )

                putExtra(
                    MainActivity.EXTRA_CONTINUAR_COMPRA,
                    true
                )
            }

            loginLauncher.launch(intent)
        }
    }

    // --------------------------------------------------
    // CONTINUAR PROCESO DE COMPRA
    // --------------------------------------------------

    private fun continuarProcesoCompra() {

        // Verificar nuevamente que el carrito tenga productos.
        if (CarritoManager.obtenerProductos().isEmpty()) {
            Toast.makeText(
                this,
                "Tu carrito está vacío.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Verificar que exista una sesión activa.
        if (!SessionManager.haySesionActiva(this)) {
            Toast.makeText(
                this,
                "Debes iniciar sesión para continuar.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Abrir la pantalla de datos de entrega.
        val intent = Intent(
            this,
            DatosEntregaActivity::class.java
        )

        startActivity(intent)
    }

    // --------------------------------------------------
    // ACTUALIZAR CARRITO
    // --------------------------------------------------

    private fun actualizarCarrito() {

        val productos = CarritoManager.obtenerProductos()

        // Mostrar productos o estado vacío.
        if (productos.isEmpty()) {

            rvCarrito.visibility = View.GONE
            contenedorCarritoVacio.visibility = View.VISIBLE

        } else {

            rvCarrito.visibility = View.VISIBLE
            contenedorCarritoVacio.visibility = View.GONE
        }

        // Configurar adaptador.
        rvCarrito.adapter = CarritoAdapter(
            productos,

            onAumentar = { item ->

                CarritoManager.actualizarCantidad(
                    item.producto,
                    item.cantidad + 1
                )

                actualizarCarrito()
            },

            onDisminuir = { item ->

                CarritoManager.actualizarCantidad(
                    item.producto,
                    item.cantidad - 1
                )

                actualizarCarrito()
            },

            onEliminar = { item ->

                CarritoManager.eliminar(
                    item.producto
                )

                actualizarCarrito()
            }
        )

        // Actualizar cantidad total.
        val cantidad = CarritoManager.obtenerCantidadTotal()

        tvCantidadCarrito.text = cantidad.toString()

        // Actualizar subtotal y total.
        val subtotal = CarritoManager.obtenerSubtotal()

        tvSubtotalCarrito.text = formatearPrecio(subtotal)

        tvTotalCarrito.text = formatearPrecio(subtotal)

        // Habilitar o deshabilitar botón.
        btnContinuarCompra.isEnabled = productos.isNotEmpty()

        btnContinuarCompra.alpha =
            if (productos.isNotEmpty()) 1f else 0.5f
    }

    // --------------------------------------------------
    // FORMATEAR PRECIOS
    // --------------------------------------------------

    private fun formatearPrecio(valor: Long): String {

        val numero = String.format(
            "%,d",
            valor
        ).replace(",", ".")

        return "$ $numero"
    }

    // --------------------------------------------------
    // ACTUALIZAR AL REGRESAR A LA PANTALLA
    // --------------------------------------------------

    override fun onResume() {
        super.onResume()

        if (::rvCarrito.isInitialized) {
            actualizarCarrito()
        }
    }
}
