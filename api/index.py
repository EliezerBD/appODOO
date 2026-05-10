import json
import os
from datetime import datetime
from flask import Flask, request, jsonify
from flask_cors import CORS
import gspread
from google.oauth2.service_account import Credentials

app = Flask(__name__)
CORS(app)

# Credenciales desde variable de entorno (obligatoria en Vercel)
try:
    creds_dict = json.loads(os.environ["GOOGLE_CREDENTIALS_JSON"])
    scopes = ["https://www.googleapis.com/auth/spreadsheets"]
    creds = Credentials.from_service_account_info(creds_dict, scopes=scopes)
    cliente_gs = gspread.authorize(creds)
except Exception as e:
    print("ERROR FATAL: No se pudo autenticar con Google Sheets:", str(e))
    cliente_gs = None

HOJA_ID = os.environ.get("GOOGLE_SHEET_ID", "1vb42AMPeRonYe9oSQ_C_nOTjWuGBER7zvXg5KXjE1QE")
DOMINIO_PERMITIDO = os.environ.get("DOMINIO_PERMITIDO", "https://app-odoo.vercel.app")

@app.route("/api/submit", methods=["POST", "OPTIONS"])
def guardar_pedido():
    # Responder a preflight CORS
    if request.method == "OPTIONS":
        response = jsonify({"status": "ok"})
        response.headers["Access-Control-Allow-Origin"] = DOMINIO_PERMITIDO
        response.headers["Access-Control-Allow-Methods"] = "POST, OPTIONS"
        response.headers["Access-Control-Allow-Headers"] = "Content-Type"
        return response

    # Verificar el origen de la petición
    origen = request.headers.get("Origin")
    # En desarrollo local el origen a veces es None, por seguridad en producción exigimos que coincida
    if origen and origen != DOMINIO_PERMITIDO:
        return jsonify({"success": False, "error": "Acceso no permitido desde este origen"}), 403

    if cliente_gs is None:
        return jsonify({"success": False, "error": "Backend no autenticado con Google Sheets. Revisa credenciales."}), 500

    try:
        datos = request.get_json()
        cliente_datos = datos["cliente"]
        carrito = datos["carrito"]

        libro = cliente_gs.open_by_key(HOJA_ID)
        
        # Obtener la hoja exacta (mayúsculas/minúsculas exactas)
        try:
            hoja = libro.worksheet("SALIDA") # Intentar obtener la hoja "SALIDA"
        except gspread.exceptions.WorksheetNotFound:
            # Si no existe, la crea con el nombre "SALIDA"
            hoja = libro.add_worksheet(title="SALIDA", rows="1000", cols="20")
            hoja.append_row(["Fecha", "Nombre", "Apellido", "Email", "DUI", "Teléfono", "Producto", "Precio", "Cantidad", "Subtotal"])

        fecha = datetime.now().strftime("%Y-%m-%d")

        for item in carrito:
            precio_val = max(0, float(item.get("precio", 0)))
            cantidad_val = max(1, int(item.get("cantidad", 1)))
            subtotal_val = precio_val * cantidad_val

            fila = [
                fecha,
                cliente_datos.get("firstname", ""),
                cliente_datos.get("lastname", ""),
                cliente_datos.get("email", ""),
                cliente_datos.get("dui", ""),
                cliente_datos.get("telefono", ""),
                item.get("nombre", ""),
                precio_val,
                cantidad_val,
                subtotal_val
            ]
            hoja.append_row(fila)

        return jsonify({"success": True})
    except Exception as e:
        # Capturar cualquier error y mostrarlo
        mensaje = str(e)
        print("ERROR en guardar_pedido:", mensaje)
        return jsonify({"success": False, "error": mensaje}), 500