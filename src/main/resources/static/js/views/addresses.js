// Saved delivery addresses: list, create (bound to a delivery zone), delete.
// Also shows the delivery zones/tariffs reference.

import { api } from '../api.js';
import { state } from '../state.js';
import { esc, money, openModal, closeModal, busy, toast, formData, spinner, empty } from '../ui.js';

export default {
    title: 'Мои адреса',
    async render(root) {
        root.innerHTML = `
            <div class="grid-3-1">
                <div class="card">
                    <div class="card-head">
                        <span class="card-title">Адреса доставки</span>
                        <button class="btn btn-primary btn-sm" id="a-new">+ Добавить адрес</button>
                    </div>
                    <div id="a-list">${spinner()}</div>
                </div>
                <div class="card">
                    <div class="card-head"><span class="card-title">Зоны и тарифы</span></div>
                    <div class="list">${state.zones.map(z => `
                        <div class="list-item">
                            <div class="grow">
                                <div style="font-weight:600">${esc(z.name)}</div>
                                <div class="muted small">~${z.estimatedMinutes} мин · бесплатно от ${money(z.freeDeliveryThreshold)}</div>
                            </div>
                            <span class="num" style="font-weight:600">${money(z.deliveryFee)}</span>
                        </div>`).join('') || empty('Зоны не настроены')}
                    </div>
                </div>
            </div>`;

        const load = async () => {
            const list = root.querySelector('#a-list');
            try {
                const items = await api.addresses();
                if (!items.length) { list.innerHTML = empty('Сохранённых адресов нет — добавьте первый, чтобы быстрее оформлять доставку.'); return; }
                list.innerHTML = `<div class="list">${items.map(a => `
                    <div class="list-item">
                        <div class="grow">
                            <div style="font-weight:600">${esc(a.formatted)} ${a.default ? '<span class="badge b-accent">по умолчанию</span>' : ''}</div>
                            <div class="muted small">Зона: ${esc(a.deliveryZoneName || '—')}</div>
                        </div>
                        <button class="btn btn-sm btn-danger" data-del="${a.id}">Удалить</button>
                    </div>`).join('')}</div>`;
                list.querySelectorAll('[data-del]').forEach(b => b.onclick = () => busy(b, async () => {
                    await api.deleteAddress(b.dataset.del);
                    toast('Адрес удалён', 'ok');
                    load();
                }));
            } catch (err) {
                list.innerHTML = empty(err.message);
            }
        };

        root.querySelector('#a-new').onclick = () => {
            const body = openModal('Новый адрес', `
                <form class="form-grid">
                    <label class="field full"><span>Зона доставки *</span>
                        <select class="input" name="deliveryZoneId" required>
                            ${state.zones.map(z => `<option value="${z.id}">${esc(z.name)} — ${money(z.deliveryFee)}</option>`).join('')}
                        </select>
                    </label>
                    <label class="field"><span>Город *</span><input class="input" name="city" required value="Москва"></label>
                    <label class="field"><span>Улица *</span><input class="input" name="street" required></label>
                    <label class="field"><span>Дом *</span><input class="input" name="houseNumber" required></label>
                    <label class="field"><span>Квартира</span><input class="input" name="apartment"></label>
                    <label class="field"><span>Подъезд</span><input class="input" name="entrance"></label>
                    <label class="field"><span>Этаж</span><input class="input" name="floor"></label>
                    <label class="check full"><input type="checkbox" name="default"> Сделать адресом по умолчанию</label>
                    <button class="btn btn-primary full" type="submit">Сохранить</button>
                </form>`);
            body.querySelector('form').onsubmit = (e) => {
                e.preventDefault();
                const d = formData(e.target);
                d.deliveryZoneId = Number(d.deliveryZoneId);
                busy(e.submitter, async () => {
                    await api.createAddress(d);
                    toast('Адрес сохранён', 'ok');
                    closeModal();
                    load();
                });
            };
        };

        await load();
    },
};
