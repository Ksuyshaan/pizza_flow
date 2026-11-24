// Orders: list with status filter + pagination, status-flow stepper, role-based actions,
// detail modal with items, status history, payment info and refund.

import { api } from '../api.js';
import { state, isStaff } from '../state.js';
import {
    esc, money, dt, badge, ORDER_STATUS, PAYMENT_STATUS, PAYMENT_METHOD,
    openModal, promptDialog, busy, toast, spinner, empty, pager,
} from '../ui.js';

const FLOW = {
    DELIVERY: ['NEW', 'CONFIRMED', 'COOKING', 'READY', 'DELIVERING', 'COMPLETED'],
    PICKUP: ['NEW', 'CONFIRMED', 'COOKING', 'READY', 'COMPLETED'],
};

export default {
    title: 'Заказы',
    autoRefreshMs: 20000,
    async render(root) {
        const f = { status: '', page: 0 };
        const customer = state.user.role === 'CUSTOMER';

        root.innerHTML = `
            <div class="toolbar">
                <div class="chips" id="o-status">
                    <button class="chip active" data-st="">Все</button>
                    ${Object.entries(ORDER_STATUS).map(([k, [l]]) => `<button class="chip" data-st="${k}">${l}</button>`).join('')}
                </div>
                <span class="grow"></span>
                <button class="btn btn-sm" id="o-refresh">↻ Обновить</button>
            </div>
            ${customer ? '' : '<div class="hint" style="margin-bottom:12px">Отмена возможна только в статусах «Новый» и «Подтверждён» — после начала готовки ингредиенты уже списаны. Самовывоз выдаётся из статуса «Готов».</div>'}
            <div id="o-list">${spinner()}</div>
        `;

        const load = async (silent) => {
            const list = root.querySelector('#o-list');
            if (!silent) list.innerHTML = spinner();
            try {
                const res = await api.orders({ status: f.status, page: f.page, size: 10 });
                const items = res.content || [];
                if (!items.length) { list.innerHTML = empty(customer ? 'У вас пока нет заказов' : 'Заказов нет'); return; }
                list.innerHTML = `
                    <div class="muted small" style="margin-bottom:10px">Всего: ${res.totalElements}</div>
                    <div class="orders">${items.map(orderCard).join('')}</div>
                    ${pager(res.page, res.totalPages)}`;
                bind(list);
            } catch (err) {
                list.innerHTML = empty(err.message);
            }
        };

        const bind = (list) => {
            list.querySelectorAll('[data-page]').forEach(b => b.onclick = () => { f.page = Number(b.dataset.page); load(); });
            list.querySelectorAll('[data-details]').forEach(b => b.onclick = () => showDetails(Number(b.dataset.details), load));
            list.querySelectorAll('[data-confirm]').forEach(b => b.onclick = () => busy(b, async () => {
                const o = await api.confirmOrder(b.dataset.confirm);
                toast(`Заказ ${o.orderNumber} подтверждён и передан на кухню`, 'ok');
                window.dispatchEvent(new Event('pf:refresh-load'));
                load(true);
            }));
            list.querySelectorAll('[data-pickup]').forEach(b => b.onclick = () => busy(b, async () => {
                const o = await api.completePickup(b.dataset.pickup);
                toast(`Заказ ${o.orderNumber} выдан клиенту`, 'ok');
                load(true);
            }));
            list.querySelectorAll('[data-cancel]').forEach(b => b.onclick = () => cancelFlow(Number(b.dataset.cancel), () => load(true)));
        };

        root.querySelectorAll('[data-st]').forEach(b => b.onclick = () => {
            root.querySelectorAll('[data-st]').forEach(x => x.classList.toggle('active', x === b));
            f.status = b.dataset.st; f.page = 0; load();
        });
        root.querySelector('#o-refresh').onclick = () => load();

        await load();
        return { refresh: load };
    },
};

function stepper(o) {
    if (o.status === 'CANCELLED') {
        return `<div class="hint warn">Отменён ${dt(o.cancelledAt)}. Причина: ${esc(o.cancellationReason || 'не указана')}</div>`;
    }
    const steps = FLOW[o.orderType] || FLOW.DELIVERY;
    const idx = steps.indexOf(o.status);
    return `<div class="stepper">${steps.map((s, i) => `
        ${i ? `<span class="step-line ${i <= idx ? 'done' : ''}"></span>` : ''}
        <span class="step ${i < idx || o.status === 'COMPLETED' ? 'done' : ''} ${i === idx && o.status !== 'COMPLETED' ? 'now' : ''}">
            <span class="d">${i < idx || o.status === 'COMPLETED' ? '✓' : i + 1}</span>${esc(ORDER_STATUS[s][0])}
        </span>`).join('')}
    </div>`;
}

function actions(o) {
    const staff = isStaff();
    const out = [];
    if (staff && o.status === 'NEW') out.push(`<button class="btn btn-primary btn-sm" data-confirm="${o.id}">✓ Подтвердить</button>`);
    if (staff && o.status === 'READY' && o.orderType === 'PICKUP') out.push(`<button class="btn btn-ok btn-sm" data-pickup="${o.id}">Выдать клиенту</button>`);
    if (o.status === 'NEW' || o.status === 'CONFIRMED') out.push(`<button class="btn btn-danger btn-sm" data-cancel="${o.id}">Отменить</button>`);
    out.push(`<button class="btn btn-sm" data-details="${o.id}">Подробнее</button>`);
    return out.join('');
}

function orderCard(o) {
    return `
        <div class="order">
            <div class="order-head">
                <span class="order-num">${esc(o.orderNumber)}</span>
                ${badge(ORDER_STATUS, o.status)}
                <span class="badge">${o.orderType === 'DELIVERY' ? '🛵 Доставка' : '🛍️ Самовывоз'}</span>
                <span class="grow"></span>
                <span class="muted small">${dt(o.createdAt)}</span>
            </div>
            ${stepper(o)}
            <div class="small">
                <div>${(o.items || []).map(i => `${esc(i.productName)} × ${i.quantity}`).join(', ')}</div>
                <div class="muted">${esc(o.deliveryAddress || '')}${o.deliveryZoneName ? ` · зона «${esc(o.deliveryZoneName)}»` : ''}${state.user.role !== 'CUSTOMER' ? ` · клиент: ${esc(o.userFullName || o.userEmail)}` : ''}</div>
            </div>
            <div class="order-foot">
                <div class="row small">
                    <span class="price">${money(o.totalAmount)}</span>
                    ${Number(o.discountAmount) ? `<span class="badge b-ok">−${money(o.discountAmount)}${o.promoCode ? ` · ${esc(o.promoCode)}` : ''}</span>` : ''}
                    ${Number(o.deliveryFee) ? `<span class="muted">доставка ${money(o.deliveryFee)}</span>` : ''}
                </div>
                <div class="row">${actions(o)}</div>
            </div>
        </div>`;
}

async function cancelFlow(id, onDone) {
    const reason = await promptDialog('Отмена заказа', {
        label: 'Причина отмены', placeholder: 'Например: клиент передумал', required: true, okText: 'Отменить заказ', danger: true,
    });
    if (!reason) return;
    await busy(null, async () => {
        const o = await api.cancelOrder(id, reason);
        toast(`Заказ ${o.orderNumber} отменён, платёж возвращён`, 'ok');
        window.dispatchEvent(new Event('pf:refresh-load'));
        onDone();
    });
}

export async function showDetails(id, onChange) {
    const body = openModal('Заказ', spinner(), { wide: true });
    try {
        const [o, history, payment] = await Promise.all([
            api.order(id),
            api.orderHistory(id),
            api.payment(id).catch(() => null),
        ]);
        document.getElementById('modal-title').textContent = `Заказ ${o.orderNumber}`;
        body.innerHTML = `
            <div class="stack">
                <div class="row">${badge(ORDER_STATUS, o.status)}<span class="badge">${o.orderType === 'DELIVERY' ? '🛵 Доставка' : '🛍️ Самовывоз'}</span></div>
                ${stepper(o)}
                <div class="grid-2">
                    <dl class="kv">
                        <dt>Клиент</dt><dd>${esc(o.userFullName || '')} <span class="muted">${esc(o.userEmail || '')}</span></dd>
                        <dt>Адрес</dt><dd>${esc(o.deliveryAddress || '—')}</dd>
                        ${o.deliveryZoneName ? `<dt>Зона</dt><dd>${esc(o.deliveryZoneName)}</dd>` : ''}
                        <dt>Создан</dt><dd>${dt(o.createdAt)}</dd>
                        <dt>Подтверждён</dt><dd>${dt(o.confirmedAt)}</dd>
                        <dt>Завершён</dt><dd>${dt(o.completedAt)}</dd>
                        ${o.cancelledAt ? `<dt>Отменён</dt><dd>${dt(o.cancelledAt)} — ${esc(o.cancellationReason || '')}</dd>` : ''}
                    </dl>
                    <div class="card card-pad stack" style="gap:8px">
                        <div class="row-between"><span class="section-label">Оплата</span>${payment ? badge(PAYMENT_STATUS, payment.paymentStatus) : ''}</div>
                        ${payment ? `
                            <dl class="kv">
                                <dt>Способ</dt><dd>${esc(PAYMENT_METHOD[payment.paymentMethod] || payment.paymentMethod)}</dd>
                                <dt>Сумма</dt><dd>${money(payment.amount)}</dd>
                                <dt>Оплачен</dt><dd>${dt(payment.paidAt)}</dd>
                                ${payment.transactionReference ? `<dt>Транзакция</dt><dd class="mono small">${esc(payment.transactionReference)}</dd>` : ''}
                            </dl>
                            ${isStaff() && payment.paymentStatus !== 'REFUNDED' ? '<button class="btn btn-danger btn-sm" id="d-refund">Оформить возврат</button>' : ''}
                        ` : '<div class="muted small">Платёж не найден</div>'}
                    </div>
                </div>

                <table class="table">
                    <thead><tr><th>Позиция</th><th class="right">Цена</th><th class="right">Кол-во</th><th class="right">Сумма</th></tr></thead>
                    <tbody>
                        ${(o.items || []).map(i => `<tr><td>${esc(i.productName)}</td><td class="right num">${money(i.unitPrice)}</td><td class="right num">${i.quantity}</td><td class="right num">${money(i.lineTotal)}</td></tr>`).join('')}
                        <tr><td colspan="3" class="right muted">Подытог</td><td class="right num">${money(o.subtotal)}</td></tr>
                        ${Number(o.discountAmount) ? `<tr><td colspan="3" class="right muted">Скидка ${esc(o.promoCode || '')}</td><td class="right num">−${money(o.discountAmount)}</td></tr>` : ''}
                        <tr><td colspan="3" class="right muted">Доставка</td><td class="right num">${money(o.deliveryFee)}</td></tr>
                        <tr><td colspan="3" class="right"><b>Итого</b></td><td class="right num"><b>${money(o.totalAmount)}</b></td></tr>
                    </tbody>
                </table>

                <div>
                    <div class="section-label" style="margin-bottom:10px">История статусов</div>
                    ${history?.length ? `<div class="timeline">${history.map(h => `
                        <div class="tl-item">
                            <div><b>${esc(h.fromStatus ? `${ORDER_STATUS[h.fromStatus]?.[0] || h.fromStatus} → ` : '')}${esc(ORDER_STATUS[h.toStatus]?.[0] || h.toStatus)}</b>
                                <span class="muted small">${dt(h.changedAt)}</span></div>
                            <div class="small">${esc(h.comment || '')}</div>
                            <div class="muted small">${esc(h.changedByUsername || '')} · ${esc(h.changedByRole || '')}</div>
                        </div>`).join('')}</div>` : '<div class="muted small">Нет записей</div>'}
                </div>
            </div>`;

        const refund = body.querySelector('#d-refund');
        if (refund) refund.onclick = async () => {
            const reason = await promptDialog('Возврат платежа', {
                label: 'Причина возврата', value: 'Ручной возврат менеджером', okText: 'Вернуть деньги', danger: true,
            });
            if (reason === null) { showDetails(id, onChange); return; }
            await busy(null, async () => {
                await api.refund(o.id, reason || undefined);
                toast('Возврат оформлен', 'ok');
                onChange?.(true);
                showDetails(id, onChange);
            });
        };
    } catch (err) {
        body.innerHTML = empty(err.message);
    }
}
