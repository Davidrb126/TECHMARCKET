package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductoDAO(context: Context) {

    private val dbLectura =
        DatabaseProvider.obtenerLectura(context)

    private val dbEscritura =
        DatabaseProvider.obtenerEscritura(context)

    private object Tabla {

        const val NOMBRE = DatabaseContract.ProductosTienda.TABLA

        const val ID = DatabaseContract.ProductosTienda.ID
        const val ID_TIENDA = DatabaseContract.ProductosTienda.ID_TIENDA
        const val NOMBRE_PRODUCTO = DatabaseContract.ProductosTienda.NOMBRE
        const val CATEGORIA = DatabaseContract.ProductosTienda.CATEGORIA
        const val PRECIO = DatabaseContract.ProductosTienda.PRECIO
        const val STOCK = DatabaseContract.ProductosTienda.STOCK
        const val DESCRIPCION = DatabaseContract.ProductosTienda.DESCRIPCION
        const val DISPONIBLE = DatabaseContract.ProductosTienda.DISPONIBLE
        const val IMAGEN_PRINCIPAL = DatabaseContract.ProductosTienda.IMAGEN_PRINCIPAL
        const val IMAGEN_2 = DatabaseContract.ProductosTienda.IMAGEN_2
        const val IMAGEN_3 = DatabaseContract.ProductosTienda.IMAGEN_3
        const val IMAGEN_4 = DatabaseContract.ProductosTienda.IMAGEN_4
        const val FECHA = DatabaseContract.ProductosTienda.FECHA_REGISTRO
    }

    // ==========================================
    // GUARDAR PRODUCTO EN LA TIENDA
    // ==========================================

    fun guardarProductoTienda(
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
    ): Long {

        if (
            idTienda <= 0 ||
            nombre.isBlank() ||
            categoria.isBlank() ||
            precio <= 0.0 ||
            stock < 0 ||
            descripcion.isBlank() ||
            imagenPrincipal.isNullOrBlank()
        ) {
            return -1L
        }

        val valores = ContentValues().apply {
            put(Tabla.ID_TIENDA, idTienda)
            put(Tabla.NOMBRE_PRODUCTO, nombre.trim())
            put(Tabla.CATEGORIA, categoria.trim())
            put(Tabla.PRECIO, precio)
            put(Tabla.STOCK, stock)
            put(Tabla.DESCRIPCION, descripcion.trim())
            put(Tabla.DISPONIBLE, if (disponible) 1 else 0)
            put(Tabla.IMAGEN_PRINCIPAL, imagenPrincipal)
            put(Tabla.IMAGEN_2, imagen2)
            put(Tabla.IMAGEN_3, imagen3)
            put(Tabla.IMAGEN_4, imagen4)

            put(
                Tabla.FECHA,
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())
            )
        }

        return try {
            dbEscritura.insertOrThrow(
                Tabla.NOMBRE,
                null,
                valores
            )
        } catch (_: SQLiteConstraintException) {
            -1L
        } catch (_: Exception) {
            -1L
        }
    }

    // ==========================================
    // OBTENER PRODUCTOS DE UNA TIENDA
    // ==========================================

    fun obtenerProductosTienda(idTienda: Long): Cursor {

        return dbLectura.query(
            Tabla.NOMBRE,
            null,
            "${Tabla.ID_TIENDA} = ?",
            arrayOf(idTienda.toString()),
            null,
            null,
            "${Tabla.ID} DESC"
        )
    }

    // ==========================================
    // OBTENER PRODUCTOS DISPONIBLES DE TODAS
    // LAS TIENDAS
    // ==========================================

    fun obtenerProductosDisponibles(): Cursor {

        return dbLectura.query(
            Tabla.NOMBRE,
            null,
            "${Tabla.DISPONIBLE} = ? AND ${Tabla.STOCK} > ?",
            arrayOf("1", "0"),
            null,
            null,
            "${Tabla.FECHA} DESC"
        )
    }

    // ==========================================
    // CONSULTAR PRODUCTO POR ID
    // ==========================================

    fun consultarProductoTienda(idProducto: Long): Cursor {

        return dbLectura.query(
            Tabla.NOMBRE,
            null,
            "${Tabla.ID} = ?",
            arrayOf(idProducto.toString()),
            null,
            null,
            null
        )
    }

    // ==========================================
    // CONTAR PRODUCTOS DE UNA TIENDA
    // ==========================================

    fun contarProductosTienda(idTienda: Long): Int {

        if (idTienda <= 0) return 0

        val cursor = dbLectura.rawQuery(
            """
            SELECT COUNT(*)
            FROM ${Tabla.NOMBRE}
            WHERE ${Tabla.ID_TIENDA} = ?
            """.trimIndent(),
            arrayOf(idTienda.toString())
        )

        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    // ==========================================
    // ACTUALIZAR PRODUCTO DE UNA TIENDA
    // ==========================================

    fun actualizarProductoTienda(
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
    ): Boolean {

        if (
            idProducto <= 0 ||
            idTienda <= 0 ||
            nombre.isBlank() ||
            categoria.isBlank() ||
            precio <= 0.0 ||
            stock < 0 ||
            descripcion.isBlank() ||
            imagenPrincipal.isNullOrBlank()
        ) {
            return false
        }

        val valores = ContentValues().apply {
            put(Tabla.NOMBRE_PRODUCTO, nombre.trim())
            put(Tabla.CATEGORIA, categoria.trim())
            put(Tabla.PRECIO, precio)
            put(Tabla.STOCK, stock)
            put(Tabla.DESCRIPCION, descripcion.trim())
            put(Tabla.DISPONIBLE, if (disponible) 1 else 0)
            put(Tabla.IMAGEN_PRINCIPAL, imagenPrincipal)
            put(Tabla.IMAGEN_2, imagen2)
            put(Tabla.IMAGEN_3, imagen3)
            put(Tabla.IMAGEN_4, imagen4)
        }

        return try {
            dbEscritura.update(
                Tabla.NOMBRE,
                valores,
                "${Tabla.ID} = ? AND ${Tabla.ID_TIENDA} = ?",
                arrayOf(
                    idProducto.toString(),
                    idTienda.toString()
                )
            ) > 0
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // CAMBIAR DISPONIBILIDAD DEL PRODUCTO
    // ==========================================

    fun actualizarDisponibilidadProducto(
        idProducto: Long,
        idTienda: Long,
        disponible: Boolean
    ): Boolean {

        if (idProducto <= 0 || idTienda <= 0) {
            return false
        }

        val valores = ContentValues().apply {
            put(
                Tabla.DISPONIBLE,
                if (disponible) 1 else 0
            )
        }

        return try {
            dbEscritura.update(
                Tabla.NOMBRE,
                valores,
                "${Tabla.ID} = ? AND ${Tabla.ID_TIENDA} = ?",
                arrayOf(
                    idProducto.toString(),
                    idTienda.toString()
                )
            ) > 0
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // ELIMINAR PRODUCTO DE UNA TIENDA
    // ==========================================

    fun eliminarProductoTienda(
        idProducto: Long,
        idTienda: Long
    ): Boolean {

        if (idProducto <= 0 || idTienda <= 0) {
            return false
        }

        return try {
            dbEscritura.delete(
                Tabla.NOMBRE,
                "${Tabla.ID} = ? AND ${Tabla.ID_TIENDA} = ?",
                arrayOf(
                    idProducto.toString(),
                    idTienda.toString()
                )
            ) > 0
        } catch (_: Exception) {
            false
        }
    }
}
