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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.TiendaDAO

class GestionTiendasActivity : AppCompatActivity() {

    private lateinit var tiendaDAO: TiendaDAO

    private lateinit var contenedorTiendas: LinearLayout
    private lateinit var tvTotalTiendas: TextView
    private lateinit var etBuscarTienda: EditText

    private val tiendas = mutableListOf<TiendaItem>()

    // ==========================================
    // MODELO DE TIENDA
    // ==========================================

    private data class TiendaItem(
        val id: Long,
        val idUsuario: Long,
        val nombre: String,
        val descripcion: String,
        val logo: String?,
        val direccion: String?,
        val sitioWeb: String?,
        val estado: Int,
        val documentoVendedor: String,
        val nombresVendedor: String,
        val apellidosVendedor: String,
        val correoVendedor: String
    )

    // ==========================================
    // CICLO DE VIDA
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_tiendas)

        tiendaDAO = TiendaDAO(this)

        contenedorTiendas = findViewById(R.id.contenedorTiendas)
        tvTotalTiendas = findViewById(R.id.tvTotalTiendas)
        etBuscarTienda = findViewById(R.id.etBuscarTienda)

        configurarEventos()
        cargarTiendas()
    }

    override fun onResume() {
        super.onResume()

        if (::tiendaDAO.isInitialized) {
            cargarTiendas()
        }
    }

    // ==========================================
    // EVENTOS
    // ==========================================

    private fun configurarEventos() {

        // VOLVER

        findViewById<ImageView>(R.id.btnVolverTiendas)
            .setOnClickListener {
                finish()
            }

        // NUEVA TIENDA

        findViewById<Button>(R.id.btnNuevaTiendaAdmin)
            .setOnClickListener {

                val intent = Intent(
                    this,
                    NuevaTiendaActivity::class.java
                )

                startActivityForResult(intent, 1001)
            }

        // BUSCADOR

        etBuscarTienda.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filtrarTiendas(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // NAVEGACIÓN INFERIOR

        findViewById<View>(R.id.navInicioAdmin)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        InicioAdministradorActivity::class.java
                    )
                )

                finish()
            }

        findViewById<View>(R.id.navUsuariosAdmin)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        GestionUsuariosActivity::class.java
                    )
                )

                finish()
            }

        findViewById<View>(R.id.navTiendasAdmin)
            .setOnClickListener {
                cargarTiendas()
            }

        findViewById<View>(R.id.navCategoriasAdmin)
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Gestión de categorías próximamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ==========================================
    // RESULTADO DE NUEVA TIENDA
    // ==========================================

    @Deprecated("Se conserva compatibilidad con el flujo actual de Activities")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == 1001 &&
            resultCode == RESULT_OK
        ) {
            cargarTiendas()
        }
    }

    // ==========================================
    // CARGAR TIENDAS DESDE SQLITE
    // ==========================================

    private fun cargarTiendas() {

        tiendas.clear()

        try {

            val cursor = tiendaDAO.consultarTodasLasTiendas()

            cursor.use {

                val colId =
                    it.getColumnIndexOrThrow("id_tienda")

                val colIdUsuario =
                    it.getColumnIndexOrThrow("id_usuario")

                val colNombre =
                    it.getColumnIndexOrThrow("nombre_tienda")

                val colDescripcion =
                    it.getColumnIndexOrThrow("descripcion")

                val colLogo =
                    it.getColumnIndexOrThrow("logo")

                val colDireccion =
                    it.getColumnIndexOrThrow("direccion")

                val colSitio =
                    it.getColumnIndexOrThrow("sitio_web_redes")

                val colEstado =
                    it.getColumnIndexOrThrow("estado")

                val colDocumento =
                    it.getColumnIndexOrThrow("documento_vendedor")

                val colNombres =
                    it.getColumnIndexOrThrow("nombres_vendedor")

                val colApellidos =
                    it.getColumnIndexOrThrow("apellidos_vendedor")

                val colCorreo =
                    it.getColumnIndexOrThrow("correo_vendedor")

                while (it.moveToNext()) {

                    tiendas.add(
                        TiendaItem(
                            id = it.getLong(colId),
                            idUsuario = it.getLong(colIdUsuario),
                            nombre = it.getString(colNombre) ?: "",
                            descripcion = it.getString(colDescripcion) ?: "",
                            logo = it.getString(colLogo),
                            direccion = it.getString(colDireccion),
                            sitioWeb = it.getString(colSitio),
                            estado = it.getInt(colEstado),
                            documentoVendedor =
                                it.getString(colDocumento) ?: "",
                            nombresVendedor =
                                it.getString(colNombres) ?: "",
                            apellidosVendedor =
                                it.getString(colApellidos) ?: "",
                            correoVendedor =
                                it.getString(colCorreo) ?: ""
                        )
                    )
                }
            }

            tvTotalTiendas.text =
                "Tiendas registradas: ${tiendas.size}"

            filtrarTiendas(etBuscarTienda.text.toString())

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Error al cargar las tiendas: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // BUSCADOR
    // ==========================================

    private fun filtrarTiendas(texto: String) {

        val busqueda = texto.trim().lowercase()

        val resultado = tiendas.filter { tienda ->

            tienda.nombre.lowercase().contains(busqueda) ||
                    tienda.documentoVendedor.lowercase()
                        .contains(busqueda) ||
                    tienda.nombresVendedor.lowercase()
                        .contains(busqueda) ||
                    tienda.apellidosVendedor.lowercase()
                        .contains(busqueda) ||
                    tienda.correoVendedor.lowercase()
                        .contains(busqueda)
        }

        mostrarTiendas(resultado)
    }

    // ==========================================
    // MOSTRAR TARJETAS
    // ==========================================

    private fun mostrarTiendas(lista: List<TiendaItem>) {

        contenedorTiendas.removeAllViews()

        if (lista.isEmpty()) {

            val mensaje = TextView(this).apply {

                text = if (tiendas.isEmpty()) {
                    "No hay tiendas registradas."
                } else {
                    "No se encontraron tiendas."
                }

                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(12, 35, 12, 35)
            }

            contenedorTiendas.addView(mensaje)
            return
        }

        lista.forEach { tienda ->

            contenedorTiendas.addView(
                crearTarjetaTienda(tienda)
            )
        }
    }

    // ==========================================
    // CREAR TARJETA DE TIENDA
    // ==========================================

    private fun crearTarjetaTienda(tienda: TiendaItem): View {

        val tarjeta = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)

            background = crearFondo(
                Color.WHITE,
                16f
            )

            elevation = 3f
        }

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(14)
        }

        // NOMBRE DE LA TIENDA

        val nombre = TextView(this).apply {

            text = tienda.nombre
            textSize = 17f

            setTextColor(
                Color.rgb(23, 43, 91)
            )

            setTypeface(null, Typeface.BOLD)
        }

        tarjeta.addView(nombre)

        // ESTADO

        val estado = TextView(this).apply {

            text = if (tienda.estado == 1) {
                "ACTIVA"
            } else {
                "INACTIVA"
            }

            textSize = 11f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                if (tienda.estado == 1) {
                    Color.rgb(20, 135, 75)
                } else {
                    Color.rgb(190, 55, 55)
                }
            )

            setPadding(
                0,
                dp(5),
                0,
                dp(8)
            )
        }

        tarjeta.addView(estado)

        // DATOS DEL VENDEDOR

        tarjeta.addView(
            crearTexto(
                "Vendedor: ${tienda.nombresVendedor} ${tienda.apellidosVendedor}"
            )
        )

        tarjeta.addView(
            crearTexto(
                "Documento: ${tienda.documentoVendedor}"
            )
        )

        tarjeta.addView(
            crearTexto(
                "Correo: ${tienda.correoVendedor}"
            )
        )

        // DESCRIPCIÓN

        if (tienda.descripcion.isNotBlank()) {

            tarjeta.addView(
                crearTexto(
                    "Descripción: ${tienda.descripcion}"
                )
            )
        }

        // DIRECCIÓN

        if (!tienda.direccion.isNullOrBlank()) {

            tarjeta.addView(
                crearTexto(
                    "Dirección: ${tienda.direccion}"
                )
            )
        }

        // ======================================
        // BOTONES DE ACCIÓN
        // ======================================

        val filaBotones = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                0,
                dp(12),
                0,
                0
            )
        }

        // BOTÓN ACTIVAR / DESACTIVAR

        val btnEstado = Button(this).apply {

            text = if (tienda.estado == 1) {
                "DESACTIVAR"
            } else {
                "ACTIVAR"
            }

            textSize = 14f
            setTextColor(Color.WHITE)

            backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    if (tienda.estado == 1) {
                        Color.rgb(210, 70, 70)
                    } else {
                        Color.rgb(20, 135, 75)
                    }
                )

            setOnClickListener {
                confirmarCambioEstado(tienda)
            }
        }

        filaBotones.addView(
            btnEstado,
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            ).apply {
                marginEnd = dp(6)
            }
        )

        // BOTÓN EDITAR

        val btnEditar = Button(this).apply {

            text = "EDITAR"
            textSize = 14f
            setTextColor(Color.WHITE)

            backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.rgb(10, 84, 213)
                )

            setOnClickListener {

                val intent = Intent(
                    this@GestionTiendasActivity,
                    EditarTiendaActivity::class.java
                )

                intent.putExtra(
                    EditarTiendaActivity.EXTRA_ID_TIENDA,
                    tienda.id
                )

                startActivity(intent)
            }
        }

        filaBotones.addView(
            btnEditar,
            LinearLayout.LayoutParams(
                0,
                dp(44),
                1f
            ).apply {
                marginStart = dp(6)
            }
        )

        tarjeta.addView(filaBotones)

        tarjeta.layoutParams = parametros

        return tarjeta
    }

    // ==========================================
    // CAMBIAR ESTADO DE TIENDA
    // ==========================================

    private fun confirmarCambioEstado(tienda: TiendaItem) {

        val nuevoEstado = if (tienda.estado == 1) {
            0
        } else {
            1
        }

        val accion = if (nuevoEstado == 1) {
            "activar"
        } else {
            "desactivar"
        }

        AlertDialog.Builder(this)
            .setTitle("Cambiar estado de tienda")
            .setMessage(
                "¿Deseas $accion la tienda \"${tienda.nombre}\"?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->

                try {

                    val actualizado =
                        tiendaDAO.cambiarEstadoTienda(
                            tienda.id,
                            nuevoEstado
                        )

                    if (actualizado) {

                        Toast.makeText(
                            this,
                            "Estado de la tienda actualizado.",
                            Toast.LENGTH_SHORT
                        ).show()

                        cargarTiendas()

                    } else {

                        Toast.makeText(
                            this,
                            "No fue posible actualizar el estado.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {

                    Toast.makeText(
                        this,
                        "Error al cambiar el estado: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    // ==========================================
    // COMPONENTES VISUALES
    // ==========================================

    private fun crearTexto(contenido: String): TextView {

        return TextView(this).apply {

            text = contenido
            textSize = 13f

            setTextColor(
                Color.rgb(80, 88, 105)
            )

            setPadding(
                0,
                dp(4),
                0,
                dp(4)
            )
        }
    }

    // ==========================================
    // FONDO DE TARJETAS
    // ==========================================

    private fun crearFondo(
        color: Int,
        radio: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)
            cornerRadius = dp(radio)
        }
    }

    // ==========================================
    // CONVERSIÓN DP
    // ==========================================

    private fun dp(valor: Int): Int {

        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    private fun dp(valor: Float): Float {

        return valor * resources.displayMetrics.density
    }
}
