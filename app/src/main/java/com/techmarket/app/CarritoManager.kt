package com.techmarket.app

data class CarritoItem(
    val producto: Producto,
    var cantidad: Int
)

object CarritoManager {

    private val productos = mutableListOf<CarritoItem>()

    // Identificar productos mediante su ID.
    // Si ambos productos no tienen ID (productos de demostración),
    // se utiliza el nombre como alternativa.
    private fun esMismoProducto(
        productoA: Producto,
        productoB: Producto
    ): Boolean {
        return if (
            productoA.idProducto != null &&
            productoB.idProducto != null
        ) {
            productoA.idProducto == productoB.idProducto
        } else {
            productoA.idProducto == null &&
                    productoB.idProducto == null &&
                    productoA.nombre == productoB.nombre
        }
    }

    // Agregar un producto o acumular su cantidad.
    fun agregar(producto: Producto, cantidad: Int) {

        if (cantidad <= 0) return

        val existente = productos.find {
            esMismoProducto(it.producto, producto)
        }

        if (existente != null) {
            existente.cantidad += cantidad
        } else {
            productos.add(
                CarritoItem(
                    producto = producto,
                    cantidad = cantidad
                )
            )
        }
    }

    // Obtener los productos del carrito.
    fun obtenerProductos(): List<CarritoItem> {
        return productos.toList()
    }

    // Cambiar la cantidad utilizando el producto.
    fun actualizarCantidad(producto: Producto, cantidad: Int) {

        val item = productos.find {
            esMismoProducto(it.producto, producto)
        }

        if (item != null) {
            if (cantidad <= 0) {
                productos.remove(item)
            } else {
                item.cantidad = cantidad
            }
        }
    }

    // Eliminar un producto utilizando su identificación.
    fun eliminar(producto: Producto) {
        productos.removeAll {
            esMismoProducto(it.producto, producto)
        }
    }

    // Vaciar el carrito.
    fun vaciar() {
        productos.clear()
    }

    // Cantidad total de unidades.
    fun obtenerCantidadTotal(): Int {
        return productos.sumOf { it.cantidad }
    }

    // Calcular subtotal.
    fun obtenerSubtotal(): Long {
        return productos.sumOf { item ->
            val precio = item.producto.precio
                .filter { it.isDigit() }
                .toLongOrNull() ?: 0L

            precio * item.cantidad
        }
    }
}
