// Cart drawer + checkout (POST /orders) with saved addresses, zones and promo validation.

import { api } from '../api.js';
import { state, saveCart } from '../state.js';
import { esc, money, toast, toastError, busy } from '../ui.js';

const checkout = {
    orderType: 'DELIVERY',
    addressMode: 'saved', // saved | custom
    addressId: null,
    zoneId: null,
    customAddress: '',
    paymentMethod: 'ONLINE',
    notes: '',
    promoInput: '',
    promo: null, // { code, discountAmount, message }
    promoError: '',
    addresses: [],
};

const subtotal = () => state.cart.reduce((s, i) => s + i.price * i.quantity, 0);
const count = () => state.cart.reduce((s, i) => s + i.quantity, 0);

export function updateCartButton() {
    document.getElementById('cart-count').textContent = count();
    document.getElementById('cart-sum').textContent = money(subtotal());
}

export function addToCart(product, quantity = 1) {
    const line = state.cart.find(l => l.productId === product.id);
    if (line) line.quantity += quantity;
    else state.cart.push({ productId: product.id, name: product.name, price: Number(product.price), quantity });
    saveCart();
    updateCartButton();
    revalidatePromo();
    toast(`«${product.name}» в корзине`, 'ok');
}

function setQty(productId, delta) {
    const line = state.cart.find(l => l.productId === productId);
    if (!line) return;
    line.quantity += delta;
    if (line.quantity <= 0) state.cart = state.cart.filter(l => l !== line);
    saveCart();
    updateCartButton();
    revalidatePromo();
    render();
}

let promoTimer = null;
function revalidatePromo() {
    if (!checkout.promo) return;
    clearTimeout(promoTimer);
    promoTimer = setTimeout(() => applyPromo(checkout.promo.code, true), 300);
}

async function applyPromo(code, silent = false) {
    checkout.promoError = '';
    if (!code) { checkout.promo = null; render(); return; }
    try {
        const r = await api.validatePromo(code, subtotal());
        if (r.valid) {
            checkout.promo = { code: r.code, discountAmount: Number(r.discountAmount), message: r.message };
            if (!silent) toast(`Промокод ${r.code}: скидка ${money(r.discountAmount)}`, 'ok');
        } else {
            checkout.promo = null;
            checkout.promoError = r.message || 'Промокод недействителен';
        }
    } catch (err) {
        checkout.promo = null;
        checkout.promoError = err.message;
    }
    render();
}

function selectedZone() {
    if (checkout.orderType !== 'DELIVERY') return null;
    if (checkout.addressMode === 'saved') {
        const a = checkout.addresses.find(a => a.id === checkout.addressId);
        return a ? state.zones.find(z => z.id === a.deliveryZoneId) || null : null;
    }
    return state.zones.find(z => z.id === checkout.zoneId) || null;
}

function deliveryFee() {
    const z = selectedZone();
    if (!z) return 0;
    return subtotal() >= Number(z.freeDeliveryThreshold) ? 0 : Number(z.deliveryFee);
}

export async function openCart() {
    document.getElementById('cart-backdrop').hidden = false;
    render();
    try {
        checkout.addresses = await api.addresses() || [];
        if (!checkout.addresses.find(a => a.id === checkout.addressId)) {
            const def = checkout.addresses.find(a => a.default) || checkout.addresses[0];
            checkout.addressId = def ? def.id : null;
        }
        if (!checkout.addresses.length) checkout.addressMode = 'custom';
        if (!checkout.zoneId && state.zones[0]) checkout.zoneId = state.zones[0].id;
        render();
    } catch (err) { toastError(err); }
}

function closeCart() {
    document.getElementById('cart-backdrop').hidden = true;
}

function render() {
    const el = document.getElementById('cart-drawer');
    if (document.getElementById('cart-backdrop').hidden) return;
    const sub = subtotal();
    const disc = checkout.promo ? checkout.promo.discountAmount : 0;
    const fee = deliveryFee();
    const zone = selectedZone();
    const total = Math.max(0, sub - disc + fee);
    const isDelivery = checkout.orderType === 'DELIVERY';

    el.innerHTML = `
        <div class="drawer-head">
            <h3>Корзина</h3>
            <button class="icon-btn" data-close type="button" aria-label="Закрыть">✕</button>
        </div>
        <div class="drawer-body">
            ${state.cart.length ? `
                <div>${state.cart.map(l => `
                    <div class="cart-line">
                        <div class="grow">
                            <div style="font-weight:600">${esc(l.name)}</div>
                            <div class="muted small">${money(l.price)} × ${l.quantity} = ${money(l.price * l.quantity)}</div>
                        </div>
                        <div class="qty">
                            <button type="button" data-dec="${l.productId}" aria-label="Меньше">−</button>
                            <span>${l.quantity}</span>
                            <button type="button" data-inc="${l.productId}" aria-label="Больше">+</button>
                        </div>
                    </div>`).join('')}
                </div>

                <div class="stack">
                    <div class="section-label">Получение</div>
                    <div class="seg">
                        <button type="button" class="seg-btn ${isDelivery ? 'active' : ''}" data-type="DELIVERY">🛵 Доставка</button>
                        <button type="button" class="seg-btn ${!isDelivery ? 'active' : ''}" data-type="PICKUP">🛍️ Самовывоз</button>
                    </div>
                    ${isDelivery ? `
                        ${checkout.addresses.length ? `
                            <div class="seg">
                                <button type="button" class="seg-btn ${checkout.addressMode === 'saved' ? 'active' : ''}" data-amode="saved">Мой адрес</button>
                                <button type="button" class="seg-btn ${checkout.addressMode === 'custom' ? 'active' : ''}" data-amode="custom">Другой адрес</button>
                            </div>` : ''}
                        ${checkout.addressMode === 'saved' && checkout.addresses.length ? `
                            <label class="field"><span>Сохранённый адрес</span>
                                <select class="input" id="ck-address">
                                    ${checkout.addresses.map(a => `<option value="${a.id}" ${a.id === checkout.addressId ? 'selected' : ''}>${esc(a.formatted)} — ${esc(a.deliveryZoneName || '')}</option>`).join('')}
                                </select>
                            </label>` : `
                            <label class="field"><span>Зона доставки</span>
                                <select class="input" id="ck-zone">
                                    ${state.zones.map(z => `<option value="${z.id}" ${z.id === checkout.zoneId ? 'selected' : ''}>${esc(z.name)} — ${money(z.deliveryFee)}, бесплатно от ${money(z.freeDeliveryThreshold)}</option>`).join('')}
                                </select>
                            </label>
                            <label class="field"><span>Адрес</span>
                                <input class="input" id="ck-custom" value="${esc(checkout.customAddress)}" placeholder="Улица, дом, квартира">
                            </label>
                            ${!checkout.addresses.length ? '<div class="hint">Адреса можно сохранить в разделе «Мои адреса».</div>' : ''}
                        `}
                        ${zone ? `<div class="hint">Зона «${esc(zone.name)}»: ~${zone.estimatedMinutes} мин доставки${fee === 0 ? ' · доставка бесплатна' : ` · до бесплатной ещё ${money(Number(zone.freeDeliveryThreshold) - sub)}`}</div>` : ''}
                    ` : '<div class="hint">Самовывоз: ул. Пиццерийная, 1</div>'}
                </div>

                <div class="stack">
                    <div class="section-label">Промокод</div>
                    <form class="row" id="ck-promo-form">
                        <input class="input grow" id="ck-promo" value="${esc(checkout.promo?.code || checkout.promoInput)}" placeholder="WELCOME10" style="text-transform:uppercase">
                        ${checkout.promo
                            ? '<button class="btn" type="button" data-promo-clear>Убрать</button>'
                            : '<button class="btn" type="submit">Применить</button>'}
                    </form>
                    ${checkout.promo ? `<div class="small" style="color:var(--ok)">✓ ${esc(checkout.promo.message)} — скидка ${money(checkout.promo.discountAmount)}</div>` : ''}
                    ${checkout.promoError ? `<div class="form-error">${esc(checkout.promoError)}</div>` : ''}
                </div>

                <div class="stack">
                    <label class="field"><span>Оплата</span>
                        <select class="input" id="ck-pay">
                            <option value="ONLINE" ${checkout.paymentMethod === 'ONLINE' ? 'selected' : ''}>💳 Онлайн</option>
                            <option value="CARD" ${checkout.paymentMethod === 'CARD' ? 'selected' : ''}>💳 Картой при получении</option>
                            <option value="CASH" ${checkout.paymentMethod === 'CASH' ? 'selected' : ''}>💵 Наличными</option>
                        </select>
                    </label>
                    <label class="field"><span>Комментарий к заказу</span>
                        <textarea class="input" id="ck-notes" rows="2" placeholder="Например: без лука, позвонить заранее">${esc(checkout.notes)}</textarea>
                    </label>
                </div>
            ` : '<div class="empty">Корзина пуста. Добавьте блюда из меню.</div>'}
        </div>
        ${state.cart.length ? `
        <div class="drawer-foot">
            <div class="sum-line"><span>Товары</span><span class="num">${money(sub)}</span></div>
            ${disc ? `<div class="sum-line disc"><span>Скидка ${esc(checkout.promo.code)}</span><span class="num">−${money(disc)}</span></div>` : ''}
            ${isDelivery ? `<div class="sum-line"><span>Доставка</span><span class="num">${fee ? money(fee) : 'бесплатно'}</span></div>` : ''}
            <div class="sum-line total"><span>Итого</span><span class="num">${money(total)}</span></div>
            <button class="btn btn-primary btn-block" type="button" id="ck-submit">Оформить заказ</button>
        </div>` : ''}
    `;

    el.querySelector('[data-close]').onclick = closeCart;
    el.querySelectorAll('[data-inc]').forEach(b => b.onclick = () => setQty(Number(b.dataset.inc), 1));
    el.querySelectorAll('[data-dec]').forEach(b => b.onclick = () => setQty(Number(b.dataset.dec), -1));
    el.querySelectorAll('[data-type]').forEach(b => b.onclick = () => { checkout.orderType = b.dataset.type; render(); });
    el.querySelectorAll('[data-amode]').forEach(b => b.onclick = () => { checkout.addressMode = b.dataset.amode; render(); });
    const on = (sel, ev, fn) => { const x = el.querySelector(sel); if (x) x.addEventListener(ev, fn); };
    on('#ck-address', 'change', e => { checkout.addressId = Number(e.target.value); render(); });
    on('#ck-zone', 'change', e => { checkout.zoneId = Number(e.target.value); render(); });
    on('#ck-custom', 'input', e => { checkout.customAddress = e.target.value; });
    on('#ck-pay', 'change', e => { checkout.paymentMethod = e.target.value; });
    on('#ck-notes', 'input', e => { checkout.notes = e.target.value; });
    on('#ck-promo', 'input', e => { checkout.promoInput = e.target.value; });
    on('#ck-promo-form', 'submit', e => { e.preventDefault(); applyPromo(checkout.promoInput.trim().toUpperCase()); });
    on('[data-promo-clear]', 'click', () => { checkout.promo = null; checkout.promoInput = ''; render(); });
    on('#ck-submit', 'click', e => submit(e.currentTarget));
}

async function submit(btn) {
    const payload = {
        orderType: checkout.orderType,
        paymentMethod: checkout.paymentMethod,
        items: state.cart.map(l => ({ productId: l.productId, quantity: l.quantity })),
        promoCode: checkout.promo?.code || null,
        notes: checkout.notes.trim() || null,
    };
    if (checkout.orderType === 'DELIVERY') {
        if (checkout.addressMode === 'saved' && checkout.addressId) {
            payload.addressId = checkout.addressId;
        } else {
            if (!checkout.zoneId) { toast('Выберите зону доставки', 'error'); return; }
            payload.deliveryZoneId = checkout.zoneId;
            payload.customAddress = checkout.customAddress.trim() || null;
        }
    }
    await busy(btn, async () => {
        const order = await api.createOrder(payload);
        toast(`Заказ ${order.orderNumber} оформлен на ${money(order.totalAmount)}`, 'ok');
        state.cart = [];
        saveCart();
        checkout.promo = null;
        checkout.promoInput = '';
        checkout.notes = '';
        updateCartButton();
        closeCart();
        window.dispatchEvent(new Event('pf:refresh-load'));
        if (location.hash === '#/orders') window.dispatchEvent(new HashChangeEvent('hashchange'));
        else location.hash = '#/orders';
    });
}

export function initCart() {
    document.getElementById('cart-backdrop').addEventListener('mousedown', (e) => {
        if (e.target.id === 'cart-backdrop') closeCart();
    });
}
