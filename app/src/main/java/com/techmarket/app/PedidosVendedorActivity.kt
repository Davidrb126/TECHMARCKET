package com.techmarket.app

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.techmarket.app.database.HistorialEstadoPedido
import com.techmarket.app.database.PedidoDAO

class PedidosVendedorActivity : AppCompatActivity() {

    private lateinit var pedidoDAO: PedidoDAO

    private lateinit var rvPedidos: RecyclerView
    private lateinit var tvCantidadPedidos: TextView
    private lateinit var tvSinPedidos: TextView
    private lateinit var btnVolver: ImageButton

    private var idTienda: Long = -1L

    private val azul = Color.rgb(10, 84, 213)
    private val texto = Color.rgb(32, 33, 36)
    private val gris = Color.rgb(119, 119, 119)
    private val fondo = Color.rgb(247, 248, 250)
    private val rojo = Color.rgb(214, 69, 69)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedidos_vendedor)

        idTienda = intent.getLongExtra("ID_TIENDA", -1L)

        if (idTienda <= 0) {
            Toast.makeText(
                this,
                "No se identificó la tienda del vendedor.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        pedidoDAO = PedidoDAO(this)

        inicializarComponentes()
        configurarEventos()
        cargarPedidos()
    }

    // ==========================================
    // COMPONENTES
    // ==========================================

    private fun inicializarComponentes() {

        rvPedidos = findViewById(R.id.rvPedidosVendedor)

        tvCantidadPedidos =
            findViewById(R.id.tvCantidadPedidosVendedor)

        tvSinPedidos =
            findViewById(R.id.tvSinPedidosVendedor)

        btnVolver =
            findViewById(R.id.btnVolverPedidosVendedor)

        rvPedidos.layoutManager = LinearLayoutManager(this)
    }

    private fun configurarEventos() {
        btnVolver.setOnClickListener {
            finish()
        }
    }

    // ==========================================
    // CARGAR PEDIDOS
    // ==========================================

    private fun cargarPedidos() {

        try {

            val pedidos = pedidoDAO.obtenerPedidosPorTienda(idTienda)

            tvCantidadPedidos.text =
                "${pedidos.size} pedidos recibidos"

            if (pedidos.isEmpty()) {

                rvPedidos.visibility = View.GONE
                tvSinPedidos.visibility = View.VISIBLE

            } else {

                rvPedidos.visibility = View.VISIBLE
                tvSinPedidos.visibility = View.GONE

                rvPedidos.adapter =
                    PedidoVendedorAdapter(pedidos) { pedido ->
                        mostrarOpcionesPedido(pedido)
                    }
            }

        } catch (e: Exception) {

            rvPedidos.visibility = View.GONE
            tvSinPedidos.visibility = View.VISIBLE

            tvSinPedidos.text =
                "No fue posible cargar los pedidos. Intenta nuevamente."

            Toast.makeText(
                this,
                "Error al consultar los pedidos: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // DIÁLOGO BASE TECHMARKET
    // ==========================================

    private fun crearDialogo(
        titulo: String,
        subtitulo: String
    ): Pair<Dialog, LinearLayout> {

        val dialogo = Dialog(this)

        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val vista = LayoutInflater.from(this).inflate(
            R.layout.dialog_base_techmarket,
            null
        )

        val contenedor =
            vista.findViewById<LinearLayout>(
                R.id.contenedorDialogoTechMarket
            )

        vista.findViewById<TextView>(
            R.id.tvTituloDialogo
        ).text = titulo

        vista.findViewById<TextView>(
            R.id.tvSubtituloDialogo
        ).text = subtitulo

        contenedor.background = fondoRedondeado(
            Color.WHITE,
            22
        )

        dialogo.setContentView(vista)

        dialogo.window?.apply {
            setBackgroundDrawableResource(
                android.R.color.transparent
            )

            val ancho = resources.displayMetrics.widthPixels -
                    dp(32)

            setLayout(
                ancho,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialogo.setCancelable(true)

        return Pair(
            dialogo,
            vista.findViewById(
                R.id.contenedorContenidoDialogo
            )
        )
    }

    // ==========================================
    // MOSTRAR OPCIONES DEL PEDIDO
    // ==========================================

    private fun mostrarOpcionesPedido(pedido: Pedido) {

        val estado = pedido.estado.trim()

        val (dialogo, contenido) = crearDialogo(
            "Pedido #${pedido.numeroPedido}",
            "Selecciona qué deseas hacer con este pedido."
        )

        // Estado actual.
        val etiquetaEstado = TextView(this).apply {
            text = estado
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(obtenerColorEstado(estado))
            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )
            background = fondoRedondeado(
                obtenerFondoEstado(estado),
                20
            )
        }

        contenido.addView(etiquetaEstado)

        val opciones = mutableListOf<Triple<String, String, String>>()

        when (estado) {

            PedidoDAO.ESTADO_PENDIENTE -> {

                opciones.add(
                    Triple(
                        "✓",
                        "Aceptar pedido",
                        "Confirmar que atenderás esta compra."
                    )
                )

                opciones.add(
                    Triple(
                        "×",
                        "Cancelar pedido",
                        "Cancelar indicando el motivo."
                    )
                )
            }

            PedidoDAO.ESTADO_ACEPTADO -> {

                opciones.add(
                    Triple(
                        "↗",
                        "Iniciar preparación",
                        "Comenzar a preparar los productos."
                    )
                )

                opciones.add(
                    Triple(
                        "×",
                        "Cancelar pedido",
                        "Cancelar indicando el motivo."
                    )
                )
            }

            PedidoDAO.ESTADO_PREPARACION -> {

                opciones.add(
                    Triple(
                        "➜",
                        "Marcar como enviado",
                        "Confirmar que el pedido fue despachado."
                    )
                )

                opciones.add(
                    Triple(
                        "×",
                        "Cancelar pedido",
                        "Cancelar indicando el motivo."
                    )
                )
            }

            PedidoDAO.ESTADO_ENVIADO -> {

                opciones.add(
                    Triple(
                        "✓",
                        "Marcar como entregado",
                        "Confirmar la entrega del pedido."
                    )
                )
            }

            PedidoDAO.ESTADO_ENTREGADO,
            PedidoDAO.ESTADO_CANCELADO -> {
                // No mostrar acciones de modificación.
            }
        }

        opciones.add(
            Triple(
                "↻",
                "Consultar historial",
                "Ver los cambios y responsables del pedido."
            )
        )

        opciones.forEach { opcion ->

            val (simbolo, titulo, descripcion) = opcion

            val colorIcono = when (titulo) {
                "Cancelar pedido" -> rojo
                else -> azul
            }

            agregarOpcion(
                contenido,
                simbolo,
                titulo,
                descripcion,
                colorIcono
            ) {

                dialogo.dismiss()

                when (titulo) {

                    "Aceptar pedido" -> {
                        actualizarEstado(
                            pedido,
                            PedidoDAO.ESTADO_ACEPTADO
                        )
                    }

                    "Iniciar preparación" -> {
                        actualizarEstado(
                            pedido,
                            PedidoDAO.ESTADO_PREPARACION
                        )
                    }

                    "Marcar como enviado" -> {
                        actualizarEstado(
                            pedido,
                            PedidoDAO.ESTADO_ENVIADO
                        )
                    }

                    "Marcar como entregado" -> {
                        actualizarEstado(
                            pedido,
                            PedidoDAO.ESTADO_ENTREGADO
                        )
                    }

                    "Cancelar pedido" -> {
                        solicitarMotivoCancelacion(pedido)
                    }

                    "Consultar historial" -> {
                        consultarHistorial(pedido)
                    }
                }
            }
        }

        val botones = dialogo.findViewById<LinearLayout>(
            R.id.contenedorBotonesDialogo
        )

        agregarBoton(
            botones,
            "Volver a pedidos",
            azul,
            true
        ) {
            dialogo.dismiss()
        }

        dialogo.show()
    }

    // ==========================================
    // OPCIONES VISUALES
    // ==========================================

    private fun agregarOpcion(
        contenedor: LinearLayout,
        simbolo: String,
        titulo: String,
        descripcion: String,
        colorIcono: Int,
        accion: () -> Unit
    ) {

        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(10),
                dp(12),
                dp(10),
                dp(12)
            )
            background = fondoRedondeado(
                Color.WHITE,
                14
            )
            isClickable = true
            isFocusable = true
        }

        val icono = TextView(this).apply {
            text = simbolo
            textSize = 23f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(colorIcono)
            background = fondoRedondeado(
                aclararColor(colorIcono),
                13
            )
        }

        fila.addView(
            icono,
            LinearLayout.LayoutParams(
                dp(46),
                dp(46)
            )
        )

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        val tituloVista = TextView(this).apply {
            text = titulo
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(texto)
        }

        val descripcionVista = TextView(this).apply {
            text = descripcion
            textSize = 13f
            setTextColor(gris)
            setPadding(0, dp(4), 0, 0)
        }

        textos.addView(tituloVista)
        textos.addView(descripcionVista)

        fila.addView(
            textos,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val flecha = TextView(this).apply {
            text = "›"
            textSize = 26f
            setTextColor(gris)
            gravity = Gravity.CENTER
        }

        fila.addView(
            flecha,
            LinearLayout.LayoutParams(
                dp(25),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        fila.setOnClickListener {
            accion()
        }

        contenedor.addView(
            fila,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )
    }

    // ==========================================
    // CANCELACIÓN PERSONALIZADA
    // ==========================================

    private fun solicitarMotivoCancelacion(pedido: Pedido) {

        val (dialogo, contenido) = crearDialogo(
            "Cancelar pedido",
            "Cuéntanos por qué necesitas cancelar este pedido."
        )

        val campoMotivo = EditText(this).apply {
            hint = "Escribe el motivo de cancelación"
            textSize = 15f
            minLines = 3
            gravity = Gravity.TOP
            setTextColor(texto)
            setHintTextColor(gris)
            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )
            background = fondoRedondeado(
                fondo,
                12,
                Color.rgb(225, 228, 233)
            )
        }

        contenido.addView(
            campoMotivo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(110)
            )
        )

        val botones = dialogo.findViewById<LinearLayout>(
            R.id.contenedorBotonesDialogo
        )

        agregarBoton(
            botones,
            "Volver",
            gris,
            false
        ) {
            dialogo.dismiss()
        }

        agregarBoton(
            botones,
            "Confirmar cancelación",
            rojo,
            true
        ) {

            val motivo = campoMotivo.text
                .toString()
                .trim()

            if (motivo.isBlank()) {

                campoMotivo.error =
                    "Debes indicar el motivo de cancelación."

                return@agregarBoton
            }

            dialogo.dismiss()

            actualizarEstado(
                pedido,
                PedidoDAO.ESTADO_CANCELADO,
                motivo
            )
        }

        dialogo.show()
    }

    // ==========================================
    // ACTUALIZAR ESTADO
    // ==========================================

    private fun actualizarEstado(
        pedido: Pedido,
        nuevoEstado: String,
        motivo: String? = null
    ) {

        try {

            val responsable =
                SessionManager.obtenerNombre(this) ?: "Vendedor"

            val actualizado = pedidoDAO.cambiarEstadoPedidoTienda(
                numeroPedido = pedido.numeroPedido,
                idTienda = idTienda,
                estadoNuevo = nuevoEstado,
                responsable = responsable,
                motivo = motivo
            )

            if (actualizado) {

                Toast.makeText(
                    this,
                    "Pedido actualizado correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                cargarPedidos()

            } else {

                Toast.makeText(
                    this,
                    "No se pudo actualizar el pedido. Verifica su estado actual.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Error al actualizar el pedido: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // CONSULTAR HISTORIAL
    // ==========================================

    private fun consultarHistorial(pedido: Pedido) {

        try {

            val historial = pedidoDAO.obtenerHistorialEstados(
                numeroPedido = pedido.numeroPedido,
                idTienda = idTienda
            )

            if (historial.isEmpty()) {

                Toast.makeText(
                    this,
                    "Este pedido todavía no tiene historial.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            mostrarHistorial(
                historial,
                pedido.numeroPedido
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No fue posible consultar el historial: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // HISTORIAL PERSONALIZADO
    // ==========================================

    private fun mostrarHistorial(
        historial: List<HistorialEstadoPedido>,
        numeroPedido: String
    ) {

        val (dialogo, contenido) = crearDialogo(
            "Historial del pedido",
            "Pedido #$numeroPedido · Registro de movimientos"
        )

        val scroll = ScrollView(this)

        val lista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        historial.forEachIndexed { index, movimiento ->

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(14),
                    dp(14),
                    dp(14),
                    dp(14)
                )
                background = fondoRedondeado(
                    fondo,
                    14
                )
            }

            val numero = TextView(this).apply {
                text = "MOVIMIENTO ${index + 1}"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(azul)
            }

            val estadoNuevo = TextView(this).apply {
                text = movimiento.estadoNuevo
                textSize = 17f
                setTypeface(null, Typeface.BOLD)
                setTextColor(texto)
                setPadding(0, dp(7), 0, dp(7))
            }

            val anterior = movimiento.estadoAnterior
                ?: "Creación del pedido"

            tarjeta.addView(numero)
            tarjeta.addView(estadoNuevo)

            tarjeta.addView(
                crearTextoDetalle(
                    "Estado anterior: $anterior"
                )
            )

            tarjeta.addView(
                crearTextoDetalle(
                    "Fecha y hora: ${movimiento.fechaHora}"
                )
            )

            tarjeta.addView(
                crearTextoDetalle(
                    "Responsable: ${movimiento.responsable}"
                )
            )

            if (!movimiento.motivo.isNullOrBlank()) {

                tarjeta.addView(
                    crearTextoDetalle(
                        "Motivo: ${movimiento.motivo}"
                    )
                )
            }

            lista.addView(
                tarjeta,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            )
        }

        scroll.addView(lista)

        contenido.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(330)
            )
        )

        val botones = dialogo.findViewById<LinearLayout>(
            R.id.contenedorBotonesDialogo
        )

        agregarBoton(
            botones,
            "Cerrar",
            azul,
            true
        ) {
            dialogo.dismiss()
        }

        dialogo.show()
    }

    private fun crearTextoDetalle(contenido: String): TextView {

        return TextView(this).apply {
            text = contenido
            textSize = 13f
            setTextColor(gris)
            setPadding(0, dp(3), 0, dp(3))
        }
    }

    // ==========================================
    // BOTONES PERSONALIZADOS
    // ==========================================

    private fun agregarBoton(
        contenedor: LinearLayout,
        titulo: String,
        color: Int,
        principal: Boolean,
        accion: () -> Unit
    ) {

        val boton = TextView(this).apply {
            text = titulo
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(
                dp(13),
                dp(12),
                dp(13),
                dp(12)
            )

            if (principal) {
                setTextColor(Color.WHITE)
                background = fondoRedondeado(
                    color,
                    12
                )
            } else {
                setTextColor(color)
                background = fondoRedondeado(
                    Color.WHITE,
                    12
                )
            }

            isClickable = true
            isFocusable = true

            setOnClickListener {
                accion()
            }
        }

        contenedor.addView(
            boton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dp(6)
            }
        )
    }

    // ==========================================
    // COLORES Y FORMAS
    // ==========================================

    private fun fondoRedondeado(
        color: Int,
        radio: Int,
        borde: Int? = null
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radio).toFloat()

            if (borde != null) {
                setStroke(dp(1), borde)
            }
        }
    }

    private fun aclararColor(color: Int): Int {

        return when (color) {
            rojo -> Color.rgb(255, 240, 240)
            else -> Color.rgb(234, 241, 255)
        }
    }

    private fun obtenerColorEstado(estado: String): Int {

        return when (estado) {
            PedidoDAO.ESTADO_PENDIENTE ->
                Color.rgb(190, 130, 0)

            PedidoDAO.ESTADO_ACEPTADO ->
                azul

            PedidoDAO.ESTADO_PREPARACION ->
                Color.rgb(120, 75, 190)

            PedidoDAO.ESTADO_ENVIADO ->
                Color.rgb(0, 130, 150)

            PedidoDAO.ESTADO_ENTREGADO ->
                Color.rgb(35, 145, 85)

            PedidoDAO.ESTADO_CANCELADO ->
                rojo

            else -> gris
        }
    }

    private fun obtenerFondoEstado(estado: String): Int {

        return when (estado) {
            PedidoDAO.ESTADO_PENDIENTE ->
                Color.rgb(255, 247, 224)

            PedidoDAO.ESTADO_ACEPTADO ->
                Color.rgb(234, 241, 255)

            PedidoDAO.ESTADO_PREPARACION ->
                Color.rgb(244, 237, 255)

            PedidoDAO.ESTADO_ENVIADO ->
                Color.rgb(230, 247, 250)

            PedidoDAO.ESTADO_ENTREGADO ->
                Color.rgb(232, 247, 238)

            PedidoDAO.ESTADO_CANCELADO ->
                Color.rgb(255, 239, 239)

            else -> Color.LTGRAY
        }
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}

