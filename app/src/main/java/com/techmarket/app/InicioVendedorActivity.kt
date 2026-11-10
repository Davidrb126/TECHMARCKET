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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_vendedor)

        usuarioDAO = UsuarioDAO(this)
        tiendaDAO = TiendaDAO(this)
        productoDAO = ProductoDAO(this)
        pedidoDAO = PedidoDAO(this)

        inicializarComponentes()
        configurarEventos()
        cargarDatosVendedor()
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

                val idUsuario = cursor.getLong(
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
                    idUsuario,
                    documento,
                    nombreCompleto,
                    rol
                )

                cargarTienda(idUsuario)
            }

        } catch (e: Exception) {

            mostrarMensaje(
                "No fue posible cargar los datos del vendedor."
            )
        }
    }

    private fun cargarTienda(idUsuario: Long) {

        try {

            val cursorTienda =
                tiendaDAO.consultarTiendaPorUsuario(idUsuario)

            cursorTienda.use { cursor ->

                if (cursor.moveToFirst()) {

                    idTiendaVendedor = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                            DatabaseContract.Tiendas.ID
                        )
                    )

                    val nombreTienda = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            DatabaseContract.Tiendas.NOMBRE
                        )
                    ).orEmpty()

                    val logoTienda = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            DatabaseContract.Tiendas.LOGO
                        )
                    )

                    tvNombreTienda.text = nombreTienda

                    cargarLogoTienda(logoTienda)

                    actualizarTotalProductos()
                    actualizarTotalPedidos()

                } else {

                    idTiendaVendedor = -1L

                    tvNombreTienda.text = "Tienda no encontrada"
                    tvTotalProductos.text = "0"
                    tvTotalPedidos.text = "0"

                    mostrarLogoPredeterminado()
                }
            }

        } catch (e: Exception) {

            tvNombreTienda.text =
                "No fue posible cargar la tienda"

            tvTotalProductos.text = "0"
            tvTotalPedidos.text = "0"

            mostrarLogoPredeterminado()
        }
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

        // MIS PRODUCTOS
        findViewById<View>(R.id.btnMisProductosVendedor)
            .setOnClickListener {

                if (idTiendaVendedor <= 0) {
                    mostrarMensaje(
                        "No se encontró una tienda asociada al vendedor."
                    )
                    return@setOnClickListener
                }

                startActivity(
                    Intent(
                        this,
                        MisProductosActivity::class.java
                    )
                )
            }

        // PEDIDOS RECIBIDOS
        findViewById<View>(R.id.btnPedidosVendedor)
            .setOnClickListener {

                if (idTiendaVendedor <= 0) {
                    mostrarMensaje(
                        "No se encontró una tienda asociada al vendedor."
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
                        "No se encontró una tienda asociada al vendedor."
                    )
                    return@setOnClickListener
                }

                startActivity(
                    Intent(
                        this,
                        MiTiendaActivity::class.java
                    )
                )
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
}
