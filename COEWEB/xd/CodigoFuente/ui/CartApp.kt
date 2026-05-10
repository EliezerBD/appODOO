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
