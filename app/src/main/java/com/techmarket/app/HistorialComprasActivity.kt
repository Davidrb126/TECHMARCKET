package com.techmarket.app

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.techmarket.app.database.PedidoDAO

class HistorialComprasActivity : AppCompatActivity() {

    private lateinit var pedidoDAO: PedidoDAO
    private lateinit var rvHistorialCompras: RecyclerView
    private lateinit var pedidoAdapter: PedidoAdapter

    private lateinit var btnFiltroTodas: Button
    private lateinit var btnFiltroEnProceso: Button
    private lateinit var btnFiltroEntregadas: Button

    private lateinit var navInicioHistorial: LinearLayout
    private lateinit var navProductosHistorial: LinearLayout
    private lateinit var navFavoritosHistorial: LinearLayout
    private lateinit var navPerfilHistorial: LinearLayout

    private var filtroActual = "Todas"

    private val colorSeleccionado = Color.rgb(22, 70, 200)
    private val colorNormal = Color.rgb(119, 119, 119)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_compras)

        pedidoDAO = PedidoDAO(this)

        inicializarComponentes()
        configurarLista()
        configurarEventos()

        actualizarNavegacion("Productos")
        aplicarFiltro("Todas")
    }

    private fun inicializarComponentes() {

        rvHistorialCompras = findViewById(R.id.rvHistorialCompras)

        btnFiltroTodas = findViewById(R.id.btnFiltroTodas)
        btnFiltroEnProceso = findViewById(R.id.btnFiltroEnProceso)
        btnFiltroEntregadas = findViewById(R.id.btnFiltroEntregadas)

        navInicioHistorial = findViewById(R.id.navInicioHistorial)
        navProductosHistorial = findViewById(R.id.navProductosHistorial)
        navFavoritosHistorial = findViewById(R.id.navFavoritosHistorial)
        navPerfilHistorial = findViewById(R.id.navPerfilHistorial)
    }

    private fun configurarLista() {

        pedidoAdapter = PedidoAdapter(emptyList()) { pedido ->

            val intent = Intent(
                this,
                DetalleCompraActivity::class.java
            )

            intent.putExtra(
                DetalleCompraActivity.EXTRA_NUMERO_PEDIDO,
                pedido.numeroPedido
            )

            startActivity(intent)
        }

        rvHistorialCompras.layoutManager = LinearLayoutManager(this)
        rvHistorialCompras.adapter = pedidoAdapter
    }

    private fun configurarEventos() {

        // Regresar directamente al inicio del comprador.
        findViewById<ImageButton>(R.id.btnVolverHistorial)
            .setOnClickListener {
                irAlInicio()
            }

        findViewById<ImageButton>(R.id.btnCarritoHistorial)
            .setOnClickListener {
                abrirCarrito()
            }

        btnFiltroTodas.setOnClickListener {
            aplicarFiltro("Todas")
        }

        btnFiltroEnProceso.setOnClickListener {
            aplicarFiltro("En proceso")
        }

        btnFiltroEntregadas.setOnClickListener {
            aplicarFiltro("Entregado")
        }

        navInicioHistorial.setOnClickListener {
            actualizarNavegacion("Inicio")
            irAlInicio()
        }

        navProductosHistorial.setOnClickListener {
            actualizarNavegacion("Productos")
            irAlInicio()
        }

        navFavoritosHistorial.setOnClickListener {
            actualizarNavegacion("Favoritos")

            Toast.makeText(
                this,
                "El módulo de favoritos estará disponible próximamente.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Abrir el perfil del comprador.
        navPerfilHistorial.setOnClickListener {
            actualizarNavegacion("Perfil")

            val intent = Intent(
                this,
                PerfilCompradorActivity::class.java
            )

            startActivity(intent)
        }
    }

    /*
     * Actualiza el color del icono y del texto
     * de la opción seleccionada en la navegación inferior.
     */
    private fun actualizarNavegacion(opcionSeleccionada: String) {

        val opciones = listOf(
            "Inicio" to navInicioHistorial,
            "Productos" to navProductosHistorial,
            "Favoritos" to navFavoritosHistorial,
            "Perfil" to navPerfilHistorial
        )

        opciones.forEach { (nombre, contenedor) ->

            val seleccionado = nombre == opcionSeleccionada

            val color = if (seleccionado) {
                colorSeleccionado
            } else {
                colorNormal
            }

            // Primer elemento: icono.
            val icono = contenedor.getChildAt(0) as? ImageView

            icono?.imageTintList = ColorStateList.valueOf(color)

            // Segundo elemento: texto.
            val texto = contenedor.getChildAt(1) as? TextView

            texto?.setTextColor(color)
            texto?.setTypeface(
                null,
                if (seleccionado) android.graphics.Typeface.BOLD
                else android.graphics.Typeface.NORMAL
            )
        }
    }

    private fun aplicarFiltro(filtro: String) {

        filtroActual = filtro

        // Obtener el documento del comprador que inició sesión.
        val documentoComprador =
            SessionManager.obtenerDocumento(this).orEmpty()

        if (documentoComprador.isBlank()) {

            pedidoAdapter.actualizarPedidos(emptyList())

            Toast.makeText(
                this,
                "No se pudo identificar al comprador. Inicia sesión nuevamente.",
                Toast.LENGTH_SHORT
            ).show()

            actualizarEstiloFiltros()
            return
        }

        // Consultar únicamente las compras del comprador.
        val todosLosPedidos =
            pedidoDAO.obtenerPedidosPorComprador(documentoComprador)

        val pedidosFiltrados = when (filtro) {

            "En proceso" -> todosLosPedidos.filter {
                it.estado == "En proceso"
            }

            "Entregado" -> todosLosPedidos.filter {
                it.estado == "Entregado"
            }

            else -> todosLosPedidos
        }

        pedidoAdapter.actualizarPedidos(pedidosFiltrados)

        actualizarEstiloFiltros()

        if (pedidosFiltrados.isEmpty()) {

            val mensaje = when (filtro) {
                "En proceso" -> "No tienes compras en proceso."
                "Entregado" -> "No tienes compras entregadas."
                else -> "Todavía no tienes compras registradas."
            }

            Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun actualizarEstiloFiltros() {

        val colorFondoSeleccionado = 0xFFE5ECFF.toInt()
        val colorFondoNormal = 0xFFF1F1F1.toInt()

        val textoSeleccionado = 0xFF1646C8.toInt()
        val textoNormal = 0xFF172554.toInt()

        btnFiltroTodas.backgroundTintList =
            ColorStateList.valueOf(
                if (filtroActual == "Todas") {
                    colorFondoSeleccionado
                } else {
                    colorFondoNormal
                }
            )

        btnFiltroEnProceso.backgroundTintList =
            ColorStateList.valueOf(
                if (filtroActual == "En proceso") {
                    colorFondoSeleccionado
                } else {
                    colorFondoNormal
                }
            )

        btnFiltroEntregadas.backgroundTintList =
            ColorStateList.valueOf(
                if (filtroActual == "Entregado") {
                    colorFondoSeleccionado
                } else {
                    colorFondoNormal
                }
            )

        btnFiltroTodas.setTextColor(
            if (filtroActual == "Todas") textoSeleccionado
            else textoNormal
        )

        btnFiltroEnProceso.setTextColor(
            if (filtroActual == "En proceso") textoSeleccionado
            else textoNormal
        )

        btnFiltroEntregadas.setTextColor(
            if (filtroActual == "Entregado") textoSeleccionado
            else textoNormal
        )
    }

    private fun abrirCarrito() {

        val intent = Intent(
            this,
            CarritoActivity::class.java
        )

        startActivity(intent)
    }

    private fun irAlInicio() {

        val intent = Intent(
            this,
            InicioCompradorActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        if (::pedidoDAO.isInitialized &&
            ::pedidoAdapter.isInitialized
        ) {
            aplicarFiltro(filtroActual)
        }
    }
}
