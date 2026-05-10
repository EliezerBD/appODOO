// ============ NAVEGACIÓN POR PESTAÑAS ============
function switchTab(tabId, buttonElement) {
    // Ocultar todas las pestañas
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });

    // Mostrar la pestaña seleccionada
    const selectedTab = document.getElementById(tabId);
    if (selectedTab) {
        selectedTab.classList.add('active');
        selectedTab.classList.add('animate-fadeIn');
    }

    // Actualizar estilos de los botones de navegación
    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('text-secondary', 'bg-secondary/10', 'rounded-xl');
        btn.classList.add('text-on-surface-variant');
        const icon = btn.querySelector('.material-symbols-outlined');
        if (icon) {
            icon.style.fontVariationSettings = "'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24";
        }
    });

    // Activar el botón clickeado
    if (buttonElement) {
        buttonElement.classList.add('text-secondary', 'bg-secondary/10', 'rounded-xl');
        buttonElement.classList.remove('text-on-surface-variant');
        const activeIcon = buttonElement.querySelector('.material-symbols-outlined');
        if (activeIcon) {
            activeIcon.style.fontVariationSettings = "'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24";
        }
    }
}

// Prevenir comportamiento por defecto en inputs para mejor UX móvil
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('input').forEach(input => {
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                input.blur();
            }
        });
    });
});
