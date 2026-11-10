package com.techmarket.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoriaAdapter(
    private val categorias: List<Categoria>,
    private val onCategoriaClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    class CategoriaViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvIcono: TextView =
            itemView.findViewById(R.id.tvIconoCategoria)

        val tvNombre: TextView =
            itemView.findViewById(R.id.tvNombreCategoria)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoriaViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_categoria, parent, false)

        return CategoriaViewHolder(vista)
    }

    override fun onBindViewHolder(
        holder: CategoriaViewHolder,
        position: Int
    ) {
        val categoria = categorias[position]

        holder.tvIcono.text = categoria.icono
        holder.tvNombre.text = categoria.nombre

        holder.itemView.setOnClickListener {
            onCategoriaClick(categoria)
        }
    }

    override fun getItemCount(): Int {
        return categorias.size
    }
}