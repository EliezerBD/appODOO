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
