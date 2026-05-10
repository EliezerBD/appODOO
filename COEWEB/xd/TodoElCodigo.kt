// ==========================================
// CÓDIGO COMPLETO DE ODOOAPP
// Generado automáticamente
// ==========================================


// ================== MainActivity.kt ==================

package com.example.odooapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.odooapp.ui.CartApp

/**
 * Actividad principal: Es el punto de inicio de la aplicación Android.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent define la interfaz de usuario usando Jetpack Compose
        setContent {
            // Aplicamos el tema visual de la aplicación
            AppTheme {
                // Llamamos a la pantalla principal del carrito
                CartApp()
            }
        }
    }
}

/**
 * Define el tema visual (Colores, Tipografía) de la aplicación.
 * Sigue las guías de Material Design 3.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1A73E8),       // Azul Odoo/Google
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD2E3FC),
            surface = Color.White,
            background = Color(0xFFE8EAED),    // Gris muy claro para el fondo
            error = Color(0xFFEA4335),
            onSurface = Color(0xFF202124),     // Negro/Gris oscuro para texto
            onSurfaceVariant = Color(0xFF5F6368),
            outline = Color(0xFFDADCE0),
        ),
        content = content
    )
}

/**
 * Previsualización de la aplicación completa.
 */
@Preview(showBackground = true)
@Composable
fun CartAppPreview() {
    AppTheme {
        CartApp()
    }
}

/**
 * Previsualización del sistema de colores y componentes básicos.
 */
@Preview(showBackground = true)
@Composable
fun AppThemePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Título Principal", 
                    style = MaterialTheme.typography.headlineMedium, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Este es un texto de ejemplo para ver cómo se aplican los colores.", 
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(onClick = {}) {
                    Text("Botón Primario")
                }
            }
        }
    }
}


// ================== model\CartItem.kt ==================

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


// ================== model\Product.kt ==================

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


// ================== network\OdooApiService.kt ==================

package com.example.odooapp.network

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Representa una solicitud JSON-RPC para comunicarse con Odoo.
 * Odoo utiliza este formato estándar para recibir comandos.
 */
data class JsonRpcRequest(
    val jsonrpc: String = "2.0", // Versión del protocolo
    val method: String = "call", // Método de Odoo (generalmente "call")
    val params: Any,             // Parámetros de la función (filtros, datos, etc.)
    val id: Int = 1              // Identificador de la transacción
)

/**
 * Representa la respuesta que envía Odoo tras una solicitud.
 */
data class JsonRpcResponse(
    val jsonrpc: String,
    val id: Int,
    val result: Any?,            // Resultado exitoso (si existe)
    val error: Any?              // Información del error (si falló)
)

/**
 * Interfaz de Retrofit que define los puntos de conexión (endpoints) con el servidor.
 */
interface OdooApiService {
    @POST("jsonrpc") // Odoo suele exponer un único endpoint para todas las peticiones JSON-RPC
    suspend fun execute(@Body request: JsonRpcRequest): JsonRpcResponse
}


// ================== network\OdooClient.kt ==================

package com.example.odooapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente de Odoo: Encapsula la lógica para conectarse a Odoo usando JSON-RPC.
 * Permite autenticarse, buscar registros y crear nuevos datos (como pedidos).
 */
object OdooClient {
    private const val BASE_URL = "https://odoo.coenergyelsalvador.com/"
    private var api: OdooApiService
    private var uid: Int = 0          // ID de usuario tras autenticarse
    private var password: String = "" // Contraseña o API Key
    private var db: String = ""       // Nombre de la base de datos de Odoo

    init {
        // Interceptor para ver las peticiones en el Logcat (depuración)
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // Configuración de Retrofit para manejar JSON
        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OdooApiService::class.java)
    }

    /**
     * Inicia sesión en Odoo y obtiene el UID del usuario.
     */
    suspend fun authenticate(db: String, user: String, pass: String): Boolean {
        this.db = db
        this.password = pass
        val params = mapOf(
            "service" to "common",
            "method" to "authenticate",
            "args" to listOf(db, user, pass, mapOf<String, Any>())
        )
        val request = JsonRpcRequest(params = params)
        val response = api.execute(request)
        return if (response.error == null) {
            uid = (response.result as? Double)?.toInt() ?: return false
            true
        } else false
    }

    /**
     * Busca y lee registros de cualquier modelo en Odoo.
     */
    suspend fun searchRead(
        model: String,              // Ejemplo: "res.partner" o "product.product"
        domain: List<List<Any>>,    // Filtros de búsqueda
        fields: List<String>,       // Campos que queremos traer
        limit: Int = 1              // Límite de resultados
    ): List<Map<String, Any>>? {
        val params = mapOf(
            "service" to "object",
            "method" to "execute_kw",
            "args" to listOf(
                db, uid, password,
                model, "search_read",
                listOf(domain),
                mapOf("fields" to fields, "limit" to limit)
            )
        )
        val request = JsonRpcRequest(params = params)
        val response = api.execute(request)
        if (response.error != null) return null
        return (response.result as? List<*>)?.filterIsInstance<Map<String, Any>>()
    }

    /**
     * Crea un nuevo registro en Odoo.
     */
    suspend fun createRecord(model: String, values: Map<String, Any>): Int? {
        val params = mapOf(
            "service" to "object",
            "method" to "execute_kw",
            "args" to listOf(
                db, uid, password,
                model, "create",
                listOf(values)
            )
        )
        val request = JsonRpcRequest(params = params)
        val response = api.execute(request)
        if (response.error != null) return null
        return (response.result as? Double)?.toInt()
    }
}


// ================== ui\CameraSectionReal.kt ==================

package com.example.odooapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sección de Cámara (UI): Controla la visualización del escáner en la pantalla.
 * Alterna entre un cuadro gris con un botón y la vista previa real de la cámara.
 */
@Composable
fun CameraSectionReal(
    isActive: Boolean,        // ¿La cámara está encendida?
    scanEffect: Boolean,      // ¿Acaba de ocurrir un escaneo exitoso? (efecto verde)
    onActivate: () -> Unit,   // Acción para encender
    onDeactivate: () -> Unit, // Acción para apagar
    onScan: (String) -> Unit  // Acción al detectar un código QR
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.padding(top = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contenedor visual de la cámara
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                if (isActive) {
                    // Muestra el flujo de cámara real
                    QrCameraScanner(
                        modifier = Modifier.fillMaxSize(),
                        onScan = onScan
                    )
                    
                    // Botón flotante para cerrar la cámara
                    IconButton(
                        onClick = onDeactivate,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Text("✖", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    // Destello verde de confirmación tras escanear
                    if (scanEffect) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Green.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✅ Escaneado", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Cuadro informativo cuando la cámara está apagada
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FontAwesomeCamera,
                            contentDescription = "Activar cámara",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Botón principal de control
            if (isActive) {
                Text(
                    "Escaneando... apunta al código QR",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Button(
                    onClick = onActivate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(" Activar cámara", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}


// ================== ui\CartApp.kt ==================

package com.example.odooapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.odooapp.viewmodel.CartViewModel

/**
 * Pantalla Principal de la Aplicación.
 * Organiza las secciones de Cámara, Carrito y Cliente usando un Scaffold (Estructura base).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartApp(viewModel: CartViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Gestor de permisos para la cámara (Aparece el diálogo de Android si no hay permiso)
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.activateCamera()
        } else {
            viewModel.showToast(" Permiso de cámara denegado")
        }
    }

    // Efecto que escucha mensajes del ViewModel para mostrar notificaciones (Toast/Snackbar)
    LaunchedEffect(viewModel.toastMessage) {
        viewModel.toastMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearToast()
        }
    }

    // Estructura de la pantalla: Superior, Inferior y Contenido
    Scaffold(
        topBar = {
            // Barra superior personalizada con título y descripción
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(" InventarioApp", fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = Color.White)
                Text("Escanea y crea pedido borrador", fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
            }
        },
        bottomBar = {
            // Barra inferior que muestra el total y el botón de enviar a Odoo
            Surface(
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total:", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            "$${"%.2f".format(viewModel.totalPrice)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.createDraftOrder() },
                        enabled = !viewModel.isCartEmpty && viewModel.isClientValid, // Se habilita solo si hay datos
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F9D58), // Verde para acción de éxito
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFCCCCCC)
                        )
                    ) {
                        Text(" Crear Pedido en Odoo (Borrador)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFE8EAED) // Fondo gris claro para toda la pantalla
    ) { innerPadding ->
        // Contenido desplazable (Scroll)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECCIÓN 1: Cámara / Escáner
            item {
                CameraSectionReal(
                    isActive = viewModel.isCameraActive,
                    scanEffect = viewModel.scanEffect,
                    onActivate = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            viewModel.activateCamera()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    onDeactivate = { viewModel.deactivateCamera() },
                    onScan = { viewModel.onQrCodeScanned(it) }
                )
            }

            // SECCIÓN 2: Lista de Productos en el Carrito
            item {
                CartSection(
                    cart = viewModel.cart,
                    onQuantityChange = { index, delta -> viewModel.changeQuantity(index, delta) },
                    onRemoveItem = { index -> viewModel.removeItem(index) }
                )
            }

            // SECCIÓN 3: Formulario del Cliente
            item {
                ClientSection(
                    clientName = viewModel.clientName,
                    clientPhone = viewModel.clientPhone,
                    onNameChange = { viewModel.clientName = it },
                    onPhoneChange = { viewModel.clientPhone = it }
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}


// ================== ui\CartItemRow.kt ==================

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


// ================== ui\CartSection.kt ==================

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


// ================== ui\ClientSection.kt ==================

package com.example.odooapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * Sección de Cliente: Formulario para ingresar los datos de la empresa.
 * Estos datos son obligatorios para crear el pedido en Odoo.
 */
@Composable
fun ClientSection(
    clientName: String,             // Nombre actual en el estado
    clientPhone: String,            // Teléfono actual en el estado
    onNameChange: (String) -> Unit, // Función para actualizar el nombre
    onPhoneChange: (String) -> Unit // Función para actualizar el teléfono
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Icono de usuario para indicar visualmente qué es esta sección
            Icon(
                imageVector = FontAwesomeUser,
                contentDescription = "Icono Cliente",
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Campo para el nombre
            OutlinedTextField(
                value = clientName,
                onValueChange = onNameChange,
                label = { Text("Nombre de empresa *") },
                placeholder = { Text("Ej: Construfácil S.A.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Campo para el teléfono
            OutlinedTextField(
                value = clientPhone,
                onValueChange = onPhoneChange,
                label = { Text("Teléfono *") },
                placeholder = { Text("Ej: 2234-5678") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

/**
 * Previsualización para el diseñador.
 */
@Preview(showBackground = true)
@Composable
fun ClientSectionPreview() {
    ClientSection(
        clientName = "Empresa Ejemplo",
        clientPhone = "1234-5678",
        onNameChange = {},
        onPhoneChange = {}
    )
}


// ================== ui\Icons.kt ==================

package com.example.odooapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

val FontAwesomeCamera: ImageVector = Icons.Filled.PhotoCamera
val FontAwesomeCartShopping: ImageVector = Icons.Filled.ShoppingCart
val FontAwesomeUser: ImageVector = Icons.Filled.Person


// ================== ui\QrCameraScanner.kt ==================

package com.example.odooapp.ui

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Componente de bajo nivel para el escaneo de QR.
 * Utiliza CameraX para la vista previa y ML Kit para procesar la imagen y detectar códigos.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun QrCameraScanner(
    modifier: Modifier = Modifier,
    onScan: (String) -> Unit // Se llama cada vez que detecta un texto en un código QR
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Usamos AndroidView para integrar una vista clásica de Android (PreviewView) en Compose
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // 1. Configuración de la vista previa
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // 2. Configuración del análisis de imagen (Escaneo de QR)
                val scanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    // Si detecta un código, envía el valor al callback
                                    barcode.rawValue?.let { value ->
                                        onScan(value)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                Log.e("QrCameraScanner", "Error al escanear QR", it)
                            }
                            .addOnCompleteListener {
                                // Es vital cerrar el frame para procesar el siguiente
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                // 3. Selección de cámara trasera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Desvincular cualquier uso previo y conectar al ciclo de vida de la pantalla
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("QrCameraScanner", "Fallo al iniciar cámara", e)
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}


// ================== viewmodel\CartViewModel.kt ==================

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

