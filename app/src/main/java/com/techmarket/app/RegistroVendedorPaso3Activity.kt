package com.techmarket.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.TiendaDAO
import com.techmarket.app.database.UsuarioDAO

class RegistroVendedorPaso3Activity : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvDocumento: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvTelefono: TextView

    private lateinit var tvNombreTienda: TextView
    private lateinit var tvDescripcionTienda: TextView
    private lateinit var tvDireccionTienda: TextView
    private lateinit var tvSitioWebTienda: TextView

    private lateinit var imgLogoTienda: ImageView
    private lateinit var btnCrearCuenta: Button
    private lateinit var btnAtras: Button

    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var tiendaDAO: TiendaDAO

    private var registroCorrecto = false

    private var tipoDocumento = ""
    private var documento = ""
    private var nombres = ""
    private var apellidos = ""
    private var correo = ""
    private var telefono = ""
    private var passwordHash = ""

    private var nombreTienda = ""
    private var descripcionTienda = ""
    private var direccionTienda = ""
    private var sitioWebTienda = ""
    private var logoTiendaUri = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_vendedor_paso3)

        usuarioDAO = UsuarioDAO(this)
        tiendaDAO = TiendaDAO(this)

        inicializarComponentes()
        recuperarDatos()
        mostrarResumen()
        configurarEventos()
    }

    private fun inicializarComponentes() {
        tvNombre = findViewById(R.id.tvResumenNombreVendedor)
        tvDocumento = findViewById(R.id.tvResumenDocumentoVendedor)
        tvCorreo = findViewById(R.id.tvResumenCorreoVendedor)
        tvTelefono = findViewById(R.id.tvResumenTelefonoVendedor)

        tvNombreTienda = findViewById(R.id.tvResumenNombreTienda)
        tvDescripcionTienda = findViewById(R.id.tvResumenDescripcionTienda)
        tvDireccionTienda = findViewById(R.id.tvResumenDireccionTienda)
        tvSitioWebTienda = findViewById(R.id.tvResumenSitioWebTienda)

        imgLogoTienda = findViewById(R.id.imgResumenLogoTienda)

        btnCrearCuenta = findViewById(R.id.btnCrearCuentaVendedor)
        btnAtras = findViewById(R.id.btnAtrasVendedorPaso3)
    }

    private fun recuperarDatos() {
        tipoDocumento = intent.getStringExtra("tipo_documento") ?: ""
        documento = intent.getStringExtra("documento") ?: ""
        nombres = intent.getStringExtra("nombres") ?: ""
        apellidos = intent.getStringExtra("apellidos") ?: ""
        correo = intent.getStringExtra("correo") ?: ""
        telefono = intent.getStringExtra("telefono") ?: ""
        passwordHash = intent.getStringExtra("password_hash") ?: ""

        nombreTienda = intent.getStringExtra("nombre_tienda") ?: ""
        descripcionTienda = intent.getStringExtra("descripcion_tienda") ?: ""
        direccionTienda = intent.getStringExtra("direccion_tienda") ?: ""
        sitioWebTienda = intent.getStringExtra("sitio_web_tienda") ?: ""
        logoTiendaUri = intent.getStringExtra("logo_tienda_uri") ?: ""
    }

    private fun mostrarResumen() {
        tvNombre.text = "$nombres $apellidos"
        tvDocumento.text = "$tipoDocumento: $documento"
        tvCorreo.text = correo
        tvTelefono.text = telefono

        tvNombreTienda.text = nombreTienda
        tvDescripcionTienda.text = descripcionTienda

        tvDireccionTienda.text =
            direccionTienda.ifBlank { "No especificada" }

        tvSitioWebTienda.text =
            sitioWebTienda.ifBlank { "No especificado" }

        if (logoTiendaUri.isNotBlank()) {
            try {
                imgLogoTienda.setImageURI(Uri.parse(logoTiendaUri))
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "No fue posible mostrar el logo seleccionado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun configurarEventos() {

        btnAtras.setOnClickListener {
            finish()
        }

        btnCrearCuenta.setOnClickListener {
            registrarVendedor()
        }
    }

    private fun registrarVendedor() {

        if (registroCorrecto) return

        if (
            tipoDocumento.isBlank() ||
            documento.isBlank() ||
            nombres.isBlank() ||
            apellidos.isBlank() ||
            correo.isBlank() ||
            telefono.isBlank() ||
            passwordHash.isBlank() ||
            nombreTienda.isBlank() ||
            descripcionTienda.isBlank()
        ) {
            Toast.makeText(
                this,
                "Faltan datos para completar el registro. Regresa y verifica la información.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        btnCrearCuenta.isEnabled = false

        try {
            // Registrar usuario mediante UsuarioDAO.
            val idUsuario = usuarioDAO.registrarUsuario(
                tipoDocumento = tipoDocumento,
                documento = documento,
                nombres = nombres,
                apellidos = apellidos,
                correo = correo,
                passwordHash = passwordHash,
                rol = "VENDEDOR"
            )

            if (idUsuario <= 0L) {
                throw Exception(
                    "No se pudo crear la cuenta. Es posible que el documento ya esté registrado."
                )
            }

            // Registrar tienda mediante TiendaDAO.
            val idTienda = tiendaDAO.guardarTienda(
                idUsuario = idUsuario,
                nombreTienda = nombreTienda,
                descripcion = descripcionTienda,
                logo = logoTiendaUri.ifBlank { null },
                direccion = direccionTienda.ifBlank { null },
                sitioWebRedes = sitioWebTienda.ifBlank { null }
            )

            if (idTienda <= 0L) {
                throw Exception(
                    "No se pudo guardar la información de la tienda."
                )
            }

            registroCorrecto = true

            Toast.makeText(
                this,
                "¡Tu cuenta de vendedor fue creada correctamente!",
                Toast.LENGTH_LONG
            ).show()

            // Redirigir al inicio de sesión.
            val intentLogin = Intent(
                this,
                MainActivity::class.java
            ).apply {
                putExtra(MainActivity.EXTRA_ABRIR_LOGIN, true)

                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            }

            startActivity(intentLogin)
            finish()

        } catch (e: Exception) {

            btnCrearCuenta.isEnabled = true

            Toast.makeText(
                this,
                e.message ?: "Ocurrió un error al crear la cuenta.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}
