const API = 'http://localhost:8080/api/dishes';
const PRODUCTS_API = 'http://localhost:8080/api/products';

const CATEGORY_LABELS = {
    DESSERT: 'Десерт', FIRST_COURSE: 'Первое', SECOND_COURSE: 'Второе',
    DRINK: 'Напиток', SALAD: 'Салат', SOUP: 'Суп', SNACK: 'Перекус'
};
const FLAG_LABELS = { VEGAN: '🌱 Веган', GLUTEN_FREE: '🚫 Без глютена', SUGAR_FREE: '🍬 Без сахара' };

const MACRO_TO_CATEGORY = [
    { token: '!десерт', category: 'DESSERT' },
    { token: '!первое', category: 'FIRST_COURSE' },
    { token: '!второе', category: 'SECOND_COURSE' },
    { token: '!напиток', category: 'DRINK' },
    { token: '!салат', category: 'SALAD' },
    { token: '!суп', category: 'SOUP' },
    { token: '!перекус', category: 'SNACK' }
];

let allProducts = [];
let currentIngredients = [];
let currentViewId = null;

let currentPhotos = [];

function addPhoto() {
    const url = document.getElementById('photoUrl').value.trim();
    if (!url) return;
    if (currentPhotos.length >= 5) { alert('Максимум 5 фото'); return; }
    currentPhotos.push(url);
    document.getElementById('photoUrl').value = '';
    renderPhotos();
}

function removePhoto(index) {
    currentPhotos.splice(index, 1);
    renderPhotos();
}

function renderPhotos() {
    const list = document.getElementById('photosList');
    if (currentPhotos.length === 0) {
        list.innerHTML = '<p style="color:#aaa;font-size:13px">Фото не добавлены</p>';
        return;
    }
    list.innerHTML = currentPhotos.map((url, i) => `
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px">
            <img src="${url}" style="width:48px;height:48px;object-fit:cover;border-radius:6px"
                 onerror="this.style.display='none'">
            <span style="flex:1;font-size:13px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${url}</span>
            <button class="btn-icon" onclick="removePhoto(${i})">✕</button>
        </div>
    `).join('');
}

async function loadDishes() {
    const name = document.getElementById('search').value;
    const category = document.getElementById('filterCategory').value;
    const params = new URLSearchParams();
    if (name) params.append('name', name);
    if (category) params.append('category', category);

    const flags = [];
    if (document.getElementById('filterVegan').checked) flags.push('VEGAN');
    if (document.getElementById('filterGluten').checked) flags.push('GLUTEN_FREE');
    if (document.getElementById('filterSugar').checked) flags.push('SUGAR_FREE');
    for (const f of flags) params.append('flags', f);

    const res = await fetch(`${API}?${params}`);
    const dishes = await res.json();
    renderList(dishes);
}

function renderList(dishes) {
    const list = document.getElementById('dishList');
    if (dishes.length === 0) {
        list.innerHTML = '<p class="empty">Блюда не найдены</p>';
        return;
    }
    list.innerHTML = dishes.map(d => `
        <div class="list-item" onclick="viewDish(${d.id})">
            <div class="item-main">
                <span class="item-name">${d.name}</span>
                <span class="item-badge badge-blue">${CATEGORY_LABELS[d.category] || d.category}</span>
                ${d.flags.map(f => `<span class="flag-badge">${FLAG_LABELS[f] || f}</span>`).join('')}
            </div>
            <div class="item-nutrition">
                <span>🔥 ${d.calories} ккал/порция</span>
                <span>Б: ${d.proteins}г</span>
                <span>Ж: ${d.fats}г</span>
                <span>У: ${d.carbohydrates}г</span>
                <span>Порция: ${d.portionSize}г</span>
            </div>
        </div>
    `).join('');
}

async function viewDish(id) {
    const res = await fetch(`${API}/${id}`);
    const d = await res.json();
    currentViewId = id;

    document.getElementById('viewName').textContent = d.name;
    const ingredientsRows = d.ingredients.map(i =>
        `<tr><td>${i.productName}</td><td>${i.quantity} г</td></tr>`
    ).join('');

    document.getElementById('viewBody').innerHTML = `
        <table class="view-table">
            ${d.photos && d.photos.length ?
            `<tr><td>Фото</td><td>${d.photos.map(u =>
                `<img src="${u}" style="width:60px;height:60px;object-fit:cover;border-radius:6px;margin-right:4px">`
            ).join('')}</td></tr>` : ''}
            <tr><td>Категория</td><td>${CATEGORY_LABELS[d.category]}</td></tr>
            <tr><td>Порция</td><td>${d.portionSize} г</td></tr>
            <tr><td>Калории</td><td>${d.calories} ккал</td></tr>
            <tr><td>Белки</td><td>${d.proteins} г</td></tr>
            <tr><td>Жиры</td><td>${d.fats} г</td></tr>
            <tr><td>Углеводы</td><td>${d.carbohydrates} г</td></tr>
            <tr><td>Флаги</td><td>${d.flags.length ? d.flags.map(f => FLAG_LABELS[f]).join(', ') : '—'}</td></tr>
            <tr><td>Создан</td><td>${new Date(d.createdAt).toLocaleString('ru')}</td></tr>
        </table>
        <h3 style="margin-top:16px">Состав:</h3>
        <table class="view-table">
            <thead><tr><th>Продукт</th><th>Количество</th></tr></thead>
            <tbody>${ingredientsRows}</tbody>
        </table>
    `;
    document.getElementById('viewModal').style.display = 'flex';
}

function closeViewModal() {
    document.getElementById('viewModal').style.display = 'none';
    currentViewId = null;
}

async function editFromView() {
    const res = await fetch(`${API}/${currentViewId}`);
    const d = await res.json();
    closeViewModal();
    openEditModal(d);
}

async function deleteFromView() {
    if (!confirm('Удалить блюдо?')) return;
    await fetch(`${API}/${currentViewId}`, { method: 'DELETE' });
    closeViewModal();
    loadDishes();
}

async function openCreateModal() {
    await loadAllProducts();
    document.getElementById('modalTitle').textContent = 'Новое блюдо';
    document.getElementById('dishId').value = '';
    document.getElementById('fName').value = '';
    document.getElementById('fCategory').value = 'SECOND_COURSE';
    document.getElementById('fPortionSize').value = '250';
    document.getElementById('fCalories').value = '';
    document.getElementById('fProteins').value = '';
    document.getElementById('fFats').value = '';
    document.getElementById('fCarbs').value = '';
    document.getElementById('fVegan').checked = false;
    document.getElementById('fGluten').checked = false;
    document.getElementById('fSugar').checked = false;
    document.getElementById('formError').style.display = 'none';
    currentIngredients = [];
    renderIngredients();
    document.getElementById('modal').style.display = 'flex';
    currentPhotos = [];
    renderPhotos();
}

async function openEditModal(d) {
    await loadAllProducts();
    document.getElementById('modalTitle').textContent = 'Редактировать блюдо';
    document.getElementById('dishId').value = d.id;
    document.getElementById('fName').value = d.name;
    document.getElementById('fCategory').value = d.category;
    document.getElementById('fPortionSize').value = d.portionSize;
    document.getElementById('fCalories').value = d.calories;
    document.getElementById('fProteins').value = d.proteins;
    document.getElementById('fFats').value = d.fats;
    document.getElementById('fCarbs').value = d.carbohydrates;
    document.getElementById('fVegan').checked = d.flags.includes('VEGAN');
    document.getElementById('fGluten').checked = d.flags.includes('GLUTEN_FREE');
    document.getElementById('fSugar').checked = d.flags.includes('SUGAR_FREE');
    document.getElementById('formError').style.display = 'none';

    currentIngredients = d.ingredients.map(i => ({
        productId: i.productId,
        productName: i.productName,
        quantity: i.quantity
    }));
    currentPhotos = d.photos || [];
    renderPhotos();
    renderIngredients();
    await recalculate();
    document.getElementById('modal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

async function loadAllProducts() {
    const res = await fetch(PRODUCTS_API);
    allProducts = await res.json();
    const select = document.getElementById('productSelect');
    select.innerHTML = allProducts.map(p =>
        `<option value="${p.id}">${p.name}</option>`
    ).join('');
}

function addIngredient() {
    const select = document.getElementById('productSelect');
    const qty = parseFloat(document.getElementById('ingredientQty').value);
    if (!qty || qty <= 0) { alert('Укажите количество в граммах'); return; }

    const productId = parseInt(select.value);
    const productName = select.options[select.selectedIndex].text;

    const existing = currentIngredients.find(i => i.productId === productId);
    if (existing) {
        existing.quantity = qty;
    } else {
        currentIngredients.push({ productId, productName, quantity: qty });
    }

    document.getElementById('ingredientQty').value = '';
    renderIngredients();
    recalculate();
}

function removeIngredient(productId) {
    currentIngredients = currentIngredients.filter(i => i.productId !== productId);
    renderIngredients();
    recalculate();
}

function renderIngredients() {
    const list = document.getElementById('ingredientsList');
    if (currentIngredients.length === 0) {
        list.innerHTML = '<p style="color:#aaa;margin-bottom:8px">Ингредиенты не добавлены</p>';
        return;
    }
    list.innerHTML = currentIngredients.map(i => `
        <div class="ingredient-row">
            <span>${i.productName}</span>
            <span>${i.quantity} г</span>
            <button class="btn-icon" onclick="removeIngredient(${i.productId})">✕</button>
        </div>
    `).join('');
}

async function recalculate() {
    if (currentIngredients.length === 0) return;

    const res = await fetch(`${API}/calculate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(currentIngredients.map(i => ({
            productId: i.productId,
            quantity: i.quantity
        })))
    });
    const calc = await res.json();

    document.getElementById('fCalories').value = calc.calories;
    document.getElementById('fProteins').value = calc.proteins;
    document.getElementById('fFats').value = calc.fats;
    document.getElementById('fCarbs').value = calc.carbohydrates;

    const veganCb = document.getElementById('fVegan');
    const glutenCb = document.getElementById('fGluten');
    const sugarCb = document.getElementById('fSugar');

    veganCb.disabled = !calc.availableFlags.includes('VEGAN');
    glutenCb.disabled = !calc.availableFlags.includes('GLUTEN_FREE');
    sugarCb.disabled = !calc.availableFlags.includes('SUGAR_FREE');

    if (veganCb.disabled) veganCb.checked = false;
    if (glutenCb.disabled) glutenCb.checked = false;
    if (sugarCb.disabled) sugarCb.checked = false;
}

async function saveDish() {
    if (currentIngredients.length === 0) {
        showError('Добавьте хотя бы один ингредиент');
        return;
    }

    const id = document.getElementById('dishId').value;
    const flags = [];
    if (document.getElementById('fVegan').checked) flags.push('VEGAN');
    if (document.getElementById('fGluten').checked) flags.push('GLUTEN_FREE');
    if (document.getElementById('fSugar').checked) flags.push('SUGAR_FREE');

    const name = document.getElementById('fName').value || '';
    const selectedCategory = document.getElementById('fCategory').value;
    let resolvedCategory = selectedCategory;
    if (selectedCategory === 'NONE') {
        for (const m of MACRO_TO_CATEGORY) {
            if (name.includes(m.token)) {
                resolvedCategory = m.category;
                break;
            }
        }
        if (resolvedCategory === 'NONE') {
            showError('Запрещено сохранять: категория --- и в названии нет макроса.');
            return;
        }
    }

    const body = {
        name,
        category: resolvedCategory,
        portionSize: parseFloat(document.getElementById('fPortionSize').value),
        calories: parseFloat(document.getElementById('fCalories').value) || null,
        proteins: parseFloat(document.getElementById('fProteins').value) || null,
        fats: parseFloat(document.getElementById('fFats').value) || null,
        carbohydrates: parseFloat(document.getElementById('fCarbs').value) || null,
        ingredients: currentIngredients.map(i => ({
            productId: i.productId,
            quantity: i.quantity
        })),
        flags,
        photos: currentPhotos
    };

    const url = id ? `${API}/${id}` : API;
    const method = id ? 'PUT' : 'POST';

    const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });

    if (!res.ok) {
        const err = await res.json();
        showError(err.error || JSON.stringify(err.errors));
        return;
    }

    closeModal();
    loadDishes();
}

function showError(msg) {
    const div = document.getElementById('formError');
    div.textContent = msg;
    div.style.display = 'block';
}


window.addPhoto = addPhoto;
window.removePhoto = removePhoto;
window.renderPhotos = renderPhotos;

loadDishes();
