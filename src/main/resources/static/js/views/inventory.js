// Inventory: ingredient stock (COOK read-only), low-stock filter, stock movement journal
// with filters, and new movements (MANAGER/ADMIN).

import { api } from '../api.js';
import { isStaff } from '../state.js';
import { esc, money, qty, dt, badge, UNIT, MOVEMENT_TYPE, openModal, closeModal, busy, toast, formData, spinner, empty } from '../ui.js';

const u = (unit) => UNIT[unit] || unit || '';

export default {
    title: 'Склад',
    async render(root) {
        const staff = isStaff();
        const f = { lowOnly: false, ingredientId: '', type: '' };
        let ingredients = [];

        root.innerHTML = `
            <div class="kpis" id="i-kpis"></div>
            <div class="card">
                <div class="card-head">
                    <span class="card-title">Остатки ингредиентов</span>
                    <div class="row">
                        ${staff ? '<label class="check small"><input type="checkbox" id="i-low"> Только дефицит</label>' : ''}
                        ${staff ? '<button class="btn btn-primary btn-sm" id="i-new">+ Складская операция</button>' : ''}
                    </div>
                </div>
                <div class="table-wrap" id="i-table">${spinner()}</div>
            </div>
            ${staff ? `
            <div class="card mt">
                <div class="card-head">
                    <span class="card-title">Журнал движений</span>
                    <div class="row">
                        <select class="input" id="m-ing" style="width:auto"><option value="">Все ингредиенты</option></select>
                        <select class="input" id="m-type" style="width:auto">
                            <option value="">Все операции</option>
                            ${Object.entries(MOVEMENT_TYPE).map(([k, [l]]) => `<option value="${k}">${l}</option>`).join('')}
                        </select>
                    </div>
                </div>
                <div class="table-wrap" id="m-table">${spinner()}</div>
            </div>` : '<div class="hint mt">Журнал движений и складские операции доступны менеджеру и администратору.</div>'}
        `;

        const renderTable = async () => {
            const box = root.querySelector('#i-table');
            try {
                ingredients = await api.ingredients();
                const rows = f.lowOnly ? await api.lowStock() : ingredients;
                const low = ingredients.filter(i => i.lowStock).length;
                const value = ingredients.reduce((s, i) => s + Number(i.stockQuantity) * Number(i.costPerUnit), 0);
                root.querySelector('#i-kpis').innerHTML = `
                    <div class="kpi"><div class="kpi-label">Позиций на складе</div><div class="kpi-val">${ingredients.length}</div></div>
                    <div class="kpi"><div class="kpi-label">В дефиците</div><div class="kpi-val" style="color:${low ? 'var(--danger)' : 'inherit'}">${low}</div></div>
                    <div class="kpi"><div class="kpi-label">Оценка остатков</div><div class="kpi-val">${money(value)}</div></div>`;

                box.innerHTML = rows.length ? `
                    <table class="table">
                        <thead><tr><th>Ингредиент</th><th class="right">Остаток</th><th class="right">Мин. порог</th><th class="right">Цена за ед.</th><th>Версия</th><th>Статус</th>${staff ? '<th></th>' : ''}</tr></thead>
                        <tbody>${rows.map(i => `
                            <tr class="${i.lowStock ? 'low' : ''}">
                                <td><b>${esc(i.name)}</b></td>
                                <td class="right num">${qty(i.stockQuantity)} ${u(i.unit)}</td>
                                <td class="right num muted">${qty(i.minStockThreshold)} ${u(i.unit)}</td>
                                <td class="right num">${money(i.costPerUnit)}</td>
                                <td class="mono small muted">v${i.version ?? 0}</td>
                                <td>${i.lowStock ? '<span class="badge b-danger">Дефицит</span>' : '<span class="badge b-ok">Норма</span>'}</td>
                                ${staff ? `<td class="right nowrap">
                                    <button class="btn btn-sm" data-mv="${i.id}" data-t="RECEIPT">+ Приход</button>
                                    <button class="btn btn-sm btn-ghost" data-hist="${i.id}" title="История движений">История</button>
                                </td>` : ''}
                            </tr>`).join('')}
                        </tbody>
                    </table>` : empty(f.lowOnly ? 'Дефицита нет 🎉' : 'Ингредиентов нет');

                box.querySelectorAll('[data-mv]').forEach(b => b.onclick = () => movementForm(Number(b.dataset.mv), b.dataset.t));
                box.querySelectorAll('[data-hist]').forEach(b => b.onclick = () => {
                    f.ingredientId = b.dataset.hist;
                    root.querySelector('#m-ing').value = f.ingredientId;
                    renderMovements();
                    root.querySelector('#m-table').scrollIntoView({ behavior: 'smooth', block: 'start' });
                });

                if (staff) {
                    const sel = root.querySelector('#m-ing');
                    sel.innerHTML = '<option value="">Все ингредиенты</option>' +
                        ingredients.map(i => `<option value="${i.id}" ${String(i.id) === String(f.ingredientId) ? 'selected' : ''}>${esc(i.name)}</option>`).join('');
                }
            } catch (err) {
                box.innerHTML = empty(err.message);
            }
        };

        const renderMovements = async () => {
            if (!staff) return;
            const box = root.querySelector('#m-table');
            try {
                let list = await api.movements({ ingredientId: f.ingredientId, type: f.ingredientId ? '' : f.type });
                if (f.type) list = list.filter(m => m.movementType === f.type);
                list = list.slice(0, 200);
                box.innerHTML = list.length ? `
                    <table class="table">
                        <thead><tr><th>Время</th><th>Операция</th><th>Ингредиент</th><th class="right">Кол-во</th><th class="right">Остаток после</th><th>Основание</th><th>Автор</th></tr></thead>
                        <tbody>${list.map(m => {
                            const sign = m.movementType === 'RECEIPT' ? '+' : m.movementType === 'ADJUSTMENT' ? '=' : '−';
                            return `<tr>
                                <td class="nowrap small">${dt(m.createdAt)}</td>
                                <td>${badge(MOVEMENT_TYPE, m.movementType)}</td>
                                <td>${esc(m.ingredientName)}</td>
                                <td class="right num">${sign}${qty(m.quantity)} ${u(m.unit)}</td>
                                <td class="right num">${qty(m.balanceAfter)} ${u(m.unit)}</td>
                                <td class="small">${esc(m.reason || '')}${m.referenceType ? ` <span class="muted">(${esc(m.referenceType)} ${esc(m.referenceId || '')})</span>` : ''}</td>
                                <td class="small muted">${esc(m.actor || '')}</td>
                            </tr>`;
                        }).join('')}</tbody>
                    </table>` : empty('Движений нет');
            } catch (err) {
                box.innerHTML = empty(err.message);
            }
        };

        const movementForm = (ingredientId, type = 'RECEIPT') => {
            const body = openModal('Складская операция', `
                <form class="stack">
                    <label class="field"><span>Ингредиент *</span>
                        <select class="input" name="ingredientId" required>
                            ${ingredients.map(i => `<option value="${i.id}" ${i.id === ingredientId ? 'selected' : ''}>${esc(i.name)} — остаток ${qty(i.stockQuantity)} ${u(i.unit)}</option>`).join('')}
                        </select>
                    </label>
                    <label class="field"><span>Тип операции *</span>
                        <select class="input" name="movementType" required>
                            ${Object.entries(MOVEMENT_TYPE).map(([k, [l]]) => `<option value="${k}" ${k === type ? 'selected' : ''}>${l}</option>`).join('')}
                        </select>
                    </label>
                    <div class="hint" id="mv-hint"></div>
                    <label class="field"><span>Количество *</span><input class="input" name="quantity" type="number" min="0.01" step="0.01" required></label>
                    <label class="field"><span>Основание / причина</span><input class="input" name="reason" placeholder="Например: накладная №123"></label>
                    <div class="form-grid">
                        <label class="field"><span>Тип документа</span><input class="input" name="referenceType" placeholder="INVOICE"></label>
                        <label class="field"><span>Номер документа</span><input class="input" name="referenceId"></label>
                    </div>
                    <button class="btn btn-primary" type="submit">Провести</button>
                </form>`);
            const form = body.querySelector('form');
            const hint = () => {
                const t = form.movementType.value;
                body.querySelector('#mv-hint').textContent = {
                    RECEIPT: 'Поступление увеличит остаток на указанное количество.',
                    CONSUMPTION: 'Расход уменьшит остаток на указанное количество.',
                    WASTE: 'Списание брака уменьшит остаток на указанное количество.',
                    ADJUSTMENT: 'Корректировка (инвентаризация) установит остаток равным указанному количеству.',
                }[t];
            };
            form.movementType.onchange = hint;
            hint();
            form.onsubmit = (e) => {
                e.preventDefault();
                const d = formData(form);
                d.ingredientId = Number(d.ingredientId);
                busy(e.submitter, async () => {
                    const m = await api.createMovement(d);
                    toast(`${m.ingredientName}: остаток ${qty(m.balanceAfter)} ${u(m.unit)}`, 'ok');
                    closeModal();
                    await renderTable();
                    renderMovements();
                });
            };
        };

        if (staff) {
            root.querySelector('#i-low').onchange = e => { f.lowOnly = e.target.checked; renderTable(); };
            root.querySelector('#i-new').onclick = () => movementForm(null, 'RECEIPT');
            root.querySelector('#m-ing').onchange = e => { f.ingredientId = e.target.value; renderMovements(); };
            root.querySelector('#m-type').onchange = e => { f.type = e.target.value; renderMovements(); };
        }

        await renderTable();
        await renderMovements();
    },
};
