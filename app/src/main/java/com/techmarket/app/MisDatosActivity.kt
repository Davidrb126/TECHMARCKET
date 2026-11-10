package com.techmarket.app

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.DatabaseContract
import com.techmarket.app.database.UsuarioDAO

class MisDatosActivity : AppCompatActivity() {

    private lateinit var tvTipoDocumento: TextView
    private lateinit var tvDocumento: TextView
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etCorreo: EditText

    private var idUsuario: Long = -1L
    private var documentoUsuario: String = ""
    private var rolUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_datos)

        inicializarComponentes()
        configurarEventos()
        cargarDatosUsuario()
    }

    private fun inicializarComponentes() {

        tvTipoDocumento = findViewById(R.id.tvTipoDocumentoMisDatos)
        tvDocumento = findViewById(R.id.tvDocumentoMisDatos)
        etNombres = findViewById(R.id.etNombresMisDatos)
        etApellidos = findViewById(R.id.etApellidosMisDatos)
        etCorreo = findViewById(R.id.etCorreoMisDatos)

        etNombres.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS

        etApellidos.inputType =
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }

    private fun configurarEventos() {

        findViewById<ImageButton>(R.id.btnVolverMisDatos)
            .setOnClickListener {
                finish()
            }

        findViewById<Button>(R.id.btnGuardarMisDatos)
            .setOnClickListener {
                guardarCambios()
            }

        findViewById<Button>(R.id.btnCancelarMisDatos)
            .setOnClickListener {
                finish()
            }
    }

    private fun cargarDatosUsuario() {

        documentoUsuario =
            SessionManager.obtenerDocumento(this) ?: ""

        if (documentoUsuario.isEmpty()) {
            mostrarMensaje("No se encontró una sesión activa.")
            finish()
            return
        }

        val usuarioDAO = UsuarioDAO(this)

        try {

            val cursor = usuarioDAO.consultarUsuario(documentoUsuario)

            cursor.use {

                if (!it.moveToFirst()) {
                    mostrarMensaje(
                        "No se encontró la información del usuario."
                    )
                    finish()
                    return
                }

                idUsuario = it.getLong(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.ID
                    )
                )

                rolUsuario = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.ROL
                    )
                )

                tvTipoDocumento.text = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.TIPO_DOCUMENTO
                    )
                )

                tvDocumento.text = documentoUsuario

                etNombres.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.NOMBRES
                        )
                    )
                )

                etApellidos.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.APELLIDOS
                        )
                    )
                )

                etCorreo.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.CORREO
                        )
                    )
                )
            }

        } catch (e: Exception) {

            mostrarMensaje("No fue posible cargar tus datos.")
            finish()
        }
    }

    private fun guardarCambios() {

        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val correo = etCorreo.text.toString().trim()

        if (nombres.isEmpty()) {
            etNombres.error = "Ingresa tus nombres"
            etNombres.requestFocus()
            return
        }

        if (apellidos.isEmpty()) {
            etApellidos.error = "Ingresa tus apellidos"
            etApellidos.requestFocus()
            return
        }

        if (correo.isEmpty()) {
            etCorreo.error = "Ingresa tu correo electrónico"
            etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo electrónico válido"
            etCorreo.requestFocus()
            return
        }

        val usuarioDAO = UsuarioDAO(this)

        try {

            val resultado = usuarioDAO.actualizarDatosUsuario(
                documento = documentoUsuario,
                nombres = nombres,
                apellidos = apellidos,
                correo = correo
            )

            if (resultado) {

                SessionManager.guardarSesion(
                    context = this,
                    id = idUsuario,
                    documento = documentoUsuario,
                    nombre = "$nombres $apellidos",
                    rol = rolUsuario
                )

                mostrarMensaje(
                    "Tus datos se actualizaron correctamente."
                )

                finish()

            } else {

                mostrarMensaje(
                    "No se realizaron cambios en la información."
                )
            }

        } catch (e: Exception) {

            mostrarMensaje("No fue posible actualizar tus datos.")
        }
    }

    private fun mostrarMensaje(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }
}
