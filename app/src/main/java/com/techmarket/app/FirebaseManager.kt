package com.techmarket.app

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {

    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    fun guardarDocumentoPrueba(
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val datos = hashMapOf(
            "mensaje" to "Conexion de prueba desde TechMarket",
            "origen" to "Android",
            "fecha" to Timestamp.now()
        )

        db.collection("pruebas_conexion")
            .document("prueba_android")
            .set(datos)
            .addOnSuccessListener {
                onSuccess("Documento guardado correctamente")
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }

    fun leerDocumentoPrueba(
        onSuccess: (Map<String, Any>?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("pruebas_conexion")
            .document("prueba_android")
            .get()
            .addOnSuccessListener { documento ->
                onSuccess(
                    if (documento.exists()) documento.data else null
                )
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }
}
