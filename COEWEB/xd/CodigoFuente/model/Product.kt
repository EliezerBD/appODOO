package com.example.odooapp.model

/**
 * Representa un producto obtenido de Odoo.
 * Esta clase se usa para manejar la información en el carrito y la UI.
 */
data class Product(
    val id: Int,            // ID único en la base de datos de Odoo
    val code: String = "",  // Código de barras o referencia interna
    val name: String,       // Nombre del producto
    val price: Double,      // Precio unitario
    val tax: Double = 0.0   // Porcentaje de impuesto (ej: 0.15 para 15%)
)
