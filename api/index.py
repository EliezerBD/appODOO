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
# CONFIGURACIÓN DE SEGURIDAD (temporal sin verificación de origen)
# ------------------------------------------------------------
DOMINIO_PERMITIDO = os.environ.get("DOMINIO_PERMITIDO", "https://app-odoo.vercel.app")
CORS(app, origins=[DOMINIO_PERMITIDO])

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
    str_val = str(valor)
    if str_val and str_val[0] in ('=', '+', '-', '@'):
        return "'" + str_val
    return str_val

# ------------------------------------------------------------
# ENDPOINT PARA OBTENER LA API KEY (sin restricción de origen)
# ------------------------------------------------------------
@app.route("/api/get-apikey", methods=["GET"])
def get_apikey():
    # Temporal: devolvemos la clave sin verificar el origen
    return jsonify({"apiKey": API_SECRET})

# ------------------------------------------------------------
# ENDPOINT PRINCIPAL (con protección de API Key)
# ------------------------------------------------------------
@app.route("/api/submit", methods=["POST", "OPTIONS"])
def guardar_pedido():
    if request.method == "OPTIONS":
        response = jsonify({"status": "ok"})
        return response

    api_key = request.headers.get("X-API-Key")
    if not api_key or api_key != API_SECRET:
        return jsonify({"success": False, "error": "Acceso no autorizado. Falta API key válida."}), 401

    if cliente_gs is None:
        return jsonify({"success": False, "error": "Backend no autenticado con Google Sheets. Revisa credenciales."}), 500

    try:
        datos = request.get_json()
        tipo = datos.get("tipo", "")
        libro = cliente_gs.open_by_key(HOJA_ID)

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

if __name__ == "__main__":
    app.run(debug=True)