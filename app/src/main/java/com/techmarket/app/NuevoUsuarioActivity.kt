package com.techmarket.app

import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.UsuarioDAO

class NuevoUsuarioActivity : AppCompatActivity() {

    private lateinit var spTipoDocumento: Spinner
    private lateinit var spRol: Spinner

    private lateinit var etDocumento: EditText
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnVolver: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_usuario)

        inicializarComponentes()
        configurarSelectores()
        configurarEventos()
    }

    private fun inicializarComponentes() {

        spTipoDocumento = findViewById(R.id.spTipoDocumentoNuevo)
        spRol = findViewById(R.id.spRolNuevo)

        etDocumento = findViewById(R.id.etDocumentoNuevo)
        etNombres = findViewById(R.id.etNombresNuevo)
        etApellidos = findViewById(R.id.etApellidosNuevo)
        etCorreo = findViewById(R.id.etCorreoNuevo)
        etContrasena = findViewById(R.id.etContrasenaNuevo)

        btnGuardar = findViewById(R.id.btnGuardarNuevoUsuario)
        btnCancelar = findViewById(R.id.btnCancelarNuevoUsuario)
        btnVolver = findViewById(R.id.btnVolverNuevoUsuario)
    }

    private fun configurarSelectores() {

        val tiposDocumento = listOf(
            "Selecciona tu tipo de documento",
            "Cédula de ciudadanía",
            "Tarjeta de identidad",
            "Cédula de extranjería",
            "Pasaporte"
        )

        val roles = listOf(
            "Selecciona el rol del usuario",
            "CLIENTE",
            "VENDEDOR",
            "ADMINISTRADOR"
        )

        val adaptadorDocumentos = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tiposDocumento
        )

        adaptadorDocumentos.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spTipoDocumento.adapter = adaptadorDocumentos

        val adaptadorRoles = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )

        adaptadorRoles.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spRol.adapter = adaptadorRoles
    }

    private fun configurarEventos() {

        btnGuardar.setOnClickListener {
            registrarUsuario()
        }

        btnCancelar.setOnClickListener {
            finish()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun registrarUsuario() {

        val tipoDocumento =
            spTipoDocumento.selectedItem?.toString().orEmpty()

        val documento =
            etDocumento.text.toString().trim()

        val nombres =
            etNombres.text.toString().trim()

        val apellidos =
            etApellidos.text.toString().trim()

        val correo =
            etCorreo.text.toString().trim()

        val contrasena =
            etContrasena.text.toString()

        val rol =
            spRol.selectedItem?.toString().orEmpty()

        // VALIDAR TIPO DE DOCUMENTO

        if (spTipoDocumento.selectedItemPosition == 0) {
            Toast.makeText(
                this,
                "Selecciona el tipo de documento.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // VALIDAR DOCUMENTO

        if (documento.isBlank()) {
            etDocumento.error = "Ingresa el número de documento"
            etDocumento.requestFocus()
            return
        }

        // VALIDAR NOMBRES

        if (nombres.isBlank()) {
            etNombres.error = "Ingresa los nombres"
            etNombres.requestFocus()
            return
        }

        // VALIDAR APELLIDOS

        if (apellidos.isBlank()) {
            etApellidos.error = "Ingresa los apellidos"
            etApellidos.requestFocus()
            return
        }

        // VALIDAR CORREO

        if (correo.isBlank()) {
            etCorreo.error = "Ingresa el correo electrónico"
            etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo electrónico válido"
            etCorreo.requestFocus()
            return
        }

        // VALIDAR ROL

        if (spRol.selectedItemPosition == 0) {
            Toast.makeText(
                this,
                "Selecciona el rol del usuario.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // VALIDAR CONTRASEÑA

        if (contrasena.isBlank()) {
            etContrasena.error = "Ingresa una contraseña"
            etContrasena.requestFocus()
            return
        }

        if (contrasena.length < 8) {
            etContrasena.error =
                "La contraseña debe tener al menos 8 caracteres"
            etContrasena.requestFocus()
            return
        }

        // GENERAR HASH DE LA CONTRASEÑA

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

        // REGISTRAR MEDIANTE USUARIODAO

        val usuarioDAO = UsuarioDAO(this)

        try {

            val resultado = usuarioDAO.registrarUsuario(
                tipoDocumento = tipoDocumento,
                documento = documento,
                nombres = nombres,
                apellidos = apellidos,
                correo = correo,
                passwordHash = passwordHash,
                rol = rol
            )

            if (resultado == -1L) {

                Toast.makeText(
                    this,
                    "Ya existe un usuario con ese documento o no fue posible registrarlo.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            Toast.makeText(
                this,
                "Usuario creado correctamente.",
                Toast.LENGTH_LONG
            ).show()

            setResult(RESULT_OK)
            finish()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Ocurrió un error al crear el usuario.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
