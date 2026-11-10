package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException

class CategoriaDAO(context: Context) {

    private val dbLectura = DatabaseProvider.obtenerLectura(context)
    private val dbEscritura = DatabaseProvider.obtenerEscritura(context)

    data class Categoria(
        val id: Long,
        val nombre: String,
        val descripcion: String,
        val icono: String,
        val estado: Boolean,
        val cantidadProductos: Int
    )

    // ==========================================
    // OBTENER CATEGORÍAS
    // ==========================================

    fun obtenerCategorias(
        incluirInactivas: Boolean = true
    ): List<Categoria> {

        val c = DatabaseContract.Categorias

        val condicion = if (incluirInactivas) {
            null
        } else {
            "${c.ESTADO} = 1"
        }

        val resultado = mutableListOf<Categoria>()

        dbLectura.query(
            c.TABLA,
            arrayOf(
                c.ID,
                c.NOMBRE,
                c.DESCRIPCION,
                c.ICONO,
                c.ESTADO
            ),
            condicion,
            null,
            null,
            null,
            "${c.NOMBRE} COLLATE NOCASE ASC"
        ).use { cursor ->

            while (cursor.moveToNext()) {

                val nombre = cursor.getString(
                    cursor.getColumnIndexOrThrow(c.NOMBRE)
                )

                resultado.add(
                    Categoria(
                        id = cursor.getLong(
                            cursor.getColumnIndexOrThrow(c.ID)
                        ),
                        nombre = nombre,
                        descripcion = cursor.getString(
                            cursor.getColumnIndexOrThrow(c.DESCRIPCION)
                        ).orEmpty(),
                        icono = cursor.getString(
                            cursor.getColumnIndexOrThrow(c.ICONO)
                        ).orEmpty().ifBlank { "📦" },
                        estado = cursor.getInt(
                            cursor.getColumnIndexOrThrow(c.ESTADO)
                        ) == 1,
                        cantidadProductos = contarProductosPorCategoria(nombre)
                    )
                )
            }
        }

        return resultado
    }

    // ==========================================
    // OBTENER CATEGORÍA POR ID
    // ==========================================

    fun obtenerCategoriaPorId(
        idCategoria: Long
    ): Categoria? {

        if (idCategoria <= 0) return null

        val c = DatabaseContract.Categorias

        dbLectura.query(
            c.TABLA,
            arrayOf(
                c.ID,
                c.NOMBRE,
                c.DESCRIPCION,
                c.ICONO,
                c.ESTADO
            ),
            "${c.ID} = ?",
            arrayOf(idCategoria.toString()),
            null,
            null,
            null
        ).use { cursor ->

            if (!cursor.moveToFirst()) return null

            val nombre = cursor.getString(
                cursor.getColumnIndexOrThrow(c.NOMBRE)
            )

            return Categoria(
                id = cursor.getLong(
                    cursor.getColumnIndexOrThrow(c.ID)
                ),
                nombre = nombre,
                descripcion = cursor.getString(
                    cursor.getColumnIndexOrThrow(c.DESCRIPCION)
                ).orEmpty(),
                icono = cursor.getString(
                    cursor.getColumnIndexOrThrow(c.ICONO)
                ).orEmpty().ifBlank { "📦" },
                estado = cursor.getInt(
                    cursor.getColumnIndexOrThrow(c.ESTADO)
                ) == 1,
                cantidadProductos = contarProductosPorCategoria(nombre)
            )
        }
    }

    // ==========================================
    // INSERTAR CATEGORÍA
    // ==========================================

    fun insertarCategoria(
        nombre: String,
        descripcion: String = "",
        icono: String = "📦"
    ): Long {

        val limpio = nombre.trim()

        if (limpio.isBlank()) return -1L

        if (existeNombreCategoria(limpio)) return -1L

        val c = DatabaseContract.Categorias

        val valores = ContentValues().apply {
            put(c.NOMBRE, limpio)
            put(c.DESCRIPCION, descripcion.trim())
            put(c.ICONO, icono.ifBlank { "📦" })
            put(c.ESTADO, 1)
        }

        return try {

            dbEscritura.insertOrThrow(
                c.TABLA,
                null,
                valores
            )

        } catch (e: SQLiteConstraintException) {
            -1L
        }
    }

    // ==========================================
    // ACTUALIZAR CATEGORÍA
    // ==========================================

    fun actualizarCategoria(
        idCategoria: Long,
        nuevoNombre: String,
        nuevaDescripcion: String = "",
        nuevoIcono: String = "📦"
    ): Boolean {

        val limpio = nuevoNombre.trim()

        if (idCategoria <= 0 || limpio.isBlank()) {
            return false
        }

        val actual = obtenerCategoriaPorId(idCategoria)
            ?: return false

        if (
            !actual.nombre.equals(limpio, ignoreCase = true) &&
            existeNombreCategoria(limpio, idCategoria)
        ) {
            return false
        }

        val c = DatabaseContract.Categorias
        val p = DatabaseContract.ProductosTienda

        val valores = ContentValues().apply {
            put(c.NOMBRE, limpio)
            put(c.DESCRIPCION, nuevaDescripcion.trim())
            put(c.ICONO, nuevoIcono.ifBlank { "📦" })
        }

        dbEscritura.beginTransaction()

        return try {

            val filas = dbEscritura.update(
                c.TABLA,
                valores,
                "${c.ID} = ?",
                arrayOf(idCategoria.toString())
            )

            // Actualizar la categoría de los productos
            // asociados si cambió el nombre.

            if (filas > 0 && actual.nombre != limpio) {

                val valoresProducto = ContentValues().apply {
                    put(p.CATEGORIA, limpio)
                }

                dbEscritura.update(
                    p.TABLA,
                    valoresProducto,
                    "TRIM(${p.CATEGORIA}) = ?",
                    arrayOf(actual.nombre)
                )
            }

            dbEscritura.setTransactionSuccessful()

            filas > 0

        } catch (e: SQLiteConstraintException) {

            false

        } finally {

            dbEscritura.endTransaction()
        }
    }

    // ==========================================
    // ACTIVAR / DESACTIVAR
    // ==========================================

    fun cambiarEstadoCategoria(
        idCategoria: Long,
        activa: Boolean
    ): Boolean {

        if (idCategoria <= 0) return false

        val c = DatabaseContract.Categorias

        val valores = ContentValues().apply {
            put(c.ESTADO, if (activa) 1 else 0)
        }

        return dbEscritura.update(
            c.TABLA,
            valores,
            "${c.ID} = ?",
            arrayOf(idCategoria.toString())
        ) > 0
    }

    // ==========================================
    // VALIDAR NOMBRE DUPLICADO
    // ==========================================

    fun existeNombreCategoria(
        nombre: String,
        excluirId: Long? = null
    ): Boolean {

        val limpio = nombre.trim()

        if (limpio.isBlank()) return false

        val c = DatabaseContract.Categorias

        val where = if (excluirId == null) {
            "${c.NOMBRE} = ? COLLATE NOCASE"
        } else {
            "${c.NOMBRE} = ? COLLATE NOCASE AND ${c.ID} != ?"
        }

        val args = if (excluirId == null) {
            arrayOf(limpio)
        } else {
            arrayOf(limpio, excluirId.toString())
        }

        dbLectura.query(
            c.TABLA,
            arrayOf(c.ID),
            where,
            args,
            null,
            null,
            null,
            "1"
        ).use { cursor ->

            return cursor.moveToFirst()
        }
    }

    // ==========================================
    // CONTAR PRODUCTOS ASOCIADOS
    // ==========================================

    fun contarProductosPorCategoria(
        nombreCategoria: String
    ): Int {

        val p = DatabaseContract.ProductosTienda

        dbLectura.rawQuery(
            """
            SELECT COUNT(*)
            FROM ${p.TABLA}
            WHERE TRIM(${p.CATEGORIA}) = ?
            """.trimIndent(),
            arrayOf(nombreCategoria.trim())
        ).use { cursor ->

            return if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }
}