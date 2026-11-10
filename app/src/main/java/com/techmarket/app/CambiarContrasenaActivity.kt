package com.techmarket.app

import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.DatabaseContract
import com.techmarket.app.database.UsuarioDAO
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class CambiarContrasenaActivity : AppCompatActivity() {

    private lateinit var documentoUsuario: String

    private lateinit var etContrasenaActual: EditText
    private lateinit var etNuevaContrasena: EditText
    private lateinit var etConfirmarContrasena: EditText

    private lateinit var usuarioDAO: UsuarioDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_contrasena)

        documentoUsuario =
            SessionManager.obtenerDocumento(this).orEmpty()

        usuarioDAO = UsuarioDAO(this)

        inicializarVistas()
        configurarEventos()
    }

    private fun inicializarVistas() {

        etContrasenaActual =
            findViewById(R.id.etContrasenaActual)

        etNuevaContrasena =
            findViewById(R.id.etNuevaContrasena)

        etConfirmarContrasena =
            findViewById(R.id.etConfirmarContrasena)
    }

    private fun configurarEventos() {

        findViewById<ImageButton>(
            R.id.btnVolverCambiarContrasena
        ).setOnClickListener {
            finish()
        }

        findViewById<Button>(
            R.id.btnGuardarNuevaContrasena
        ).setOnClickListener {
            cambiarContrasena()
        }
    }

    private fun cambiarContrasena() {

        val actual = etContrasenaActual.text.toString()
        val nueva = etNuevaContrasena.text.toString()
        val confirmacion = etConfirmarContrasena.text.toString()

        if (documentoUsuario.isBlank()) {
            mostrarMensaje("No se pudo identificar al usuario.")
            return
        }

        if (actual.isEmpty()) {
            etContrasenaActual.error =
                "Ingresa tu contraseña actual"
            etContrasenaActual.requestFocus()
            return
        }

        if (nueva.isEmpty()) {
            etNuevaContrasena.error =
                "Ingresa tu nueva contraseña"
            etNuevaContrasena.requestFocus()
            return
        }

        if (nueva.length < 8) {
            etNuevaContrasena.error =
                "La contraseña debe tener al menos 8 caracteres"
            etNuevaContrasena.requestFocus()
            return
        }

        if (confirmacion.isEmpty()) {
            etConfirmarContrasena.error =
                "Confirma tu nueva contraseña"
            etConfirmarContrasena.requestFocus()
            return
        }

        if (nueva != confirmacion) {
            etConfirmarContrasena.error =
                "Las contraseñas no coinciden"
            etConfirmarContrasena.requestFocus()
            return
        }

        if (actual == nueva) {
            etNuevaContrasena.error =
                "La nueva contraseña debe ser diferente"
            etNuevaContrasena.requestFocus()
            return
        }

        try {

            val cursor = usuarioDAO.consultarUsuario(documentoUsuario)

            val hashAlmacenado = cursor.use {
                if (it.moveToFirst()) {
                    it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.PASSWORD_HASH
                        )
                    )
                } else {
                    null
                }
            }

            if (hashAlmacenado.isNullOrBlank()) {
                mostrarMensaje(
                    "No fue posible consultar la contraseña registrada."
                )
                return
            }

            if (!verificarContrasena(actual, hashAlmacenado)) {
                etContrasenaActual.error =
                    "La contraseña actual es incorrecta"
                etContrasenaActual.requestFocus()
                return
            }

            val nuevoHash = generarHashContrasena(nueva)

            val resultado = usuarioDAO.actualizarContrasena(
                documentoUsuario,
                nuevoHash
            )

            if (resultado) {

                Toast.makeText(
                    this,
                    "Tu contraseña fue actualizada correctamente.",
                    Toast.LENGTH_LONG
                ).show()

                finish()

            } else {

                mostrarMensaje(
                    "No fue posible actualizar la contraseña."
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

            val hash = factory
                .generateSecret(especificacion)
                .encoded

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

            if (algoritmo != "PBKDF2WithHmacSHA256") {
                return false
            }

            if (iteraciones <= 0) return false

            val salt = Base64.decode(
                partes[2],
                Base64.NO_WRAP
            )

            val hashOriginal = Base64.decode(
                partes[3],
                Base64.NO_WRAP
            )

            if (salt.isEmpty() || hashOriginal.isEmpty()) {
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
}
