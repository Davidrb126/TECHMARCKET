package com.techmarket.app

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordUtils {

    fun generarHashContrasena(contrasena: String): String {

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

            val hash = factory.generateSecret(especificacion).encoded

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
}
