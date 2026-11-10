package com.techmarket.app

import android.content.ContentValues
import android.os.Bundle
import android.util.Base64
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.DatabaseContract
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PerfilVendedorActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var etTipoDocumento: EditText
    private lateinit var etDocumento: EditText
    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etTelefono: EditText

    private lateinit var etContrasenaActual: EditText
    private lateinit var etNuevaContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText

    private var idUsuario: Long = -1L
    private var hashActual: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_vendedor)

        dbHelper = DatabaseHelper(this)

        inicializarVistas()
        configurarEventos()
        cargarDatosUsuario()
    }

    private fun inicializarVistas() {

        etTipoDocumento =
            findViewById(R.id.etTipoDocumentoPerfilVendedor)

        etDocumento =
            findViewById(R.id.etDocumentoPerfilVendedor)

        etNombres =
            findViewById(R.id.etNombresPerfilVendedor)

        etApellidos =
            findViewById(R.id.etApellidosPerfilVendedor)

        etCorreo =
            findViewById(R.id.etCorreoPerfilVendedor)

        etTelefono =
            findViewById(R.id.etTelefonoPerfilVendedor)

        etContrasenaActual =
            findViewById(R.id.etContrasenaActualVendedor)

        etNuevaContrasena =
            findViewById(R.id.etNuevaContrasenaVendedor)

        etConfirmarContrasena =
            findViewById(R.id.etConfirmarContrasenaVendedor)
    }

    private fun configurarEventos() {

        findViewById<ImageView>(R.id.btnVolverPerfilVendedor)
            .setOnClickListener {
                finish()
            }

        findViewById<Button>(R.id.btnGuardarPerfilVendedor)
            .setOnClickListener {
                guardarDatosPersonales()
            }

        findViewById<Button>(R.id.btnCambiarContrasenaVendedor)
            .setOnClickListener {
                cambiarContrasena()
            }
    }

    private fun cargarDatosUsuario() {

        val documento = SessionManager.obtenerDocumento(this)

        if (documento.isNullOrBlank()) {
            mostrarMensaje("No se encontró una sesión activa.")
            finish()
            return
        }

        try {

            val db = dbHelper.readableDatabase

            val cursor = db.query(
                DatabaseContract.Usuarios.TABLA,
                arrayOf(
                    DatabaseContract.Usuarios.ID,
                    DatabaseContract.Usuarios.TIPO_DOCUMENTO,
                    DatabaseContract.Usuarios.DOCUMENTO,
                    DatabaseContract.Usuarios.NOMBRES,
                    DatabaseContract.Usuarios.APELLIDOS,
                    DatabaseContract.Usuarios.CORREO,
                    DatabaseContract.Usuarios.TELEFONO,
                    DatabaseContract.Usuarios.PASSWORD_HASH
                ),
                "${DatabaseContract.Usuarios.DOCUMENTO} = ?",
                arrayOf(documento),
                null,
                null,
                null
            )

            cursor.use {

                if (!it.moveToFirst()) {
                    mostrarMensaje(
                        "No se encontraron los datos del vendedor."
                    )
                    finish()
                    return
                }

                idUsuario = it.getLong(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.ID
                    )
                )

                etTipoDocumento.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.TIPO_DOCUMENTO
                        )
                    ).orEmpty()
                )

                etDocumento.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.DOCUMENTO
                        )
                    ).orEmpty()
                )

                etNombres.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.NOMBRES
                        )
                    ).orEmpty()
                )

                etApellidos.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.APELLIDOS
                        )
                    ).orEmpty()
                )

                etCorreo.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.CORREO
                        )
                    ).orEmpty()
                )

                etTelefono.setText(
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.TELEFONO
                        )
                    ).orEmpty()
                )

                hashActual = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.PASSWORD_HASH
                    )
                ).orEmpty()
            }

        } catch (e: Exception) {

            mostrarMensaje(
                "No fue posible cargar la información del perfil."
            )
        }
    }

    private fun guardarDatosPersonales() {

        val nombres = etNombres.text.toString().trim()
        val apellidos = etApellidos.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()

        if (nombres.isBlank()) {
            etNombres.error = "Ingresa tus nombres."
            etNombres.requestFocus()
            return
        }

        if (apellidos.isBlank()) {
            etApellidos.error = "Ingresa tus apellidos."
            etApellidos.requestFocus()
            return
        }

        if (correo.isBlank()) {
            etCorreo.error = "Ingresa tu correo electrónico."
            etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo electrónico válido."
            etCorreo.requestFocus()
            return
        }

        if (idUsuario <= 0) {
            mostrarMensaje("No se pudo identificar al usuario.")
            return
        }

        try {

            val valores = ContentValues().apply {
                put(DatabaseContract.Usuarios.NOMBRES, nombres)
                put(DatabaseContract.Usuarios.APELLIDOS, apellidos)
                put(DatabaseContract.Usuarios.CORREO, correo)
                put(DatabaseContract.Usuarios.TELEFONO, telefono)
            }

            val resultado = dbHelper.writableDatabase.update(
                DatabaseContract.Usuarios.TABLA,
                valores,
                "${DatabaseContract.Usuarios.ID} = ?",
                arrayOf(idUsuario.toString())
            )

            if (resultado > 0) {

                // Actualizar los datos de la sesión
                // para reflejar el nuevo nombre en InicioVendedorActivity.
                val documento =
                    SessionManager.obtenerDocumento(this).orEmpty()

                val rol =
                    SessionManager.obtenerRol(this).orEmpty()

                val nombreCompleto =
                    "$nombres $apellidos".trim()

                SessionManager.guardarSesion(
                    this,
                    idUsuario,
                    documento,
                    nombreCompleto,
                    rol
                )

                mostrarMensaje(
                    "Tus datos se actualizaron correctamente."
                )

                setResult(RESULT_OK)

            } else {

                mostrarMensaje(
                    "No se realizaron cambios en el perfil."
                )
            }

        } catch (e: Exception) {

            mostrarMensaje(
                "No fue posible actualizar tus datos: ${e.message}"
            )
        }
    }

    private fun cambiarContrasena() {

        val actual = etContrasenaActual.text.toString()
        val nueva = etNuevaContrasena.text.toString()
        val confirmacion = etConfirmarContrasena.text.toString()

        if (actual.isBlank()) {
            etContrasenaActual.error =
                "Ingresa tu contraseña actual."
            etContrasenaActual.requestFocus()
            return
        }

        if (nueva.isBlank()) {
            etNuevaContrasena.error =
                "Ingresa la nueva contraseña."
            etNuevaContrasena.requestFocus()
            return
        }

        if (nueva.length < 8) {
            etNuevaContrasena.error =
                "La contraseña debe tener al menos 8 caracteres."
            etNuevaContrasena.requestFocus()
            return
        }

        if (confirmacion.isBlank()) {
            etConfirmarContrasena.error =
                "Confirma tu nueva contraseña."
            etConfirmarContrasena.requestFocus()
            return
        }

        if (nueva != confirmacion) {
            etConfirmarContrasena.error =
                "Las contraseñas no coinciden."
            etConfirmarContrasena.requestFocus()
            return
        }

        if (!verificarContrasena(actual, hashActual)) {
            etContrasenaActual.error =
                "La contraseña actual es incorrecta."
            etContrasenaActual.requestFocus()
            return
        }

        if (actual == nueva) {
            etNuevaContrasena.error =
                "La nueva contraseña debe ser diferente."
            etNuevaContrasena.requestFocus()
            return
        }

        try {

            val nuevoHash = generarHashContrasena(nueva)

            val valores = ContentValues().apply {
                put(
                    DatabaseContract.Usuarios.PASSWORD_HASH,
                    nuevoHash
                )
            }

            val resultado = dbHelper.writableDatabase.update(
                DatabaseContract.Usuarios.TABLA,
                valores,
                "${DatabaseContract.Usuarios.ID} = ?",
                arrayOf(idUsuario.toString())
            )

            if (resultado > 0) {

                hashActual = nuevoHash

                etContrasenaActual.text.clear()
                etNuevaContrasena.text.clear()
                etConfirmarContrasena.text.clear()

                mostrarMensaje(
                    "Tu contraseña se actualizó correctamente."
                )

            } else {

                mostrarMensaje(
                    "No se pudo actualizar la contraseña."
                )
            }

        } catch (e: Exception) {

            mostrarMensaje(
                "Ocurrió un error al cambiar la contraseña."
            )
        }
    }

    private fun generarHashContrasena(
        contrasena: String
    ): String {

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val iteraciones = 120000

        val especificacion = PBEKeySpec(
            contrasena.toCharArray(),
            salt,
            iteraciones,
            256
        )

        return try {

            val factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            )

            val hash = factory.generateSecret(
                especificacion
            ).encoded

            val saltCodificado = Base64.encodeToString(
                salt,
                Base64.NO_WRAP
            )

            val hashCodificado = Base64.encodeToString(
                hash,
                Base64.NO_WRAP
            )

            "PBKDF2WithHmacSHA256:$iteraciones:$saltCodificado:$hashCodificado"

        } finally {
            especificacion.clearPassword()
        }
    }

    private fun verificarContrasena(
        contrasena: String,
        hashAlmacenado: String
    ): Boolean {

        return try {

            val partes = hashAlmacenado.split(":")

            if (partes.size != 4) return false

            val algoritmo = partes[0]
            val iteraciones = partes[1].toInt()

            val salt = Base64.decode(
                partes[2],
                Base64.NO_WRAP
            )

            val hashOriginal = Base64.decode(
                partes[3],
                Base64.NO_WRAP
            )

            if (algoritmo != "PBKDF2WithHmacSHA256") {
                return false
            }

            if (
                iteraciones <= 0 ||
                salt.isEmpty() ||
                hashOriginal.isEmpty()
            ) {
                return false
            }

            val especificacion = PBEKeySpec(
                contrasena.toCharArray(),
                salt,
                iteraciones,
                hashOriginal.size * 8
            )

            val hashCalculado = try {

                SecretKeyFactory
                    .getInstance(algoritmo)
                    .generateSecret(especificacion)
                    .encoded

            } finally {
                especificacion.clearPassword()
            }

            MessageDigest.isEqual(
                hashOriginal,
                hashCalculado
            )

        } catch (e: Exception) {
            false
        }
    }

    private fun mostrarMensaje(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {

        if (::dbHelper.isInitialized) {
            dbHelper.close()
        }

        super.onDestroy()
    }
}
