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
