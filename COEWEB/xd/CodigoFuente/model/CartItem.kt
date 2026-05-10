package com.example.odooapp.model

/**
 * Representa un elemento dentro del carrito de compras.
 * Vincula un producto con la cantidad seleccionada por el usuario.
 */
data class CartItem(
    val product: Product, // El producto base
    var quantity: Int     // La cantidad (mutable para poder cambiarla fácilmente)
) {
    // Cálculo automático del subtotal sin impuestos
    val subtotal: Double get() = product.price * quantity
    
    // Cálculo del impuesto total basado en el porcentaje del producto
    val totalTax: Double get() = subtotal * product.tax / 100.0
    
    // Total final incluyendo el impuesto
    val subtotalWithTax: Double get() = subtotal + totalTax
}
