// Reports (MANAGER/ADMIN): revenue KPIs for a period, category breakdown,
// Excel sales export for the period and Word shift report for a date.

import { api } from '../api.js';
import { esc, money, qty, busy, toast, saveBlob, spinner, empty } from '../ui.js';

const iso = (d) => d.toISOString().slice(0, 10);

export default {
    title: 'Отчёты',
    async render(root) {
        const today = new Date();
        const monthAgo = new Date(today.getTime() - 30 * 86400000);
        const f = { from: iso(monthAgo), to: iso(today) };

        root.innerHTML = `
            <div class="card card-pad" style="margin-bottom:16px">
                <div class="row-between">
                    <div class="row">
                        <label class="field"><span>С</span><input class="input" type="date" id="r-from" value="${f.from}"></label>
                        <label class="field"><span>По</span><input class="input" type="date" id="r-to" value="${f.to}"></label>
                        <div class="row" style="align-self:flex-end">
                            <button class="btn btn-sm" data-preset="0">Сегодня</button>
                            <button class="btn btn-sm" data-preset="7">7 дней</button>
                            <button class="btn btn-sm" data-preset="30">30 дней</button>
                        </div>
                    </div>
                    <div class="row" style="align-self:flex-end">
                        <button class="btn" id="r-excel">📊 Продажи в Excel</button>
                        <input class="input" type="date" id="r-shift" value="${f.to}" style="width:auto" title="Дата смены">
                        <button class="btn" id="r-word">📄 Отчёт смены (Word)</button>
                    </div>
                </div>
            </div>
            <div id="r-body">${spinner()}</div>`;

        // Period boundaries as Instants: start of "from" day .. end of "to" day in local time.
        const period = () => ({
            startDate: new Date(`${f.from}T00:00:00`).toISOString(),
            endDate: new Date(`${f.to}T23:59:59.999`).toISOString(),
        });

        const load = async () => {
            const box = root.querySelector('#r-body');
            box.innerHTML = spinner();
            try {
                const r = await api.revenue(period());
                const cats = r.categoryBreakdown || [];
                const maxRev = Math.max(1, ...cats.map(c => Number(c.revenue)));
                box.innerHTML = `
                    <div class="kpis">
                        <div class="kpi"><div class="kpi-label">Выручка</div><div class="kpi-val">${money(r.totalRevenue)}</div></div>
                        <div class="kpi"><div class="kpi-label">Средний чек</div><div class="kpi-val">${money(r.averageCheck)}</div></div>
                        <div class="kpi"><div class="kpi-label">Заказов всего</div><div class="kpi-val">${r.totalOrdersCount}</div></div>
                        <div class="kpi"><div class="kpi-label">Завершено</div><div class="kpi-val" style="color:var(--ok)">${r.completedOrdersCount}</div></div>
                        <div class="kpi"><div class="kpi-label">Отменено</div><div class="kpi-val" style="color:var(--danger)">${r.cancelledOrdersCount}</div></div>
                        <div class="kpi"><div class="kpi-label">Скидки</div><div class="kpi-val">${money(r.totalDiscounts)}</div></div>
                        <div class="kpi"><div class="kpi-label">Доставка</div><div class="kpi-val">${money(r.totalDeliveryFees)}</div></div>
                    </div>
                    <div class="card">
                        <div class="card-head"><span class="card-title">Продажи по категориям</span></div>
                        ${cats.length ? `<div class="table-wrap"><table class="table">
                            <thead><tr><th>Категория</th><th class="right">Продано, шт</th><th class="right">Выручка</th><th style="width:35%">Доля</th></tr></thead>
                            <tbody>${cats.map(c => `
                                <tr>
                                    <td><b>${esc(c.categoryName)}</b></td>
                                    <td class="right num">${c.itemsSold}</td>
                                    <td class="right num">${money(c.revenue)}</td>
                                    <td><div class="row" style="flex-wrap:nowrap"><div class="bar grow"><i style="width:${(Number(c.revenue) / maxRev) * 100}%"></i></div><span class="small num" style="width:48px;text-align:right">${qty(c.percentageShare)}%</span></div></td>
                                </tr>`).join('')}
                            </tbody></table></div>` : empty('За период продаж нет (учитываются завершённые заказы)')}
                    </div>`;
            } catch (err) {
                box.innerHTML = empty(err.message);
            }
        };

        const download = async (btn, call, name) => busy(btn, async () => {
            const res = await call();
            saveBlob(await res.blob(), name);
            toast(`Файл ${name} скачан`, 'ok');
        });

        root.querySelector('#r-from').onchange = e => { f.from = e.target.value; load(); };
        root.querySelector('#r-to').onchange = e => { f.to = e.target.value; load(); };
        root.querySelectorAll('[data-preset]').forEach(b => b.onclick = () => {
            const days = Number(b.dataset.preset);
            f.to = iso(new Date());
            f.from = iso(new Date(Date.now() - days * 86400000));
            root.querySelector('#r-from').value = f.from;
            root.querySelector('#r-to').value = f.to;
            load();
        });
        root.querySelector('#r-excel').onclick = (e) =>
            download(e.currentTarget, () => api.downloadExcel(period()), `pizzaflow-sales-${f.from}_${f.to}.xlsx`);
        root.querySelector('#r-word').onclick = (e) => {
            const date = root.querySelector('#r-shift').value;
            download(e.currentTarget, () => api.downloadWord({ date }), `pizzaflow-shift-${date || 'today'}.docx`);
        };

        await load();
    },
};
