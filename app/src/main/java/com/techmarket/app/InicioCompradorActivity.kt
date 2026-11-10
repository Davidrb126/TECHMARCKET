package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.techmarket.app.database.DatabaseContract
import com.techmarket.app.database.CategoriaDAO
import com.techmarket.app.database.ProductoDAO
import java.util.Locale

class InicioCompradorActivity : AppCompatActivity() {

    private lateinit var tvContadorCarrito: TextView
    private lateinit var rvCategorias: RecyclerView
    private lateinit var rvProductos: RecyclerView

    private lateinit var productoDAO: ProductoDAO
    private lateinit var categoriaDAO: CategoriaDAO
    private lateinit var productoAdapter: ProductoAdapter

    private val listaProductos = mutableListOf<Producto>()

    // Categoría seleccionada. Null significa mostrar todos.
    private var categoriaSeleccionada: String? = null

    // Texto ingresado en el buscador.
    private var textoBusqueda: String = ""

    // ==========================================
    // CICLO DE VIDA
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_comprador)

        productoDAO = ProductoDAO(this)
        categoriaDAO = CategoriaDAO(this)

        inicializarComponentes()
        configurarCategorias()
        configurarProductos()
        configurarEventos()
        actualizarContadorCarrito()
    }

    override fun onResume() {
        super.onResume()

        if (::categoriaDAO.isInitialized && ::rvCategorias.isInitialized) {
            cargarCategorias()
        }

        if (::tvContadorCarrito.isInitialized) {
            actualizarContadorCarrito()
        }

        if (::productoDAO.isInitialized && ::productoAdapter.isInitialized) {
            cargarProductos()
        }
    }

    // ==========================================
    // INICIALIZAR COMPONENTES
    // ==========================================

    private fun inicializarComponentes() {

        tvContadorCarrito = findViewById(R.id.tvContadorCarrito)
        rvCategorias = findViewById(R.id.rvCategorias)
        rvProductos = findViewById(R.id.rvProductos)
    }

    // ==========================================
    // CONFIGURAR CATEGORÍAS
    // ==========================================

    private fun configurarCategorias() {

        rvCategorias.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        cargarCategorias()
    }

    // ==========================================
    // CARGAR CATEGORÍAS DESDE SQLITE
    // ==========================================

    private fun cargarCategorias() {

        try {

            // Consultar únicamente las categorías activas.
            val categoriasBD = categoriaDAO.obtenerCategorias(
                incluirInactivas = false
            )

            // Mostrar exclusivamente las categorías registradas.
            val categorias = categoriasBD.map { registro ->

                Categoria(
                    nombre = registro.nombre,
                    icono = registro.icono
                )
            }

            // Comprobar que la categoría seleccionada siga activa.
            if (
                categoriaSeleccionada != null &&
                categorias.none {
                    it.nombre.equals(
                        categoriaSeleccionada,
                        ignoreCase = true
                    )
                }
            ) {
                categoriaSeleccionada = null
            }

            rvCategorias.adapter = CategoriaAdapter(categorias) { categoria ->

                categoriaSeleccionada = categoria.nombre

                aplicarFiltroCategoria()
            }

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No fue posible cargar las categorías: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // CONFIGURAR PRODUCTOS
    // ==========================================

    private fun configurarProductos() {

        // Productos organizados en dos columnas.
        rvProductos.layoutManager = GridLayoutManager(
            this,
            2,
            RecyclerView.VERTICAL,
            false
        )

        rvProductos.isNestedScrollingEnabled = false
        rvProductos.setHasFixedSize(false)
        rvProductos.overScrollMode = View.OVER_SCROLL_NEVER

        productoAdapter = ProductoAdapter(
            emptyList(),

            onProductoClick = { producto ->
                abrirDetalleProducto(producto)
            },

            onAgregarClick = { producto ->

                CarritoManager.agregar(producto, 1)

                actualizarContadorCarrito()

                Toast.makeText(
                    this,
                    "${producto.nombre} agregado al carrito.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        rvProductos.adapter = productoAdapter

        cargarProductos()
    }

    // ==========================================
    // CARGAR PRODUCTOS DESDE SQLITE
    // ==========================================

    private fun cargarProductos() {

        val nuevosProductos = mutableListOf<Producto>()

        try {

            val cursor = productoDAO.obtenerProductosDisponibles()

            cursor.use {

                while (it.moveToNext()) {

                    val idProducto = it.getLong(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.ProductosTienda.ID
                        )
                    )

                    val nombre = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.ProductosTienda.NOMBRE
                        )
                    ).orEmpty()

                    val precio = it.getDouble(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.ProductosTienda.PRECIO
                        )
                    )

                    val descripcion = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.ProductosTienda.DESCRIPCION
                        )
                    ).orEmpty()

                    val imagen = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.ProductosTienda.IMAGEN_PRINCIPAL
                        )
                    )

                    // Leer la categoría asociada al producto.
                    val categoria = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.ProductosTienda.CATEGORIA
                        )
                    ).orEmpty()

                    val producto = Producto(
                        nombre = nombre,
                        precio = String.format(
                            Locale("es", "CO"),
                            "$ %,.0f",
                            precio
                        ),
                        calificacion = "⭐ Nuevo",
                        imagenResId = R.drawable.ic_carrito,
                        descripcion = descripcion,
                        activo = true,
                        publicado = true,
                        idProducto = idProducto,
                        imagenUri = imagen,
                        categoria = categoria
                    )

                    nuevosProductos.add(producto)
                }
            }

            // Actualizar la lista completa.
            listaProductos.clear()
            listaProductos.addAll(nuevosProductos)

            // Aplicar los filtros correspondientes.
            aplicarFiltroCategoria()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No fue posible cargar los productos: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // FILTRAR PRODUCTOS POR CATEGORÍA Y BÚSQUEDA
    // ==========================================

    private fun aplicarFiltroCategoria() {

        // Primero se aplica la categoría y luego el texto de búsqueda.
        val productosFiltrados = listaProductos.filter { producto ->

            val coincideCategoria =
                categoriaSeleccionada == null ||
                        producto.categoria.trim().equals(
                            categoriaSeleccionada?.trim(),
                            ignoreCase = true
                        )

            val coincideBusqueda =
                textoBusqueda.isBlank() ||
                        producto.nombre.contains(
                            textoBusqueda.trim(),
                            ignoreCase = true
                        )

            coincideCategoria && coincideBusqueda
        }

        // Actualizar el adaptador.
        productoAdapter.actualizarLista(productosFiltrados)

        if (productosFiltrados.isEmpty()) {

            val mensaje = when {

                textoBusqueda.isNotBlank() &&
                        categoriaSeleccionada != null -> {
                    "No se encontraron productos en esta categoría."
                }

                textoBusqueda.isNotBlank() -> {
                    "No se encontraron productos con ese nombre."
                }

                categoriaSeleccionada != null -> {
                    "No hay productos disponibles en $categoriaSeleccionada."
                }

                else -> {
                    "Todavía no hay productos disponibles."
                }
            }

            Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==========================================
    // ABRIR DETALLE DEL PRODUCTO
    // ==========================================

    private fun abrirDetalleProducto(producto: Producto) {

        val intent = Intent(
            this,
            DetalleProductoActivity::class.java
        ).apply {

            putExtra("nombre", producto.nombre)
            putExtra("precio", producto.precio)
            putExtra("calificacion", producto.calificacion)
            putExtra("descripcion", producto.descripcion)
            putExtra("imagenResId", producto.imagenResId)

            producto.idProducto?.let { id ->

                putExtra("idProducto", id)

                // Consultar las imágenes adicionales guardadas en SQLite.
                productoDAO.consultarProductoTienda(id).use { cursor ->

                    if (cursor.moveToFirst()) {

                        val imagen2 = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.IMAGEN_2
                            )
                        )

                        val imagen3 = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.IMAGEN_3
                            )
                        )

                        val imagen4 = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.IMAGEN_4
                            )
                        )

                        putExtra("imagenUri2", imagen2)
                        putExtra("imagenUri3", imagen3)
                        putExtra("imagenUri4", imagen4)
                    }
                }
            }

            producto.imagenUri?.let {
                putExtra("imagenUri", it)
            }
        }

        startActivity(intent)
    }

    // ==========================================
    // CONFIGURAR EVENTOS
    // ==========================================

    private fun configurarEventos() {

        // ABRIR CARRITO

        findViewById<FrameLayout>(R.id.btnAbrirCarrito)
            .setOnClickListener {

                startActivity(
                    Intent(this, CarritoActivity::class.java)
                )
            }

        // VER OFERTAS

        findViewById<TextView>(R.id.btnVerOfertas)
            .setOnClickListener {

                mostrarMensaje("Ofertas")
            }

        // BUSCADOR DE PRODUCTOS

        findViewById<EditText>(R.id.etBuscarProductos)
            .addTextChangedListener(object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    textoBusqueda = s?.toString().orEmpty()

                    aplicarFiltroCategoria()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })

        // VER TODOS LOS PRODUCTOS

        findViewById<TextView>(R.id.btnVerTodos)
            .setOnClickListener {

                categoriaSeleccionada = null

                aplicarFiltroCategoria()
            }

        // CATEGORÍAS

        findViewById<View>(R.id.navCategorias)
            .setOnClickListener {

                mostrarMensaje("Categorías")
            }

        // FAVORITOS

        findViewById<View>(R.id.navFavoritos)
            .setOnClickListener {

                mostrarMensaje("Favoritos")
            }

        // PERFIL SEGÚN EL ROL DEL USUARIO

        findViewById<View>(R.id.navPerfil)
            .setOnClickListener {

                val rol = SessionManager.obtenerRol(this).orEmpty()

                when {

                    rol.equals(
                        "ADMINISTRADOR",
                        ignoreCase = true
                    ) -> {

                        startActivity(
                            Intent(
                                this,
                                InicioAdministradorActivity::class.java
                            )
                        )
                    }

                    rol.equals(
                        "VENDEDOR",
                        ignoreCase = true
                    ) -> {

                        startActivity(
                            Intent(
                                this,
                                InicioVendedorActivity::class.java
                            )
                        )
                    }

                    else -> {

                        startActivity(
                            Intent(
                                this,
                                PerfilCompradorActivity::class.java
                            )
                        )
                    }
                }
            }

        // INICIO

        findViewById<View>(R.id.navInicio)
            .setOnClickListener {

                Toast.makeText(
                    this,
                    "Ya estás en Inicio.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // MENÚ DEL COMPRADOR

        findViewById<TextView>(R.id.btnMenuComprador)
            .setOnClickListener {

                mostrarMensaje("Menú del comprador")
            }
    }

    // ==========================================
    // ACTUALIZAR CONTADOR DEL CARRITO
    // ==========================================

    private fun actualizarContadorCarrito() {

        val cantidad = CarritoManager.obtenerCantidadTotal()

        if (cantidad > 0) {

            tvContadorCarrito.text = if (cantidad > 99) {
                "99+"
            } else {
                cantidad.toString()
            }

            tvContadorCarrito.visibility = View.VISIBLE

        } else {

            tvContadorCarrito.text = ""
            tvContadorCarrito.visibility = View.GONE
        }
    }

    // ==========================================
    // MOSTRAR MENSAJES
    // ==========================================

    private fun mostrarMensaje(seccion: String) {

        Toast.makeText(
            this,
            "$seccion estará disponible próximamente.",
            Toast.LENGTH_SHORT
        ).show()
    }
}
