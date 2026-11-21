package com.techmarket.app.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.techmarket.app.Pedido
import com.techmarket.app.Producto
import com.techmarket.app.CarritoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DAO encargado de guardar, consultar y gestionar
 * los pedidos de TechMarket.
 *
 * Los estados se administran independientemente
 * para cada tienda que participa en una compra.
 */
class PedidoDAO(context: Context) {

    private val dbLectura = DatabaseProvider.obtenerLectura(context)
    private val dbEscritura = DatabaseProvider.obtenerEscritura(context)

    companion object {
        private const val COL_ID_TIENDA = "id_tienda"
        private const val COL_DOCUMENTO_COMPRADOR = "documento_comprador"

        private const val TABLA_PEDIDO_TIENDAS = "pedido_tiendas"
        private const val TABLA_HISTORIAL = "pedido_historial_estados"

        const val ESTADO_PENDIENTE = "Pendiente de aceptación"
        const val ESTADO_ACEPTADO = "Aceptado"
        const val ESTADO_PREPARACION = "En preparación"
        const val ESTADO_ENVIADO = "Enviado"
        const val ESTADO_ENTREGADO = "Entregado"
        const val ESTADO_CANCELADO = "Cancelado"

        val ESTADOS_VALIDOS = listOf(
            ESTADO_PENDIENTE,
            ESTADO_ACEPTADO,
            ESTADO_PREPARACION,
            ESTADO_ENVIADO,
            ESTADO_ENTREGADO,
            ESTADO_CANCELADO
        )
    }

    private fun fechaActual(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

    // ==========================================
    // OBTENER TIENDA DE UN PRODUCTO
    // ==========================================

    private fun obtenerIdTiendaProducto(idProducto: Long?): Long? {
        if (idProducto == null || idProducto <= 0) return null

        val cursor = dbEscritura.query(
            DatabaseContract.ProductosTienda.TABLA,
            arrayOf(DatabaseContract.ProductosTienda.ID_TIENDA),
            "${DatabaseContract.ProductosTienda.ID} = ?",
            arrayOf(idProducto.toString()),
            null,
            null,
            null
        )

        cursor.use {
            return if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null
        }
    }

    // ==========================================
    // GUARDAR PEDIDO Y SUS PRODUCTOS
    // ==========================================

    /**
     * Guarda el pedido asociado al documento del comprador autenticado.
     * El documento es obligatorio para evitar pedidos sin propietario.
     */
    fun guardarPedido(pedido: Pedido, documentoComprador: String) {
        require(documentoComprador.isNotBlank()) {
            "El documento del comprador es obligatorio para registrar el pedido."
        }
        require(pedido.numeroPedido.isNotBlank()) {
            "El número del pedido es obligatorio."
        }

        dbEscritura.beginTransaction()

        try {
            val valoresPedido = ContentValues().apply {
                put(DatabaseContract.Pedidos.NUMERO_PEDIDO, pedido.numeroPedido)
                put(DatabaseContract.Pedidos.DIRECCION, pedido.direccion)
                put(DatabaseContract.Pedidos.METODO_PAGO, pedido.metodoPago)
                put(DatabaseContract.Pedidos.SUBTOTAL, pedido.subtotal)
                put(DatabaseContract.Pedidos.ENVIO, pedido.envio)
                put(DatabaseContract.Pedidos.TOTAL, pedido.total)
                put(DatabaseContract.Pedidos.FECHA, pedido.fecha)
                put(DatabaseContract.Pedidos.ESTADO, ESTADO_PENDIENTE)
                put(COL_DOCUMENTO_COMPRADOR, documentoComprador.trim())
                if (pedido.latitud != null) {
                    put(DatabaseContract.Pedidos.LATITUD, pedido.latitud)
                } else {
                    putNull(DatabaseContract.Pedidos.LATITUD)
                }
                if (pedido.longitud != null) {
                    put(DatabaseContract.Pedidos.LONGITUD, pedido.longitud)
                } else {
                    putNull(DatabaseContract.Pedidos.LONGITUD)
                }
            }

            dbEscritura.insertOrThrow(
                DatabaseContract.Pedidos.TABLA,
                null,
                valoresPedido
            )

            // Tiendas que participan en la compra.
            val tiendasPedido = mutableSetOf<Long>()

            for (item in pedido.productos) {
                val producto = item.producto
                val idTienda = obtenerIdTiendaProducto(producto.idProducto)

                val valoresProducto = ContentValues().apply {
                    put(DatabaseContract.PedidoProductos.NUMERO_PEDIDO, pedido.numeroPedido)
                    put(DatabaseContract.PedidoProductos.NOMBRE_PRODUCTO, producto.nombre)
                    put(DatabaseContract.PedidoProductos.PRECIO_PRODUCTO, producto.precio)
                    put(DatabaseContract.PedidoProductos.CALIFICACION, producto.calificacion)
                    put(DatabaseContract.PedidoProductos.IMAGEN_RES_ID, producto.imagenResId)
                    put(DatabaseContract.PedidoProductos.DESCRIPCION, producto.descripcion)
                    put(DatabaseContract.PedidoProductos.ACTIVO, if (producto.activo) 1 else 0)
                    put(DatabaseContract.PedidoProductos.PUBLICADO, if (producto.publicado) 1 else 0)
                    put(DatabaseContract.PedidoProductos.CANTIDAD, item.cantidad)
                    put(DatabaseContract.PedidoProductos.ID_PRODUCTO, producto.idProducto)
                    put(DatabaseContract.PedidoProductos.ID_TIENDA, idTienda)
                }

                dbEscritura.insertOrThrow(
                    DatabaseContract.PedidoProductos.TABLA,
                    null,
                    valoresProducto
                )

                if (idTienda != null && idTienda > 0) {
                    tiendasPedido.add(idTienda)
                }
            }

            // Crear un estado independiente para cada tienda.
            for (idTienda in tiendasPedido) {
                val valoresEstado = ContentValues().apply {
                    put("numero_pedido", pedido.numeroPedido)
                    put(COL_ID_TIENDA, idTienda)
                    put("estado", ESTADO_PENDIENTE)
                    put("fecha_actualizacion", fechaActual())
                }

                dbEscritura.insertOrThrow(
                    TABLA_PEDIDO_TIENDAS,
                    null,
                    valoresEstado
                )

                registrarHistorial(
                    numeroPedido = pedido.numeroPedido,
                    idTienda = idTienda,
                    estadoAnterior = null,
                    estadoNuevo = ESTADO_PENDIENTE,
                    responsable = "Sistema",
                    motivo = "Creación inicial del pedido"
                )
            }

            dbEscritura.setTransactionSuccessful()
        } finally {
            dbEscritura.endTransaction()
        }
    }

    // ==========================================
    // REGISTRAR HISTORIAL
    // ==========================================

    private fun registrarHistorial(
        numeroPedido: String,
        idTienda: Long,
        estadoAnterior: String?,
        estadoNuevo: String,
        responsable: String,
        motivo: String?
    ) {
        val valores = ContentValues().apply {
            put("numero_pedido", numeroPedido)
            put(COL_ID_TIENDA, idTienda)
            put("estado_anterior", estadoAnterior)
            put("estado_nuevo", estadoNuevo)
            put("fecha_hora", fechaActual())
            put("responsable", responsable)
            put("motivo", motivo)
        }

        dbEscritura.insertOrThrow(TABLA_HISTORIAL, null, valores)
    }

    // ==========================================
    // CONSULTAR TODOS LOS PEDIDOS
    // ==========================================

    fun obtenerPedidos(): List<Pedido> {
        val pedidos = mutableListOf<Pedido>()

        val cursor = dbLectura.query(
            DatabaseContract.Pedidos.TABLA,
            null,
            null,
            null,
            null,
            null,
            "${DatabaseContract.Pedidos.ID} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val numeroPedido = it.getString(
                    it.getColumnIndexOrThrow(DatabaseContract.Pedidos.NUMERO_PEDIDO)
                )

                pedidos.add(
                    construirPedido(
                        it,
                        obtenerProductosPedido(numeroPedido)
                    )
                )
            }
        }

        return pedidos
    }

    // ==========================================
    // CONSULTAR PEDIDOS DE UN COMPRADOR
    // ==========================================

    /**
     * Devuelve únicamente los pedidos cuyo documento_comprador
     * coincide con el documento de la sesión activa.
     *
     * Los pedidos históricos cuyo propietario sea NULL no se
     * asignan a ningún comprador por inferencia.
     */
    fun obtenerPedidosPorComprador(documentoComprador: String): List<Pedido> {
        val pedidos = mutableListOf<Pedido>()
        val documento = documentoComprador.trim()

        if (documento.isBlank()) return pedidos

        val cursor = dbLectura.query(
            DatabaseContract.Pedidos.TABLA,
            null,
            "$COL_DOCUMENTO_COMPRADOR = ?",
            arrayOf(documento),
            null,
            null,
            "${DatabaseContract.Pedidos.ID} DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val numeroPedido = it.getString(
                    it.getColumnIndexOrThrow(DatabaseContract.Pedidos.NUMERO_PEDIDO)
                )

                // El comprador ve un estado general calculado a partir
                // del estado individual de todas las tiendas del pedido.
                pedidos.add(
                    construirPedido(
                        it,
                        obtenerProductosPedido(numeroPedido),
                        obtenerEstadoGeneralPedido(numeroPedido)
                    )
                )
            }
        }

        return pedidos
    }    // ==========================================
    // CONSULTAR PEDIDOS DE UNA TIENDA
    // ==========================================

    fun obtenerPedidosPorTienda(idTienda: Long): List<Pedido> {
        val pedidos = mutableListOf<Pedido>()
        if (idTienda <= 0) return pedidos

        val sql = """
            SELECT DISTINCT p.*
            FROM pedidos p
            INNER JOIN pedido_productos pp
                ON pp.numero_pedido = p.numero_pedido
            WHERE pp.id_tienda = ?
            ORDER BY p.id DESC
        """.trimIndent()

        val cursor = dbLectura.rawQuery(sql, arrayOf(idTienda.toString()))

        cursor.use {
            while (it.moveToNext()) {
                val numeroPedido = it.getString(
                    it.getColumnIndexOrThrow(DatabaseContract.Pedidos.NUMERO_PEDIDO)
                )

                val productosTienda = obtenerProductosPedido(numeroPedido, idTienda)

                if (productosTienda.isNotEmpty()) {
                    val estadoTienda = obtenerEstadoPedidoTienda(numeroPedido, idTienda)

                    pedidos.add(
                        construirPedido(
                            it,
                            productosTienda,
                            estadoTienda
                        )
                    )
                }
            }
        }

        return pedidos
    }

    // ==========================================
    // CONTAR PEDIDOS DE UNA TIENDA
    // ==========================================

    fun contarPedidosPorTienda(idTienda: Long): Int {
        if (idTienda <= 0) return 0

        val sql = """
            SELECT COUNT(DISTINCT numero_pedido)
            FROM pedido_productos
            WHERE id_tienda = ?
        """.trimIndent()

        val cursor = dbLectura.rawQuery(sql, arrayOf(idTienda.toString()))

        cursor.use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    // ==========================================
    // CONSULTAR ESTADO DE UNA TIENDA EN UN PEDIDO
    // ==========================================

    fun obtenerEstadoPedidoTienda(numeroPedido: String, idTienda: Long): String {
        val cursor = dbLectura.query(
            TABLA_PEDIDO_TIENDAS,
            arrayOf("estado"),
            "numero_pedido = ? AND $COL_ID_TIENDA = ?",
            arrayOf(numeroPedido, idTienda.toString()),
            null,
            null,
            null
        )

        cursor.use {
            return if (it.moveToFirst()) it.getString(0) else ESTADO_PENDIENTE
        }
    }

    // ==========================================
    // CALCULAR ESTADO GENERAL DEL PEDIDO
    // ==========================================

    /**
     * Calcula el estado que debe visualizar el comprador:
     * - Entregado: todas las tiendas marcaron el pedido como Entregado.
     * - Cancelado: todas las tiendas marcaron el pedido como Cancelado.
     * - En proceso: cualquier otra combinación de estados.
     *
     * Los estados individuales de cada tienda no se modifican.
     */
    private fun obtenerEstadoGeneralPedido(numeroPedido: String): String {
        val estados = mutableListOf<String>()

        val cursor = dbLectura.query(
            TABLA_PEDIDO_TIENDAS,
            arrayOf("estado"),
            "numero_pedido = ?",
            arrayOf(numeroPedido),
            null,
            null,
            null
        )

        cursor.use {
            while (it.moveToNext()) {
                estados.add(it.getString(0))
            }
        }

        if (estados.isEmpty()) {
            return ESTADO_PENDIENTE
        }

        return when {
            estados.all { it == ESTADO_ENTREGADO } -> ESTADO_ENTREGADO
            estados.all { it == ESTADO_CANCELADO } -> ESTADO_CANCELADO
            estados.all { it == ESTADO_ENVIADO || it == ESTADO_ENTREGADO } &&
                    estados.any { it == ESTADO_ENVIADO } -> ESTADO_ENVIADO
            else -> "En proceso"
        }
    }

    // ==========================================
    // VALIDAR TRANSICIONES DE ESTADO
    // ==========================================

    private fun transicionPermitida(
        estadoActual: String,
        estadoNuevo: String
    ): Boolean {
        if (estadoActual == estadoNuevo) return false

        if (estadoActual == ESTADO_ENTREGADO || estadoActual == ESTADO_CANCELADO) {
            return false
        }

        if (estadoNuevo == ESTADO_CANCELADO) {
            return estadoActual == ESTADO_PENDIENTE ||
                    estadoActual == ESTADO_ACEPTADO ||
                    estadoActual == ESTADO_PREPARACION
        }

        return when (estadoActual) {
            ESTADO_PENDIENTE -> estadoNuevo == ESTADO_ACEPTADO
            ESTADO_ACEPTADO -> estadoNuevo == ESTADO_PREPARACION
            ESTADO_PREPARACION -> estadoNuevo == ESTADO_ENVIADO
            ESTADO_ENVIADO -> estadoNuevo == ESTADO_ENTREGADO
            else -> false
        }
    }

    // ==========================================
    // ACTUALIZAR ESTADO DE PEDIDO POR TIENDA
    // ==========================================

    fun cambiarEstadoPedidoTienda(
        numeroPedido: String,
        idTienda: Long,
        estadoNuevo: String,
        responsable: String,
        motivo: String? = null
    ): Boolean {
        if (
            numeroPedido.isBlank() ||
            idTienda <= 0 ||
            responsable.isBlank() ||
            estadoNuevo !in ESTADOS_VALIDOS
        ) {
            return false
        }

        if (estadoNuevo == ESTADO_CANCELADO && motivo.isNullOrBlank()) {
            return false
        }

        dbEscritura.beginTransaction()

        try {
            val cursor = dbEscritura.query(
                TABLA_PEDIDO_TIENDAS,
                arrayOf("estado"),
                "numero_pedido = ? AND $COL_ID_TIENDA = ?",
                arrayOf(numeroPedido, idTienda.toString()),
                null,
                null,
                null
            )

            val estadoActual = cursor.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: return false

            if (!transicionPermitida(estadoActual, estadoNuevo)) {
                return false
            }

            val valores = ContentValues().apply {
                put("estado", estadoNuevo)
                put("fecha_actualizacion", fechaActual())
            }

            val filasActualizadas = dbEscritura.update(
                TABLA_PEDIDO_TIENDAS,
                valores,
                "numero_pedido = ? AND $COL_ID_TIENDA = ?",
                arrayOf(numeroPedido, idTienda.toString())
            )

            if (filasActualizadas != 1) return false

            registrarHistorial(
                numeroPedido = numeroPedido,
                idTienda = idTienda,
                estadoAnterior = estadoActual,
                estadoNuevo = estadoNuevo,
                responsable = responsable,
                motivo = motivo
            )

            dbEscritura.setTransactionSuccessful()
            return true
        } finally {
            dbEscritura.endTransaction()
        }
    }

    // ==========================================
    // CONSULTAR HISTORIAL DE UN PEDIDO
    // ==========================================

    fun obtenerHistorialEstados(
        numeroPedido: String,
        idTienda: Long
    ): List<HistorialEstadoPedido> {
        val historial = mutableListOf<HistorialEstadoPedido>()

        val cursor = dbLectura.query(
            TABLA_HISTORIAL,
            null,
            "numero_pedido = ? AND $COL_ID_TIENDA = ?",
            arrayOf(numeroPedido, idTienda.toString()),
            null,
            null,
            "id ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                val indiceEstadoAnterior = it.getColumnIndexOrThrow("estado_anterior")
                val indiceMotivo = it.getColumnIndexOrThrow("motivo")

                historial.add(
                    HistorialEstadoPedido(
                        estadoAnterior = if (it.isNull(indiceEstadoAnterior)) {
                            null
                        } else {
                            it.getString(indiceEstadoAnterior)
                        },
                        estadoNuevo = it.getString(
                            it.getColumnIndexOrThrow("estado_nuevo")
                        ),
                        fechaHora = it.getString(
                            it.getColumnIndexOrThrow("fecha_hora")
                        ),
                        responsable = it.getString(
                            it.getColumnIndexOrThrow("responsable")
                        ),
                        motivo = if (it.isNull(indiceMotivo)) {
                            null
                        } else {
                            it.getString(indiceMotivo)
                        }
                    )
                )
            }
        }

        return historial
    }    // ==========================================
    // CONSULTAR DETALLE DE PEDIDO DEL COMPRADOR
    // ==========================================

    /**
     * Consulta un pedido únicamente cuando pertenece al documento
     * del comprador autenticado.
     */
    fun obtenerDetallePedido(
        numeroPedido: String,
        documentoComprador: String
    ): Pedido? {
        val numero = numeroPedido.trim()
        val documento = documentoComprador.trim()

        if (numero.isBlank() || documento.isBlank()) return null

        val cursor = dbLectura.query(
            DatabaseContract.Pedidos.TABLA,
            null,
            "${DatabaseContract.Pedidos.NUMERO_PEDIDO} = ? AND $COL_DOCUMENTO_COMPRADOR = ?",
            arrayOf(numero, documento),
            null,
            null,
            null,
            "1"
        )

        cursor.use {
            if (!it.moveToFirst()) return null

            return construirPedido(
                it,
                obtenerProductosPedido(numero),
                obtenerEstadoGeneralPedido(numero)
            )
        }
    }

    // ==========================================
    // CONSULTAR TIENDAS, PRODUCTOS E HISTORIAL
    // DE UN PEDIDO
    // ==========================================

    fun obtenerTiendasPedido(numeroPedido: String): List<DetalleTiendaPedido> {
        val resultado = mutableListOf<DetalleTiendaPedido>()
        val numero = numeroPedido.trim()

        if (numero.isBlank()) return resultado

        val sql = """
            SELECT
                pt.id_tienda,
                COALESCE(t.nombre_tienda, 'Tienda'),
                pt.estado
            FROM $TABLA_PEDIDO_TIENDAS pt
            LEFT JOIN ${DatabaseContract.Tiendas.TABLA} t
                ON t.${DatabaseContract.Tiendas.ID} = pt.id_tienda
            WHERE pt.numero_pedido = ?
            ORDER BY pt.id_tienda ASC
        """.trimIndent()

        val cursor = dbLectura.rawQuery(sql, arrayOf(numero))

        cursor.use {
            while (it.moveToNext()) {
                val idTienda = it.getLong(0)

                resultado.add(
                    DetalleTiendaPedido(
                        idTienda = idTienda,
                        nombreTienda = it.getString(1),
                        estado = it.getString(2),
                        productos = obtenerProductosPedido(numero, idTienda),
                        historial = obtenerHistorialEstados(numero, idTienda)
                    )
                )
            }
        }

        return resultado
    }

    // ==========================================
    // CONSTRUIR OBJETO PEDIDO
    // ==========================================

    private fun construirPedido(
        cursor: Cursor,
        productos: List<CarritoItem>,
        estadoTienda: String? = null
    ): Pedido {
        return Pedido(
            numeroPedido = cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.NUMERO_PEDIDO)
            ),
            productos = productos,
            direccion = cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.DIRECCION)
            ),
            metodoPago = cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.METODO_PAGO)
            ),
            subtotal = cursor.getLong(
                cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.SUBTOTAL)
            ),
            envio = cursor.getLong(
                cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.ENVIO)
            ),
            total = cursor.getLong(
                cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.TOTAL)
            ),
            fecha = cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.FECHA)
            ),
            estado = estadoTienda ?: cursor.getString(
                cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.ESTADO)
            ),
            latitud = cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.LATITUD)
                .let { indice ->
                    if (cursor.isNull(indice)) null else cursor.getDouble(indice)
                },
            longitud = cursor.getColumnIndexOrThrow(DatabaseContract.Pedidos.LONGITUD)
                .let { indice ->
                    if (cursor.isNull(indice)) null else cursor.getDouble(indice)
                }
        )
    }

    // ==========================================
    // CONSULTAR PRODUCTOS DE UN PEDIDO
    // ==========================================

    private fun obtenerProductosPedido(
        numeroPedido: String,
        idTienda: Long? = null
    ): List<CarritoItem> {
        val productos = mutableListOf<CarritoItem>()

        val cursor = if (idTienda == null) {
            dbLectura.query(
                DatabaseContract.PedidoProductos.TABLA,
                null,
                "${DatabaseContract.PedidoProductos.NUMERO_PEDIDO} = ?",
                arrayOf(numeroPedido),
                null,
                null,
                "${DatabaseContract.PedidoProductos.ID} ASC"
            )
        } else {
            dbLectura.query(
                DatabaseContract.PedidoProductos.TABLA,
                null,
                "${DatabaseContract.PedidoProductos.NUMERO_PEDIDO} = ? AND $COL_ID_TIENDA = ?",
                arrayOf(numeroPedido, idTienda.toString()),
                null,
                null,
                "${DatabaseContract.PedidoProductos.ID} ASC"
            )
        }

        cursor.use {
            while (it.moveToNext()) {
                val indiceIdProducto = it.getColumnIndexOrThrow(
                    DatabaseContract.PedidoProductos.ID_PRODUCTO
                )

                val idProducto = if (it.isNull(indiceIdProducto)) {
                    null
                } else {
                    it.getLong(indiceIdProducto)
                }

                val producto = Producto(
                    nombre = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.PedidoProductos.NOMBRE_PRODUCTO
                        )
                    ),
                    precio = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.PedidoProductos.PRECIO_PRODUCTO
                        )
                    ),
                    calificacion = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.PedidoProductos.CALIFICACION
                        )
                    ),
                    imagenResId = it.getInt(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.PedidoProductos.IMAGEN_RES_ID
                        )
                    ),
                    descripcion = it.getString(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.PedidoProductos.DESCRIPCION
                        )
                    ),
                    activo = it.getInt(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.PedidoProductos.ACTIVO
                        )
                    ) == 1,
                    publicado = it.getInt(
                        it.getColumnIndexOrThrow(
                            DatabaseContract.PedidoProductos.PUBLICADO
                        )
                    ) == 1,
                    idProducto = idProducto
                )

                val cantidad = it.getInt(
                    it.getColumnIndexOrThrow(DatabaseContract.PedidoProductos.CANTIDAD)
                )

                productos.add(
                    CarritoItem(
                        producto = producto,
                        cantidad = cantidad
                    )
                )
            }
        }

        return productos
    }

    // ==========================================
    // ESTADÍSTICAS GENERALES PARA EL DASHBOARD
    // ==========================================

    /**
     * Cuenta todos los pedidos registrados.
     * Cada pedido se cuenta una sola vez, aunque participe
     * más de una tienda.
     */
    fun contarComprasTotales(): Int {
        val sql = """
            SELECT COUNT(*)
            FROM ${DatabaseContract.Pedidos.TABLA}
        """.trimIndent()

        dbLectura.rawQuery(sql, null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    /**
     * Suma el total de los pedidos que no están completamente
     * cancelados.
     *
     * Un pedido solo se excluye cuando todas las tiendas
     * participantes tienen el estado "Cancelado".
     *
     * Si el pedido no tiene registros en pedido_tiendas,
     * se conserva en el total, igual que en la lógica del
     * estado general del pedido.
     */
    fun obtenerValorTotalCompras(): Long {
        val sql = """
            SELECT COALESCE(
                SUM(p.${DatabaseContract.Pedidos.TOTAL}),
                0
            )
            FROM ${DatabaseContract.Pedidos.TABLA} p
            WHERE
                NOT EXISTS (
                    SELECT 1
                    FROM $TABLA_PEDIDO_TIENDAS pt
                    WHERE pt.numero_pedido = p.${DatabaseContract.Pedidos.NUMERO_PEDIDO}
                )
                OR EXISTS (
                    SELECT 1
                    FROM $TABLA_PEDIDO_TIENDAS pt
                    WHERE pt.numero_pedido = p.${DatabaseContract.Pedidos.NUMERO_PEDIDO}
                    AND COALESCE(pt.estado, '') != ?
                )
        """.trimIndent()

        dbLectura.rawQuery(
            sql,
            arrayOf(ESTADO_CANCELADO)
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        }
    }

}

// ==========================================
// MODELO DEL HISTORIAL DE ESTADOS
// ==========================================

data class HistorialEstadoPedido(
    val estadoAnterior: String?,
    val estadoNuevo: String,
    val fechaHora: String,
    val responsable: String,
    val motivo: String?
)


/**
 * Información de cada tienda que participa en un pedido,
 * con sus productos y el historial de cambios de estado.
 */
data class DetalleTiendaPedido(
    val idTienda: Long,
    val nombreTienda: String,
    val estado: String,
    val productos: List<CarritoItem>,
    val historial: List<HistorialEstadoPedido>

)
