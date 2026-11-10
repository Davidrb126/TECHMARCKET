package com.techmarket.app

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.techmarket.app.database.TiendaDAO

class EditarTiendaActivity : Activity() {

    companion object {
        const val EXTRA_ID_TIENDA = "id_tienda"
    }

    private lateinit var tiendaDAO: TiendaDAO

    private lateinit var tvVendedor: TextView
    private lateinit var tvDocumento: TextView

    private lateinit var etNombreTienda: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etSitioWeb: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnVolver: ImageView

    private var idTienda: Long = -1L
    private var logoActual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_tienda)

        tiendaDAO = TiendaDAO(this)

        idTienda = intent.getLongExtra(EXTRA_ID_TIENDA, -1L)

        inicializarVistas()
        configurarEventos()

        if (idTienda <= 0L) {
            Toast.makeText(
                this,
                "No se recibió una tienda válida.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        cargarDatosTienda()
    }

    private fun inicializarVistas() {
        btnVolver = findViewById(R.id.btnVolverEditarTienda)

        tvVendedor = findViewById(R.id.tvVendedorEditar)
        tvDocumento = findViewById(R.id.tvDocumentoEditar)

        etNombreTienda = findViewById(R.id.etNombreTiendaEditar)
        etDescripcion = findViewById(R.id.etDescripcionEditar)
        etDireccion = findViewById(R.id.etDireccionEditar)
        etSitioWeb = findViewById(R.id.etSitioWebEditar)

        btnGuardar = findViewById(R.id.btnGuardarCambiosTienda)
        btnCancelar = findViewById(R.id.btnCancelarEditarTienda)
    }

    private fun configurarEventos() {
        btnVolver.setOnClickListener {
            finish()
        }

        btnCancelar.setOnClickListener {
            finish()
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }
    }

    // ==========================================
    // CARGAR INFORMACIÓN DE LA TIENDA
    // ==========================================

    private fun cargarDatosTienda() {
        try {
            val cursor = tiendaDAO.consultarTiendaPorId(idTienda)

            cursor.use {
                if (!it.moveToFirst()) {
                    Toast.makeText(
                        this,
                        "No se encontró la tienda.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return
                }

                val colNombre =
                    it.getColumnIndexOrThrow("nombre_tienda")

                val colDescripcion =
                    it.getColumnIndexOrThrow("descripcion")

                val colDireccion =
                    it.getColumnIndexOrThrow("direccion")

                val colSitio =
                    it.getColumnIndexOrThrow("sitio_web_redes")

                val colLogo =
                    it.getColumnIndexOrThrow("logo")

                val colDocumento =
                    it.getColumnIndexOrThrow("documento_vendedor")

                val colNombres =
                    it.getColumnIndexOrThrow("nombres_vendedor")

                val colApellidos =
                    it.getColumnIndexOrThrow("apellidos_vendedor")

                val nombre = it.getString(colNombre) ?: ""
                val descripcion = it.getString(colDescripcion) ?: ""
                val direccion = it.getString(colDireccion) ?: ""
                val sitioWeb = it.getString(colSitio) ?: ""

                logoActual = it.getString(colLogo)

                val documento = it.getString(colDocumento) ?: ""
                val nombres = it.getString(colNombres) ?: ""
                val apellidos = it.getString(colApellidos) ?: ""

                etNombreTienda.setText(nombre)
                etDescripcion.setText(descripcion)
                etDireccion.setText(direccion)
                etSitioWeb.setText(sitioWeb)

                tvVendedor.text = "$nombres $apellidos"
                tvDocumento.text = documento
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al consultar la tienda: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    // ==========================================
    // GUARDAR CAMBIOS
    // ==========================================

    private fun guardarCambios() {
        val nombre = etNombreTienda.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val sitioWeb = etSitioWeb.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombreTienda.error = "Ingresa el nombre de la tienda"
            etNombreTienda.requestFocus()
            return
        }

        if (descripcion.isEmpty()) {
            etDescripcion.error = "Ingresa la descripción de la tienda"
            etDescripcion.requestFocus()
            return
        }

        btnGuardar.isEnabled = false

        try {
            val actualizado = tiendaDAO.actualizarTiendaPorId(
                idTienda = idTienda,
                nombreTienda = nombre,
                descripcion = descripcion,
                logo = logoActual,
                direccion = direccion,
                sitioWebRedes = sitioWeb
            )

            if (actualizado) {
                Toast.makeText(
                    this,
                    "Tienda actualizada correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "No fue posible actualizar la tienda.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Error al guardar los cambios: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        } finally {
            btnGuardar.isEnabled = true
        }
    }
}
