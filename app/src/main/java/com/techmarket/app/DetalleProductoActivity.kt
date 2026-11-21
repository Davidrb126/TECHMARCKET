package com.techmarket.app

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class DetalleProductoActivity : AppCompatActivity() {

    private lateinit var btnVolver: ImageButton
    private lateinit var btnCarrito: ImageButton
    private lateinit var btnDisminuir: ImageButton
    private lateinit var btnAumentar: ImageButton
    private lateinit var btnAgregarCarrito: Button

    private lateinit var tvCantidad: TextView
    private lateinit var tvNombre: TextView
    private lateinit var tvPrecio: TextView
    private lateinit var tvCalificacion: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var imgProducto: ImageView

    private var cantidad = 1

    // Datos del producto seleccionado
    private var nombre = "Producto"
    private var precio = "$ 0"
    private var calificacion = "Sin calificación"
    private var descripcion = "Sin descripción disponible."
    private var imagenResId = android.R.drawable.ic_menu_gallery
    private var idProducto: Long? = null
    private var imagenUri: String? = null

    // Galería de imágenes del producto
    private val imagenesProducto = mutableListOf<String>()
    private var indiceImagenActual = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_producto)

        inicializarComponentes()
        recibirDatosProducto()
        configurarEventos()
        actualizarCantidad()
    }

    private fun inicializarComponentes() {

        btnVolver = findViewById(R.id.btnVolverDetalle)
        btnCarrito = findViewById(R.id.btnCarritoDetalle)
        btnDisminuir = findViewById(R.id.btnDisminuirCantidad)
        btnAumentar = findViewById(R.id.btnAumentarCantidad)
        btnAgregarCarrito = findViewById(R.id.btnAgregarAlCarrito)

        tvCantidad = findViewById(R.id.tvCantidad)
        tvNombre = findViewById(R.id.tvNombreDetalle)
        tvPrecio = findViewById(R.id.tvPrecioDetalle)
        tvCalificacion = findViewById(R.id.tvCalificacionDetalle)
        tvDescripcion = findViewById(R.id.tvDescripcionDetalle)
        imgProducto = findViewById(R.id.imgDetalleProducto)
    }

    // ==========================================
    // RECIBIR DATOS DEL PRODUCTO
    // ==========================================

    private fun recibirDatosProducto() {

        nombre = intent.getStringExtra("nombre") ?: "Producto"

        precio = intent.getStringExtra("precio") ?: "$ 0"

        calificacion = intent.getStringExtra("calificacion")
            ?: "Sin calificación"

        descripcion = intent.getStringExtra("descripcion")
            ?: "Sin descripción disponible."

        imagenResId = intent.getIntExtra(
            "imagenResId",
            android.R.drawable.ic_menu_gallery
        )

        idProducto = if (intent.hasExtra("idProducto")) {
            intent.getLongExtra("idProducto", -1L)
                .takeIf { it != -1L }
        } else {
            null
        }

        imagenUri = intent.getStringExtra("imagenUri")

        tvNombre.text = nombre
        tvPrecio.text = precio
        tvCalificacion.text = calificacion
        tvDescripcion.text = descripcion

        // Cargar las imágenes disponibles en el orden correspondiente.
        imagenesProducto.clear()

        listOf(
            intent.getStringExtra("imagenUri"),
            intent.getStringExtra("imagenUri2"),
            intent.getStringExtra("imagenUri3"),
            intent.getStringExtra("imagenUri4")
        ).forEach { uri ->

            if (!uri.isNullOrBlank() &&
                !imagenesProducto.contains(uri)
            ) {
                imagenesProducto.add(uri)
            }
        }

        indiceImagenActual = 0

        mostrarImagenActual()
    }

    // ==========================================
    // MOSTRAR IMAGEN ACTUAL
    // ==========================================

    private fun mostrarImagenActual() {

        if (imagenesProducto.isEmpty()) {
            imgProducto.setImageResource(imagenResId)
            return
        }

        val imagenActual = imagenesProducto[indiceImagenActual]

        // ImagenesManager permite cargar tanto imágenes locales
        // como URL HTTPS de Cloudinary sin bloquear la pantalla.
        ImagenesManager.cargar(
            vista = imgProducto,
            origen = imagenActual,
            imagenPredeterminada = imagenResId
        )
    }

    // ==========================================
    // CONFIGURAR EVENTOS
    // ==========================================

    private fun configurarEventos() {

        // Volver a la pantalla anterior
        btnVolver.setOnClickListener {
            finish()
        }

        // Abrir pantalla del carrito
        btnCarrito.setOnClickListener {

            val intent = Intent(
                this,
                CarritoActivity::class.java
            )

            startActivity(intent)
        }

        // Deslizar imágenes horizontalmente
        val detectorDeslizamiento = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {

                    if (e1 == null || imagenesProducto.size <= 1) {
                        return false
                    }

                    val diferenciaX = e2.x - e1.x
                    val diferenciaY = e2.y - e1.y

                    if (
                        abs(diferenciaX) > abs(diferenciaY) &&
                        abs(diferenciaX) > 50
                    ) {

                        if (diferenciaX < 0) {

                            // Siguiente imagen
                            if (indiceImagenActual < imagenesProducto.size - 1) {
                                indiceImagenActual++
                                mostrarImagenActual()
                            }

                        } else {

                            // Imagen anterior
                            if (indiceImagenActual > 0) {
                                indiceImagenActual--
                                mostrarImagenActual()
                            }
                        }

                        return true
                    }

                    return false
                }
            }
        )

        imgProducto.setOnTouchListener { _, event ->
            detectorDeslizamiento.onTouchEvent(event)
        }

        // Disminuir cantidad
        btnDisminuir.setOnClickListener {

            if (cantidad > 1) {
                cantidad--
                actualizarCantidad()
            }
        }

        // Aumentar cantidad
        btnAumentar.setOnClickListener {

            cantidad++
            actualizarCantidad()
        }

        // Agregar producto al carrito
        btnAgregarCarrito.setOnClickListener {

            val producto = Producto(
                nombre = nombre,
                precio = precio,
                calificacion = calificacion,
                imagenResId = imagenResId,
                descripcion = descripcion,
                idProducto = idProducto,
                imagenUri = imagenUri
            )

            CarritoManager.agregar(producto, cantidad)

            Toast.makeText(
                this,
                "$nombre agregado al carrito. Cantidad: $cantidad",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }

    // ==========================================
    // ACTUALIZAR CANTIDAD
    // ==========================================

    private fun actualizarCantidad() {
        tvCantidad.text = cantidad.toString()
    }
}
