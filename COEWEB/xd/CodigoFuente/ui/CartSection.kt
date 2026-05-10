package com.example.odooapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odooapp.model.CartItem
import androidx.compose.ui.tooling.preview.Preview
import com.example.odooapp.model.Product

/**
 * Sección del Carrito: Muestra la lista de productos que han sido escaneados.
 * Si no hay productos, muestra un mensaje informativo.
 */
@Composable
fun CartSection(
    cart: List<CartItem>,                // Lista de items actual
    onQuantityChange: (Int, Int) -> Unit, // Callback para cambiar cantidad (índice, delta)
    onRemoveItem: (Int) -> Unit           // Callback para eliminar por índice
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera de la sección
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = FontAwesomeCartShopping,
                    contentDescription = "Icono Carrito",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Carrito ", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    "(${cart.size})", // Contador de productos únicos
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            if (cart.isEmpty()) {
                // Estado vacío
                Text(
                    "No hay productos aún. Escanea un código para empezar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    fontSize = 14.sp
                )
            } else {
                // Lista de productos
                cart.forEachIndexed { index, item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { onQuantityChange(index, 1) },
                        onDecrement = { onQuantityChange(index, -1) },
                        onRemove = { onRemoveItem(index) }
                    )
                    // Línea divisoria entre productos, excepto en el último
                    if (index < cart.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Previsualización para el diseñador.
 */
@Preview(showBackground = true)
@Composable
fun CartSectionPreview() {
    val sampleCart = listOf(
        CartItem(Product(1, "", "Producto A", 10.0), 1),
        CartItem(Product(2, "", "Producto B", 20.0), 2)
    )
    CartSection(
        cart = sampleCart,
        onQuantityChange = { _, _ -> },
        onRemoveItem = { _ -> }
    )
}
