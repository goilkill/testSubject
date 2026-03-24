const API = 'http://localhost:8080/api/products';

const CATEGORY_LABELS = {
    FROZEN: 'Замороженный', MEAT: 'Мясной', VEGETABLES: 'Овощи',
    GREENS: 'Зелень', SPICES: 'Специи', GRAINS: 'Крупы',
    CANNED: 'Консервы', LIQUID: 'Жидкость', SWEETS: 'Сладости'
};
const COOKING_LABELS = {
    READY_TO_EAT: 'Готовый к употреблению',
    SEMI_FINISHED: 'Полуфабрикат',
    REQUIRES_COOKING: 'Требует приготовления'
};
const FLAG_LABELS = { VEGAN: '🌱 Веган', GLUTEN_FREE: '🚫 Без глютена', SUGAR_FREE: '🍬 Без сахара' };

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

async function loadProducts() {
    const name = document.getElementById('search').value;
    const category = document.getElementById('filterCategory').value;
    const cookingStatus = document.getElementById('filterCooking').value;
    const sortBy = document.getElementById('sortBy').value;

    const params = new URLSearchParams();
    if (name) params.append('name', name);
    if (category) params.append('category', category);
    if (cookingStatus) params.append('cookingStatus', cookingStatus);
    params.append('sortBy', sortBy);

    const flags = [];
    if (document.getElementById('filterVegan').checked) flags.push('VEGAN');
    if (document.getElementById('filterGluten').checked) flags.push('GLUTEN_FREE');
    if (document.getElementById('filterSugar').checked) flags.push('SUGAR_FREE');
    for (const f of flags) params.append('flags', f);

    const res = await fetch(`${API}?${params}`);
    const products = await res.json();
    renderList(products);
}

function renderList(products) {
    const list = document.getElementById('productList');
    if (products.length === 0) {
        list.innerHTML = '<p class="empty">Продукты не найдены</p>';
        return;
    }
    list.innerHTML = products.map(p => `
        <div class="list-item" onclick="viewProduct(${p.id})">
            <div class="item-main">
                <span class="item-name">${p.name}</span>
                <span class="item-badge">${CATEGORY_LABELS[p.category] || p.category}</span>
                ${p.flags.map(f => `<span class="flag-badge">${FLAG_LABELS[f] || f}</span>`).join('')}
            </div>
            <div class="item-nutrition">
                <span>🔥 ${p.calories} ккал</span>
                <span>Б: ${p.proteins}г</span>
                <span>Ж: ${p.fats}г</span>
                <span>У: ${p.carbohydrates}г</span>
            </div>
        </div>
    `).join('');
}

async function viewProduct(id) {
    const res = await fetch(`${API}/${id}`);
    const p = await res.json();
    currentViewId = id;

    document.getElementById('viewName').textContent = p.name;
    document.getElementById('viewBody').innerHTML = `
        <table class="view-table">
            ${p.photos && p.photos.length ?
            `<tr><td>Фото</td><td>${p.photos.map(u =>
                `<img src="${u}" style="width:60px;height:60px;object-fit:cover;border-radius:6px;margin-right:4px">`
            ).join('')}</td></tr>` : ''}
            <tr><td>Категория</td><td>${CATEGORY_LABELS[p.category]}</td></tr>
            <tr><td>Готовность</td><td>${COOKING_LABELS[p.cookingStatus]}</td></tr>
            <tr><td>Калории</td><td>${p.calories} ккал/100г</td></tr>
            <tr><td>Белки</td><td>${p.proteins} г/100г</td></tr>
            <tr><td>Жиры</td><td>${p.fats} г/100г</td></tr>
            <tr><td>Углеводы</td><td>${p.carbohydrates} г/100г</td></tr>
            <tr><td>Флаги</td><td>${p.flags.length ? p.flags.map(f => FLAG_LABELS[f]).join(', ') : '—'}</td></tr>
            <tr><td>Состав</td><td>${p.composition || '—'}</td></tr>
            <tr><td>Создан</td><td>${new Date(p.createdAt).toLocaleString('ru')}</td></tr>
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
    const p = await res.json();
    closeViewModal();
    openEditModal(p);
}

async function deleteFromView() {
    const checkRes = await fetch(`${API}/${currentViewId}/check-deletion`);
    const check = await checkRes.json();

    if (!check.canDelete) {
        alert(`Нельзя удалить: продукт используется в блюдах:\n${check.usedInDishes.join('\n')}`);
        return;
    }

    if (!confirm('Удалить продукт?')) return;

    await fetch(`${API}/${currentViewId}`, { method: 'DELETE' });
    closeViewModal();
    loadProducts();
}

function openCreateModal() {
    document.getElementById('modalTitle').textContent = 'Новый продукт';
    document.getElementById('productId').value = '';
    document.getElementById('fName').value = '';
    document.getElementById('fCategory').value = 'VEGETABLES';
    document.getElementById('fCooking').value = 'READY_TO_EAT';
    document.getElementById('fCalories').value = '';
    document.getElementById('fProteins').value = '';
    document.getElementById('fFats').value = '';
    document.getElementById('fCarbs').value = '';
    document.getElementById('fComposition').value = '';
    document.getElementById('fVegan').checked = false;
    document.getElementById('fGluten').checked = false;
    document.getElementById('fSugar').checked = false;
    document.getElementById('formError').style.display = 'none';
    document.getElementById('modal').style.display = 'flex';
    currentPhotos = [];
    renderPhotos();
}

function openEditModal(p) {
    document.getElementById('modalTitle').textContent = 'Редактировать продукт';
    document.getElementById('productId').value = p.id;
    document.getElementById('fName').value = p.name;
    document.getElementById('fCategory').value = p.category;
    document.getElementById('fCooking').value = p.cookingStatus;
    document.getElementById('fCalories').value = p.calories;
    document.getElementById('fProteins').value = p.proteins;
    document.getElementById('fFats').value = p.fats;
    document.getElementById('fCarbs').value = p.carbohydrates;
    document.getElementById('fComposition').value = p.composition || '';
    document.getElementById('fVegan').checked = p.flags.includes('VEGAN');
    document.getElementById('fGluten').checked = p.flags.includes('GLUTEN_FREE');
    document.getElementById('fSugar').checked = p.flags.includes('SUGAR_FREE');
    document.getElementById('formError').style.display = 'none';
    document.getElementById('modal').style.display = 'flex';
    currentPhotos = p.photos || [];
    renderPhotos();
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

async function saveProduct() {
    const id = document.getElementById('productId').value;
    const flags = [];
    if (document.getElementById('fVegan').checked) flags.push('VEGAN');
    if (document.getElementById('fGluten').checked) flags.push('GLUTEN_FREE');
    if (document.getElementById('fSugar').checked) flags.push('SUGAR_FREE');

    const body = {
        name: document.getElementById('fName').value,
        category: document.getElementById('fCategory').value,
        cookingStatus: document.getElementById('fCooking').value,
        calories: parseFloat(document.getElementById('fCalories').value),
        proteins: parseFloat(document.getElementById('fProteins').value),
        fats: parseFloat(document.getElementById('fFats').value),
        carbohydrates: parseFloat(document.getElementById('fCarbs').value),
        composition: document.getElementById('fComposition').value || null,
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
        const errDiv = document.getElementById('formError');
        errDiv.textContent = err.error || JSON.stringify(err.errors);
        errDiv.style.display = 'block';
        return;
    }

    closeModal();
    loadProducts();
}


window.addPhoto = addPhoto;
window.removePhoto = removePhoto;

loadProducts();
