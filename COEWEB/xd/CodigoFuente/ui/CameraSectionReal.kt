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
