package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.DireccionDAO
import com.techmarket.app.database.UsuarioDAO

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
    private lateinit var spinnerDirecciones: Spinner

    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var direccionDAO: DireccionDAO

    private lateinit var documentoUsuario: String

    private val direccionesGuardadas =
        mutableListOf<DireccionEntrega>()

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

                    // La columna correo pertenece a la tabla usuarios.
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

        // Volver a la pantalla anterior.
        btnVolver.setOnClickListener {
            finish()
        }

        // Seleccionar una dirección guardada.
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

        // Continuar al pago.
        btnContinuarPago.setOnClickListener {
            validarDatosEntrega()
        }
    }

    // SELECCIONAR DIRECCIÓN

    private fun seleccionarDireccion(
        direccion: DireccionEntrega
    ) {

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

        // GUARDAR DATOS TEMPORALMENTE.

        val datosEntrega = DatosEntrega(
            nombre = nombre,
            documento = documento,
            telefono = telefono,
            correo = correo,
            departamento = departamento,
            ciudad = ciudad,
            direccion = direccion,
            informacionAdicional = informacionAdicional
        )

        DatosEntregaManager.guardar(datosEntrega)

        Toast.makeText(
            this,
            "Datos de entrega guardados correctamente.",
            Toast.LENGTH_SHORT
        ).show()

        // ABRIR PASARELA DE PAGOS.

        val intent = Intent(
            this,
            PasarelaPagoActivity::class.java
        )

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
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
    val informacionAdicional: String
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
