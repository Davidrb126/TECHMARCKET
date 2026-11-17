package com.techmarket.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FirebaseTestActivity : AppCompatActivity() {

    private lateinit var tvResultado: TextView
    private lateinit var btnGuardar: Button
    private lateinit var btnLeer: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firebase_test)

        tvResultado = findViewById(R.id.tvResultadoFirebase)
        btnGuardar = findViewById(R.id.btnGuardarFirebase)
        btnLeer = findViewById(R.id.btnLeerFirebase)

        btnGuardar.setOnClickListener {
            guardarEnFirebase()
        }

        btnLeer.setOnClickListener {
            leerDesdeFirebase()
        }
    }

    private fun guardarEnFirebase() {
        tvResultado.text = "Guardando información en Firebase..."

        FirebaseManager.guardarDocumentoPrueba(
            onSuccess = { mensaje ->
                tvResultado.text = "ÉXITO\n\n$mensaje"
            },
            onError = { error ->
                tvResultado.text =
                    "ERROR AL GUARDAR\n\n${error.localizedMessage}"
            }
        )
    }

    private fun leerDesdeFirebase() {
        tvResultado.text = "Consultando información en Firebase..."

        FirebaseManager.leerDocumentoPrueba(
            onSuccess = { datos ->
                if (datos != null) {
                    val mensaje = datos["mensaje"] ?: "Sin mensaje"
                    val origen = datos["origen"] ?: "Sin origen"

                    tvResultado.text =
                        "LECTURA EXITOSA\n\n" +
                                "Mensaje: $mensaje\n\n" +
                                "Origen: $origen"
                } else {
                    tvResultado.text =
                        "No existe el documento. Primero debes guardarlo."
                }
            },
            onError = { error ->
                tvResultado.text =
                    "ERROR AL LEER\n\n${error.localizedMessage}"
            }
        )
    }
}
