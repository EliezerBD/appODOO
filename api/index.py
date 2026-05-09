import json
import os
from datetime import datetime
from flask import Flask, request, jsonify
from flask_cors import CORS
import gspread
from google.oauth2.service_account import Credentials

app = Flask(__name__)
CORS(app)

creds_dict = json.loads(os.environ["GOOGLE_CREDENTIALS_JSON"])
scopes = ["https://www.googleapis.com/auth/spreadsheets"]
creds = Credentials.from_service_account_info(creds_dict, scopes=scopes)
cliente_gs = gspread.authorize(creds)

HOJA_ID = "1vb42AMPeRonYe9oSQ_C_nOTjWuGBER7zvXg5KXjE1QE"
HOJA_NOMBRE = "SALIDA"
API_TOKEN = "inventario2024seguro"

@app.route("/api/submit", methods=["POST", "OPTIONS"])
def guardar_pedido():
    if request.method == "OPTIONS":
        response = jsonify({"status": "ok"})
        response.headers["Access-Control-Allow-Origin"] = "*"
        response.headers["Access-Control-Allow-Methods"] = "POST, OPTIONS"
        response.headers["Access-Control-Allow-Headers"] = "Content-Type, x-api-token"
        return response

    token = request.headers.get("x-api-token")
    if token != API_TOKEN:
        return jsonify({"success": False, "error": "Token inválido"}), 403

    try:
        datos = request.get_json()
        cliente_datos = datos["cliente"]
        carrito = datos["carrito"]

        hoja = cliente_gs.open_by_key(HOJA_ID).worksheet(HOJA_NOMBRE)
        fecha = datetime.now().strftime("%Y-%m-%d")

        # Recorremos cada producto y añadimos una fila
        for item in carrito:
            fila = [
                fecha,
                cliente_datos.get("firstname", ""),
                cliente_datos.get("lastname", ""),
                cliente_datos.get("email", ""),
                cliente_datos.get("dui", ""),
                cliente_datos.get("telefono", ""),
                item.get("nombre", ""),
                item.get("precio", 0),
                item.get("cantidad", 0),
                item.get("subtotal", 0)
            ]
            hoja.append_row(fila)

        return jsonify({"success": True})
    except Exception as e:
        print("Error:", str(e))
        return jsonify({"success": False, "error": str(e)}), 500