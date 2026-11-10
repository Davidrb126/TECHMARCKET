package com.techmarket.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.DireccionDAO

class DireccionesActivity : AppCompatActivity() {

    private lateinit var direccionDAO: DireccionDAO
    private lateinit var documentoUsuario: String

    private val direcciones = mutableListOf<DireccionEntrega>()

    private val azul = Color.rgb(13, 80, 255)
    private val azulOscuro = Color.rgb(23, 43, 91)
    private val gris = Color.rgb(110, 110, 110)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_direcciones)

        direccionDAO = DireccionDAO(this)

        documentoUsuario =
            SessionManager.obtenerDocumento(this).orEmpty()

        if (documentoUsuario.isBlank()) {
            Toast.makeText(
                this,
                "No se pudo identificar al usuario.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        findViewById<View>(R.id.btnVolverDirecciones)
            .setOnClickListener {
                finish()
            }

        findViewById<View>(R.id.btnAgregarDireccion)
            .setOnClickListener {
                mostrarFormularioDireccion(null)
            }

        cargarDirecciones()
    }

    // CARGAR DIRECCIONES

    private fun cargarDirecciones() {

        direcciones.clear()

        direcciones.addAll(
            direccionDAO.obtenerDirecciones(documentoUsuario)
        )

        mostrarDirecciones()
    }

    // MOSTRAR DIRECCIONES

    private fun mostrarDirecciones() {

        val contenedor = findViewById<LinearLayout>(
            R.id.contenedorDirecciones
        )

        contenedor.removeAllViews()

        if (direcciones.isEmpty()) {

            val mensaje = TextView(this).apply {
                text = "Aún no tienes direcciones guardadas."
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(gris)
                setPadding(12, 35, 12, 35)
            }

            contenedor.addView(mensaje)
            return
        }

        direcciones.forEach { direccion ->

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
                setBackgroundResource(
                    R.drawable.bg_tarjeta_direccion
                )
            }

            // ENCABEZADO DE LA TARJETA

            val encabezado = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val titulo = TextView(this).apply {
                text = direccion.alias
                textSize = 15f
                setTextColor(azulOscuro)
                setTypeface(null, Typeface.BOLD)
            }

            encabezado.addView(
                titulo,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            if (direccion.predeterminada) {

                val etiqueta = TextView(this).apply {
                    text = "Predeterminada"
                    textSize = 10f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(azul)
                    setPadding(8, 6, 8, 6)
                }

                encabezado.addView(etiqueta)
            }

            tarjeta.addView(encabezado)

            // DESTINATARIO

            tarjeta.addView(
                crearTexto(direccion.nombre, true)
            )

            tarjeta.addView(
                crearTexto(direccion.telefono)
            )

            // UBICACIÓN

            tarjeta.addView(
                crearTexto(direccion.direccion)
            )

            tarjeta.addView(
                crearTexto(
                    "${direccion.ciudad}, ${direccion.departamento}"
                )
            )

            if (direccion.informacionAdicional.isNotBlank()) {
                tarjeta.addView(
                    crearTexto(direccion.informacionAdicional)
                )
            }

            // ACCIONES

            val acciones = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, 12, 0, 0)
            }

            if (!direccion.predeterminada) {
                acciones.addView(
                    crearBotonAccion("Predeterminada") {
                        establecerPredeterminada(direccion)
                    }
                )
            }

            acciones.addView(
                crearBotonAccion("Editar") {
                    mostrarFormularioDireccion(direccion)
                }
            )

            acciones.addView(
                crearBotonAccion("Eliminar", true) {
                    confirmarEliminacion(direccion)
                }
            )

            tarjeta.addView(acciones)

            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 14
            }

            contenedor.addView(tarjeta, parametros)
        }
    }

    // TEXTOS DE LAS TARJETAS

    private fun crearTexto(
        contenido: String,
        destacado: Boolean = false
    ): TextView {

        return TextView(this).apply {
            text = contenido
            textSize = 12f

            setTextColor(
                if (destacado) azulOscuro else gris
            )

            if (destacado) {
                setTypeface(null, Typeface.BOLD)
            }

            setPadding(0, 7, 0, 0)
        }
    }

    // BOTONES DE ACCIONES

    private fun crearBotonAccion(
        texto: String,
        peligro: Boolean = false,
        accion: () -> Unit
    ): TextView {

        return TextView(this).apply {

            text = texto
            textSize = 11f
            setTypeface(null, Typeface.BOLD)

            setTextColor(
                if (peligro) {
                    Color.rgb(210, 60, 60)
                } else {
                    azul
                }
            )

            setPadding(10, 8, 10, 8)

            setOnClickListener {
                accion()
            }
        }
    }

    // FORMULARIO PERSONALIZADO

    private fun mostrarFormularioDireccion(
        direccionExistente: DireccionEntrega?
    ) {

        val vista = LayoutInflater.from(this).inflate(
            R.layout.dialog_direccion,
            null
        )

        val etAlias = vista.findViewById<EditText>(
            R.id.etAliasDireccion
        )

        val etNombre = vista.findViewById<EditText>(
            R.id.etNombreDireccion
        )

        val etTelefono = vista.findViewById<EditText>(
            R.id.etTelefonoDireccion
        )

        val etDepartamento = vista.findViewById<EditText>(
            R.id.etDepartamentoDireccion
        )

        val etCiudad = vista.findViewById<EditText>(
            R.id.etCiudadDireccion
        )

        val etDireccion = vista.findViewById<EditText>(
            R.id.etDireccionCompleta
        )

        val etAdicional = vista.findViewById<EditText>(
            R.id.etInformacionAdicional
        )

        val checkPredeterminada = vista.findViewById<CheckBox>(
            R.id.checkPredeterminada
        )

        // CARGAR INFORMACIÓN AL EDITAR

        if (direccionExistente != null) {

            etAlias.setText(direccionExistente.alias)
            etNombre.setText(direccionExistente.nombre)
            etTelefono.setText(direccionExistente.telefono)
            etDepartamento.setText(direccionExistente.departamento)
            etCiudad.setText(direccionExistente.ciudad)
            etDireccion.setText(direccionExistente.direccion)

            etAdicional.setText(
                direccionExistente.informacionAdicional
            )

            checkPredeterminada.isChecked =
                direccionExistente.predeterminada
        }

        val titulo = if (direccionExistente == null) {
            "Agregar dirección"
        } else {
            "Editar dirección"
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(vista)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar dirección", null)
            .create()

        dialogo.setOnShowListener {

            val botonGuardar =
                dialogo.getButton(AlertDialog.BUTTON_POSITIVE)

            val botonCancelar =
                dialogo.getButton(AlertDialog.BUTTON_NEGATIVE)

            botonGuardar.setTextColor(Color.WHITE)

            botonGuardar.backgroundTintList =
                ColorStateList.valueOf(azul)

            botonCancelar.setTextColor(azul)

            botonGuardar.setOnClickListener {

                val alias = etAlias.text.toString().trim()
                val nombre = etNombre.text.toString().trim()
                val telefono = etTelefono.text.toString().trim()
                val departamento =
                    etDepartamento.text.toString().trim()
                val ciudad = etCiudad.text.toString().trim()
                val direccionCompleta =
                    etDireccion.text.toString().trim()
                val adicional =
                    etAdicional.text.toString().trim()

                if (
                    alias.isBlank() ||
                    nombre.isBlank() ||
                    telefono.isBlank() ||
                    departamento.isBlank() ||
                    ciudad.isBlank() ||
                    direccionCompleta.isBlank()
                ) {

                    Toast.makeText(
                        this,
                        "Completa todos los campos obligatorios.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }

                val nuevaDireccion = DireccionEntrega(
                    id = direccionExistente?.id ?: 0L,
                    alias = alias,
                    nombre = nombre,
                    telefono = telefono,
                    departamento = departamento,
                    ciudad = ciudad,
                    direccion = direccionCompleta,
                    informacionAdicional = adicional,
                    predeterminada = checkPredeterminada.isChecked
                )

                if (direccionExistente == null) {

                    val resultado = direccionDAO.guardarDireccion(
                        documentoUsuario,
                        nuevaDireccion
                    )

                    if (resultado > 0L) {
                        direccionGuardada(dialogo)
                    } else {
                        mostrarMensajeError()
                    }

                } else {

                    val resultado = direccionDAO.actualizarDireccion(
                        documentoUsuario,
                        nuevaDireccion
                    )

                    if (resultado) {
                        direccionGuardada(dialogo)
                    } else {
                        mostrarMensajeError()
                    }
                }
            }
        }

        dialogo.show()
    }

    // DIRECCIÓN GUARDADA

    private fun direccionGuardada(dialogo: AlertDialog) {

        Toast.makeText(
            this,
            "Dirección guardada correctamente.",
            Toast.LENGTH_SHORT
        ).show()

        dialogo.dismiss()
        cargarDirecciones()
    }

    private fun mostrarMensajeError() {

        Toast.makeText(
            this,
            "No fue posible guardar la dirección.",
            Toast.LENGTH_LONG
        ).show()
    }

    // ESTABLECER DIRECCIÓN PREDETERMINADA

    private fun establecerPredeterminada(
        direccion: DireccionEntrega
    ) {

        val resultado =
            direccionDAO.establecerDireccionPredeterminada(
                documentoUsuario,
                direccion.id
            )

        if (resultado) {

            Toast.makeText(
                this,
                "Dirección predeterminada actualizada.",
                Toast.LENGTH_SHORT
            ).show()

            cargarDirecciones()

        } else {

            Toast.makeText(
                this,
                "No fue posible actualizar la dirección.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ELIMINAR DIRECCIÓN

    private fun confirmarEliminacion(
        direccion: DireccionEntrega
    ) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar dirección")
            .setMessage(
                "¿Estás seguro de eliminar la dirección ${direccion.alias}?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                val resultado = direccionDAO.eliminarDireccion(
                    documentoUsuario,
                    direccion.id
                )

                if (resultado) {

                    Toast.makeText(
                        this,
                        "Dirección eliminada correctamente.",
                        Toast.LENGTH_SHORT
                    ).show()

                    cargarDirecciones()

                } else {

                    Toast.makeText(
                        this,
                        "No fue posible eliminar la dirección.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    // CERRAR DAO

    override fun onDestroy() {
        super.onDestroy()
    }
}
