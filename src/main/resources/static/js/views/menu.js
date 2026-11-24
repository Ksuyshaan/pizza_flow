// Catalog: server-side search/filter/sort/pagination, product details with recipe,
// and (for MANAGER/ADMIN) creating categories and products with recipes.

import { api } from '../api.js';
import { state, isStaff } from '../state.js';
import { esc, money, qty, UNIT, openModal, closeModal, busy, toast, formData, spinner, empty, pager } from '../ui.js';
import { addToCart } from './cart.js';

const PAGE_SIZE = 12;
const SORTS = [
    ['name,asc', 'По названию'],
    ['price,asc', 'Сначала дешёвые'],
    ['price,desc', 'Сначала дорогие'],
    ['preparationMinutes,asc', 'Быстрее готовятся'],
];

export default {
    title: 'Меню',
    async render(root) {
        const f = { categoryId: '', search: '', minPrice: '', maxPrice: '', sort: 'name,asc', page: 0 };
        let products = [];

        root.innerHTML = `
            <div class="filters">
                <div class="row-between">
                    <div class="chips" id="m-cats"></div>
                    ${isStaff() ? `<div class="row">
                        <button class="btn btn-sm" id="m-new-cat">+ Категория</button>
                        <button class="btn btn-sm btn-primary" id="m-new-prod">+ Блюдо</button>
                    </div>` : ''}
                </div>
                <div class="toolbar" style="margin:0">
                    <input class="input grow" id="m-search" placeholder="Поиск по названию…" style="min-width:200px">
                    <input class="input" id="m-min" type="number" min="0" placeholder="Цена от" style="width:110px">
                    <input class="input" id="m-max" type="number" min="0" placeholder="до" style="width:110px">
                    <select class="input" id="m-sort">${SORTS.map(([v, l]) => `<option value="${v}">${l}</option>`).join('')}</select>
                </div>
            </div>
            <div id="m-list">${spinner()}</div>
        `;

        const renderCats = () => {
            const cats = [...state.categories].sort((a, b) => a.displayOrder - b.displayOrder);
            root.querySelector('#m-cats').innerHTML =
                `<button class="chip ${f.categoryId === '' ? 'active' : ''}" data-cat="">Все</button>` +
                cats.map(c => `<button class="chip ${String(c.id) === String(f.categoryId) ? 'active' : ''}" data-cat="${c.id}" title="${esc(c.description || '')}">${esc(c.name)}</button>`).join('');
            root.querySelectorAll('[data-cat]').forEach(b => b.onclick = () => {
                f.categoryId = b.dataset.cat; f.page = 0; renderCats(); load();
            });
        };

        const load = async () => {
            const list = root.querySelector('#m-list');
            try {
                const res = await api.products({
                    categoryId: f.categoryId, search: f.search, minPrice: f.minPrice, maxPrice: f.maxPrice,
                    sort: f.sort, page: f.page, size: PAGE_SIZE,
                });
                products = res.content || [];
                if (!products.length) { list.innerHTML = empty('Ничего не найдено'); return; }
                list.innerHTML = `
                    <div class="muted small" style="margin-bottom:10px">Найдено: ${res.totalElements}</div>
                    <div class="products">${products.map(card).join('')}</div>
                    ${pager(res.page, res.totalPages)}`;
                list.querySelectorAll('[data-open]').forEach(el => el.onclick = (e) => {
                    if (e.target.closest('[data-add]')) return;
                    showProduct(Number(el.dataset.open));
                });
                list.querySelectorAll('[data-add]').forEach(b => b.onclick = () => {
                    const p = products.find(x => x.id === Number(b.dataset.add));
                    if (p) addToCart(p);
                });
                list.querySelectorAll('[data-page]').forEach(b => b.onclick = () => { f.page = Number(b.dataset.page); load(); window.scrollTo(0, 0); });
            } catch (err) {
                list.innerHTML = empty(err.message);
            }
        };

        let t;
        const debounced = () => { clearTimeout(t); t = setTimeout(() => { f.page = 0; load(); }, 300); };
        root.querySelector('#m-search').oninput = e => { f.search = e.target.value.trim(); debounced(); };
        root.querySelector('#m-min').oninput = e => { f.minPrice = e.target.value; debounced(); };
        root.querySelector('#m-max').oninput = e => { f.maxPrice = e.target.value; debounced(); };
        root.querySelector('#m-sort').onchange = e => { f.sort = e.target.value; f.page = 0; load(); };

        if (isStaff()) {
            root.querySelector('#m-new-cat').onclick = () => categoryForm(() => renderCats());
            root.querySelector('#m-new-prod').onclick = () => productForm(() => load());
        }

        renderCats();
        await load();
    },
};

// imageUrl may be a real URL or (in demo seed data) just an emoji.
function image(p) {
    const v = (p.imageUrl || '').trim();
    if (/^(https?:\/\/|\/|data:image\/)/i.test(v)) {
        return `<div class="ph">🍕</div><img src="${esc(v)}" alt="" loading="lazy" onerror="this.remove()">`;
    }
    return `<div class="ph">${esc(v && v.length <= 8 ? v : '🍕')}</div>`;
}

function card(p) {
    return `
        <div class="product" data-open="${p.id}">
            <div class="product-img">
                ${image(p)}
                <span class="badge">⏱ ${p.preparationMinutes} мин</span>
            </div>
            <div class="product-body">
                <div class="product-cat">${esc(p.categoryName || '')}</div>
                <div class="product-name">${esc(p.name)}</div>
                <div class="product-desc">${esc(p.description || '')}</div>
                <div class="product-foot">
                    <span class="price">${money(p.price)}</span>
                    ${p.active === false
                        ? '<span class="badge b-danger">Недоступно</span>'
                        : `<button class="btn btn-primary btn-sm" data-add="${p.id}" type="button">В корзину</button>`}
                </div>
            </div>
        </div>`;
}

async function showProduct(id) {
    const body = openModal('Блюдо', spinner(), { wide: true });
    try {
        const p = await api.product(id);
        body.innerHTML = `
            <div class="grid-2">
                <div class="product-img" style="border-radius:var(--radius);overflow:hidden">${image(p)}</div>
                <div class="stack">
                    <div class="product-cat">${esc(p.categoryName || '')}</div>
                    <h2>${esc(p.name)}</h2>
                    <div class="muted">${esc(p.description || '')}</div>
                    <dl class="kv">
                        <dt>Цена</dt><dd>${money(p.price)}</dd>
                        <dt>Время готовки</dt><dd>${p.preparationMinutes} мин</dd>
                        <dt>Статус</dt><dd>${p.active === false ? 'недоступно' : 'в продаже'}</dd>
                    </dl>
                    ${p.recipe?.length ? `
                        <div>
                            <div class="section-label" style="margin-bottom:6px">Рецептура (списывается со склада)</div>
                            <table class="table"><tbody>
                                ${p.recipe.map(r => `<tr><td>${esc(r.ingredientName)}</td><td class="right num">${qty(r.quantityRequired)} ${esc(UNIT[r.unit] || r.unit)}</td></tr>`).join('')}
                            </tbody></table>
                        </div>` : '<div class="hint">Рецептура не задана</div>'}
                    ${p.active === false ? '' : `
                    <div class="row">
                        <div class="qty"><button type="button" data-q="-1">−</button><span id="pq">1</span><button type="button" data-q="1">+</button></div>
                        <button class="btn btn-primary grow" id="p-add">Добавить в корзину</button>
                    </div>`}
                </div>
            </div>`;
        let n = 1;
        body.querySelectorAll('[data-q]').forEach(b => b.onclick = () => {
            n = Math.max(1, n + Number(b.dataset.q));
            body.querySelector('#pq').textContent = n;
        });
        const add = body.querySelector('#p-add');
        if (add) add.onclick = () => { addToCart(p, n); closeModal(); };
    } catch (err) {
        body.innerHTML = empty(err.message);
    }
}

function categoryForm(onDone) {
    const body = openModal('Новая категория', `
        <form class="stack">
            <label class="field"><span>Название *</span><input class="input" name="name" required></label>
            <label class="field"><span>Описание</span><input class="input" name="description"></label>
            <label class="field"><span>Порядок отображения</span><input class="input" name="displayOrder" type="number" value="${state.categories.length + 1}"></label>
            <label class="check"><input type="checkbox" name="active" checked> Активна</label>
            <button class="btn btn-primary" type="submit">Создать</button>
        </form>`);
    body.querySelector('form').onsubmit = (e) => {
        e.preventDefault();
        const data = formData(e.target);
        data.displayOrder = data.displayOrder ?? 0;
        busy(e.submitter, async () => {
            const c = await api.createCategory(data);
            state.categories = await api.categories();
            toast(`Категория «${c.name}» создана`, 'ok');
            closeModal();
            onDone();
        });
    };
}

async function productForm(onDone) {
    const body = openModal('Новое блюдо', spinner(), { wide: true });
    let ingredients = [];
    try { ingredients = await api.ingredients(); } catch (err) { toast(err.message, 'error'); }
    const ingOptions = ingredients.map(i => `<option value="${i.id}">${esc(i.name)} (${esc(UNIT[i.unit] || i.unit)})</option>`).join('');

    body.innerHTML = `
        <form class="stack">
            <div class="form-grid">
                <label class="field"><span>Название *</span><input class="input" name="name" required></label>
                <label class="field"><span>Категория *</span>
                    <select class="input" name="categoryId" required>${state.categories.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('')}</select>
                </label>
                <label class="field"><span>Цена, ₽ *</span><input class="input" name="price" type="number" min="0.01" step="0.01" required></label>
                <label class="field"><span>Время готовки, мин</span><input class="input" name="preparationMinutes" type="number" min="1" value="15"></label>
                <label class="field full"><span>Описание</span><textarea class="input" name="description" rows="2"></textarea></label>
                <label class="field full"><span>URL изображения</span><input class="input" name="imageUrl" placeholder="https://…"></label>
            </div>
            <div class="stack">
                <div class="row-between">
                    <div class="section-label">Рецептура</div>
                    <button class="btn btn-sm" type="button" id="r-add" ${ingredients.length ? '' : 'disabled'}>+ Ингредиент</button>
                </div>
                <div class="stack" id="r-rows" style="gap:8px"></div>
                ${ingredients.length ? '' : '<div class="hint">Список ингредиентов недоступен.</div>'}
            </div>
            <button class="btn btn-primary" type="submit">Создать блюдо</button>
        </form>`;

    const rows = body.querySelector('#r-rows');
    const addRow = () => {
        const div = document.createElement('div');
        div.className = 'recipe-row';
        div.innerHTML = `
            <select class="input" data-ing>${ingOptions}</select>
            <input class="input" data-qty type="number" min="0.01" step="0.01" placeholder="Кол-во" required>
            <button class="icon-btn" type="button" aria-label="Удалить">✕</button>`;
        div.querySelector('button').onclick = () => div.remove();
        rows.appendChild(div);
    };
    body.querySelector('#r-add').onclick = addRow;

    body.querySelector('form').onsubmit = (e) => {
        e.preventDefault();
        const d = formData(e.target);
        const recipe = [...rows.querySelectorAll('.recipe-row')].map(r => ({
            ingredientId: Number(r.querySelector('[data-ing]').value),
            quantityRequired: Number(r.querySelector('[data-qty]').value),
        }));
        const payload = {
            name: d.name, description: d.description, imageUrl: d.imageUrl,
            categoryId: Number(d.categoryId), price: d.price,
            preparationMinutes: d.preparationMinutes ?? 15,
            recipe,
        };
        busy(e.submitter, async () => {
            const p = await api.createProduct(payload);
            toast(`Блюдо «${p.name}» добавлено в меню`, 'ok');
            closeModal();
            onDone();
        });
    };
}
