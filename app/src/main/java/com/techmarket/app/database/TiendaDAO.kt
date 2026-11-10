package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException

/**
 * DAO encargado de las operaciones relacionadas
 * con las tiendas de los vendedores de TechMarket.
 */
class TiendaDAO(context: Context) {

    companion object {
        val COL_NOMBRE_TIENDA = DatabaseContract.Tiendas.NOMBRE
        val COL_LOGO_TIENDA = DatabaseContract.Tiendas.LOGO
    }

    private val dbLectura = DatabaseProvider.obtenerLectura(context)
    private val dbEscritura = DatabaseProvider.obtenerEscritura(context)

    private object Tabla {
        const val NOMBRE = DatabaseContract.Tiendas.TABLA
        const val ID = DatabaseContract.Tiendas.ID
        const val ID_USUARIO = DatabaseContract.Tiendas.ID_USUARIO
        const val NOMBRE_TIENDA = DatabaseContract.Tiendas.NOMBRE
        const val DESCRIPCION = DatabaseContract.Tiendas.DESCRIPCION
        const val LOGO = DatabaseContract.Tiendas.LOGO
        const val DIRECCION = DatabaseContract.Tiendas.DIRECCION
        const val SITIO_WEB_REDES = DatabaseContract.Tiendas.SITIO_WEB_REDES
        const val ESTADO = DatabaseContract.Tiendas.ESTADO
    }

    private object Usuario {
        const val NOMBRE = DatabaseContract.Usuarios.TABLA
        const val ID = DatabaseContract.Usuarios.ID
        const val DOCUMENTO = DatabaseContract.Usuarios.DOCUMENTO
        const val NOMBRES = DatabaseContract.Usuarios.NOMBRES
        const val APELLIDOS = DatabaseContract.Usuarios.APELLIDOS
        const val CORREO = DatabaseContract.Usuarios.CORREO
    }

    // ==========================================
    // REGISTRAR UNA TIENDA
    // ==========================================

    fun guardarTienda(
        idUsuario: Long,
        nombreTienda: String,
        descripcion: String,
        logo: String? = null,
        direccion: String? = null,
        sitioWebRedes: String? = null
    ): Long {

        if (
            idUsuario <= 0 ||
            nombreTienda.isBlank() ||
            descripcion.isBlank()
        ) {
            return -1L
        }

        val valores = ContentValues().apply {
            put(Tabla.ID_USUARIO, idUsuario)
            put(Tabla.NOMBRE_TIENDA, nombreTienda.trim())
            put(Tabla.DESCRIPCION, descripcion.trim())

            put(Tabla.LOGO, logo?.trim()?.takeIf { it.isNotEmpty() })
            put(Tabla.DIRECCION, direccion?.trim()?.takeIf { it.isNotEmpty() })
            put(Tabla.SITIO_WEB_REDES, sitioWebRedes?.trim()?.takeIf { it.isNotEmpty() })
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
    // CONSULTAR TIENDA POR USUARIO
    // ==========================================

    fun consultarTiendaPorUsuario(
        idUsuario: Long
    ): Cursor {

        return dbLectura.query(
            Tabla.NOMBRE,
            arrayOf(
                Tabla.ID,
                Tabla.ID_USUARIO,
                Tabla.NOMBRE_TIENDA,
                Tabla.DESCRIPCION,
                Tabla.LOGO,
                Tabla.DIRECCION,
                Tabla.SITIO_WEB_REDES
            ),
            "${Tabla.ID_USUARIO} = ?",
            arrayOf(idUsuario.toString()),
            null,
            null,
            null
        )
    }

    // ==========================================
    // ACTUALIZAR INFORMACIÓN DE UNA TIENDA
    // MÉTODO UTILIZADO POR EL VENDEDOR
    // ==========================================

    fun actualizarTienda(
        idUsuario: Long,
        nombreTienda: String,
        descripcion: String,
        logo: String? = null,
        direccion: String? = null,
        sitioWebRedes: String? = null
    ): Boolean {

        if (
            idUsuario <= 0 ||
            nombreTienda.isBlank() ||
            descripcion.isBlank()
        ) {
            return false
        }

        val valores = ContentValues().apply {
            put(Tabla.NOMBRE_TIENDA, nombreTienda.trim())
            put(Tabla.DESCRIPCION, descripcion.trim())

            put(Tabla.LOGO, logo?.trim()?.takeIf { it.isNotEmpty() })
            put(Tabla.DIRECCION, direccion?.trim()?.takeIf { it.isNotEmpty() })
            put(Tabla.SITIO_WEB_REDES, sitioWebRedes?.trim()?.takeIf { it.isNotEmpty() })
        }

        return try {
            dbEscritura.update(
                Tabla.NOMBRE,
                valores,
                "${Tabla.ID_USUARIO} = ?",
                arrayOf(idUsuario.toString())
            ) > 0
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // ADMINISTRACIÓN: CONSULTAR TODAS LAS TIENDAS
    // INCLUYE INFORMACIÓN DEL VENDEDOR
    // ==========================================

    fun consultarTodasLasTiendas(): Cursor {

        val consulta = """
            SELECT
                t.${Tabla.ID} AS id_tienda,
                t.${Tabla.ID_USUARIO} AS id_usuario,
                t.${Tabla.NOMBRE_TIENDA} AS nombre_tienda,
                t.${Tabla.DESCRIPCION} AS descripcion,
                t.${Tabla.LOGO} AS logo,
                t.${Tabla.DIRECCION} AS direccion,
                t.${Tabla.SITIO_WEB_REDES} AS sitio_web_redes,
                t.${Tabla.ESTADO} AS estado,
                u.${Usuario.DOCUMENTO} AS documento_vendedor,
                u.${Usuario.NOMBRES} AS nombres_vendedor,
                u.${Usuario.APELLIDOS} AS apellidos_vendedor,
                u.${Usuario.CORREO} AS correo_vendedor
            FROM ${Tabla.NOMBRE} t
            INNER JOIN ${Usuario.NOMBRE} u
                ON t.${Tabla.ID_USUARIO} = u.${Usuario.ID}
            ORDER BY t.${Tabla.ID} DESC
        """.trimIndent()

        return dbLectura.rawQuery(consulta, null)
    }

    // ==========================================
    // ADMINISTRACIÓN: CONSULTAR TIENDA POR ID
    // ==========================================

    fun consultarTiendaPorId(idTienda: Long): Cursor {

        val consulta = """
            SELECT
                t.${Tabla.ID} AS id_tienda,
                t.${Tabla.ID_USUARIO} AS id_usuario,
                t.${Tabla.NOMBRE_TIENDA} AS nombre_tienda,
                t.${Tabla.DESCRIPCION} AS descripcion,
                t.${Tabla.LOGO} AS logo,
                t.${Tabla.DIRECCION} AS direccion,
                t.${Tabla.SITIO_WEB_REDES} AS sitio_web_redes,
                t.${Tabla.ESTADO} AS estado,
                u.${Usuario.DOCUMENTO} AS documento_vendedor,
                u.${Usuario.NOMBRES} AS nombres_vendedor,
                u.${Usuario.APELLIDOS} AS apellidos_vendedor,
                u.${Usuario.CORREO} AS correo_vendedor
            FROM ${Tabla.NOMBRE} t
            INNER JOIN ${Usuario.NOMBRE} u
                ON t.${Tabla.ID_USUARIO} = u.${Usuario.ID}
            WHERE t.${Tabla.ID} = ?
        """.trimIndent()

        return dbLectura.rawQuery(
            consulta,
            arrayOf(idTienda.toString())
        )
    }

    // ==========================================
    // ADMINISTRACIÓN: ACTUALIZAR TIENDA POR ID
    // ==========================================

    fun actualizarTiendaPorId(
        idTienda: Long,
        nombreTienda: String,
        descripcion: String,
        logo: String? = null,
        direccion: String? = null,
        sitioWebRedes: String? = null
    ): Boolean {

        if (
            idTienda <= 0 ||
            nombreTienda.isBlank() ||
            descripcion.isBlank()
        ) {
            return false
        }

        val valores = ContentValues().apply {
            put(Tabla.NOMBRE_TIENDA, nombreTienda.trim())
            put(Tabla.DESCRIPCION, descripcion.trim())
            put(Tabla.LOGO, logo?.trim()?.takeIf { it.isNotEmpty() })
            put(Tabla.DIRECCION, direccion?.trim()?.takeIf { it.isNotEmpty() })
            put(Tabla.SITIO_WEB_REDES, sitioWebRedes?.trim()?.takeIf { it.isNotEmpty() })
        }

        return try {
            dbEscritura.update(
                Tabla.NOMBRE,
                valores,
                "${Tabla.ID} = ?",
                arrayOf(idTienda.toString())
            ) > 0
        } catch (_: Exception) {
            false
        }
    }

    // ==========================================
    // ADMINISTRACIÓN: ACTIVAR / DESACTIVAR TIENDA
    // ==========================================

    fun cambiarEstadoTienda(
        idTienda: Long,
        estado: Int
    ): Boolean {

        if (idTienda <= 0 || estado !in 0..1) {
            return false
        }

        val valores = ContentValues().apply {
            put(Tabla.ESTADO, estado)
        }

        return try {
            dbEscritura.update(
                Tabla.NOMBRE,
                valores,
                "${Tabla.ID} = ?",
                arrayOf(idTienda.toString())
            ) > 0
        } catch (_: Exception) {
            false
        }
    }
}
