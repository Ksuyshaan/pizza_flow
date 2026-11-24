// Global client state + small persistence layer (token and cart survive reloads).

const store = {
    get(key) {
        try { return JSON.parse(localStorage.getItem(key)); } catch { return null; }
    },
    set(key, value) {
        try {
            if (value === null || value === undefined) localStorage.removeItem(key);
            else localStorage.setItem(key, JSON.stringify(value));
        } catch { /* storage unavailable */ }
    },
};

export const state = {
    token: store.get('pf.token'),
    user: null,
    cart: store.get('pf.cart') || [], // [{ productId, name, price, quantity }]
    zones: [],
    categories: [],
};

export function setToken(token) {
    state.token = token;
    store.set('pf.token', token);
}

export function saveCart() {
    store.set('pf.cart', state.cart);
}

export const ROLE_LABELS = {
    CUSTOMER: 'Клиент',
    COOK: 'Повар',
    COURIER: 'Курьер',
    MANAGER: 'Менеджер',
    ADMIN: 'Администратор',
};

export function hasRole(...roles) {
    return !!state.user && roles.includes(state.user.role);
}

export const isStaff = () => hasRole('MANAGER', 'ADMIN');
