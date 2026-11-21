package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.techmarket.app.database.DatabaseContract
import com.techmarket.app.database.ProductoDAO
import com.techmarket.app.database.TiendaDAO
import com.techmarket.app.database.UsuarioDAO

data class ProductoVendedor(
    val id: Long,
    val nombre: String,
    val categoria: String,
    val precio: Double,
    val stock: Int,
    val imagen: String?,
    val disponible: Boolean
)

class MisProductosActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID_PRODUCTO =
            "com.techmarket.app.EXTRA_ID_PRODUCTO"
        const val EXTRA_ID_TIENDA =
            "com.techmarket.app.EXTRA_ID_TIENDA"
    }

    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var tiendaDAO: TiendaDAO
    private lateinit var productoDAO: ProductoDAO
    private lateinit var adapter: ProductoVendedorAdapter

    private lateinit var rvProductos: RecyclerView
    private lateinit var tvCantidadProductos: TextView
    private lateinit var tvMensajeVacio: TextView

    private var idTienda: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_productos)

        usuarioDAO = UsuarioDAO(this)
        tiendaDAO = TiendaDAO(this)
        productoDAO = ProductoDAO(this)

        rvProductos = findViewById(R.id.rvProductosVendedor)
        tvCantidadProductos = findViewById(R.id.tvCantidadProductos)
        tvMensajeVacio = findViewById(R.id.tvMensajeVacio)

        adapter = ProductoVendedorAdapter(
            productos = mutableListOf(),

            onCambiarDisponibilidad = { producto ->
                cambiarDisponibilidad(producto)
            },

            onEliminar = { producto ->
                confirmarEliminacion(producto)
            },

            onEditar = { producto ->
                editarProducto(producto)
            }
        )

        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = adapter

        findViewById<ImageButton>(R.id.btnVolverProductos)
            .setOnClickListener {
                finish()
            }

        findViewById<Button>(R.id.btnAgregarProducto)
            .setOnClickListener {
                if (idTienda <= 0) {
                    mostrarMensaje("Selecciona una tienda antes de agregar productos.")
                    return@setOnClickListener
                }

                val intent = Intent(this, AgregarProductoActivity::class.java).apply {
                    putExtra(EXTRA_ID_TIENDA, idTienda)
                }
                startActivity(intent)
            }

        obtenerTiendaVendedor()
    }

    override fun onResume() {
        super.onResume()

        if (idTienda > 0) {
            cargarProductos()
        }
    }

    private fun obtenerTiendaVendedor() {

        val documento = SessionManager.obtenerDocumento(this)

        if (documento.isNullOrBlank()) {
            mostrarMensaje("No se encontró una sesión activa.")
            finish()
            return
        }

        try {
            var idUsuario = -1L

            usuarioDAO.consultarUsuario(documento).use { cursorUsuario ->
                if (cursorUsuario.moveToFirst()) {
                    idUsuario = cursorUsuario.getLong(
                        cursorUsuario.getColumnIndexOrThrow(
                            DatabaseContract.Usuarios.ID
                        )
                    )
                }
            }

            if (idUsuario <= 0) {
                mostrarMensaje("No se encontró el usuario.")
                finish()
                return
            }

            val tiendas = mutableListOf<Pair<Long, String>>()

            tiendaDAO.consultarTiendaPorUsuario(idUsuario).use { cursorTienda ->
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

            when {
                tiendas.isEmpty() -> {
                    mostrarMensaje("No se encontraron tiendas asociadas al vendedor.")
                    finish()
                }

                tiendas.size == 1 -> {
                    seleccionarTienda(tiendas.first().first)
                }

                else -> {
                    val nombresTiendas = tiendas.map { it.second }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Selecciona la tienda que deseas administrar")
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
        cargarProductos()
    }

    private fun cargarProductos() {

        if (idTienda <= 0) return

        val lista = mutableListOf<ProductoVendedor>()

        try {
            productoDAO.obtenerProductosTienda(idTienda).use { cursor ->

                while (cursor.moveToNext()) {

                    val producto = ProductoVendedor(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.ID
                            )
                        ),

                        nombre = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.NOMBRE
                            )
                        ).orEmpty(),

                        categoria = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.CATEGORIA
                            )
                        ).orEmpty(),

                        precio = cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.PRECIO
                            )
                        ),

                        stock = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.STOCK
                            )
                        ),

                        imagen = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.IMAGEN_PRINCIPAL
                            )
                        ),

                        disponible = cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                DatabaseContract.ProductosTienda.DISPONIBLE
                            )
                        ) == 1
                    )

                    lista.add(producto)
                }
            }

            adapter.actualizarLista(lista)

            tvCantidadProductos.text =
                "${lista.size} productos registrados"

            tvMensajeVacio.visibility =
                if (lista.isEmpty()) android.view.View.VISIBLE
                else android.view.View.GONE

            rvProductos.visibility =
                if (lista.isEmpty()) android.view.View.GONE
                else android.view.View.VISIBLE

        } catch (e: Exception) {
            mostrarMensaje("No fue posible cargar los productos.")
        }
    }

    private fun editarProducto(producto: ProductoVendedor) {

        val intent = Intent(
            this,
            AgregarProductoActivity::class.java
        ).apply {
            putExtra(EXTRA_ID_PRODUCTO, producto.id)
            putExtra(EXTRA_ID_TIENDA, idTienda)
        }

        startActivity(intent)
    }

    // ==========================================
    // CAMBIAR DISPONIBILIDAD DEL PRODUCTO
    // ==========================================

    private fun cambiarDisponibilidad(producto: ProductoVendedor) {

        if (idTienda <= 0) {
            mostrarMensaje("No se encontró la tienda del vendedor.")
            return
        }

        val nuevoEstado = !producto.disponible

        val actualizado = productoDAO.actualizarDisponibilidadProducto(
            producto.id,
            idTienda,
            nuevoEstado
        )

        if (actualizado) {
            mostrarMensaje("Estado del producto actualizado.")
            cargarProductos()
        } else {
            mostrarMensaje("No fue posible actualizar el producto.")
        }
    }

    // ==========================================
    // CONFIRMAR ELIMINACIÓN
    // ==========================================

    private fun confirmarEliminacion(producto: ProductoVendedor) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage(
                "¿Estás seguro de que deseas eliminar ${producto.nombre}?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                if (idTienda <= 0) {
                    mostrarMensaje(
                        "No se encontró la tienda del vendedor."
                    )
                    return@setPositiveButton
                }

                val eliminado = productoDAO.eliminarProductoTienda(
                    producto.id,
                    idTienda
                )

                if (eliminado) {
                    mostrarMensaje("Producto eliminado correctamente.")
                    cargarProductos()
                } else {
                    mostrarMensaje("No fue posible eliminar el producto.")
                }
            }
            .show()
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}

// ==========================================
// ADAPTADOR DE PRODUCTOS DEL VENDEDOR
// ==========================================

class ProductoVendedorAdapter(
    private val productos: MutableList<ProductoVendedor>,
    private val onCambiarDisponibilidad: (ProductoVendedor) -> Unit,
    private val onEliminar: (ProductoVendedor) -> Unit,
    private val onEditar: (ProductoVendedor) -> Unit
) : RecyclerView.Adapter<ProductoVendedorAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(
        val vista: android.view.View
    ) : RecyclerView.ViewHolder(vista) {

        val imagen: ImageView =
            vista.findViewById(R.id.imgProductoVendedor)

        val nombre: TextView =
            vista.findViewById(R.id.tvNombreProductoVendedor)

        val categoria: TextView =
            vista.findViewById(R.id.tvCategoriaProductoVendedor)

        val precio: TextView =
            vista.findViewById(R.id.tvPrecioProductoVendedor)

        val stock: TextView =
            vista.findViewById(R.id.tvStockProductoVendedor)

        val estado: Button =
            vista.findViewById(R.id.btnEstadoProductoVendedor)

        val eliminar: ImageButton =
            vista.findViewById(R.id.btnEliminarProductoVendedor)

        val editar: ImageButton =
            vista.findViewById(R.id.btnEditarProductoVendedor)
    }

    override fun onCreateViewHolder(
        parent: android.view.ViewGroup,
        viewType: Int
    ): ProductoViewHolder {

        val vista = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_vendedor, parent, false)

        return ProductoViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: ProductoViewHolder,
        position: Int
    ) {

        val producto = productos[position]

        holder.nombre.text = producto.nombre
        holder.categoria.text = producto.categoria

        holder.precio.text =
            "$ ${"%,.0f".format(java.util.Locale("es", "CO"), producto.precio)}"

        holder.stock.text = "Stock: ${producto.stock}"

        holder.estado.text =
            if (producto.disponible) "Activo" else "Inactivo"

        holder.estado.setOnClickListener {
            onCambiarDisponibilidad(producto)
        }

        holder.editar.setOnClickListener {
            onEditar(producto)
        }

        holder.eliminar.setOnClickListener {
            onEliminar(producto)
        }

        // Carga URL de Cloudinary o URI local de forma asíncrona.
        ImagenesManager.cargar(
            vista = holder.imagen,
            origen = producto.imagen
        )
    }

    override fun getItemCount(): Int = productos.size

    fun actualizarLista(nuevaLista: List<ProductoVendedor>) {
        productos.clear()
        productos.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}