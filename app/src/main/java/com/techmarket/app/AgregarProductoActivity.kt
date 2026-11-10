package com.techmarket.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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
import androidx.appcompat.app.AppCompatActivity
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

            tiendaDAO.consultarTiendaPorUsuario(usuarioId)
                .use { cursorTienda ->

                    if (cursorTienda.moveToFirst()) {
                        idTienda = cursorTienda.getLong(
                            cursorTienda.getColumnIndexOrThrow(
                                DatabaseContract.Tiendas.ID
                            )
                        )
                    }
                }

            if (idTienda <= 0) {
                mostrarMensaje(
                    "No se encontró la tienda asociada al vendedor."
                )
                finish()
                return
            }

            // Si se recibió un ID, cargar el producto para editarlo.
            if (esEdicion) {
                cargarProductoParaEditar()
            }

        } catch (e: Exception) {
            mostrarMensaje("No fue posible consultar la tienda.")
            finish()
        }
    }

    private fun cargarProductoParaEditar() {

        if (idProductoEditar <= 0 || idTienda <= 0) return

        try {

            // Primero se comprueba que el producto pertenezca
            // a la tienda del vendedor que inició sesión.
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

                    // Recuperar las imágenes que ya tiene el producto.
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

                    // Mostrar las imágenes existentes.
                    mostrarImagenExistente(
                        imagenPrincipal,
                        imgPrincipal
                    )

                    mostrarImagenExistente(
                        imagen2,
                        btnImagen2
                    )

                    mostrarImagenExistente(
                        imagen3,
                        btnImagen3
                    )

                    mostrarImagenExistente(
                        imagen4,
                        btnImagen4
                    )
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

        try {
            val bitmap = contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }

            if (bitmap == null) {
                mostrarMensaje(
                    "No fue posible cargar una de las imágenes guardadas."
                )
                return
            }

            vista.imageTintList = null
            vista.imageTintMode = null
            vista.clearColorFilter()
            vista.setImageDrawable(null)
            vista.setImageBitmap(bitmap)
            vista.scaleType = ImageView.ScaleType.CENTER_CROP

        } catch (_: Exception) {
            mostrarMensaje(
                "No fue posible mostrar una de las imágenes guardadas."
            )
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

        selectorImagen.launch(
            arrayOf("image/jpeg", "image/png")
        )
    }

    private fun validarImagen(uri: Uri): Boolean {

        return try {

            val tipo = contentResolver.getType(uri)

            if (tipo != "image/jpeg" && tipo != "image/png") {
                return false
            }

            val cursor = contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.SIZE),
                null,
                null,
                null
            )

            val tamano = cursor?.use {

                if (it.moveToFirst()) {

                    val indice = it.getColumnIndex(
                        android.provider.OpenableColumns.SIZE
                    )

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

    // MÉTODO CORREGIDO: mostrar las fotografías sin el tinte azul.
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

    private fun guardarProducto() {

        val nombre = etNombre.text.toString().trim()

        val categoria = spCategoria.selectedItem?.toString() ?: ""

        val precioTexto = etPrecio.text.toString()
            .trim()
            .replace(",", ".")

        val stockTexto = etStock.text.toString().trim()

        val descripcion = etDescripcion.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombre.error = "Ingresa el nombre del producto"
            etNombre.requestFocus()
            return
        }

        if (categoria == "Selecciona una categoría" ||
            categoria.isBlank()
        ) {
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

        // En edición se permite conservar la imagen principal existente.
        if (imagenPrincipal == null) {
            mostrarMensaje(
                "Debes seleccionar una imagen principal."
            )
            return
        }

        if (idTienda <= 0) {
            mostrarMensaje("No se pudo identificar tu tienda.")
            return
        }

        try {

            if (esEdicion) {

                // Actualizar el producto verificando su tienda propietaria.
                val actualizado = productoDAO.actualizarProductoTienda(
                    idProducto = idProductoEditar,
                    idTienda = idTienda,
                    nombre = nombre,
                    categoria = categoria,
                    precio = precio,
                    stock = stock,
                    descripcion = descripcion,
                    disponible = switchDisponible.isChecked,
                    imagenPrincipal = imagenPrincipal?.toString(),
                    imagen2 = imagen2?.toString(),
                    imagen3 = imagen3?.toString(),
                    imagen4 = imagen4?.toString()
                )

                if (actualizado) {

                    mostrarMensaje(
                        "Producto actualizado correctamente."
                    )

                    setResult(RESULT_OK)
                    finish()

                } else {
                    mostrarMensaje(
                        "No fue posible actualizar el producto."
                    )
                }

            } else {

                // Registrar un producto nuevo.
                val resultado = productoDAO.guardarProductoTienda(
                    idTienda = idTienda,
                    nombre = nombre,
                    categoria = categoria,
                    precio = precio,
                    stock = stock,
                    descripcion = descripcion,
                    disponible = switchDisponible.isChecked,
                    imagenPrincipal = imagenPrincipal?.toString(),
                    imagen2 = imagen2?.toString(),
                    imagen3 = imagen3?.toString(),
                    imagen4 = imagen4?.toString()
                )

                if (resultado > 0) {

                    mostrarMensaje(
                        "Producto registrado correctamente."
                    )

                    setResult(RESULT_OK)
                    finish()

                } else {
                    mostrarMensaje(
                        "No fue posible guardar el producto."
                    )
                }
            }

        } catch (e: Exception) {
            mostrarMensaje(
                "Ocurrió un error al guardar el producto."
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
}
