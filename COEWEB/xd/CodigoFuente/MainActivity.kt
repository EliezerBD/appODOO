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
