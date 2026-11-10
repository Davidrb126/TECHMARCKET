package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class RegistroVendedorActivity : AppCompatActivity() {

    private lateinit var spTipoDocumento: Spinner
    private lateinit var etDocumento: EditText
    private lateinit var etNombreCompleto: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText
    private lateinit var btnSiguiente: Button
    private lateinit var tvIniciarSesion: TextView
    private lateinit var btnVolverRegistroVendedor: ImageButton

    // Datos recuperados del paso 2.
    private var datosPaso2: Intent? = null

    // Recibir los datos cuando el paso 2 regrese al paso 1.
    private val resultadoPaso2 = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            datosPaso2 = resultado.data
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_vendedor_paso1)

        inicializarComponentes()
        configurarTiposDocumento()
        configurarEventos()
    }

    private fun inicializarComponentes() {
        spTipoDocumento = findViewById(R.id.spTipoDocumentoVendedor)
        etDocumento = findViewById(R.id.etDocumentoVendedor)
        etNombreCompleto = findViewById(R.id.etNombreCompletoVendedor)
        etCorreo = findViewById(R.id.etCorreoVendedor)
        etTelefono = findViewById(R.id.etTelefonoVendedor)
        etContrasena = findViewById(R.id.etContrasenaVendedor)
        etConfirmarContrasena =
            findViewById(R.id.etConfirmarContrasenaVendedor)

        btnSiguiente = findViewById(R.id.btnSiguienteVendedor)
        tvIniciarSesion = findViewById(R.id.tvIniciarSesionVendedor)

        btnVolverRegistroVendedor =
            findViewById(R.id.btnVolverRegistroVendedor)
    }

    private fun configurarTiposDocumento() {
        val tiposDocumento = listOf(
            "Selecciona tu tipo de documento",
            "Cédula de ciudadanía",
            "Tarjeta de identidad",
            "Cédula de extranjería",
            "Pasaporte"
        )

        val adaptador = object : ArrayAdapter<String>(
            this,
            R.layout.item_tipo_documento,
            tiposDocumento
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val vista = super.getView(
                    position,
                    convertView,
                    parent
                ) as TextView

                vista.text = getItem(position)
                vista.textSize = 16f
                vista.setTextColor(0xFF858585.toInt())
                vista.gravity = Gravity.CENTER_VERTICAL

                return vista
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val vista = super.getDropDownView(
                    position,
                    convertView,
                    parent
                ) as TextView

                vista.text = getItem(position)
                vista.textSize = 16f
                vista.setTextColor(0xFF373737.toInt())

                return vista
            }
        }

        adaptador.setDropDownViewResource(
            R.layout.item_tipo_documento_dropdown
        )

        spTipoDocumento.adapter = adaptador
        spTipoDocumento.setSelection(0)
    }

    private fun configurarEventos() {

        btnSiguiente.setOnClickListener {
            validarDatosVendedor()
        }

        tvIniciarSesion.setOnClickListener {
            volverAlLogin()
        }

        // Flecha para regresar al inicio de sesión.
        btnVolverRegistroVendedor.setOnClickListener {
            volverAlLogin()
        }
    }

    private fun validarDatosVendedor() {

        val tipoDocumento = spTipoDocumento.selectedItem.toString()
        val documento = etDocumento.text.toString().trim()
        val nombreCompleto = etNombreCompleto.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val contrasena = etContrasena.text.toString()
        val confirmarContrasena =
            etConfirmarContrasena.text.toString()

        if (spTipoDocumento.selectedItemPosition == 0) {
            Toast.makeText(
                this,
                "Selecciona tu tipo de documento.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (documento.isBlank()) {
            etDocumento.error = "Ingresa tu número de documento"
            etDocumento.requestFocus()
            return
        }

        if (nombreCompleto.isBlank()) {
            etNombreCompleto.error = "Ingresa tu nombre completo"
            etNombreCompleto.requestFocus()
            return
        }

        val partesNombre = nombreCompleto.split(Regex("\\s+"))

        if (partesNombre.size < 2) {
            etNombreCompleto.error = "Ingresa nombres y apellidos"
            etNombreCompleto.requestFocus()
            return
        }

        val apellidos = partesNombre.last()
        val nombres = partesNombre.dropLast(1).joinToString(" ")

        if (correo.isBlank()) {
            etCorreo.error = "Ingresa tu correo electrónico"
            etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo válido"
            etCorreo.requestFocus()
            return
        }

        if (telefono.isBlank()) {
            etTelefono.error = "Ingresa tu teléfono"
            etTelefono.requestFocus()
            return
        }

        val telefonoLimpio = telefono.replace(Regex("[^0-9]"), "")

        if (telefonoLimpio.length !in 7..15) {
            etTelefono.error = "Ingresa un teléfono válido"
            etTelefono.requestFocus()
            return
        }

        if (contrasena.isBlank()) {
            etContrasena.error = "Ingresa una contraseña"
            etContrasena.requestFocus()
            return
        }

        if (contrasena.length < 8) {
            etContrasena.error =
                "La contraseña debe tener mínimo 8 caracteres"
            etContrasena.requestFocus()
            return
        }

        if (confirmarContrasena.isBlank()) {
            etConfirmarContrasena.error = "Confirma tu contraseña"
            etConfirmarContrasena.requestFocus()
            return
        }

        if (contrasena != confirmarContrasena) {
            etConfirmarContrasena.error = "Las contraseñas no coinciden"
            etConfirmarContrasena.requestFocus()
            return
        }

        val passwordHash = try {
            PasswordUtils.generarHashContrasena(contrasena)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No fue posible proteger la contraseña.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Preparar los datos personales para el paso 2.
        val intent = Intent(
            this,
            RegistroVendedorPaso2Activity::class.java
        ).apply {
            putExtra("tipo_documento", tipoDocumento)
            putExtra("documento", documento)
            putExtra("nombres", nombres)
            putExtra("apellidos", apellidos)
            putExtra("correo", correo)
            putExtra("telefono", telefonoLimpio)
            putExtra("password_hash", passwordHash)

            // Recuperar los datos del paso 2 si ya fueron diligenciados.
            datosPaso2?.let {
                putExtras(it)
            }
        }

        resultadoPaso2.launch(intent)
    }

    private fun volverAlLogin() {
        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            putExtra(MainActivity.EXTRA_ABRIR_LOGIN, true)
        }

        startActivity(intent)
        finish()
    }
}
