package com.techmarket.app

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

object FirebaseManager {

    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    // ==========================================
    // PRUEBA DE CONEXIÓN CON FIRESTORE
    // ==========================================

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

    // ==========================================
    // SINCRONIZAR USUARIO CON FIRESTORE
    // ==========================================

    fun sincronizarUsuario(
        tipoDocumento: String,
        documento: String,
        nombres: String,
        apellidos: String,
        correo: String,
        rol: String,
        estado: Int
    ): Task<Void> {

        val datos = hashMapOf<String, Any>(
            "tipoDocumento" to tipoDocumento,
            "documento" to documento,
            "nombres" to nombres,
            "apellidos" to apellidos,
            "correo" to correo,
            "rol" to rol,
            "estado" to estado,
            "fechaActualizacion" to Timestamp.now()
        )

        return db.collection("usuarios")
            .document(documento)
            .set(datos, SetOptions.merge())
    }

    // ==========================================
    // ELIMINAR USUARIO DE FIRESTORE
    // ==========================================

    fun eliminarUsuarioFirestore(
        documento: String
    ): Task<Void> {

        return db.collection("usuarios")
            .document(documento)
            .delete()
    }

    // ==========================================
    // SUBIR IMAGEN DE PRODUCTO A STORAGE
    // ==========================================

    fun subirImagenProducto(
        uri: Uri,
        idTienda: Long,
        idProducto: Long,
        numeroImagen: Int
    ): Task<Uri> {

        val referencia = storage.reference
            .child("productos")
            .child(idTienda.toString())
            .child(idProducto.toString())
            .child("imagen_$numeroImagen.jpg")

        return referencia.putFile(uri)
            .continueWithTask { tarea ->

                if (!tarea.isSuccessful) {
                    throw tarea.exception
                        ?: Exception("No fue posible subir la imagen.")
                }

                referencia.downloadUrl
            }
    }

    // ==========================================
    // SINCRONIZAR PRODUCTO CON FIRESTORE
    // ==========================================

    fun sincronizarProducto(
        idProducto: Long,
        idTienda: Long,
        nombre: String,
        categoria: String,
        precio: Double,
        stock: Int,
        descripcion: String,
        disponible: Boolean,
        imagenPrincipal: String?,
        imagen2: String?,
        imagen3: String?,
        imagen4: String?
    ): Task<Void> {

        val datos = hashMapOf<String, Any>(
            "idProducto" to idProducto,
            "idTienda" to idTienda,
            "nombre" to nombre,
            "categoria" to categoria,
            "precio" to precio,
            "stock" to stock,
            "descripcion" to descripcion,
            "disponible" to disponible,
            "fechaActualizacion" to Timestamp.now()
        )

        imagenPrincipal?.let {
            datos["imagenPrincipal"] = it
        }

        imagen2?.let {
            datos["imagen2"] = it
        }

        imagen3?.let {
            datos["imagen3"] = it
        }

        imagen4?.let {
            datos["imagen4"] = it
        }

        val idDocumento = "${idTienda}_$idProducto"

        return db.collection("productos")
            .document(idDocumento)
            .set(datos, SetOptions.merge())
    }

    // ==========================================
    // ELIMINAR PRODUCTO DE FIRESTORE
    // ==========================================

    fun eliminarProductoFirestore(
        idTienda: Long,
        idProducto: Long
    ): Task<Void> {

        val idDocumento = "${idTienda}_$idProducto"

        return db.collection("productos")
            .document(idDocumento)
            .delete()
    }

    // ==========================================
    // ELIMINAR IMAGEN DE STORAGE
    // ==========================================

    fun eliminarImagenProducto(
        idTienda: Long,
        idProducto: Long,
        numeroImagen: Int
    ): Task<Void> {

        val referencia = storage.reference
            .child("productos")
            .child(idTienda.toString())
            .child(idProducto.toString())
            .child("imagen_$numeroImagen.jpg")

        return referencia.delete()
    }
}
