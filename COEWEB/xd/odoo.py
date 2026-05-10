import shutil
import os

# Origen del código fuente de tu app
origen = r"C:\Users\eliez\AndroidStudioProjects\ODOOAPP\app\src\main\java\com\example\odooapp"

# Destino: subcarpeta 'CodigoFuente' dentro de la carpeta actual (ODOOAPP)
# Así no intenta borrar la carpeta donde trabaja la terminal
destino = os.path.join(r"C:\Users\eliez\Desktop\ODOOAPP", "CodigoFuente")

# Ignoramos la carpeta 'theme' como querías
def ignorar_carpetas(directory, contents):
    return ['theme'] if 'theme' in contents else []

# Si la subcarpeta de destino ya existe, la borramos sin problemas (no es la carpeta actual)
if os.path.exists(destino):
    shutil.rmtree(destino)
    print("⚠ Carpeta destino anterior eliminada.")

# Copiar todo
shutil.copytree(origen, destino, ignore=ignorar_carpetas)

print(f"✅ Listo! Código copiado a: {destino}")
print("   ↳ Se omitió la carpeta ui/theme")
input("Presioná Enter para salir...")  # Para que veas el mensaje antes de cerrar