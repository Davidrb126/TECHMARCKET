package com.techmarket.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.CategoriaDAO

class GestionCategoriasActivity : AppCompatActivity() {

    private lateinit var dao: CategoriaDAO
    private lateinit var contenedorCategorias: LinearLayout
    private lateinit var tvTotalCategorias: TextView
    private lateinit var etBuscarCategoria: EditText
    private lateinit var btnNuevaCategoria: Button

    private val categorias = mutableListOf<CategoriaDAO.Categoria>()

    private val azul = Color.rgb(7, 85, 255)
    private val textoPrincipal = Color.rgb(35, 43, 58)
    private val textoSecundario = Color.rgb(119, 119, 119)
    private val fondo = Color.rgb(246, 248, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dao = CategoriaDAO(this)

        construirInterfaz()
        configurarEventos()
        cargarCategorias()
    }

    override fun onResume() {
        super.onResume()

        if (::contenedorCategorias.isInitialized) {
            cargarCategorias()
        }
    }

    // ==========================================
    // CONSTRUIR INTERFAZ
    // ==========================================

    private fun construirInterfaz() {

        val raiz = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(fondo)
        }

        val scroll = ScrollView(this)

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(30), dp(12), dp(24))
        }

        // ENCABEZADO

        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnVolver = TextView(this).apply {
            text = "‹"
            textSize = 34f
            setTextColor(textoPrincipal)
            gravity = Gravity.CENTER
            setPadding(dp(4), 0, dp(14), 0)

            setOnClickListener {
                finish()
            }
        }

        val titulo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        titulo.addView(TextView(this).apply {
            text = "Gestión de categorías"
            textSize = 21f
            setTextColor(textoPrincipal)
            setTypeface(null, Typeface.BOLD)
        })

        titulo.addView(TextView(this).apply {
            text = "Administra las categorías de productos"
            textSize = 12f
            setTextColor(textoSecundario)
        })

        encabezado.addView(btnVolver)
        encabezado.addView(titulo)

        contenido.addView(encabezado)

        // TOTAL DE CATEGORÍAS

        tvTotalCategorias = TextView(this).apply {
            textSize = 14f
            setTextColor(textoSecundario)
            setPadding(0, dp(22), 0, dp(12))
        }

        contenido.addView(tvTotalCategorias)

        // BUSCADOR

        etBuscarCategoria = EditText(this).apply {
            hint = "Buscar categoría..."
            textSize = 14f
            isSingleLine = true
            setPadding(dp(14), 0, dp(14), 0)

            background = fondoRedondeado(
                Color.WHITE,
                Color.rgb(225, 230, 238)
            )
        }

        contenido.addView(
            etBuscarCategoria,
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                bottomMargin = dp(14)
            }
        )

        // BOTÓN NUEVA CATEGORÍA

        btnNuevaCategoria = Button(this).apply {
            text = "+  Nueva categoría"
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            background = fondoRedondeado(azul, azul)
        }

        contenido.addView(
            btnNuevaCategoria,
            LinearLayout.LayoutParams(-1, dp(48)).apply {
                bottomMargin = dp(18)
            }
        )

        // CONTENEDOR DE CATEGORÍAS

        contenedorCategorias = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        contenido.addView(contenedorCategorias)

        scroll.addView(contenido)

        raiz.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        // BARRA INFERIOR

        raiz.addView(crearNavegacionInferior())

        setContentView(raiz)
    }

    // ==========================================
    // NAVEGACIÓN INFERIOR
    // ==========================================

    private fun crearNavegacionInferior(): View {

        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            elevation = dp(8).toFloat()
            setPadding(dp(4), dp(5), dp(4), dp(5))
            minimumHeight = dp(72)
        }

        data class OpcionNavegacion(
            val etiqueta: String,
            val destino: String,
            val icono: Int
        )

        val opciones = listOf(
            OpcionNavegacion(
                "Inicio",
                "inicio",
                R.drawable.ic_nav_inicio
            ),
            OpcionNavegacion(
                "Usuarios",
                "usuarios",
                R.drawable.ic_usuario
            ),
            OpcionNavegacion(
                "Tiendas",
                "tiendas",
                R.drawable.ic_tienda_vendedor
            ),
            OpcionNavegacion(
                "Categorías",
                "categorias",
                R.drawable.ic_nav_categorias
            )
        )

        opciones.forEach { opcion ->

            val seleccionado = opcion.destino == "categorias"

            val color = if (seleccionado) {
                azul
            } else {
                textoSecundario
            }

            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true

                setPadding(
                    dp(2),
                    dp(3),
                    dp(2),
                    dp(3)
                )

                layoutParams = LinearLayout.LayoutParams(
                    0,
                    -1,
                    1f
                )

                setOnClickListener {
                    navegar(opcion.destino)
                }
            }

            // ICONO

            val imagen = ImageView(this).apply {
                setImageResource(opcion.icono)
                setColorFilter(color)
                contentDescription = opcion.etiqueta
                scaleType = ImageView.ScaleType.FIT_CENTER

                layoutParams = LinearLayout.LayoutParams(
                    dp(28),
                    dp(28)
                )
            }

            // TEXTO

            val texto = TextView(this).apply {
                text = opcion.etiqueta
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(color)

                setTypeface(
                    null,
                    if (seleccionado) {
                        Typeface.BOLD
                    } else {
                        Typeface.NORMAL
                    }
                )

                layoutParams = LinearLayout.LayoutParams(
                    -1,
                    -2
                ).apply {
                    topMargin = dp(2)
                }
            }

            item.addView(imagen)
            item.addView(texto)

            barra.addView(item)
        }

        return barra
    }

    // ==========================================
    // EVENTOS
    // ==========================================

    private fun configurarEventos() {

        btnNuevaCategoria.setOnClickListener {
            mostrarFormularioCategoria(null)
        }

        etBuscarCategoria.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    mostrarCategorias(s?.toString().orEmpty())
                }

                override fun afterTextChanged(
                    s: Editable?
                ) = Unit
            }
        )
    }

    // ==========================================
    // NAVEGACIÓN
    // ==========================================

    private fun navegar(destino: String) {

        when (destino) {

            "inicio" -> {
                startActivity(
                    Intent(
                        this,
                        InicioAdministradorActivity::class.java
                    )
                )
                finish()
            }

            "usuarios" -> {
                startActivity(
                    Intent(
                        this,
                        GestionUsuariosActivity::class.java
                    )
                )
                finish()
            }

            "tiendas" -> {
                startActivity(
                    Intent(
                        this,
                        GestionTiendasActivity::class.java
                    )
                )
                finish()
            }

            "categorias" -> {
                cargarCategorias()
            }
        }
    }

    // ==========================================
    // CARGAR CATEGORÍAS
    // ==========================================

    private fun cargarCategorias() {

        try {

            categorias.clear()

            categorias.addAll(
                dao.obtenerCategorias(incluirInactivas = true)
            )

            tvTotalCategorias.text =
                "${categorias.size} ${
                    if (categorias.size == 1) {
                        "categoría registrada"
                    } else {
                        "categorías registradas"
                    }
                }"

            mostrarCategorias(
                etBuscarCategoria.text.toString()
            )

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No fue posible cargar las categorías: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // MOSTRAR CATEGORÍAS
    // ==========================================

    private fun mostrarCategorias(filtro: String) {

        contenedorCategorias.removeAllViews()

        val consulta = filtro.trim()

        val filtradas = categorias.filter {
            it.nombre.contains(consulta, ignoreCase = true) ||
                    it.descripcion.contains(consulta, ignoreCase = true)
        }

        if (filtradas.isEmpty()) {

            val mensaje = if (consulta.isBlank()) {
                "Aún no hay categorías registradas."
            } else {
                "No se encontraron categorías para \"$consulta\"."
            }

            contenedorCategorias.addView(
                TextView(this).apply {
                    text = mensaje
                    textSize = 14f
                    setTextColor(textoSecundario)
                    gravity = Gravity.CENTER

                    setPadding(
                        dp(16),
                        dp(32),
                        dp(16),
                        dp(32)
                    )
                }
            )

            return
        }

        filtradas.forEach { categoria ->
            contenedorCategorias.addView(
                crearTarjetaCategoria(categoria)
            )
        }
    }

    // ==========================================
    // TARJETA DE CATEGORÍA
    // ==========================================

    private fun crearTarjetaCategoria(
        categoria: CategoriaDAO.Categoria
    ): View {

        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16),
                dp(14),
                dp(16),
                dp(14)
            )

            background = fondoRedondeado(
                Color.WHITE,
                Color.rgb(232, 235, 241)
            )
        }

        val filaSuperior = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val informacion = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        }

        informacion.addView(
            TextView(this).apply {
                text = "${categoria.icono}  ${categoria.nombre}"
                textSize = 16f
                setTextColor(textoPrincipal)
                setTypeface(null, Typeface.BOLD)
            }
        )

        if (categoria.descripcion.isNotBlank()) {

            informacion.addView(
                TextView(this).apply {
                    text = categoria.descripcion
                    textSize = 12f
                    setTextColor(textoSecundario)
                    setPadding(0, dp(3), 0, 0)
                }
            )
        }

        // ESTADO

        val estado = TextView(this).apply {

            text = if (categoria.estado) {
                "ACTIVA"
            } else {
                "INACTIVA"
            }

            textSize = 10f
            setTypeface(null, Typeface.BOLD)

            setTextColor(
                if (categoria.estado) {
                    Color.rgb(24, 130, 78)
                } else {
                    Color.rgb(180, 75, 65)
                }
            )

            setPadding(
                dp(9),
                dp(5),
                dp(9),
                dp(5)
            )

            background = fondoRedondeado(
                if (categoria.estado) {
                    Color.rgb(226, 246, 235)
                } else {
                    Color.rgb(253, 235, 232)
                },
                Color.TRANSPARENT
            )
        }

        filaSuperior.addView(informacion)
        filaSuperior.addView(estado)

        tarjeta.addView(filaSuperior)

        // PRODUCTOS ASOCIADOS

        tarjeta.addView(
            TextView(this).apply {
                text = "${categoria.cantidadProductos} productos asociados"
                textSize = 12f
                setTextColor(textoSecundario)

                setPadding(
                    0,
                    dp(10),
                    0,
                    dp(10)
                )
            }
        )

        // ACCIONES

        val acciones = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val editar = TextView(this).apply {
            text = "Editar"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(azul)

            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )

            setOnClickListener {
                mostrarFormularioCategoria(categoria)
            }
        }

        val cambiarEstado = TextView(this).apply {

            text = if (categoria.estado) {
                "Desactivar"
            } else {
                "Activar"
            }

            textSize = 13f
            setTypeface(null, Typeface.BOLD)

            setTextColor(
                if (categoria.estado) {
                    Color.rgb(190, 75, 65)
                } else {
                    Color.rgb(24, 130, 78)
                }
            )

            setPadding(
                dp(12),
                dp(8),
                0,
                dp(8)
            )

            setOnClickListener {
                confirmarCambioEstado(categoria)
            }
        }

        acciones.addView(editar)
        acciones.addView(cambiarEstado)

        tarjeta.addView(acciones)

        return tarjeta.apply {
            layoutParams = LinearLayout.LayoutParams(
                -1,
                -2
            ).apply {
                bottomMargin = dp(12)
            }
        }
    }

    // ==========================================
    // FORMULARIO CREAR / EDITAR
    // ÚNICA SECCIÓN CON CAMBIO VISUAL
    // ==========================================

    private fun mostrarFormularioCategoria(
        categoria: CategoriaDAO.Categoria?
    ) {

        val iconos = listOf(
            "📦", "📱", "🎧", "💻", "🔌",
            "🖥️", "📺", "🎮", "⌚", "📷",
            "🖨️", "🔋", "🎤", "🕹️", "💾",
            "🧩", "🏠", "🚗"
        )

        var iconoSeleccionado = categoria?.icono ?: "📦"

        // CONTENEDOR PRINCIPAL

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(12))
            background = fondoRedondeado(
                Color.WHITE,
                Color.TRANSPARENT
            )
        }


        // TÍTULO

        contenedor.addView(TextView(this).apply {
            text = if (categoria == null) {
                "Nueva categoría"
            } else {
                "Editar categoría"
            }

            textSize = 21f
            setTextColor(textoPrincipal)
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, dp(18))
        })

        // ETIQUETA DEL ICONO

        contenedor.addView(TextView(this).apply {
            text = "Icono de la categoría"
            textSize = 13f
            setTextColor(textoPrincipal)
            setTypeface(null, Typeface.BOLD)
        })

        // FILA DEL ICONO

        val filaIcono = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(18))
        }

        val tvIcono = TextView(this).apply {
            text = iconoSeleccionado
            textSize = 30f
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                dp(58),
                dp(54)
            )

            background = fondoRedondeado(
                Color.rgb(242, 245, 250),
                Color.rgb(225, 230, 238)
            )
        }

        val btnElegirIcono = TextView(this).apply {
            text = "Seleccionar icono"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(textoPrincipal)
            setTypeface(null, Typeface.BOLD)

            background = fondoRedondeado(
                Color.rgb(242, 245, 250),
                Color.rgb(225, 230, 238)
            )

            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                leftMargin = dp(12)
            }

            setOnClickListener {

                val seleccionado = iconos.indexOf(
                    iconoSeleccionado
                ).coerceAtLeast(0)

                AlertDialog.Builder(
                    this@GestionCategoriasActivity
                )
                    .setTitle("Selecciona un icono")
                    .setSingleChoiceItems(
                        iconos.toTypedArray(),
                        seleccionado
                    ) { dialogoIconos, cual ->

                        iconoSeleccionado = iconos[cual]
                        tvIcono.text = iconoSeleccionado

                        dialogoIconos.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        filaIcono.addView(tvIcono)
        filaIcono.addView(btnElegirIcono)
        contenedor.addView(filaIcono)

        // CAMPO NOMBRE

        val etNombre = EditText(this).apply {
            hint = "Nombre de la categoría"
            setText(categoria?.nombre.orEmpty())
            textSize = 14f
            setTextColor(textoPrincipal)
            setHintTextColor(Color.rgb(160, 160, 160))
            isSingleLine = true
            setPadding(dp(14), 0, dp(14), 0)

            background = fondoRedondeado(
                Color.WHITE,
                Color.rgb(225, 230, 238)
            )
        }

        contenedor.addView(
            etNombre,
            LinearLayout.LayoutParams(-1, dp(50)).apply {
                bottomMargin = dp(14)
            }
        )

        // CAMPO DESCRIPCIÓN

        val etDescripcion = EditText(this).apply {
            hint = "Descripción (opcional)"
            setText(categoria?.descripcion.orEmpty())
            textSize = 14f
            setTextColor(textoPrincipal)
            setHintTextColor(Color.rgb(160, 160, 160))
            minLines = 2
            gravity = Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))

            background = fondoRedondeado(
                Color.WHITE,
                Color.rgb(225, 230, 238)
            )
        }

        contenedor.addView(
            etDescripcion,
            LinearLayout.LayoutParams(-1, dp(82))
        )

        // FILA DE BOTONES

        val filaBotones = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(4))
        }

        val btnCancelar = TextView(this).apply {
            text = "Cancelar"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(textoSecundario)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val btnGuardar = TextView(this).apply {
            text = if (categoria == null) {
                "Guardar"
            } else {
                "Guardar cambios"
            }

            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(12), dp(18), dp(12))

            background = fondoRedondeado(azul, azul)
        }

        filaBotones.addView(btnCancelar)
        filaBotones.addView(btnGuardar)
        contenedor.addView(filaBotones)

        // DIÁLOGO PERSONALIZADO

        val dialogo = AlertDialog.Builder(this)
            .setView(contenedor)
            .create()

        btnCancelar.setOnClickListener {
            dialogo.dismiss()
        }

        // GUARDAR CATEGORÍA

        btnGuardar.setOnClickListener {

            val nombre = etNombre.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            if (nombre.isBlank()) {
                etNombre.error = "Ingresa el nombre de la categoría"
                return@setOnClickListener
            }

            val guardado = if (categoria == null) {

                dao.insertarCategoria(
                    nombre,
                    descripcion,
                    iconoSeleccionado
                ) > 0

            } else {

                dao.actualizarCategoria(
                    categoria.id,
                    nombre,
                    descripcion,
                    iconoSeleccionado
                )
            }

            if (guardado) {

                dialogo.dismiss()
                cargarCategorias()

                Toast.makeText(
                    this,
                    if (categoria == null) {
                        "Categoría registrada."
                    } else {
                        "Categoría actualizada."
                    },
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                etNombre.error =
                    "Ya existe una categoría con ese nombre o no se pudo guardar"
            }
        }

        dialogo.show()

        // ANCHO DEL DIÁLOGO

        dialogo.window?.apply {
            setBackgroundDrawableResource(
                android.R.color.transparent
            )

            setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                -2
            )
        }
    }

    // ==========================================
    // ACTIVAR / DESACTIVAR
    // ==========================================

    private fun confirmarCambioEstado(
        categoria: CategoriaDAO.Categoria
    ) {

        val accion = if (categoria.estado) {
            "desactivar"
        } else {
            "activar"
        }

        AlertDialog.Builder(this)
            .setTitle(
                "${accion.replaceFirstChar { it.uppercase() }} categoría"
            )
            .setMessage(
                if (categoria.estado) {
                    "¿Deseas desactivar \"${categoria.nombre}\"? Los productos existentes conservarán su categoría."
                } else {
                    "¿Deseas activar nuevamente \"${categoria.nombre}\"?"
                }
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->

                val resultado = dao.cambiarEstadoCategoria(
                    categoria.id,
                    activa = !categoria.estado
                )

                if (resultado) {

                    cargarCategorias()

                    Toast.makeText(
                        this,
                        if (categoria.estado) {
                            "Categoría desactivada."
                        } else {
                            "Categoría activada."
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "No fue posible cambiar el estado.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    // ==========================================
    // UTILIDADES
    // ==========================================

    private fun fondoRedondeado(
        color: Int,
        borde: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(12).toFloat()

            if (borde != Color.TRANSPARENT) {
                setStroke(dp(1), borde)
            }
        }
    }

    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }
}
