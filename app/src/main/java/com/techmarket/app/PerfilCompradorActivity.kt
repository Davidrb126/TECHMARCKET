package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.UsuarioDAO
import com.techmarket.app.database.DatabaseContract

class PerfilCompradorActivity : AppCompatActivity() {

    private lateinit var usuarioDAO: UsuarioDAO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_comprador)

        usuarioDAO = UsuarioDAO(this)

        configurarEventos()
        cargarDatosPerfil()
    }

    // ==========================================
    // CONFIGURAR EVENTOS
    // ==========================================

    private fun configurarEventos() {

        // Volver
        findViewById<ImageButton>(R.id.btnVolverPerfil)
            .setOnClickListener {
                finish()
            }

        // Editar perfil
        findViewById<TextView>(R.id.tvEditarPerfil)
            .setOnClickListener {
                abrirMisDatos()
            }

        // Mis datos
        findViewById<View>(R.id.opcionMisDatos)
            .setOnClickListener {
                abrirMisDatos()
            }

        // Direcciones
        findViewById<View>(R.id.opcionDirecciones)
            .setOnClickListener {
                startActivity(
                    Intent(this, DireccionesActivity::class.java)
                )
            }

        // Mis compras
        findViewById<View>(R.id.opcionMisCompras)
            .setOnClickListener {
                startActivity(
                    Intent(this, HistorialComprasActivity::class.java)
                )
            }

        // Métodos de pago
        findViewById<View>(R.id.opcionMetodosPago)
            .setOnClickListener {
                startActivity(
                    Intent(this, MetodosPagoActivity::class.java)
                )
            }

        // Notificaciones
        findViewById<View>(R.id.opcionNotificaciones)
            .setOnClickListener {
                startActivity(
                    Intent(this, NotificacionesActivity::class.java)
                )
            }

        // Cambiar contraseña
        findViewById<View>(R.id.opcionCambiarContrasena)
            .setOnClickListener {
                startActivity(
                    Intent(this, CambiarContrasenaActivity::class.java)
                )
            }

        // Cerrar sesión
        findViewById<View>(R.id.btnCerrarSesionPerfil)
            .setOnClickListener {
                confirmarCerrarSesion()
            }

        // ==========================================
        // NAVEGACIÓN INFERIOR
        // ==========================================

        // Inicio
        findViewById<View>(R.id.navInicioPerfil)
            .setOnClickListener {

                val intent = Intent(
                    this,
                    InicioCompradorActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                startActivity(intent)
                finish()
            }

        // Productos
        findViewById<View>(R.id.navProductosPerfil)
            .setOnClickListener {
                mostrarMensaje(
                    "El catálogo se conectará próximamente."
                )
            }

        // Favoritos
        findViewById<View>(R.id.navFavoritosPerfil)
            .setOnClickListener {
                mostrarMensaje(
                    "Favoritos estará disponible próximamente."
                )
            }
    }

    // ==========================================
    // ABRIR MIS DATOS
    // ==========================================

    private fun abrirMisDatos() {
        startActivity(
            Intent(this, MisDatosActivity::class.java)
        )
    }

    // ==========================================
    // CARGAR Y VALIDAR INFORMACIÓN DEL PERFIL
    // ==========================================

    private fun cargarDatosPerfil() {

        // Comprobar que exista una sesión activa.
        if (!SessionManager.haySesionActiva(this)) {
            redirigirAlLogin()
            return
        }

        val documento = SessionManager.obtenerDocumento(this)

        if (documento.isNullOrBlank()) {
            cerrarSesionPorInconsistencia()
            return
        }

        try {

            val cursor = usuarioDAO.consultarUsuario(documento)

            cursor.use {

                // Si el documento no existe en SQLite,
                // la sesión guardada ya no es válida.
                if (!it.moveToFirst()) {
                    cerrarSesionPorInconsistencia()
                    return
                }

                val nombres = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.NOMBRES
                    )
                ) ?: ""

                val apellidos = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.APELLIDOS
                    )
                ) ?: ""

                val correo = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.CORREO
                    )
                ) ?: ""

                // Mostrar información directamente desde SQLite.
                findViewById<TextView>(R.id.tvNombrePerfil).text =
                    "$nombres $apellidos".trim()

                findViewById<TextView>(R.id.tvCorreoPerfil).text =
                    correo
            }

        } catch (e: Exception) {

            mostrarMensaje(
                "No fue posible validar la información de tu cuenta."
            )
        }
    }

    // ==========================================
    // SESIÓN NO VÁLIDA
    // ==========================================

    private fun cerrarSesionPorInconsistencia() {

        SessionManager.cerrarSesion(this)

        CarritoManager.vaciar()
        DatosEntregaManager.limpiar()
        MetodoPagoManager.limpiar()

        mostrarMensaje(
            "La sesión no es válida. Inicia sesión nuevamente."
        )

        redirigirAlLogin()
    }

    // ==========================================
    // REDIRIGIR AL LOGIN
    // ==========================================

    private fun redirigirAlLogin() {

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {

            putExtra(
                MainActivity.EXTRA_ABRIR_LOGIN,
                true
            )

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

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
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==========================================
    // CERRAR SESIÓN
    // ==========================================

    private fun cerrarSesion() {

        // Eliminar sesión guardada.
        SessionManager.cerrarSesion(this)

        // Limpiar información temporal del comprador.
        CarritoManager.vaciar()
        DatosEntregaManager.limpiar()
        MetodoPagoManager.limpiar()

        // Regresar al login.
        redirigirAlLogin()
    }

    // ==========================================
    // MOSTRAR MENSAJES
    // ==========================================

    private fun mostrarMensaje(mensaje: String) {

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }
}
