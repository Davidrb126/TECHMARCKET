package com.techmarket.app

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Carga imágenes locales y URL HTTPS (por ejemplo, Cloudinary) sin bloquear
 * el hilo principal. También evita que una respuesta tardía cambie una vista
 * que RecyclerView ya reutilizó para otro producto.
 */
object ImagenesManager {

    private val executor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun cargar(
        vista: ImageView,
        origen: String?,
        imagenPredeterminada: Int = android.R.drawable.ic_menu_gallery
    ) {
        vista.setImageResource(imagenPredeterminada)
        vista.tag = origen

        if (origen.isNullOrBlank()) return

        val contexto = vista.context.applicationContext

        executor.execute {
            var conexion: HttpURLConnection? = null

            val bitmap = try {
                if (origen.startsWith("https://") || origen.startsWith("http://")) {
                    conexion = URL(origen).openConnection() as HttpURLConnection
                    conexion.connectTimeout = 15000
                    conexion.readTimeout = 20000
                    conexion.requestMethod = "GET"
                    conexion.instanceFollowRedirects = true

                    if (conexion.responseCode !in 200..299) {
                        throw java.io.IOException(
                            "El servidor respondió HTTP ${conexion.responseCode}"
                        )
                    }

                    conexion.inputStream.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                } else {
                    val uri = Uri.parse(origen)
                    contexto.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
            } catch (_: Exception) {
                null
            } finally {
                conexion?.disconnect()
            }

            mainHandler.post {
                if (vista.tag == origen) {
                    if (bitmap != null) {
                        vista.imageTintList = null
                        vista.clearColorFilter()
                        vista.setImageBitmap(bitmap)
                    } else {
                        vista.setImageResource(imagenPredeterminada)
                    }
                }
            }
        }
    }
}
