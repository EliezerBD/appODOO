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
