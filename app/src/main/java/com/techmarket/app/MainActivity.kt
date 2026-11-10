package com.techmarket.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Base64
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.techmarket.app.database.DatabaseContract
import com.techmarket.app.database.UsuarioDAO
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ABRIR_LOGIN = "abrir_login"
        const val EXTRA_CONTINUAR_COMPRA = "continuar_compra"
    }

    private lateinit var etDocumento: EditText
    private lateinit var etContrasena: EditText
    private lateinit var btnIniciarSesion: Button
    private lateinit var tvOlvideContrasena: TextView
    private lateinit var btnRegistrarse: Button
    private lateinit var tvAccesoVendedor: TextView
    private lateinit var btnAccesoBiometrico: Button

    private val preferenciasBiometria by lazy {
        getSharedPreferences("techmarket_biometria", Context.MODE_PRIVATE)
    }
    private val keyDocumentoBiometrico = "documento_biometrico"

    private val handler = Handler(Looper.getMainLooper())

    private val mostrarInicioComprador = Runnable {
        mostrarPantallaComprador()
    }

    // --------------------------------------------------
    // INICIO DE LA APLICACIÓN
    // --------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configurarControlBotonAtras()

        if (intent.getBooleanExtra(EXTRA_ABRIR_LOGIN, false)) {
            mostrarPantallaLogin()
            return
        }

        setContentView(R.layout.activity_bienvenida)

        handler.postDelayed(mostrarInicioComprador, 3000)
    }

    // --------------------------------------------------
    // NAVEGACIÓN PRINCIPAL
    // --------------------------------------------------

    private fun mostrarPantallaComprador() {
        handler.removeCallbacks(mostrarInicioComprador)

        val intent = Intent(
            this,
            InicioCompradorActivity::class.java
        )

        startActivity(intent)
        finish()
    }

    private fun mostrarPantallaLogin() {
        handler.removeCallbacks(mostrarInicioComprador)

        setContentView(R.layout.activity_main)

        inicializarComponentes()
        configurarEventos()
    }

    // --------------------------------------------------
    // INICIALIZAR COMPONENTES DEL LOGIN
    // --------------------------------------------------

    private fun inicializarComponentes() {
        etDocumento = findViewById(R.id.etDocumento)
        etContrasena = findViewById(R.id.etContrasena)
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion)
        tvOlvideContrasena = findViewById(R.id.tvOlvideContrasena)
        btnRegistrarse = findViewById(R.id.btnRegistrarse)
        tvAccesoVendedor = findViewById(R.id.tvAccesoVendedor)
        btnAccesoBiometrico = findViewById(R.id.btnAccesoBiometrico)
    }

    private fun configurarEventos() {

        configurarAccesoBiometrico()

        btnIniciarSesion.setOnClickListener {
            validarFormulario()
        }

        btnAccesoBiometrico.setOnClickListener {
            autenticarConHuella()
        }

        tvOlvideContrasena.setOnClickListener {
            mostrarRecuperacionContrasena()
        }

        btnRegistrarse.setOnClickListener {
            mostrarPantallaRegistro()
        }

        tvAccesoVendedor.setOnClickListener {
            val intent = Intent(
                this,
                RegistroVendedorActivity::class.java
            )
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnVolverLogin).setOnClickListener {

            val intent = Intent(this, InicioCompradorActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            startActivity(intent)
            finish()
        }
    }

    // --------------------------------------------------
    // ACCESO BIOMÉTRICO
    // --------------------------------------------------
    private fun configurarAccesoBiometrico() {
        val documentoGuardado = preferenciasBiometria.getString(keyDocumentoBiometrico, null)
        val biometricManager = BiometricManager.from(this)
        val disponible = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS

        btnAccesoBiometrico.visibility = if (disponible && !documentoGuardado.isNullOrBlank()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun crearPromptBiometrico(
        alAutenticar: () -> Unit,
        alCancelar: (() -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    alAutenticar()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                    alCancelar?.invoke()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@MainActivity, "No se reconoció la huella. Inténtalo nuevamente.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso a TechMarket")
            .setSubtitle("Confirma tu identidad con tu huella")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(promptInfo)
    }

    private fun autenticarConHuella() {
        val documento = preferenciasBiometria.getString(keyDocumentoBiometrico, null)
        if (documento.isNullOrBlank()) {
            configurarAccesoBiometrico()
            Toast.makeText(this, "Primero inicia sesión y activa el acceso con huella.", Toast.LENGTH_LONG).show()
            return
        }
        crearPromptBiometrico(
            alAutenticar = {
                iniciarSesionBiometrica(documento)
            }
        )
    }

    private fun iniciarSesionBiometrica(documento: String) {
        try {
            val cursor = UsuarioDAO(this).consultarUsuario(documento)
            cursor.use {
                if (!it.moveToFirst()) {
                    preferenciasBiometria.edit().remove(keyDocumentoBiometrico).apply()
                    configurarAccesoBiometrico()
                    Toast.makeText(this, "La cuenta asociada ya no existe. Inicia sesión con tu contraseña.", Toast.LENGTH_LONG).show()
                    return
                }
                val id = it.getLong(it.getColumnIndexOrThrow(DatabaseContract.Usuarios.ID))
                val nombres = it.getString(it.getColumnIndexOrThrow(DatabaseContract.Usuarios.NOMBRES))
                val apellidos = it.getString(it.getColumnIndexOrThrow(DatabaseContract.Usuarios.APELLIDOS))
                val rol = it.getString(it.getColumnIndexOrThrow(DatabaseContract.Usuarios.ROL)).trim().uppercase()
                val estado = it.getString(it.getColumnIndexOrThrow(DatabaseContract.Usuarios.ESTADO))?.trim()
                val activo = estado.equals("ACTIVO", true) || estado == "1"
                if (!activo || rol !in listOf("CLIENTE", "VENDEDOR", "ADMINISTRADOR")) {
                    Toast.makeText(this, "La cuenta está inactiva o tiene un rol no válido.", Toast.LENGTH_LONG).show()
                    return
                }
                SessionManager.guardarSesion(this, id, documento, "$nombres $apellidos", rol)
                Toast.makeText(this, "¡Bienvenido, $nombres!", Toast.LENGTH_SHORT).show()
                navegarSegunRol(rol)
            }
        } catch (_: Exception) {
            Toast.makeText(this, "No fue posible validar el acceso biométrico.", Toast.LENGTH_LONG).show()
        }
    }

    private fun ofrecerActivacionBiometrica(documento: String, continuar: () -> Unit) {
        val documentoActual = preferenciasBiometria.getString(keyDocumentoBiometrico, null)

        // Si esta cuenta ya está asociada, no volver a preguntar.
        if (documentoActual == documento) {
            continuar()
            return
        }

        val estadoBiometrico = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        val mensaje = if (documentoActual.isNullOrBlank()) {
            "¿Deseas habilitar el acceso con huella para esta cuenta en este dispositivo?"
        } else {
            "Ya hay otra cuenta asociada al acceso biométrico en este dispositivo. ¿Deseas reemplazarla por esta cuenta?"
        }

        // El diálogo se muestra después del login válido aunque el emulador
        // todavía no tenga una huella biométrica enrolada. La asociación
        // solo se guarda después de una autenticación biométrica exitosa.
        AlertDialog.Builder(this)
            .setTitle("Activar acceso con huella")
            .setMessage(mensaje)
            .setNegativeButton("Ahora no") { _, _ -> continuar() }
            .setPositiveButton("Activar") { _, _ ->
                if (estadoBiometrico != BiometricManager.BIOMETRIC_SUCCESS) {
                    val detalle = when (estadoBiometrico) {
                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                            "Este emulador no tiene una huella registrada para autenticación. No se activó el acceso biométrico."
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                            "Este dispositivo no dispone de hardware biométrico compatible."
                        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                            "El sensor biométrico no está disponible en este momento."
                        else ->
                            "La biometría no está disponible en este dispositivo."
                    }
                    Toast.makeText(this, detalle, Toast.LENGTH_LONG).show()
                    continuar()
                    return@setPositiveButton
                }

                crearPromptBiometrico(
                    alAutenticar = {
                        preferenciasBiometria.edit()
                            .putString(keyDocumentoBiometrico, documento)
                            .apply()
                        configurarAccesoBiometrico()
                        Toast.makeText(
                            this,
                            "Acceso con huella activado.",
                            Toast.LENGTH_SHORT
                        ).show()
                        continuar()
                    },
                    alCancelar = continuar
                )
            }
            .setOnCancelListener { continuar() }
            .show()
    }

    private fun navegarSegunRol(rol: String) {
        when (rol) {
            "CLIENTE" -> {
                if (intent.getBooleanExtra(EXTRA_CONTINUAR_COMPRA, false)) {
                    setResult(RESULT_OK)
                    finish()
                } else mostrarPantallaComprador()
            }
            "VENDEDOR" -> {
                startActivity(Intent(this, InicioVendedorActivity::class.java))
                finish()
            }
            "ADMINISTRADOR" -> {
                startActivity(Intent(this, InicioAdministradorActivity::class.java))
                finish()
            }
        }
    }

    // --------------------------------------------------
    // REGISTRO DE CLIENTES
    // --------------------------------------------------

    private fun mostrarPantallaRegistro() {
        handler.removeCallbacks(mostrarInicioComprador)

        setContentView(R.layout.activity_registro)

        configurarSelectorDocumento()

        val tvVolverLogin =
            findViewById<TextView>(R.id.tvVolverLoginRegistro)

        tvVolverLogin.setOnClickListener {
            mostrarPantallaLogin()
        }

        val btnRegistrarseCliente =
            findViewById<Button>(R.id.btnRegistrarseCliente)

        btnRegistrarseCliente.setOnClickListener {
            registrarCliente()
        }

        findViewById<ImageButton>(R.id.btnVolverRegistro).setOnClickListener {
            mostrarPantallaLogin()
        }
    }

    // --------------------------------------------------
    // SELECTOR DE TIPO DE DOCUMENTO
    // --------------------------------------------------

    private fun configurarSelectorDocumento() {

        val spTipoDocumento =
            findViewById<Spinner>(R.id.spTipoDocumentoRegistro)

        val tiposDocumento = listOf(
            "Selecciona tu tipo de documento",
            "Cédula de ciudadanía",
            "Tarjeta de identidad",
            "Cédula de extranjería",
            "Pasaporte"
        )

        val adaptador = object : ArrayAdapter<String>(
            this,
            R.layout.item_tipo_documento,
            tiposDocumento
        ) {

            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val vista = super.getView(
                    position,
                    convertView,
                    parent
                )

                val texto = vista as TextView

                texto.text = getItem(position)
                texto.textSize = 16f
                texto.setTextColor(0xFF858585.toInt())
                texto.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
                texto.gravity = Gravity.CENTER_VERTICAL

                return vista
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {

                val vista = super.getDropDownView(
                    position,
                    convertView,
                    parent
                )

                val texto = vista as TextView

                texto.text = getItem(position)
                texto.textSize = 16f
                texto.setTextColor(0xFF373737.toInt())
                texto.gravity = Gravity.CENTER_VERTICAL

                return vista
            }
        }

        adaptador.setDropDownViewResource(
            R.layout.item_tipo_documento_dropdown
        )

        spTipoDocumento.adapter = adaptador
        spTipoDocumento.setSelection(0)
        spTipoDocumento.isClickable = true

        val flechaDocumento = findViewById<View>(
            R.id.flechaTipoDocumento
        )

        flechaDocumento.setOnClickListener {
            spTipoDocumento.performClick()
        }
    }

    // --------------------------------------------------
    // REGISTRAR CLIENTE MEDIANTE USUARIODAO
    // --------------------------------------------------

    private fun registrarCliente() {

        val spTipoDocumento =
            findViewById<Spinner>(R.id.spTipoDocumentoRegistro)

        val etDocumentoRegistro =
            findViewById<EditText>(R.id.etDocumentoRegistro)

        val etNombresRegistro =
            findViewById<EditText>(R.id.etNombresRegistro)

        val etApellidosRegistro =
            findViewById<EditText>(R.id.etApellidosRegistro)

        val etCorreoRegistro =
            findViewById<EditText>(R.id.etCorreoRegistro)

        val etContrasenaRegistro =
            findViewById<EditText>(R.id.etContrasenaRegistro)

        val documento =
            etDocumentoRegistro.text.toString().trim()

        val nombres =
            etNombresRegistro.text.toString().trim()

        val apellidos =
            etApellidosRegistro.text.toString().trim()

        val correo =
            etCorreoRegistro.text.toString().trim()

        val contrasena =
            etContrasenaRegistro.text.toString()

        if (spTipoDocumento.selectedItemPosition == 0) {
            Toast.makeText(
                this,
                "Selecciona tu tipo de documento.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (documento.isEmpty()) {
            etDocumentoRegistro.error =
                "Ingresa tu número de documento"
            etDocumentoRegistro.requestFocus()
            return
        }

        if (nombres.isEmpty()) {
            etNombresRegistro.error = "Ingresa tus nombres"
            etNombresRegistro.requestFocus()
            return
        }

        if (apellidos.isEmpty()) {
            etApellidosRegistro.error = "Ingresa tus apellidos"
            etApellidosRegistro.requestFocus()
            return
        }

        if (correo.isEmpty()) {
            etCorreoRegistro.error =
                "Ingresa tu correo electrónico"
            etCorreoRegistro.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreoRegistro.error =
                "Ingresa un correo electrónico válido"
            etCorreoRegistro.requestFocus()
            return
        }

        if (contrasena.isEmpty()) {
            etContrasenaRegistro.error =
                "Ingresa una contraseña"
            etContrasenaRegistro.requestFocus()
            return
        }

        if (contrasena.length < 8) {
            etContrasenaRegistro.error =
                "La contraseña debe tener al menos 8 caracteres"
            etContrasenaRegistro.requestFocus()
            return
        }

        val passwordHash = try {
            generarHashContrasena(contrasena)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "No fue posible proteger la contraseña.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val tipoDocumento =
            spTipoDocumento.selectedItem.toString()

        val usuarioDAO = UsuarioDAO(this)

        try {
            val resultado = usuarioDAO.registrarUsuario(
                tipoDocumento = tipoDocumento,
                documento = documento,
                nombres = nombres,
                apellidos = apellidos,
                correo = correo,
                passwordHash = passwordHash,
                rol = "CLIENTE"
            )

            if (resultado == -1L) {
                Toast.makeText(
                    this,
                    "Ya existe una cuenta con ese documento.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            Toast.makeText(
                this,
                "¡Registro exitoso! Tu cuenta fue creada.",
                Toast.LENGTH_LONG
            ).show()

            mostrarPantallaLogin()

        } catch (_: SQLiteConstraintException) {
            Toast.makeText(
                this,
                "No fue posible registrar la cuenta. Verifica los datos.",
                Toast.LENGTH_LONG
            ).show()

        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Ocurrió un error al registrar la cuenta.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --------------------------------------------------
    // RECUPERACIÓN LOCAL DE CONTRASEÑA
    // --------------------------------------------------

    private fun mostrarRecuperacionContrasena() {

        val campoDocumento = EditText(this).apply {
            hint = "Número de documento"
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            val margen =
                (24 * resources.displayMetrics.density).toInt()

            setPadding(margen, 0, margen, 0)

            addView(
                campoDocumento,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage("Ingresa el documento asociado a tu cuenta.")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar", null)
            .create()
            .also { dialog ->

                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {

                            val documento =
                                campoDocumento.text.toString().trim()

                            if (documento.isBlank()) {
                                campoDocumento.error =
                                    "Ingresa tu número de documento"
                                campoDocumento.requestFocus()
                                return@setOnClickListener
                            }

                            if (!existeCuentaParaRecuperar(documento)) {
                                campoDocumento.error =
                                    "No se encontró una cuenta con ese documento"
                                campoDocumento.requestFocus()
                                return@setOnClickListener
                            }

                            dialog.dismiss()
                            mostrarFormularioNuevaContrasena(documento)
                        }
                }

                dialog.show()
            }
    }

    private fun existeCuentaParaRecuperar(
        documento: String
    ): Boolean {

        val usuarioDAO = UsuarioDAO(this)

        return try {
            val cursor = usuarioDAO.consultarUsuario(documento)
            cursor.use {
                it.moveToFirst()
            }

        } catch (_: Exception) {
            Toast.makeText(
                this,
                "No fue posible consultar la cuenta.",
                Toast.LENGTH_LONG
            ).show()

            false
        }
    }

    private fun mostrarFormularioNuevaContrasena(
        documento: String
    ) {

        val campoNuevaContrasena = EditText(this).apply {
            hint = "Nueva contraseña"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }

        val campoConfirmarContrasena = EditText(this).apply {
            hint = "Confirmar nueva contraseña"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            val margen =
                (24 * resources.displayMetrics.density).toInt()

            setPadding(margen, 0, margen, 0)

            addView(
                campoNuevaContrasena,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addView(
                campoConfirmarContrasena,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Crear nueva contraseña")
            .setMessage("La contraseña debe tener al menos 8 caracteres.")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar contraseña", null)
            .create()
            .also { dialog ->

                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {

                            val nueva =
                                campoNuevaContrasena.text.toString()

                            val confirmacion =
                                campoConfirmarContrasena.text.toString()

                            if (nueva.length < 8) {
                                campoNuevaContrasena.error =
                                    "Debe tener al menos 8 caracteres"
                                campoNuevaContrasena.requestFocus()
                                return@setOnClickListener
                            }

                            if (nueva != confirmacion) {
                                campoConfirmarContrasena.error =
                                    "Las contraseñas no coinciden"
                                campoConfirmarContrasena.requestFocus()
                                return@setOnClickListener
                            }

                            actualizarContrasenaRecuperada(
                                documento,
                                nueva
                            )

                            dialog.dismiss()
                        }
                }

                dialog.show()
            }
    }

    private fun actualizarContrasenaRecuperada(
        documento: String,
        nuevaContrasena: String
    ) {

        val hashNuevo = try {
            generarHashContrasena(nuevaContrasena)
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "No fue posible proteger la nueva contraseña.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val usuarioDAO = UsuarioDAO(this)

        try {
            val actualizacionExitosa =
                usuarioDAO.actualizarContrasena(
                    documento = documento,
                    passwordHash = hashNuevo
                )

            if (actualizacionExitosa) {

                Toast.makeText(
                    this,
                    "Contraseña actualizada. Ya puedes iniciar sesión.",
                    Toast.LENGTH_LONG
                ).show()

                etDocumento.setText(documento)
                etContrasena.text.clear()
                etContrasena.requestFocus()

            } else {

                Toast.makeText(
                    this,
                    "No fue posible actualizar la contraseña.",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Ocurrió un error al actualizar la contraseña.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --------------------------------------------------
    // GENERAR HASH DE CONTRASEÑA
    // --------------------------------------------------

    private fun generarHashContrasena(
        contrasena: String
    ): String {

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val iteraciones = 120000

        val especificacion = PBEKeySpec(
            contrasena.toCharArray(),
            salt,
            iteraciones,
            256
        )

        return try {

            val factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            )

            val hash =
                factory.generateSecret(especificacion).encoded

            val saltCodificado = Base64.encodeToString(
                salt,
                Base64.NO_WRAP
            )

            val hashCodificado = Base64.encodeToString(
                hash,
                Base64.NO_WRAP
            )

            "PBKDF2WithHmacSHA256:$iteraciones:$saltCodificado:$hashCodificado"

        } finally {
            especificacion.clearPassword()
        }
    }

    // --------------------------------------------------
    // VERIFICAR CONTRASEÑA
    // --------------------------------------------------

    private fun verificarContrasena(
        contrasena: String,
        hashAlmacenado: String
    ): Boolean {

        return try {

            val partes = hashAlmacenado.split(":")

            if (partes.size != 4) return false

            val algoritmo = partes[0]
            val iteraciones = partes[1].toInt()

            val salt = Base64.decode(
                partes[2],
                Base64.NO_WRAP
            )

            val hashOriginal = Base64.decode(
                partes[3],
                Base64.NO_WRAP
            )

            if (algoritmo != "PBKDF2WithHmacSHA256") {
                return false
            }

            if (iteraciones <= 0 || salt.isEmpty() || hashOriginal.isEmpty()) {
                return false
            }

            val especificacion = PBEKeySpec(
                contrasena.toCharArray(),
                salt,
                iteraciones,
                hashOriginal.size * 8
            )

            val hashCalculado = try {

                SecretKeyFactory
                    .getInstance(algoritmo)
                    .generateSecret(especificacion)
                    .encoded

            } finally {
                especificacion.clearPassword()
            }

            MessageDigest.isEqual(
                hashOriginal,
                hashCalculado
            )

        } catch (_: Exception) {
            false
        }
    }

    // --------------------------------------------------
    // VALIDAR INICIO DE SESIÓN MEDIANTE USUARIODAO
    // --------------------------------------------------

    private fun validarFormulario() {

        val documento =
            etDocumento.text.toString().trim()

        val contrasena =
            etContrasena.text.toString()

        if (documento.isEmpty()) {
            etDocumento.error =
                "Ingresa tu número de documento"
            etDocumento.requestFocus()
            return
        }

        if (contrasena.isEmpty()) {
            etContrasena.error =
                "Ingresa tu contraseña"
            etContrasena.requestFocus()
            return
        }

        val usuarioDAO = UsuarioDAO(this)

        try {

            val cursor = usuarioDAO.consultarUsuario(documento)

            cursor.use {

                if (!it.moveToFirst()) {
                    Toast.makeText(
                        this,
                        "El documento o la contraseña son incorrectos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val id = it.getLong(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.ID
                    )
                )

                val nombres = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.NOMBRES
                    )
                )

                val apellidos = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.APELLIDOS
                    )
                )

                val hashAlmacenado = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.PASSWORD_HASH
                    )
                )

                val rol = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.ROL
                    )
                )

                // --------------------------------------------------
                // VALIDAR ESTADO DE LA CUENTA
                // Acepta ACTIVO/INACTIVO y 1/0.
                // --------------------------------------------------

                val estado = it.getString(
                    it.getColumnIndexOrThrow(
                        DatabaseContract.Usuarios.ESTADO
                    )
                )?.trim()

                val cuentaActiva =
                    estado.equals("ACTIVO", ignoreCase = true) ||
                            estado == "1"

                if (!cuentaActiva) {
                    Toast.makeText(
                        this,
                        "Tu cuenta se encuentra inactiva.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                if (!verificarContrasena(contrasena, hashAlmacenado)) {
                    Toast.makeText(
                        this,
                        "El documento o la contraseña son incorrectos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val rolNormalizado = rol.trim().uppercase()

                if (
                    rolNormalizado !in listOf(
                        "CLIENTE",
                        "VENDEDOR",
                        "ADMINISTRADOR"
                    )
                ) {
                    Toast.makeText(
                        this,
                        "El rol de esta cuenta no está reconocido.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val continuar = {
                    Toast.makeText(
                        this,
                        "¡Bienvenido, $nombres!",
                        Toast.LENGTH_SHORT
                    ).show()
                    navegarSegunRol(rolNormalizado)
                }

                SessionManager.guardarSesion(
                    context = this,
                    id = id,
                    documento = documento,
                    nombre = "$nombres $apellidos",
                    rol = rolNormalizado
                )

                ofrecerActivacionBiometrica(documento, continuar)
            }

        } catch (_: Exception) {

            Toast.makeText(
                this,
                "No fue posible validar el inicio de sesión.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --------------------------------------------------
    // CONTROL DEL BOTÓN ATRÁS
    // --------------------------------------------------

    private fun configurarControlBotonAtras() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Si está visible el formulario de registro,
                    // regresar al login. En las demás pantallas,
                    // permitir el comportamiento normal de Android.
                    val vistaVolverRegistro =
                        findViewById<View?>(R.id.tvVolverLoginRegistro)

                    if (vistaVolverRegistro != null) {
                        mostrarPantallaLogin()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    // --------------------------------------------------
    // DESTRUIR ACTIVIDAD
    // --------------------------------------------------

    override fun onDestroy() {
        handler.removeCallbacks(mostrarInicioComprador)
        super.onDestroy()
    }
}

// --------------------------------------------------
// ADMINISTRADOR DE SESIÓN DE TECHMARKET
// --------------------------------------------------

object SessionManager {

    private const val PREFERENCIAS = "techmarket_sesion"

    private const val KEY_SESION_ACTIVA = "sesion_activa"
    private const val KEY_ID = "usuario_id"
    private const val KEY_DOCUMENTO = "usuario_documento"
    private const val KEY_NOMBRE = "usuario_nombre"
    private const val KEY_ROL = "usuario_rol"

    fun guardarSesion(
        context: Context,
        id: Long,
        documento: String,
        nombre: String,
        rol: String
    ) {

        val preferencias = context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        )

        preferencias.edit()
            .putBoolean(KEY_SESION_ACTIVA, true)
            .putLong(KEY_ID, id)
            .putString(KEY_DOCUMENTO, documento)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_ROL, rol)
            .apply()
    }

    fun haySesionActiva(context: Context): Boolean {

        val preferencias = context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        )

        return preferencias.getBoolean(
            KEY_SESION_ACTIVA,
            false
        )
    }

    fun obtenerDocumento(context: Context): String? {
        return context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        ).getString(KEY_DOCUMENTO, null)
    }

    fun obtenerNombre(context: Context): String? {
        return context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        ).getString(KEY_NOMBRE, null)
    }

    fun obtenerRol(context: Context): String? {
        return context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        ).getString(KEY_ROL, null)
    }

    fun cerrarSesion(context: Context) {
        context.getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        ).edit().clear().apply()
    }
}
