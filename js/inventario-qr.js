// ==================== CONFIGURACIÓN DEL BACKEND ====================
const GOOGLE_SHEET_URL = '/api/submit';


// ==================== API KEY DINÁMICA (OCULTA) ====================
let API_KEY = '';

async function obtenerApiKey() {
    try {
        showLoader('Iniciando...');
        const response = await fetch('/api/get-apikey');
        const data = await response.json();
        API_KEY = data.apiKey;
    } catch (error) {
        console.error('Error obteniendo API Key');
    } finally {
        hideLoader();
    }
}
// ==================== MODELOS ====================
class Product {
    constructor(id, name, price, tax = 0, code = '') {
        this.id = id; this.code = code; this.name = name;
        this.price = price; this.tax = tax;
    }
}
class CartItem {
    constructor(product, quantity = 1) {
        this.product = product; this.quantity = quantity;
    }
    get subtotal() { return this.product.price * this.quantity; }
    get totalTax() { return this.subtotal * this.product.tax / 100; }
    get subtotalWithTax() { return this.subtotal + this.totalTax; }
}

// ==================== ESTADO ====================
let cart = [];
let clientFirstName = '', clientLastName = '', clientEmail = '', clientDUI = '', clientPhone = '';
let lastScanTime = 0;
let html5QrCode = null;

// ==================== UI ====================
function showToast(msg) {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = msg;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 2600);
}

function showLoader(text = 'Procesando...') {
    const overlay = document.getElementById('loader-overlay');
    const textEl = overlay.querySelector('.loader-text');
    if (textEl) textEl.textContent = text;
    overlay.style.display = 'flex';
}

function hideLoader() {
    const overlay = document.getElementById('loader-overlay');
    overlay.style.display = 'none';
}

function flashEffect() {
    const flash = document.getElementById('scanFlash');
    flash.classList.add('show');
    setTimeout(() => flash.classList.remove('show'), 800);
}

function renderAll() {
    renderCart();
    updateCreateButton();
}

function renderCart() {
    const container = document.getElementById('cartContainer');
    const countSpan = document.getElementById('cartCount');
    const totalRow = document.getElementById('cartTotalRow');
    const subtotalDisplay = document.getElementById('subtotalDisplay');
    const taxDisplay = document.getElementById('taxDisplay');
    const totalDisplay = document.getElementById('totalPriceDisplay');

    if (cart.length === 0) {
        container.innerHTML = '<div class="empty-cart text-center text-on-surface-variant p-lg" id="emptyCartMsg">No produtos en el carrito</div>';
        countSpan.textContent = '(0) ITEMS';
        totalRow.style.display = 'none';
        return;
    }
    
    const itemsCount = cart.reduce((sum, item) => sum + item.quantity, 0);
    countSpan.textContent = `(${itemsCount}) ITEM${itemsCount !== 1 ? 'S' : ''}`;
    totalRow.style.display = 'block';

    let html = '';
    cart.forEach((item, index) => {
        html += `
            <div class="glass-card light-catch rounded-[24px] p-md flex gap-md items-center">
                <div class="w-14 h-14 rounded-xl overflow-hidden bg-gradient-to-br from-blue-900/50 to-cyan-900/50 shrink-0 flex items-center justify-center">
                    <span class="material-symbols-outlined text-[28px] text-secondary/50">inventory_2</span>
                </div>
                <div class="flex-grow min-w-0">
                        <h3 class="font-title-sm text-title-sm text-on-surface leading-tight mb-xs">${escapeHtml(item.product.name)}</h3>
                        <!-- 🔧 CORREGIDO: Se añadió la etiqueta <p> que faltaba para el precio -->
                        <p class="font-body-sm text-body-sm text-secondary">
                            $<input type="number" step="0.01" min="0" class="w-24 bg-transparent text-secondary border-b border-secondary/30 outline-none" value="${item.product.price.toFixed(2)}" onchange="updatePrice(${index}, this.value)"> c/u
                        </p>
                    <div class="flex items-center mt-sm">
                        <div class="flex items-center bg-surface-container-low rounded-lg p-1 border border-white/5">
                            <button class="w-8 h-8 flex items-center justify-center text-outline hover:text-secondary active:scale-90 transition-all" onclick="changeQuantity(${index}, -1)">
                                <span class="material-symbols-outlined text-[18px]">remove</span>
                            </button>
                            <span class="px-md font-label-caps text-label-caps text-on-surface quantity-display">${item.quantity}</span>
                            <button class="w-8 h-8 flex items-center justify-center text-outline hover:text-secondary active:scale-90 transition-all" onclick="changeQuantity(${index}, 1)">
                                <span class="material-symbols-outlined text-[18px]">add</span>
                            </button>
                        </div>
                    </div>
                </div>
                <button class="self-start text-outline hover:text-error transition-colors p-xs active:scale-90" onclick="removeItem(${index})">
                    <span class="material-symbols-outlined">delete</span>
                </button>
            </div>
        `;
    });
    container.innerHTML = html;

    const subtotal = cart.reduce((sum, item) => sum + item.subtotal, 0);
    const tax = subtotal * 0.08;
    const total = subtotal + tax;

    subtotalDisplay.textContent = `$${subtotal.toFixed(2)}`;
    taxDisplay.textContent = `$${tax.toFixed(2)}`;
    totalDisplay.textContent = `$${total.toFixed(2)}`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function updateCreateButton() {
    const btn = document.getElementById('btnCreateOrder');
    const clientValid = 
        clientFirstName.trim() !== '' &&
        clientLastName.trim() !== '' &&
        clientEmail.trim()    !== '';
    btn.disabled = !(cart.length > 0 && clientValid);
}

// ==================== CARRITO ====================
function changeQuantity(index, delta) {
    if (index < 0 || index >= cart.length) return;
    const item = cart[index];
    const newQty = item.quantity + delta;
    if (newQty <= 0) {
        // 🔧 CORREGIDO: escapeHtml en el nombre del producto
        showToast(`❌ ${escapeHtml(item.product.name)} eliminado`);
        cart.splice(index, 1);
    } else {
        item.quantity = newQty;
        // 🔧 CORREGIDO: escapeHtml en el nombre del producto
        showToast(`🔄 ${escapeHtml(item.product.name)} (x${newQty})`);
    }
    renderAll();
}

function removeItem(index) {
    if (index < 0 || index >= cart.length) return;
    const item = cart[index];
    // 🔧 CORREGIDO: escapeHtml en el nombre del producto
    showToast(`❌ ${escapeHtml(item.product.name)} eliminado`);
    cart.splice(index, 1);
    renderAll();
}

function updatePrice(index, newPrice) {
    if (index < 0 || index >= cart.length) return;
    const price = parseFloat(newPrice);
    if (isNaN(price) || price < 0) return;
    cart[index].product.price = price;
    renderAll();
}

// ==================== PROCESAMIENTO DE QR ====================
function processScannedText(text) {
    const now = Date.now();
    if (now - lastScanTime < 1500) return;
    lastScanTime = now;

    // Verificar en qué pestaña estamos para saber qué lógica aplicar
    const activeTab = document.querySelector('.tab-content.active');
    if (activeTab && activeTab.id === 'almacenTab') {
        processAlmacenScan(text);
    } else {
        processOrderScan(text);
    }
}

function processOrderScan(text) {
    try {
        const obj = JSON.parse(text);
        if (obj.id && obj.name && obj.price !== undefined) {
            const product = new Product(obj.id, obj.name, obj.price, obj.tax || 0, obj.code || '');
            addProduct(product);
            return;
        }
    } catch (e) { }
    const product = new Product(Date.now(), text.trim() || "Producto escaneado", 0.01, 0, '');
    addProduct(product);
}

function processAlmacenScan(text) {
    flashEffect();
    let productName = text.trim();
    try {
        const obj = JSON.parse(text);
        if (obj.name) productName = obj.name;
    } catch (e) { }
    
    document.getElementById('almacenProductName').value = productName;
    document.getElementById('almacenForm').style.display = 'block';
    // 🔧 CORREGIDO: escapeHtml para evitar XSS
    showToast(`📸 Escaneado: ${escapeHtml(productName)}`);
    
    // Scroll al formulario
    document.getElementById('almacenForm').scrollIntoView({ behavior: 'smooth' });
}

function addProduct(product) {
    const existingIndex = cart.findIndex(item => item.product.id === product.id);
    if (existingIndex !== -1) {
        cart[existingIndex].quantity += 1;
        // 🔧 CORREGIDO: escapeHtml en el nombre del producto
        showToast(`➕ ${escapeHtml(product.name)} (x${cart[existingIndex].quantity})`);
    } else {
        cart.push(new CartItem(product, 1));
        // Ya usabas escapeHtml aquí, se mantiene
        showToast(`🆕 ${escapeHtml(product.name)} agregado`);
    }
    flashEffect();
    renderAll();
}

// ==================== CÁMARA REAL ====================
function startCamera(readerId = 'qrReader') {
    if (html5QrCode) stopCamera();
    
    const isAlmacen = readerId === 'almacenQrReader';
    const readerElement = document.getElementById(readerId);
    const placeholderId = isAlmacen ? 'almacenCameraPlaceholder' : 'cameraPlaceholder';
    const btnStartId = isAlmacen ? 'btnActivateAlmacenCamera' : 'btnActivateCamera';
    const btnStopId = isAlmacen ? 'btnStopAlmacenCamera' : 'btnStopCamera';

    readerElement.style.display = 'block';
    readerElement.classList.add('active');
    document.getElementById(placeholderId).style.display = 'none';
    document.getElementById(btnStartId).disabled = true;
    document.getElementById(btnStopId).disabled = false;

    html5QrCode = new Html5Qrcode(readerId);
    html5QrCode.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        (decodedText) => {
            processScannedText(decodedText);
            // Detener cámara después de escanear para confirmar datos
            stopCamera();
        },
        (error) => { }
    ).catch(err => {
        showToast('Error al acceder a la cámara. Revisa permisos.');
        stopCamera();
    });
}

function stopCamera() {
    if (html5QrCode) {
        html5QrCode.stop().then(() => {
            html5QrCode = null;
            // Resetear UI de ambos posibles scanners
            ['qrReader', 'almacenQrReader'].forEach(id => {
                const el = document.getElementById(id);
                if (el) {
                    el.style.display = 'none';
                    el.classList.remove('active');
                }
            });
            
            const p1 = document.getElementById('cameraPlaceholder');
            if (p1) p1.style.display = 'flex';
            const p2 = document.getElementById('almacenCameraPlaceholder');
            if (p2) p2.style.display = 'flex';

            const b1 = document.getElementById('btnActivateCamera');
            if (b1) b1.disabled = false;
            const b2 = document.getElementById('btnActivateAlmacenCamera');
            if (b2) b2.disabled = false;

            const s1 = document.getElementById('btnStopCamera');
            if (s1) s1.disabled = true;
            const s2 = document.getElementById('btnStopAlmacenCamera');
            if (s2) s2.disabled = true;
        }).catch(() => { });
    }
}

// ==================== CAMBIO DE PESTAÑAS ====================
function switchTab(tabId) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.getElementById(`seccion-${tabId}`).classList.add('active');
}

// ==================== ENVIAR PEDIDO (BACKEND VERCEL) ====================
async function createDraftOrder() {
    if (cart.length === 0 || !clientFirstName.trim() || !clientLastName.trim() || !clientEmail.trim()) {
        showToast('Completa los campos obligatorios (Nombre, Apellido y Email) y añade productos');
        return;
    }

    const btn = document.getElementById('btnCreateOrder');
    const originalText = btn.innerHTML;
    btn.disabled = true;
    showLoader('Enviando pedido...');

    const datos = {
        cliente: {
            firstname: clientFirstName.trim(),
            lastname:  clientLastName.trim(),
            email:     clientEmail.trim(),
            dui:       clientDUI.trim(),
            telefono:  clientPhone.trim()
        },
        carrito: cart.map(item => ({
            nombre:   item.product.name,
            precio:   item.product.price,
            cantidad: item.quantity,
            subtotal: item.subtotal
        }))
    };

    try {
        const response = await fetch(GOOGLE_SHEET_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-API-Key': API_KEY // Reemplaza con tu clave real
            },
            body: JSON.stringify(datos)
        });
        const result = await response.json();
        
        if (result.success) {
            showToast(' Pedido guardado en Google Sheets');
            // Limpiar todo
            cart = [];
            document.getElementById('first_name').value = '';
            document.getElementById('last_name').value = '';
            document.getElementById('email').value = '';
            document.getElementById('dui').value = '';
            document.getElementById('phone').value = '';
            clientFirstName = ''; clientLastName = ''; clientEmail = ''; clientDUI = ''; clientPhone = '';
            renderAll();
            // Switch to camera tab
            const btnCamera = document.querySelector('[data-tab="cameraTab"]');
            if(btnCamera && typeof switchTab === 'function') switchTab('cameraTab', btnCamera);
        } else {
            showToast('❌ Error del servidor: ' + (result.error || 'desconocido'));
        }
    } catch (error) {
        console.error(error);
        showToast('⚠️ Sin respuesta del servidor. Verifica tu conexión, pero los datos podrían haberse guardado. Revisa la hoja.');
    } finally {
        hideLoader();
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

// ==================== GESTIÓN DE ALMACÉN (BACKEND) ====================
let inventarioBodegas = JSON.parse(localStorage.getItem('inventarioDataBodegas')) || [];

function renderizarInventario() {
    const inventarioList = document.getElementById('inventarioList');
    const totalCount = document.getElementById('totalCount');
    if (!inventarioList) return;

    inventarioList.innerHTML = '';
    if (inventarioBodegas.length === 0) {
        inventarioList.innerHTML = '<li style="text-align:center; color:#555; font-size:14px; padding: 15px 0;">Aún no hay productos registrados.</li>';
        totalCount.textContent = '0';
        return;
    }

    const inventarioInvertido = [...inventarioBodegas].reverse();
    
    inventarioInvertido.forEach((item) => {
        const li = document.createElement('li');
        li.className = 'item-inventario';
        // 🔧 CORREGIDO: escapeHtml para prevenir XSS en datos almacenados
        li.innerHTML = `
            <div class="item-info">
                <span class="item-nombre">${escapeHtml(item.producto)}</span>
                <span class="item-bodega">Bodega ${escapeHtml(item.bodega)}</span>
            </div>
            <span class="item-cantidad">${item.cantidad} uds</span>
        `;
        inventarioList.appendChild(li);
    });
    
    totalCount.textContent = inventarioBodegas.length;
}

async function submitStock() {
    const productName = document.getElementById('almacenProductName').value;
    const quantity = document.getElementById('almacenQuantity').value;
    const bodegaRadio = document.querySelector('input[name="bodega"]:checked');
    const bodega = bodegaRadio ? bodegaRadio.value : '';

    if (!productName.trim() || !quantity || quantity <= 0) {
        showToast('⚠️ Completa el nombre y una cantidad válida');
        return;
    }

    if (!bodega || !['1','2','3','4'].includes(bodega)) {
        showToast('⚠️ Por favor, selecciona una bodega válida.');
        return;
    }

    const btn = document.querySelector('#almacenForm button');
    const originalText = btn.innerHTML;
    btn.disabled = true;
    showLoader('Registrando stock...');

    const datos = {
        tipo: 'ALMACEN',
        producto: productName.trim(),
        cantidad: parseInt(quantity),
        bodega: bodega,
        fecha: new Date().toLocaleString()
    };

    try {
        const response = await fetch(GOOGLE_SHEET_URL, {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json',
                'X-API-Key': API_KEY // Reemplaza con tu clave real
             },
            body: JSON.stringify(datos)
        });
        const result = await response.json();
        
        if (result.success) {
            showToast('✅ Stock registrado correctamente');
            
            // Agregar el nuevo item localmente
            inventarioBodegas.push({
                producto: productName.trim(),
                cantidad: parseInt(quantity),
                bodega: bodega
            });
            localStorage.setItem('inventarioDataBodegas', JSON.stringify(inventarioBodegas));
            renderizarInventario();

            document.getElementById('almacenProductName').value = '';
            document.getElementById('almacenQuantity').value = '1';
            if (bodegaRadio) bodegaRadio.checked = false;
        } else {
            showToast('❌ Error: ' + (result.error || 'desconocido'));
        }
    } catch (error) {
        console.error(error);
        showToast('⚠️ Error de conexión. Revisa el backend.');
    } finally {
        hideLoader();
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

window.saveCustomer = function() {
    if (!clientFirstName.trim() || !clientLastName.trim() || !clientEmail.trim()) {
        showToast('Completa Nombre, Apellido y Email');
        return;
    }
    showToast('Cliente guardado, procede a escanear');
    const btnCamera = document.querySelector('[data-tab="cameraTab"]');
    if(btnCamera) btnCamera.click();
}

// ==================== INICIALIZACIÓN ====================
window.addEventListener('load', async () => {
    // Primero obtener la API Key
    await obtenerApiKey();
    
    // Luego el resto de la inicialización
    const firstNameEl = document.getElementById('first_name');
    const lastNameEl = document.getElementById('last_name');
    const emailEl = document.getElementById('email');
    const duiEl = document.getElementById('dui');
    const phoneEl = document.getElementById('phone');

    if(firstNameEl) {
        firstNameEl.addEventListener('input', e => {
            clientFirstName = e.target.value;
            updateCreateButton();
        });
    }
    if(lastNameEl) {
        lastNameEl.addEventListener('input', e => {
            clientLastName = e.target.value;
            updateCreateButton();
        });
    }
    if(emailEl) {
        emailEl.addEventListener('input', e => {
            clientEmail = e.target.value;
            updateCreateButton();
        });
    }
    if(duiEl) {
        duiEl.addEventListener('input', e => {
            clientDUI = e.target.value;
            updateCreateButton();
        });
    }
    if(phoneEl) {
        phoneEl.addEventListener('input', e => {
            clientPhone = e.target.value;
            updateCreateButton();
        });
    }
    
    renderAll();
    if (typeof renderizarInventario === 'function') {
        renderizarInventario();
    }
});