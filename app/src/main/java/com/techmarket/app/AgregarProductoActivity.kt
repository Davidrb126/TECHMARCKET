package com.techmarket.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import com.techmarket.app.database.DatabaseContract
import com.techmarket.app.database.ProductoDAO
import com.techmarket.app.database.TiendaDAO
import com.techmarket.app.database.UsuarioDAO

class AgregarProductoActivity : AppCompatActivity() {

    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var tiendaDAO: TiendaDAO
    private lateinit var productoDAO: ProductoDAO

    private lateinit var contenedorImagenPrincipal: LinearLayout
    private lateinit var imgPrincipal: ImageView

    private lateinit var btnImagen2: ImageButton
    private lateinit var btnImagen3: ImageButton
    private lateinit var btnImagen4: ImageButton

    private lateinit var switchDisponible: Switch
    private lateinit var etNombre: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var etPrecio: EditText
    private lateinit var etStock: EditText
    private lateinit var etDescripcion: EditText

    private lateinit var btnGuardar: Button
    private lateinit var btnVolver: ImageButton

    private var imagenPrincipal: Uri? = null
    private var imagen2: Uri? = null
    private var imagen3: Uri? = null
    private var imagen4: Uri? = null

    private var imagenSeleccionada = 0
    private var idTienda: Long = -1L
    private var idProductoEditar: Long = -1L

    private val esEdicion: Boolean
        get() = idProductoEditar > 0

    private val executorImagenes = Executors.newSingleThreadExecutor()

    private val selectorImagen =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->

            if (uri != null) {

                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Algunos proveedores no permiten persistir el permiso.
                }

                if (!validarImagen(uri)) {
                    Toast.makeText(
                        this,
                        "Selecciona una imagen PNG o JPG de máximo 5 MB",
                        Toast.LENGTH_LONG
                    ).show()
                    return@registerForActivityResult
                }

                asignarImagen(uri)
            }
        }

    private val tomarFotografia =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                guardarFotografiaTemporal(bitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_producto)

        usuarioDAO = UsuarioDAO(this)
        tiendaDAO = TiendaDAO(this)
        productoDAO = ProductoDAO(this)

        inicializarComponentes()
        configurarCategorias()

        idProductoEditar = intent.getLongExtra(
            MisProductosActivity.EXTRA_ID_PRODUCTO,
            -1L
        )

        if (esEdicion) {
            btnGuardar.text = "Guardar cambios"
        }

        obtenerTiendaVendedor()
        configurarEventos()
    }

    private fun inicializarComponentes() {

        contenedorImagenPrincipal =
            findViewById(R.id.contenedorImagenPrincipalProducto)

        imgPrincipal =
            findViewById(R.id.imgPrincipalProducto)

        btnImagen2 =
            findViewById(R.id.btnImagenProducto2)

        btnImagen3 =
            findViewById(R.id.btnImagenProducto3)

        btnImagen4 =
            findViewById(R.id.btnImagenProducto4)

        switchDisponible =
            findViewById(R.id.switchProductoDisponible)

        etNombre =
            findViewById(R.id.etNombreProducto)

        spCategoria =
            findViewById(R.id.spCategoriaProducto)

        etPrecio =
            findViewById(R.id.etPrecioProducto)

        etStock =
            findViewById(R.id.etStockProducto)

        etDescripcion =
            findViewById(R.id.etDescripcionProducto)

        btnGuardar =
            findViewById(R.id.btnGuardarProducto)

        btnVolver =
            findViewById(R.id.btnVolverAgregarProducto)
    }

    private fun configurarCategorias() {

        val categorias = listOf(
            "Selecciona una categoría",
            "Tecnología",
            "Celulares",
            "Computadores",
            "Accesorios",
            "Audio",
            "Video",
            "Hogar",
            "Electrodomésticos",
            "Moda",
            "Deportes",
            "Otros"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categorias
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spCategoria.adapter = adapter
    }

    private fun obtenerTiendaVendedor() {

        val documento = SessionManager.obtenerDocumento(this)

        if (documento.isNullOrBlank()) {
            mostrarMensaje("No se encontró una sesión activa.")
            finish()
            return
        }

        try {
            var usuarioId = -1L

            usuarioDAO.consultarUsuario(documento).use { cursorUsuario ->
                if (cursorUsuario.moveToFirst()) {
                    usuarioId = cursorUsuario.getLong(
                        cursorUsuario.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.ID
                        )
                    )
                }
            }

            if (usuarioId <= 0) {
                mostrarMensaje("No se encontró el usuario vendedor.")
                finish()
                return
            }

            val tiendas = mutableListOf<Pair<Long, String>>()

            tiendaDAO.consultarTiendaPorUsuario(usuarioId).use { cursorTienda ->
                val indiceId = cursorTienda.getColumnIndexOrThrow(
                    DatabaseContract.Tiendas.ID
                )
                val indiceNombre = cursorTienda.getColumnIndexOrThrow(
                    DatabaseContract.Tiendas.NOMBRE
                )

                while (cursorTienda.moveToNext()) {
                    val id = cursorTienda.getLong(indiceId)
                    val nombre = cursorTienda.getString(indiceNombre).orEmpty()
                    tiendas.add(id to nombre)
                }
            }

            if (tiendas.isEmpty()) {
                mostrarMensaje("No se encontraron tiendas asociadas al vendedor.")
                finish()
                return
            }

            // Si la pantalla anterior envió una tienda, se valida que pertenezca
            // al vendedor autenticado antes de permitir guardar o editar productos.
            val idTiendaRecibida = intent.getLongExtra(
                MisProductosActivity.EXTRA_ID_TIENDA,
                -1L
            )

            when {
                idTiendaRecibida > 0 -> {
                    val tiendaPerteneceAlVendedor = tiendas.any {
                        it.first == idTiendaRecibida
                    }

                    if (!tiendaPerteneceAlVendedor) {
                        mostrarMensaje("La tienda seleccionada no pertenece a tu usuario.")
                        finish()
                        return
                    }

                    seleccionarTienda(idTiendaRecibida)
                }

                tiendas.size == 1 -> {
                    seleccionarTienda(tiendas.first().first)
                }

                else -> {
                    val nombresTiendas = tiendas.map { it.second }.toTypedArray()

                    AlertDialog.Builder(this)
                        .setTitle("Selecciona la tienda para el producto")
                        .setItems(nombresTiendas) { _, posicion ->
                            seleccionarTienda(tiendas[posicion].first)
                        }
                        .setCancelable(false)
                        .show()
                }
            }

        } catch (e: Exception) {
            mostrarMensaje("No fue posible consultar las tiendas.")
            finish()
        }
    }

    private fun seleccionarTienda(nuevoIdTienda: Long) {
        if (nuevoIdTienda <= 0) {
            mostrarMensaje("La tienda seleccionada no es válida.")
            finish()
            return
        }

        idTienda = nuevoIdTienda

        if (esEdicion) {
            cargarProductoParaEditar()
        }
    }

    private fun cargarProductoParaEditar() {

        if (idProductoEditar <= 0 || idTienda <= 0) return

        try {

            var perteneceALaTienda = false

            productoDAO.obtenerProductosTienda(idTienda).use { cursor ->

                while (cursor.moveToNext()) {

                    val id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            DatabaseContract.ProductosTienda.ID
                        )
                    )

                    if (id == idProductoEditar) {
                        perteneceALaTienda = true
                        break
                    }
                }
            }

            if (!perteneceALaTienda) {
                mostrarMensaje(
                    "El producto no pertenece a tu tienda."
                )
                finish()
                return
            }

            productoDAO.consultarProductoTienda(idProductoEditar)
                .use { cursor ->

                    if (!cursor.moveToFirst()) {
                        mostrarMensaje("No se encontró el producto.")
                        finish()
                        return
                    }

                    etNombre.setText(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.NOMBRE
                            )
                        ).orEmpty()
                    )

                    val categoria = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            DatabaseContract.ProductosTienda.CATEGORIA
                        )
                    ).orEmpty()

                    val posicionCategoria =
                        (spCategoria.adapter as ArrayAdapter<String>)
                            .getPosition(categoria)

                    if (posicionCategoria >= 0) {
                        spCategoria.setSelection(posicionCategoria)
                    }

                    etPrecio.setText(
                        cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.PRECIO
                            )
                        ).toString()
                    )

                    etStock.setText(
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.STOCK
                            )
                        ).toString()
                    )

                    etDescripcion.setText(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.DESCRIPCION
                            )
                        ).orEmpty()
                    )

                    switchDisponible.isChecked =
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.DISPONIBLE
                            )
                        ) == 1

                    imagenPrincipal = obtenerUriImagen(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.IMAGEN_PRINCIPAL
                            )
                        )
                    )

                    imagen2 = obtenerUriImagen(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.IMAGEN_2
                            )
                        )
                    )

                    imagen3 = obtenerUriImagen(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.IMAGEN_3
                            )
                        )
                    )

                    imagen4 = obtenerUriImagen(
                        cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.IMAGEN_4
                            )
                        )
                    )

                    mostrarImagenExistente(imagenPrincipal, imgPrincipal)
                    mostrarImagenExistente(imagen2, btnImagen2)
                    mostrarImagenExistente(imagen3, btnImagen3)
                    mostrarImagenExistente(imagen4, btnImagen4)
                }

        } catch (e: Exception) {
            mostrarMensaje(
                "No fue posible cargar la información del producto."
            )
            finish()
        }
    }

    private fun obtenerUriImagen(ruta: String?): Uri? {

        if (ruta.isNullOrBlank()) return null

        return try {
            Uri.parse(ruta)
        } catch (_: Exception) {
            null
        }
    }

    private fun mostrarImagenExistente(
        uri: Uri?,
        vista: ImageView
    ) {

        if (uri == null) return

        vista.imageTintList = null
        vista.clearColorFilter()
        vista.scaleType = ImageView.ScaleType.CENTER_CROP
        vista.tag = uri.toString()

        if (uri.scheme == "https" || uri.scheme == "http") {

            executorImagenes.execute {
                var conexion: HttpURLConnection? = null

                try {
                    conexion = URL(uri.toString()).openConnection() as HttpURLConnection
                    conexion.connectTimeout = 15000
                    conexion.readTimeout = 20000
                    conexion.requestMethod = "GET"

                    if (conexion.responseCode !in 200..299) {
                        throw java.io.IOException(
                            "El servidor respondió HTTP ${conexion.responseCode}"
                        )
                    }

                    val bitmap = conexion.inputStream.use { input ->
                        BitmapFactory.decodeStream(input)
                    }

                    runOnUiThread {
                        if (vista.tag == uri.toString() && bitmap != null) {
                            vista.setImageBitmap(bitmap)
                        }
                    }
                } catch (_: Exception) {
                    runOnUiThread {
                        if (vista.tag == uri.toString()) {
                            mostrarMensaje("No fue posible cargar la imagen remota.")
                        }
                    }
                } finally {
                    conexion?.disconnect()
                }
            }

        } else {
            try {
                val bitmap = contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }

                if (bitmap != null) {
                    vista.setImageDrawable(null)
                    vista.setImageBitmap(bitmap)
                } else {
                    mostrarMensaje("No fue posible cargar una imagen.")
                }
            } catch (_: Exception) {
                mostrarMensaje("No fue posible mostrar una imagen guardada.")
            }
        }
    }

    private fun configurarEventos() {

        btnVolver.setOnClickListener {
            finish()
        }

        contenedorImagenPrincipal.setOnClickListener {
            abrirSelectorImagen(1)
        }

        btnImagen2.setOnClickListener {
            abrirSelectorImagen(2)
        }

        btnImagen3.setOnClickListener {
            abrirSelectorImagen(3)
        }

        btnImagen4.setOnClickListener {
            abrirSelectorImagen(4)
        }

        btnGuardar.setOnClickListener {
            guardarProducto()
        }
    }

    private fun abrirSelectorImagen(numero: Int) {

        imagenSeleccionada = numero
        mostrarOpcionesImagen()
    }

    private fun mostrarOpcionesImagen() {

        val opciones = arrayOf(
            "Tomar fotografía",
            "Seleccionar desde galería"
        )

        AlertDialog.Builder(this)
            .setTitle("Agregar imagen del producto")
            .setItems(opciones) { _, seleccion ->
                when (seleccion) {
                    0 -> tomarFotografia.launch(null)
                    1 -> selectorImagen.launch(
                        arrayOf("image/jpeg", "image/png")
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarFotografiaTemporal(bitmap: Bitmap) {

        try {
            val uriGuardado = MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "TechMarket_${System.currentTimeMillis()}",
                "Fotografía de producto"
            )

            if (uriGuardado.isNullOrBlank()) {
                mostrarMensaje("No fue posible guardar la fotografía.")
                return
            }

            val uri = Uri.parse(uriGuardado)

            if (!validarImagen(uri)) {
                mostrarMensaje("La fotografía no es válida o supera los 5 MB.")
                return
            }

            asignarImagen(uri)

        } catch (e: Exception) {
            mostrarMensaje(
                "Error al guardar la fotografía: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    private fun validarImagen(uri: Uri): Boolean {

        return try {

            val tipo = contentResolver.getType(uri)

            if (tipo != "image/jpeg" && tipo != "image/png") {
                return false
            }

            val cursor = contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )

            val tamano = cursor?.use {

                if (it.moveToFirst()) {

                    val indice = it.getColumnIndex(OpenableColumns.SIZE)

                    if (indice >= 0 && !it.isNull(indice)) {
                        it.getLong(indice)
                    } else {
                        -1L
                    }

                } else {
                    -1L
                }

            } ?: -1L

            tamano == -1L || tamano <= 5L * 1024L * 1024L

        } catch (_: Exception) {
            false
        }
    }

    private fun asignarImagen(uri: Uri) {

        when (imagenSeleccionada) {

            1 -> {
                imagenPrincipal = uri
                imgPrincipal.imageTintList = null
                imgPrincipal.clearColorFilter()
                imgPrincipal.setImageURI(uri)
                imgPrincipal.scaleType = ImageView.ScaleType.CENTER_CROP
            }

            2 -> {
                imagen2 = uri
                btnImagen2.imageTintList = null
                btnImagen2.clearColorFilter()
                btnImagen2.setImageURI(uri)
                btnImagen2.scaleType = ImageView.ScaleType.CENTER_CROP
            }

            3 -> {
                imagen3 = uri
                btnImagen3.imageTintList = null
                btnImagen3.clearColorFilter()
                btnImagen3.setImageURI(uri)
                btnImagen3.scaleType = ImageView.ScaleType.CENTER_CROP
            }

            4 -> {
                imagen4 = uri
                btnImagen4.imageTintList = null
                btnImagen4.clearColorFilter()
                btnImagen4.setImageURI(uri)
                btnImagen4.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    // ==========================================
    // GUARDAR O ACTUALIZAR PRODUCTO
    // ==========================================

    private fun guardarProducto() {

        val nombre = etNombre.text.toString().trim()
        val categoria = spCategoria.selectedItem?.toString() ?: ""
        val precioTexto = etPrecio.text.toString().trim().replace(",", ".")
        val stockTexto = etStock.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombre.error = "Ingresa el nombre del producto"
            etNombre.requestFocus()
            return
        }

        if (categoria == "Selecciona una categoría" || categoria.isBlank()) {
            mostrarMensaje("Selecciona una categoría.")
            return
        }

        val precio = precioTexto.toDoubleOrNull()
        if (precio == null || precio <= 0) {
            etPrecio.error = "Ingresa un precio válido"
            etPrecio.requestFocus()
            return
        }

        val stock = stockTexto.toIntOrNull()
        if (stock == null || stock < 0) {
            etStock.error = "Ingresa una cantidad válida"
            etStock.requestFocus()
            return
        }

        if (descripcion.isEmpty()) {
            etDescripcion.error = "Ingresa la descripción"
            etDescripcion.requestFocus()
            return
        }

        if (imagenPrincipal == null) {
            mostrarMensaje("Debes seleccionar una imagen principal.")
            return
        }

        if (idTienda <= 0) {
            mostrarMensaje("No se pudo identificar tu tienda.")
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text = "Subiendo imágenes..."

        val imagenes = listOf(imagenPrincipal, imagen2, imagen3, imagen4)
        val imagenesFinales = imagenes.map { it?.toString() }.toMutableList()
        val tareas = mutableListOf<Pair<Int, com.google.android.gms.tasks.Task<String>>>()

        imagenes.forEachIndexed { indice, uri ->
            if (uri != null && uri.scheme != "https" && uri.scheme != "http") {
                tareas.add(
                    indice to CloudinaryManager.subirImagen(this, uri)
                )
            }
        }

        val tareaCarga = if (tareas.isEmpty()) {
            Tasks.forResult(emptyList<String>())
        } else {
            Tasks.whenAllSuccess<String>(tareas.map { it.second })
        }

        tareaCarga
            .addOnSuccessListener { urlsSubidas ->
                tareas.forEachIndexed { indiceTarea, tarea ->
                    imagenesFinales[tarea.first] = urlsSubidas[indiceTarea]
                }

                guardarProductoLocalYFirestore(
                    nombre = nombre,
                    categoria = categoria,
                    precio = precio,
                    stock = stock,
                    descripcion = descripcion,
                    imagenes = imagenesFinales
                )
            }
            .addOnFailureListener { error ->
                restaurarBotonGuardar()
                mostrarMensaje(
                    "No fue posible cargar las imágenes en Cloudinary: ${error.localizedMessage ?: error.message}"
                )
            }
    }

    // ==========================================
    // GUARDAR EN SQLITE Y SINCRONIZAR FIRESTORE
    // ==========================================

    private fun guardarProductoLocalYFirestore(
        nombre: String,
        categoria: String,
        precio: Double,
        stock: Int,
        descripcion: String,
        imagenes: List<String?>
    ) {

        val principal = imagenes.getOrNull(0)
        if (principal.isNullOrBlank()) {
            restaurarBotonGuardar()
            mostrarMensaje("No se obtuvo la URL de la imagen principal.")
            return
        }

        try {
            val idProducto: Long

            if (esEdicion) {
                val actualizado = productoDAO.actualizarProductoTienda(
                    idProducto = idProductoEditar,
                    idTienda = idTienda,
                    nombre = nombre,
                    categoria = categoria,
                    precio = precio,
                    stock = stock,
                    descripcion = descripcion,
                    disponible = switchDisponible.isChecked,
                    imagenPrincipal = principal,
                    imagen2 = imagenes.getOrNull(1),
                    imagen3 = imagenes.getOrNull(2),
                    imagen4 = imagenes.getOrNull(3)
                )

                if (!actualizado) {
                    restaurarBotonGuardar()
                    mostrarMensaje("No fue posible actualizar el producto localmente.")
                    return
                }

                idProducto = idProductoEditar
            } else {
                idProducto = productoDAO.guardarProductoTienda(
                    idTienda = idTienda,
                    nombre = nombre,
                    categoria = categoria,
                    precio = precio,
                    stock = stock,
                    descripcion = descripcion,
                    disponible = switchDisponible.isChecked,
                    imagenPrincipal = principal,
                    imagen2 = imagenes.getOrNull(1),
                    imagen3 = imagenes.getOrNull(2),
                    imagen4 = imagenes.getOrNull(3)
                )

                if (idProducto <= 0) {
                    restaurarBotonGuardar()
                    mostrarMensaje("No fue posible guardar el producto localmente.")
                    return
                }
            }

            btnGuardar.text = "Sincronizando con Firestore..."

            FirebaseManager.sincronizarProducto(
                idProducto = idProducto,
                idTienda = idTienda,
                nombre = nombre,
                categoria = categoria,
                precio = precio,
                stock = stock,
                descripcion = descripcion,
                disponible = switchDisponible.isChecked,
                imagenPrincipal = principal,
                imagen2 = imagenes.getOrNull(1),
                imagen3 = imagenes.getOrNull(2),
                imagen4 = imagenes.getOrNull(3)
            )
                .addOnSuccessListener {
                    mostrarMensaje(
                        if (esEdicion) {
                            "Producto actualizado y sincronizado correctamente."
                        } else {
                            "Producto registrado y sincronizado correctamente."
                        }
                    )
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { error ->
                    mostrarMensaje(
                        "El producto quedó guardado localmente, pero falló la sincronización con Firestore: ${error.localizedMessage ?: error.message}"
                    )
                    setResult(RESULT_OK)
                    finish()
                }

        } catch (e: Exception) {
            restaurarBotonGuardar()
            mostrarMensaje(
                "Ocurrió un error al guardar el producto: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    private fun restaurarBotonGuardar() {
        btnGuardar.isEnabled = true
        btnGuardar.text = if (esEdicion) "Guardar cambios" else "Guardar producto"
    }

    private fun finalizarConError(mensaje: String) {

        mostrarMensaje(mensaje)
        setResult(RESULT_OK)
        finish()
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}