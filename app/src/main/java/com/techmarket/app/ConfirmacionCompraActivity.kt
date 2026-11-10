package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConfirmacionCompraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_compra)

        // Inicializar SQLite y el administrador de pedidos.
        PedidoManager.inicializar(this)

        // Consultar el último pedido registrado.
        val pedido = PedidoManager.obtenerUltimoPedido()

        if (pedido == null) {
            Toast.makeText(
                this,
                "No se encontró la información del pedido.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // Mostrar el número del pedido.
        findViewById<TextView>(R.id.tvNumeroPedidoConfirmacion).text =
            "Tu pedido #${pedido.numeroPedido} ha sido registrado."

        // Botón para volver al inicio.
        findViewById<ImageButton>(R.id.btnVolverConfirmacion)
            .setOnClickListener {
                irAlInicio()
            }

        // Botón para seguir comprando.
        findViewById<Button>(R.id.btnSeguirComprando)
            .setOnClickListener {
                irAlInicio()
            }

        // Botón para consultar el historial de compras.
        findViewById<Button>(R.id.btnVerMisCompras)
            .setOnClickListener {
                verMisCompras()
            }
    }

    private fun verMisCompras() {

        // Inicializar el administrador antes de abrir el historial.
        PedidoManager.inicializar(this)

        val intent = Intent(
            this,
            HistorialComprasActivity::class.java
        )

        startActivity(intent)

        finish()
    }

    private fun irAlInicio() {

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
}
