package com.techmarket.app

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.DatabaseContract
import com.techmarket.app.database.PedidoDAO
import java.text.NumberFormat
import java.util.Locale

class InicioAdministradorActivity : AppCompatActivity() {

    // ==========================================
    // COMPONENTES DEL DASHBOARD
    // ==========================================

    private lateinit var tvBienvenida: TextView

    private lateinit var tvTotalUsuarios: TextView
    private lateinit var tvTotalVendedores: TextView
    private lateinit var tvTotalProductos: TextView
    private lateinit var tvTotalCategorias: TextView
    private lateinit var tvTotalCompras: TextView
    private lateinit var tvValorTotalCompras: TextView

    private lateinit var tvVerActividades: TextView

    private lateinit var btnVolver: ImageButton
    private lateinit var btnCerrarSesion: TextView

    // ==========================================
    // NAVEGACIÓN INFERIOR
    // ==========================================

    private lateinit var navInicio: LinearLayout
    private lateinit var navUsuarios: LinearLayout
    private lateinit var navTiendas: LinearLayout
    private lateinit var navCategorias: LinearLayout

    private lateinit var imgInicio: ImageView
    private lateinit var imgUsuarios: ImageView
    private lateinit var imgTiendas: ImageView
    private lateinit var imgCategorias: ImageView

    private lateinit var tvInicio: TextView
    private lateinit var tvUsuarios: TextView
    private lateinit var tvTiendas: TextView
    private lateinit var tvCategorias: TextView

    private val colorSeleccionado = Color.rgb(7, 85, 255)
    private val colorNoSeleccionado = Color.rgb(119, 119, 119)

    // ==========================================
    // CICLO DE VIDA
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_inicio_administrador)

        val rol = SessionManager.obtenerRol(this).orEmpty()

        // Validar sesión y rol administrativo
        if (
            !SessionManager.haySesionActiva(this) ||
            !rol.equals("ADMINISTRADOR", ignoreCase = true)
        ) {
            regresarAlLogin()
            return
        }

        inicializarComponentes()
        configurarEventos()
        seleccionarOpcion("inicio")
    }

    override fun onResume() {
        super.onResume()

        if (::tvTotalUsuarios.isInitialized) {
            cargarInformacion()
        }
    }

    // ==========================================
    // INICIALIZAR COMPONENTES
    // ==========================================

    private fun inicializarComponentes() {

        tvBienvenida =
            findViewById(R.id.tvBienvenidaAdministrador)

        tvTotalUsuarios =
            findViewById(R.id.tvTotalUsuariosAdmin)

        tvTotalVendedores =
            findViewById(R.id.tvTotalVendedoresAdmin)

        tvTotalProductos =
            findViewById(R.id.tvTotalProductosAdmin)

        tvTotalCategorias =
            findViewById(R.id.tvTotalCategoriasAdmin)

        tvTotalCompras =
            findViewById(R.id.tvTotalComprasAdmin)

        tvValorTotalCompras =
            findViewById(R.id.tvValorTotalComprasAdmin)

        tvVerActividades =
            findViewById(R.id.tvVerActividadesAdmin)

        btnVolver =
            findViewById(R.id.btnVolverAdministrador)

        btnCerrarSesion =
            findViewById(R.id.btnCerrarSesionAdmin)

        // Navegación
        navInicio =
            findViewById(R.id.navInicioAdmin)

        navUsuarios =
            findViewById(R.id.navUsuariosAdmin)

        navTiendas =
            findViewById(R.id.navTiendasAdmin)

        navCategorias =
            findViewById(R.id.navCategoriasAdmin)

        // Iconos
        imgInicio =
            findViewById(R.id.imgNavInicioAdmin)

        imgUsuarios =
            findViewById(R.id.imgNavUsuariosAdmin)

        imgTiendas =
            findViewById(R.id.imgNavTiendasAdmin)

        imgCategorias =
            findViewById(R.id.imgNavCategoriasAdmin)

        // Textos de navegación
        tvInicio =
            findViewById(R.id.tvNavInicioAdmin)

        tvUsuarios =
            findViewById(R.id.tvNavUsuariosAdmin)

        tvTiendas =
            findViewById(R.id.tvNavTiendasAdmin)

        tvCategorias =
            findViewById(R.id.tvNavCategoriasAdmin)

        // Mensaje de bienvenida
        val nombre = SessionManager.obtenerNombre(this).orEmpty()

        tvBienvenida.text = if (nombre.isNotBlank()) {
            "Resumen general de la aplicación\nBienvenido, $nombre"
        } else {
            "Resumen general de la aplicación"
        }

        cargarInformacion()
    }

    // ==========================================
    // CONFIGURAR EVENTOS
    // ==========================================

    private fun configurarEventos() {

        // VOLVER A TECHMARKET
        btnVolver.setOnClickListener {
            regresarSinCerrarSesion()
        }

        // CERRAR SESIÓN
        btnCerrarSesion.setOnClickListener {
            confirmarCerrarSesion()
        }

        // INICIO
        navInicio.setOnClickListener {
            seleccionarOpcion("inicio")
            cargarInformacion()
        }

        // GESTIÓN DE USUARIOS
        navUsuarios.setOnClickListener {

            seleccionarOpcion("usuarios")

            startActivity(
                Intent(
                    this,
                    GestionUsuariosActivity::class.java
                )
            )
        }

        // GESTIÓN DE TIENDAS
        navTiendas.setOnClickListener {

            seleccionarOpcion("tiendas")

            startActivity(
                Intent(
                    this,
                    GestionTiendasActivity::class.java
                )
            )
        }

        // GESTIÓN DE CATEGORÍAS
        navCategorias.setOnClickListener {

            seleccionarOpcion("categorias")

            startActivity(
                Intent(
                    this,
                    GestionCategoriasActivity::class.java
                )
            )
        }

        // ACTIVIDAD RECIENTE
        tvVerActividades.setOnClickListener {

            Toast.makeText(
                this,
                "El historial de actividades estará disponible próximamente.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ==========================================
    // SELECCIONAR OPCIÓN DE NAVEGACIÓN
    // ==========================================

    private fun seleccionarOpcion(opcion: String) {

        val elementos = listOf(
            Triple("inicio", imgInicio, tvInicio),
            Triple("usuarios", imgUsuarios, tvUsuarios),
            Triple("tiendas", imgTiendas, tvTiendas),
            Triple("categorias", imgCategorias, tvCategorias)
        )

        elementos.forEach { (nombre, imagen, texto) ->

            val seleccionado = nombre == opcion

            val color = if (seleccionado) {
                colorSeleccionado
            } else {
                colorNoSeleccionado
            }

            imagen.setColorFilter(color)
            texto.setTextColor(color)

            texto.setTypeface(
                null,
                if (seleccionado) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                }
            )
        }
    }

    // ==========================================
    // CARGAR INFORMACIÓN DEL DASHBOARD
    // ==========================================

    private fun cargarInformacion() {

        val db = DatabaseHelper(this).readableDatabase

        try {

            // TOTAL DE USUARIOS
            tvTotalUsuarios.text = contarRegistros(
                db,
                """
                SELECT COUNT(*)
                FROM usuarios
                """.trimIndent()
            )

            // TOTAL DE VENDEDORES ACTIVOS
            tvTotalVendedores.text = contarRegistros(
                db,
                """
                SELECT COUNT(*)
                FROM usuarios
                WHERE UPPER(rol) = 'VENDEDOR'
                AND estado = 1
                """.trimIndent()
            )

            // TOTAL DE PRODUCTOS
            tvTotalProductos.text = contarRegistros(
                db,
                """
                SELECT COUNT(*)
                FROM productos_tienda
                """.trimIndent()
            )

            // TOTAL DE CATEGORÍAS ACTIVAS
            tvTotalCategorias.text = contarRegistros(
                db,
                """
                SELECT COUNT(*)
                FROM ${DatabaseContract.Categorias.TABLA}
                WHERE ${DatabaseContract.Categorias.ESTADO} = 1
                """.trimIndent()
            )

            // TOTAL DE COMPRAS
            tvTotalCompras.text = contarRegistros(
                db,
                """
                SELECT COUNT(*)
                FROM ${DatabaseContract.Pedidos.TABLA}
                """.trimIndent()
            )

            // VALOR TOTAL DE COMPRAS
            // Se excluyen únicamente los pedidos
            // cancelados por todas sus tiendas.

            val valorTotal = obtenerValorTotalCompras(db)

            val formatoMoneda = NumberFormat.getCurrencyInstance(
                Locale("es", "CO")
            ).apply {
                maximumFractionDigits = 0
                minimumFractionDigits = 0
            }

            tvValorTotalCompras.text =
                formatoMoneda.format(valorTotal)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No fue posible cargar el resumen administrativo.",
                Toast.LENGTH_SHORT
            ).show()

        } finally {
            db.close()
        }
    }

    // ==========================================
    // CONTAR REGISTROS
    // ==========================================

    private fun contarRegistros(
        db: SQLiteDatabase,
        consulta: String
    ): String {

        return try {

            db.rawQuery(consulta, null).use { cursor ->

                if (cursor.moveToFirst()) {
                    cursor.getInt(0).toString()
                } else {
                    "0"
                }
            }

        } catch (e: Exception) {
            "0"
        }
    }

    // ==========================================
    // OBTENER VALOR TOTAL DE COMPRAS
    // ==========================================

    private fun obtenerValorTotalCompras(
        db: SQLiteDatabase
    ): Long {

        val sql = """
            SELECT COALESCE(
                SUM(p.${DatabaseContract.Pedidos.TOTAL}),
                0
            )
            FROM ${DatabaseContract.Pedidos.TABLA} p
            WHERE
                NOT EXISTS (
                    SELECT 1
                    FROM pedido_tiendas pt
                    WHERE pt.numero_pedido = p.numero_pedido
                )
                OR EXISTS (
                    SELECT 1
                    FROM pedido_tiendas pt
                    WHERE pt.numero_pedido = p.numero_pedido
                    AND COALESCE(pt.estado, '') != ?
                )
        """.trimIndent()

        return db.rawQuery(
            sql,
            arrayOf(PedidoDAO.ESTADO_CANCELADO)
        ).use { cursor ->

            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else {
                0L
            }
        }
    }

    // ==========================================
    // REGRESAR A TECHMARKET SIN CERRAR SESIÓN
    // ==========================================

    private fun regresarSinCerrarSesion() {

        val intent = Intent(
            this,
            InicioCompradorActivity::class.java
        )

        startActivity(intent)
        finish()
    }

    // ==========================================
    // CONFIRMAR CIERRE DE SESIÓN
    // ==========================================

    private fun confirmarCerrarSesion() {

        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage(
                "¿Estás seguro de que deseas cerrar tu sesión?"
            )
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Cerrar sesión") { _, _ ->

                SessionManager.cerrarSesion(this)

                regresarAlLogin()
            }
            .show()
    }

    // ==========================================
    // REGRESAR AL LOGIN
    // ==========================================

    private fun regresarAlLogin() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        intent.putExtra(
            MainActivity.EXTRA_ABRIR_LOGIN,
            true
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}