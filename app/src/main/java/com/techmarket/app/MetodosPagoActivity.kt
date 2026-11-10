package com.techmarket.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmarket.app.database.PagoDAO

class MetodosPagoActivity : AppCompatActivity() {

    private lateinit var pagoDAO: PagoDAO
    private lateinit var documentoUsuario: String

    private lateinit var contenedorMetodosPago: LinearLayout
    private lateinit var contenedorVacioMetodos: View

    // Colores de TechMarket
    private val colorAzul = Color.rgb(23, 43, 91)
    private val colorNegro = Color.rgb(32, 33, 36)
    private val colorGris = Color.rgb(107, 114, 128)
    private val colorVerde = Color.rgb(0, 130, 70)
    private val colorRojo = Color.rgb(210, 55, 55)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metodos_pago)

        pagoDAO = PagoDAO(this)

        documentoUsuario = SessionManager.obtenerDocumento(this) ?: ""

        if (documentoUsuario.isBlank()) {
            Toast.makeText(
                this,
                "No se pudo identificar al usuario. Inicia sesión nuevamente.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        contenedorMetodosPago =
            findViewById(R.id.contenedorMetodosPago)

        contenedorVacioMetodos =
            findViewById(R.id.contenedorVacioMetodos)

        findViewById<TextView>(R.id.btnVolverMetodosPago)
            .setOnClickListener {
                finish()
            }

        findViewById<View>(R.id.btnAgregarMetodoPago)
            .setOnClickListener {
                mostrarDialogoAgregar()
            }

        cargarMetodosPago()
    }

    // Consultar y mostrar los métodos de pago guardados
    private fun cargarMetodosPago() {

        contenedorMetodosPago.removeAllViews()

        val metodos = pagoDAO.obtenerMetodosPago(documentoUsuario)

        contenedorVacioMetodos.visibility =
            if (metodos.isEmpty()) View.VISIBLE else View.GONE

        for (metodo in metodos) {
            agregarTarjetaVisual(metodo)
        }
    }

    // Crear visualmente cada tarjeta guardada
    private fun agregarTarjetaVisual(metodo: MetodoPago) {

        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundResource(R.drawable.bg_tarjeta_direccion)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        val titulo = TextView(this).apply {
            text = "💳  Tarjeta terminada en ${metodo.ultimosCuatro}"
            textSize = 16f
            setTextColor(colorNegro)
            setTypeface(null, Typeface.BOLD)
        }

        val titular = TextView(this).apply {
            text = "Titular: ${metodo.titular}"
            textSize = 14f
            setTextColor(colorGris)
            setPadding(0, dp(12), 0, 0)
        }

        val vencimiento = TextView(this).apply {
            text = "Vencimiento: ${metodo.vencimiento}"
            textSize = 14f
            setTextColor(colorGris)
            setPadding(0, dp(8), 0, 0)
        }

        val estado = TextView(this).apply {

            text = if (metodo.predeterminado) {
                "✓ Método de pago predeterminado"
            } else {
                "Establecer como predeterminado"
            }

            textSize = 14f

            setTextColor(
                if (metodo.predeterminado) {
                    colorVerde
                } else {
                    colorAzul
                }
            )

            setPadding(0, dp(16), 0, 0)

            if (!metodo.predeterminado) {
                setOnClickListener {

                    val actualizado =
                        pagoDAO.establecerMetodoPagoPredeterminado(
                            documentoUsuario,
                            metodo.id
                        )

                    if (actualizado) {

                        Toast.makeText(
                            this@MetodosPagoActivity,
                            "Método predeterminado actualizado",
                            Toast.LENGTH_SHORT
                        ).show()

                        cargarMetodosPago()

                    } else {

                        Toast.makeText(
                            this@MetodosPagoActivity,
                            "No fue posible actualizar el método",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        val eliminar = TextView(this).apply {
            text = "Eliminar método de pago"
            textSize = 14f
            setTextColor(colorRojo)
            setPadding(0, dp(16), 0, 0)

            setOnClickListener {
                confirmarEliminar(metodo)
            }
        }

        tarjeta.addView(titulo)
        tarjeta.addView(titular)
        tarjeta.addView(vencimiento)
        tarjeta.addView(estado)
        tarjeta.addView(eliminar)

        contenedorMetodosPago.addView(tarjeta)
    }

    // Mostrar formulario personalizado para agregar una tarjeta
    private fun mostrarDialogoAgregar() {

        val vista = LayoutInflater.from(this)
            .inflate(R.layout.dialog_metodo_pago, null)

        val etTitular =
            vista.findViewById<EditText>(R.id.etTitularTarjeta)

        val etUltimosCuatro =
            vista.findViewById<EditText>(R.id.etUltimosCuatro)

        val etVencimiento =
            vista.findViewById<EditText>(R.id.etVencimientoTarjeta)

        // Contenedor principal
        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
            }
        }

        // Encabezado
        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titulo = TextView(this).apply {
            text = "Agregar tarjeta"
            textSize = 20f
            setTextColor(colorAzul)
            setTypeface(null, Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val cerrar = TextView(this).apply {
            text = "✕"
            textSize = 20f
            setTextColor(colorGris)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(4), dp(4), dp(4))
            contentDescription = "Cerrar"
        }

        encabezado.addView(titulo)
        encabezado.addView(cerrar)

        // Espacio superior
        val espacioSuperior = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                1,
                dp(18)
            )
        }

        // Botones inferiores
        val botones = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        val btnCancelar = TextView(this).apply {
            text = "Cancelar"
            textSize = 14f
            setTextColor(colorGris)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(13), dp(16), dp(13))
        }

        // Botón Guardar tarjeta
        val btnGuardar = TextView(this).apply {
            text = "Guardar tarjeta"
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(13), dp(18), dp(13))

            background = GradientDrawable().apply {
                setColor(Color.rgb(10, 84, 213))
                cornerRadius = dp(10).toFloat()
            }
        }

        val parametrosCancelar = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = dp(8)
        }

        botones.addView(btnCancelar, parametrosCancelar)
        botones.addView(btnGuardar)

        // Espacio inferior
        val espacioInferior = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                1,
                dp(20)
            )
        }

        // Integrar elementos
        contenedor.addView(encabezado)
        contenedor.addView(espacioSuperior)
        contenedor.addView(vista)
        contenedor.addView(espacioInferior)
        contenedor.addView(botones)

        // Crear diálogo personalizado
        val dialog = AlertDialog.Builder(this)
            .setView(contenedor)
            .create()

        cerrar.setOnClickListener {
            dialog.dismiss()
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {

            val titular = etTitular.text.toString().trim()

            val ultimosCuatro =
                etUltimosCuatro.text.toString().trim()

            val vencimiento =
                etVencimiento.text.toString().trim()

            // Validar información ingresada
            if (
                titular.isEmpty() ||
                ultimosCuatro.length != 4 ||
                !ultimosCuatro.all { it.isDigit() } ||
                !vencimiento.matches(
                    Regex("(0[1-9]|1[0-2])/([0-9]{2})")
                )
            ) {

                Toast.makeText(
                    this@MetodosPagoActivity,
                    "Verifica los datos de la tarjeta",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Crear objeto del método de pago
            val metodo = MetodoPago(
                titular = titular,
                ultimosCuatro = ultimosCuatro,
                vencimiento = vencimiento
            )

            // Guardar mediante PagoDAO
            val resultado = pagoDAO.guardarMetodoPago(
                documentoUsuario,
                metodo
            )

            if (resultado > 0L) {

                Toast.makeText(
                    this@MetodosPagoActivity,
                    "Tarjeta guardada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                cargarMetodosPago()
                dialog.dismiss()

            } else {

                Toast.makeText(
                    this@MetodosPagoActivity,
                    "No fue posible guardar la tarjeta",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(
                android.R.color.transparent
            )

            setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    // Confirmar eliminación de una tarjeta
    private fun confirmarEliminar(metodo: MetodoPago) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar tarjeta")
            .setMessage(
                "¿Deseas eliminar la tarjeta terminada en ${metodo.ultimosCuatro}?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                val resultado = pagoDAO.eliminarMetodoPago(
                    documentoUsuario,
                    metodo.id
                )

                if (resultado) {

                    Toast.makeText(
                        this,
                        "Tarjeta eliminada",
                        Toast.LENGTH_SHORT
                    ).show()

                    cargarMetodosPago()

                } else {

                    Toast.makeText(
                        this,
                        "No fue posible eliminar la tarjeta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    // Conversión de dp a píxeles
    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
