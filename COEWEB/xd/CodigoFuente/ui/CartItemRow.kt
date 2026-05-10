package com.example.odooapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odooapp.model.CartItem
import androidx.compose.ui.tooling.preview.Preview
import com.example.odooapp.model.Product

/**
 * Representa una fila individual en la lista del carrito.
 * Muestra el nombre, precio unitario, cantidad y botones para modificar o eliminar.
 */
@Composable
fun CartItemRow(
    item: CartItem,         // El producto y su cantidad
    onIncrement: () -> Unit, // Acción al pulsar "+"
    onDecrement: () -> Unit, // Acción al pulsar "-"
    onRemove: () -> Unit     // Acción al pulsar el basurero
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Información del producto (Nombre y Precio)
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                "$${"%.2f".format(item.product.price)} c/u",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Controles de cantidad (Botones +/-)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFEEEEEE), CircleShape)
            ) {
                Text("−", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Text(
                text = item.quantity.toString(),
                modifier = Modifier.padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
            
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(0xFFEEEEEE), CircleShape)
            ) {
                Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Botón de eliminar (Basurero)
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Text("🗑️", fontSize = 18.sp)
        }
    }
}

/**
 * Previsualización para el diseñador.
 */
@Preview(showBackground = true)
@Composable
fun CartItemRowPreview() {
    val sampleProduct = Product(id = 1, name = "Producto de Prueba", price = 10.50)
    val sampleItem = CartItem(product = sampleProduct, quantity = 2)
    CartItemRow(
        item = sampleItem,
        onIncrement = {},
        onDecrement = {},
        onRemove = {}
    )
}
