package com.techmarket.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class PedidoVendedorAdapter(
    private val pedidos: List<Pedido>,
    private val onPedidoClick: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidoVendedorAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val numero: TextView =
            itemView.findViewById(R.id.tvNumeroPedidoVendedor)

        val estado: TextView =
            itemView.findViewById(R.id.tvEstadoPedidoVendedor)

        val fecha: TextView =
            itemView.findViewById(R.id.tvFechaPedidoVendedor)

        val productos: TextView =
            itemView.findViewById(R.id.tvProductosPedidoVendedor)

        val direccion: TextView =
            itemView.findViewById(R.id.tvDireccionPedidoVendedor)

        val metodoPago: TextView =
            itemView.findViewById(R.id.tvMetodoPagoPedidoVendedor)

        val subtotal: TextView =
            itemView.findViewById(R.id.tvSubtotalPedidoVendedor)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PedidoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_pedido_vendedor,
                parent,
                false
            )

        return PedidoViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: PedidoViewHolder,
        position: Int
    ) {

        val pedido = pedidos[position]

        holder.numero.text = "Pedido ${pedido.numeroPedido}"
        holder.estado.text = pedido.estado
        holder.fecha.text = pedido.fecha

        holder.productos.text = pedido.productos.joinToString("\n") {
            "• ${it.cantidad} x ${it.producto.nombre} — ${it.producto.precio}"
        }

        holder.direccion.text = pedido.direccion

        holder.metodoPago.text =
            "Método de pago: ${pedido.metodoPago}"

        // Calcular únicamente los productos de esta tienda.
        val subtotalTienda = pedido.productos.sumOf { item ->

            val precio = item.producto.precio
                .filter { it.isDigit() }
                .toLongOrNull() ?: 0L

            precio * item.cantidad
        }

        val formatoMoneda =
            NumberFormat.getCurrencyInstance(Locale("es", "CO"))

        holder.subtotal.text =
            "Subtotal de tu tienda: ${formatoMoneda.format(subtotalTienda)}"

        // Abrir las opciones del pedido.
        holder.itemView.setOnClickListener {
            onPedidoClick(pedido)
        }
    }

    override fun getItemCount(): Int = pedidos.size
}
