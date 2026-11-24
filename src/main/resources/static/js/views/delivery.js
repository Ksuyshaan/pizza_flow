// Delivery: deliveries board with status filter, manual/auto courier assignment (MANAGER/ADMIN),
// pickup/complete (COURIER, own deliveries), couriers + FIFO queue, delivery zones.

import { api } from '../api.js';
import { state, isStaff, hasRole } from '../state.js';
import {
    esc, money, time, sinceMin, badge, DELIVERY_STATUS, VEHICLE,
    openModal, closeModal, busy, toast, formData, spinner, empty,
} from '../ui.js';
import { showDetails } from './orders.js';

export default {
    title: 'Доставка',
    autoRefreshMs: 15000,
    async render(root) {
        const staff = isStaff();
        const courier = hasRole('COURIER');
        const f = { status: '', mine: courier };
        let couriers = [];

        root.innerHTML = `
            <div class="grid-3-1">
                <div class="card">
                    <div class="card-head">
                        <span class="card-title">Доставки</span>
                        <div class="row">
                            ${courier ? `<div class="seg">
                                <button class="seg-btn active" data-mine="1">Мои</button>
                                <button class="seg-btn" data-mine="0">Все</button>
                            </div>` : ''}
                            <select class="input" id="d-status" style="width:auto">
                                <option value="">Все статусы</option>
                                ${Object.entries(DELIVERY_STATUS).map(([k, [l]]) => `<option value="${k}">${l}</option>`).join('')}
                            </select>
                            <button class="btn btn-sm" id="d-refresh">↻</button>
                        </div>
                    </div>
                    <div id="d-list">${spinner()}</div>
                </div>
                <div class="stack">
                    <div class="card">
                        <div class="card-head"><span class="card-title">Курьеры</span><span class="badge" id="c-count"></span></div>
                        <div id="c-list">${spinner()}</div>
                    </div>
                    <div class="card">
                        <div class="card-head">
                            <span class="card-title">Зоны доставки</span>
                            ${staff ? '<button class="btn btn-sm" id="z-new">+ Зона</button>' : ''}
                        </div>
                        <div id="z-list"></div>
                    </div>
                </div>
            </div>`;

        const loadCouriers = async () => {
            const box = root.querySelector('#c-list');
            try {
                const [all, queue] = await Promise.all([
                    api.couriers(),
                    staff ? api.availableCouriers() : Promise.resolve(null),
                ]);
                couriers = all || [];
                const order = new Map((queue || []).map((c, i) => [c.id, i + 1]));
                root.querySelector('#c-count').textContent = `${couriers.filter(c => c.available).length} / ${couriers.length} свободно`;
                const sorted = [...couriers].sort((a, b) =>
                    (b.available - a.available) || (new Date(a.availableSince) - new Date(b.availableSince)));
                box.innerHTML = sorted.length ? `<div class="list">${sorted.map(c => `
                    <div class="list-item">
                        <span class="avatar">${esc((c.fullName || '?').charAt(0))}</span>
                        <div class="grow">
                            <div style="font-weight:600">${esc(c.fullName)}${c.userId === state.user.id ? ' <span class="badge b-accent">это вы</span>' : ''}</div>
                            <div class="muted small">${esc(VEHICLE[c.vehicleType] || c.vehicleType || '')} · ${esc(c.email || '')}</div>
                        </div>
                        <div class="right">
                            ${c.available ? `<span class="badge b-ok">${order.has(c.id) ? `#${order.get(c.id)} в очереди` : 'Свободен'}</span>` : '<span class="badge b-warn">На заказе</span>'}
                            ${c.available && c.availableSince ? `<div class="muted small">${sinceMin(c.availableSince)}</div>` : ''}
                        </div>
                    </div>`).join('')}</div>` : `<div class="empty">Курьеров нет.<div class="small mt">Профили курьеров не созданы в базе — назначение доставок недоступно.</div></div>`;
            } catch (err) {
                box.innerHTML = empty(err.message);
            }
        };

        const loadZones = async () => {
            try { state.zones = await api.zones(); } catch { /* keep cached */ }
            root.querySelector('#z-list').innerHTML = state.zones.length ? `<div class="list">${state.zones.map(z => `
                <div class="list-item">
                    <div class="grow">
                        <div style="font-weight:600">${esc(z.name)}</div>
                        <div class="muted small">~${z.estimatedMinutes} мин · бесплатно от ${money(z.freeDeliveryThreshold)}</div>
                    </div>
                    <span class="num" style="font-weight:600">${money(z.deliveryFee)}</span>
                </div>`).join('')}</div>` : empty('Зон нет');
        };

        const loadDeliveries = async (silent) => {
            const box = root.querySelector('#d-list');
            if (!silent) box.innerHTML = spinner();
            try {
                const q = { status: f.status };
                if (f.mine) q.courierUserId = state.user.id;
                const list = (await api.deliveries(q)).sort((a, b) => b.id - a.id);
                box.innerHTML = list.length ? `<div class="list">${list.map(deliveryRow).join('')}</div>`
                    : empty(f.mine ? 'Вам пока не назначено доставок' : 'Доставок нет');
                bind(box);
            } catch (err) {
                box.innerHTML = empty(err.status === 404 && f.mine
                    ? 'Профиль курьера для вашего аккаунта не найден. Попросите менеджера создать его.'
                    : err.message);
            }
        };

        const deliveryRow = (d) => {
            const mine = couriers.find(c => c.id === d.courierId)?.userId === state.user.id || f.mine;
            const btns = [];
            if (staff && d.status === 'PENDING_ASSIGNMENT') {
                btns.push(`<button class="btn btn-primary btn-sm" data-auto="${d.id}">Авто (FIFO)</button>`);
                btns.push(`<button class="btn btn-sm" data-assign="${d.id}">Назначить…</button>`);
            }
            if (staff && d.status === 'ASSIGNED') btns.push(`<button class="btn btn-sm" data-assign="${d.id}">Переназначить</button>`);
            if (courier && mine && d.status === 'ASSIGNED') btns.push(`<button class="btn btn-primary btn-sm" data-pickup="${d.id}">Забрал заказ</button>`);
            if (courier && mine && (d.status === 'IN_TRANSIT' || d.status === 'ASSIGNED')) btns.push(`<button class="btn btn-ok btn-sm" data-complete="${d.id}">Доставлено</button>`);
            return `
                <div class="list-item" style="align-items:flex-start">
                    <div class="grow stack" style="gap:4px">
                        <div class="row">
                            <button class="btn btn-ghost btn-sm mono" data-order="${d.orderId}" style="padding:0;height:auto;font-weight:700;color:var(--text)">${esc(d.orderNumber)}</button>
                            ${badge(DELIVERY_STATUS, d.status)}
                        </div>
                        <div class="small">${esc(d.deliveryNotes || '')}</div>
                        <div class="muted small">
                            Курьер: <b>${esc(d.courierName || '—')}</b>
                            ${d.assignedAt ? ` · назначен ${time(d.assignedAt)}` : ''}
                            ${d.pickedUpAt ? ` · забран ${time(d.pickedUpAt)}` : ''}
                            ${d.deliveredAt ? ` · доставлен ${time(d.deliveredAt)}` : ''}
                        </div>
                    </div>
                    <div class="row" style="justify-content:flex-end">${btns.join('')}</div>
                </div>`;
        };

        const bind = (box) => {
            box.querySelectorAll('[data-order]').forEach(b => b.onclick = () => showDetails(Number(b.dataset.order)));
            box.querySelectorAll('[data-auto]').forEach(b => b.onclick = () => busy(b, async () => {
                const d = await api.autoAssign(b.dataset.auto);
                toast(d.courierName ? `Назначен ${d.courierName}` : 'Свободных курьеров нет — доставка ждёт в очереди', d.courierName ? 'ok' : 'info');
                refresh();
            }));
            box.querySelectorAll('[data-assign]').forEach(b => b.onclick = () => assignDialog(Number(b.dataset.assign)));
            box.querySelectorAll('[data-pickup]').forEach(b => b.onclick = () => busy(b, async () => {
                await api.pickup(b.dataset.pickup);
                toast('Заказ в пути', 'ok');
                refresh();
            }));
            box.querySelectorAll('[data-complete]').forEach(b => b.onclick = () => busy(b, async () => {
                await api.completeDelivery(b.dataset.complete);
                toast('Доставка завершена, платёж проведён', 'ok');
                refresh();
            }));
        };

        const assignDialog = (deliveryId) => {
            if (!couriers.length) { toast('Нет ни одного курьера', 'error'); return; }
            const body = openModal('Назначить курьера', `
                <form class="stack">
                    <label class="field"><span>Курьер</span>
                        <select class="input" name="courierId" required>
                            ${couriers.map(c => `<option value="${c.id}" ${c.available ? '' : 'disabled'}>${esc(c.fullName)} — ${esc(VEHICLE[c.vehicleType] || '')} ${c.available ? '' : '(занят)'}</option>`).join('')}
                        </select>
                    </label>
                    <button class="btn btn-primary" type="submit">Назначить</button>
                </form>`);
            body.querySelector('form').onsubmit = (e) => {
                e.preventDefault();
                busy(e.submitter, async () => {
                    const d = await api.assignCourier(deliveryId, e.target.courierId.value);
                    toast(`Назначен ${d.courierName}`, 'ok');
                    closeModal();
                    refresh();
                });
            };
        };

        const zoneForm = () => {
            const body = openModal('Новая зона доставки', `
                <form class="form-grid">
                    <label class="field full"><span>Название *</span><input class="input" name="name" required></label>
                    <label class="field"><span>Стоимость доставки, ₽ *</span><input class="input" name="deliveryFee" type="number" min="0" step="0.01" required></label>
                    <label class="field"><span>Бесплатно от, ₽ *</span><input class="input" name="freeDeliveryThreshold" type="number" min="0" step="0.01" required></label>
                    <label class="field"><span>Время доставки, мин *</span><input class="input" name="estimatedMinutes" type="number" min="1" value="30" required></label>
                    <button class="btn btn-primary full" type="submit">Создать</button>
                </form>`);
            body.querySelector('form').onsubmit = (e) => {
                e.preventDefault();
                busy(e.submitter, async () => {
                    const z = await api.createZone(formData(e.target));
                    toast(`Зона «${z.name}» создана`, 'ok');
                    closeModal();
                    loadZones();
                });
            };
        };

        const refresh = async (silent) => {
            await loadCouriers();
            await loadDeliveries(silent);
        };

        root.querySelector('#d-status').onchange = e => { f.status = e.target.value; loadDeliveries(); };
        root.querySelector('#d-refresh').onclick = () => refresh();
        root.querySelectorAll('[data-mine]').forEach(b => b.onclick = () => {
            root.querySelectorAll('[data-mine]').forEach(x => x.classList.toggle('active', x === b));
            f.mine = b.dataset.mine === '1';
            loadDeliveries();
        });
        if (staff) root.querySelector('#z-new').onclick = zoneForm;

        loadZones();
        await refresh();
        return { refresh };
    },
};
