import { api, setUnauthorizedHandler } from './api.js';
import { state, setToken, ROLE_LABELS, hasRole } from './state.js';
import { esc, toast, toastError, closeModal, formData, busy } from './ui.js';
import { initCart, updateCartButton, openCart } from './views/cart.js';

import menuView from './views/menu.js';
import ordersView from './views/orders.js';
import addressesView from './views/addresses.js';
import kitchenView from './views/kitchen.js';
import inventoryView from './views/inventory.js';
import deliveryView from './views/delivery.js';
import promosView from './views/promos.js';
import reportsView from './views/reports.js';
import auditView from './views/audit.js';
import profileView from './views/profile.js';

const ALL = ['CUSTOMER', 'COOK', 'COURIER', 'MANAGER', 'ADMIN'];
const ROUTES = [
    { id: 'menu', icon: '🍕', label: 'Меню', group: 'Заказ', roles: ALL, view: menuView },
    { id: 'orders', icon: '📋', label: 'Заказы', group: 'Заказ', roles: ALL, view: ordersView },
    { id: 'addresses', icon: '📍', label: 'Мои адреса', group: 'Заказ', roles: ALL, view: addressesView },
    { id: 'kitchen', icon: '👨‍🍳', label: 'Кухня', group: 'Операции', roles: ['COOK', 'MANAGER', 'ADMIN'], view: kitchenView },
    { id: 'inventory', icon: '📦', label: 'Склад', group: 'Операции', roles: ['COOK', 'MANAGER', 'ADMIN'], view: inventoryView },
    { id: 'delivery', icon: '🛵', label: 'Доставка', group: 'Операции', roles: ['COURIER', 'MANAGER', 'ADMIN'], view: deliveryView },
    { id: 'promos', icon: '🏷️', label: 'Промокоды', group: 'Управление', roles: ['MANAGER', 'ADMIN'], view: promosView },
    { id: 'reports', icon: '📊', label: 'Отчёты', group: 'Управление', roles: ['MANAGER', 'ADMIN'], view: reportsView },
    { id: 'audit', icon: '📜', label: 'Аудит', group: 'Управление', roles: ['MANAGER', 'ADMIN'], view: auditView },
    { id: 'profile', icon: '👤', label: 'Профиль', group: null, roles: ALL, view: profileView, hidden: true },
];
const HOME = { CUSTOMER: 'menu', COOK: 'kitchen', COURIER: 'delivery', MANAGER: 'orders', ADMIN: 'orders' };

const DEMO = [
    ['customer@pizzaflow.com', 'Клиент', 'Иван Петров'],
    ['cook@pizzaflow.com', 'Повар', 'Сергей Готовцев'],
    ['courier@pizzaflow.com', 'Курьер', 'Андрей Быстров'],
    ['courier2@pizzaflow.com', 'Курьер 2', 'Мария Скорова'],
    ['manager@pizzaflow.com', 'Менеджер', 'Ольга Управлова'],
    ['admin@pizzaflow.com', 'Админ', 'Алексей Главный'],
];

let current = null; // { route, handle, timer }

// ---------------- auth ----------------
function showAuth() {
    document.getElementById('app-shell').hidden = true;
    document.getElementById('auth-screen').hidden = false;
}

async function enterApp(user) {
    state.user = user;
    document.getElementById('auth-screen').hidden = true;
    document.getElementById('app-shell').hidden = false;
    document.getElementById('user-name').textContent = user.fullName;
    document.getElementById('user-role').textContent = ROLE_LABELS[user.role] || user.role;
    document.getElementById('user-avatar').textContent = (user.fullName || '?').trim().charAt(0).toUpperCase();
    renderNav();
    try {
        const [zones, categories] = await Promise.all([api.zones(), api.categories()]);
        state.zones = zones || [];
        state.categories = categories || [];
    } catch (err) { toastError(err); }
    updateCartButton();
    refreshLoad();
    if (!location.hash || !routeFromHash()) location.hash = `#/${HOME[user.role] || 'menu'}`;
    else navigate();
}

async function login(email, password) {
    const res = await api.login(email, password);
    setToken(res.token);
    toast(`Вы вошли как ${res.user.fullName}`, 'ok');
    location.hash = '';
    await enterApp(res.user);
}

function logout() {
    stopCurrent();
    setToken(null);
    state.user = null;
    location.hash = '';
    showAuth();
}

setUnauthorizedHandler(() => {
    toast('Сессия истекла — войдите снова', 'error');
    logout();
});

function initAuthScreen() {
    document.querySelectorAll('[data-auth-tab]').forEach(btn => {
        btn.onclick = () => {
            document.querySelectorAll('[data-auth-tab]').forEach(b => b.classList.toggle('active', b === btn));
            document.getElementById('login-form').hidden = btn.dataset.authTab !== 'login';
            document.getElementById('register-form').hidden = btn.dataset.authTab !== 'register';
        };
    });

    document.getElementById('login-form').onsubmit = (e) => {
        e.preventDefault();
        const f = formData(e.target);
        busy(e.submitter, () => login(f.email, f.password));
    };

    document.getElementById('register-form').onsubmit = (e) => {
        e.preventDefault();
        const f = formData(e.target);
        busy(e.submitter, async () => {
            const res = await api.register(f);
            setToken(res.token);
            toast('Аккаунт создан', 'ok');
            location.hash = '';
            await enterApp(res.user);
        });
    };

    document.getElementById('demo-accounts').innerHTML = DEMO.map(([email, role, name]) => `
        <button class="demo-btn" type="button" data-email="${esc(email)}"><b>${esc(role)}</b><span>${esc(name)}</span></button>
    `).join('');
    document.querySelectorAll('.demo-btn').forEach(b => {
        b.onclick = () => busy(b, () => login(b.dataset.email, 'password123'));
    });
}

// ---------------- navigation ----------------
function allowedRoutes() {
    return ROUTES.filter(r => hasRole(...r.roles));
}

function renderNav() {
    const nav = document.getElementById('nav');
    let html = '';
    let group = null;
    for (const r of allowedRoutes().filter(r => !r.hidden)) {
        if (r.group !== group) {
            group = r.group;
            html += `<div class="nav-group">${esc(group)}</div>`;
        }
        html += `<button class="nav-item" data-route="${r.id}" type="button">
            <span class="ico">${r.icon}</span>${esc(r.label)}
            ${r.id === 'kitchen' ? '<span class="count" id="nav-kitchen-count" hidden></span>' : ''}
        </button>`;
    }
    nav.innerHTML = html;
    nav.querySelectorAll('[data-route]').forEach(b => {
        b.onclick = () => {
            location.hash = `#/${b.dataset.route}`;
            document.getElementById('sidebar').classList.remove('open');
        };
    });
}

function routeFromHash() {
    const id = location.hash.replace(/^#\/?/, '').split('?')[0];
    return allowedRoutes().find(r => r.id === id) || null;
}

function stopCurrent() {
    if (!current) return;
    clearInterval(current.timer);
    current.handle?.destroy?.();
    current = null;
}

async function navigate() {
    if (!state.user) return;
    const route = routeFromHash();
    if (!route) {
        location.hash = `#/${HOME[state.user.role] || 'menu'}`;
        return;
    }
    stopCurrent();
    document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.route === route.id));
    document.getElementById('page-title').textContent = route.view.title || route.label;
    const el = document.getElementById('view');
    el.innerHTML = '';
    closeModal();

    const entry = { route, handle: null, timer: null };
    current = entry;
    try {
        entry.handle = await route.view.render(el);
    } catch (err) {
        toastError(err);
    }
    if (current !== entry) return;
    if (route.view.autoRefreshMs && entry.handle?.refresh) {
        entry.timer = setInterval(() => {
            if (document.hidden || !document.getElementById('modal-backdrop').hidden) return;
            entry.handle.refresh(true);
        }, route.view.autoRefreshMs);
    }
}

// Kitchen load indicator in the top bar (public endpoint).
export async function refreshLoad() {
    try {
        const l = await api.kitchenLoad();
        const pill = document.getElementById('load-pill');
        pill.className = `load-pill ${l.loadLevel}`;
        const lvl = { LOW: 'низкая', MEDIUM: 'средняя', HIGH: 'высокая' }[l.loadLevel] || l.loadLevel;
        document.getElementById('load-text').textContent = `Кухня: ${lvl} · ~${l.estimatedWaitMinutes} мин`;
        pill.title = `В очереди: ${l.pendingOrders}, готовится: ${l.inProgressOrders}`;
        const c = document.getElementById('nav-kitchen-count');
        if (c) {
            c.hidden = !l.pendingOrders;
            c.textContent = l.pendingOrders;
        }
    } catch { /* non-critical */ }
}

// ---------------- boot ----------------
document.addEventListener('DOMContentLoaded', async () => {
    initAuthScreen();
    initCart();

    document.getElementById('logout-btn').onclick = logout;
    document.getElementById('user-chip').onclick = () => { location.hash = '#/profile'; };
    document.getElementById('cart-btn').onclick = openCart;
    document.getElementById('menu-toggle').onclick = () => document.getElementById('sidebar').classList.toggle('open');
    document.getElementById('modal-close').onclick = closeModal;
    document.getElementById('modal-backdrop').addEventListener('mousedown', (e) => {
        if (e.target.id === 'modal-backdrop') closeModal();
    });
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            if (!document.getElementById('modal-backdrop').hidden) closeModal();
            else document.getElementById('cart-backdrop').hidden = true;
        }
    });
    window.addEventListener('hashchange', navigate);
    window.addEventListener('pf:refresh-load', refreshLoad);
    setInterval(() => { if (state.user && !document.hidden) refreshLoad(); }, 30000);

    if (state.token) {
        try {
            const me = await api.me();
            await enterApp(me);
            return;
        } catch {
            setToken(null);
        }
    }
    showAuth();
});
