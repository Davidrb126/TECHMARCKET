package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PasarelaPagoActivity : AppCompatActivity() {

    private lateinit var rgMetodosPago: RadioGroup
    private lateinit var btnContinuar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasarela_pago)

        inicializarComponentes()
        configurarEventos()
    }

    private fun inicializarComponentes() {

        rgMetodosPago = findViewById(R.id.rgMetodosPago)
        btnContinuar = findViewById(R.id.btnContinuarPago)
    }

    private fun configurarEventos() {

        findViewById<ImageButton>(R.id.btnVolverPago)
            .setOnClickListener {
                finish()
            }

        btnContinuar.setOnClickListener {

            val metodoSeleccionado = when (
                rgMetodosPago.checkedRadioButtonId
            ) {

                R.id.rbTarjeta ->
                    "Tarjeta de crédito o débito"

                R.id.rbPSE ->
                    "PSE"

                R.id.rbContraEntrega ->
                    "Pago contra entrega"

                else -> null
            }

            if (metodoSeleccionado == null) {

                Toast.makeText(
                    this,
                    "Selecciona un método de pago para continuar.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Guardar el método de pago seleccionado.
            MetodoPagoManager.guardar(metodoSeleccionado)

            // Abrir la pantalla de finalización.
            val intent = Intent(
                this,
                FinalizarCompraActivity::class.java
            )

            startActivity(intent)
        }
    }
}

// Almacenamiento temporal del método de pago.
object MetodoPagoManager {

    private var metodoSeleccionado: String? = null

    fun guardar(metodo: String) {
        metodoSeleccionado = metodo
    }

    fun obtener(): String? {
        return metodoSeleccionado
    }

    fun limpiar() {
        metodoSeleccionado = null
    }
}
