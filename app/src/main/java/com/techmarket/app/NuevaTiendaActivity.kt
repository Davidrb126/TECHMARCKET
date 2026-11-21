package com.techmarket.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import com.techmarket.app.database.DatabaseProvider
import com.techmarket.app.database.TiendaDAO

class NuevaTiendaActivity : Activity() {

    private lateinit var spVendedor: Spinner

    private lateinit var etNombreTienda: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etSitioWeb: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnVolver: ImageView

    private lateinit var tiendaDAO: TiendaDAO

    private val vendedores = mutableListOf<VendedorDisponible>()

    private val textoSeleccionInicial = "Seleccione un vendedor"

    private data class VendedorDisponible(
        val id: Long,
        val documento: String,
        val nombres: String,
        val apellidos: String,
        val correo: String
    ) {
        fun etiqueta(): String {
            return "$nombres $apellidos - Documento: $documento"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_tienda)

        tiendaDAO = TiendaDAO(this)

        inicializarVistas()
        configurarEventos()
        cargarVendedoresDisponibles()
    }

    // ==========================================
    // INICIALIZAR VISTAS
    // ==========================================

    private fun inicializarVistas() {

        btnVolver = findViewById(R.id.btnVolverNuevaTienda)

        spVendedor = findViewById(R.id.spVendedor)

        etNombreTienda = findViewById(R.id.etNombreTienda)
        etDescripcion = findViewById(R.id.etDescripcion)
        etDireccion = findViewById(R.id.etDireccion)
        etSitioWeb = findViewById(R.id.etSitioWeb)

        btnGuardar = findViewById(R.id.btnGuardarTienda)
        btnCancelar = findViewById(R.id.btnCancelarTienda)
    }

    // ==========================================
    // CARGAR VENDEDORES ACTIVOS
    // ==========================================

    private fun cargarVendedoresDisponibles() {

        vendedores.clear()

        val opciones = mutableListOf(textoSeleccionInicial)

        val db = DatabaseProvider.obtenerLectura(this)

        val consulta = """
            SELECT
                u.id,
                u.documento,
                u.nombres,
                u.apellidos,
                u.correo
            FROM usuarios u
            WHERE UPPER(TRIM(u.rol)) = 'VENDEDOR'
              AND u.estado = 1
            ORDER BY
                u.nombres COLLATE NOCASE,
                u.apellidos COLLATE NOCASE
        """.trimIndent()

        try {

            db.rawQuery(consulta, null).use { cursor ->

                val colId = cursor.getColumnIndexOrThrow("id")
                val colDocumento = cursor.getColumnIndexOrThrow("documento")
                val colNombres = cursor.getColumnIndexOrThrow("nombres")
                val colApellidos = cursor.getColumnIndexOrThrow("apellidos")
                val colCorreo = cursor.getColumnIndexOrThrow("correo")

                while (cursor.moveToNext()) {

                    val vendedor = VendedorDisponible(
                        id = cursor.getLong(colId),
                        documento = cursor.getString(colDocumento) ?: "",
                        nombres = cursor.getString(colNombres) ?: "",
                        apellidos = cursor.getString(colApellidos) ?: "",
                        correo = cursor.getString(colCorreo) ?: ""
                    )

                    vendedores.add(vendedor)
                    opciones.add(vendedor.etiqueta())
                }
            }

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                opciones
            )

            adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )

            spVendedor.adapter = adapter

            // Siempre iniciar en el elemento de selección,
            // nunca en el primer vendedor de la lista.
            spVendedor.setSelection(0)

            if (vendedores.isEmpty()) {

                btnGuardar.isEnabled = false

                Toast.makeText(
                    this,
                    "No existen vendedores activos para asignar.",
                    Toast.LENGTH_LONG
                ).show()

            } else {
                btnGuardar.isEnabled = true
            }

        } catch (e: Exception) {

            btnGuardar.isEnabled = false

            Toast.makeText(
                this,
                "Error al consultar los vendedores: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // CONFIGURAR EVENTOS
    // ==========================================

    private fun configurarEventos() {

        btnVolver.setOnClickListener {
            finish()
        }

        btnCancelar.setOnClickListener {
            finish()
        }

        btnGuardar.setOnClickListener {
            registrarTienda()
        }
    }

    // ==========================================
    // REGISTRAR TIENDA
    // ==========================================

    private fun registrarTienda() {

        if (vendedores.isEmpty()) {

            Toast.makeText(
                this,
                "No existen vendedores activos para asignar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // La posición 0 corresponde al texto:
        // "Seleccione un vendedor".
        val posicionSeleccionada = spVendedor.selectedItemPosition

        if (posicionSeleccionada <= 0) {

            Toast.makeText(
                this,
                "Selecciona un vendedor responsable.",
                Toast.LENGTH_SHORT
            ).show()

            spVendedor.performClick()
            return
        }

        // Se resta 1 porque la posición 0 es el texto inicial.
        val indiceVendedor = posicionSeleccionada - 1

        if (indiceVendedor !in vendedores.indices) {

            Toast.makeText(
                this,
                "Selecciona un vendedor válido.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val nombreTienda = etNombreTienda.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val sitioWeb = etSitioWeb.text.toString().trim()

        // ======================================
        // VALIDAR CAMPOS OBLIGATORIOS
        // ======================================

        if (nombreTienda.isBlank()) {

            etNombreTienda.error = "Ingresa el nombre de la tienda"
            etNombreTienda.requestFocus()
            return
        }

        if (descripcion.isBlank()) {

            etDescripcion.error = "Ingresa la descripción de la tienda"
            etDescripcion.requestFocus()
            return
        }

        val vendedorSeleccionado = vendedores[indiceVendedor]

        btnGuardar.isEnabled = false

        try {

            val idTienda = tiendaDAO.guardarTienda(
                idUsuario = vendedorSeleccionado.id,
                nombreTienda = nombreTienda,
                descripcion = descripcion,
                logo = null,
                direccion = direccion.ifBlank { null },
                sitioWebRedes = sitioWeb.ifBlank { null }
            )

            if (idTienda <= 0L) {
                throw IllegalStateException(
                    "No fue posible registrar la tienda."
                )
            }

            Toast.makeText(
                this,
                "Tienda registrada y asignada correctamente.",
                Toast.LENGTH_LONG
            ).show()

            setResult(Activity.RESULT_OK)
            finish()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                e.message ?: "Ocurrió un error durante el registro.",
                Toast.LENGTH_LONG
            ).show()

        } finally {

            btnGuardar.isEnabled = true
        }
    }
}