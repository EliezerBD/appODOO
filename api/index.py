import json
import os
import re
from datetime import datetime
from flask import Flask, request, jsonify
from flask_cors import CORS
import gspread
from google.oauth2.service_account import Credentials

app = Flask(__name__)

# ------------------------------------------------------------
# CONFIGURACIÓN DE SEGURIDAD
# ------------------------------------------------------------
# Permite CORS únicamente desde el dominio de tu frontend
DOMINIO_PERMITIDO = os.environ.get("DOMINIO_PERMITIDO", "https://app-odoo.vercel.app")
CORS(app, origins=[DOMINIO_PERMITIDO])

# Clave secreta para proteger el endpoint (debe coincidir con la del frontend)
API_SECRET = os.environ.get("API_SECRET", "cambia-esta-clave-en-produccion")

# ------------------------------------------------------------
# AUTENTICACIÓN CON GOOGLE SHEETS
# ------------------------------------------------------------
try:
    creds_dict = json.loads(os.environ["GOOGLE_CREDENTIALS_JSON"])
    scopes = ["https://www.googleapis.com/auth/spreadsheets"]
    creds = Credentials.from_service_account_info(creds_dict, scopes=scopes)
    cliente_gs = gspread.authorize(creds)
except Exception as e:
    print("ERROR FATAL: No se pudo autenticar con Google Sheets:", str(e))
    cliente_gs = None

HOJA_ID = os.environ.get("GOOGLE_SHEET_ID", "1vb42AMPeRonYe9oSQ_C_nOTjWuGBER7zvXg5KXjE1QE")

# ------------------------------------------------------------
# FUNCIÓN PARA EVITAR INYECCIÓN DE FÓRMULAS EN SHEETS
# ------------------------------------------------------------
def sanitizar_para_sheets(valor):
    """
    Convierte cualquier valor a string y lo escapa
    si comienza con =, +, - o @ (evita que Google Sheets
    interprete el contenido como una fórmula maliciosa).
    """
    str_val = str(valor)
    if str_val and str_val[0] in ('=', '+', '-', '@'):
        # Una comilla simple al principio fuerza a Sheets a tratarlo como texto
        return "'" + str_val
    return str_val

# ------------------------------------------------------------
# ENDPOINT PRINCIPAL
# ------------------------------------------------------------
@app.route("/api/get-apikey", methods=["GET"])
def get_apikey():
    # Verificar que la petición viene de tu dominio
    origen = request.headers.get("Origin", "")
    if origen != DOMINIO_PERMITIDO:
        return jsonify({"error": "No autorizado"}), 403
    
    # Devolver la clave (el frontend la usará en memoria)
    return jsonify({"apiKey": API_SECRET})

@app.route("/api/submit", methods=["POST", "OPTIONS"])
def guardar_pedido():
    # Respuesta automática a la petición preflight CORS
    if request.method == "OPTIONS":
        response = jsonify({"status": "ok"})
        # Los headers CORS los agrega automáticamente flask_cors
        return response

    # 1. Verificar API Key
    api_key = request.headers.get("X-API-Key")
    if not api_key or api_key != API_SECRET:
        return jsonify({"success": False, "error": "Acceso no autorizado. Falta API key válida."}), 401

    # 2. Verificar que Google Sheets esté disponible
    if cliente_gs is None:
        return jsonify({"success": False, "error": "Backend no autenticado con Google Sheets. Revisa credenciales."}), 500

    try:
        datos = request.get_json()
        tipo = datos.get("tipo", "")
        libro = cliente_gs.open_by_key(HOJA_ID)

        # ---------------------------
        # CASO: ENTRADA DE ALMACÉN
        # ---------------------------
        if tipo == "ALMACEN":
            bodega_num = datos.get("bodega", "")
            nombre_hoja = f"BODEGA{bodega_num}"

            try:
                hoja = libro.worksheet(nombre_hoja)
            except gspread.exceptions.WorksheetNotFound:
                hoja = libro.add_worksheet(title=nombre_hoja, rows="1000", cols="5")
                hoja.append_row(["Fecha", "Producto", "Cantidad"])

            fila = [
                sanitizar_para_sheets(datos.get("fecha", datetime.now().strftime("%Y-%m-%d %H:%M:%S"))),
                sanitizar_para_sheets(datos.get("producto", "")),
                sanitizar_para_sheets(datos.get("cantidad", 0))
            ]
            hoja.append_row(fila)
            return jsonify({"success": True})
        # ---------------------------
        # CASO: PEDIDO DE CLIENTE
        # ---------------------------
        else:
            cliente_datos = datos.get("cliente", {})
            carrito = datos.get("carrito", [])

            try:
                hoja = libro.worksheet("SALIDA")
            except gspread.exceptions.WorksheetNotFound:
                hoja = libro.add_worksheet(title="SALIDA", rows="1000", cols="20")
                hoja.append_row([
                    "Fecha", "Nombre", "Apellido", "Email", "DUI", "Teléfono",
                    "Producto", "Precio", "Cantidad", "Subtotal"
                ])

            fecha = datetime.now().strftime("%Y-%m-%d")

            for item in carrito:
                precio_val = max(0, float(item.get("precio", 0)))
                cantidad_val = max(1, int(item.get("cantidad", 1)))
                subtotal_val = precio_val * cantidad_val

                fila = [
                    sanitizar_para_sheets(fecha),
                    sanitizar_para_sheets(cliente_datos.get("firstname", "")),
                    sanitizar_para_sheets(cliente_datos.get("lastname", "")),
                    sanitizar_para_sheets(cliente_datos.get("email", "")),
                    sanitizar_para_sheets(cliente_datos.get("dui", "")),
                    sanitizar_para_sheets(cliente_datos.get("telefono", "")),
                    sanitizar_para_sheets(item.get("nombre", "")),
                    sanitizar_para_sheets(precio_val),
                    sanitizar_para_sheets(cantidad_val),
                    sanitizar_para_sheets(subtotal_val)
                ]
                hoja.append_row(fila)

            return jsonify({"success": True})

    except Exception as e:
        mensaje = str(e)
        print("ERROR en guardar_pedido:", mensaje)
        return jsonify({"success": False, "error": mensaje}), 500

# ------------------------------------------------------------
# PARA PRUEBAS LOCALES (opcional)
# ------------------------------------------------------------
if __name__ == "__main__":
    app.run(debug=True)