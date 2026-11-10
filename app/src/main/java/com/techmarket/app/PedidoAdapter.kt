package com.techmarket.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PedidoAdapter(
    private var pedidos: List<Pedido>,
    private val onPedidoClick: ((Pedido) -> Unit)? = null
) : RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    class PedidoViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imgProducto: ImageView =
            itemView.findViewById(R.id.imgProductoPedido)

        val tvNumero: TextView =
            itemView.findViewById(R.id.tvNumeroPedido)

        val tvFecha: TextView =
            itemView.findViewById(R.id.tvFechaPedido)

        val tvTotal: TextView =
            itemView.findViewById(R.id.tvTotalPedido)

        val tvEstado: TextView =
            itemView.findViewById(R.id.tvEstadoPedido)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PedidoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)

        return PedidoViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: PedidoViewHolder,
        position: Int
    ) {

        val pedido = pedidos[position]

        holder.tvNumero.text = "Pedido #${pedido.numeroPedido}"
        holder.tvFecha.text = pedido.fecha
        holder.tvTotal.text = formatearPrecio(pedido.total)

        // Mostrar el icono uniforme de pedido.
        holder.imgProducto.setImageResource(R.drawable.ic_pedido)

        when (pedido.estado) {

            "Entregado" -> {
                holder.tvEstado.text = "✓ Entregado"

                holder.tvEstado.setBackgroundResource(
                    R.drawable.bg_estado_pedido_entregado
                )

                holder.tvEstado.setTextColor(
                    Color.rgb(8, 116, 67)
                )
            }

            "Enviado" -> {
                holder.tvEstado.text = "✓ Enviado"

                holder.tvEstado.setBackgroundResource(
                    R.drawable.bg_estado_pedido_proceso
                )

                holder.tvEstado.setTextColor(
                    Color.rgb(29, 78, 216)
                )
            }

            "Cancelado" -> {
                holder.tvEstado.text = "Cancelado"

                holder.tvEstado.setBackgroundResource(
                    R.drawable.bg_estado_pedido_cancelado
                )

                holder.tvEstado.setTextColor(
                    Color.rgb(185, 28, 28)
                )
            }

            else -> {
                holder.tvEstado.text = "✓ En proceso"

                holder.tvEstado.setBackgroundResource(
                    R.drawable.bg_estado_pedido_proceso
                )

                holder.tvEstado.setTextColor(
                    Color.rgb(8, 116, 67)
                )
            }
        }

        // Abrir el detalle cuando el comprador selecciona el pedido.
        holder.itemView.setOnClickListener {
            onPedidoClick?.invoke(pedido)
        }
    }

    override fun getItemCount(): Int = pedidos.size

    fun actualizarPedidos(nuevaLista: List<Pedido>) {
        pedidos = nuevaLista
        notifyDataSetChanged()
    }

    private fun formatearPrecio(valor: Long): String {
        val numero = String.format("%,d", valor).replace(",", ".")
        return "$ $numero"
    }
}
