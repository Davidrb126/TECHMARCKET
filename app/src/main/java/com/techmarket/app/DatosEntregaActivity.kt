package com.techmarket.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.techmarket.app.database.DireccionDAO
import com.techmarket.app.database.UsuarioDAO
import java.util.Locale
import java.util.concurrent.Executors

class DatosEntregaActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etDocumento: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etDepartamento: EditText
    private lateinit var etCiudad: EditText
    private lateinit var etDireccion: EditText
    private lateinit var etInformacionAdicional: EditText

    private lateinit var btnVolver: TextView
    private lateinit var btnContinuarPago: Button
    private lateinit var btnUsarMiUbicacion: Button
    private lateinit var spinnerDirecciones: Spinner

    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var direccionDAO: DireccionDAO

    private lateinit var documentoUsuario: String

    private val direccionesGuardadas =
        mutableListOf<DireccionEntrega>()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val geocoderExecutor =
        Executors.newSingleThreadExecutor()

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var latitudUbicacion: Double? = null
    private var longitudUbicacion: Double? = null

    private val solicitudPermisosUbicacion =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->

            val ubicacionPermitida =
                permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (ubicacionPermitida) {
                obtenerUbicacionActual()
            } else {
                Toast.makeText(
                    this,
                    "No se concedió el permiso de ubicación. Puedes ingresar la dirección manualmente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_entrega)

        usuarioDAO = UsuarioDAO(this)
        direccionDAO = DireccionDAO(this)

        documentoUsuario =
            SessionManager.obtenerDocumento(this).orEmpty()

        inicializarComponentes()
        precargarDatosComprador()
        cargarDireccionesGuardadas()
        configurarEventos()
    }

    // INICIALIZAR COMPONENTES

    private fun inicializarComponentes() {

        etNombre = findViewById(R.id.etNombreEntrega)
        etDocumento = findViewById(R.id.etDocumentoEntrega)
        etTelefono = findViewById(R.id.etTelefonoEntrega)
        etCorreo = findViewById(R.id.etCorreoEntrega)

        etDepartamento =
            findViewById(R.id.etDepartamentoEntrega)

        etCiudad = findViewById(R.id.etCiudadEntrega)
        etDireccion = findViewById(R.id.etDireccionEntrega)

        etInformacionAdicional =
            findViewById(R.id.etInformacionAdicional)

        btnVolver =
            findViewById(R.id.btnVolverDirecciones)

        btnContinuarPago =
            findViewById(R.id.btnContinuarPago)

        btnUsarMiUbicacion =
            findViewById(R.id.btnUsarMiUbicacion)

        spinnerDirecciones =
            findViewById(R.id.spinnerDireccionesGuardadas)
    }

    // PRECARGAR DATOS DEL COMPRADOR

    private fun precargarDatosComprador() {

        etNombre.setText(
            SessionManager.obtenerNombre(this).orEmpty()
        )

        etDocumento.setText(documentoUsuario)

        if (documentoUsuario.isNotBlank()) {

            val cursor =
                usuarioDAO.consultarUsuario(documentoUsuario)

            cursor.use {

                if (it.moveToFirst()) {

                    val indiceCorreo =
                        it.getColumnIndex("correo")

                    if (indiceCorreo >= 0) {
                        etCorreo.setText(
                            it.getString(indiceCorreo).orEmpty()
                        )
                    }
                }
            }
        }
    }

    // CARGAR DIRECCIONES GUARDADAS

    private fun cargarDireccionesGuardadas() {

        direccionesGuardadas.clear()

        if (documentoUsuario.isBlank()) {
            configurarSpinnerVacio()
            return
        }

        direccionesGuardadas.addAll(
            direccionDAO.obtenerDirecciones(documentoUsuario)
        )

        val opciones = mutableListOf<String>()

        opciones.add("Selecciona una dirección")

        direccionesGuardadas.forEach { direccion ->

            val etiqueta = if (direccion.predeterminada) {
                "${direccion.alias} (Predeterminada)"
            } else {
                direccion.alias
            }

            opciones.add(etiqueta)
        }

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opciones
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerDirecciones.adapter = adaptador

        spinnerDirecciones.isEnabled =
            direccionesGuardadas.isNotEmpty()
    }

    private fun configurarSpinnerVacio() {

        val opciones = listOf(
            "No hay direcciones guardadas"
        )

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opciones
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerDirecciones.adapter = adaptador
        spinnerDirecciones.isEnabled = false
    }

    // CONFIGURAR EVENTOS

    private fun configurarEventos() {

        btnVolver.setOnClickListener {
            finish()
        }

        btnUsarMiUbicacion.setOnClickListener {
            solicitarPermisoUbicacion()
        }

        spinnerDirecciones.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (position > 0) {

                        val direccion =
                            direccionesGuardadas.getOrNull(position - 1)

                        if (direccion != null) {
                            seleccionarDireccion(direccion)
                        }
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    // No se requiere ninguna acción.
                }
            }

        btnContinuarPago.setOnClickListener {
            validarDatosEntrega()
        }
    }

    // SOLICITAR PERMISOS DE UBICACIÓN

    private fun solicitarPermisoUbicacion() {

        val tieneUbicacionPrecisa =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val tieneUbicacionAproximada =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (tieneUbicacionPrecisa || tieneUbicacionAproximada) {
            obtenerUbicacionActual()
        } else {
            solicitudPermisosUbicacion.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // OBTENER COORDENADAS ACTUALES

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionActual() {

        val tieneUbicacionPrecisa =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val tieneUbicacionAproximada =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!tieneUbicacionPrecisa && !tieneUbicacionAproximada) {
            return
        }

        btnUsarMiUbicacion.isEnabled = false
        btnUsarMiUbicacion.text = "Obteniendo ubicación..."

        val prioridad = if (tieneUbicacionPrecisa) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val cancellationTokenSource =
            CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            prioridad,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->

            if (location != null) {
                procesarUbicacion(
                    location.latitude,
                    location.longitude
                )
            } else {
                obtenerUltimaUbicacion()
            }

        }.addOnFailureListener {

            obtenerUltimaUbicacion()
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUltimaUbicacion() {

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {
                    procesarUbicacion(
                        location.latitude,
                        location.longitude
                    )
                } else {
                    restaurarBotonUbicacion()

                    Toast.makeText(
                        this,
                        "No fue posible obtener tu ubicación. Verifica que la ubicación del dispositivo esté activada.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener {

                restaurarBotonUbicacion()

                Toast.makeText(
                    this,
                    "Ocurrió un error al consultar la ubicación.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // CONVERTIR COORDENADAS EN DIRECCIÓN

    private fun procesarUbicacion(
        latitud: Double,
        longitud: Double
    ) {

        latitudUbicacion = latitud
        longitudUbicacion = longitud

        if (!Geocoder.isPresent()) {
            restaurarBotonUbicacion()

            Toast.makeText(
                this,
                "No está disponible el servicio para convertir la ubicación en dirección.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val geocoder = Geocoder(
            this,
            Locale("es", "CO")
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            geocoder.getFromLocation(
                latitud,
                longitud,
                1,
                object : Geocoder.GeocodeListener {

                    override fun onGeocode(addresses: MutableList<Address>) {
                        mainHandler.post {
                            aplicarDireccionObtenida(addresses)
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        mainHandler.post {
                            restaurarBotonUbicacion()

                            Toast.makeText(
                                this@DatosEntregaActivity,
                                "Se obtuvieron las coordenadas, pero no fue posible identificar la dirección. Puedes ingresarla manualmente.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )

        } else {

            geocoderExecutor.execute {

                try {
                    @Suppress("DEPRECATION")
                    val direcciones = geocoder.getFromLocation(
                        latitud,
                        longitud,
                        1
                    )

                    mainHandler.post {
                        aplicarDireccionObtenida(
                            direcciones.orEmpty()
                        )
                    }

                } catch (e: Exception) {

                    mainHandler.post {
                        restaurarBotonUbicacion()

                        Toast.makeText(
                            this,
                            "No fue posible identificar la dirección. Puedes ingresarla manualmente.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // LLENAR LOS CAMPOS DE DIRECCIÓN

    private fun aplicarDireccionObtenida(
        direcciones: List<Address>
    ) {

        restaurarBotonUbicacion()

        val direccion = direcciones.firstOrNull()

        if (direccion == null) {

            Toast.makeText(
                this,
                "No se encontró una dirección para esta ubicación. Puedes ingresarla manualmente.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val departamento =
            direccion.adminArea.orEmpty()

        val ciudad =
            direccion.locality
                ?: direccion.subAdminArea
                ?: direccion.subLocality
                ?: ""

        val direccionCalle = listOfNotNull(
            direccion.thoroughfare,
            direccion.subThoroughfare
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val direccionFinal = if (direccionCalle.isNotBlank()) {
            direccionCalle
        } else {
            direccion.featureName
                ?.takeIf { it.isNotBlank() && it != ciudad }
                ?: ""
        }

        if (departamento.isNotBlank()) {
            etDepartamento.setText(departamento)
        }

        if (ciudad.isNotBlank()) {
            etCiudad.setText(ciudad)
        }

        if (direccionFinal.isNotBlank()) {
            etDireccion.setText(direccionFinal)
        }

        if (departamento.isBlank() && ciudad.isBlank() &&
            direccionFinal.isBlank()
        ) {
            Toast.makeText(
                this,
                "No se pudieron identificar los datos de la dirección. Puedes completarlos manualmente.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                "Ubicación obtenida. Verifica y completa tu dirección antes de continuar.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun restaurarBotonUbicacion() {
        btnUsarMiUbicacion.isEnabled = true
        btnUsarMiUbicacion.text = "Usar mi ubicación"
    }

    // SELECCIONAR DIRECCIÓN GUARDADA

    private fun seleccionarDireccion(
        direccion: DireccionEntrega
    ) {

        // Una dirección guardada no tiene coordenadas GPS asociadas.
        latitudUbicacion = null
        longitudUbicacion = null

        etNombre.setText(direccion.nombre)
        etTelefono.setText(direccion.telefono)
        etDepartamento.setText(direccion.departamento)
        etCiudad.setText(direccion.ciudad)
        etDireccion.setText(direccion.direccion)

        etInformacionAdicional.setText(
            direccion.informacionAdicional
        )

        Toast.makeText(
            this,
            "Dirección ${direccion.alias} seleccionada.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // VALIDAR DATOS DE ENTREGA

    private fun validarDatosEntrega() {

        val nombre = etNombre.text.toString().trim()
        val documento = etDocumento.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val correo = etCorreo.text.toString().trim()

        val departamento =
            etDepartamento.text.toString().trim()

        val ciudad = etCiudad.text.toString().trim()
        val direccion = etDireccion.text.toString().trim()

        val informacionAdicional =
            etInformacionAdicional.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombre.error = "Ingresa tus nombres y apellidos"
            etNombre.requestFocus()
            return
        }

        if (documento.isEmpty()) {
            etDocumento.error = "Ingresa tu documento"
            etDocumento.requestFocus()
            return
        }

        if (telefono.isEmpty()) {
            etTelefono.error = "Ingresa tu teléfono"
            etTelefono.requestFocus()
            return
        }

        if (telefono.length < 7) {
            etTelefono.error = "Ingresa un teléfono válido"
            etTelefono.requestFocus()
            return
        }

        if (correo.isEmpty()) {
            etCorreo.error = "Ingresa tu correo electrónico"
            etCorreo.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo electrónico válido"
            etCorreo.requestFocus()
            return
        }

        if (departamento.isEmpty()) {
            etDepartamento.error = "Ingresa el departamento"
            etDepartamento.requestFocus()
            return
        }

        if (ciudad.isEmpty()) {
            etCiudad.error = "Ingresa la ciudad o municipio"
            etCiudad.requestFocus()
            return
        }

        if (direccion.isEmpty()) {
            etDireccion.error = "Ingresa la dirección de entrega"
            etDireccion.requestFocus()
            return
        }

        val datosEntrega = DatosEntrega(
            nombre = nombre,
            documento = documento,
            telefono = telefono,
            correo = correo,
            departamento = departamento,
            ciudad = ciudad,
            direccion = direccion,
            informacionAdicional = informacionAdicional,
            latitud = latitudUbicacion,
            longitud = longitudUbicacion
        )

        DatosEntregaManager.guardar(datosEntrega)

        Toast.makeText(
            this,
            "Datos de entrega guardados correctamente.",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(
            this,
            PasarelaPagoActivity::class.java
        )

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        geocoderExecutor.shutdown()
    }
}

// MODELO DE DATOS DE ENTREGA

data class DatosEntrega(
    val nombre: String,
    val documento: String,
    val telefono: String,
    val correo: String,
    val departamento: String,
    val ciudad: String,
    val direccion: String,
    val informacionAdicional: String,
    val latitud: Double? = null,
    val longitud: Double? = null
)

// ALMACENAMIENTO TEMPORAL

object DatosEntregaManager {

    private var datos: DatosEntrega? = null

    fun guardar(datosEntrega: DatosEntrega) {
        datos = datosEntrega
    }

    fun obtener(): DatosEntrega? {
        return datos
    }

    fun limpiar() {
        datos = null
    }
}
