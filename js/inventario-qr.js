// ==================== CONFIGURACIÓN DEL BACKEND ====================
const GOOGLE_SHEET_URL = '/api/submit';
const API_TOKEN = 'inventario2024seguro';// El mismo que pusiste en api/index.py

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
let clientFirstname = '', clientLastname = '', clientEmail = '', clientDUI = '', clientPhone = '';
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
    const totalDisplay = document.getElementById('totalPriceDisplay');

    if (cart.length === 0) {
        container.innerHTML = '<div class="empty-cart">No hay productos aún. Escanea un código para empezar.</div>';
        countSpan.textContent = '(0)';
        totalRow.style.display = 'none';
        return;
    }
    countSpan.textContent = `(${cart.length})`;
    totalRow.style.display = 'flex';

    let html = '';
    cart.forEach((item, index) => {
        html += `
            <div class="cart-item">
                <div class="item-info">
                    <div class="item-name">${escapeHtml(item.product.name)}</div>
                    <div class="item-price">
                        $<input type="number" step="0.01" min="0" 
                               class="price-input" 
                               value="${item.product.price.toFixed(2)}"
                               oninput="updatePrice(${index}, this.value)">
                        c/u
                    </div>
                </div>
                <div class="quantity-controls">
                    <button class="qty-btn" onclick="changeQuantity(${index}, -1)">−</button>
                    <span style="min-width:20px; text-align:center;">${item.quantity}</span>
                    <button class="qty-btn" onclick="changeQuantity(${index}, 1)">+</button>
                </div>
                <button class="remove-btn" onclick="removeItem(${index})">🗑️</button>
            </div>
        `;
    });
    container.innerHTML = html;

    const total = cart.reduce((sum, item) => sum + item.subtotal, 0);
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
        clientFirstname.trim() !== '' &&
        clientLastname.trim()  !== '' &&
        clientDUI.trim()       !== '' &&
        clientPhone.trim()     !== '';
    btn.disabled = !(cart.length > 0 && clientValid);
}

// ==================== CARRITO ====================
function changeQuantity(index, delta) {
    if (index < 0 || index >= cart.length) return;
    const item = cart[index];
    const newQty = item.quantity + delta;
    if (newQty <= 0) {
        showToast(`❌ ${item.product.name} eliminado`);
        cart.splice(index, 1);
    } else {
        item.quantity = newQty;
        showToast(`🔄 ${item.product.name} (x${newQty})`);
    }
    renderAll();
}

function removeItem(index) {
    if (index < 0 || index >= cart.length) return;
    const item = cart[index];
    showToast(`❌ ${item.product.name} eliminado`);
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

function addProduct(product) {
    const existingIndex = cart.findIndex(item => item.product.id === product.id);
    if (existingIndex !== -1) {
        cart[existingIndex].quantity += 1;
        showToast(`➕ ${product.name} (x${cart[existingIndex].quantity})`);
    } else {
        cart.push(new CartItem(product, 1));
        showToast(`🆕 ${product.name} agregado`);
    }
    flashEffect();
    renderAll();
}

// ==================== CÁMARA REAL ====================
function startCamera() {
    if (html5QrCode) stopCamera();
    document.getElementById('qrReader').classList.add('active');
    document.getElementById('cameraPlaceholder').style.display = 'none';
    document.getElementById('btnActivateCamera').disabled = true;
    document.getElementById('btnStopCamera').disabled = false;

    html5QrCode = new Html5Qrcode("qrReader");
    html5QrCode.start(
        { facingMode: "environment" },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        (decodedText) => {
            processScannedText(decodedText);
            if (html5QrCode) {
                html5QrCode.stop().then(() => {
                    html5QrCode = null;
                    document.getElementById('btnActivateCamera').disabled = false;
                    document.getElementById('btnStopCamera').disabled = true;
                    document.getElementById('qrReader').classList.remove('active');
                    document.getElementById('cameraPlaceholder').style.display = 'flex';
                });
            }
        },
        (error) => { }
    ).catch(err => {
        showToast('Error al acceder a la cámara. Revisa permisos.');
        stopCamera();
    });
}

function stopCamera() {
    if (html5QrCode) {
        html5QrCode.stop().then(() => html5QrCode = null).catch(() => { });
    }
    document.getElementById('qrReader').classList.remove('active');
    document.getElementById('cameraPlaceholder').style.display = 'flex';
    document.getElementById('btnActivateCamera').disabled = false;
    document.getElementById('btnStopCamera').disabled = true;
}

// ==================== CAMBIO DE PESTAÑAS ====================
function switchTab(tabId) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.getElementById(`seccion-${tabId}`).classList.add('active');
}

// ==================== ENVIAR PEDIDO (BACKEND VERCEL) ====================
async function createDraftOrder() {
    if (cart.length === 0 || 
        !clientFirstname.trim() || !clientLastname.trim() ||
        !clientDUI.trim()      || !clientPhone.trim()) {
        showToast('Completa los campos obligatorios y añade productos');
        return;
    }

    const btn = document.getElementById('btnCreateOrder');
    const originalText = btn.textContent;
    btn.textContent = '⏳ Enviando...';
    btn.disabled = true;

    const datos = {
        cliente: {
            firstname: clientFirstname.trim(),
            lastname:  clientLastname.trim(),
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
                'x-api-token': API_TOKEN
            },
            body: JSON.stringify(datos)
        });
        const result = await response.json();
        
        if (result.success) {
            showToast('✅ Pedido guardado en Google Sheets');
            // Limpiar todo
            cart = [];
            document.getElementById('clientFirstname').value = '';
            document.getElementById('clientLastname').value = '';
            document.getElementById('clientEmail').value = '';
            document.getElementById('clientPassword').value = '';
            document.getElementById('clientConfirmPassword').value = '';
            clientFirstname = ''; clientLastname = '';
            clientEmail = ''; clientDUI = ''; clientPhone = '';
            renderAll();
            document.querySelector('input[value="camara"]').checked = true;
            switchTab('camara');
        } else {
            showToast('❌ Error del servidor: ' + (result.error || 'desconocido'));
        }
    } catch (error) {
        console.error(error);
        showToast('⚠️ Sin respuesta del servidor. Verifica tu conexión, pero los datos podrían haberse guardado. Revisa la hoja.');
    } finally {
        btn.textContent = originalText;
        btn.disabled = false;
    }
}

// ==================== INICIALIZACIÓN ====================
window.addEventListener('load', () => {
    document.getElementById('clientFirstname').addEventListener('input', e => {
        clientFirstname = e.target.value;
        updateCreateButton();
    });
    document.getElementById('clientLastname').addEventListener('input', e => {
        clientLastname = e.target.value;
        updateCreateButton();
    });
    document.getElementById('clientEmail').addEventListener('input', e => {
        clientEmail = e.target.value;
        updateCreateButton();
    });
    document.getElementById('clientPassword').addEventListener('input', e => {
        clientDUI = e.target.value;
        updateCreateButton();
    });
    document.getElementById('clientConfirmPassword').addEventListener('input', e => {
        clientPhone = e.target.value;
        updateCreateButton();
    });
    renderAll();
});