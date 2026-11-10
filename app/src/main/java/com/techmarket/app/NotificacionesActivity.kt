package com.techmarket.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.techmarket.app.database.NotificacionDAO

class NotificacionesActivity : AppCompatActivity() {

    private lateinit var notificacionDAO: NotificacionDAO
    private lateinit var documentoUsuario: String

    private lateinit var rvNotificaciones: RecyclerView
    private lateinit var contenedorVacio: View
    private lateinit var btnVolver: ImageView
    private lateinit var tvMarcarTodasLeidas: TextView

    private lateinit var adaptador: NotificacionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        notificacionDAO = NotificacionDAO(this)

        documentoUsuario =
            SessionManager.obtenerDocumento(this).orEmpty()

        inicializarVistas()
        configurarRecyclerView()
        configurarEventos()
        cargarNotificaciones()
    }

    private fun inicializarVistas() {
        rvNotificaciones = findViewById(R.id.rvNotificaciones)

        contenedorVacio =
            findViewById(R.id.contenedorVacioNotificaciones)

        btnVolver =
            findViewById(R.id.btnVolverNotificaciones)

        tvMarcarTodasLeidas =
            findViewById(R.id.tvMarcarTodasLeidas)
    }

    private fun configurarRecyclerView() {
        rvNotificaciones.layoutManager =
            LinearLayoutManager(this)

        adaptador = NotificacionAdapter(
            mutableListOf()
        ) { notificacion ->
            marcarComoLeida(notificacion)
        }

        rvNotificaciones.adapter = adaptador
    }

    private fun configurarEventos() {

        btnVolver.setOnClickListener {
            finish()
        }

        tvMarcarTodasLeidas.setOnClickListener {

            val resultado =
                notificacionDAO.marcarTodasNotificacionesLeidas(
                    documentoUsuario
                )

            if (resultado > 0) {
                Toast.makeText(
                    this,
                    "Todas las notificaciones fueron marcadas como leídas",
                    Toast.LENGTH_SHORT
                ).show()
            }

            cargarNotificaciones()
        }
    }

    private fun cargarNotificaciones() {

        if (documentoUsuario.isBlank()) {
            Toast.makeText(
                this,
                "No se pudo identificar al usuario",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        val lista =
            notificacionDAO.obtenerNotificaciones(documentoUsuario)

        adaptador.actualizarLista(lista)

        val hayNotificaciones = lista.isNotEmpty()

        rvNotificaciones.visibility =
            if (hayNotificaciones) View.VISIBLE else View.GONE

        contenedorVacio.visibility =
            if (hayNotificaciones) View.GONE else View.VISIBLE

        val hayPendientes = lista.any { !it.leida }

        tvMarcarTodasLeidas.visibility =
            if (hayPendientes) View.VISIBLE else View.GONE
    }

    private fun marcarComoLeida(
        notificacion: Notificacion
    ) {

        if (notificacion.leida) {
            return
        }

        val resultado =
            notificacionDAO.marcarNotificacionLeida(
                documentoUsuario,
                notificacion.id
            )

        if (resultado) {
            cargarNotificaciones()
        }
    }

    override fun onResume() {
        super.onResume()

        if (
            ::notificacionDAO.isInitialized &&
            ::documentoUsuario.isInitialized &&
            documentoUsuario.isNotBlank()
        ) {
            cargarNotificaciones()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // ==========================================
    // ADAPTADOR DE NOTIFICACIONES
    // ==========================================

    private class NotificacionAdapter(
        private val lista: MutableList<Notificacion>,
        private val alSeleccionar: (Notificacion) -> Unit
    ) : RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder>() {

        class NotificacionViewHolder(
            itemView: View
        ) : RecyclerView.ViewHolder(itemView) {

            val titulo: TextView =
                itemView.findViewById(
                    R.id.tvTituloItemNotificacion
                )

            val mensaje: TextView =
                itemView.findViewById(
                    R.id.tvMensajeItemNotificacion
                )

            val fecha: TextView =
                itemView.findViewById(
                    R.id.tvFechaItemNotificacion
                )

            val indicador: View =
                itemView.findViewById(
                    R.id.indicadorNoLeida
                )

            val contenedor: View =
                itemView.findViewById(
                    R.id.contenedorItemNotificacion
                )
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): NotificacionViewHolder {

            val vista = LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_notificacion,
                    parent,
                    false
                )

            return NotificacionViewHolder(vista)
        }

        override fun onBindViewHolder(
            holder: NotificacionViewHolder,
            position: Int
        ) {

            val notificacion = lista[position]

            holder.titulo.text = notificacion.titulo
            holder.mensaje.text = notificacion.mensaje
            holder.fecha.text = notificacion.fecha

            holder.indicador.visibility =
                if (notificacion.leida) View.GONE
                else View.VISIBLE

            holder.titulo.setTypeface(
                null,
                if (notificacion.leida)
                    android.graphics.Typeface.NORMAL
                else
                    android.graphics.Typeface.BOLD
            )

            holder.contenedor.setOnClickListener {
                alSeleccionar(notificacion)
            }
        }

        override fun getItemCount(): Int = lista.size

        fun actualizarLista(
            nuevaLista: List<Notificacion>
        ) {
            lista.clear()
            lista.addAll(nuevaLista)
            notifyDataSetChanged()
        }
    }
}
