package com.techmarket.app

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import android.util.Patterns
import com.techmarket.app.database.DatabaseProvider
import com.techmarket.app.database.TiendaDAO
import com.techmarket.app.database.UsuarioDAO

class NuevaTiendaActivity : Activity() {

    private lateinit var spTipoDocumento: Spinner

    private lateinit var etDocumento: EditText
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText

    private lateinit var etNombreTienda: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etSitioWeb: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnVolver: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nueva_tienda)

        inicializarVistas()
        configurarSpinner()
        configurarEventos()
    }

    // ==========================================
    // INICIALIZAR VISTAS
    // ==========================================

    private fun inicializarVistas() {

        btnVolver = findViewById(R.id.btnVolverNuevaTienda)

        spTipoDocumento = findViewById(R.id.spTipoDocumento)

        etDocumento = findViewById(R.id.etDocumento)
        etNombres = findViewById(R.id.etNombres)
        etApellidos = findViewById(R.id.etApellidos)
        etCorreo = findViewById(R.id.etCorreo)
        etTelefono = findViewById(R.id.etTelefono)
        etContrasena = findViewById(R.id.etContrasena)
        etConfirmarContrasena =
            findViewById(R.id.etConfirmarContrasena)

        etNombreTienda = findViewById(R.id.etNombreTienda)
        etDescripcion = findViewById(R.id.etDescripcion)
        etDireccion = findViewById(R.id.etDireccion)
        etSitioWeb = findViewById(R.id.etSitioWeb)

        btnGuardar = findViewById(R.id.btnGuardarTienda)
        btnCancelar = findViewById(R.id.btnCancelarTienda)
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
            registrarVendedorYTienda()
        }
    }

    // ==========================================
    // CONFIGURAR TIPO DE DOCUMENTO
    // ==========================================

    private fun configurarSpinner() {

        val tipos = listOf(
            "Cédula de ciudadanía",
            "Cédula de extranjería",
            "Pasaporte"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tipos
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spTipoDocumento.adapter = adapter
    }

    // ==========================================
    // REGISTRAR VENDEDOR Y TIENDA
    // ==========================================

    private fun registrarVendedorYTienda() {

        val tipoDocumento =
            spTipoDocumento.selectedItem.toString()

        val documento =
            etDocumento.text.toString().trim()

        val nombres =
            etNombres.text.toString().trim()

        val apellidos =
            etApellidos.text.toString().trim()

        val correo =
            etCorreo.text.toString().trim()

        val telefono =
            etTelefono.text.toString().trim()

        val contrasena =
            etContrasena.text.toString()

        val confirmar =
            etConfirmarContrasena.text.toString()

        val nombreTienda =
            etNombreTienda.text.toString().trim()

        val descripcion =
            etDescripcion.text.toString().trim()

        val direccion =
            etDireccion.text.toString().trim()

        val sitioWeb =
            etSitioWeb.text.toString().trim()

        // ======================================
        // VALIDAR CAMPOS OBLIGATORIOS
        // ======================================

        if (
            documento.isEmpty() ||
            nombres.isEmpty() ||
            apellidos.isEmpty() ||
            correo.isEmpty() ||
            telefono.isEmpty() ||
            contrasena.isEmpty() ||
            confirmar.isEmpty() ||
            nombreTienda.isEmpty() ||
            descripcion.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Completa todos los campos obligatorios",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // ======================================
        // VALIDAR CORREO
        // ======================================

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {

            etCorreo.error = "Correo electrónico no válido"
            etCorreo.requestFocus()

            return
        }

        // ======================================
        // VALIDAR CONTRASEÑA
        // ======================================

        if (contrasena.length < 6) {

            etContrasena.error =
                "La contraseña debe tener mínimo 6 caracteres"

            etContrasena.requestFocus()

            return
        }

        if (contrasena != confirmar) {

            etConfirmarContrasena.error =
                "Las contraseñas no coinciden"

            etConfirmarContrasena.requestFocus()

            return
        }

        // ======================================
        // PREPARAR DAO Y BASE DE DATOS
        // ======================================

        val usuarioDAO = UsuarioDAO(this)
        val tiendaDAO = TiendaDAO(this)

        val db = DatabaseProvider.obtenerEscritura(this)

        val passwordHash =
            PasswordUtils.generarHashContrasena(contrasena)

        btnGuardar.isEnabled = false

        var transaccionIniciada = false

        try {

            // ==================================
            // INICIAR TRANSACCIÓN
            // ==================================

            db.beginTransaction()
            transaccionIniciada = true

            // ==================================
            // REGISTRAR VENDEDOR
            // ==================================

            val idUsuario = usuarioDAO.registrarUsuario(
                tipoDocumento = tipoDocumento,
                documento = documento,
                nombres = nombres,
                apellidos = apellidos,
                correo = correo,
                passwordHash = passwordHash,
                rol = "VENDEDOR",
                telefono = telefono
            )

            if (idUsuario <= 0L) {

                throw IllegalStateException(
                    "No fue posible registrar el vendedor. Verifica si el documento ya existe."
                )
            }

            // ==================================
            // REGISTRAR TIENDA
            // ==================================

            val idTienda = tiendaDAO.guardarTienda(
                idUsuario = idUsuario,
                nombreTienda = nombreTienda,
                descripcion = descripcion,
                logo = null,
                direccion = direccion,
                sitioWebRedes = sitioWeb
            )

            if (idTienda <= 0L) {

                throw IllegalStateException(
                    "No fue posible registrar la tienda."
                )
            }

            // ==================================
            // CONFIRMAR TRANSACCIÓN
            // ==================================

            db.setTransactionSuccessful()

            Toast.makeText(
                this,
                "Vendedor y tienda registrados correctamente",
                Toast.LENGTH_LONG
            ).show()

            setResult(Activity.RESULT_OK)
            finish()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                e.message
                    ?: "Ocurrió un error durante el registro",
                Toast.LENGTH_LONG
            ).show()

        } finally {

            // ==================================
            // FINALIZAR TRANSACCIÓN
            // ==================================

            if (transaccionIniciada) {
                db.endTransaction()
            }

            btnGuardar.isEnabled = true
        }
    }
}
