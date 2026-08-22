const state = {
  referenceDate: new Date(),
  csrf: null,
  user: null,
  currentView: 'dashboard'
};

const views = ['dashboard', 'agenda', 'processos', 'equipe'];
const categoryLabels = {
  DEADLINE: 'Prazo',
  HEARING: 'Audiência',
  PENDING_DOCUMENT: 'Documentação',
  CLIENT_MEETING: 'Reunião com cliente',
  INTERNAL_MEETING: 'Reunião interna',
  CASE_PENDING_ITEM: 'Resposta em processo',
  URGENT_PROTOCOL: 'Protocolo urgente',
  DOCUMENT_COLLECTION: 'Levantamento de documentos',
  OTHER: 'Outra atividade'
};
const statusLabels = {
  PENDING: 'Pendente',
  IN_PROGRESS: 'Em andamento',
  COMPLETED: 'Concluída',
  CANCELED: 'Cancelada'
};
const roleLabels = { ADMIN: 'Administrador', USER: 'Usuário' };

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);
const toIsoDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};
const formatDate = (value, options = { day: '2-digit', month: 'short', year: 'numeric' }) =>
  value ? new Intl.DateTimeFormat('pt-BR', options).format(new Date(`${value}T12:00:00`)) : null;
const initials = (name = 'AJ') => name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
const escapeHtml = (value = '') => String(value).replace(/[&<>"']/g, (char) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
}[char]));

async function request(url, options = {}) {
  const response = await fetch(url, {
    credentials: 'same-origin',
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
  });

  if (!response.ok) {
    let detail = '';
    try { detail = (await response.json()).detail || ''; } catch (_) { /* resposta sem JSON */ }
    const error = new Error(
      response.status === 401 ? 'Sua sessão expirou. Entre novamente.' :
        response.status === 403 ? 'Você não tem permissão para acessar estes dados.' :
          detail || 'Não foi possível carregar os dados.'
    );
    error.status = response.status;
    throw error;
  }

  return response.status === 204 ? null : response.json();
}

async function loadCsrf() { state.csrf = await request('/api/auth/csrf'); }
function csrfHeaders() { return state.csrf ? { [state.csrf.headerName]: state.csrf.token } : {}; }

function normalizeView(hash = window.location.hash) {
  const view = hash.replace('#', '');
  return views.includes(view) ? view : 'dashboard';
}

function showApp(user) {
  state.user = user;
  $('#login-view').hidden = true;
  $('#app-view').hidden = false;
  const name = user.name || user.fullName || user.email?.split('@')[0] || 'Advogado';
  $('#user-name').textContent = name;
  $('#greeting-name').textContent = name.split(' ')[0];
  $('#user-role').textContent = roleLabels[user.role] || 'Equipe jurídica';
  $('#user-avatar').textContent = initials(name);
  navigateTo(normalizeView());
}

function showLogin(message = '') {
  state.user = null;
  $('#app-view').hidden = true;
  $('#login-view').hidden = false;
  $('#login-error').textContent = message;
}

function navigateTo(view) {
  const selectedView = views.includes(view) ? view : 'dashboard';
  state.currentView = selectedView;

  $$('[data-view-panel]').forEach((panel) => { panel.hidden = panel.dataset.viewPanel !== selectedView; });
  $$('[data-view]').forEach((link) => {
    const active = link.dataset.view === selectedView;
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });

  if (!state.user) return;
  refreshCurrentView();
}

function refreshCurrentView() {
  if (state.currentView === 'dashboard') return loadDashboard();
  if (state.currentView === 'agenda') return loadAgenda();
  if (state.currentView === 'processos') return loadProcesses();
  return loadTeam();
}

function taskDate(task) { return task.scheduledDate || task.dueDate; }
function allTasks(data) { return (data.weeklyTasks || []).flatMap((day) => day.tasks || []); }
function taskTime(task) {
  if (task.scheduledTime) return task.scheduledTime.slice(0, 5);
  if (task.dueTime) return `limite ${task.dueTime.slice(0, 5)}`;
  return 'Sem horário';
}
function taskCard(task) {
  const priority = task.priority === 'URGENT' ? 'urgent' : task.priority === 'HIGH' ? 'high' : '';
  return `<article class="task-card ${priority}"><span class="task-time">${taskTime(task)}</span><span class="task-title">${escapeHtml(task.title)}</span><span class="task-type">${categoryLabels[task.category] || 'Atividade'}</span></article>`;
}

function renderSummary(data) {
  $('#pending-total').textContent = data.totalPending ?? 0;
  $('#overdue-total').textContent = data.totalOverdue ?? 0;
  $('#progress-total').textContent = data.totalInProgress ?? 0;
  $('#completed-total').textContent = data.totalCompleted ?? 0;
  const start = formatDate(data.weekStart, { day: '2-digit', month: 'short' });
  const end = formatDate(data.weekEnd, { day: '2-digit', month: 'short', year: 'numeric' });
  $('#week-label').textContent = `${start} a ${end}`;
  $('#today-label').textContent = formatDate(data.referenceDate, { day: '2-digit', month: 'long' });
}

function renderSchedule(data) {
  const tasks = allTasks(data);
  const priorityTasks = tasks.filter((task) => ['HIGH', 'URGENT'].includes(task.priority));
  const groups = [
    ['DEADLINE', data.upcomingTasks || []],
    ['HEARING', tasks.filter((task) => task.category === 'HEARING')],
    ['PENDING_DOCUMENT', tasks.filter((task) => task.category === 'PENDING_DOCUMENT')],
    ['PRIORITY', priorityTasks],
    ['CLIENT_MEETING', tasks.filter((task) => task.category === 'CLIENT_MEETING')],
    ['INTERNAL_MEETING', tasks.filter((task) => task.category === 'INTERNAL_MEETING')],
    ['CASE_PENDING_ITEM', tasks.filter((task) => task.category === 'CASE_PENDING_ITEM')]
  ];
  const headers = ['Prazos próximos', 'Audiência', 'Documentação pendente', 'Prioridade da semana', 'Reunião com cliente', 'Reunião interna', 'Resposta em processo'];
  const grid = $('#schedule-grid');
  grid.innerHTML = groups.map(([, items], index) => `<section class="day-column ${index === 0 ? 'today' : ''}"><header class="day-header"><span class="day-name">${headers[index]}</span><span class="day-number">${items.length}</span></header><div class="task-list">${items.length ? items.slice(0, 4).map(taskCard).join('') : '<span class="empty-day">Tudo em dia</span>'}</div></section>`).join('');
  $('#empty-state').hidden = tasks.length > 0;
}

async function loadDashboard() {
  $('#dashboard-error').textContent = '';
  $('#refresh-button').disabled = true;
  try {
    const data = await request(`/api/dashboard?referenceDate=${toIsoDate(state.referenceDate)}`);
    renderSummary(data);
    renderSchedule(data);
  } catch (error) {
    if (error.status === 401) showLogin(error.message);
    else $('#dashboard-error').textContent = error.message;
  } finally {
    $('#refresh-button').disabled = false;
  }
}

function taskActions(task, allowDelete = false) {
  const remove = allowDelete && task.status === 'CANCELED'
    ? `<button class="record-action danger-action" type="button" data-task-id="${task.id}" data-task-action="remove" data-task-title="${escapeHtml(task.title)}">Remover</button>`
    : '';
  if (task.status === 'CANCELED') {
    return `<button class="record-action" type="button" data-task-id="${task.id}" data-task-action="reopen">Reabrir</button>${remove}`;
  }
  if (task.status === 'COMPLETED') {
    return `<button class="record-action" type="button" data-task-id="${task.id}" data-task-action="reopen">Reabrir</button>`;
  }
  const start = task.status === 'PENDING'
    ? `<button class="record-action" type="button" data-task-id="${task.id}" data-task-action="start">Iniciar</button>`
    : '';
  return `${start}<button class="record-action" type="button" data-task-id="${task.id}" data-task-action="complete">Concluir</button><button class="record-action cancel-action" type="button" data-task-id="${task.id}" data-task-action="cancel">Cancelar</button>`;
}

function renderTaskRows(items, targetSelector, emptyTitle, emptyText, allowDelete = false) {
  const target = $(targetSelector);
  if (!items.length) {
    target.innerHTML = `<div class="list-message"><strong>${emptyTitle}</strong>${emptyText}</div>`;
    return;
  }

  target.innerHTML = items.map((task) => {
    const date = taskDate(task);
    const responsible = task.responsibleUser?.name || 'Sem responsável';
    const priorityClass = task.priority === 'URGENT' ? 'priority-urgent' : task.priority === 'HIGH' ? 'priority-high' : '';
    return `<article class="record-item ${priorityClass}">
      <div>
        <div class="record-title-row"><h2 class="record-title">${escapeHtml(task.title)}</h2><span class="status-badge status-${task.status.toLowerCase().replace('_', '-')}">${statusLabels[task.status] || task.status}</span></div>
        <div class="record-meta"><span>${categoryLabels[task.category] || 'Atividade'}</span><span>${date ? formatDate(date) : 'Sem data definida'}</span><span>${escapeHtml(responsible)}</span></div>
      </div>
      <div class="record-actions">${taskActions(task, allowDelete)}</div>
    </article>`;
  }).join('');
}

async function loadAgenda() {
  const target = $('#agenda-list');
  const errorTarget = $('#agenda-error');
  target.innerHTML = '<div class="list-message">Carregando atividades...</div>';
  errorTarget.textContent = '';
  const params = new URLSearchParams({ size: '100' });
  const search = $('#agenda-search').value.trim();
  const status = $('#agenda-status').value;
  const category = $('#agenda-category').value;
  if (search) params.set('search', search);
  if (status) params.set('status', status);
  if (category) params.set('category', category);

  try {
    const data = await request(`/api/tasks?${params}`);
    renderTaskRows(data.content || [], '#agenda-list', 'Nenhuma atividade encontrada', 'Ajuste os filtros ou cadastre uma nova atividade.', true);
  } catch (error) {
    if (error.status === 401) showLogin(error.message);
    else {
      target.innerHTML = '';
      errorTarget.textContent = error.message;
    }
  }
}

async function loadProcesses() {
  const target = $('#processos-list');
  const errorTarget = $('#processos-error');
  target.innerHTML = '<div class="list-message">Carregando atividades processuais...</div>';
  errorTarget.textContent = '';
  try {
    const data = await request('/api/tasks?category=CASE_PENDING_ITEM&size=100');
    renderTaskRows(data.content || [], '#processos-list', 'Nenhuma pendência processual', 'As respostas em processo cadastradas aparecerão aqui.');
  } catch (error) {
    if (error.status === 401) showLogin(error.message);
    else {
      target.innerHTML = '';
      errorTarget.textContent = error.message;
    }
  }
}

function renderTeam(users, limited = false) {
  const target = $('#team-list');
  target.innerHTML = users.map((user) => `<article class="team-card">
    <span class="team-avatar">${initials(user.name)}</span>
    <div><strong>${escapeHtml(user.name)}</strong><small title="${escapeHtml(user.email)}">${escapeHtml(user.email)}</small></div>
    <div class="team-role"><span>${roleLabels[user.role] || user.role}</span><span><i class="active-dot ${user.active === false ? 'inactive-dot' : ''}"></i>${user.active === false ? 'Inativo' : 'Ativo'}</span></div>
  </article>`).join('');
  if (limited) {
    target.insertAdjacentHTML('beforeend', '<div class="list-message team-access-note"><strong>Visualização individual</strong>O diretório completo da equipe é restrito a administradores.</div>');
  }
}

async function loadTeam() {
  const target = $('#team-list');
  const errorTarget = $('#team-error');
  errorTarget.textContent = '';
  if (state.user.role !== 'ADMIN') {
    renderTeam([{ ...state.user, active: true }], true);
    return;
  }

  target.innerHTML = '<div class="list-message">Carregando equipe...</div>';
  try {
    const data = await request('/api/users?size=100');
    renderTeam(data.content || []);
  } catch (error) {
    if (error.status === 401) showLogin(error.message);
    else {
      target.innerHTML = '';
      errorTarget.textContent = error.message;
    }
  }
}

async function changeTaskStatus(button) {
  const taskId = button.dataset.taskId;
  const action = button.dataset.taskAction;
  if (action === 'remove') {
    const title = button.dataset.taskTitle || 'esta atividade';
    if (!window.confirm(`Remover "${title}" da agenda? Esta ação não poderá ser desfeita.`)) return;
  }
  button.disabled = true;
  try {
    if (action === 'remove') {
      await request(`/api/tasks/${taskId}`, { method: 'DELETE', headers: csrfHeaders() });
    } else if (action === 'start') {
      await request(`/api/tasks/${taskId}/status`, {
        method: 'PATCH', headers: csrfHeaders(), body: JSON.stringify({ status: 'IN_PROGRESS' })
      });
    } else {
      await request(`/api/tasks/${taskId}/${action}`, { method: 'POST', headers: csrfHeaders() });
    }
    await refreshCurrentView();
  } catch (error) {
    const errorTarget = state.currentView === 'processos' ? $('#processos-error') : $('#agenda-error');
    errorTarget.textContent = error.message;
    button.disabled = false;
  }
}

function toggleTaskModal(open) {
  $('#task-modal').hidden = !open;
  if (open) {
    $('#task-error').textContent = '';
    $('#task-title').focus();
  }
}

async function bootstrap() {
  try {
    await loadCsrf();
    const user = await request('/api/auth/me');
    showApp(user);
  } catch (_) {
    showLogin('Entre para acessar sua agenda.');
  }
}

$('#login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  $('#login-error').textContent = '';
  const form = new FormData(event.currentTarget);
  try {
    const user = await request('/api/auth/login', {
      method: 'POST',
      headers: csrfHeaders(),
      body: JSON.stringify({ email: form.get('email'), password: form.get('password') })
    });
    showApp(user);
  } catch (_) {
    $('#login-error').textContent = 'E-mail ou senha inválidos.';
  }
});

window.addEventListener('hashchange', () => navigateTo(normalizeView()));
$('#refresh-button').addEventListener('click', refreshCurrentView);
$('#today-button').addEventListener('click', () => { state.referenceDate = new Date(); loadDashboard(); });
$('#previous-week').addEventListener('click', () => { state.referenceDate.setDate(state.referenceDate.getDate() - 7); loadDashboard(); });
$('#next-week').addEventListener('click', () => { state.referenceDate.setDate(state.referenceDate.getDate() + 7); loadDashboard(); });
$('#new-task-button').addEventListener('click', () => toggleTaskModal(true));
$('#new-agenda-task-button').addEventListener('click', () => toggleTaskModal(true));
$('#close-modal').addEventListener('click', () => toggleTaskModal(false));
$('#cancel-modal').addEventListener('click', () => toggleTaskModal(false));
$('#task-modal').addEventListener('click', (event) => { if (event.target === $('#task-modal')) toggleTaskModal(false); });
document.addEventListener('keydown', (event) => { if (event.key === 'Escape' && !$('#task-modal').hidden) toggleTaskModal(false); });
$('#agenda-filters').addEventListener('submit', (event) => { event.preventDefault(); loadAgenda(); });
document.addEventListener('click', (event) => {
  const actionButton = event.target.closest('[data-task-action]');
  if (actionButton) changeTaskStatus(actionButton);
});

$('#task-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const payload = {
    title: form.get('title'),
    description: form.get('description') || null,
    category: form.get('category'),
    priority: form.get('priority'),
    scheduledDate: form.get('scheduledDate') || null,
    scheduledTime: form.get('scheduledTime') || null,
    dueDate: null,
    dueTime: null,
    reminderDate: null,
    responsibleUserId: state.user.id
  };
  $('#task-error').textContent = '';
  $('#save-task').disabled = true;
  try {
    await request('/api/tasks', { method: 'POST', headers: csrfHeaders(), body: JSON.stringify(payload) });
    toggleTaskModal(false);
    event.currentTarget.reset();
    await refreshCurrentView();
  } catch (error) {
    $('#task-error').textContent = error.message || 'Não foi possível salvar a atividade.';
  } finally {
    $('#save-task').disabled = false;
  }
});

$('#logout-button').addEventListener('click', async () => {
  try { await request('/api/auth/logout', { method: 'POST', headers: csrfHeaders() }); }
  finally { showLogin('Sessão encerrada.'); }
});

bootstrap();
