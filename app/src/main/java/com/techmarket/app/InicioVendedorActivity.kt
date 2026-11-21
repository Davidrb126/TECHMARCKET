package com.techmarket.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.DatabaseContract
import com.techmarket.app.database.PedidoDAO
import com.techmarket.app.database.ProductoDAO
import com.techmarket.app.database.TiendaDAO
import com.techmarket.app.database.UsuarioDAO

class InicioVendedorActivity : AppCompatActivity() {

    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var tiendaDAO: TiendaDAO
    private lateinit var productoDAO: ProductoDAO
    private lateinit var pedidoDAO: PedidoDAO

    private lateinit var tvNombreVendedor: TextView
    private lateinit var tvNombreTienda: TextView
    private lateinit var tvTotalProductos: TextView
    private lateinit var tvTotalPedidos: TextView
    private lateinit var imgPerfil: ImageView

    private var idTiendaVendedor: Long = -1L
    private var idUsuarioVendedor: Long = -1L
    private var tiendasVendedor: List<TiendaVendedor> = emptyList()

    private data class TiendaVendedor(
        val id: Long,
        val nombre: String,
        val logo: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_vendedor)

        usuarioDAO = UsuarioDAO(this)
        tiendaDAO = TiendaDAO(this)
        productoDAO = ProductoDAO(this)
        pedidoDAO = PedidoDAO(this)

        inicializarComponentes()
        configurarEventos()

        // Restaurar la tienda seleccionada si Android recreó la actividad.
        idTiendaVendedor = savedInstanceState?.getLong(
            ESTADO_ID_TIENDA_SELECCIONADA,
            -1L
        ) ?: -1L

        cargarDatosVendedor()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(
            ESTADO_ID_TIENDA_SELECCIONADA,
            idTiendaVendedor
        )
        super.onSaveInstanceState(outState)
    }

    private fun inicializarComponentes() {

        tvNombreVendedor =
            findViewById(R.id.tvNombreVendedorInicio)

        tvNombreTienda =
            findViewById(R.id.tvNombreTiendaInicio)

        tvTotalProductos =
            findViewById(R.id.tvTotalProductosVendedor)

        tvTotalPedidos =
            findViewById(R.id.tvTotalPedidosVendedor)

        imgPerfil =
            findViewById(R.id.imgPerfilVendedor)
    }

    private fun cargarDatosVendedor() {

        val documento = SessionManager.obtenerDocumento(this)

        if (documento.isNullOrBlank()) {
            mostrarMensaje("No se encontró una sesión activa.")
            finish()
            return
        }

        try {

            val cursorUsuario =
                usuarioDAO.consultarUsuario(documento)

            cursorUsuario.use { cursor ->

                if (!cursor.moveToFirst()) {
                    mostrarMensaje(
                        "No se encontraron los datos del vendedor."
                    )
                    finish()
                    return
                }

                idUsuarioVendedor = cursor.getLong(
                    cursor.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.ID
                    )
                )

                val nombres = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.NOMBRES
                    )
                ).orEmpty()

                val apellidos = cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.APELLIDOS
                    )
                ).orEmpty()

                val nombreCompleto = "$nombres $apellidos".trim()

                tvNombreVendedor.text = nombreCompleto

                val rol = SessionManager.obtenerRol(this).orEmpty()

                SessionManager.guardarSesion(
                    this,
                    idUsuarioVendedor,
                    documento,
                    nombreCompleto,
                    rol
                )

                cargarTiendas(idUsuarioVendedor)
            }

        } catch (e: Exception) {

            mostrarMensaje(
                "No fue posible cargar los datos del vendedor."
            )
        }
    }

    private fun cargarTiendas(idUsuario: Long) {

        try {

            val cursorTiendas =
                tiendaDAO.consultarTiendaPorUsuario(idUsuario)

            val lista = mutableListOf<TiendaVendedor>()

            cursorTiendas.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            DatabaseContract.Tiendas.ID
                        )
                    )

                    val nombre = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            DatabaseContract.Tiendas.NOMBRE
                        )
                    ).orEmpty()

                    val logo = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            DatabaseContract.Tiendas.LOGO
                        )
                    )

                    lista.add(
                        TiendaVendedor(
                            id = id,
                            nombre = nombre,
                            logo = logo
                        )
                    )
                }
            }

            tiendasVendedor = lista

            if (tiendasVendedor.isEmpty()) {
                idTiendaVendedor = -1L
                tvNombreTienda.text = "Tienda no encontrada"
                tvTotalProductos.text = "0"
                tvTotalPedidos.text = "0"
                mostrarLogoPredeterminado()
                return
            }

            // Si la tienda seleccionada sigue perteneciendo al vendedor,
            // se conserva al regresar de otros módulos.
            val tiendaActual = tiendasVendedor.firstOrNull {
                it.id == idTiendaVendedor
            }

            when {
                tiendaActual != null -> seleccionarTienda(tiendaActual)
                tiendasVendedor.size == 1 -> seleccionarTienda(tiendasVendedor.first())
                else -> mostrarSelectorTiendas()
            }

        } catch (e: Exception) {

            idTiendaVendedor = -1L
            tvNombreTienda.text = "No fue posible cargar las tiendas"
            tvTotalProductos.text = "0"
            tvTotalPedidos.text = "0"
            mostrarLogoPredeterminado()
        }
    }

    private fun mostrarSelectorTiendas() {

        if (tiendasVendedor.isEmpty()) {
            mostrarMensaje("No tienes tiendas asociadas.")
            return
        }

        val nombresTiendas = tiendasVendedor.map { it.nombre }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Seleccionar tienda")
            .setItems(nombresTiendas) { _, posicion ->
                seleccionarTienda(tiendasVendedor[posicion])
            }
            .setCancelable(true)
            .show()
    }

    private fun seleccionarTienda(tienda: TiendaVendedor) {

        idTiendaVendedor = tienda.id
        tvNombreTienda.text = tienda.nombre

        cargarLogoTienda(tienda.logo)
        actualizarTotalProductos()
        actualizarTotalPedidos()
    }

    private fun actualizarTotalProductos() {

        if (idTiendaVendedor <= 0) {
            tvTotalProductos.text = "0"
            return
        }

        try {

            val total = productoDAO.contarProductosTienda(
                idTiendaVendedor
            )

            tvTotalProductos.text = total.toString()

        } catch (e: Exception) {

            tvTotalProductos.text = "0"
        }
    }

    private fun actualizarTotalPedidos() {

        if (idTiendaVendedor <= 0) {
            tvTotalPedidos.text = "0"
            return
        }

        try {

            val total = pedidoDAO.contarPedidosPorTienda(
                idTiendaVendedor
            )

            tvTotalPedidos.text = total.toString()

        } catch (e: Exception) {

            tvTotalPedidos.text = "0"
        }
    }

    private fun cargarLogoTienda(rutaLogo: String?) {

        if (rutaLogo.isNullOrBlank()) {
            mostrarLogoPredeterminado()
            return
        }

        try {

            val uriLogo = Uri.parse(rutaLogo)

            val inputStream =
                contentResolver.openInputStream(uriLogo)

            if (inputStream == null) {
                mostrarLogoPredeterminado()
                return
            }

            inputStream.use { input ->

                val bitmap =
                    android.graphics.BitmapFactory.decodeStream(input)

                if (bitmap != null) {

                    imgPerfil.setImageBitmap(bitmap)

                    imgPerfil.scaleType =
                        ImageView.ScaleType.CENTER_CROP

                } else {

                    mostrarLogoPredeterminado()
                }
            }

        } catch (e: Exception) {

            mostrarLogoPredeterminado()
        }
    }

    private fun mostrarLogoPredeterminado() {

        imgPerfil.setImageResource(
            android.R.drawable.ic_menu_myplaces
        )

        imgPerfil.scaleType =
            ImageView.ScaleType.CENTER_INSIDE
    }

    private fun configurarEventos() {

        // VOLVER AL INICIO DEL COMPRADOR
        findViewById<ImageButton>(R.id.btnVolverComprador)
            .setOnClickListener {

                val intent = Intent(
                    this,
                    InicioCompradorActivity::class.java
                )

                startActivity(intent)
                finish()
            }

        // Permite cambiar de tienda desde el nombre mostrado en el panel.
        tvNombreTienda.setOnClickListener {
            if (tiendasVendedor.size > 1) {
                mostrarSelectorTiendas()
            }
        }

        // MIS PRODUCTOS
        findViewById<View>(R.id.btnMisProductosVendedor)
            .setOnClickListener {

                if (idTiendaVendedor <= 0) {
                    mostrarMensaje(
                        "Selecciona una tienda para continuar."
                    )
                    return@setOnClickListener
                }

                val intent = Intent(
                    this,
                    MisProductosActivity::class.java
                ).apply {
                    putExtra(
                        MisProductosActivity.EXTRA_ID_TIENDA,
                        idTiendaVendedor
                    )
                }

                startActivity(intent)
            }

        // PEDIDOS RECIBIDOS
        findViewById<View>(R.id.btnPedidosVendedor)
            .setOnClickListener {

                if (idTiendaVendedor <= 0) {
                    mostrarMensaje(
                        "Selecciona una tienda para continuar."
                    )
                    return@setOnClickListener
                }

                val intent = Intent(
                    this,
                    PedidosVendedorActivity::class.java
                )

                intent.putExtra(
                    "ID_TIENDA",
                    idTiendaVendedor
                )

                startActivity(intent)
            }

        // MI TIENDA
        findViewById<View>(R.id.btnMiTiendaVendedor)
            .setOnClickListener {

                if (idTiendaVendedor <= 0) {
                    mostrarMensaje(
                        "Selecciona una tienda para continuar."
                    )
                    return@setOnClickListener
                }

                val intent = Intent(
                    this,
                    MiTiendaActivity::class.java
                ).apply {
                    putExtra("ID_TIENDA", idTiendaVendedor)
                }

                startActivity(intent)
            }

        // MI PERFIL
        findViewById<View>(R.id.btnPerfilVendedor)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        PerfilVendedorActivity::class.java
                    )
                )
            }

        // CERRAR SESIÓN
        findViewById<View>(R.id.btnCerrarSesionVendedor)
            .setOnClickListener {
                mostrarDialogoCerrarSesion()
            }

        // LOGO DE LA TIENDA
        imgPerfil.setOnClickListener {
            mostrarMensaje("Perfil de la tienda")
        }
    }

    private fun mostrarDialogoCerrarSesion() {

        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage(
                "¿Estás seguro de que deseas cerrar tu sesión?"
            )
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {

        SessionManager.cerrarSesion(this)

        CarritoManager.vaciar()
        DatosEntregaManager.limpiar()
        MetodoPagoManager.limpiar()

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

            putExtra(
                MainActivity.EXTRA_ABRIR_LOGIN,
                true
            )
        }

        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()

        if (::usuarioDAO.isInitialized) {
            cargarDatosVendedor()
        }
    }

    private fun mostrarMensaje(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val ESTADO_ID_TIENDA_SELECCIONADA =
            "estado_id_tienda_seleccionada"
    }
}
