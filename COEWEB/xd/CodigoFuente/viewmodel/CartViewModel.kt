package com.example.odooapp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.odooapp.model.CartItem
import com.example.odooapp.model.Product
import com.example.odooapp.network.OdooClient
import com.google.gson.JsonParser
import kotlinx.coroutines.launch

/**
 * ViewModel del Carrito: Es el cerebro de la aplicación.
 * Maneja el estado de los productos, el escaneo y la comunicación con Odoo.
 */
class CartViewModel : ViewModel() {

    // Lista observable de items en el carrito (Reactivo)
    var cart = mutableStateListOf<CartItem>()
        private set

    // Estado para controlar si la cámara está encendida o apagada
    var isCameraActive by mutableStateOf(false)
        private set

    // Datos del cliente vinculados a los campos de texto
    var clientName by mutableStateOf("")
    var clientPhone by mutableStateOf("")
    
    // Control para efectos visuales y notificaciones
    var scanEffect by mutableStateOf(false)
    var toastMessage by mutableStateOf<String?>(null)

    // Propiedades calculadas automáticamente
    val totalPrice: Double get() = cart.sumOf { it.subtotal }
    val isCartEmpty: Boolean get() = cart.isEmpty()
    val isClientValid: Boolean get() = clientName.trim().isNotEmpty() && clientPhone.trim().isNotEmpty()

    private var odooReady = false
    private var lastScanTime: Long = 0

    init {
        // Autenticación inicial con Odoo al abrir el app
        viewModelScope.launch {
            odooReady = OdooClient.authenticate(
                db = "coenergyelsalvador",
                user = "aberciano@coenergyelsalvador.com",
                pass = "4491ee16c0bbddd4bf127cb2ea799293306c17d7" // API KEY / Password
            )
            if (!odooReady) showToast("Error al conectar con Odoo")
        }
    }

    // Funciones para la cámara
    fun activateCamera() { isCameraActive = true }
    fun deactivateCamera() { isCameraActive = false }

    /**
     * Incrementa o decrementa la cantidad de un producto.
     */
    fun changeQuantity(index: Int, delta: Int) {
        if (index !in cart.indices) return
        val item = cart[index]
        item.quantity += delta
        if (item.quantity <= 0) {
            showToast("❌ ${item.product.name} eliminado")
            cart.removeAt(index)
        }
    }

    /**
     * Elimina un producto del carrito.
     */
    fun removeItem(index: Int) {
        if (index !in cart.indices) return
        val item = cart[index]
        showToast("❌ ${item.product.name} eliminado")
        cart.removeAt(index)
    }

    /**
     * Procesa el código QR leído por la cámara.
     */
    fun onQrCodeScanned(code: String) {
        val now = System.currentTimeMillis()
        // Evita lecturas múltiples en menos de 1.5 segundos
        if (now - lastScanTime < 1500) return
        lastScanTime = now

        try {
            // Se espera un formato JSON en el QR: {"id": 1, "name": "Prod", "price": 10.0}
            val json = JsonParser.parseString(code).asJsonObject
            val id = json.get("id").asInt
            val name = json.get("name").asString
            val price = json.get("price").asDouble
            val tax = json.get("tax")?.asDouble ?: 0.0
            val product = Product(id = id, name = name, price = price, tax = tax)

            // Si ya existe, suma cantidad; si no, añade nuevo
            val existing = cart.find { it.product.id == product.id }
            if (existing != null) {
                existing.quantity++
                showToast("➕ ${product.name} (x${existing.quantity})")
            } else {
                cart.add(CartItem(product, 1))
                showToast("🆕 ${product.name} agregado")
            }
            scanEffect = true
        } catch (e: Exception) {
            showToast("QR no válido o formato incorrecto")
        }
    }

    /**
     * Envía el carrito a Odoo para crear un "Pedido de Venta" (sale.order).
     */
    fun createDraftOrder() {
        if (!odooReady) { showToast("Sin conexión con Odoo"); return }
        if (!isClientValid || isCartEmpty) return

        viewModelScope.launch {
            // 1. Busca si el cliente existe o lo crea
            val partnerId = findOrCreatePartner(clientName.trim(), clientPhone.trim())
            if (partnerId == null) {
                showToast("Error al crear/obtener cliente")
                return@launch
            }

            // 2. Prepara las líneas del pedido
            val orderLines = cart.map { item ->
                mapOf(
                    "product_id" to item.product.id,
                    "product_uom_qty" to item.quantity,
                    "price_unit" to item.product.price
                )
            }

            // 3. Envía el pedido a Odoo
            val orderValues = mapOf<String, Any>(
                "partner_id" to partnerId,
                "order_line" to orderLines
            )

            val orderId = OdooClient.createRecord("sale.order", orderValues)
            if (orderId != null) {
                showToast("✅ Pedido borrador creado (ID: $orderId)")
                cart.clear() // Limpia carrito tras éxito
                clientName = ""
                clientPhone = ""
            } else {
                showToast("Error al crear el pedido en Odoo")
            }
        }
    }

    /**
     * Busca un cliente en Odoo por nombre y teléfono, si no existe lo crea.
     */
    private suspend fun findOrCreatePartner(name: String, phone: String): Int? {
        val domain = listOf(
            listOf("name", "=", name),
            listOf("phone", "=", phone)
        )
        val partners = OdooClient.searchRead("res.partner", domain, listOf("id"), 1)
        if (!partners.isNullOrEmpty()) {
            return (partners[0]["id"] as? Double)?.toInt()
        }
        return OdooClient.createRecord("res.partner", mapOf(
            "name" to name,
            "phone" to phone,
            "customer_rank" to 1
        ))
    }

    fun showToast(message: String) { toastMessage = message }
    fun clearToast() { toastMessage = null }
}
