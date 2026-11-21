package com.techmarket.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.PedidoPagoDAO
import java.text.NumberFormat
import java.util.Locale

class ResultadoPagoActivity : AppCompatActivity() {

    private lateinit var pedidoPagoDAO: PedidoPagoDAO

    private lateinit var contenedor: LinearLayout
    private lateinit var tvTitulo: TextView
    private lateinit var tvMensaje: TextView
    private lateinit var tvIcono: TextView
    private lateinit var tvMonto: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvPedido: TextView
    private lateinit var tvMetodo: TextView
    private lateinit var tvReferencia: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnAccion: Button
    private lateinit var btnConsultar: Button

    private var externalReference: String? = null
    private var estadoActual: String = ""

    private var montoActual: Long = 0L
    private var numeroPedidoActual: String = ""
    private var metodoPagoActual: String = "No disponible"

    private val modoOscuro: Boolean
        get() {
            val modo = resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK

            return modo == Configuration.UI_MODE_NIGHT_YES
        }

    private val colorFondo: Int
        get() = if (modoOscuro) Color.rgb(18, 18, 18) else Color.WHITE

    private val colorSuperficie: Int
        get() = if (modoOscuro) Color.rgb(30, 30, 30) else Color.WHITE

    private val colorSecundario: Int
        get() = if (modoOscuro) Color.rgb(190, 190, 190)
        else Color.rgb(100, 100, 100)

    private val colorTexto: Int
        get() = if (modoOscuro) Color.WHITE else Color.rgb(30, 30, 30)

    private val colorBorde: Int
        get() = if (modoOscuro) Color.rgb(65, 65, 65)
        else Color.rgb(225, 225, 225)

    private val azulTechMarket = Color.rgb(37, 99, 235)
    private val verde = Color.rgb(22, 163, 74)
    private val rojo = Color.rgb(220, 38, 38)
    private val naranja = Color.rgb(217, 119, 6)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pedidoPagoDAO = PedidoPagoDAO(this)
        PedidoManager.inicializar(this)

        configurarBarrasSistema()
        construirInterfaz()
        procesarEnlace(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        procesarEnlace(intent)
    }

    private fun configurarBarrasSistema() {

        window.statusBarColor = colorFondo
        window.navigationBarColor = colorFondo

        if (!modoOscuro) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            window.decorView.systemUiVisibility = 0
        }
    }

    private fun construirInterfaz() {

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(colorFondo)
            isFillViewport = true
            clipToPadding = false
        }

        contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(24))
            setBackgroundColor(colorFondo)
        }

        // ENCABEZADO

        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(2), 0, dp(16))
        }

        val tvMarca = TextView(this).apply {
            text = "TechMarket"
            textSize = 21f
            setTextColor(colorTexto)
            setTypeface(null, Typeface.BOLD)
        }

        val espacio = View(this)

        val tvBolsa = TextView(this).apply {
            text = "▢"
            textSize = 27f
            setTextColor(colorTexto)
            gravity = Gravity.CENTER
        }

        encabezado.addView(
            tvMarca,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        encabezado.addView(tvBolsa)

        contenedor.addView(encabezado)

        agregarSeparador(contenedor)

        // RESULTADO PRINCIPAL

        val seccionResultado = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(38), dp(8), dp(28))
        }

        tvIcono = TextView(this).apply {
            text = "✓"
            textSize = 43f
            gravity = Gravity.CENTER
            setTextColor(verde)
            typeface = Typeface.DEFAULT_BOLD
            background = circulo(verde)
        }

        seccionResultado.addView(
            tvIcono,
            LinearLayout.LayoutParams(dp(82), dp(82)).apply {
                gravity = Gravity.CENTER
            }
        )

        tvTitulo = TextView(this).apply {
            text = "Verificando tu pago"
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
            setTextColor(colorTexto)
            gravity = Gravity.CENTER
        }

        seccionResultado.addView(
            tvTitulo,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        )

        tvMensaje = TextView(this).apply {
            text = "Estamos consultando el estado de tu transacción."
            textSize = 15f
            setTextColor(colorSecundario)
            gravity = Gravity.CENTER
        }

        seccionResultado.addView(
            tvMensaje,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        )

        tvMonto = TextView(this).apply {
            text = ""
            textSize = 30f
            setTextColor(colorTexto)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        seccionResultado.addView(
            tvMonto,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(16)
            }
        )

        tvEstado = TextView(this).apply {
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(13), dp(7), dp(13), dp(7))
        }

        seccionResultado.addView(
            tvEstado,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                topMargin = dp(10)
            }
        )

        contenedor.addView(seccionResultado)

        // INFORMACIÓN DEL PEDIDO

        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(17), dp(17), dp(17))
            background = tarjetaRedondeada(colorSuperficie, colorBorde)
        }

        agregarDato(tarjeta, "Pedido", "")
        tvPedido = (tarjeta.getChildAt(0) as LinearLayout)
            .getChildAt(1) as TextView

        agregarDato(tarjeta, "Medio de pago", "")
        tvMetodo = (tarjeta.getChildAt(1) as LinearLayout)
            .getChildAt(1) as TextView

        agregarDato(tarjeta, "Referencia", "")
        tvReferencia = (tarjeta.getChildAt(2) as LinearLayout)
            .getChildAt(1) as TextView

        contenedor.addView(
            tarjeta,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // INDICADOR DE CONSULTA

        progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
            indeterminateTintList =
                android.content.res.ColorStateList.valueOf(azulTechMarket)
        }

        contenedor.addView(
            progressBar,
            LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                gravity = Gravity.CENTER
                topMargin = dp(24)
            }
        )

        // BOTONES

        btnConsultar = crearBoton(
            "CONSULTAR NUEVAMENTE",
            azulTechMarket
        ).apply {
            visibility = View.GONE
            setOnClickListener {
                consultarEstadoPago()
            }
        }

        contenedor.addView(
            btnConsultar,
            parametrosBoton(dp(20))
        )

        btnAccion = crearBoton(
            "VER DETALLE DE MI COMPRA",
            azulTechMarket
        ).apply {
            visibility = View.GONE
            setOnClickListener {
                ejecutarAccionPrincipal()
            }
        }

        contenedor.addView(
            btnAccion,
            parametrosBoton(dp(12))
        )

        val tvAgradecimiento = TextView(this).apply {
            text = "Gracias por comprar en TechMarket"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(colorSecundario)
        }

        contenedor.addView(
            tvAgradecimiento,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        )

        scrollView.addView(contenedor)
        setContentView(scrollView)
    }

    private fun procesarEnlace(intent: Intent?) {

        val referencia = intent?.data
            ?.getQueryParameter("external_reference")

        if (referencia.isNullOrBlank()) {
            mostrarError(
                "No fue posible identificar la referencia del pago. " +
                        "Regresa a la aplicación e intenta consultar nuevamente."
            )
            return
        }

        externalReference = referencia

        cargarInformacionLocal(referencia)
        consultarEstadoPago()
    }

    private fun cargarInformacionLocal(referencia: String) {

        val pago = pedidoPagoDAO.obtenerPagoPorReferencia(referencia)

        if (pago == null) return

        montoActual = pago.monto
        numeroPedidoActual = pago.numeroPedido

        val pedido = PedidoManager.obtenerPedidos()
            .firstOrNull { it.numeroPedido == pago.numeroPedido }

        metodoPagoActual = pedido?.metodoPago ?: "Pago electrónico"

        tvPedido.text = numeroPedidoActual
        tvMetodo.text = metodoPagoActual
        tvReferencia.text = referencia
        tvMonto.text = formatearPrecio(montoActual)
    }

    private fun consultarEstadoPago() {

        val referencia = externalReference

        if (referencia.isNullOrBlank()) {
            mostrarError("No se encontró la referencia de la transacción.")
            return
        }

        progressBar.visibility = View.VISIBLE
        btnConsultar.visibility = View.GONE
        btnAccion.visibility = View.GONE

        tvTitulo.text = "Verificando tu pago"
        tvMensaje.text =
            "Estamos consultando el estado confirmado por Mercado Pago."

        MercadoPagoApi.consultarEstado(
            referencia,
            object : MercadoPagoApi.Callback<MercadoPagoApi.EstadoPago> {

                override fun onSuccess(result: MercadoPagoApi.EstadoPago) {

                    runOnUiThread {

                        progressBar.visibility = View.GONE

                        estadoActual = result.estado.uppercase()

                        montoActual = result.monto
                        numeroPedidoActual = result.numeroPedido

                        pedidoPagoDAO.actualizarEstado(
                            referencia,
                            estadoActual,
                            result.paymentId
                        )

                        cargarInformacionLocal(referencia)
                        mostrarResultado()
                    }
                }

                override fun onError(message: String) {

                    runOnUiThread {

                        progressBar.visibility = View.GONE

                        tvTitulo.text = "No pudimos verificar el pago"
                        tvMensaje.text =
                            "No fue posible consultar el estado de la transacción. " +
                                    "Esto no significa que el pago haya sido rechazado."

                        tvIcono.visibility = View.GONE
                        tvMonto.visibility = View.GONE
                        tvEstado.visibility = View.GONE

                        btnConsultar.visibility = View.VISIBLE
                        btnAccion.text = "VOLVER A LA COMPRA"
                        btnAccion.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    private fun mostrarResultado() {

        tvIcono.visibility = View.VISIBLE
        tvMonto.visibility = View.VISIBLE
        tvEstado.visibility = View.VISIBLE

        when (estadoActual) {

            "APROBADO" -> {

                tvIcono.text = "✓"
                tvIcono.setTextColor(verde)
                tvIcono.background = circulo(verde)

                tvTitulo.text = "¡Pago aprobado!"
                tvTitulo.setTextColor(verde)

                tvMensaje.text =
                    "Tu pago fue confirmado correctamente por Mercado Pago."

                tvEstado.text = "Transacción acreditada"
                tvEstado.setTextColor(verde)
                tvEstado.background = etiqueta(verde)

                btnAccion.text = "VER DETALLE DE MI COMPRA"
                btnAccion.visibility = View.VISIBLE
                btnConsultar.visibility = View.GONE
            }

            "RECHAZADO" -> {

                tvIcono.text = "×"
                tvIcono.setTextColor(rojo)
                tvIcono.background = circulo(rojo)

                tvTitulo.text = "¡Pago rechazado!"
                tvTitulo.setTextColor(rojo)

                tvMensaje.text =
                    "No fue posible aprobar tu pago. " +
                            "Puedes regresar e intentar nuevamente."

                tvEstado.text = "Transacción rechazada"
                tvEstado.setTextColor(rojo)
                tvEstado.background = etiqueta(rojo)

                btnAccion.text = "VOLVER A LA COMPRA"
                btnAccion.visibility = View.VISIBLE
                btnConsultar.visibility = View.VISIBLE
            }

            "CANCELADO", "EXPIRADO" -> {

                tvIcono.text = "!"
                tvIcono.setTextColor(naranja)
                tvIcono.background = circulo(naranja)

                tvTitulo.text = "Pago no completado"
                tvTitulo.setTextColor(naranja)

                tvMensaje.text =
                    "La transacción no se completó. " +
                            "Puedes regresar a la compra para intentarlo nuevamente."

                tvEstado.text = "Transacción no completada"
                tvEstado.setTextColor(naranja)
                tvEstado.background = etiqueta(naranja)

                btnAccion.text = "VOLVER A LA COMPRA"
                btnAccion.visibility = View.VISIBLE
                btnConsultar.visibility = View.VISIBLE
            }

            else -> {

                tvIcono.text = "…"
                tvIcono.setTextColor(naranja)
                tvIcono.background = circulo(naranja)

                tvTitulo.text = "Pago pendiente"
                tvTitulo.setTextColor(naranja)

                tvMensaje.text =
                    "Todavía no recibimos la confirmación definitiva. " +
                            "Puedes consultar nuevamente en unos momentos."

                tvEstado.text = "Esperando confirmación"
                tvEstado.setTextColor(naranja)
                tvEstado.background = etiqueta(naranja)

                btnAccion.text = "VOLVER A LA COMPRA"
                btnAccion.visibility = View.VISIBLE
                btnConsultar.visibility = View.VISIBLE
            }
        }
    }

    private fun mostrarError(mensaje: String) {

        progressBar.visibility = View.GONE

        tvIcono.visibility = View.GONE
        tvMonto.visibility = View.GONE
        tvEstado.visibility = View.GONE

        tvTitulo.text = "No se pudo identificar el pago"
        tvTitulo.setTextColor(colorTexto)

        tvMensaje.text = mensaje

        btnConsultar.visibility = View.GONE
        btnAccion.text = "VOLVER A LA COMPRA"
        btnAccion.visibility = View.VISIBLE
    }

    private fun ejecutarAccionPrincipal() {

        if (estadoActual == "APROBADO") {

            CarritoManager.vaciar()
            DatosEntregaManager.limpiar()
            MetodoPagoManager.limpiar()

            val intent = Intent(
                this,
                ConfirmacionCompraActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)
            finish()

        } else {

            val intent = Intent(
                this,
                FinalizarCompraActivity::class.java
            )

            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

            startActivity(intent)
            finish()
        }
    }

    // ==================================================
    // COMPONENTES VISUALES
    // ==================================================

    private fun agregarDato(
        tarjeta: LinearLayout,
        etiqueta: String,
        valor: String
    ) {

        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(9), 0, dp(9))
        }

        val tvEtiqueta = TextView(this).apply {
            text = etiqueta
            textSize = 14f
            setTextColor(colorSecundario)
        }

        val tvValor = TextView(this).apply {
            text = valor
            textSize = 14f
            setTextColor(colorTexto)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.END
        }

        fila.addView(
            tvEtiqueta,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        fila.addView(
            tvValor,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.5f
            )
        )

        tarjeta.addView(fila)
    }

    private fun crearBoton(
        texto: String,
        color: Int
    ): Button {

        return Button(this).apply {
            text = texto
            textSize = 14f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            background = tarjetaRedondeada(color, color)
            elevation = dp(2).toFloat()
            minHeight = dp(54)
        }
    }

    private fun parametrosBoton(
        margenSuperior: Int
    ): LinearLayout.LayoutParams {

        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(56)
        ).apply {
            topMargin = margenSuperior
        }
    }

    private fun agregarSeparador(
        contenedor: LinearLayout
    ) {

        val separador = View(this).apply {
            setBackgroundColor(colorBorde)
        }

        contenedor.addView(
            separador,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )
    }

    // ==================================================
    // ESTILOS
    // ==================================================

    private fun circulo(color: Int): GradientDrawable {

        val fondo = when (color) {
            verde -> Color.rgb(220, 252, 231)
            rojo -> Color.rgb(254, 226, 226)
            else -> Color.rgb(254, 243, 199)
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fondo)
        }
    }

    private fun etiqueta(color: Int): GradientDrawable {

        val fondo = when (color) {
            verde -> Color.rgb(220, 252, 231)
            rojo -> Color.rgb(254, 226, 226)
            else -> Color.rgb(254, 243, 199)
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(30).toFloat()
            setColor(
                if (modoOscuro) {
                    Color.rgb(
                        Color.red(color) / 5,
                        Color.green(color) / 5,
                        Color.blue(color) / 5
                    )
                } else {
                    fondo
                }
            )
        }
    }

    private fun tarjetaRedondeada(
        fondo: Int,
        borde: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(15).toFloat()
            setColor(fondo)
            setStroke(dp(1), borde)
        }
    }

    // ==================================================
    // UTILIDADES
    // ==================================================

    private fun formatearPrecio(valor: Long): String {

        val formato = NumberFormat.getCurrencyInstance(
            Locale("es", "CO")
        )

        formato.maximumFractionDigits = 0
        formato.minimumFractionDigits = 0

        return formato.format(valor)
            .replace("COP", "")
            .trim()
    }

    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }
}
