// UI helpers: escaping, formatting, toasts, modal dialogs, async-button wrapper.

export function esc(v) {
    if (v === null || v === undefined) return '';
    return String(v)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

const rub = new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'RUB', maximumFractionDigits: 2 });
const num = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 });
export const money = (v) => rub.format(Number(v || 0));
export const qty = (v) => num.format(Number(v || 0));

export function dt(v) {
    if (!v) return '—';
    const d = new Date(v);
    return isNaN(d) ? '—' : d.toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
}
export function time(v) {
    if (!v) return '—';
    const d = new Date(v);
    return isNaN(d) ? '—' : d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}
export function sinceMin(v) {
    if (!v) return '';
    const m = Math.round((Date.now() - new Date(v).getTime()) / 60000);
    if (m < 1) return 'только что';
    if (m < 60) return `${m} мин назад`;
    return `${Math.floor(m / 60)} ч ${m % 60} мин назад`;
}

// ---------- labels ----------
export const ORDER_STATUS = {
    NEW: ['Новый', 'b-info'],
    CONFIRMED: ['Подтверждён', 'b-violet'],
    COOKING: ['Готовится', 'b-warn'],
    READY: ['Готов', 'b-accent'],
    DELIVERING: ['В пути', 'b-info'],
    COMPLETED: ['Завершён', 'b-ok'],
    CANCELLED: ['Отменён', 'b-danger'],
};
export const DELIVERY_STATUS = {
    PENDING_ASSIGNMENT: ['Ждёт курьера', 'b-warn'],
    ASSIGNED: ['Назначен', 'b-violet'],
    IN_TRANSIT: ['В пути', 'b-info'],
    DELIVERED: ['Доставлен', 'b-ok'],
    CANCELLED: ['Отменён', 'b-danger'],
};
export const KITCHEN_STATUS = {
    PENDING: ['В очереди', 'b-warn'],
    IN_PROGRESS: ['Готовится', 'b-accent'],
    COMPLETED: ['Готово', 'b-ok'],
};
export const PAYMENT_STATUS = {
    PENDING: ['Ожидает', 'b-warn'],
    PAID: ['Оплачен', 'b-ok'],
    REFUNDED: ['Возврат', 'b-violet'],
    FAILED: ['Ошибка', 'b-danger'],
};
export const PAYMENT_METHOD = { CASH: 'Наличные', CARD: 'Карта курьеру', ONLINE: 'Онлайн' };
export const MOVEMENT_TYPE = {
    RECEIPT: ['Поступление', 'b-ok'],
    CONSUMPTION: ['Расход', 'b-info'],
    ADJUSTMENT: ['Корректировка', 'b-violet'],
    WASTE: ['Списание (брак)', 'b-danger'],
};
export const VEHICLE = { BICYCLE: 'Велосипед', SCOOTER: 'Скутер', CAR: 'Авто', ON_FOOT: 'Пешком' };
export const UNIT = { G: 'г', ML: 'мл', PCS: 'шт' };

export function badge(map, key) {
    const [label, cls] = map[key] || [key, ''];
    return `<span class="badge ${cls}">${esc(label)}</span>`;
}

// ---------- toasts ----------
export function toast(message, type = 'info', details) {
    const box = document.getElementById('toasts');
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.innerHTML = esc(message) + (details?.length ? `<ul>${details.map(d => `<li>${esc(d)}</li>`).join('')}</ul>` : '');
    box.appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity .3s'; }, 4200);
    setTimeout(() => el.remove(), 4600);
}
export const toastError = (err) => toast(err?.message || 'Ошибка', 'error', err?.details);

// ---------- modal ----------
let modalCloseHandler = null;
export function openModal(title, html, { wide = false, onClose } = {}) {
    document.getElementById('modal-title').textContent = title;
    const body = document.getElementById('modal-body');
    body.innerHTML = html;
    document.querySelector('#modal-backdrop .modal').classList.toggle('wide', wide);
    document.getElementById('modal-backdrop').hidden = false;
    modalCloseHandler = onClose || null;
    const first = body.querySelector('input, select, textarea');
    if (first) setTimeout(() => first.focus(), 30);
    return body;
}
export function closeModal() {
    document.getElementById('modal-backdrop').hidden = true;
    document.getElementById('modal-body').innerHTML = '';
    const h = modalCloseHandler; modalCloseHandler = null;
    if (h) h();
}

/** Ask for a text value in a modal. Resolves to the string, or null if cancelled. */
export function promptDialog(title, { label = '', placeholder = '', value = '', required = false, okText = 'OK', danger = false } = {}) {
    return new Promise(resolve => {
        let done = false;
        const finish = (v) => { if (!done) { done = true; resolve(v); } };
        const body = openModal(title, `
            <form class="stack" id="prompt-form">
                ${label ? `<label class="field"><span>${esc(label)}</span>` : ''}
                <input class="input" name="v" value="${esc(value)}" placeholder="${esc(placeholder)}" ${required ? 'required' : ''}>
                ${label ? '</label>' : ''}
                <div class="row" style="justify-content:flex-end">
                    <button type="button" class="btn" data-cancel>Отмена</button>
                    <button type="submit" class="btn ${danger ? 'btn-danger' : 'btn-primary'}">${esc(okText)}</button>
                </div>
            </form>`, { onClose: () => finish(null) });
        body.querySelector('[data-cancel]').onclick = () => closeModal();
        body.querySelector('form').onsubmit = (e) => {
            e.preventDefault();
            const v = e.target.v.value.trim();
            finish(v);
            closeModal();
        };
    });
}

/** Run an async action while disabling the triggering button. */
export async function busy(btn, fn) {
    if (btn) btn.disabled = true;
    try { return await fn(); }
    catch (err) { toastError(err); return undefined; }
    finally { if (btn && btn.isConnected) btn.disabled = false; }
}

export function formData(form) {
    const out = {};
    for (const el of form.elements) {
        if (!el.name) continue;
        if (el.type === 'checkbox') out[el.name] = el.checked;
        else if (el.type === 'number') out[el.name] = el.value === '' ? null : Number(el.value);
        else out[el.name] = el.value.trim() === '' ? null : el.value.trim();
    }
    return out;
}

export const spinner = () => '<div class="spinner"></div>';
export const empty = (text) => `<div class="empty">${esc(text)}</div>`;

export function pager(page, totalPages, attr = 'data-page') {
    if (!totalPages || totalPages <= 1) return '';
    return `<div class="pager">
        <button class="btn btn-sm" ${attr}="${page - 1}" ${page <= 0 ? 'disabled' : ''}>← Назад</button>
        <span class="muted small">Стр. ${page + 1} из ${totalPages}</span>
        <button class="btn btn-sm" ${attr}="${page + 1}" ${page >= totalPages - 1 ? 'disabled' : ''}>Вперёд →</button>
    </div>`;
}

export function saveBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = filename;
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}
