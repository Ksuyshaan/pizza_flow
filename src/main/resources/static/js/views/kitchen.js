// Kitchen board: PENDING → IN_PROGRESS → COMPLETED tickets, start/complete cooking,
// plus live kitchen load metrics.

import { api } from '../api.js';
import { esc, time, sinceMin, busy, toast, spinner, empty } from '../ui.js';
import { showDetails } from './orders.js';

const COLS = [
    ['PENDING', '⏳ В очереди'],
    ['IN_PROGRESS', '🔥 Готовится'],
    ['COMPLETED', '✓ Готово'],
];

export default {
    title: 'Кухня',
    autoRefreshMs: 15000,
    async render(root) {
        root.innerHTML = `
            <div class="kpis" id="k-load"></div>
            <div class="toolbar">
                <span class="muted small">При старте готовки ингредиенты списываются со склада по рецептуре. Обновляется автоматически.</span>
                <span class="grow"></span>
                <button class="btn btn-sm" id="k-refresh">↻ Обновить</button>
            </div>
            <div class="kanban" id="k-board">${spinner()}</div>`;

        const load = async () => {
            try {
                const [tickets, l] = await Promise.all([api.kitchenOrders(), api.kitchenLoad()]);
                const lvl = { LOW: ['Низкая', 'b-ok'], MEDIUM: ['Средняя', 'b-warn'], HIGH: ['Высокая', 'b-danger'] }[l.loadLevel] || [l.loadLevel, ''];
                root.querySelector('#k-load').innerHTML = `
                    <div class="kpi"><div class="kpi-label">Нагрузка</div><div class="kpi-val"><span class="badge ${lvl[1]}" style="font-size:14px">${lvl[0]}</span></div></div>
                    <div class="kpi"><div class="kpi-label">В очереди</div><div class="kpi-val">${l.pendingOrders}</div></div>
                    <div class="kpi"><div class="kpi-label">Готовится</div><div class="kpi-val">${l.inProgressOrders}</div></div>
                    <div class="kpi"><div class="kpi-label">Ожидание нового заказа</div><div class="kpi-val">~${l.estimatedWaitMinutes} мин</div></div>`;

                const byStatus = (s) => tickets
                    .filter(t => t.status === s)
                    .sort((a, b) => s === 'COMPLETED'
                        ? new Date(b.completedAt) - new Date(a.completedAt)
                        : (b.priority - a.priority) || (a.id - b.id));

                root.querySelector('#k-board').innerHTML = COLS.map(([s, label]) => {
                    let list = byStatus(s);
                    const total = list.length;
                    if (s === 'COMPLETED') list = list.slice(0, 10);
                    return `
                        <div class="col">
                            <div class="col-head"><span>${label}</span><span class="badge">${total}</span></div>
                            ${list.length ? list.map(ticket).join('') : '<div class="muted small" style="padding:6px">Пусто</div>'}
                            ${s === 'COMPLETED' && total > 10 ? `<div class="muted small" style="padding:0 6px">и ещё ${total - 10}…</div>` : ''}
                        </div>`;
                }).join('');

                root.querySelectorAll('[data-start]').forEach(b => b.onclick = () => busy(b, async () => {
                    const t = await api.startCooking(b.dataset.start);
                    toast(`${t.orderNumber}: готовка начата, сырьё списано`, 'ok');
                    window.dispatchEvent(new Event('pf:refresh-load'));
                    load();
                }));
                root.querySelectorAll('[data-done]').forEach(b => b.onclick = () => busy(b, async () => {
                    const t = await api.completeCooking(b.dataset.done);
                    toast(`${t.orderNumber}: готово`, 'ok');
                    window.dispatchEvent(new Event('pf:refresh-load'));
                    load();
                }));
                root.querySelectorAll('[data-order]').forEach(b => b.onclick = () => showDetails(Number(b.dataset.order)));
            } catch (err) {
                root.querySelector('#k-board').innerHTML = empty(err.message);
            }
        };

        root.querySelector('#k-refresh').onclick = load;
        await load();
        return { refresh: load };
    },
};

function ticket(t) {
    return `
        <div class="ticket">
            <div class="row-between">
                <button class="btn btn-ghost btn-sm mono" data-order="${t.orderId}" style="padding:0;height:auto;font-weight:700;color:var(--text)">${esc(t.orderNumber)}</button>
                ${t.priority > 0 ? `<span class="badge b-danger">приоритет ${t.priority}</span>` : ''}
            </div>
            <ul>${(t.items || []).map(i => `<li><b>${esc(i.productName)}</b> × ${i.quantity}</li>`).join('')}</ul>
            <div class="muted small">
                ~${t.estimatedCookingMinutes} мин
                ${t.cookUsername ? ` · ${esc(t.cookUsername)}` : ''}
                ${t.startedAt ? ` · начато ${time(t.startedAt)} (${sinceMin(t.startedAt)})` : ''}
                ${t.completedAt ? ` · готово ${time(t.completedAt)}` : ''}
            </div>
            ${t.status === 'PENDING' ? `<button class="btn btn-primary btn-sm btn-block" data-start="${t.id}">Начать готовить</button>` : ''}
            ${t.status === 'IN_PROGRESS' ? `<button class="btn btn-ok btn-sm btn-block" data-done="${t.id}">Готово</button>` : ''}
        </div>`;
}
