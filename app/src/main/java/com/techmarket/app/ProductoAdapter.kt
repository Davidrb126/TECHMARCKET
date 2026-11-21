package com.techmarket.app

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

    // Lista interna modificable para permitir actualizar los filtros.
    private val productos = productos.toMutableList()

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

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

        holder.nombre.text = producto.nombre
        holder.precio.text = producto.precio
        holder.calificacion.text = producto.calificacion

        // Imagen predeterminada mientras carga la imagen real.
        holder.imagen.setImageResource(producto.imagenResId)
        holder.imagen.imageTintList = null
        holder.imagen.clearColorFilter()

        // Cargar imagen local o URL HTTPS de Cloudinary.
        if (!producto.imagenUri.isNullOrBlank()) {

            ImagenesManager.cargar(
                vista = holder.imagen,
                origen = producto.imagenUri,
                imagenPredeterminada = producto.imagenResId
            )

        } else {

            holder.imagen.tag = null
            holder.imagen.setImageResource(producto.imagenResId)
        }

        holder.itemView.setOnClickListener {
            onProductoClick(producto)
        }

        holder.btnAgregar.setOnClickListener {
            onAgregarClick(producto)
        }
    }

    override fun getItemCount(): Int = productos.size

    // Actualizar productos cuando se aplica un filtro o búsqueda.
    fun actualizarLista(nuevaLista: List<Producto>) {

        productos.clear()
        productos.addAll(nuevaLista)

        notifyDataSetChanged()
    }
}
