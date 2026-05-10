import xmlrpc.client
import qrcode
import os
import re
import json

# ========== CONFIGURACIÓN ==========
URL = 'https://odoo.coenergyelsalvador.com'
DB = 'coenergyelsalvador'
USERNAME = 'aberciano@coenergyelsalvador.com'
PASSWORD = '4491ee16c0bbddd4bf127cb2ea799293306c17d7'          # <--- ¡PON TU API KEY!

LISTA_PRODUCTOS = [
    "Galón de Refrigerante EXTRA 25% ROJO",
    "Galón de Refrigerante EXTRA 25% VERDE",
    "Galón de Refrigerante SUPER ROJO",
    "Galón de Refrigerante SUPER VERDE",
    "Galón de Refrigerante ULTRA 50% (ROJO)",
    "Galón de Refrigerante ULTRA 50% (VERDE)",
    "GRASA COMPLEJO DE LITIO BLANCA (1 Lb)",
    "GRASA COMPLEJO DE LITIO BLANCA (114GR)",
    "GRASA COMPLEJO DE LITIO BLANCA (7LB)",
    "GRASA COMPLEJO DE LITIO BLANCA (Cubeta 35 Lb)",
    "GRASA DE CALCIO AZUL (14 Lb)",
    "GRASA DE CALCIO AZUL (4.5 L)",
    "GRASA DE CALCIO AZUL (7 Lb)",
    "GRASA DE CALCIO AZUL (Cubeta 35 Lb)",
    "GRASA DE CALCIO AZUL 350 gr",
    "GRASA DE CALCIO ROJA (14 Lb)",
    "GRASA DE CALCIO ROJA (4.5 Lb)",
    "GRASA DE CALCIO ROJA (7 Lb)",
    "GRASA DE CALCIO ROJA (Cubeta 35 Lb)",
    "GRASA DE CALCIO ROJA 350 gr",
    "GRASA DE LITIO ROJA (360GR)",
    "GRASA DE LITIO ROJA (4.5LB)",
    "Libre de Humo (12 oz)",
    "Libre de Humo (8 oz)",
    "Súper Estabilizador De Aceite (32 oz)",
    "Súper Estabilizador De Aceite (Galón)",
    "Tratamiento para Combustible (2 oz)",
    "Tratamiento para Combustible (8 oz)",
    "Tratamiento para Metal (2 oz)",
    "Tratamiento para Metal (32 oz)",
    "Tratamiento para Metal (8 oz)",
    "Tratamiento para Metal (Galón)",
    "UBERLUB 10W30 4T SEMI SINTETICO JASO MA2 (1 L)",
    "UBERLUB 10W30 SEMI SINTETICO API SN (1 L)",
    "UBERLUB 10W30 SEMI SINTETICO API SN (20 L)",
    "UBERLUB 10W30 SEMI SINTETICO API SN (4 L)",
    "UBERLUB 15W40 API CI4/CI-PLUS HIGH MILEAGE FULL SINTETICO (1 L)",
    "UBERLUB 15W40 API CI4/CI-PLUS HIGH MILEAGE FULL SINTETICO (20 L)",
    "UBERLUB 15W40 API CI4/CI-PLUS HIGH MILEAGE FULL SINTETICO (4 L)",
    "UBERLUB 15W40 API CI4/CI-PLUS MINERAL (20 L)",
    "UBERLUB 20W50 API CI4/CI-PLUS MINERAL (1 L)",
    "UBERLUB 20W50 API CI4/CI-PLUS MINERAL (4 L)",
    "UBERLUB 20W50 API CI4/CI-PLUS MINERAL (20 L)",
    "UBERLUB 5W30 FULL SINTETICO ACEA C2/C3 BMWLL-04 (1 L)",
    "UBERLUB 5W30 FULL SINTETICO ACEA C2/C3 BMWLL-04 (4 L)",
    "UBERLUB BRAKE FLUID DOT 3 (0.5 L)",
    "UBERLUB BRAKE FLUID DOT 3 (1 L)",
    "UBERLUB BRAKE FLUID DOT 4 (0.5 L)",
    "UBERLUB BRAKE FLUID DOT 4 (1 L)",
    "UBERLUB GRASA GRAFITADA",
    "UBERLUB HIDRAULYC 68 (4 L)",
]

# =====================================

def conectar():
    common = xmlrpc.client.ServerProxy(f'{URL}/xmlrpc/2/common')
    uid = common.authenticate(DB, USERNAME, PASSWORD, {})
    if not uid:
        raise Exception("Autenticación fallida")
    models = xmlrpc.client.ServerProxy(f'{URL}/xmlrpc/2/object')
    return uid, models

def obtener_datos_producto(models, uid, nombre):
    """Busca por nombre exacto y devuelve id, name, list_price y el primer impuesto."""
    domain = [('name', '=', nombre)]
    ids = models.execute_kw(DB, uid, PASSWORD, 'product.product', 'search', [domain])
    if not ids:
        return None
    fields = ['id', 'name', 'list_price', 'taxes_id']
    resultado = models.execute_kw(DB, uid, PASSWORD,
        'product.product', 'read', [ids[0]], {'fields': fields})
    if not resultado:
        return None
    prod = resultado[0]
    tax_id = None
    if prod.get('taxes_id'):
        # Suponiendo que solo tiene un impuesto, tomamos el primer ID
        tax_id = prod['taxes_id'][0]
    return {
        'id': prod['id'],
        'name': prod['name'],
        'price': prod['list_price'],
        'tax_id': tax_id
    }

def obtener_porcentaje_impuesto(models, uid, tax_id):
    """Obtiene el porcentaje de impuesto (amount) a partir de su ID."""
    if not tax_id:
        return 0.0
    tax_data = models.execute_kw(DB, uid, PASSWORD,
        'account.tax', 'read', [tax_id], {'fields': ['amount']})
    if tax_data:
        return tax_data[0]['amount']
    return 0.0

def sanitizar_archivo(texto):
    return re.sub(r'[\\/*?:"<>|]', '_', texto)

def main():
    uid, models = conectar()
    print(f"Conectado. UID={uid}")
    os.makedirs('qr_productos', exist_ok=True)

    for nombre in LISTA_PRODUCTOS:
        print(f"\nBuscando: {nombre}")
        datos = obtener_datos_producto(models, uid, nombre)
        if not datos:
            print("   ❌ NO ENCONTRADO. Verifica el nombre exacto en Odoo.")
            continue

        tax_percent = obtener_porcentaje_impuesto(models, uid, datos['tax_id'])
        
        # Información que irá dentro del QR (JSON)
        qr_info = {
            "id": datos['id'],
            "name": datos['name'],
            "price": datos['price'],
            "tax": tax_percent
        }
        qr_json = json.dumps(qr_info, ensure_ascii=False)

        print(f"   ✔ Producto: {datos['name']}")
        print(f"   Precio: {datos['price']}, IVA: {tax_percent}%")

        # Generar QR
        img = qrcode.make(qr_json)
        nombre_archivo = f"{sanitizar_archivo(datos['name'])}.png"
        ruta = os.path.join('qr_productos', nombre_archivo)
        img.save(ruta)
        print(f"   💾 Guardado: {ruta}")

if __name__ == '__main__':
    main()