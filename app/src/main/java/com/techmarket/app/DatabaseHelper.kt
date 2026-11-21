package com.techmarket.app

import android.content.ContentValues
import android.content.Context
import android.util.Base64
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.techmarket.app.database.DatabaseContract
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "techmarket.db"
        private const val DATABASE_VERSION = 19
    }

    // ==========================================
    // CONFIGURACIÓN DE BASE DE DATOS
    // ==========================================

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        crearAdministradorInicial(db)
    }

    // ==========================================
    // CREACIÓN INICIAL DE LA BASE DE DATOS
    // ==========================================

    override fun onCreate(db: SQLiteDatabase) {
        crearTablaUsuarios(db)
        crearTablasPedidos(db)
        crearTablaPedidosPagos(db)
        crearTablaDirecciones(db)
        crearTablaMetodosPago(db)
        crearTablaNotificaciones(db)
        crearTablaTiendas(db)
        crearTablaProductosTienda(db)
        crearTablaCategorias(db)
        migrarCategoriasExistentes(db)
        crearTablaPedidosTiendas(db)
        crearTablaHistorialEstadosPedidos(db)

        // Crear la cuenta inicial de administrador en instalaciones nuevas.
        crearAdministradorInicial(db)
    }

    // ==========================================
    // TABLA DE USUARIOS
    // ==========================================

    private fun crearTablaUsuarios(db: SQLiteDatabase) {
        val u = DatabaseContract.Usuarios

        db.execSQL(
            """
            CREATE TABLE ${u.TABLA} (
                ${u.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${u.TIPO_DOCUMENTO} TEXT NOT NULL,
                ${u.DOCUMENTO} TEXT NOT NULL UNIQUE,
                ${u.NOMBRES} TEXT NOT NULL,
                ${u.APELLIDOS} TEXT NOT NULL,
                ${u.CORREO} TEXT NOT NULL,
                ${u.TELEFONO} TEXT NOT NULL DEFAULT '',
                ${u.PASSWORD_HASH} TEXT NOT NULL,
                ${u.ROL} TEXT NOT NULL DEFAULT 'CLIENTE',
                ${u.ESTADO} INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_usuarios_documento
            ON ${u.TABLA} (${u.DOCUMENTO})
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLAS DE PEDIDOS
    // ==========================================

    private fun crearTablasPedidos(db: SQLiteDatabase) {
        val p = DatabaseContract.Pedidos
        val pp = DatabaseContract.PedidoProductos

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${p.TABLA} (
                ${p.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${p.NUMERO_PEDIDO} TEXT NOT NULL UNIQUE,
                ${p.DIRECCION} TEXT NOT NULL,
                ${p.METODO_PAGO} TEXT NOT NULL,
                ${p.SUBTOTAL} INTEGER NOT NULL,
                ${p.ENVIO} INTEGER NOT NULL,
                ${p.TOTAL} INTEGER NOT NULL,
                ${p.FECHA} TEXT NOT NULL,
                ${p.ESTADO} TEXT NOT NULL DEFAULT 'En proceso',
                documento_comprador TEXT,
                ${p.LATITUD} REAL,
                ${p.LONGITUD} REAL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${pp.TABLA} (
                ${pp.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${pp.NUMERO_PEDIDO} TEXT NOT NULL,
                ${pp.ID_PRODUCTO} INTEGER,
                ${pp.ID_TIENDA} INTEGER,
                ${pp.NOMBRE_PRODUCTO} TEXT NOT NULL,
                ${pp.PRECIO_PRODUCTO} TEXT NOT NULL,
                ${pp.CALIFICACION} TEXT NOT NULL,
                ${pp.IMAGEN_RES_ID} INTEGER NOT NULL,
                ${pp.DESCRIPCION} TEXT NOT NULL,
                ${pp.ACTIVO} INTEGER NOT NULL,
                ${pp.PUBLICADO} INTEGER NOT NULL,
                ${pp.CANTIDAD} INTEGER NOT NULL,
                FOREIGN KEY (${pp.NUMERO_PEDIDO})
                    REFERENCES ${p.TABLA}(${p.NUMERO_PEDIDO})
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_pedidos_numero
            ON ${p.TABLA} (${p.NUMERO_PEDIDO})
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_pedido_producto_id
            ON ${pp.TABLA} (${pp.ID_PRODUCTO})
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_pedido_tienda
            ON ${pp.TABLA} (${pp.ID_TIENDA})
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE DIRECCIONES
    // ==========================================

    private fun crearTablaDirecciones(db: SQLiteDatabase) {
        val d = DatabaseContract.Direcciones
        val u = DatabaseContract.Usuarios

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${d.TABLA} (
                ${d.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${d.DOCUMENTO_USUARIO} TEXT NOT NULL,
                ${d.ALIAS} TEXT NOT NULL,
                ${d.NOMBRE_DESTINATARIO} TEXT NOT NULL,
                ${d.TELEFONO} TEXT NOT NULL,
                ${d.DEPARTAMENTO} TEXT NOT NULL,
                ${d.CIUDAD} TEXT NOT NULL,
                ${d.DIRECCION_COMPLETA} TEXT NOT NULL,
                ${d.INFORMACION_ADICIONAL} TEXT,
                ${d.PREDETERMINADA} INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (${d.DOCUMENTO_USUARIO})
                    REFERENCES ${u.TABLA}(${u.DOCUMENTO})
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_direcciones_usuario
            ON ${d.TABLA} (${d.DOCUMENTO_USUARIO})
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE MÉTODOS DE PAGO
    // ==========================================

    private fun crearTablaMetodosPago(db: SQLiteDatabase) {
        val m = DatabaseContract.MetodosPago
        val u = DatabaseContract.Usuarios

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${m.TABLA} (
                ${m.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${m.DOCUMENTO_USUARIO} TEXT NOT NULL,
                ${m.TITULAR} TEXT NOT NULL,
                ${m.ULTIMOS_CUATRO} TEXT NOT NULL,
                ${m.VENCIMIENTO} TEXT NOT NULL,
                ${m.PREDETERMINADO} INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (${m.DOCUMENTO_USUARIO})
                    REFERENCES ${u.TABLA}(${u.DOCUMENTO})
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_metodos_pago_usuario
            ON ${m.TABLA} (${m.DOCUMENTO_USUARIO})
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE NOTIFICACIONES
    // ==========================================

    private fun crearTablaNotificaciones(db: SQLiteDatabase) {
        val n = DatabaseContract.Notificaciones
        val u = DatabaseContract.Usuarios

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${n.TABLA} (
                ${n.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${n.DOCUMENTO_USUARIO} TEXT NOT NULL,
                ${n.TITULO} TEXT NOT NULL,
                ${n.MENSAJE} TEXT NOT NULL,
                ${n.FECHA} TEXT NOT NULL,
                ${n.LEIDA} INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (${n.DOCUMENTO_USUARIO})
                    REFERENCES ${u.TABLA}(${u.DOCUMENTO})
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario
            ON ${n.TABLA} (${n.DOCUMENTO_USUARIO})
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE TIENDAS
    // ==========================================

    private fun crearTablaTiendas(db: SQLiteDatabase) {
        val t = DatabaseContract.Tiendas
        val u = DatabaseContract.Usuarios

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${t.TABLA} (
                ${t.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${t.ID_USUARIO} INTEGER NOT NULL,
                ${t.NOMBRE} TEXT NOT NULL,
                ${t.DESCRIPCION} TEXT NOT NULL,
                ${t.LOGO} TEXT,
                ${t.DIRECCION} TEXT,
                ${t.SITIO_WEB_REDES} TEXT,
                ${t.ESTADO} INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (${t.ID_USUARIO})
                    REFERENCES ${u.TABLA}(${u.ID})
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_tiendas_usuario
            ON ${t.TABLA} (${t.ID_USUARIO})
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE PRODUCTOS DE TIENDA
    // ==========================================

    private fun crearTablaProductosTienda(db: SQLiteDatabase) {
        val pt = DatabaseContract.ProductosTienda
        val t = DatabaseContract.Tiendas

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${pt.TABLA} (
                ${pt.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${pt.ID_TIENDA} INTEGER NOT NULL,
                ${pt.NOMBRE} TEXT NOT NULL,
                ${pt.CATEGORIA} TEXT NOT NULL,
                ${pt.PRECIO} REAL NOT NULL,
                ${pt.STOCK} INTEGER NOT NULL DEFAULT 0,
                ${pt.DESCRIPCION} TEXT NOT NULL,
                ${pt.DISPONIBLE} INTEGER NOT NULL DEFAULT 1,
                ${pt.IMAGEN_PRINCIPAL} TEXT,
                ${pt.IMAGEN_2} TEXT,
                ${pt.IMAGEN_3} TEXT,
                ${pt.IMAGEN_4} TEXT,
                ${pt.FECHA_REGISTRO} TEXT NOT NULL,
                FOREIGN KEY (${pt.ID_TIENDA})
                    REFERENCES ${t.TABLA}(${t.ID})
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_productos_tienda
            ON ${pt.TABLA} (${pt.ID_TIENDA})
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE CATEGORÍAS
    // ==========================================

    private fun crearTablaCategorias(db: SQLiteDatabase) {
        val c = DatabaseContract.Categorias

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${c.TABLA} (
                ${c.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${c.NOMBRE} TEXT NOT NULL,
                ${c.DESCRIPCION} TEXT NOT NULL DEFAULT '',
                ${c.ICONO} TEXT NOT NULL DEFAULT '📦',
                ${c.ESTADO} INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_categorias_nombre
            ON ${c.TABLA} (${c.NOMBRE} COLLATE NOCASE)
            """.trimIndent()
        )
    }

    /**
     * Conserva las categorías que ya están registradas como texto
     * en productos_tienda y las incorpora al catálogo administrable.
     */
    private fun migrarCategoriasExistentes(db: SQLiteDatabase) {
        val c = DatabaseContract.Categorias
        val p = DatabaseContract.ProductosTienda

        db.execSQL(
            """
            INSERT OR IGNORE INTO ${c.TABLA} (${c.NOMBRE})
            SELECT DISTINCT TRIM(${p.CATEGORIA})
            FROM ${p.TABLA}
            WHERE ${p.CATEGORIA} IS NOT NULL
              AND TRIM(${p.CATEGORIA}) != ''
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE ESTADOS DE PEDIDOS POR TIENDA
    // ==========================================

    private fun crearTablaPedidosTiendas(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pedido_tiendas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_pedido TEXT NOT NULL,
                id_tienda INTEGER NOT NULL,
                estado TEXT NOT NULL DEFAULT 'Pendiente de aceptación',
                fecha_actualizacion TEXT NOT NULL,

                UNIQUE(numero_pedido, id_tienda),

                FOREIGN KEY (numero_pedido)
                    REFERENCES pedidos(numero_pedido)
                    ON DELETE CASCADE,

                FOREIGN KEY (id_tienda)
                    REFERENCES tiendas(id_tienda)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_pedido_tiendas_numero
            ON pedido_tiendas(numero_pedido)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_pedido_tiendas_tienda
            ON pedido_tiendas(id_tienda)
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE HISTORIAL DE ESTADOS
    // ==========================================

    private fun crearTablaHistorialEstadosPedidos(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pedido_historial_estados (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_pedido TEXT NOT NULL,
                id_tienda INTEGER NOT NULL,
                estado_anterior TEXT,
                estado_nuevo TEXT NOT NULL,
                fecha_hora TEXT NOT NULL,
                responsable TEXT NOT NULL,
                motivo TEXT,

                FOREIGN KEY (numero_pedido)
                    REFERENCES pedidos(numero_pedido)
                    ON DELETE CASCADE,

                FOREIGN KEY (id_tienda)
                    REFERENCES tiendas(id_tienda)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_historial_pedido_tienda
            ON pedido_historial_estados(numero_pedido, id_tienda)
            """.trimIndent()
        )
    }

    // ==========================================
    // TABLA DE PAGOS DE PEDIDOS
    // ==========================================

    private fun crearTablaPedidosPagos(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pedido_pagos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numero_pedido TEXT NOT NULL,
                proveedor TEXT NOT NULL DEFAULT 'MERCADO_PAGO',
                estado TEXT NOT NULL DEFAULT 'PENDIENTE',
                preference_id TEXT,
                payment_id TEXT,
                external_reference TEXT NOT NULL,
                monto INTEGER NOT NULL,
                moneda TEXT NOT NULL DEFAULT 'COP',
                init_point TEXT,
                sandbox_init_point TEXT,
                fecha_creacion TEXT NOT NULL,
                fecha_actualizacion TEXT NOT NULL,

                FOREIGN KEY (numero_pedido)
                    REFERENCES pedidos(numero_pedido)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_pedido_pagos_numero
            ON pedido_pagos(numero_pedido)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_pedido_pagos_preference
            ON pedido_pagos(preference_id)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS idx_pedido_pagos_payment
            ON pedido_pagos(payment_id)
            """.trimIndent()
        )
    }

    // ==========================================
    // CUENTA INICIAL DE ADMINISTRADOR
    // ==========================================

    private fun crearAdministradorInicial(db: SQLiteDatabase) {
        val u = DatabaseContract.Usuarios

        // No modifica ninguna cuenta existente ni duplica el documento.
        val valores = ContentValues().apply {
            put(u.TIPO_DOCUMENTO, "Cédula de ciudadanía")
            put(u.DOCUMENTO, "123456789")
            put(u.NOMBRES, "Usuario")
            put(u.APELLIDOS, "Prueba Administrador")
            put(u.CORREO, "pruebaAdmi@gmail.com")
            put(u.TELEFONO, "31111111111111")
            put(u.PASSWORD_HASH, generarHashAdministrador())
            put(u.ROL, "ADMINISTRADOR")
            put(u.ESTADO, 1)
        }

        db.insertWithOnConflict(
            u.TABLA,
            null,
            valores,
            SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    /**
     * Utiliza exactamente el formato y parámetros de MainActivity:
     * PBKDF2WithHmacSHA256, 120000 iteraciones, salt de 16 bytes y 256 bits.
     */
    private fun generarHashAdministrador(): String {
        val contrasena = "123456789Prueba"
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val iteraciones = 120000
        val especificacion = PBEKeySpec(
            contrasena.toCharArray(),
            salt,
            iteraciones,
            256
        )

        return try {
            val factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            )
            val hash = factory.generateSecret(especificacion).encoded

            val saltCodificado = Base64.encodeToString(
                salt,
                Base64.NO_WRAP
            )
            val hashCodificado = Base64.encodeToString(
                hash,
                Base64.NO_WRAP
            )

            "PBKDF2WithHmacSHA256:$iteraciones:$saltCodificado:$hashCodificado"
        } finally {
            especificacion.clearPassword()
        }
    }

    // ==========================================
    // MIGRACIONES DE BASE DE DATOS
    // ==========================================

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        val u = DatabaseContract.Usuarios
        val pp = DatabaseContract.PedidoProductos

        if (oldVersion < 2) {
            db.execSQL(
                """
                ALTER TABLE ${u.TABLA}
                ADD COLUMN ${u.TIPO_DOCUMENTO}
                TEXT NOT NULL DEFAULT 'No especificado'
                """.trimIndent()
            )
        }

        if (oldVersion < 3) {
            crearTablasPedidos(db)
        }

        if (oldVersion < 4) {
            crearTablaDirecciones(db)
        }

        if (oldVersion < 5) {
            crearTablaMetodosPago(db)
        }

        if (oldVersion < 6) {
            crearTablaNotificaciones(db)
        }

        if (oldVersion < 7) {
            db.execSQL(
                """
                ALTER TABLE ${u.TABLA}
                ADD COLUMN ${u.TELEFONO}
                TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )

            crearTablaTiendas(db)
        }

        if (oldVersion < 8) {
            crearTablaProductosTienda(db)
        }

        if (oldVersion < 9) {
            db.execSQL(
                """
                ALTER TABLE ${pp.TABLA}
                ADD COLUMN ${pp.ID_PRODUCTO} INTEGER
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_pedido_producto_id
                ON ${pp.TABLA} (${pp.ID_PRODUCTO})
                """.trimIndent()
            )
        }

        if (oldVersion < 10) {
            db.execSQL(
                """
                ALTER TABLE ${pp.TABLA}
                ADD COLUMN ${pp.ID_TIENDA} INTEGER
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_pedido_tienda
                ON ${pp.TABLA} (${pp.ID_TIENDA})
                """.trimIndent()
            )
        }

        if (oldVersion < 11) {

            crearTablaPedidosTiendas(db)
            crearTablaHistorialEstadosPedidos(db)

            db.execSQL(
                """
                INSERT OR IGNORE INTO pedido_tiendas (
                    numero_pedido,
                    id_tienda,
                    estado,
                    fecha_actualizacion
                )
                SELECT DISTINCT
                    pp.numero_pedido,
                    pp.id_tienda,
                    'Pendiente de aceptación',
                    p.fecha
                FROM pedido_productos pp
                INNER JOIN pedidos p
                    ON p.numero_pedido = pp.numero_pedido
                WHERE pp.id_tienda IS NOT NULL
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO pedido_historial_estados (
                    numero_pedido,
                    id_tienda,
                    estado_anterior,
                    estado_nuevo,
                    fecha_hora,
                    responsable,
                    motivo
                )
                SELECT
                    numero_pedido,
                    id_tienda,
                    NULL,
                    estado,
                    fecha_actualizacion,
                    'Sistema',
                    'Estado inicial generado durante la migración'
                FROM pedido_tiendas
                """.trimIndent()
            )
        }

        if (oldVersion < 12) {
            db.execSQL(
                """
                ALTER TABLE ${DatabaseContract.Pedidos.TABLA}
                ADD COLUMN documento_comprador TEXT
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS idx_pedidos_comprador
                ON ${DatabaseContract.Pedidos.TABLA} (documento_comprador)
                """.trimIndent()
            )
        }

        if (oldVersion < 13) {
            crearAdministradorInicial(db)
        }

        // ==========================================
        // VERSIÓN 14: ESTADO DE LAS TIENDAS
        // ==========================================

        if (oldVersion < 14) {
            db.execSQL(
                """
                ALTER TABLE ${DatabaseContract.Tiendas.TABLA}
                ADD COLUMN ${DatabaseContract.Tiendas.ESTADO}
                INTEGER NOT NULL DEFAULT 1
                """.trimIndent()
            )
        }

        // ==========================================
        // VERSIÓN 15: CATÁLOGO DE CATEGORÍAS
        // ==========================================

        if (oldVersion < 15) {
            crearTablaCategorias(db)
            migrarCategoriasExistentes(db)
        }

        // VERSIÓN 16: ICONO PERSONALIZABLE DE CADA CATEGORÍA
        if (oldVersion < 16) {
            val columnaIconoExiste = db.rawQuery(
                "PRAGMA table_info(${DatabaseContract.Categorias.TABLA})",
                null
            ).use { cursor ->
                val indiceNombre = cursor.getColumnIndex("name")
                var existe = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(indiceNombre) == DatabaseContract.Categorias.ICONO) {
                        existe = true
                        break
                    }
                }
                existe
            }

            if (!columnaIconoExiste) {
                db.execSQL(
                    """
                    ALTER TABLE ${DatabaseContract.Categorias.TABLA}
                    ADD COLUMN ${DatabaseContract.Categorias.ICONO}
                    TEXT NOT NULL DEFAULT '📦'
                    """.trimIndent()
                )
            }
        }

        // ==========================================
        // VERSIÓN 17: COORDENADAS DE UBICACIÓN DE PEDIDOS
        // ==========================================

        if (oldVersion < 17) {
            db.execSQL(
                """
                ALTER TABLE ${DatabaseContract.Pedidos.TABLA}
                ADD COLUMN ${DatabaseContract.Pedidos.LATITUD} REAL
                """.trimIndent()
            )

            db.execSQL(
                """
                ALTER TABLE ${DatabaseContract.Pedidos.TABLA}
                ADD COLUMN ${DatabaseContract.Pedidos.LONGITUD} REAL
                """.trimIndent()
            )
        }

        // ==========================================
        // VERSIÓN 18: REGISTRO DE PAGOS
        // ==========================================
        if (oldVersion < 18) {
            crearTablaPedidosPagos(db)
        }

        // ==========================================
        // VERSIÓN 19: UN VENDEDOR PUEDE TENER VARIAS TIENDAS
        // ==========================================
        if (oldVersion < 19) {
            migrarTiendasVendedorMultiple(db)
        }
    }

    /**
     * Elimina la restricción UNIQUE de tiendas.id_usuario sin perder
     * tiendas, productos, pedidos por tienda ni su historial.
     * Las tablas dependientes se respaldan dentro de la misma transacción
     * de onUpgrade, se recrean y se restauran antes de finalizar.
     */
    private fun migrarTiendasVendedorMultiple(db: SQLiteDatabase) {
        db.execSQL("CREATE TEMP TABLE respaldo_productos_tienda AS SELECT * FROM productos_tienda")
        db.execSQL("CREATE TEMP TABLE respaldo_pedido_tiendas AS SELECT * FROM pedido_tiendas")
        db.execSQL("CREATE TEMP TABLE respaldo_pedido_historial_estados AS SELECT * FROM pedido_historial_estados")

        // Eliminar primero las tablas hijas evita que ON DELETE CASCADE
        // borre registros relacionados al reemplazar la tabla tiendas.
        db.execSQL("DROP TABLE productos_tienda")
        db.execSQL("DROP TABLE pedido_tiendas")
        db.execSQL("DROP TABLE pedido_historial_estados")

        db.execSQL(
            """
            CREATE TABLE tiendas_nueva (
                id_tienda INTEGER PRIMARY KEY AUTOINCREMENT,
                id_usuario INTEGER NOT NULL,
                nombre_tienda TEXT NOT NULL,
                descripcion TEXT NOT NULL,
                logo TEXT,
                direccion TEXT,
                sitio_web_redes TEXT,
                estado INTEGER NOT NULL DEFAULT 1,
                FOREIGN KEY (id_usuario)
                    REFERENCES usuarios(id_usuario)
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO tiendas_nueva (
                id_tienda, id_usuario, nombre_tienda, descripcion,
                logo, direccion, sitio_web_redes, estado
            )
            SELECT
                id_tienda, id_usuario, nombre_tienda, descripcion,
                logo, direccion, sitio_web_redes, estado
            FROM tiendas
            """.trimIndent()
        )

        db.execSQL("DROP TABLE tiendas")
        db.execSQL("ALTER TABLE tiendas_nueva RENAME TO tiendas")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_tiendas_usuario ON tiendas(id_usuario)")

        // Recrear estructura e índices originales y recuperar todos los registros.
        crearTablaProductosTienda(db)
        crearTablaPedidosTiendas(db)
        crearTablaHistorialEstadosPedidos(db)

        db.execSQL("INSERT INTO productos_tienda SELECT * FROM respaldo_productos_tienda")
        db.execSQL("INSERT INTO pedido_tiendas SELECT * FROM respaldo_pedido_tiendas")
        db.execSQL("INSERT INTO pedido_historial_estados SELECT * FROM respaldo_pedido_historial_estados")

        db.execSQL("DROP TABLE respaldo_productos_tienda")
        db.execSQL("DROP TABLE respaldo_pedido_tiendas")
        db.execSQL("DROP TABLE respaldo_pedido_historial_estados")
    }

    // ==========================================
    // CONTROL DE VERSIONES
    // ==========================================

    override fun onDowngrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        super.onDowngrade(db, oldVersion, newVersion)
    }
}