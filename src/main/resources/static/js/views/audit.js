// Audit log (MANAGER/ADMIN): recent business events with client-side filtering.

import { api } from '../api.js';
import { esc, dt, spinner, empty } from '../ui.js';

const ACTION_CLS = (a) =>
    /CANCEL|REFUND|FAIL|ERROR|WASTE/.test(a) ? 'b-danger'
    : /LOGIN|REGISTER/.test(a) ? 'b-info'
    : /STOCK|INVENTORY|MOVEMENT|COOK/.test(a) ? 'b-warn'
    : /DELIVER|COURIER/.test(a) ? 'b-violet'
    : /CREATED|PAID|COMPLETE/.test(a) ? 'b-ok' : '';

export default {
    title: 'Аудит',
    autoRefreshMs: 30000,
    async render(root) {
        let logs = [];
        const f = { action: '', q: '' };

        root.innerHTML = `
            <div class="toolbar">
                <select class="input" id="a-action" style="width:auto"><option value="">Все действия</option></select>
                <input class="input grow" id="a-q" placeholder="Поиск по пользователю или описанию…" style="min-width:200px">
                <button class="btn btn-sm" id="a-refresh">↻ Обновить</button>
            </div>
            <div class="card" id="a-list">${spinner()}</div>`;

        const render = () => {
            const q = f.q.toLowerCase();
            const list = logs.filter(l =>
                (!f.action || l.action === f.action) &&
                (!q || `${l.actor} ${l.details} ${l.role}`.toLowerCase().includes(q)));
            root.querySelector('#a-list').innerHTML = list.length ? list.map(l => `
                <div class="feed-item">
                    <div class="row">
                        <span class="badge ${ACTION_CLS(l.action || '')}">${esc(l.action)}</span>
                        <span class="grow"></span>
                        <span class="muted small nowrap">${dt(l.timestamp)}</span>
                    </div>
                    <div>${esc(l.details)}</div>
                    <div class="muted small">${esc(l.actor)} · ${esc(l.role)}</div>
                </div>`).join('') : empty('Событий нет');
        };

        const load = async () => {
            try {
                logs = await api.auditLogs() || [];
                const actions = [...new Set(logs.map(l => l.action))].sort();
                const sel = root.querySelector('#a-action');
                sel.innerHTML = '<option value="">Все действия</option>' +
                    actions.map(a => `<option ${a === f.action ? 'selected' : ''}>${esc(a)}</option>`).join('');
                render();
            } catch (err) {
                root.querySelector('#a-list').innerHTML = empty(err.message);
            }
        };

        root.querySelector('#a-action').onchange = e => { f.action = e.target.value; render(); };
        root.querySelector('#a-q').oninput = e => { f.q = e.target.value.trim(); render(); };
        root.querySelector('#a-refresh').onclick = load;

        await load();
        return { refresh: load };
    },
};
