'use strict';

/* ================================================================
   Domínio: rótulos e regras de exibição
   ================================================================ */

const VIEWS = {
  dashboard: 'Visão geral',
  agenda: 'Agenda',
  processos: 'Processos',
  equipe: 'Equipe'
};

const CATEGORY_LABELS = {
  DEADLINE: 'Prazo',
  HEARING: 'Audiência',
  PENDING_DOCUMENT: 'Documentação pendente',
  CLIENT_MEETING: 'Reunião com cliente',
  INTERNAL_MEETING: 'Reunião interna',
  CASE_PENDING_ITEM: 'Resposta em processo',
  URGENT_PROTOCOL: 'Protocolo urgente',
  DOCUMENT_COLLECTION: 'Levantamento de documentos',
  OTHER: 'Outra atividade'
};

const STATUS = {
  PENDING: { label: 'Pendente', tone: 'info' },
  IN_PROGRESS: { label: 'Em andamento', tone: 'gold' },
  COMPLETED: { label: 'Concluída', tone: 'success' },
  CANCELED: { label: 'Cancelada', tone: 'neutral' }
};

const PRIORITY = {
  HIGH: { label: 'Prioridade alta', tone: 'gold', rowClass: 'is-high' },
  URGENT: { label: 'Urgente', tone: 'danger', rowClass: 'is-urgent' }
};

const ROLE_LABELS = { ADMIN: 'Administrador', USER: 'Advogado(a)' };
const COLLAPSED_KEY = 'agenda.sidebar-collapsed';
const MS_PER_DAY = 86_400_000;

/* ================================================================
   Utilitários
   ================================================================ */

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const escapeHtml = (value = '') => String(value).replace(/[&<>"']/g, (char) => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;'
}[char]));

const icon = (name) => `<svg class="icon" aria-hidden="true"><use href="#i-${name}"/></svg>`;

const toIsoDate = (date) => [
  date.getFullYear(),
  String(date.getMonth() + 1).padStart(2, '0'),
  String(date.getDate()).padStart(2, '0')
].join('-');

const parseDate = (isoDate) => new Date(`${isoDate}T12:00:00`);

const formatDate = (isoDate, options = { day: '2-digit', month: 'short', year: 'numeric' }) =>
  isoDate ? new Intl.DateTimeFormat('pt-BR', options).format(parseDate(isoDate)) : '';

const daysFromToday = (isoDate) => {
  const today = parseDate(toIsoDate(new Date()));
  return Math.round((parseDate(isoDate) - today) / MS_PER_DAY);
};

const initials = (name = '') => name.split(' ').filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'VA';
const firstName = (name = '') => name.split(' ')[0] || name;
const plural = (count, singular, pluralForm) => `${count} ${count === 1 ? singular : pluralForm}`;

function greeting(date = new Date()) {
  const hour = date.getHours();
  if (hour < 12) return 'Bom dia';
  if (hour < 18) return 'Boa tarde';
  return 'Boa noite';
}

/* ================================================================
   API
   ================================================================ */

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

const api = {
  csrf: null,

  async request(url, { method = 'GET', body } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (method !== 'GET' && this.csrf) headers[this.csrf.headerName] = this.csrf.token;

    const response = await fetch(url, {
      method,
      headers,
      credentials: 'same-origin',
      body: body === undefined ? undefined : JSON.stringify(body)
    });

    if (!response.ok) throw new ApiError(await this.errorMessage(response), response.status);
    return response.status === 204 ? null : response.json();
  },

  async errorMessage(response) {
    if (response.status === 401) return 'Sua sessão expirou. Entre novamente.';
    if (response.status === 403) return 'Você não tem permissão para esta ação.';
    try {
      const problem = await response.json();
      return problem.detail || 'Não foi possível concluir a operação.';
    } catch {
      return 'Não foi possível concluir a operação.';
    }
  },

  async refreshCsrf() { this.csrf = await this.request('/api/auth/csrf'); },

  me: () => api.request('/api/auth/me'),
  login: (credentials) => api.request('/api/auth/login', { method: 'POST', body: credentials }),
  logout: () => api.request('/api/auth/logout', { method: 'POST' }),
  dashboard: (referenceDate) => api.request(`/api/dashboard?referenceDate=${toIsoDate(referenceDate)}`),
  tasks: (params) => api.request(`/api/tasks?${new URLSearchParams({ size: '100', ...params })}`),
  createTask: (payload) => api.request('/api/tasks', { method: 'POST', body: payload }),
  updateTask: (id, payload) => api.request(`/api/tasks/${id}`, { method: 'PUT', body: payload }),
  startTask: (id) => api.request(`/api/tasks/${id}/status`, { method: 'PATCH', body: { status: 'IN_PROGRESS' } }),
  completeTask: (id) => api.request(`/api/tasks/${id}/complete`, { method: 'POST' }),
  reopenTask: (id) => api.request(`/api/tasks/${id}/reopen`, { method: 'POST' }),
  cancelTask: (id) => api.request(`/api/tasks/${id}/cancel`, { method: 'POST' }),
  deleteTask: (id) => api.request(`/api/tasks/${id}`, { method: 'DELETE' }),
  users: () => api.request('/api/users?size=100&sort=name')
};

/* ================================================================
   Estado
   ================================================================ */

const state = {
  user: null,
  view: 'dashboard',
  dashboardDate: new Date(),
  agendaDate: new Date(),
  tasksById: new Map(),
  team: null,
  editingTaskId: null
};

const isAdmin = () => state.user?.role === 'ADMIN';

/* ================================================================
   Componentes de interface
   ================================================================ */

const ui = {
  skeleton: (rows = 3) => `<div class="skeleton" aria-busy="true" aria-label="Carregando">${'<span></span>'.repeat(rows)}</div>`,

  empty: (title, text, { success = false } = {}) => `
    <div class="state ${success ? 'state--success' : ''}">
      <span class="state-icon">${icon(success ? 'check' : 'inbox')}</span>
      <div><h3>${title}</h3><p>${text}</p></div>
    </div>`,

  error(target, message, retry) {
    target.innerHTML = `<div class="alert" role="alert">${icon('alert')}<span>${escapeHtml(message)}</span>
      ${retry ? '<button class="button button--secondary button--sm" type="button">Tentar novamente</button>' : ''}</div>`;
    if (retry) $('button', target).addEventListener('click', retry, { once: true });
  },

  pill: (label, tone = 'neutral', withDot = false) =>
    `<span class="pill pill--${tone}">${withDot ? '<i class="status-dot"></i>' : ''}${escapeHtml(label)}</span>`,

  toast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `${icon('check')}<span>${escapeHtml(message)}</span>`;
    $('#toasts').append(toast);
    setTimeout(() => {
      toast.classList.add('is-leaving');
      toast.addEventListener('animationend', () => toast.remove(), { once: true });
    }, 2800);
  },

  confirm(title, text) {
    const dialog = $('#confirm-dialog');
    $('#confirm-title').textContent = title;
    $('#confirm-text').textContent = text;
    dialog.returnValue = '';
    dialog.showModal();
    return new Promise((resolve) => {
      dialog.addEventListener('close', () => resolve(dialog.returnValue === 'confirm'), { once: true });
    });
  }
};

/* ---------- Atividades ---------- */

function referenceDateOf(task) { return task.dueDate || task.scheduledDate; }

function byDateAndTime(a, b) {
  const key = (task) => `${referenceDateOf(task) || '9999-12-31'} ${task.scheduledTime || task.dueTime || '99'}`;
  return key(a).localeCompare(key(b));
}

function timeOf(task) {
  if (task.scheduledTime) return task.scheduledTime.slice(0, 5);
  if (task.dueTime) return `até ${task.dueTime.slice(0, 5)}`;
  return null;
}

function deadlinePill(task) {
  switch (task.deadlineAlertStatus) {
    case 'OVERDUE': {
      const days = Math.abs(daysFromToday(task.dueDate));
      return ui.pill(days === 0 ? 'Vencido' : `Vencido há ${plural(days, 'dia', 'dias')}`, 'danger');
    }
    case 'DUE_TODAY': return ui.pill('Vence hoje', 'warning');
    case 'UPCOMING': {
      const days = daysFromToday(task.dueDate);
      return ui.pill(days === 1 ? 'Vence amanhã' : `Vence em ${days} dias`, 'info');
    }
    case 'REMINDER_ACTIVE': return ui.pill('Lembrete', 'gold');
    default: return '';
  }
}

function dateBlock(task) {
  const date = referenceDateOf(task);
  if (!date) return `<span class="task-date"><small>sem</small><strong>—</strong></span>`;
  const tone = task.deadlineAlertStatus === 'OVERDUE' ? 'task-date--danger'
    : task.deadlineAlertStatus === 'DUE_TODAY' ? 'task-date--warning' : '';
  const month = formatDate(date, { month: 'short' }).replace('.', '');
  return `<span class="task-date ${tone}"><strong>${formatDate(date, { day: '2-digit' })}</strong><small>${month}</small></span>`;
}

function taskMeta(task) {
  const time = timeOf(task);
  return `<div class="task-meta">
    <span>${escapeHtml(CATEGORY_LABELS[task.category] || 'Atividade')}</span>
    ${time ? `<span>${icon('clock')}${escapeHtml(time)}</span>` : ''}
    <span>${icon('users')}${escapeHtml(task.responsibleUser?.name || 'Sem responsável')}</span>
  </div>`;
}

function taskActions(task) {
  const button = (action, label, iconName, tone = '') =>
    `<button class="action ${tone}" type="button" data-task-id="${task.id}" data-task-action="${action}">${icon(iconName)}${label}</button>`;

  if (task.status === 'CANCELED') {
    return button('reopen', 'Reabrir', 'reopen') + button('remove', 'Remover', 'trash', 'action--danger');
  }
  if (task.status === 'COMPLETED') return button('reopen', 'Reabrir', 'reopen');

  return [
    button('edit', 'Editar', 'pencil'),
    task.status === 'PENDING' ? button('start', 'Iniciar', 'play') : '',
    button('complete', 'Concluir', 'check', 'action--success'),
    button('cancel', 'Cancelar', 'ban', 'action--danger')
  ].join('');
}

function taskRow(task, { withActions = false } = {}) {
  state.tasksById.set(task.id, task);
  const priority = PRIORITY[task.priority];
  const status = STATUS[task.status];
  return `<article class="task-row ${priority?.rowClass || ''}">
    ${dateBlock(task)}
    <div class="task-main">
      <h3 class="task-title">${escapeHtml(task.title)}</h3>
      ${taskMeta(task)}
    </div>
    <div class="task-badges">
      ${deadlinePill(task)}
      ${priority ? ui.pill(priority.label, priority.tone) : ''}
      ${withActions && status ? ui.pill(status.label, status.tone) : ''}
    </div>
    ${withActions ? `<div class="task-actions">${taskActions(task)}</div>` : ''}
  </article>`;
}

/* ================================================================
   Shell: sessão, navegação e menu lateral
   ================================================================ */

const shell = $('#app-view');

function showApp(user) {
  state.user = user;
  state.team = null;
  $('#login-view').hidden = true;
  shell.hidden = false;
  $('#user-name').textContent = user.name;
  $('#user-role').textContent = ROLE_LABELS[user.role] || 'Equipe jurídica';
  $('#user-avatar').textContent = initials(user.name);
  $('#greeting-name').textContent = firstName(user.name);
  navigate(viewFromHash());
}

function showLogin(message = '') {
  state.user = null;
  shell.hidden = true;
  $('#login-view').hidden = false;
  $('#login-error').textContent = message;
  $('#email').focus();
}

function viewFromHash() {
  const view = window.location.hash.slice(1);
  return view in VIEWS ? view : 'dashboard';
}

function navigate(view) {
  state.view = view;
  $('#page-name').textContent = VIEWS[view];
  document.title = `${VIEWS[view]} | Agenda Jurídica`;

  $$('[data-view-panel]').forEach((panel) => { panel.hidden = panel.dataset.viewPanel !== view; });
  $$('[data-view]').forEach((link) => {
    const active = link.dataset.view === view;
    link.classList.toggle('is-active', active);
    if (active) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });

  closeMobileMenu();
  if (state.user) loadCurrentView();
}

async function loadCurrentView() {
  const refresh = $('#refresh-button');
  refresh.disabled = true;
  try {
    const loaders = { dashboard: loadDashboard, agenda: loadAgenda, processos: loadProcesses, equipe: loadTeam };
    await loaders[state.view]();
  } finally {
    refresh.disabled = false;
  }
}

function handleError(error, target, retry) {
  if (error.status === 401) return showLogin(error.message);
  ui.error(target, error.message, retry);
}

const desktop = window.matchMedia('(min-width: 64rem)');

function setCollapsed(collapsed) {
  shell.classList.toggle('is-collapsed', collapsed);
  $('#collapse-button').setAttribute('aria-label', collapsed ? 'Expandir menu lateral' : 'Recolher menu lateral');
  try { localStorage.setItem(COLLAPSED_KEY, String(collapsed)); } catch { /* armazenamento indisponível */ }
}

function openMobileMenu() {
  shell.classList.add('is-mobile-open');
  $('#sidebar-backdrop').hidden = false;
  $('#menu-button').setAttribute('aria-expanded', 'true');
  $('.nav-link.is-active')?.focus();
}

function closeMobileMenu() {
  if (!shell.classList.contains('is-mobile-open')) return;
  shell.classList.remove('is-mobile-open');
  $('#sidebar-backdrop').hidden = true;
  $('#menu-button').setAttribute('aria-expanded', 'false');
}

/* ================================================================
   Visão geral
   ================================================================ */

async function loadDashboard() {
  const errorTarget = $('#dashboard-error');
  errorTarget.innerHTML = '';
  $('#greeting').textContent = greeting();
  $('#today-label').textContent = formatDate(toIsoDate(new Date()), { weekday: 'long', day: '2-digit', month: 'long' });
  $('#attention-list').innerHTML = ui.skeleton(2);
  $('#upcoming-list').innerHTML = ui.skeleton(2);

  try {
    const data = await api.dashboard(state.dashboardDate);
    renderStats(data);
    renderAttention(data);
    renderUpcoming(data);
    renderWeekOverview(data);
  } catch (error) {
    $('#attention-list').innerHTML = '';
    $('#upcoming-list').innerHTML = '';
    handleError(error, errorTarget, loadDashboard);
  }
}

function renderStats(data) {
  $('#pending-total').textContent = data.totalPending;
  $('#progress-total').textContent = data.totalInProgress;
  $('#overdue-total').textContent = data.totalOverdue;
  $('#completed-total').textContent = data.totalCompleted;
  $('#overdue-total').closest('.stat').classList.toggle('has-value', data.totalOverdue > 0);
}

function renderAttention(data) {
  const tasks = [...data.overdueTasks, ...data.dueTodayTasks, ...data.reminderActiveTasks];
  const count = $('#attention-count');
  count.hidden = tasks.length === 0;
  count.textContent = plural(tasks.length, 'item', 'itens');
  $('#attention-list').innerHTML = tasks.length
    ? tasks.map((task) => taskRow(task)).join('')
    : ui.empty('Tudo em dia', 'Nenhum prazo vencido ou vencendo hoje.', { success: true });
}

function renderUpcoming(data) {
  $('#upcoming-list').innerHTML = data.upcomingTasks.length
    ? data.upcomingTasks.map((task) => taskRow(task)).join('')
    : ui.empty('Sem vencimentos próximos', 'Os prazos dos próximos dias aparecem aqui.');
}

function renderWeekOverview(data) {
  const days = data.weeklyTasks;
  const tasks = days.flatMap((day) => day.tasks);
  const today = toIsoDate(new Date());
  const busiestDay = Math.max(1, ...days.map((day) => day.tasks.length));

  $('#week-label').textContent = `${formatDate(data.weekStart, { day: '2-digit', month: 'short' })} a ${formatDate(data.weekEnd)} · ${plural(tasks.length, 'compromisso', 'compromissos')}`;

  const bars = $('#week-bars');
  bars.style.setProperty('--days', days.length);
  bars.innerHTML = days.map((day, index) => `
    <div class="week-bar ${day.date === today ? 'is-today' : ''}">
      <span class="week-bar-count">${day.tasks.length}</span>
      <span class="week-bar-track"><span class="week-bar-fill" style="--value:${(day.tasks.length / busiestDay) * 100}; animation-delay:${index * 60}ms"></span></span>
      <span class="week-bar-day">${formatDate(day.date, { weekday: 'short' }).replace('.', '')}</span>
    </div>`).join('');

  const counts = tasks.reduce((total, task) => total.set(task.category, (total.get(task.category) || 0) + 1), new Map());
  const byCategory = [...counts].sort(([, a], [, b]) => b - a);
  const largest = byCategory[0]?.[1] || 1;

  $('#category-bars').innerHTML = byCategory.length
    ? byCategory.map(([category, count], index) => `
      <div class="category-bar">
        <div class="category-bar-head"><span>${CATEGORY_LABELS[category]}</span><strong>${count}</strong></div>
        <div class="category-bar-track"><div class="category-bar-fill" style="--value:${(count / largest) * 100}; animation-delay:${index * 60}ms"></div></div>
      </div>`).join('')
    : ui.empty('Semana livre', 'Nenhum compromisso agendado nesta semana.');
}

/* ================================================================
   Agenda
   ================================================================ */

async function loadAgenda() {
  await Promise.all([loadAgendaWeek(), loadAgendaList()]);
}

async function loadAgendaWeek() {
  const board = $('#agenda-week');
  board.innerHTML = ui.skeleton(1);
  try {
    const data = await api.dashboard(state.agendaDate);
    renderAgendaWeek(data);
  } catch (error) {
    board.innerHTML = '';
    handleError(error, board, loadAgendaWeek);
  }
}

function renderAgendaWeek(data) {
  const today = toIsoDate(new Date());
  const board = $('#agenda-week');
  const total = data.weeklyTasks.reduce((sum, day) => sum + day.tasks.length, 0);

  $('#agenda-week-label').textContent = `${formatDate(data.weekStart, { day: '2-digit', month: 'long' })} a ${formatDate(data.weekEnd, { day: '2-digit', month: 'long', year: 'numeric' })} · ${plural(total, 'compromisso', 'compromissos')}`;
  board.style.setProperty('--days', data.weeklyTasks.length);
  board.innerHTML = data.weeklyTasks.map((day) => {
    const tasks = [...day.tasks].sort((a, b) => (timeOf(a) || '99').localeCompare(timeOf(b) || '99'));
    return `<section class="day ${day.date === today ? 'is-today' : ''}" aria-label="${formatDate(day.date, { weekday: 'long', day: '2-digit', month: 'long' })}">
      <header class="day-head">
        <span>${formatDate(day.date, { weekday: 'long' }).replace('-feira', '')}</span>
        <strong>${formatDate(day.date, { day: '2-digit' })}</strong>
      </header>
      ${tasks.length ? tasks.map((task, index) => appointment(task, index)).join('') : '<p class="day-empty">Livre</p>'}
    </section>`;
  }).join('');
}

function appointment(task, index) {
  const done = task.status === 'COMPLETED' || task.status === 'CANCELED';
  const time = timeOf(task);
  return `<article class="appointment ${PRIORITY[task.priority]?.rowClass || ''} ${done ? 'is-done' : ''}" style="animation-delay:${index * 40}ms">
    ${time ? `<time>${escapeHtml(time)}</time>` : ''}
    <strong>${escapeHtml(task.title)}</strong>
    <small>${escapeHtml(CATEGORY_LABELS[task.category] || 'Atividade')}</small>
  </article>`;
}

async function loadAgendaList() {
  const list = $('#agenda-list');
  const errorTarget = $('#agenda-error');
  errorTarget.innerHTML = '';
  list.innerHTML = ui.skeleton(4);

  const filters = Object.fromEntries(
    Object.entries({
      search: $('#agenda-search').value.trim(),
      status: $('#agenda-status').value,
      category: $('#agenda-category').value
    }).filter(([, value]) => value)
  );

  try {
    const page = await api.tasks(filters);
    $('#agenda-count').textContent = plural(page.totalElements, 'atividade encontrada', 'atividades encontradas');
    list.innerHTML = page.content.length
      ? page.content.toSorted(byDateAndTime).map((task) => taskRow(task, { withActions: true })).join('')
      : ui.empty('Nenhuma atividade encontrada', 'Ajuste os filtros ou cadastre uma nova atividade.');
  } catch (error) {
    list.innerHTML = '';
    handleError(error, errorTarget, loadAgendaList);
  }
}

/* ================================================================
   Processos
   ================================================================ */

async function loadProcesses() {
  const list = $('#processos-list');
  const errorTarget = $('#processos-error');
  errorTarget.innerHTML = '';
  list.innerHTML = ui.skeleton(3);
  try {
    const page = await api.tasks({ category: 'CASE_PENDING_ITEM' });
    list.innerHTML = page.content.length
      ? page.content.toSorted(byDateAndTime).map((task) => taskRow(task, { withActions: true })).join('')
      : ui.empty('Nenhuma pendência processual', 'As respostas em processo cadastradas aparecem aqui.');
  } catch (error) {
    list.innerHTML = '';
    handleError(error, errorTarget, loadProcesses);
  }
}

/* ================================================================
   Equipe
   ================================================================ */

async function teamMembers() {
  if (!isAdmin()) return [{ ...state.user, active: true }];
  state.team ??= (await api.users()).content;
  return state.team;
}

async function loadTeam() {
  const list = $('#team-list');
  const errorTarget = $('#team-error');
  errorTarget.innerHTML = '';
  list.innerHTML = ui.skeleton(2);
  try {
    const members = await teamMembers();
    list.innerHTML = members.map((member, index) => `
      <article class="member" style="animation-delay:${index * 40}ms">
        <span class="avatar" aria-hidden="true">${initials(member.name)}</span>
        <div class="member-info">
          <strong>${escapeHtml(member.name)}</strong>
          <small title="${escapeHtml(member.email)}">${escapeHtml(member.email)}</small>
          <div class="member-tags">
            ${ui.pill(ROLE_LABELS[member.role] || member.role, member.role === 'ADMIN' ? 'gold' : 'info')}
            ${member.active === false ? ui.pill('Inativo', 'neutral', true) : ui.pill('Ativo', 'success', true)}
          </div>
        </div>
      </article>`).join('');
    if (!isAdmin()) {
      list.insertAdjacentHTML('beforeend', ui.empty('Visualização individual', 'O diretório completo da equipe é restrito a administradores.'));
    }
  } catch (error) {
    list.innerHTML = '';
    handleError(error, errorTarget, loadTeam);
  }
}

/* ================================================================
   Ações nas atividades
   ================================================================ */

const TASK_ACTIONS = {
  start: { run: api.startTask, done: 'Atividade iniciada.' },
  complete: { run: api.completeTask, done: 'Atividade concluída.' },
  reopen: { run: api.reopenTask, done: 'Atividade reaberta.' },
  cancel: { run: api.cancelTask, done: 'Atividade cancelada.' },
  remove: { run: api.deleteTask, done: 'Atividade removida.' }
};

async function runTaskAction(button) {
  const task = state.tasksById.get(Number(button.dataset.taskId));
  const action = button.dataset.taskAction;
  if (!task) return;

  if (action === 'edit') return openTaskDialog(task);
  if (action === 'remove') {
    const confirmed = await ui.confirm('Remover atividade?', `"${task.title}" sairá da agenda. Esta ação não pode ser desfeita.`);
    if (!confirmed) return;
  }

  button.disabled = true;
  try {
    await TASK_ACTIONS[action].run(task.id);
    ui.toast(TASK_ACTIONS[action].done);
    await loadCurrentView();
  } catch (error) {
    button.disabled = false;
    const target = state.view === 'processos' ? $('#processos-error') : $('#agenda-error');
    handleError(error, target);
  }
}

/* ================================================================
   Formulário de atividade (criar e editar)
   ================================================================ */

const taskDialog = $('#task-dialog');
const taskForm = $('#task-form');

function fillCategoryOptions() {
  const options = Object.entries(CATEGORY_LABELS)
    .map(([value, label]) => `<option value="${value}">${label}</option>`).join('');
  $$('[data-category-options]').forEach((select) => select.insertAdjacentHTML('beforeend', options));
}

async function fillResponsibleOptions(selectedId) {
  const field = $('#responsible-field');
  field.hidden = !isAdmin();
  if (!isAdmin()) return;

  const select = $('#task-responsible');
  try {
    const members = (await teamMembers()).filter((member) => member.active || member.id === selectedId);
    select.innerHTML = members.map((member) => `<option value="${member.id}">${escapeHtml(member.name)}</option>`).join('');
    select.value = String(selectedId ?? state.user.id);
  } catch {
    field.hidden = true;
  }
}

async function openTaskDialog(task = null) {
  state.editingTaskId = task?.id ?? null;
  taskForm.reset();
  $('#task-error').textContent = '';
  $('#task-dialog-title').textContent = task ? 'Editar atividade' : 'Nova atividade';
  $('#save-task').textContent = task ? 'Salvar alterações' : 'Salvar atividade';

  if (task) {
    const fields = ['title', 'description', 'category', 'priority', 'scheduledDate', 'dueDate', 'reminderDate'];
    fields.forEach((name) => { taskForm.elements[name].value = task[name] ?? ''; });
    taskForm.elements.scheduledTime.value = task.scheduledTime?.slice(0, 5) ?? '';
    taskForm.elements.dueTime.value = task.dueTime?.slice(0, 5) ?? '';
  }

  await fillResponsibleOptions(task?.responsibleUser?.id);
  taskDialog.showModal();
  $('#task-title').focus();
}

function taskPayload() {
  const form = new FormData(taskForm);
  const optional = (name) => form.get(name) || null;
  return {
    title: form.get('title').trim(),
    description: optional('description'),
    category: form.get('category'),
    priority: form.get('priority'),
    scheduledDate: optional('scheduledDate'),
    scheduledTime: optional('scheduledTime'),
    dueDate: optional('dueDate'),
    dueTime: optional('dueTime'),
    reminderDate: optional('reminderDate'),
    responsibleUserId: Number(form.get('responsibleUserId')) || state.user.id
  };
}

async function submitTask(event) {
  event.preventDefault();
  const title = $('#task-title');
  title.setAttribute('aria-invalid', String(!title.value.trim()));
  if (!title.value.trim()) {
    $('#task-error').textContent = 'Informe o título da atividade.';
    return title.focus();
  }

  const save = $('#save-task');
  save.disabled = true;
  try {
    const payload = taskPayload();
    if (state.editingTaskId) await api.updateTask(state.editingTaskId, payload);
    else await api.createTask(payload);
    taskDialog.close();
    ui.toast(state.editingTaskId ? 'Atividade atualizada.' : 'Atividade criada.');
    await loadCurrentView();
  } catch (error) {
    if (error.status === 401) { taskDialog.close(); return showLogin(error.message); }
    $('#task-error').textContent = error.message;
  } finally {
    save.disabled = false;
  }
}

/* ================================================================
   Login
   ================================================================ */

async function submitLogin(event) {
  event.preventDefault();
  const loginForm = event.currentTarget;
  const form = new FormData(loginForm);
  const submit = $('#login-submit');
  $('#login-error').textContent = '';
  submit.disabled = true;
  try {
    const user = await api.login({ email: form.get('email'), password: form.get('password') });
    await api.refreshCsrf();
    loginForm.reset();
    showApp(user);
  } catch (error) {
    $('#login-error').textContent = error.status === 401 || error.status === 400
      ? 'E-mail ou senha inválidos.'
      : error.message;
  } finally {
    submit.disabled = false;
  }
}

async function logout() {
  try { await api.logout(); } catch { /* a sessão já pode ter expirado */ }
  await api.refreshCsrf().catch(() => {});
  showLogin('Sessão encerrada.');
}

/* ================================================================
   Eventos
   ================================================================ */

function shiftWeek(target, step) {
  const key = target === 'agenda' ? 'agendaDate' : 'dashboardDate';
  const date = step === 0 ? new Date() : new Date(state[key]);
  if (step !== 0) date.setDate(date.getDate() + step);
  state[key] = date;
  return target === 'agenda' ? loadAgendaWeek() : loadDashboard();
}

function bindEvents() {
  $('#login-form').addEventListener('submit', submitLogin);
  $('#logout-button').addEventListener('click', logout);
  $('#refresh-button').addEventListener('click', loadCurrentView);
  window.addEventListener('hashchange', () => navigate(viewFromHash()));

  $('#collapse-button').addEventListener('click', () => setCollapsed(!shell.classList.contains('is-collapsed')));
  $('#menu-button').addEventListener('click', () => {
    if (desktop.matches) setCollapsed(!shell.classList.contains('is-collapsed'));
    else openMobileMenu();
  });
  $('#sidebar-backdrop').addEventListener('click', closeMobileMenu);
  desktop.addEventListener('change', closeMobileMenu);
  document.addEventListener('keydown', (event) => { if (event.key === 'Escape') closeMobileMenu(); });

  $$('[data-week]').forEach((button) => button.addEventListener('click', () =>
    shiftWeek(button.dataset.week, Number(button.dataset.step))));

  $$('[data-filter-status]').forEach((link) => link.addEventListener('click', () => {
    $('#agenda-status').value = link.dataset.filterStatus;
    if (state.view === 'agenda') loadAgendaList();
  }));

  $('#agenda-filters').addEventListener('submit', (event) => { event.preventDefault(); loadAgendaList(); });

  $$('[data-open-task]').forEach((button) => button.addEventListener('click', () => openTaskDialog()));
  taskForm.addEventListener('submit', submitTask);
  $$('[data-close-dialog]').forEach((button) => button.addEventListener('click', () => taskDialog.close()));
  $$('dialog').forEach((dialog) => dialog.addEventListener('click', (event) => {
    if (event.target === dialog) dialog.close();
  }));

  document.addEventListener('click', (event) => {
    const button = event.target.closest('[data-task-action]');
    if (button) runTaskAction(button);
  });
}

async function bootstrap() {
  fillCategoryOptions();
  try { setCollapsed(localStorage.getItem(COLLAPSED_KEY) === 'true'); } catch { /* armazenamento indisponível */ }
  bindEvents();

  try {
    await api.refreshCsrf();
    showApp(await api.me());
  } catch {
    showLogin();
  }
}

bootstrap();
