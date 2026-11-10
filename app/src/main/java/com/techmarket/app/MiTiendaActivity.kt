package com.techmarket.app

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MiTiendaActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var imgLogo: ImageView
    private lateinit var etNombre: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etSitioWeb: EditText

    private var idUsuario: Long = -1L
    private var logoActual: String? = null

    private val selectorLogo =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->

            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    logoActual = uri.toString()
                    cargarImagenLogo(uri)

                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "No se pudo seleccionar el logo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mi_tienda)

        dbHelper = DatabaseHelper(this)

        inicializarVistas()
        configurarEventos()
        cargarDatosTienda()
    }

    private fun inicializarVistas() {

        imgLogo = findViewById(R.id.imgLogoMiTienda)

        etNombre = findViewById(R.id.etNombreTienda)
        etDescripcion = findViewById(R.id.etDescripcionTienda)
        etDireccion = findViewById(R.id.etDireccionTienda)
        etSitioWeb = findViewById(R.id.etSitioWebTienda)
    }

    private fun configurarEventos() {

        findViewById<ImageView>(R.id.btnVolverMiTienda)
            .setOnClickListener {
                finish()
            }

        findViewById<Button>(R.id.btnCambiarLogoTienda)
            .setOnClickListener {
                selectorLogo.launch(arrayOf("image/*"))
            }

        findViewById<Button>(R.id.btnGuardarTienda)
            .setOnClickListener {
                guardarCambios()
            }
    }

    private fun cargarDatosTienda() {

        val documento = SessionManager.obtenerDocumento(this)

        if (documento.isNullOrBlank()) {
            mostrarMensaje("No se encontró una sesión activa.")
            finish()
            return
        }

        val db = dbHelper.readableDatabase

        // Consultar el usuario que inició sesión.
        val cursorUsuario = db.query(
            "usuarios",
            arrayOf("id"),
            "documento = ?",
            arrayOf(documento),
            null,
            null,
            null
        )

        cursorUsuario.use { cursor ->

            if (!cursor.moveToFirst()) {
                mostrarMensaje("No se encontró el usuario vendedor.")
                finish()
                return
            }

            idUsuario = cursor.getLong(
                cursor.getColumnIndexOrThrow("id")
            )
        }

        if (idUsuario <= 0) {
            mostrarMensaje("No se pudo identificar al vendedor.")
            finish()
            return
        }

        // Consultar la tienda asociada al usuario.
        val cursorTienda = db.query(
            "tiendas",
            arrayOf(
                "id_tienda",
                "id_usuario",
                "nombre_tienda",
                "descripcion",
                "logo",
                "direccion",
                "sitio_web_redes"
            ),
            "id_usuario = ?",
            arrayOf(idUsuario.toString()),
            null,
            null,
            null
        )

        cursorTienda.use { cursor ->

            if (!cursor.moveToFirst()) {
                mostrarMensaje(
                    "No se encontró una tienda asociada a este vendedor."
                )
                finish()
                return
            }

            etNombre.setText(
                cursor.getString(
                    cursor.getColumnIndexOrThrow("nombre_tienda")
                ).orEmpty()
            )

            etDescripcion.setText(
                cursor.getString(
                    cursor.getColumnIndexOrThrow("descripcion")
                ).orEmpty()
            )

            logoActual = cursor.getString(
                cursor.getColumnIndexOrThrow("logo")
            )

            etDireccion.setText(
                cursor.getString(
                    cursor.getColumnIndexOrThrow("direccion")
                ).orEmpty()
            )

            etSitioWeb.setText(
                cursor.getString(
                    cursor.getColumnIndexOrThrow("sitio_web_redes")
                ).orEmpty()
            )
        }

        cargarLogoGuardado()
    }

    private fun cargarLogoGuardado() {

        if (logoActual.isNullOrBlank()) {
            mostrarLogoPredeterminado()
            return
        }

        try {
            cargarImagenLogo(Uri.parse(logoActual))
        } catch (e: Exception) {
            mostrarLogoPredeterminado()
        }
    }

    private fun cargarImagenLogo(uri: Uri) {

        try {

            contentResolver.openInputStream(uri)?.use { input ->

                val bitmap = BitmapFactory.decodeStream(input)

                if (bitmap != null) {
                    imgLogo.setImageBitmap(bitmap)
                    imgLogo.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    mostrarLogoPredeterminado()
                }

            } ?: mostrarLogoPredeterminado()

        } catch (e: Exception) {

            mostrarLogoPredeterminado()

            Toast.makeText(
                this,
                "No se pudo cargar la imagen seleccionada.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarLogoPredeterminado() {

        imgLogo.setImageResource(
            android.R.drawable.ic_menu_gallery
        )

        imgLogo.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    private fun guardarCambios() {

        val nombre = etNombre.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()
        val sitioWeb = etSitioWeb.text.toString().trim()

        if (nombre.isBlank()) {
            etNombre.error = "Ingresa el nombre de la tienda."
            etNombre.requestFocus()
            return
        }

        if (descripcion.isBlank()) {
            etDescripcion.error = "Ingresa una descripción."
            etDescripcion.requestFocus()
            return
        }

        if (idUsuario <= 0) {
            mostrarMensaje("No se pudo identificar la tienda.")
            return
        }

        // Preparar los datos que se actualizarán.
        val valores = ContentValues().apply {

            put("nombre_tienda", nombre)
            put("descripcion", descripcion)
            put("logo", logoActual)
            put("direccion", direccion)
            put("sitio_web_redes", sitioWeb)
        }

        try {

            // Actualizar únicamente la tienda del vendedor autenticado.
            val resultado = dbHelper.writableDatabase.update(
                "tiendas",
                valores,
                "id_usuario = ?",
                arrayOf(idUsuario.toString())
            )

            if (resultado > 0) {

                Toast.makeText(
                    this,
                    "Información de la tienda actualizada correctamente.",
                    Toast.LENGTH_LONG
                ).show()

                setResult(RESULT_OK)
                finish()

            } else {

                mostrarMensaje(
                    "No se pudieron guardar los cambios."
                )
            }

        } catch (e: Exception) {

            mostrarMensaje(
                "Ocurrió un error al actualizar la tienda."
            )
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
