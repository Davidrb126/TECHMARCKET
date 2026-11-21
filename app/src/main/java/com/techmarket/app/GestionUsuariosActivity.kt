package com.techmarket.app

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog

class GestionUsuariosActivity : androidx.appcompat.app.AppCompatActivity() {

    private lateinit var contenedorUsuarios: LinearLayout
    private lateinit var tvTotalUsuarios: TextView
    private lateinit var etBuscarUsuario: EditText
    private lateinit var btnNuevoUsuario: Button

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

    private val usuarios = mutableListOf<UsuarioAdmin>()

    data class UsuarioAdmin(
        val documento: String,
        val nombres: String,
        val apellidos: String,
        val correo: String,
        val rol: String,
        val estado: String
    )

    private val registrarUsuarioLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { resultado ->

            if (resultado.resultCode == RESULT_OK) {
                cargarUsuarios()

                Toast.makeText(
                    this,
                    "Listado de usuarios actualizado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_usuarios)

        inicializarComponentes()
        configurarEventos()
        seleccionarOpcion("usuarios")
        cargarUsuarios()
    }

    // =====================================================
    // INICIALIZAR COMPONENTES
    // =====================================================

    private fun inicializarComponentes() {

        contenedorUsuarios = findViewById(R.id.contenedorUsuarios)
        tvTotalUsuarios = findViewById(R.id.tvTotalUsuarios)
        etBuscarUsuario = findViewById(R.id.etBuscarUsuario)

        btnNuevoUsuario = findViewById(R.id.btnNuevoUsuarioAdmin)

        navInicio = findViewById(R.id.navInicioAdmin)
        navUsuarios = findViewById(R.id.navUsuariosAdmin)
        navTiendas = findViewById(R.id.navTiendasAdmin)
        navCategorias = findViewById(R.id.navCategoriasAdmin)

        imgInicio = findViewById(R.id.imgNavInicioAdmin)
        imgUsuarios = findViewById(R.id.imgNavUsuariosAdmin)
        imgTiendas = findViewById(R.id.imgNavTiendasAdmin)
        imgCategorias = findViewById(R.id.imgNavCategoriasAdmin)

        tvInicio = findViewById(R.id.tvNavInicioAdmin)
        tvUsuarios = findViewById(R.id.tvNavUsuariosAdmin)
        tvTiendas = findViewById(R.id.tvNavTiendasAdmin)
        tvCategorias = findViewById(R.id.tvNavCategoriasAdmin)
    }

    // =====================================================
    // CONFIGURAR EVENTOS
    // =====================================================

    private fun configurarEventos() {

        findViewById<View>(R.id.btnVolverUsuarios).setOnClickListener {
            finish()
        }

        btnNuevoUsuario.setOnClickListener {

            val intent = Intent(
                this,
                NuevoUsuarioActivity::class.java
            )

            registrarUsuarioLauncher.launch(intent)
        }

        etBuscarUsuario.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                mostrarUsuarios(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        navInicio.setOnClickListener {

            val intent = Intent(
                this,
                InicioAdministradorActivity::class.java
            )

            startActivity(intent)
            finish()
        }

        navUsuarios.setOnClickListener {
            seleccionarOpcion("usuarios")
        }

        navTiendas.setOnClickListener {

            val intent = Intent(
                this,
                GestionTiendasActivity::class.java
            )

            startActivity(intent)
        }

        navCategorias.setOnClickListener {

            val intent = Intent(
                this,
                GestionCategoriasActivity::class.java
            )

            startActivity(intent)
        }
    }

    // =====================================================
    // SELECCIONAR OPCIÓN DE NAVEGACIÓN
    // =====================================================

    private fun seleccionarOpcion(opcion: String) {

        val opciones = listOf(
            Triple("inicio", imgInicio, tvInicio),
            Triple("usuarios", imgUsuarios, tvUsuarios),
            Triple("tiendas", imgTiendas, tvTiendas),
            Triple("categorias", imgCategorias, tvCategorias)
        )

        opciones.forEach { (nombre, imagen, texto) ->

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
                if (seleccionado) Typeface.BOLD else Typeface.NORMAL
            )
        }
    }

    // =====================================================
    // CARGAR USUARIOS DESDE SQLITE
    // =====================================================

    private fun cargarUsuarios() {

        usuarios.clear()

        val dbHelper = DatabaseHelper(this)

        try {

            val db = dbHelper.readableDatabase

            val cursor = db.query(
                "usuarios",
                arrayOf(
                    "documento",
                    "nombres",
                    "apellidos",
                    "correo",
                    "rol",
                    "estado"
                ),
                null,
                null,
                null,
                null,
                "nombres ASC"
            )

            cursor.use {

                while (it.moveToNext()) {

                    usuarios.add(
                        UsuarioAdmin(
                            documento = it.getString(
                                it.getColumnIndexOrThrow("documento")
                            ) ?: "",

                            nombres = it.getString(
                                it.getColumnIndexOrThrow("nombres")
                            ) ?: "",

                            apellidos = it.getString(
                                it.getColumnIndexOrThrow("apellidos")
                            ) ?: "",

                            correo = it.getString(
                                it.getColumnIndexOrThrow("correo")
                            ) ?: "",

                            rol = it.getString(
                                it.getColumnIndexOrThrow("rol")
                            ) ?: "",

                            estado = it.getString(
                                it.getColumnIndexOrThrow("estado")
                            ) ?: ""
                        )
                    )
                }
            }

            tvTotalUsuarios.text =
                "Usuarios registrados: ${usuarios.size}"

            mostrarUsuarios(etBuscarUsuario.text.toString())

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "No fue posible cargar los usuarios: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

        } finally {
            dbHelper.close()
        }
    }

    // =====================================================
    // MOSTRAR Y FILTRAR USUARIOS
    // =====================================================

    private fun mostrarUsuarios(filtro: String) {

        contenedorUsuarios.removeAllViews()

        val texto = filtro.trim().lowercase()

        val filtrados = usuarios.filter { usuario ->

            usuario.nombres.lowercase().contains(texto) ||
                    usuario.apellidos.lowercase().contains(texto) ||
                    usuario.documento.lowercase().contains(texto) ||
                    usuario.correo.lowercase().contains(texto)
        }

        if (filtrados.isEmpty()) {

            val mensaje = TextView(this).apply {
                text = "No se encontraron usuarios."
                textSize = 16f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
                setPadding(16, 40, 16, 40)
            }

            contenedorUsuarios.addView(mensaje)
            return
        }

        filtrados.forEach { usuario ->
            contenedorUsuarios.addView(crearTarjetaUsuario(usuario))
        }
    }

    // =====================================================
    // CREAR TARJETA DE USUARIO
    // =====================================================

    private fun crearTarjetaUsuario(usuario: UsuarioAdmin): View {

        // Evento compartido para abrir la edición
        val abrirEdicion = View.OnClickListener {
            mostrarDialogoEdicion(usuario)
        }

        val tarjeta = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL
            setPadding(18, 16, 18, 16)

            background = fondoRedondeado(
                Color.WHITE,
                16f
            )

            elevation = 3f

            isClickable = true
            isFocusable = true

            setOnClickListener(abrirEdicion)
        }

        val nombre = TextView(this).apply {

            text = "${usuario.nombres} ${usuario.apellidos}"
            textSize = 18f

            setTextColor(Color.rgb(35, 45, 60))

            setTypeface(null, Typeface.BOLD)
        }

        val documento = crearTexto(
            "Documento: ${usuario.documento}"
        )

        val correo = crearTexto(
            "Correo: ${usuario.correo}"
        )

        val rol = crearTexto(
            "Rol: ${usuario.rol}"
        )

        val estadoActivo = esUsuarioActivo(usuario.estado)

        val estado = TextView(this).apply {

            text = if (estadoActivo) "Activo" else "Inactivo"
            textSize = 14f

            setTypeface(null, Typeface.BOLD)

            setTextColor(
                if (estadoActivo) {
                    Color.rgb(22, 135, 75)
                } else {
                    Color.rgb(190, 55, 55)
                }
            )

            setPadding(0, 8, 0, 0)
        }

        val indicacion = TextView(this).apply {

            text = "Toca para editar este usuario"
            textSize = 12f
            setTextColor(colorSeleccionado)
            setPadding(0, 10, 0, 0)

            isClickable = true
            isFocusable = true

            setOnClickListener(abrirEdicion)
        }

        tarjeta.addView(nombre)
        tarjeta.addView(documento)
        tarjeta.addView(correo)
        tarjeta.addView(rol)
        tarjeta.addView(estado)
        tarjeta.addView(indicacion)

        tarjeta.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 12
        }

        return tarjeta
    }

    // =====================================================
    // FORMULARIO DE EDICIÓN DE USUARIO
    // =====================================================

    private fun mostrarDialogoEdicion(usuario: UsuarioAdmin) {

        val dialogo = Dialog(this)

        dialogo.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialogo.setContentView(
            R.layout.dialog_editar_usuario
        )

        dialogo.setCancelable(true)

        val documento = dialogo.findViewById<EditText>(
            R.id.etDocumentoEditar
        )

        val nombres = dialogo.findViewById<EditText>(
            R.id.etNombresEditar
        )

        val apellidos = dialogo.findViewById<EditText>(
            R.id.etApellidosEditar
        )

        val correo = dialogo.findViewById<EditText>(
            R.id.etCorreoEditar
        )

        val contrasena = dialogo.findViewById<EditText>(
            R.id.etContrasenaEditar
        )

        val spinnerRol = dialogo.findViewById<Spinner>(
            R.id.spRolEditar
        )

        val switchEstado = dialogo.findViewById<Switch>(
            R.id.swEstadoEditar
        )

        val btnCerrar = dialogo.findViewById<ImageView>(
            R.id.btnCerrarEditarUsuario
        )

        val btnGuardar = dialogo.findViewById<Button>(
            R.id.btnGuardarEdicionUsuario
        )

        val btnCancelar = dialogo.findViewById<Button>(
            R.id.btnCancelarEdicionUsuario
        )

        val btnEliminarDefinitivamente = dialogo.findViewById<Button>(
            R.id.btnEliminarDefinitivamenteUsuario
        )

        documento.setText(usuario.documento)
        nombres.setText(usuario.nombres)
        apellidos.setText(usuario.apellidos)
        correo.setText(usuario.correo)

        documento.isEnabled = false

        val roles = listOf(
            "CLIENTE",
            "VENDEDOR",
            "ADMINISTRADOR"
        )

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            roles
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerRol.adapter = adaptador

        val posicionRol = roles.indexOf(
            usuario.rol.uppercase()
        )

        spinnerRol.setSelection(
            if (posicionRol >= 0) posicionRol else 0
        )

        switchEstado.isChecked = esUsuarioActivo(
            usuario.estado
        )

        switchEstado.text = if (switchEstado.isChecked) {
            "Activo"
        } else {
            "Inactivo"
        }

        switchEstado.setOnCheckedChangeListener { _, activo ->

            switchEstado.text = if (activo) {
                "Activo"
            } else {
                "Inactivo"
            }
        }

        btnCerrar.setOnClickListener {
            dialogo.dismiss()
        }

        btnCancelar.setOnClickListener {
            dialogo.dismiss()
        }

        btnEliminarDefinitivamente.setOnClickListener {
            confirmarEliminacionDefinitiva(usuario, dialogo, btnEliminarDefinitivamente)
        }

        btnGuardar.setOnClickListener {

            val nombresTexto = nombres.text.toString().trim()
            val apellidosTexto = apellidos.text.toString().trim()
            val correoTexto = correo.text.toString().trim()
            val contrasenaTexto = contrasena.text.toString()

            if (nombresTexto.isEmpty()) {
                nombres.error = "Ingresa los nombres"
                nombres.requestFocus()
                return@setOnClickListener
            }

            if (apellidosTexto.isEmpty()) {
                apellidos.error = "Ingresa los apellidos"
                apellidos.requestFocus()
                return@setOnClickListener
            }

            if (
                correoTexto.isEmpty() ||
                !Patterns.EMAIL_ADDRESS.matcher(correoTexto).matches()
            ) {
                correo.error = "Ingresa un correo válido"
                correo.requestFocus()
                return@setOnClickListener
            }

            if (
                contrasenaTexto.isNotEmpty() &&
                contrasenaTexto.length < 8
            ) {
                contrasena.error =
                    "La contraseña debe tener mínimo 8 caracteres"

                contrasena.requestFocus()
                return@setOnClickListener
            }

            btnGuardar.isEnabled = false

            guardarCambiosUsuario(
                documento = usuario.documento,
                nombres = nombresTexto,
                apellidos = apellidosTexto,
                correo = correoTexto,
                rol = spinnerRol.selectedItem.toString(),
                activo = switchEstado.isChecked,
                nuevaContrasena = contrasenaTexto,
                dialogo = dialogo,
                btnGuardar = btnGuardar
            )
        }

        dialogo.show()

        dialogo.window?.apply {

            setBackgroundDrawableResource(
                android.R.color.transparent
            )

            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )

            setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
    }

    // =====================================================
    // GUARDAR CAMBIOS EN SQLITE Y FIRESTORE
    // =====================================================

    private fun guardarCambiosUsuario(
        documento: String,
        nombres: String,
        apellidos: String,
        correo: String,
        rol: String,
        activo: Boolean,
        nuevaContrasena: String,
        dialogo: Dialog,
        btnGuardar: Button
    ) {

        val dbHelper = DatabaseHelper(this)

        try {

            val valores = ContentValues().apply {

                put("nombres", nombres)
                put("apellidos", apellidos)
                put("correo", correo)
                put("rol", rol)

                put(
                    "estado",
                    if (activo) 1 else 0
                )

                if (nuevaContrasena.isNotEmpty()) {

                    put(
                        "password_hash",
                        PasswordUtils.generarHashContrasena(
                            nuevaContrasena
                        )
                    )
                }
            }

            val filasActualizadas = dbHelper.writableDatabase.update(
                "usuarios",
                valores,
                "documento = ?",
                arrayOf(documento)
            )

            if (filasActualizadas <= 0) {

                btnGuardar.isEnabled = true

                Toast.makeText(
                    this,
                    "No se encontraron cambios para guardar.",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            FirebaseManager.sincronizarUsuario(
                tipoDocumento = obtenerTipoDocumentoLocal(documento),
                documento = documento,
                nombres = nombres,
                apellidos = apellidos,
                correo = correo,
                rol = rol,
                estado = if (activo) 1 else 0
            ).addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Usuario actualizado y sincronizado correctamente.",
                    Toast.LENGTH_LONG
                ).show()

                dialogo.dismiss()
                cargarUsuarios()

            }.addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Cambios guardados en SQLite, pero no sincronizados con Firestore: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()

                dialogo.dismiss()
                cargarUsuarios()
            }

        } catch (e: Exception) {

            btnGuardar.isEnabled = true

            Toast.makeText(
                this,
                "No fue posible actualizar el usuario: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

        } finally {
            dbHelper.close()
        }
    }

    // =====================================================
    // OBTENER TIPO DE DOCUMENTO DESDE SQLITE
    // =====================================================

    private fun obtenerTipoDocumentoLocal(documento: String): String {

        val dbHelper = DatabaseHelper(this)

        return try {

            val cursor = dbHelper.readableDatabase.query(
                "usuarios",
                arrayOf("tipo_documento"),
                "documento = ?",
                arrayOf(documento),
                null,
                null,
                null
            )

            cursor.use {

                if (it.moveToFirst()) {
                    it.getString(
                        it.getColumnIndexOrThrow("tipo_documento")
                    ) ?: ""
                } else {
                    ""
                }
            }

        } catch (e: Exception) {
            ""
        } finally {
            dbHelper.close()
        }
    }

    // =====================================================
    // CONFIRMAR ELIMINACIÓN DEFINITIVA
    // =====================================================

    private fun confirmarEliminacionDefinitiva(
        usuario: UsuarioAdmin,
        dialogo: Dialog,
        boton: Button
    ) {

        val documentoSesion = SessionManager.obtenerDocumento(this)

        if (usuario.documento == documentoSesion) {
            Toast.makeText(
                this,
                "No puedes eliminar la cuenta que tiene la sesión activa.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (usuario.rol.equals("VENDEDOR", ignoreCase = true) &&
            vendedorTieneTienda(usuario.documento)
        ) {
            Toast.makeText(
                this,
                "No puedes eliminar este vendedor porque tiene una tienda asociada. Primero debes gestionar la tienda.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar definitivamente")
            .setMessage(
                "¿Deseas eliminar definitivamente a " +
                        "${usuario.nombres} ${usuario.apellidos}?\n\n" +
                        "Esta acción no se puede deshacer.\n\n" +
                        "Los pedidos históricos del comprador se conservarán, " +
                        "pero dejarán de estar asociados a su cuenta."
            )
            .setPositiveButton("Eliminar definitivamente") { _, _ ->
                eliminarUsuarioDefinitivamente(usuario, dialogo, boton)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    // =====================================================
    // VALIDAR SI EL VENDEDOR TIENE UNA TIENDA
    // =====================================================

    private fun vendedorTieneTienda(documento: String): Boolean {

        val dbHelper = DatabaseHelper(this)

        return try {
            val cursor = dbHelper.readableDatabase.rawQuery(
                """
                SELECT COUNT(*)
                FROM tiendas t
                INNER JOIN usuarios u ON t.id_usuario = u.id
                WHERE u.documento = ?
                """.trimIndent(),
                arrayOf(documento)
            )

            cursor.use {
                it.moveToFirst() && it.getInt(0) > 0
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No fue posible validar la tienda asociada: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            // Si no se puede comprobar, se bloquea la eliminación por seguridad.
            true

        } finally {
            dbHelper.close()
        }
    }


    // =====================================================
    // ELIMINAR DEFINITIVAMENTE EN FIRESTORE Y SQLITE
    // =====================================================

    private fun eliminarUsuarioDefinitivamente(
        usuario: UsuarioAdmin,
        dialogo: Dialog,
        boton: Button
    ) {

        // Revalidar justo antes de iniciar la operación.
        if (usuario.documento == SessionManager.obtenerDocumento(this)) {
            Toast.makeText(
                this,
                "No puedes eliminar la cuenta que tiene la sesión activa.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (usuario.rol.equals("VENDEDOR", ignoreCase = true) &&
            vendedorTieneTienda(usuario.documento)
        ) {
            Toast.makeText(
                this,
                "El vendedor tiene una tienda asociada y no puede eliminarse.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        boton.isEnabled = false
        boton.text = "ELIMINANDO..."

        FirebaseManager.eliminarUsuarioFirestore(usuario.documento)
            .addOnSuccessListener {

                val resultadoLocal = eliminarUsuarioDeSQLite(usuario)

                if (resultadoLocal) {
                    // Limpiar una posible asociación biométrica de esta cuenta.
                    val preferencias = getSharedPreferences(
                        "techmarket_biometria",
                        MODE_PRIVATE
                    )

                    if (preferencias.getString("documento_biometrico", null) == usuario.documento) {
                        preferencias.edit()
                            .remove("documento_biometrico")
                            .apply()
                    }

                    Toast.makeText(
                        this,
                        "Usuario eliminado definitivamente de SQLite y Firestore.",
                        Toast.LENGTH_LONG
                    ).show()

                    dialogo.dismiss()
                    cargarUsuarios()

                } else {
                    boton.isEnabled = true
                    boton.text = "ELIMINAR DEFINITIVAMENTE"

                    Toast.makeText(
                        this,
                        "El usuario se eliminó de Firestore, pero no fue posible eliminarlo de SQLite. Se requiere revisar la sincronización.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
            .addOnFailureListener { error ->

                boton.isEnabled = true
                boton.text = "ELIMINAR DEFINITIVAMENTE"

                Toast.makeText(
                    this,
                    "No se eliminó el usuario. Firestore informó: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    // =====================================================
    // ELIMINAR USUARIO LOCAL Y CONSERVAR PEDIDOS
    // =====================================================

    private fun eliminarUsuarioDeSQLite(usuario: UsuarioAdmin): Boolean {

        val dbHelper = DatabaseHelper(this)

        return try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()

            try {
                // Los pedidos del comprador se conservan sin el documento personal.
                if (usuario.rol.equals("CLIENTE", ignoreCase = true)) {
                    val valoresPedido = ContentValues().apply {
                        putNull("documento_comprador")
                    }

                    db.update(
                        "pedidos",
                        valoresPedido,
                        "documento_comprador = ?",
                        arrayOf(usuario.documento)
                    )
                }

                val filasEliminadas = db.delete(
                    "usuarios",
                    "documento = ?",
                    arrayOf(usuario.documento)
                )

                if (filasEliminadas <= 0) {
                    throw IllegalStateException(
                        "No se encontró el usuario en la base de datos local."
                    )
                }

                db.setTransactionSuccessful()
                true

            } finally {
                db.endTransaction()
            }

        } catch (e: Exception) {
            false

        } finally {
            dbHelper.close()
        }
    }

    // =====================================================
    // VALIDAR ESTADO DEL USUARIO
    // =====================================================

    private fun esUsuarioActivo(estado: String): Boolean {

        return estado == "1" ||
                estado.equals("ACTIVO", ignoreCase = true)
    }

    // =====================================================
    // COMPONENTES VISUALES AUXILIARES
    // =====================================================

    private fun crearTexto(contenido: String): TextView {

        return TextView(this).apply {

            text = contenido
            textSize = 14f

            setTextColor(
                Color.rgb(100, 110, 125)
            )

            setPadding(0, 7, 0, 0)
        }
    }

    private fun fondoRedondeado(
        color: Int,
        radio: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)
            cornerRadius = radio

            setStroke(
                1,
                Color.rgb(230, 234, 240)
            )
        }
    }
}

