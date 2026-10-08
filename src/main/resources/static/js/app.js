const menuButton = document.querySelector('#menuButton');
const sidebar = document.querySelector('#sidebar');
const notificationButton = document.querySelector('#notificationButton');
const notificationPanel = document.querySelector('#notificationPanel');
const notificationClose = document.querySelector('#notificationClose');
const markAllRead = document.querySelector('#markAllRead');
const notificationBadge = document.querySelector('#notificationBadge');
const globalSearch = document.querySelector('#globalSearch');
const searchEmpty = document.querySelector('#searchEmpty');

menuButton?.addEventListener('click', () => {
  const open = sidebar.classList.toggle('open');
  menuButton.setAttribute('aria-expanded', String(open));
});

const pageSurfaces = [...document.querySelectorAll('[data-page]')];
const pageLinks = [...document.querySelectorAll('[data-page-link]')];
const hashToPage = new Map([
  ['campus', 'campus'], ['today', 'today'], ['schedule', 'today'],
  ['circles-page', 'circles'], ['messages-page', 'messages'], ['events-page', 'events'],
  ['services-page', 'services'], ['opportunities-page', 'opportunities'], ['map-page', 'map'],
  ['wellbeing-page', 'wellbeing'], ['lost-found-page', 'lost-found'], ['assistant-page', 'assistant']
]);

function showPage(pageName, { updateHistory = true, focus = true } = {}) {
  const requestedPage = pageSurfaces.find((page) => page.dataset.page === pageName);
  const nextPage = requestedPage ?? pageSurfaces.find((page) => page.dataset.page === 'campus');
  pageSurfaces.forEach((page) => { page.hidden = page !== nextPage; });
  pageLinks.forEach((link) => {
    const active = link.dataset.pageLink === nextPage.dataset.page;
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  sidebar.classList.remove('open');
  menuButton?.setAttribute('aria-expanded', 'false');
  notificationPanel.hidden = true;
  notificationButton?.setAttribute('aria-expanded', 'false');
  if (updateHistory) {
    const targetLink = pageLinks.find((link) => link.dataset.pageLink === nextPage.dataset.page);
    history.pushState({ page: nextPage.dataset.page }, '', targetLink?.hash || '#campus');
  }
  window.scrollTo({ top: 0, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
  if (focus && nextPage !== document.querySelector('#campus')) nextPage.focus({ preventScroll: true });
}

pageLinks.forEach((item) => {
  item.addEventListener('click', (event) => {
    event.preventDefault();
    showPage(item.dataset.pageLink);
  });
});

document.querySelectorAll('[data-open-page]').forEach((item) => {
  item.addEventListener('click', (event) => {
    event.preventDefault();
    showPage(item.dataset.openPage);
  });
});

window.addEventListener('popstate', () => {
  const pageName = hashToPage.get(location.hash.slice(1)) ?? 'campus';
  showPage(pageName, { updateHistory: false, focus: false });
});

notificationButton?.addEventListener('click', () => {
  const willOpen = notificationPanel.hidden;
  notificationPanel.hidden = !willOpen;
  notificationButton.setAttribute('aria-expanded', String(willOpen));
});

document.addEventListener('pointerdown', (event) => {
  if (!notificationPanel?.hidden && !notificationPanel.contains(event.target) && !notificationButton.contains(event.target)) {
    notificationPanel.hidden = true;
    notificationButton.setAttribute('aria-expanded', 'false');
  }
});

notificationClose?.addEventListener('click', () => {
  notificationPanel.hidden = true;
  notificationButton.setAttribute('aria-expanded', 'false');
  notificationButton.focus();
});

markAllRead?.addEventListener('click', () => {
  document.querySelectorAll('.unread-notification').forEach((item) => item.classList.remove('unread-notification'));
  document.querySelectorAll('.notification-unread').forEach((dot) => dot.remove());
  notificationBadge.hidden = true;
  notificationButton.setAttribute('aria-label', 'Notifications, none unread');
  document.querySelector('.notification-toolbar span').textContent = 'You are up to date';
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && !notificationPanel.hidden) {
    notificationClose.click();
  }
});

globalSearch?.addEventListener('input', () => {
  const query = globalSearch.value.trim().toLowerCase();
  let visible = 0;
  document.querySelectorAll('.searchable').forEach((item) => {
    const matches = !query || item.dataset.search?.includes(query) || item.textContent.toLowerCase().includes(query);
    item.classList.toggle('search-hidden', !matches);
    if (matches) visible += 1;
  });
  searchEmpty.hidden = visible !== 0;
});

document.querySelectorAll('.join-button').forEach((button) => {
  button.addEventListener('click', () => {
    const joined = button.classList.toggle('joined');
    button.textContent = joined ? 'Joined' : button.closest('.feature-card') ? 'Join circle' : 'Join';
    button.setAttribute('aria-pressed', String(joined));
    const card = button.closest('[data-category]');
    if (card) {
      const categories = new Set((card.dataset.category || '').split(/\s+/).filter(Boolean));
      if (joined) categories.add('joined'); else categories.delete('joined');
      card.dataset.category = [...categories].join(' ');
    }
  });
});

document.querySelectorAll('.filter-row').forEach((row) => {
  row.querySelectorAll('.filter-chip').forEach((button) => {
    button.addEventListener('click', () => {
      row.querySelectorAll('.filter-chip').forEach((other) => other.classList.remove('active'));
      button.classList.add('active');
      if (!row.dataset.filterTarget) return;
      const filter = button.dataset.filter || 'all';
      document.querySelectorAll(row.dataset.filterTarget).forEach((item) => {
        const categories = (item.dataset.category || '').split(/\s+/);
        item.classList.toggle('filter-hidden', filter !== 'all' && !categories.includes(filter));
      });
    });
  });
});

document.querySelectorAll('.page-search').forEach((input) => {
  input.addEventListener('input', () => {
    const query = input.value.trim().toLowerCase();
    document.querySelectorAll(input.dataset.searchItems).forEach((item) => {
      item.classList.toggle('page-search-hidden', Boolean(query) && !item.textContent.toLowerCase().includes(query));
    });
  });
});

document.querySelectorAll('.save-toggle').forEach((button) => {
  button.addEventListener('click', () => {
    const saved = button.getAttribute('aria-pressed') !== 'true';
    button.setAttribute('aria-pressed', String(saved));
    button.textContent = saved ? 'Saved locally' : button.closest('.event-card-large') ? 'Save event' : 'Save';
  });
});

let prototypeToastTimer;
function showPrototypeToast(message) {
  let toast = document.querySelector('.prototype-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.className = 'prototype-toast';
    toast.setAttribute('role', 'status');
    toast.setAttribute('aria-live', 'polite');
    document.body.append(toast);
  }
  toast.textContent = `${message}. No live university action was completed.`;
  toast.hidden = false;
  clearTimeout(prototypeToastTimer);
  prototypeToastTimer = setTimeout(() => { toast.hidden = true; }, 4500);
}

document.querySelectorAll('.prototype-action').forEach((button) => {
  button.addEventListener('click', () => showPrototypeToast(button.dataset.action));
});

const bookingTabs = [...document.querySelectorAll('.tabs .tab')];
const bookingPanel = document.querySelector('#bookingPanel');
const bookingName = document.querySelector('#bookingName');
const bookingDetail = document.querySelector('#bookingDetail');
const bookingAvatar = document.querySelector('#bookingAvatar');
const bookButton = document.querySelector('#bookButton');

function selectBookingTab(tab) {
  bookingTabs.forEach((other) => {
    other.classList.remove('active');
    other.setAttribute('aria-selected', 'false');
    other.setAttribute('tabindex', '-1');
  });
  tab.classList.add('active');
  tab.setAttribute('aria-selected', 'true');
  tab.setAttribute('tabindex', '0');
  bookingPanel.setAttribute('aria-labelledby', tab.id);
  const facilityMode = tab.dataset.mode === 'facility';
  bookingAvatar.textContent = facilityMode ? 'SC' : 'DT';
  bookingName.textContent = facilityMode ? 'Sports Centre' : 'Dr Taylor';
  bookingDetail.textContent = facilityMode ? 'Facilities booking' : 'Computer Science';
  bookButton.textContent = facilityMode ? 'Book facility' : 'Book appointment';
  document.querySelector('#bookingStatus').textContent = '';
}

bookingTabs.forEach((tab, index) => {
  tab.addEventListener('click', () => {
    selectBookingTab(tab);
  });
  tab.addEventListener('keydown', (event) => {
    if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
    event.preventDefault();
    let nextIndex = index;
    if (event.key === 'ArrowLeft') nextIndex = (index - 1 + bookingTabs.length) % bookingTabs.length;
    if (event.key === 'ArrowRight') nextIndex = (index + 1) % bookingTabs.length;
    if (event.key === 'Home') nextIndex = 0;
    if (event.key === 'End') nextIndex = bookingTabs.length - 1;
    selectBookingTab(bookingTabs[nextIndex]);
    bookingTabs[nextIndex].focus();
  });
});

document.querySelectorAll('.date-strip button').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('.date-strip button').forEach((other) => other.classList.remove('selected'));
    document.querySelectorAll('.date-strip button').forEach((other) => other.setAttribute('aria-pressed', 'false'));
    button.classList.add('selected');
    button.setAttribute('aria-pressed', 'true');
  });
});

document.querySelectorAll('.time-strip button').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('.time-strip button').forEach((other) => other.classList.remove('selected'));
    document.querySelectorAll('.time-strip button').forEach((other) => other.setAttribute('aria-pressed', 'false'));
    button.classList.add('selected');
    button.setAttribute('aria-pressed', 'true');
  });
});

bookButton?.addEventListener('click', () => {
  const mode = document.querySelector('.tabs .tab[aria-selected="true"]').dataset.mode;
  const date = document.querySelector('.date-strip .selected').textContent.replace(/\s+/g, ' ').trim();
  const time = document.querySelector('.time-strip .selected').textContent.trim();
  const selection = mode === 'facility' ? 'facility request' : 'appointment request';
  document.querySelector('#bookingStatus').textContent = `Prototype ${selection} saved locally for ${date} at ${time}. No live booking was created.`;
});

const assistantAnswers = new Map([
  ['find a quiet study space', 'The prototype would show live room availability from an approved university service. In this fixture, try Library Study Room 3.'],
  ['how do i book a sports facility?', 'Open Book a Service, choose Room / Facility, then select the Sports Centre and an available time.'],
  ['tell me about career opportunities', 'The Opportunities area brings together approved jobs, internships, volunteering and careers events.']
]);

function answerQuestion(question) {
  const normalised = question.trim().toLowerCase();
  const exact = assistantAnswers.get(normalised);
  const response = exact ?? 'This Sprint 1 guide only answers the three demonstrated campus questions. A later approved retrieval service will provide sourced university information.';
  document.querySelector('#assistantResponse').textContent = response;
}

document.querySelectorAll('.prompt-list button').forEach((button) => {
  button.addEventListener('click', () => answerQuestion(button.textContent));
});

document.querySelector('#assistantForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const input = document.querySelector('#assistantInput');
  if (!input.value.trim()) return;
  answerQuestion(input.value);
});

function answerPageQuestion(question) {
  const normalised = question.trim().toLowerCase();
  const response = assistantAnswers.get(normalised) ?? 'This Sprint 1 campus guide only answers the three demonstrated questions. A later approved retrieval service will provide sourced university information.';
  const responsePanel = document.querySelector('#pageAssistantResponse');
  responsePanel.replaceChildren();
  const bubble = document.createElement('p');
  bubble.className = 'bubble received';
  bubble.textContent = response;
  responsePanel.append(bubble);
}

document.querySelectorAll('[data-page-question]').forEach((button) => {
  button.addEventListener('click', () => answerPageQuestion(button.dataset.pageQuestion));
});

document.querySelector('#pageAssistantForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const input = document.querySelector('#pageAssistantInput');
  if (!input.value.trim()) return;
  answerPageQuestion(input.value);
});

const messageDialog = document.querySelector('#messageDialog');
document.querySelectorAll('.message-button').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelector('#dialogRecipient').textContent = `To: ${button.dataset.person}`;
    messageDialog.showModal();
  });
});

fetch('/api/platform/status')
  .then((response) => {
    if (!response.ok) throw new Error('Status endpoint unavailable');
    return response.json();
  })
  .then((status) => document.documentElement.dataset.platformStage = status.stage)
  .catch(() => document.documentElement.dataset.platformStage = 'offline');

const initialPage = hashToPage.get(location.hash.slice(1)) ?? 'campus';
showPage(initialPage, { updateHistory: false, focus: false });
