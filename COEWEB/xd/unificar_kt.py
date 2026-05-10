import os
from pathlib import Path

# Carpeta donde está todo tu código Kotlin
raiz = r"C:\Users\eliez\AndroidStudioProjects\ODOOAPP\app\src\main\java\com\example\odooapp"

# Archivo de salida en C:\Users\eliez\Desktop\ODOOAPP
salida = r"C:\Users\eliez\Desktop\ODOOAPP\TodoElCodigo.kt"

# Abrimos el archivo de salida (se sobreescribe si ya existe)
with open(salida, "w", encoding="utf-8") as out:
    out.write("// ==========================================\n")
    out.write("// CÓDIGO COMPLETO DE ODOOAPP\n")
    out.write("// Generado automáticamente\n")
    out.write("// ==========================================\n\n")

    # Recorremos todos los archivos .kt dentro de 'raiz', ignorando la carpeta 'theme'
    for ruta in Path(raiz).rglob("*.kt"):
        # Excluir archivos que estén dentro de cualquier carpeta 'theme'
        if "theme" in ruta.parts:
            continue

        # Ruta relativa para mostrar como cabecera
        relativa = ruta.relative_to(raiz)
        out.write(f"\n// ================== {relativa} ==================\n\n")
        
        # Leer y escribir el contenido
        with open(ruta, "r", encoding="utf-8") as f:
            out.write(f.read())
        out.write("\n")

print(f"✅ Listo! Todo el código Kotlin se unificó en:\n{salida}")
print("   ↳ Se omitió la carpeta 'theme'")
input("Presioná Enter para cerrar...")