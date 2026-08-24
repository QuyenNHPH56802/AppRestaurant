// PHASE 7: Admin SPA. Vanilla JS, talks to the REST API via fetch().
// All UI strings are in Vietnamese; bilingual admin is a v1+ extension.

const state = {
    token: null,
    user: null,
    view: 'dashboard',
    lang: 'vi',
    foods: [],
    categories: [],
    users: [],
    shifts: [],
    shiftAssignments: [],
    zones: [],
    zoneAssignments: [],
    checklists: [],
    checkIns: [],
    activity: [],
    notifications: [],
    devices: [],
    foodForm: null,
    categoryForm: null,
    userForm: null,
    storeForm: null,
};

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => Array.from(document.querySelectorAll(sel));

function api(path, options = {}) {
    const headers = { 'Accept': 'application/json' };
    if (state.token) headers['Authorization'] = `Bearer ${state.token}`;
    if (options.body && !(options.body instanceof FormData) && typeof options.body !== 'string') {
        headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }
    return fetch(path, { ...options, headers: { ...headers, ...(options.headers || {}) } })
        .then(async r => {
            const j = await r.json().catch(() => ({}));
            if (!r.ok || j.success === false) {
                throw new Error((j.error && j.error.message) || `HTTP ${r.status}`);
            }
            return j.data;
        });
}

function t(key, fallback = '') {
    const dict = {
        'common.save': 'Lưu', 'common.cancel': 'Hủy', 'common.delete': 'Xóa', 'common.edit': 'Sửa',
        'common.loading': 'Đang tải…', 'common.retry': 'Thử lại', 'common.error': 'Đã xảy ra lỗi',
        'common.empty': 'Không có dữ liệu',
        'food.available': 'Đang bán', 'food.sold_out': 'Hết món', 'food.hidden': 'Đã ẩn',
        'food.featured': 'Nổi bật', 'food.active': 'Hoạt động', 'food.disabled': 'Vô hiệu',
        'food.name_vi': 'Tên (Tiếng Việt)', 'food.name_ko': 'Tên (한국어)',
        'food.desc_vi': 'Mô tả (Tiếng Việt)', 'food.desc_ko': 'Mô tả (한국어)',
        'food.ing_vi': 'Thành phần (Tiếng Việt)', 'food.ing_ko': 'Thành phần (한국어)',
        'food.portion_vi': 'Khẩu phần (Tiếng Việt)', 'food.portion_ko': 'Khẩn phần (한국어)',
        'food.price': 'Giá (VND)', 'food.category': 'Danh mục', 'food.status': 'Trạng thái',
        'food.featured_q': 'Món nổi bật?', 'food.image': 'Ảnh',
        'cat.name_vi': 'Tên (Tiếng Việt)', 'cat.name_ko': 'Tên (한국어)',
        'cat.desc_vi': 'Mô tả (Tiếng Việt)', 'cat.desc_ko': 'Mô tả (한국어)',
        'cat.sort': 'Thứ tự', 'cat.status': 'Trạng thái',
        'user.username': 'Tên đăng nhập', 'user.password': 'Mật khẩu', 'user.full_name': 'Họ và tên',
        'user.role': 'Vai trò', 'user.lang': 'Ngôn ngữ', 'user.status': 'Trạng thái',
        'store.name_vi': 'Tên cửa hàng (Tiếng Việt)', 'store.name_ko': 'Tên cửa hàng (한국어)',
        'store.address': 'Địa chỉ', 'store.phone': 'Điện thoại', 'store.hours': 'Giờ mở cửa',
    };
    return dict[key] || fallback;
}

// --- Login ---
$('#login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = $('#login-username').value.trim();
    const password = $('#login-password').value;
    const lang = document.querySelector('input[name="login-lang"]:checked').value;
    state.lang = lang;
    $('#login-error').hidden = true;
    try {
        const data = await api('/api/auth/login', {
            method: 'POST', body: { username, password }
        });
        state.token = data.token;
        state.user = data.user;
        showApp();
    } catch (err) {
        $('#login-error').textContent = err.message || 'Đăng nhập thất bại';
        $('#login-error').hidden = false;
    }
});

function showApp() {
    $('#login-page').hidden = true;
    $('#app').hidden = false;
    $('#who').textContent = `${state.user.fullName} (${state.user.role})`;
    render();
}

$('#logout-btn').addEventListener('click', async () => {
    try { await api('/api/auth/logout', { method: 'POST' }); } catch (e) {}
    state.token = null; state.user = null;
    $('#app').hidden = true;
    $('#login-page').hidden = false;
});

$$('.side button').forEach(btn => {
    btn.addEventListener('click', () => {
        $$('.side button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        state.view = btn.dataset.view;
        render();
    });
});

// --- Dashboard ---
async function renderDashboard() {
    const root = $('#content');
    root.innerHTML = `<div class="notice">Đang tải…</div>`;
    try {
        const [foods, categories] = await Promise.all([
            api('/api/admin/foods?size=1000'),
            api('/api/admin/categories?size=1000'),
        ]);
        const byStatus = (s) => foods.items.filter(f => f.status === s).length;
        const featured = foods.items.filter(f => f.featured).length;
        const activeCat = categories.items.filter(c => c.status === 'ACTIVE').length;
        root.innerHTML = `
            <h2>Tổng quan</h2>
            <div class="stat-grid">
                <div class="stat"><div class="label">Tổng món</div><div class="value">${foods.items.length}</div></div>
                <div class="stat"><div class="label">Đang bán</div><div class="value">${byStatus('AVAILABLE')}</div></div>
                <div class="stat"><div class="label">Hết món</div><div class="value">${byStatus('SOLD_OUT')}</div></div>
                <div class="stat"><div class="label">Đã ẩn</div><div class="value">${byStatus('HIDDEN')}</div></div>
                <div class="stat"><div class="label">Nổi bật</div><div class="value">${featured}</div></div>
                <div class="stat"><div class="label">Danh mục</div><div class="value">${activeCat}</div></div>
            </div>
            <div class="notice">Đăng nhập với quyền quản trị viên để chỉnh sửa menu. Nhân viên chỉ có thể xem qua ứng dụng di động.</div>
        `;
    } catch (e) {
        root.innerHTML = `<p class="error">${e.message}</p>`;
    }
}

// --- Categories ---
async function renderCategories() {
    const root = $('#content');
    root.innerHTML = `<div class="toolbar"><button class="primary" id="add-cat">+ Thêm danh mục</button></div><div id="cat-table">Đang tải…</div>`;
    try {
        const data = await api('/api/admin/categories?size=1000');
        state.categories = data.items;
        const rows = data.items.map(c => `
            <tr>
                <td>${c.id}</td>
                <td>${c.name || '—'}</td>
                <td>${c.sortOrder ?? 0}</td>
                <td><span class="badge ${c.status === 'ACTIVE' ? 'active' : 'hidden'}">${c.status}</span></td>
                <td>${(c.fallback || []).includes('vi') ? '<span class="warn">Thiếu bản dịch</span>' : ''}</td>
                <td>
                    <button class="ghost" data-edit-cat="${c.id}">${t('common.edit')}</button>
                    <button class="danger" data-del-cat="${c.id}">${t('common.delete')}</button>
                </td>
            </tr>
        `).join('');
        $('#cat-table').innerHTML = `
            <table>
                <thead><tr><th>#</th><th>Tên</th><th>Thứ tự</th><th>Trạng thái</th><th></th><th>Hành động</th></tr></thead>
                <tbody>${rows || `<tr><td colspan="6">${t('common.empty')}</td></tr>`}</tbody>
            </table>
        `;
        $('#add-cat').onclick = () => openCategoryModal(null);
        $$('[data-edit-cat]').forEach(b => b.onclick = () => openCategoryModal(state.categories.find(x => x.id == b.dataset.editCat)));
        $$('[data-del-cat]').forEach(b => b.onclick = () => hideCategory(b.dataset.delCat));
    } catch (e) { $('#cat-table').innerHTML = `<p class="error">${e.message}</p>`; }
}

function openCategoryModal(cat) {
    const isEdit = !!cat;
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    modal.innerHTML = `
        <div class="modal">
            <h2>${isEdit ? 'Sửa danh mục' : 'Thêm danh mục'}</h2>
            <div class="row">
                <div>
                    <label>${t('cat.name_vi')}</label>
                    <input id="cat-name-vi" value="${cat ? (findTr(cat, 'vi')?.name || '') : ''}" />
                </div>
                <div>
                    <label>${t('cat.name_ko')}</label>
                    <input id="cat-name-ko" value="${cat ? (findTr(cat, 'ko')?.name || '') : ''}" />
                </div>
            </div>
            <div class="row">
                <div>
                    <label>${t('cat.desc_vi')}</label>
                    <textarea id="cat-desc-vi">${cat ? (findTr(cat, 'vi')?.description || '') : ''}</textarea>
                </div>
                <div>
                    <label>${t('cat.desc_ko')}</label>
                    <textarea id="cat-desc-ko">${cat ? (findTr(cat, 'ko')?.description || '') : ''}</textarea>
                </div>
            </div>
            <div class="row">
                <div><label>${t('cat.sort')}</label><input type="number" id="cat-sort" value="${cat ? (cat.sortOrder ?? 0) : 0}" /></div>
                <div><label>${t('cat.status')}</label>
                    <select id="cat-status">
                        <option value="ACTIVE" ${cat?.status === 'ACTIVE' ? 'selected' : ''}>ACTIVE</option>
                        <option value="HIDDEN" ${cat?.status === 'HIDDEN' ? 'selected' : ''}>HIDDEN</option>
                    </select>
                </div>
            </div>
            <div class="actions">
                <button class="ghost" id="cat-cancel">${t('common.cancel')}</button>
                <button class="primary" id="cat-save">${t('common.save')}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#cat-cancel').onclick = () => modal.remove();
    $('#cat-save').onclick = async () => {
        const payload = {
            sortOrder: parseInt($('#cat-sort').value || '0', 10),
            status: $('#cat-status').value,
            translations: [
                { lang: 'vi', name: $('#cat-name-vi').value.trim(), description: $('#cat-desc-vi').value },
                { lang: 'ko', name: $('#cat-name-ko').value.trim(), description: $('#cat-desc-ko').value },
            ],
        };
        try {
            if (isEdit) await api(`/api/admin/categories/${cat.id}`, { method: 'PUT', body: payload });
            else await api('/api/admin/categories', { method: 'POST', body: payload });
            modal.remove(); renderCategories();
        } catch (e) { alert(e.message); }
    };
}

async function hideCategory(id) {
    if (!confirm('Ẩn danh mục này?')) return;
    try { await api(`/api/admin/categories/${id}`, { method: 'DELETE' }); renderCategories(); }
    catch (e) { alert(e.message); }
}

// --- Foods ---
async function renderFoods() {
    const root = $('#content');
    root.innerHTML = `
        <div class="toolbar">
            <input id="food-search" placeholder="Tìm món..." />
            <select id="food-filter-cat"><option value="">Tất cả danh mục</option></select>
            <select id="food-filter-status">
                <option value="">Tất cả trạng thái</option>
                <option value="AVAILABLE">AVAILABLE</option>
                <option value="SOLD_OUT">SOLD_OUT</option>
                <option value="HIDDEN">HIDDEN</option>
            </select>
            <button class="primary" id="add-food">+ Thêm món</button>
        </div>
        <div id="food-table">Đang tải…</div>
    `;
    try {
        const [foods, cats] = await Promise.all([
            api('/api/admin/foods?size=1000'),
            api('/api/admin/categories?size=1000'),
        ]);
        state.foods = foods.items;
        state.categories = cats.items;
        $('#food-filter-cat').innerHTML = '<option value="">Tất cả danh mục</option>' +
            cats.items.map(c => `<option value="${c.id}">${c.name || '—'}</option>`).join('');
        const renderRows = () => {
            const q = $('#food-search').value.toLowerCase();
            const catId = $('#food-filter-cat').value;
            const status = $('#food-filter-status').value;
            const list = foods.items.filter(f =>
                (!q || (f.name || '').toLowerCase().includes(q)) &&
                (!catId || String(f.categoryId) === catId) &&
                (!status || f.status === status)
            );
            const rows = list.map(f => `
                <tr>
                    <td>${f.id}</td>
                    <td>${f.imageUrl ? `<img class="img-preview" src="${f.imageUrl}" alt=""/>` : '—'}</td>
                    <td>${f.name || '—'}</td>
                    <td>${f.categoryName || '—'}</td>
                    <td>${f.price} ₫</td>
                    <td><span class="badge ${f.status.toLowerCase()}">${statusText(f.status)}</span> ${f.featured ? '<span class="badge featured">Nổi bật</span>' : ''}</td>
                    <td>
                        <button class="ghost" data-edit-food="${f.id}">${t('common.edit')}</button>
                        <button class="danger" data-del-food="${f.id}">Ẩn</button>
                    </td>
                </tr>
            `).join('');
            $('#food-table').innerHTML = `
                <table>
                    <thead><tr><th>#</th><th>Ảnh</th><th>Tên</th><th>Danh mục</th><th>Giá</th><th>Trạng thái</th><th>Hành động</th></tr></thead>
                    <tbody>${rows || `<tr><td colspan="7">${t('common.empty')}</td></tr>`}</tbody>
                </table>
            `;
            $$('[data-edit-food]').forEach(b => b.onclick = () => openFoodModal(state.foods.find(x => x.id == b.dataset.editFood)));
            $$('[data-del-food]').forEach(b => b.onclick = () => hideFood(b.dataset.delFood));
        };
        $('#food-search').oninput = renderRows;
        $('#food-filter-cat').onchange = renderRows;
        $('#food-filter-status').onchange = renderRows;
        $('#add-food').onclick = () => openFoodModal(null);
        renderRows();
    } catch (e) { $('#food-table').innerHTML = `<p class="error">${e.message}</p>`; }
}

function statusText(s) {
    if (s === 'AVAILABLE') return 'Đang bán';
    if (s === 'SOLD_OUT') return 'Hết món';
    return 'Đã ẩn';
}

function findTr(obj, lang) {
    if (!obj) return null;
    if (obj.fallback) {
        // already localized; no raw translations list
        return { name: obj.name, description: obj.description };
    }
    return null;
}

function openFoodModal(food) {
    const isEdit = !!food;
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    modal.innerHTML = `
        <div class="modal">
            <h2>${isEdit ? 'Sửa món' : 'Thêm món'}</h2>
            <div class="row">
                <div><label>${t('food.name_vi')}</label><input id="f-name-vi" value="${food?.name || ''}" /></div>
                <div><label>${t('food.name_ko')}</label><input id="f-name-ko" value="${food?.name || ''}" /></div>
            </div>
            <div class="row">
                <div><label>${t('food.desc_vi')}</label><textarea id="f-desc-vi">${food?.description || ''}</textarea></div>
                <div><label>${t('food.desc_ko')}</label><textarea id="f-desc-ko">${food?.description || ''}</textarea></div>
            </div>
            <div class="row">
                <div><label>${t('food.ing_vi')}</label><input id="f-ing-vi" value="${food?.ingredients || ''}" /></div>
                <div><label>${t('food.ing_ko')}</label><input id="f-ing-ko" value="${food?.ingredients || ''}" /></div>
            </div>
            <div class="row">
                <div><label>${t('food.portion_vi')}</label><input id="f-portion-vi" value="${food?.portion || ''}" /></div>
                <div><label>${t('food.portion_ko')}</label><input id="f-portion-ko" value="${food?.portion || ''}" /></div>
            </div>
            <div class="row">
                <div><label>${t('food.price')}</label><input type="number" id="f-price" value="${food?.price || 0}" /></div>
                <div><label>${t('food.category')}</label>
                    <select id="f-cat">${state.categories.map(c => `<option value="${c.id}" ${food?.categoryId == c.id ? 'selected' : ''}>${c.name || '—'}</option>`).join('')}</select>
                </div>
            </div>
            <div class="row">
                <div><label>${t('food.status')}</label>
                    <select id="f-status">
                        <option value="AVAILABLE" ${food?.status === 'AVAILABLE' ? 'selected' : ''}>AVAILABLE</option>
                        <option value="SOLD_OUT" ${food?.status === 'SOLD_OUT' ? 'selected' : ''}>SOLD_OUT</option>
                        <option value="HIDDEN" ${food?.status === 'HIDDEN' ? 'selected' : ''}>HIDDEN</option>
                    </select>
                </div>
                <div><label>${t('food.featured_q')}</label>
                    <select id="f-featured">
                        <option value="false" ${!food?.featured ? 'selected' : ''}>Không</option>
                        <option value="true" ${food?.featured ? 'selected' : ''}>Có</option>
                    </select>
                </div>
            </div>
            <label>${t('food.image')}</label>
            <input type="file" id="f-image" accept="image/jpeg,image/png,image/webp" />
            <p class="warn" id="f-image-warn" hidden></p>
            <div class="actions">
                <button class="ghost" id="f-cancel">${t('common.cancel')}</button>
                <button class="primary" id="f-save">${t('common.save')}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#f-cancel').onclick = () => modal.remove();
    $('#f-save').onclick = async () => {
        let imageUrl = food?.imageUrl || null;
        const file = $('#f-image').files[0];
        if (file) {
            const fd = new FormData();
            fd.append('file', file);
            try {
                const uploaded = await api('/api/admin/uploads/food-image', { method: 'POST', body: fd });
                imageUrl = uploaded.imageUrl;
            } catch (e) { alert('Tải ảnh thất bại: ' + e.message); return; }
        }
        const payload = {
            categoryId: parseInt($('#f-cat').value, 10),
            price: parseFloat($('#f-price').value || '0'),
            status: $('#f-status').value,
            featured: $('#f-featured').value === 'true',
            sortOrder: food?.sortOrder ?? 0,
            imageUrl,
            translations: [
                { lang: 'vi', name: $('#f-name-vi').value.trim(), description: $('#f-desc-vi').value, ingredients: $('#f-ing-vi').value, portion: $('#f-portion-vi').value },
                { lang: 'ko', name: $('#f-name-ko').value.trim(), description: $('#f-desc-ko').value, ingredients: $('#f-ing-ko').value, portion: $('#f-portion-ko').value },
            ],
        };
        if (!payload.translations[0].name) { alert('Vui lòng nhập tên tiếng Việt'); return; }
        if (!payload.translations[1].name) { $('#f-image-warn').textContent = '한국어 번역이 없습니다 (Thiếu bản dịch tiếng Hàn)'; $('#f-image-warn').hidden = false; }
        try {
            if (isEdit) await api(`/api/admin/foods/${food.id}`, { method: 'PUT', body: payload });
            else await api('/api/admin/foods', { method: 'POST', body: payload });
            modal.remove(); renderFoods();
        } catch (e) { alert(e.message); }
    };
}

async function hideFood(id) {
    if (!confirm('Ẩn món này?')) return;
    try { await api(`/api/admin/foods/${id}`, { method: 'DELETE' }); renderFoods(); }
    catch (e) { alert(e.message); }
}

// --- Users ---
async function renderUsers() {
    const root = $('#content');
    root.innerHTML = `<div class="toolbar"><button class="primary" id="add-user">+ Thêm nhân viên</button></div><div id="user-table">Đang tải…</div>`;
    try {
        const data = await api('/api/admin/users?size=1000');
        state.users = data.items;
        const rows = data.items.map(u => `
            <tr>
                <td>${u.id}</td>
                <td>${u.username}</td>
                <td>${u.fullName}</td>
                <td><span class="badge ${u.role === 'ADMIN' ? 'featured' : 'active'}">${u.role}</span></td>
                <td><span class="badge ${u.status === 'ACTIVE' ? 'active' : 'disabled'}">${u.status}</span></td>
                <td>${u.lang}</td>
                <td>
                    <button class="ghost" data-edit-user="${u.id}">${t('common.edit')}</button>
                    <button class="ghost" data-reset-user="${u.id}">Reset mật khẩu</button>
                </td>
            </tr>
        `).join('');
        $('#user-table').innerHTML = `
            <table>
                <thead><tr><th>#</th><th>Username</th><th>Họ tên</th><th>Vai trò</th><th>Trạng thái</th><th>Ngôn ngữ</th><th>Hành động</th></tr></thead>
                <tbody>${rows || `<tr><td colspan="7">${t('common.empty')}</td></tr>`}</tbody>
            </table>
        `;
        $('#add-user').onclick = () => openUserModal(null);
        $$('[data-edit-user]').forEach(b => b.onclick = () => openUserModal(state.users.find(x => x.id == b.dataset.editUser)));
        $$('[data-reset-user]').forEach(b => b.onclick = () => resetUserPassword(b.dataset.resetUser));
    } catch (e) { $('#user-table').innerHTML = `<p class="error">${e.message}</p>`; }
}

function openUserModal(user) {
    const isEdit = !!user;
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    modal.innerHTML = `
        <div class="modal">
            <h2>${isEdit ? 'Sửa nhân viên' : 'Thêm nhân viên'}</h2>
            <div class="row">
                <div><label>${t('user.username')}</label><input id="u-username" value="${user?.username || ''}" ${isEdit ? 'disabled' : ''} /></div>
                <div><label>${isEdit ? 'Mật khẩu mới (để trống nếu giữ nguyên)' : t('user.password')}</label><input type="password" id="u-password" /></div>
            </div>
            <div class="row">
                <div><label>${t('user.full_name')}</label><input id="u-full" value="${user?.fullName || ''}" /></div>
                <div><label>${t('user.role')}</label>
                    <select id="u-role">
                        <option value="ADMIN" ${user?.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                        <option value="STAFF" ${user?.role === 'STAFF' ? 'selected' : ''}>STAFF</option>
                    </select>
                </div>
            </div>
            <div class="row">
                <div><label>${t('user.status')}</label>
                    <select id="u-status">
                        <option value="ACTIVE" ${user?.status === 'ACTIVE' ? 'selected' : ''}>ACTIVE</option>
                        <option value="DISABLED" ${user?.status === 'DISABLED' ? 'selected' : ''}>DISABLED</option>
                    </select>
                </div>
                <div><label>${t('user.lang')}</label>
                    <select id="u-lang">
                        <option value="vi" ${user?.lang === 'vi' ? 'selected' : ''}>Tiếng Việt</option>
                        <option value="ko" ${user?.lang === 'ko' ? 'selected' : ''}>한국어</option>
                    </select>
                </div>
            </div>
            <div class="actions">
                <button class="ghost" id="u-cancel">${t('common.cancel')}</button>
                <button class="primary" id="u-save">${t('common.save')}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#u-cancel').onclick = () => modal.remove();
    $('#u-save').onclick = async () => {
        const password = $('#u-password').value;
        const payload = {
            username: $('#u-username').value.trim(),
            password: password || (isEdit ? 'placeholder' : ''),
            fullName: $('#u-full').value.trim(),
            role: $('#u-role').value,
            status: $('#u-status').value,
            lang: $('#u-lang').value,
        };
        if (!isEdit && !password) { alert('Vui lòng nhập mật khẩu'); return; }
        try {
            if (isEdit) await api(`/api/admin/users/${user.id}`, { method: 'PUT', body: payload });
            else await api('/api/admin/users', { method: 'POST', body: payload });
            modal.remove(); renderUsers();
        } catch (e) { alert(e.message); }
    };
}

async function resetUserPassword(id) {
    const np = prompt('Mật khẩu mới (tối thiểu 6 ký tự):');
    if (!np) return;
    try {
        await api(`/api/admin/users/${id}/reset-password`, { method: 'POST', body: { newPassword: np } });
        alert('Đã đặt lại mật khẩu');
    } catch (e) { alert(e.message); }
}

// --- Store ---
async function renderStore() {
    const root = $('#content');
    root.innerHTML = `<div id="store-form">Đang tải…</div>`;
    try {
        const data = await api('/api/store');
        const form = document.createElement('div');
        form.innerHTML = `
            <h2>Thông tin cửa hàng</h2>
            <div class="row">
                <div><label>${t('store.name_vi')}</label><input id="s-name-vi" value="${data.name || ''}" /></div>
                <div><label>${t('store.name_ko')}</label><input id="s-name-ko" value="${data.name || ''}" /></div>
            </div>
            <label>${t('store.address')}</label><input id="s-address" value="${data.address || ''}" />
            <label>${t('store.phone')}</label><input id="s-phone" value="${data.phone || ''}" />
            <label>${t('store.hours')}</label><input id="s-hours" value="${data.openingHours || ''}" />
            <div class="actions"><button class="primary" id="s-save">${t('common.save')}</button></div>
        `;
        $('#store-form').replaceWith(form);
        $('#s-save').onclick = async () => {
            const payload = {
                address: $('#s-address').value, phone: $('#s-phone').value, openingHours: $('#s-hours').value,
                translations: [
                    { lang: 'vi', storeName: $('#s-name-vi').value.trim() },
                    { lang: 'ko', storeName: $('#s-name-ko').value.trim() },
                ],
            };
            try {
                await api('/api/admin/store', { method: 'PUT', body: payload });
                alert('Đã lưu');
            } catch (e) { alert(e.message); }
        };
    } catch (e) { $('#store-form').innerHTML = `<p class="error">${e.message}</p>`; }
}

// ============================ Phase G — V2.2 / V2.3 ============================

// --- Shifts ---
async function renderShifts() {
    const root = $('#content');
    root.innerHTML = `
        <div class="tabs">
            <button class="tab active" data-tab="shifts-list">Ca (mẫu)</button>
            <button class="tab" data-tab="shifts-assignments">Phân ca</button>
        </div>
        <div id="shifts-content">Đang tải…</div>
    `;
    const setTab = (name) => {
        root.querySelectorAll('.tab').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
        if (name === 'shifts-list') renderShiftsList();
        else renderShiftAssignments();
    };
    root.querySelectorAll('.tab').forEach(b => b.onclick = () => setTab(b.dataset.tab));
    renderShiftsList();
}

async function renderShiftsList() {
    const wrap = $('#shifts-content');
    wrap.innerHTML = `<div class="toolbar"><button class="primary" id="add-shift">+ Thêm ca</button></div><div id="shift-table">Đang tải…</div>`;
    try {
        const shifts = await api('/api/admin/shifts');
        state.shifts = shifts;
        const rows = shifts.map(s => `
            <tr>
                <td>${s.id}</td>
                <td>${s.name || '—'}</td>
                <td>${s.startTime || '—'} → ${s.endTime || '—'}</td>
                <td>${s.tz || '—'}</td>
                <td><span class="badge ${s.active ? 'active' : 'hidden'}">${s.active ? 'ACTIVE' : 'INACTIVE'}</span></td>
                <td>${s.sortOrder ?? 0}</td>
                <td>
                    <button class="ghost" data-edit-shift="${s.id}">${t('common.edit')}</button>
                    <button class="danger" data-del-shift="${s.id}">${t('common.delete')}</button>
                </td>
            </tr>
        `).join('');
        $('#shift-table').innerHTML = `
            <table>
                <thead><tr><th>#</th><th>Tên</th><th>Giờ</th><th>Múi giờ</th><th>Trạng thái</th><th>Thứ tự</th><th>Hành động</th></tr></thead>
                <tbody>${rows || `<tr><td colspan="7">${t('common.empty')}</td></tr>`}</tbody>
            </table>
        `;
        $('#add-shift').onclick = () => openShiftModal(null);
        $$('[data-edit-shift]').forEach(b => b.onclick = () => openShiftModal(state.shifts.find(x => x.id == b.dataset.editShift)));
        $$('[data-del-shift]').forEach(b => b.onclick = () => deleteShift(b.dataset.delShift));
    } catch (e) { $('#shift-table').innerHTML = `<p class="error">${e.message}</p>`; }
}

function openShiftModal(shift) {
    const isEdit = !!shift;
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    modal.innerHTML = `
        <div class="modal">
            <h2>${isEdit ? 'Sửa ca' : 'Thêm ca'}</h2>
            <div class="row">
                <div><label>Tên ca</label><input id="sh-name" value="${shift?.name || ''}" /></div>
                <div><label>Mô tả</label><input id="sh-desc" value="${shift?.description || ''}" /></div>
            </div>
            <div class="row">
                <div><label>Giờ bắt đầu (HH:MM)</label><input id="sh-start" value="${shift?.startTime || ''}" /></div>
                <div><label>Giờ kết thúc (HH:MM)</label><input id="sh-end" value="${shift?.endTime || ''}" /></div>
            </div>
            <div class="row">
                <div><label>Múi giờ</label><input id="sh-tz" value="${shift?.tz || 'Asia/Ho_Chi_Minh'}" /></div>
                <div><label>Thứ tự</label><input type="number" id="sh-sort" value="${shift?.sortOrder ?? 0}" /></div>
            </div>
            <div class="row">
                <div><label>Hoạt động</label>
                    <select id="sh-active">
                        <option value="true" ${shift?.active ? 'selected' : ''}>ACTIVE</option>
                        <option value="false" ${!shift?.active ? 'selected' : ''}>INACTIVE</option>
                    </select>
                </div>
            </div>
            <div class="actions">
                <button class="ghost" id="sh-cancel">${t('common.cancel')}</button>
                <button class="primary" id="sh-save">${t('common.save')}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#sh-cancel').onclick = () => modal.remove();
    $('#sh-save').onclick = async () => {
        const payload = {
            name: $('#sh-name').value.trim(),
            description: $('#sh-desc').value,
            startTime: $('#sh-start').value.trim(),
            endTime: $('#sh-end').value.trim(),
            tz: $('#sh-tz').value.trim(),
            active: $('#sh-active').value === 'true',
            sortOrder: parseInt($('#sh-sort').value || '0', 10),
        };
        if (!payload.name || !payload.startTime || !payload.endTime) { alert('Vui lòng nhập tên và giờ'); return; }
        try {
            if (isEdit) await api(`/api/admin/shifts/${shift.id}`, { method: 'PUT', body: payload });
            else await api('/api/admin/shifts', { method: 'POST', body: payload });
            modal.remove(); renderShiftsList();
        } catch (e) { alert(e.message); }
    };
}

async function deleteShift(id) {
    if (!confirm('Xóa ca này?')) return;
    try { await api(`/api/admin/shifts/${id}`, { method: 'DELETE' }); renderShiftsList(); }
    catch (e) { alert(e.message); }
}

// --- Shift Assignments ---
async function renderShiftAssignments() {
    const wrap = $('#shifts-content');
    wrap.innerHTML = `
        <div class="toolbar">
            <input type="date" id="ass-date" value="${new Date().toISOString().slice(0, 10)}" />
            <button class="primary" id="ass-refresh">Tải lại</button>
            <button class="ghost" id="add-assignment">+ Phân ca</button>
        </div>
        <div id="ass-table">Đang tải…</div>
    `;
    const load = async () => {
        const date = $('#ass-date').value;
        try {
            const rows = await api(`/api/admin/shift-assignments?date=${date}`);
            state.shiftAssignments = rows;
            state.shiftAssignments.forEach(a => { /* keep ref */ });
            const html = rows.map(a => `
                <tr>
                    <td>${a.id}</td>
                    <td>${a.shiftName || a.shiftId} (${a.shiftStartTime}–${a.shiftEndTime})</td>
                    <td>${a.userName || a.userId}</td>
                    <td>${a.date}</td>
                    <td><span class="badge ${badgeForStatus(a.status)}">${statusTextForShift(a.status)}</span></td>
                    <td>${a.notes || ''}</td>
                    <td>
                        <button class="danger" data-del-ass="${a.id}">${t('common.delete')}</button>
                    </td>
                </tr>
            `).join('');
            $('#ass-table').innerHTML = `
                <table>
                    <thead><tr><th>#</th><th>Ca</th><th>Nhân viên</th><th>Ngày</th><th>Trạng thái</th><th>Ghi chú</th><th>Hành động</th></tr></thead>
                    <tbody>${html || `<tr><td colspan="7">${t('common.empty')}</td></tr>`}</tbody>
                </table>
            `;
            $$('[data-del-ass]').forEach(b => b.onclick = () => deleteAssignment(b.dataset.delAss));
            $('#add-assignment').onclick = () => openAssignmentModal(date);
        } catch (e) { $('#ass-table').innerHTML = `<p class="error">${e.message}</p>`; }
    };
    $('#ass-refresh').onclick = load;
    $('#ass-date').onchange = load;
    load();
}

async function openAssignmentModal(date) {
    let shifts, users;
    try {
        [shifts, users] = await Promise.all([
            api('/api/admin/shifts'),
            api('/api/admin/users?size=1000'),
        ]);
    } catch (e) { alert(e.message); return; }
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    modal.innerHTML = `
        <div class="modal">
            <h2>Phân ca</h2>
            <div class="row">
                <div><label>Ca</label>
                    <select id="a-shift">${shifts.map(s => `<option value="${s.id}">${s.name} (${s.startTime}–${s.endTime})</option>`).join('')}</select>
                </div>
                <div><label>Nhân viên</label>
                    <select id="a-user">${users.items.filter(u => u.status === 'ACTIVE').map(u => `<option value="${u.id}">${u.fullName} (${u.username})</option>`).join('')}</select>
                </div>
            </div>
            <div class="row">
                <div><label>Ngày</label><input type="date" id="a-date" value="${date}" /></div>
                <div><label>Trạng thái</label>
                    <select id="a-status">
                        <option value="SCHEDULED">SCHEDULED</option>
                        <option value="CONFIRMED">CONFIRMED</option>
                    </select>
                </div>
            </div>
            <label>Ghi chú</label><textarea id="a-notes"></textarea>
            <div class="actions">
                <button class="ghost" id="a-cancel">${t('common.cancel')}</button>
                <button class="primary" id="a-save">${t('common.save')}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#a-cancel').onclick = () => modal.remove();
    $('#a-save').onclick = async () => {
        try {
            await api('/api/admin/shift-assignments', { method: 'POST', body: {
                shiftId: parseInt($('#a-shift').value, 10),
                userId: parseInt($('#a-user').value, 10),
                date: $('#a-date').value,
                status: $('#a-status').value,
                notes: $('#a-notes').value,
            }});
            modal.remove();
            $('#ass-refresh').click();
        } catch (e) { alert(e.message); }
    };
}

async function deleteAssignment(id) {
    if (!confirm('Xóa phân ca này?')) return;
    try { await api(`/api/admin/shift-assignments/${id}`, { method: 'DELETE' }); $('#ass-refresh').click(); }
    catch (e) { alert(e.message); }
}

function statusTextForShift(s) {
    const dict = {
        SCHEDULED: 'Đã lên lịch', CONFIRMED: 'Đã xác nhận', ACCEPTED: 'Chấp nhận',
        REJECTED: 'Từ chối', CHANGE_REQUESTED: 'Yêu cầu đổi', CANCELLED: 'Đã hủy',
        COMPLETED: 'Hoàn thành', SWAPPED: 'Đã đổi ca',
    };
    return dict[s] || s;
}

function badgeForStatus(s) {
    const ok = ['ACCEPTED', 'CONFIRMED', 'COMPLETED'];
    const warn = ['CHANGE_REQUESTED', 'SWAPPED', 'SCHEDULED'];
    const bad = ['REJECTED', 'CANCELLED'];
    if (ok.includes(s)) return 'active';
    if (warn.includes(s)) return 'featured';
    if (bad.includes(s)) return 'hidden';
    return 'hidden';
}

// --- Checklists ---
async function renderChecklists() {
    const root = $('#content');
    root.innerHTML = `
        <div class="toolbar">
            <select id="cl-zone"><option value="">Tất cả khu vực</option></select>
            <button class="primary" id="cl-add">+ Thêm checklist</button>
        </div>
        <div id="cl-list">Đang tải…</div>
    `;
    try {
        const [checklists, zones] = await Promise.all([
            api('/api/admin/checklists'),
            api('/api/admin/zones'),
        ]);
        state.checklists = checklists;
        state.zones = zones;
        $('#cl-zone').innerHTML = '<option value="">Tất cả khu vực</option>' +
            zones.map(z => `<option value="${z.id}">${(z.translations||[]).find(t=>t.lang==='vi')?.name || z.code}</option>`).join('');
        renderChecklistCards(checklists);
        $('#cl-add').onclick = () => openChecklistModal(null);
        $('#cl-zone').onchange = () => {
            const zid = parseInt($('#cl-zone').value || '0', 10);
            const filtered = zid ? state.checklists.filter(c => c.zoneId === zid) : state.checklists;
            renderChecklistCards(filtered);
        };
    } catch (e) { $('#cl-list').innerHTML = `<p class="error">${e.message}</p>`; }
}

function renderChecklistCards(items) {
    const root = $('#cl-list');
    if (!items.length) { root.innerHTML = `<p>${t('common.empty')}</p>`; return; }
    root.innerHTML = `<div class="grid-2">${items.map(c => {
        const vi = (c.translations || []).find(t => t.lang === 'vi');
        return `
            <div class="zone-card">
                <div class="zone-card-head">
                    <strong>${vi?.title || '—'}</strong>
                    <span class="badge ${c.active ? 'active' : 'hidden'}">${c.active ? 'ACTIVE' : 'INACTIVE'}</span>
                </div>
                <div class="muted">Khu vực: ${c.zoneCode || c.zoneId} — ${c.zoneName || ''}</div>
                <div style="margin:8px 0">${(c.tasks || []).map(task => {
                    const tvi = (task.translations || []).find(x => x.lang === 'vi');
                    return `<div class="task-row">
                        <div>
                            <div>${tvi?.title || '—'} ${task.required ? '<span class="badge featured">Bắt buộc</span>' : ''}</div>
                            <div class="task-meta">${tvi?.description || ''}</div>
                        </div>
                    </div>`;
                }).join('') || '<div class="muted">Chưa có task</div>'}</div>
                <div class="row">
                    <button class="ghost" data-edit-cl="${c.id}">${t('common.edit')}</button>
                    <button class="danger" data-del-cl="${c.id}">${t('common.delete')}</button>
                </div>
            </div>
        `;
    }).join('')}</div>`;
    $$('[data-edit-cl]').forEach(b => b.onclick = () => openChecklistModal(state.checklists.find(x => x.id == b.dataset.editCl)));
    $$('[data-del-cl]').forEach(b => b.onclick = () => deleteChecklist(b.dataset.delCl));
}

async function openChecklistModal(checklist) {
    const isEdit = !!checklist;
    const vi = checklist ? (checklist.translations || []).find(t => t.lang === 'vi') : null;
    const ko = checklist ? (checklist.translations || []).find(t => t.lang === 'ko') : null;
    const initialTasks = checklist?.tasks || [];
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    modal.innerHTML = `
        <div class="modal" style="width:880px">
            <h2>${isEdit ? 'Sửa checklist' : 'Thêm checklist'}</h2>
            <div class="row">
                <div><label>Khu vực</label>
                    <select id="cl-zone-sel">${(state.zones || []).map(z =>
                        `<option value="${z.id}" ${checklist?.zoneId === z.id ? 'selected' : ''}>${(z.translations||[]).find(t=>t.lang==='vi')?.name || z.code}</option>`
                    ).join('')}</select>
                </div>
                <div><label>Thứ tự</label><input type="number" id="cl-sort" value="${checklist?.sortOrder ?? 0}" /></div>
            </div>
            <div class="row">
                <div><label>Tiêu đề (Tiếng Việt)</label><input id="cl-title-vi" value="${vi?.title || ''}" /></div>
                <div><label>Tiêu đề (한국어)</label><input id="cl-title-ko" value="${ko?.title || ''}" /></div>
            </div>
            <div class="row">
                <div><label>Mô tả (Tiếng Việt)</label><textarea id="cl-desc-vi">${vi?.description || ''}</textarea></div>
                <div><label>Mô tả (한국어)</label><textarea id="cl-desc-ko">${ko?.description || ''}</textarea></div>
            </div>
            <h3>Tasks</h3>
            <div id="cl-tasks">${initialTasks.map((task, i) => renderTaskEditor(task, i)).join('')}</div>
            <button class="ghost" id="cl-add-task">+ Thêm task</button>
            <div class="actions">
                <button class="ghost" id="cl-cancel">${t('common.cancel')}</button>
                <button class="primary" id="cl-save">${t('common.save')}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#cl-add-task').onclick = () => {
        const div = document.createElement('div');
        div.innerHTML = renderTaskEditor(null, $('#cl-tasks').children.length);
        $('#cl-tasks').appendChild(div.firstElementChild);
    };
    $('#cl-cancel').onclick = () => modal.remove();
    $('#cl-save').onclick = async () => {
        const tasks = Array.from($('#cl-tasks').children).map((row, idx) => {
            const id = row.dataset.taskId ? parseInt(row.dataset.taskId, 10) : null;
            const required = row.querySelector('[data-task-required]').value === 'true';
            const active = row.querySelector('[data-task-active]').value === 'true';
            const sortOrder = parseInt(row.querySelector('[data-task-sort]').value || '0', 10);
            return {
                id: id,
                required, active, sortOrder,
                translations: [
                    { lang: 'vi', title: row.querySelector('[data-task-title-vi]').value, description: row.querySelector('[data-task-desc-vi]').value },
                    { lang: 'ko', title: row.querySelector('[data-task-title-ko]').value, description: row.querySelector('[data-task-desc-ko]').value },
                ],
            };
        });
        const payload = {
            zoneId: parseInt($('#cl-zone-sel').value, 10),
            sortOrder: parseInt($('#cl-sort').value || '0', 10),
            active: true,
            translations: [
                { lang: 'vi', title: $('#cl-title-vi').value.trim(), description: $('#cl-desc-vi').value },
                { lang: 'ko', title: $('#cl-title-ko').value.trim(), description: $('#cl-desc-ko').value },
            ],
            tasks,
        };
        try {
            if (isEdit) await api(`/api/admin/checklists/${checklist.id}`, { method: 'PUT', body: payload });
            else await api('/api/admin/checklists', { method: 'POST', body: payload });
            modal.remove(); renderChecklists();
        } catch (e) { alert(e.message); }
    };
}

function renderTaskEditor(task, idx) {
    const vi = task ? (task.translations || []).find(t => t.lang === 'vi') : null;
    const ko = task ? (task.translations || []).find(t => t.lang === 'ko') : null;
    return `
        <div class="task-row" data-task-id="${task?.id || ''}" style="flex-direction:column;align-items:stretch">
            <div class="row">
                <select data-task-required>
                    <option value="false" ${task?.required ? '' : 'selected'}>Tuỳ chọn</option>
                    <option value="true" ${task?.required ? 'selected' : ''}>Bắt buộc</option>
                </select>
                <select data-task-active>
                    <option value="true" ${task?.active !== false ? 'selected' : ''}>Active</option>
                    <option value="false" ${task?.active === false ? 'selected' : ''}>Inactive</option>
                </select>
                <input type="number" data-task-sort placeholder="Sort" value="${task?.sortOrder ?? idx}" style="width:80px" />
                <button class="danger" data-remove-task>Xoá</button>
            </div>
            <div class="row">
                <input data-task-title-vi placeholder="Tiêu đề (VI)" value="${vi?.title || ''}" />
                <input data-task-title-ko placeholder="Tiêu đề (KO)" value="${ko?.title || ''}" />
            </div>
            <div class="row">
                <textarea data-task-desc-vi placeholder="Mô tả (VI)">${vi?.description || ''}</textarea>
                <textarea data-task-desc-ko placeholder="Mô tả (KO)">${ko?.description || ''}</textarea>
            </div>
        </div>
    `;
}

async function deleteChecklist(id) {
    if (!confirm('Xóa checklist này?')) return;
    try { await api(`/api/admin/checklists/${id}`, { method: 'DELETE' }); renderChecklists(); }
    catch (e) { alert(e.message); }
}

// --- Check-ins (admin) ---
async function renderCheckIns() {
    const root = $('#content');
    root.innerHTML = `<div class="toolbar"><select id="ci-zone"><option value="">Tất cả khu vực</option></select><select id="ci-action"><option value="">Tất cả</option><option value="CHECK_IN">CHECK_IN</option><option value="CHECK_OUT">CHECK_OUT</option></select><select id="ci-user"><option value="">Tất cả nhân viên</option></select><button class="primary" id="ci-refresh">Tải lại</button></div><div id="ci-table">Đang tải…</div>`;
    try {
        const [zones, users] = await Promise.all([api('/api/admin/zones'), api('/api/admin/users?size=1000')]);
        $('#ci-zone').innerHTML = '<option value="">Tất cả khu vực</option>' + zones.map(z => `<option value="${z.id}">${(z.translations||[]).find(t=>t.lang==='vi')?.name || z.code}</option>`).join('');
        $('#ci-user').innerHTML = '<option value="">Tất cả nhân viên</option>' + users.items.map(u => `<option value="${u.id}">${u.fullName} (${u.username})</option>`).join('');
        const load = async () => {
            const params = new URLSearchParams();
            const uid = $('#ci-user').value; if (uid) params.set('userId', uid);
            const zid = $('#ci-zone').value; if (zid) params.set('zoneId', zid);
            const act = $('#ci-action').value; if (act) params.set('action', act);
            try {
                const data = await api('/api/admin/check-ins?' + params.toString());
                const rows = data.items.map(r => `<tr><td>${r.id}</td><td>${r.userName||r.userId}</td><td>${r.zoneCode||r.zoneId}</td><td><span class="badge ${r.action==='CHECK_IN'?'active':'hidden'}">${r.action}</span></td><td>${r.notes||''}</td><td>${r.deviceId||''}</td><td>${r.createdAt||''}</td></tr>`).join('');
                $('#ci-table').innerHTML = `<table><thead><tr><th>#</th><th>Nhân viên</th><th>Khu vực</th><th>Hành động</th><th>Ghi chú</th><th>Thiết bị</th><th>Thời gian</th></tr></thead><tbody>${rows || `<tr><td colspan="7">${t('common.empty')}</td></tr>`}</tbody></table>`;
            } catch (e) { $('#ci-table').innerHTML = `<p class="error">${e.message}</p>`; }
        };
        $('#ci-refresh').onclick = load; $('#ci-user').onchange = load; $('#ci-zone').onchange = load; $('#ci-action').onchange = load;
        load();
    } catch (e) { $('#ci-table').innerHTML = `<p class="error">${e.message}</p>`; }
}

// --- Activity log ---
async function renderActivity() {
    const root = $('#content');
    root.innerHTML = `<div class="toolbar"><input id="al-action" placeholder="Hành động" /><input id="al-entity" placeholder="Entity" /><input id="al-limit" type="number" value="100" style="width:80px" /><button class="primary" id="al-refresh">Tải lại</button></div><div id="al-table">Đang tải…</div>`;
    const load = async () => {
        const params = new URLSearchParams();
        const act = $('#al-action').value.trim(); if (act) params.set('action', act);
        const ent = $('#al-entity').value.trim(); if (ent) params.set('entity', ent);
        params.set('limit', $('#al-limit').value || '100');
        try {
            const rows = await api('/api/admin/activity-logs?' + params.toString());
            const html = rows.map(r => `<tr><td>${r.id}</td><td>${r.actorName||r.actorUserId||''}</td><td><span class="badge ${r.result==='SUCCESS'?'active':'hidden'}">${r.action||''}</span></td><td>${r.entity||''} ${r.entityId?'#'+r.entityId:''}</td><td>${r.targetName||r.targetUserId||''}</td><td>${r.ip||''}</td><td>${r.createdAt||''}</td></tr>`).join('');
            $('#al-table').innerHTML = `<table><thead><tr><th>#</th><th>Actor</th><th>Hành động</th><th>Entity</th><th>Target</th><th>IP</th><th>Thời gian</th></tr></thead><tbody>${html || `<tr><td colspan="7">${t('common.empty')}</td></tr>`}</tbody></table>`;
        } catch (e) { $('#al-table').innerHTML = `<p class="error">${e.message}</p>`; }
    };
    $('#al-refresh').onclick = load; load();
}

// --- Notifications (admin) ---
async function renderNotifications() {
    const root = $('#content');
    root.innerHTML = `<div class="toolbar"><input id="nt-type" placeholder="Loại" /><input id="nt-user" type="number" placeholder="User ID" /><button class="primary" id="nt-refresh">Tải lại</button></div><div id="nt-table">Đang tải…</div>`;
    const load = async () => {
        const params = new URLSearchParams();
        const tp = $('#nt-type').value.trim(); if (tp) params.set('type', tp);
        const uid = $('#nt-user').value; if (uid) params.set('userId', uid);
        try {
            const data = await api('/api/admin/notifications?' + params.toString());
            state.notifications = data.items;
            const rows = data.items.map(n => `<tr><td>${n.id}</td><td>${n.username||n.userId}</td><td><span class="badge featured">${n.type||''}</span></td><td>${(n.titleVi||'').slice(0,60)}</td><td>${(n.bodyVi||'').slice(0,60)}</td><td>${n.payloadJson?`<span class="json-cell" title="${escapeHtml(n.payloadJson)}">${escapeHtml((n.payloadJson||'').slice(0,40))}</span>`:''}</td><td>${n.readAt?'<span class="badge active">Đã đọc</span>':'<span class="badge hidden">Chưa</span>'}</td><td>${n.createdAt||''}</td><td><button class="ghost" data-view-events="${n.id}">Events</button></td></tr>`).join('');
            $('#nt-table').innerHTML = `<table><thead><tr><th>#</th><th>User</th><th>Loại</th><th>Tiêu đề</th><th>Nội dung</th><th>Payload</th><th>Trạng thái</th><th>Thời gian</th><th></th></tr></thead><tbody>${rows || `<tr><td colspan="9">${t('common.empty')}</td></tr>`}</tbody></table>`;
            $$('[data-view-events]').forEach(b => b.onclick = () => openEventsModal(b.dataset.viewEvents));
        } catch (e) { $('#nt-table').innerHTML = `<p class="error">${e.message}</p>`; }
    };
    $('#nt-refresh').onclick = load; load();
}

function escapeHtml(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

async function openEventsModal(id) {
    let events;
    try { events = await api(`/api/admin/notifications/${id}/events`); } catch (e) { alert(e.message); return; }
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    const rows = events.map(e => `<tr><td>${e.id}</td><td>${e.channel}</td><td>${e.status}</td><td>${e.attemptNumber}</td><td>${e.errorCode||''}</td><td>${e.errorMessage||''}</td><td>${e.createdAt||''}</td></tr>`).join('') || `<tr><td colspan="7">${t('common.empty')}</td></tr>`;
    modal.innerHTML = `<div class="modal"><h2>Events #${id}</h2><table><thead><tr><th>#</th><th>Channel</th><th>Status</th><th>Attempt</th><th>Code</th><th>Message</th><th>Created</th></tr></thead><tbody>${rows}</tbody></table><div class="actions"><button class="ghost" id="ev-close">Đóng</button></div></div>`;
    document.body.appendChild(modal);
    $('#ev-close').onclick = () => modal.remove();
}

// --- Device tokens (admin) ---
async function renderDevices() {
    const root = $('#content');
    root.innerHTML = `<div class="toolbar"><select id="dv-active"><option value="">Tất cả</option><option value="true">Active</option><option value="false">Inactive</option></select><button class="primary" id="dv-refresh">Tải lại</button></div><div id="dv-stats"></div><div id="dv-table">Đang tải…</div>`;
    const load = async () => {
        const params = new URLSearchParams();
        const act = $('#dv-active').value; if (act) params.set('active', act);
        try {
            const [tokens, stats] = await Promise.all([api('/api/admin/device-tokens?' + params.toString()), api('/api/admin/device-tokens/stats')]);
            const top = stats.slice().sort((a, b) => b.activeCount - a.activeCount).slice(0, 6);
            $('#dv-stats').innerHTML = `<div class="stat-grid">${top.map(s => `<div class="stat"><div class="label">${escapeHtml(s.displayName||s.username)}</div><div class="value">${s.activeCount}</div></div>`).join('')}</div>`;
            const rows = tokens.map(t => `<tr><td>${t.id}</td><td>${t.userId}</td><td><span class="badge ${t.platform==='ANDROID'?'active':'featured'}">${t.platform}</span></td><td>${t.deviceId||''}</td><td>${t.appVersion||''}</td><td><span class="json-cell">${t.tokenPreview}</span> <span class="muted small">(${t.tokenLength})</span></td><td>${t.lastSeenAt||''}</td><td>${t.isActive?'<span class="badge active">A</span>':'<span class="badge hidden">I</span>'}</td></tr>`).join('');
            $('#dv-table').innerHTML = `<table><thead><tr><th>#</th><th>User</th><th>Platform</th><th>Device</th><th>App</th><th>Token</th><th>Last seen</th><th>Status</th></tr></thead><tbody>${rows || `<tr><td colspan="8">${t('common.empty')}</td></tr>`}</tbody></table>`;
        } catch (e) { $('#dv-table').innerHTML = `<p class="error">${e.message}</p>`; }
    };
    $('#dv-refresh').onclick = load; $('#dv-active').onchange = load; load();
}

async function renderZones() {
    const root = $('#content');
    root.innerHTML = `
        <div class="toolbar"><button class="primary" id="add-zone">+ Thêm khu vực</button></div>
        <div id="zone-list">Đang tải…</div>
    `;
    try {
        const zones = await api('/api/admin/zones');
        state.zones = zones;
        const cards = zones.map(z => {
            const vi = (z.translations || []).find(t => t.lang === 'vi');
            const ko = (z.translations || []).find(t => t.lang === 'ko');
            return `
                <div class="card zone-card" data-zone="${z.id}">
                    <div class="zone-card-head">
                        <span class="color-dot" style="background:${z.color}"></span>
                        <strong>${vi?.name || z.code}</strong>
                        <span class="badge ${z.status === 'ACTIVE' ? 'active' : 'hidden'}">${z.status}</span>
                    </div>
                    <div class="muted">${vi?.description || ''}</div>
                    <div class="muted small">한국: ${ko?.name || '—'}</div>
                    <div class="row" style="margin-top:8px">
                        <button class="ghost" data-edit-zone="${z.id}">${t('common.edit')}</button>
                        <button class="ghost" data-view-zone-ass="${z.id}">Phân công hiện tại</button>
                        <button class="danger" data-del-zone="${z.id}">${t('common.delete')}</button>
                    </div>
                </div>
            `;
        }).join('');
        $('#zone-list').innerHTML = `<div class="grid-2">${cards || `<p>${t('common.empty')}</p>`}</div>`;
        $('#add-zone').onclick = () => openZoneModal(null);
        $$('[data-edit-zone]').forEach(b => b.onclick = () => openZoneModal(state.zones.find(x => x.id == b.dataset.editZone)));
        $$('[data-del-zone]').forEach(b => b.onclick = () => deleteZone(b.dataset.delZone));
        $$('[data-view-zone-ass]').forEach(b => b.onclick = () => openZoneAssignmentsModal(b.dataset.viewZoneAss));
    } catch (e) { $('#zone-list').innerHTML = `<p class="error">${e.message}</p>`; }
}

function openZoneModal(zone) {
    const isEdit = !!zone;
    const vi = zone ? (zone.translations || []).find(t => t.lang === 'vi') : null;
    const ko = zone ? (zone.translations || []).find(t => t.lang === 'ko') : null;
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    modal.innerHTML = `
        <div class="modal">
            <h2>${isEdit ? 'Sửa khu vực' : 'Thêm khu vực'}</h2>
            <div class="row">
                <div><label>Mã (code)</label><input id="z-code" value="${zone?.code || ''}" /></div>
                <div><label>Màu (hex)</label><input id="z-color" value="${zone?.color || '#3B82F6'}" /></div>
            </div>
            <div class="row">
                <div><label>Trạng thái</label>
                    <select id="z-status">
                        <option value="ACTIVE" ${zone?.status === 'ACTIVE' ? 'selected' : ''}>ACTIVE</option>
                        <option value="DISABLED" ${zone?.status === 'DISABLED' ? 'selected' : ''}>DISABLED</option>
                    </select>
                </div>
                <div><label>Số nhân viên yêu cầu</label><input type="number" id="z-req" value="${zone?.requiredStaff ?? 0}" /></div>
            </div>
            <div class="row">
                <div><label>Tên (Tiếng Việt)</label><input id="z-name-vi" value="${vi?.name || ''}" /></div>
                <div><label>Tên (한국어)</label><input id="z-name-ko" value="${ko?.name || ''}" /></div>
            </div>
            <div class="row">
                <div><label>Mô tả (Tiếng Việt)</label><textarea id="z-desc-vi">${vi?.description || ''}</textarea></div>
                <div><label>Mô tả (한국어)</label><textarea id="z-desc-ko">${ko?.description || ''}</textarea></div>
            </div>
            <div class="row">
                <div><label>Thứ tự</label><input type="number" id="z-sort" value="${zone?.sortOrder ?? 0}" /></div>
            </div>
            <div class="actions">
                <button class="ghost" id="z-cancel">${t('common.cancel')}</button>
                <button class="primary" id="z-save">${t('common.save')}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#z-cancel').onclick = () => modal.remove();
    $('#z-save').onclick = async () => {
        const payload = {
            code: $('#z-code').value.trim(),
            color: $('#z-color').value.trim(),
            status: $('#z-status').value,
            requiredStaff: parseInt($('#z-req').value || '0', 10),
            sortOrder: parseInt($('#z-sort').value || '0', 10),
            translations: [
                { lang: 'vi', name: $('#z-name-vi').value.trim(), description: $('#z-desc-vi').value },
                { lang: 'ko', name: $('#z-name-ko').value.trim(), description: $('#z-desc-ko').value },
            ],
        };
        if (!payload.code || !payload.color) { alert('Vui lòng nhập mã và màu'); return; }
        try {
            if (isEdit) await api(`/api/admin/zones/${zone.id}`, { method: 'PUT', body: payload });
            else await api('/api/admin/zones', { method: 'POST', body: payload });
            modal.remove(); renderZones();
        } catch (e) { alert(e.message); }
    };
}

async function deleteZone(id) {
    if (!confirm('Xóa khu vực này?')) return;
    try { await api(`/api/admin/zones/${id}`, { method: 'DELETE' }); renderZones(); }
    catch (e) { alert(e.message); }
}

async function openZoneAssignmentsModal(zoneId) {
    let current;
    try {
        current = await api(`/api/admin/zones/${zoneId}/current`);
    } catch (e) { alert(e.message); return; }
    const zone = state.zones.find(z => z.id == zoneId);
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    const rows = current.length
        ? current.map(a => `<tr><td>${a.userName || a.userId}</td><td>${a.effectiveFrom || ''}</td><td>${a.effectiveTo || ''}</td></tr>`).join('')
        : `<tr><td colspan="3">${t('common.empty')}</td></tr>`;
    modal.innerHTML = `
        <div class="modal">
            <h2>Phân công hiện tại — ${zone?.code || zoneId}</h2>
            <table>
                <thead><tr><th>Nhân viên</th><th>Từ</th><th>Đến</th></tr></thead>
                <tbody>${rows}</tbody>
            </table>
            <div class="actions">
                <button class="ghost" id="za-cancel">Đóng</button>
                <button class="primary" id="za-assign">+ Phân công</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#za-cancel').onclick = () => modal.remove();
    $('#za-assign').onclick = () => { modal.remove(); openAssignZoneModal(zoneId); };
}

async function openAssignZoneModal(zoneId) {
    let users;
    try { users = await api('/api/admin/users?size=1000'); }
    catch (e) { alert(e.message); return; }
    const modal = document.createElement('div');
    modal.className = 'modal-backdrop';
    modal.innerHTML = `
        <div class="modal">
            <h2>Phân công vào khu vực</h2>
            <label>Nhân viên</label>
            <select id="au-user">${users.items.filter(u => u.status === 'ACTIVE').map(u => `<option value="${u.id}">${u.fullName} (${u.username})</option>`).join('')}</select>
            <label>Lý do</label><textarea id="au-reason"></textarea>
            <div class="actions">
                <button class="ghost" id="au-cancel">${t('common.cancel')}</button>
                <button class="primary" id="au-save">${t('common.save')}</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    $('#au-cancel').onclick = () => modal.remove();
    $('#au-save').onclick = async () => {
        try {
            await api('/api/admin/zone-assignments', { method: 'POST', body: {
                userId: parseInt($('#au-user').value, 10),
                zoneId: parseInt(zoneId, 10),
                reason: $('#au-reason').value,
            }});
            modal.remove();
            renderZones();
        } catch (e) { alert(e.message); }
    };
}

function render() {
    if (state.view === 'dashboard') renderDashboard();
    else if (state.view === 'categories') renderCategories();
    else if (state.view === 'foods') renderFoods();
    else if (state.view === 'users') renderUsers();
    else if (state.view === 'store') renderStore();
    else if (state.view === 'shifts') renderShifts();
    else if (state.view === 'zones') renderZones();
    else if (state.view === 'checklists') renderChecklists();
    else if (state.view === 'checkins') renderCheckIns();
    else if (state.view === 'activity') renderActivity();
    else if (state.view === 'notifications') renderNotifications();
    else if (state.view === 'devices') renderDevices();
}