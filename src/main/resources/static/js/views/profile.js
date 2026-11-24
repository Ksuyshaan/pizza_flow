// Current user profile (GET /auth/me).

import { api } from '../api.js';
import { ROLE_LABELS } from '../state.js';
import { esc, spinner, empty } from '../ui.js';

export default {
    title: 'Профиль',
    async render(root) {
        root.innerHTML = spinner();
        try {
            const me = await api.me();
            root.innerHTML = `
                <div class="card card-pad" style="max-width:520px">
                    <div class="row" style="margin-bottom:16px">
                        <span class="avatar" style="width:48px;height:48px;font-size:20px">${esc((me.fullName || '?').charAt(0))}</span>
                        <div>
                            <h2>${esc(me.fullName)}</h2>
                            <span class="badge b-accent">${esc(ROLE_LABELS[me.role] || me.role)}</span>
                        </div>
                    </div>
                    <dl class="kv">
                        <dt>ID</dt><dd class="mono">${me.id}</dd>
                        <dt>Email</dt><dd>${esc(me.email)}</dd>
                        <dt>Телефон</dt><dd>${esc(me.phone || '—')}</dd>
                    </dl>
                </div>`;
        } catch (err) {
            root.innerHTML = empty(err.message);
        }
    },
};
