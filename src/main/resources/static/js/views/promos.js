// Promo codes (MANAGER/ADMIN): list with usage, create, and a validation sandbox.

import { api } from '../api.js';
import { esc, money, dt, openModal, closeModal, busy, toast, formData, spinner, empty } from '../ui.js';

const toLocalInput = (d) => {
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

function promoState(p) {
    const now = Date.now();
    if (!p.active) return ['Выключен', 'b-danger'];
    if (p.validFrom && new Date(p.validFrom) > now) return ['Ещё не начался', 'b-info'];
    if (p.validTo && new Date(p.validTo) < now) return ['Истёк', 'b-danger'];
    if (p.usageLimit != null && p.timesUsed >= p.usageLimit) return ['Лимит исчерпан', 'b-warn'];
    return ['Активен', 'b-ok'];
}

export default {
    title: 'Промокоды',
    async render(root) {
        root.innerHTML = `
            <div class="grid-3-1">
                <div class="card">
                    <div class="card-head">
                        <span class="card-title">Все промокоды</span>
                        <button class="btn btn-primary btn-sm" id="p-new">+ Промокод</button>
                    </div>
                    <div class="table-wrap" id="p-table">${spinner()}</div>
                </div>
                <div class="card card-pad stack">
                    <div class="card-title">Проверка промокода</div>
                    <form class="stack" id="p-check">
                        <label class="field"><span>Код</span><input class="input" name="code" required style="text-transform:uppercase"></label>
                        <label class="field"><span>Сумма заказа, ₽</span><input class="input" name="orderAmount" type="number" min="0" step="0.01" value="1000" required></label>
                        <button class="btn" type="submit">Проверить</button>
                    </form>
                    <div id="p-check-res"></div>
                </div>
            </div>`;

        const load = async () => {
            const box = root.querySelector('#p-table');
            try {
                const list = await api.promos();
                box.innerHTML = list.length ? `
                    <table class="table">
                        <thead><tr><th>Код</th><th>Скидка</th><th class="right">Мин. заказ</th><th class="right">Макс. скидка</th><th>Использовано</th><th>Период</th><th>Статус</th></tr></thead>
                        <tbody>${list.map(p => {
                            const [l, c] = promoState(p);
                            const used = p.usageLimit ? Math.min(100, (p.timesUsed / p.usageLimit) * 100) : 0;
                            return `<tr>
                                <td class="mono"><b>${esc(p.code)}</b></td>
                                <td>${p.discountType === 'PERCENTAGE' ? `${Number(p.discountValue)}%` : money(p.discountValue)}</td>
                                <td class="right num">${money(p.minOrderAmount)}</td>
                                <td class="right num">${p.maxDiscountAmount != null ? money(p.maxDiscountAmount) : '—'}</td>
                                <td class="nowrap">
                                    <div class="small num">${p.timesUsed ?? 0}${p.usageLimit != null ? ` / ${p.usageLimit}` : ' / ∞'}</div>
                                    ${p.usageLimit ? `<div class="bar"><i style="width:${used}%"></i></div>` : ''}
                                </td>
                                <td class="small nowrap">${dt(p.validFrom)}<br>${dt(p.validTo)}</td>
                                <td><span class="badge ${c}">${l}</span></td>
                            </tr>`;
                        }).join('')}</tbody>
                    </table>` : empty('Промокодов нет');
            } catch (err) {
                box.innerHTML = empty(err.message);
            }
        };

        root.querySelector('#p-check').onsubmit = (e) => {
            e.preventDefault();
            const d = formData(e.target);
            const out = root.querySelector('#p-check-res');
            busy(e.submitter, async () => {
                const r = await api.validatePromo(d.code.toUpperCase(), d.orderAmount);
                out.innerHTML = r.valid
                    ? `<div class="hint" style="background:var(--ok-soft);color:var(--ok)">✓ ${esc(r.message)}<br>Скидка: <b>${money(r.discountAmount)}</b> · к оплате: <b>${money(r.finalAmount)}</b></div>`
                    : `<div class="hint warn">✕ ${esc(r.message)}</div>`;
            });
        };

        root.querySelector('#p-new').onclick = () => {
            const now = new Date();
            const end = new Date(now.getTime() + 30 * 86400000);
            const body = openModal('Новый промокод', `
                <form class="form-grid">
                    <label class="field"><span>Код *</span><input class="input" name="code" required style="text-transform:uppercase" placeholder="SUMMER25"></label>
                    <label class="field"><span>Тип скидки *</span>
                        <select class="input" name="discountType">
                            <option value="PERCENTAGE">Процент от суммы</option>
                            <option value="FIXED">Фиксированная сумма</option>
                        </select>
                    </label>
                    <label class="field"><span>Размер скидки *</span><input class="input" name="discountValue" type="number" min="0.01" step="0.01" required></label>
                    <label class="field"><span>Мин. сумма заказа, ₽ *</span><input class="input" name="minOrderAmount" type="number" min="0" step="0.01" value="0" required></label>
                    <label class="field"><span>Макс. скидка, ₽</span><input class="input" name="maxDiscountAmount" type="number" min="0" step="0.01" placeholder="без ограничения"></label>
                    <label class="field"><span>Лимит использований</span><input class="input" name="usageLimit" type="number" min="1" placeholder="без ограничения"></label>
                    <label class="field"><span>Действует с</span><input class="input" name="validFrom" type="datetime-local" value="${toLocalInput(now)}"></label>
                    <label class="field"><span>Действует до</span><input class="input" name="validTo" type="datetime-local" value="${toLocalInput(end)}"></label>
                    <button class="btn btn-primary full" type="submit">Создать</button>
                </form>`);
            body.querySelector('form').onsubmit = (e) => {
                e.preventDefault();
                const d = formData(e.target);
                d.code = d.code.toUpperCase();
                d.validFrom = d.validFrom ? new Date(d.validFrom).toISOString() : undefined;
                d.validTo = d.validTo ? new Date(d.validTo).toISOString() : undefined;
                busy(e.submitter, async () => {
                    const p = await api.createPromo(d);
                    toast(`Промокод ${p.code} создан`, 'ok');
                    closeModal();
                    load();
                });
            };
        };

        await load();
    },
};
