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

document.querySelectorAll('.nav-item').forEach((item) => {
  item.addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach((link) => {
      link.classList.remove('active');
      link.removeAttribute('aria-current');
    });
    item.classList.add('active');
    item.setAttribute('aria-current', 'page');
    sidebar.classList.remove('open');
    menuButton?.setAttribute('aria-expanded', 'false');
  });
});

notificationButton?.addEventListener('click', () => {
  const willOpen = notificationPanel.hidden;
  notificationPanel.hidden = !willOpen;
  notificationButton.setAttribute('aria-expanded', String(willOpen));
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
    button.textContent = joined ? 'Joined' : 'Join';
    button.setAttribute('aria-pressed', String(joined));
  });
});

document.querySelectorAll('.tabs .tab').forEach((tab) => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tabs .tab').forEach((other) => {
      other.classList.remove('active');
      other.setAttribute('aria-selected', 'false');
    });
    tab.classList.add('active');
    tab.setAttribute('aria-selected', 'true');
  });
});

document.querySelectorAll('.date-strip button').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('.date-strip button').forEach((other) => other.classList.remove('selected'));
    button.classList.add('selected');
  });
});

document.querySelectorAll('.time-strip button').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('.time-strip button').forEach((other) => other.classList.remove('selected'));
    button.classList.add('selected');
  });
});

document.querySelector('#bookButton')?.addEventListener('click', () => {
  document.querySelector('#bookingStatus').textContent = 'Prototype selection saved locally. No live booking was created.';
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
