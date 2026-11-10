package com.techmarket.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    productos: List<Producto>,
    private val onProductoClick: (Producto) -> Unit,
    private val onAgregarClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    // Lista interna independiente de la lista original.
    private val productos = productos.toMutableList()

    class ProductoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imagen: ImageView =
            itemView.findViewById(R.id.imgProducto)

        val nombre: TextView =
            itemView.findViewById(R.id.tvNombreProducto)

        val precio: TextView =
            itemView.findViewById(R.id.tvPrecioProducto)

        val calificacion: TextView =
            itemView.findViewById(R.id.tvCalificacionProducto)

        val btnAgregar: ImageButton =
            itemView.findViewById(R.id.btnAgregarProducto)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)

        return ProductoViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: ProductoViewHolder,
        position: Int
    ) {

        val producto = productos[position]

        // INFORMACIÓN DEL PRODUCTO

        holder.nombre.text = producto.nombre
        holder.precio.text = producto.precio
        holder.calificacion.text = producto.calificacion

        // RESTABLECER IMAGEN PARA EVITAR IMÁGENES RECICLADAS

        holder.imagen.setImageResource(producto.imagenResId)

        // CARGAR IMAGEN DESDE URI

        val imagenUri = producto.imagenUri

        if (!imagenUri.isNullOrBlank()) {
            try {
                holder.imagen.setImageURI(
                    Uri.parse(imagenUri)
                )
            } catch (e: Exception) {
                holder.imagen.setImageResource(
                    producto.imagenResId
                )
            }
        }

        // ABRIR DETALLE DEL PRODUCTO

        holder.itemView.setOnClickListener {
            onProductoClick(producto)
        }

        // AGREGAR AL CARRITO

        holder.btnAgregar.setOnClickListener {
            onAgregarClick(producto)
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }

    // ==========================================
    // ACTUALIZAR LISTA DE PRODUCTOS
    // ==========================================

    fun actualizarLista(nuevosProductos: List<Producto>) {

        productos.clear()
        productos.addAll(nuevosProductos)

        notifyDataSetChanged()
    }

    // ==========================================
    // OBTENER PRODUCTOS ACTUALES
    // ==========================================

    fun obtenerProductos(): List<Producto> {
        return productos.toList()
    }
}
