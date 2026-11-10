package com.techmarket.app.database

object DatabaseContract {

    // ==========================================
    // USUARIOS
    // ==========================================

    object Usuarios {
        const val TABLA = "usuarios"

        const val ID = "id"
        const val TIPO_DOCUMENTO = "tipo_documento"
        const val DOCUMENTO = "documento"
        const val NOMBRES = "nombres"
        const val APELLIDOS = "apellidos"
        const val CORREO = "correo"
        const val TELEFONO = "telefono"
        const val PASSWORD_HASH = "password_hash"
        const val ROL = "rol"
        const val ESTADO = "estado"
    }

    // ==========================================
    // CATEGORÍAS
    // ==========================================

    object Categorias {
        const val TABLA = "categorias"

        const val ID = "id_categoria"
        const val NOMBRE = "nombre"
        const val DESCRIPCION = "descripcion"
        const val ICONO = "icono"
        const val ESTADO = "estado"
    }

    // ==========================================
    // PRODUCTOS DE TIENDA
    // ==========================================

    object ProductosTienda {
        const val TABLA = "productos_tienda"

        const val ID = "id_producto"
        const val ID_TIENDA = "id_tienda"
        const val NOMBRE = "nombre"
        const val CATEGORIA = "categoria"
        const val PRECIO = "precio"
        const val STOCK = "stock"
        const val DESCRIPCION = "descripcion"
        const val DISPONIBLE = "disponible"
        const val IMAGEN_PRINCIPAL = "imagen_principal"
        const val IMAGEN_2 = "imagen_2"
        const val IMAGEN_3 = "imagen_3"
        const val IMAGEN_4 = "imagen_4"
        const val FECHA_REGISTRO = "fecha_registro"
    }

    // ==========================================
    // TIENDAS
    // ==========================================

    object Tiendas {
        const val TABLA = "tiendas"

        const val ID = "id_tienda"
        const val ID_USUARIO = "id_usuario"
        const val NOMBRE = "nombre_tienda"
        const val DESCRIPCION = "descripcion"
        const val LOGO = "logo"
        const val DIRECCION = "direccion"
        const val SITIO_WEB_REDES = "sitio_web_redes"
        const val ESTADO = "estado"
    }

    // ==========================================
    // PEDIDOS
    // ==========================================

    object Pedidos {
        const val TABLA = "pedidos"

        const val ID = "id"
        const val NUMERO_PEDIDO = "numero_pedido"
        const val DIRECCION = "direccion"
        const val METODO_PAGO = "metodo_pago"
        const val SUBTOTAL = "subtotal"
        const val ENVIO = "envio"
        const val TOTAL = "total"
        const val FECHA = "fecha"
        const val ESTADO = "estado"
    }

    // ==========================================
    // PRODUCTOS DEL PEDIDO
    // ==========================================

    object PedidoProductos {
        const val TABLA = "pedido_productos"

        const val ID = "id"
        const val NUMERO_PEDIDO = "numero_pedido"
        const val NOMBRE_PRODUCTO = "nombre_producto"
        const val PRECIO_PRODUCTO = "precio_producto"
        const val CALIFICACION = "calificacion"
        const val IMAGEN_RES_ID = "imagen_res_id"
        const val DESCRIPCION = "descripcion"
        const val ACTIVO = "activo"
        const val PUBLICADO = "publicado"
        const val CANTIDAD = "cantidad"

        const val ID_PRODUCTO = "id_producto"
        const val ID_TIENDA = "id_tienda"
    }

    // ==========================================
    // DIRECCIONES DE ENTREGA
    // ==========================================

    object Direcciones {
        const val TABLA = "direcciones_entrega"

        const val ID = "id_direccion"
        const val DOCUMENTO_USUARIO = "documento_usuario"
        const val ALIAS = "alias"
        const val NOMBRE_DESTINATARIO = "nombre_destinatario"
        const val TELEFONO = "telefono"
        const val DEPARTAMENTO = "departamento"
        const val CIUDAD = "ciudad"
        const val DIRECCION_COMPLETA = "direccion_completa"
        const val INFORMACION_ADICIONAL = "informacion_adicional"
        const val PREDETERMINADA = "predeterminada"
    }

    // ==========================================
    // MÉTODOS DE PAGO
    // ==========================================

    object MetodosPago {
        const val TABLA = "metodos_pago"

        const val ID = "id_metodo"
        const val DOCUMENTO_USUARIO = "documento_usuario"
        const val TITULAR = "titular"
        const val ULTIMOS_CUATRO = "ultimos_cuatro"
        const val VENCIMIENTO = "vencimiento"
        const val PREDETERMINADO = "predeterminado"
    }

    // ==========================================
    // NOTIFICACIONES
    // ==========================================

    object Notificaciones {
        const val TABLA = "notificaciones"

        const val ID = "id_notificacion"
        const val DOCUMENTO_USUARIO = "documento_usuario"
        const val TITULO = "titulo"
        const val MENSAJE = "mensaje"
        const val FECHA = "fecha"
        const val LEIDA = "leida"
    }
}
