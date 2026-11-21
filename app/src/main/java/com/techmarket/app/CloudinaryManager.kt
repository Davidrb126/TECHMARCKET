package com.techmarket.app

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

object CloudinaryManager {

    private const val CLOUD_NAME = "t1wxgorc"
    private const val UPLOAD_PRESET = "techmarket_productos"

    private val executor = Executors.newSingleThreadExecutor()

    fun subirImagen(
        context: Context,
        uri: Uri
    ): Task<String> {

        val resultado = TaskCompletionSource<String>()

        executor.execute {
            var conexion: HttpURLConnection? = null

            try {
                val contexto = context.applicationContext
                val boundary = "----TechMarket${UUID.randomUUID()}"

                val tipoMime = contexto.contentResolver.getType(uri)
                    ?: "image/jpeg"

                if (tipoMime != "image/jpeg" && tipoMime != "image/png") {
                    throw IOException("El formato de imagen no es compatible.")
                }

                val extension = if (tipoMime == "image/png") "png" else "jpg"
                val nombreArchivo = "producto_${System.currentTimeMillis()}.$extension"

                conexion = URL(
                    "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
                ).openConnection() as HttpURLConnection

                conexion.requestMethod = "POST"
                conexion.doOutput = true
                conexion.doInput = true
                conexion.connectTimeout = 30000
                conexion.readTimeout = 60000
                conexion.useCaches = false

                conexion.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=$boundary"
                )

                conexion.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                DataOutputStream(conexion.outputStream).use { salida ->

                    salida.writeBytes("--$boundary\r\n")
                    salida.writeBytes(
                        "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n"
                    )
                    salida.writeBytes("$UPLOAD_PRESET\r\n")

                    salida.writeBytes("--$boundary\r\n")
                    salida.writeBytes(
                        "Content-Disposition: form-data; name=\"file\"; filename=\"$nombreArchivo\"\r\n"
                    )
                    salida.writeBytes("Content-Type: $tipoMime\r\n\r\n")

                    val flujoImagen = contexto.contentResolver.openInputStream(uri)
                        ?: throw IOException(
                            "No fue posible leer la imagen seleccionada."
                        )

                    flujoImagen.use { entrada ->
                        val buffer = ByteArray(8192)
                        var bytesLeidos: Int

                        while (entrada.read(buffer).also {
                                bytesLeidos = it
                            } != -1
                        ) {
                            salida.write(buffer, 0, bytesLeidos)
                        }
                    }

                    salida.writeBytes("\r\n")
                    salida.writeBytes("--$boundary--\r\n")
                    salida.flush()
                }

                val codigoRespuesta = conexion.responseCode

                val flujoRespuesta = if (codigoRespuesta in 200..299) {
                    conexion.inputStream
                } else {
                    conexion.errorStream
                }

                val respuesta = flujoRespuesta?.use { flujo ->
                    BufferedReader(InputStreamReader(flujo)).use { lector ->
                        lector.readText()
                    }
                }.orEmpty()

                if (codigoRespuesta !in 200..299) {
                    throw IOException(
                        "Cloudinary respondió HTTP $codigoRespuesta: $respuesta"
                    )
                }

                val json = JSONObject(respuesta)

                val urlSegura = json.optString("secure_url")
                val publicId = json.optString("public_id")

                if (urlSegura.isBlank() || publicId.isBlank()) {
                    throw IOException(
                        "Cloudinary no devolvió la URL segura o el identificador de la imagen."
                    )
                }

                resultado.setResult(urlSegura)

            } catch (e: Exception) {
                resultado.setException(e)

            } finally {
                conexion?.disconnect()
            }
        }

        return resultado.task
    }
}
