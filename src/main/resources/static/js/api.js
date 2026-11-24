// Thin HTTP client over the PizzaFlow REST API.
// /api/v1/** endpoints wrap results in ApiResponse {success, message, data};
// /api/delivery/** returns raw DTOs. `api()` unwraps both to plain data.

import { state } from './state.js';

export class ApiError extends Error {
    constructor(message, status, details) {
        super(message);
        this.status = status;
        this.details = details || [];
    }
}

let onUnauthorized = () => {};
export function setUnauthorizedHandler(fn) { onUnauthorized = fn; }

function buildUrl(path, query) {
    if (!query) return path;
    const qs = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
        if (v !== undefined && v !== null && v !== '') qs.append(k, v);
    }
    const s = qs.toString();
    return s ? `${path}?${s}` : path;
}

async function request(method, path, { body, query, raw = false } = {}) {
    const headers = {};
    if (state.token) headers.Authorization = `Bearer ${state.token}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';

    const res = await fetch(buildUrl(path, query), {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (raw) {
        if (!res.ok) throw await toError(res);
        return res;
    }

    const text = await res.text();
    let json = null;
    if (text) {
        try { json = JSON.parse(text); } catch { json = null; }
    }

    if (!res.ok) {
        if (res.status === 401 && state.token) onUnauthorized();
        throw errorFromJson(json, res.status);
    }

    if (json && typeof json === 'object' && 'success' in json && 'data' in json) {
        return { data: json.data, message: json.message };
    }
    return { data: json, message: null };
}

async function toError(res) {
    let json = null;
    try { json = await res.json(); } catch { /* binary or empty */ }
    if (res.status === 401 && state.token) onUnauthorized();
    return errorFromJson(json, res.status);
}

function errorFromJson(json, status) {
    const fallback = {
        400: 'Некорректный запрос',
        401: 'Требуется вход в систему',
        403: 'Недостаточно прав для этого действия',
        404: 'Не найдено',
        409: 'Конфликт данных',
    }[status] || `Ошибка сервера (${status})`;
    return new ApiError(json?.message || fallback, status, json?.details);
}

export const http = {
    get: (p, query) => request('GET', p, { query }),
    post: (p, body, query) => request('POST', p, { body, query }),
    del: (p) => request('DELETE', p),
    download: (p, query) => request('GET', p, { query, raw: true }),
};

const V1 = '/api/v1';
const D = '/api/delivery';

// Each function returns plain data (the `data` field when wrapped).
const d = (promise) => promise.then(r => r.data);

export const api = {
    // auth
    login: (email, password) => d(http.post(`${V1}/auth/login`, { email, password })),
    register: (payload) => d(http.post(`${V1}/auth/register`, payload)),
    me: () => d(http.get(`${V1}/auth/me`)),

    // catalog
    categories: () => d(http.get(`${V1}/categories`)),
    createCategory: (p) => d(http.post(`${V1}/categories`, p)),
    products: (q) => d(http.get(`${V1}/products`, q)),
    product: (id) => d(http.get(`${V1}/products/${id}`)),
    createProduct: (p) => d(http.post(`${V1}/products`, p)),

    // orders
    orders: (q) => d(http.get(`${V1}/orders`, q)),
    order: (id) => d(http.get(`${V1}/orders/${id}`)),
    orderHistory: (id) => d(http.get(`${V1}/orders/${id}/history`)),
    createOrder: (p) => d(http.post(`${V1}/orders`, p)),
    confirmOrder: (id) => d(http.post(`${V1}/orders/${id}/confirm`)),
    cancelOrder: (id, reason) => d(http.post(`${V1}/orders/${id}/cancel`, { reason })),
    completePickup: (id) => d(http.post(`${V1}/orders/${id}/complete-pickup`)),

    // addresses
    addresses: () => d(http.get(`${V1}/addresses`)),
    createAddress: (p) => d(http.post(`${V1}/addresses`, p)),
    deleteAddress: (id) => d(http.del(`${V1}/addresses/${id}`)),

    // promo codes
    validatePromo: (code, orderAmount) => d(http.post(`${V1}/promocodes/validate`, { code, orderAmount })),
    promos: () => d(http.get(`${V1}/promocodes`)),
    createPromo: (p) => d(http.post(`${V1}/promocodes`, p)),

    // kitchen
    kitchenOrders: (status) => d(http.get(`${V1}/kitchen/orders`, { status })),
    startCooking: (id) => d(http.post(`${V1}/kitchen/orders/${id}/start`)),
    completeCooking: (id) => d(http.post(`${V1}/kitchen/orders/${id}/complete`)),
    kitchenLoad: () => d(http.get(`${V1}/kitchen/load`)),

    // inventory
    ingredients: () => d(http.get(`${V1}/inventory/ingredients`)),
    lowStock: () => d(http.get(`${V1}/inventory/low-stock`)),
    movements: (q) => d(http.get(`${V1}/inventory/movements`, q)),
    createMovement: (p) => d(http.post(`${V1}/inventory/movements`, p)),

    // delivery
    zones: () => d(http.get(`${V1}/delivery-zones`)),
    createZone: (p) => d(http.post(`${V1}/delivery-zones`, p)),
    couriers: () => d(http.get(`${V1}/couriers`)),
    availableCouriers: () => d(http.get(`${V1}/couriers/available`)),
    deliveries: (q) => d(http.get(D, q)),
    assignCourier: (id, courierId) => d(http.post(`${D}/${id}/assign`, undefined, { courierId })),
    autoAssign: (id) => d(http.post(`${D}/${id}/auto-assign`)),
    pickup: (id) => d(http.post(`${D}/${id}/pickup`)),
    completeDelivery: (id) => d(http.post(`${D}/${id}/complete`)),

    // payments
    payment: (orderId) => d(http.get(`${V1}/payments/order/${orderId}`)),
    refund: (orderId, reason) => d(http.post(`${V1}/payments/${orderId}/refund`, { reason })),

    // reports & audit
    revenue: (q) => d(http.get(`${V1}/reports/revenue`, q)),
    downloadExcel: (q) => http.download(`${V1}/reports/sales/excel`, q),
    downloadWord: (q) => http.download(`${V1}/reports/shift/word`, q),
    auditLogs: () => d(http.get(`${V1}/audit/logs`)),
};
