package com.techmarket.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CarritoAdapter(
    private val productos: List<CarritoItem>,
    private val onAumentar: (CarritoItem) -> Unit,
    private val onDisminuir: (CarritoItem) -> Unit,
    private val onEliminar: (CarritoItem) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    class CarritoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imagen: ImageView =
            itemView.findViewById(R.id.imgProductoCarrito)

        val nombre: TextView =
            itemView.findViewById(R.id.tvNombreProductoCarrito)

        val precio: TextView =
            itemView.findViewById(R.id.tvPrecioProductoCarrito)

        val cantidad: TextView =
            itemView.findViewById(R.id.tvCantidadProductoCarrito)

        val btnAumentar: ImageButton =
            itemView.findViewById(R.id.btnAumentarCarrito)

        val btnDisminuir: ImageButton =
            itemView.findViewById(R.id.btnDisminuirCarrito)

        val btnEliminar: ImageButton =
            itemView.findViewById(R.id.btnEliminarProductoCarrito)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CarritoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)

        return CarritoViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: CarritoViewHolder,
        position: Int
    ) {

        val item = productos[position]
        val producto = item.producto

        holder.nombre.text = producto.nombre
        holder.precio.text = producto.precio
        holder.cantidad.text = item.cantidad.toString()

        // Mostrar imagen real o imagen predeterminada
        if (!producto.imagenUri.isNullOrBlank()) {

            try {
                holder.imagen.setImageURI(
                    Uri.parse(producto.imagenUri)
                )
            } catch (e: Exception) {
                holder.imagen.setImageResource(producto.imagenResId)
            }

        } else {

            holder.imagen.setImageResource(producto.imagenResId)
        }

        // Aumentar cantidad
        holder.btnAumentar.setOnClickListener {
            onAumentar(item)
        }

        // Disminuir cantidad
        holder.btnDisminuir.setOnClickListener {
            onDisminuir(item)
        }

        // Eliminar producto
        holder.btnEliminar.setOnClickListener {
            onEliminar(item)
        }
    }

    override fun getItemCount(): Int {
        return productos.size
    }
}
