package com.techmarket.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class RegistroVendedorPaso2Activity : AppCompatActivity() {

    private lateinit var imgLogoTienda: ImageView
    private lateinit var btnSeleccionarLogo: ImageView
    private lateinit var etNombreTienda: EditText
    private lateinit var etDescripcionTienda: EditText
    private lateinit var etDireccionTienda: EditText
    private lateinit var etSitioWebTienda: EditText
    private lateinit var btnAtras: Button
    private lateinit var btnSiguiente: Button

    private var logoTiendaUri: Uri? = null

    companion object {
        private const val KEY_LOGO = "logo_tienda_uri"
        private const val KEY_NOMBRE = "nombre_tienda"
        private const val KEY_DESCRIPCION = "descripcion_tienda"
        private const val KEY_DIRECCION = "direccion_tienda"
        private const val KEY_SITIO_WEB = "sitio_web_tienda"
    }

    private val selectorImagen = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Se conserva el permiso temporal si no es posible persistirlo.
            }

            logoTiendaUri = uri
            imgLogoTienda.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_vendedor_paso2)

        inicializarComponentes()
        restaurarDatos(savedInstanceState)
        configurarEventos()
    }

    private fun inicializarComponentes() {
        imgLogoTienda = findViewById(R.id.imgLogoTiendaVendedor)
        btnSeleccionarLogo = findViewById(R.id.btnSeleccionarLogoTienda)
        etNombreTienda = findViewById(R.id.etNombreTiendaVendedor)
        etDescripcionTienda = findViewById(R.id.etDescripcionTiendaVendedor)
        etDireccionTienda = findViewById(R.id.etDireccionTiendaVendedor)
        etSitioWebTienda = findViewById(R.id.etSitioWebTiendaVendedor)
        btnAtras = findViewById(R.id.btnAtrasVendedorPaso2)
        btnSiguiente = findViewById(R.id.btnSiguienteVendedorPaso2)
    }

    private fun restaurarDatos(savedInstanceState: Bundle?) {

        // Primero recuperamos los datos enviados desde el paso 1.
        etNombreTienda.setText(intent.getStringExtra(KEY_NOMBRE) ?: "")
        etDescripcionTienda.setText(intent.getStringExtra(KEY_DESCRIPCION) ?: "")
        etDireccionTienda.setText(intent.getStringExtra(KEY_DIRECCION) ?: "")
        etSitioWebTienda.setText(intent.getStringExtra(KEY_SITIO_WEB) ?: "")

        // Si la actividad fue recreada, prevalecen los datos guardados.
        if (savedInstanceState != null) {
            etNombreTienda.setText(
                savedInstanceState.getString(KEY_NOMBRE, "")
            )
            etDescripcionTienda.setText(
                savedInstanceState.getString(KEY_DESCRIPCION, "")
            )
            etDireccionTienda.setText(
                savedInstanceState.getString(KEY_DIRECCION, "")
            )
            etSitioWebTienda.setText(
                savedInstanceState.getString(KEY_SITIO_WEB, "")
            )
        }

        // Recuperamos el logo desde el estado guardado o desde el Intent.
        val uriGuardada = savedInstanceState?.getString(KEY_LOGO)
            ?: intent.getStringExtra(KEY_LOGO)

        if (!uriGuardada.isNullOrBlank()) {
            try {
                logoTiendaUri = Uri.parse(uriGuardada)
                imgLogoTienda.setImageURI(logoTiendaUri)
            } catch (_: Exception) {
                logoTiendaUri = null
            }
        }
    }

    private fun configurarEventos() {

        btnSeleccionarLogo.setOnClickListener {
            selectorImagen.launch(arrayOf("image/*"))
        }

        imgLogoTienda.setOnClickListener {
            selectorImagen.launch(arrayOf("image/*"))
        }

        btnAtras.setOnClickListener {
            devolverDatosPaso1()
        }

        btnSiguiente.setOnClickListener {
            validarDatosTienda()
        }
    }

    private fun devolverDatosPaso1() {

        // Enviamos los datos personales y los datos actuales de la tienda.
        val resultado = Intent().apply {
            putExtras(this@RegistroVendedorPaso2Activity.intent)

            putExtra(KEY_NOMBRE, etNombreTienda.text.toString().trim())
            putExtra(KEY_DESCRIPCION, etDescripcionTienda.text.toString().trim())
            putExtra(KEY_DIRECCION, etDireccionTienda.text.toString().trim())
            putExtra(KEY_SITIO_WEB, etSitioWebTienda.text.toString().trim())
            putExtra(KEY_LOGO, logoTiendaUri?.toString() ?: "")
        }

        setResult(RESULT_OK, resultado)
        finish()
    }

    private fun validarDatosTienda() {

        val nombreTienda = etNombreTienda.text.toString().trim()
        val descripcion = etDescripcionTienda.text.toString().trim()
        val direccion = etDireccionTienda.text.toString().trim()
        val sitioWeb = etSitioWebTienda.text.toString().trim()

        if (nombreTienda.isBlank()) {
            etNombreTienda.error = "Ingresa el nombre de tu tienda"
            etNombreTienda.requestFocus()
            return
        }

        if (descripcion.isBlank()) {
            etDescripcionTienda.error = "Describe tu tienda"
            etDescripcionTienda.requestFocus()
            return
        }

        // Pasamos los datos personales y los datos de la tienda al paso 3.
        val intentPaso3 = Intent(
            this,
            RegistroVendedorPaso3Activity::class.java
        ).apply {

            putExtras(this@RegistroVendedorPaso2Activity.intent)

            putExtra(KEY_NOMBRE, nombreTienda)
            putExtra(KEY_DESCRIPCION, descripcion)
            putExtra(KEY_DIRECCION, direccion)
            putExtra(KEY_SITIO_WEB, sitioWeb)
            putExtra(KEY_LOGO, logoTiendaUri?.toString() ?: "")
        }

        startActivity(intentPaso3)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(KEY_NOMBRE, etNombreTienda.text.toString())
        outState.putString(KEY_DESCRIPCION, etDescripcionTienda.text.toString())
        outState.putString(KEY_DIRECCION, etDireccionTienda.text.toString())
        outState.putString(KEY_SITIO_WEB, etSitioWebTienda.text.toString())
        outState.putString(KEY_LOGO, logoTiendaUri?.toString())
    }
}
