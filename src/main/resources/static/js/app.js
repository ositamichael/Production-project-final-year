const menuButton = document.querySelector('#menuButton');
const mobileMoreButton = document.querySelector('#mobileMoreButton');
const sidebar = document.querySelector('#sidebar');
const sidebarClose = document.querySelector('#sidebarClose');
const sidebarScrim = document.querySelector('#sidebarScrim');
const notificationButton = document.querySelector('#notificationButton');
const notificationPanel = document.querySelector('#notificationPanel');
const notificationClose = document.querySelector('#notificationClose');
const markAllRead = document.querySelector('#markAllRead');
const notificationBadge = document.querySelector('#notificationBadge');
const notificationList = document.querySelector('#notificationList');
const notificationSummary = document.querySelector('#notificationSummary');
const messageUnreadBadge = document.querySelector('#messageUnreadBadge');
const mobileMessageUnreadBadge = document.querySelector('#mobileMessageUnreadBadge');
const globalSearch = document.querySelector('#globalSearch');
const globalSearchResults = document.querySelector('#globalSearchResults');
const searchEmpty = document.querySelector('#searchEmpty');
let pilotMode = false;
let signedIn = false;
let locationRefreshTimer;
let campusRefreshTimer;
let meetingRoomRefreshTimer;
let lastDialogTrigger = null;
let lastNavigationTrigger = null;
let activeAnnouncementCategory = '';

function campusNow() {
  return new Date(new Date().toLocaleString('en-US', { timeZone: 'Europe/London' }));
}

function updateCurrentDateAndGreeting() {
  const now = campusNow();
  const hour = now.getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
  document.querySelectorAll('[data-time-greeting]').forEach((node) => { node.textContent = greeting; });
  const weekday = document.querySelector('#todayWeekday');
  const date = document.querySelector('#todayDate');
  if (weekday) weekday.textContent = new Intl.DateTimeFormat('en-GB', { weekday: 'long', timeZone: 'Europe/London' }).format(now).toUpperCase();
  if (date) date.textContent = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'long', timeZone: 'Europe/London' }).format(now);
}

function openModal(dialog) {
  lastDialogTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  if (dialog.matches('dialog')) {
    if (typeof dialog.showModal === 'function' && !dialog.open) dialog.showModal();
    else dialog.setAttribute('open', '');
  } else {
    dialog.hidden = false;
  }
  dialog.classList.add('dialog-fallback');
  document.body.classList.add('modal-open');
  requestAnimationFrame(() => dialog.querySelector('button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled])')?.focus());
}

function closeModal(dialog) {
  if (dialog.matches('dialog')) {
    if (typeof dialog.close === 'function') dialog.close();
    else dialog.removeAttribute('open');
  } else {
    dialog.hidden = true;
  }
  dialog.classList.remove('dialog-fallback');
  if (!document.querySelector('dialog[open], .modal-surface:not([hidden])')) document.body.classList.remove('modal-open');
  lastDialogTrigger?.focus?.({ preventScroll: true });
  lastDialogTrigger = null;
}

document.addEventListener('keydown', (event) => {
  if (event.key !== 'Tab') return;
  const dialog = document.querySelector('dialog[open], .modal-surface:not([hidden])');
  if (!dialog) return;
  const focusable = [...dialog.querySelectorAll('button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')]
    .filter((node) => !node.hidden && node.getClientRects().length);
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
});

function setNavigationOpen(open) {
  if (open) lastNavigationTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null;
  sidebar.classList.toggle('open', open);
  sidebarScrim.hidden = !open;
  document.body.classList.toggle('nav-open', open);
  menuButton?.setAttribute('aria-expanded', String(open));
  mobileMoreButton?.setAttribute('aria-expanded', String(open));
  if (open) sidebarClose?.focus({ preventScroll: true });
  else if (lastNavigationTrigger) {
    lastNavigationTrigger.focus({ preventScroll: true });
    lastNavigationTrigger = null;
  }
}

menuButton?.addEventListener('click', () => setNavigationOpen(!sidebar.classList.contains('open')));
mobileMoreButton?.addEventListener('click', () => setNavigationOpen(!sidebar.classList.contains('open')));
sidebarClose?.addEventListener('click', () => setNavigationOpen(false));
sidebarScrim?.addEventListener('click', () => setNavigationOpen(false));
document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && sidebar.classList.contains('open')) setNavigationOpen(false);
});

const pageSurfaces = [...document.querySelectorAll('[data-page]')];
const pageLinks = [...document.querySelectorAll('[data-page-link]')];
const hashToPage = new Map([
  ['campus', 'campus'], ['today', 'today'], ['schedule', 'today'],
  ['updates-page', 'updates'], ['live-classes-page', 'live-classes'],
  ['circles-page', 'circles'], ['messages-page', 'messages'], ['events-page', 'events'],
  ['services-page', 'services'], ['opportunities-page', 'opportunities'], ['map-page', 'map'],
  ['wellbeing-page', 'wellbeing'], ['lost-found-page', 'lost-found'],
  ['lost-found-review-page', 'lost-found-review'], ['pilot-tests-page', 'pilot-tests'],
  ['admin-page', 'admin'],
  ['assistant-page', 'assistant']
]);

function showPage(pageName, { updateHistory = true, focus = true } = {}) {
  const requestedPage = pageSurfaces.find((page) => page.dataset.page === pageName);
  const nextPage = requestedPage ?? pageSurfaces.find((page) => page.dataset.page === 'campus');
  document.body.dataset.activePage = nextPage.dataset.page;
  pageSurfaces.forEach((page) => { page.hidden = page !== nextPage; });
  pageLinks.forEach((link) => {
    const active = link.dataset.pageLink === nextPage.dataset.page;
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  setNavigationOpen(false);
  notificationPanel.hidden = true;
  notificationButton?.setAttribute('aria-expanded', 'false');
  if (updateHistory) {
    const targetLink = pageLinks.find((link) => link.dataset.pageLink === nextPage.dataset.page);
    history.pushState({ page: nextPage.dataset.page }, '', targetLink?.hash || '#campus');
  }
  window.scrollTo({ top: 0, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
  if (focus && nextPage !== document.querySelector('#campus')) nextPage.focus({ preventScroll: true });
  if (nextPage.dataset.page === 'today') void loadCampusSchedule();
  if (nextPage.dataset.page === 'events') void loadCampusEvents();
  if (nextPage.dataset.page === 'services') void Promise.all([loadCampusServices(), loadCampusBookings()]);
  if (nextPage.dataset.page === 'opportunities') void loadOpportunities();
  if (nextPage.dataset.page === 'lost-found') void loadLostFound();
  if (nextPage.dataset.page === 'messages') void loadMessagingWorkspace();
  if (nextPage.dataset.page === 'circles') void loadCircles();
  if (nextPage.dataset.page === 'updates') void loadAnnouncements();
  if (nextPage.dataset.page === 'live-classes') void loadMeetings();
  if (nextPage.dataset.page === 'wellbeing') void loadSupportCases();
  if (nextPage.dataset.page === 'lost-found-review' && staffReviewAccess) void loadStaffReview();
  if (nextPage.dataset.page === 'map' && pilotMode) void loadLocationPanel();
  if (nextPage.dataset.page === 'pilot-tests' && pilotMode) void loadPilotTests();
  if (nextPage.dataset.page === 'admin' && adminAccess) void loadAdminWorkspace();
  if (nextPage.dataset.page === 'assistant') void Promise.all([loadAssistantStatus(), loadAssistantWorkspace()]);
  if (nextPage.dataset.page !== 'map') clearInterval(locationRefreshTimer);
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
  if (willOpen) void loadNotifications();
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

markAllRead?.addEventListener('click', async () => {
  markAllRead.disabled = true;
  try {
    await socialPost('/api/social/notifications/read-all');
    await loadNotifications();
  } catch (error) {
    showToast(error.message);
  } finally {
    markAllRead.disabled = false;
  }
});

document.querySelector('#notificationMessagesLink')?.addEventListener('click', () => showPage('messages'));

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape' && !notificationPanel.hidden) {
    notificationClose.click();
  }
});

let globalSearchTimer;
globalSearch?.addEventListener('input', () => {
  const query = globalSearch.value.trim().toLowerCase();
  if (signedIn && query.length >= 2) {
    clearTimeout(globalSearchTimer);
    globalSearchTimer = setTimeout(() => loadGlobalSearch(query), 220);
  } else if (globalSearchResults) {
    globalSearchResults.hidden = true;
    globalSearchResults.replaceChildren();
  }
  let visible = 0;
  document.querySelectorAll('.searchable').forEach((item) => {
    const matches = !query || item.dataset.search?.includes(query) || item.textContent.toLowerCase().includes(query);
    item.classList.toggle('search-hidden', !matches);
    if (matches) visible += 1;
  });
  searchEmpty.hidden = visible !== 0;
});

async function loadGlobalSearch(query) {
  try {
    const response = await fetch(`/api/search?query=${encodeURIComponent(query)}`, { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error();
    const results = await response.json();
    if (globalSearch.value.trim().toLowerCase() !== query) return;
    globalSearchResults.replaceChildren();
    if (!results.length) {
      const empty = document.createElement('p');
      empty.textContent = 'No permitted Test Campus records match this search.';
      globalSearchResults.append(empty);
    } else {
      results.forEach((result) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.setAttribute('role', 'option');
        const type = document.createElement('span');
        type.textContent = result.type.toLowerCase().replaceAll('_', ' ');
        const copy = document.createElement('span');
        const title = document.createElement('strong');
        title.textContent = result.title;
        const subtitle = document.createElement('small');
        subtitle.textContent = result.subtitle;
        copy.append(title, subtitle);
        button.append(type, copy);
        button.addEventListener('click', () => {
          const pageName = hashToPage.get(result.destination.slice(1)) ?? 'campus';
          globalSearchResults.hidden = true;
          globalSearch.value = '';
          showPage(pageName);
        });
        globalSearchResults.append(button);
      });
    }
    globalSearchResults.hidden = false;
  } catch {
    globalSearchResults.hidden = true;
  }
}

document.addEventListener('pointerdown', (event) => {
  if (!globalSearchResults?.hidden && !globalSearchResults.contains(event.target) && !globalSearch.contains(event.target)) {
    globalSearchResults.hidden = true;
  }
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

function showToast(message) {
  let toast = document.querySelector('.prototype-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.className = 'prototype-toast';
    toast.setAttribute('role', 'status');
    toast.setAttribute('aria-live', 'polite');
    document.body.append(toast);
  }
  toast.textContent = message;
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
const bookingDateStrip = document.querySelector('#bookingDateStrip');
const bookingTimeStrip = document.querySelector('#bookingTimeStrip');
const eventCatalogue = document.querySelector('#eventCatalogue');
const dashboardEventList = document.querySelector('#dashboardEventList');
const serviceCatalogue = document.querySelector('#serviceCatalogue');
const bookingActivity = document.querySelector('#bookingActivity');
const scheduleList = document.querySelector('#schedule-list');
const todayCommitmentList = document.querySelector('#todayCommitmentList');
const todayPlannedCount = document.querySelector('#todayPlannedCount');
const todayCompletedCount = document.querySelector('#todayCompletedCount');
const todayCampusCommitmentCount = document.querySelector('#todayCampusCommitmentCount');
const todayFinishTime = document.querySelector('#todayFinishTime');
const opportunityCatalogue = document.querySelector('#opportunityCatalogue');
const dashboardOpportunityList = document.querySelector('#dashboardOpportunityList');
const opportunitySearch = document.querySelector('#opportunitySearch');
const opportunityDialog = document.querySelector('#opportunityDialog');
const lostFoundCatalogue = document.querySelector('#lostFoundCatalogue');
const lostFoundSearch = document.querySelector('#lostFoundSearch');
const lostFoundCategoryFilter = document.querySelector('#lostFoundCategoryFilter');
const lostFoundReportDialog = document.querySelector('#lostFoundReportDialog');
const lostFoundDetailDialog = document.querySelector('#lostFoundDetailDialog');
const staffReviewNav = document.querySelector('#staffReviewNav');
const adminNav = document.querySelector('#adminNav');
const staffReviewDialog = document.querySelector('#staffReviewDialog');
const staffReviewList = document.querySelector('#staffReviewList');
const circleCatalogue = document.querySelector('#circleCatalogue');
const dashboardCircleList = document.querySelector('#dashboardCircleList');
const circleDialog = document.querySelector('#circleDialog');
const circleForm = document.querySelector('#circleForm');
const circleSearch = document.querySelector('#circleSearch');
const guestScheduleMarkup = scheduleList?.innerHTML || '';
const agendaList = document.querySelector('#today .agenda-list');

function renderSignedOutAccountStates() {
  signedIn = false;
  clearInterval(meetingRoomRefreshTimer);
  if (scheduleList) scheduleList.innerHTML = guestScheduleMarkup;
  if (agendaList) agendaList.innerHTML = '<li class="signed-out-agenda"><svg class="icon" aria-hidden="true"><use href="#lock"/></svg><div><strong>Sign in to view your day</strong><p>Guest mode never displays a fictional timetable as your personal activity.</p></div></li>';
  if (todayPlannedCount) todayPlannedCount.textContent = '—';
  if (todayCompletedCount) todayCompletedCount.textContent = '—';
  if (todayCampusCommitmentCount) todayCampusCommitmentCount.textContent = '—';
  if (todayFinishTime) todayFinishTime.textContent = '—';
  document.querySelector('#calendarDownloadButton')?.setAttribute('disabled', '');
  const bookingName = document.querySelector('#bookingName');
  const bookingDetail = document.querySelector('#bookingDetail');
  if (bookingName) bookingName.textContent = 'Sign in to view services';
  if (bookingDetail) bookingDetail.textContent = 'No personal booking information is loaded';
  if (bookButton) bookButton.disabled = true;
  const dashboardIntro = document.querySelector('[data-dashboard-intro]');
  if (dashboardIntro) dashboardIntro.textContent = 'Explore UniCircle or sign in to see your account activity.';
  const dashboardAssistantIntro = document.querySelector('#dashboardAssistantIntro');
  if (dashboardAssistantIntro) dashboardAssistantIntro.textContent = 'Ask for basic study, computing and campus guidance. Account-specific university data is not available to guests.';
}

function renderSignedInAccountStates() {
  signedIn = true;
  if (agendaList) agendaList.innerHTML = '<li class="signed-out-agenda"><svg class="icon" aria-hidden="true"><use href="#calendar"/></svg><div><strong>Loading your day…</strong><p>Your assigned and booked activities will appear here.</p></div></li>';
  if (todayCompletedCount) todayCompletedCount.textContent = '0';
  if (todayFinishTime) todayFinishTime.textContent = '—';
  document.querySelector('#calendarDownloadButton')?.removeAttribute('disabled');
  const dashboardIntro = document.querySelector('[data-dashboard-intro]');
  if (dashboardIntro) dashboardIntro.textContent = 'Here is what is happening in your UniCircle pilot today.';
  const dashboardAssistantIntro = document.querySelector('#dashboardAssistantIntro');
  if (dashboardAssistantIntro) dashboardAssistantIntro.textContent = 'Ask for study, computing and campus guidance, then open Ask UniCircle to keep separate conversation histories.';
}
let loadedCampusEvents = [];
let loadedCampusServices = [];
let selectedBookingSlotId = null;
let selectedEventCategory = '';
let loadedOpportunities = [];
let trackedOpportunities = [];
let selectedOpportunityType = '';
let showingOpportunityTracker = false;
let activeOpportunityId = null;
let loadedLostFoundReports = [];
let myLostFoundReports = [];
let myLostFoundClaims = [];
let selectedLostFoundType = '';
let lostFoundView = 'catalogue';
let activeLostFoundReportId = null;
let staffReviewAccess = false;
let adminAccess = false;
let staffPendingReports = [];
let activeStaffReportId = null;
let loadedCircles = [];
let selectedCircleCategory = '';
let joinedCirclesOnly = false;
let circleSearchTimer;
let loadedScheduleActivity = [];

function circlePresentation(category) {
  const presentation = {
    ACADEMIC: ['circle-computing', 'info', 'book-open'],
    SOCIAL: ['circle-games', 'warning', 'dice'],
    COMMUNITY: ['circle-sustainability', 'success', 'leaf'],
    CAREERS: ['circle-creative', 'warning', 'briefcase'],
    PROJECTS: ['circle-projects', 'neutral', 'puzzle']
  };
  return presentation[category] || presentation.COMMUNITY;
}

function readableCircleCategory(category) {
  return category.charAt(0) + category.slice(1).toLowerCase();
}

function renderDashboardCircles() {
  if (!dashboardCircleList) return;
  dashboardCircleList.replaceChildren();
  loadedCircles.slice(0, 3).forEach((circle) => {
    const [photoClass] = circlePresentation(circle.category);
    const row = document.createElement('li');
    const photo = document.createElement('div');
    photo.className = `circle-photo campus-photo ${photoClass}`;
    photo.setAttribute('role', 'img');
    photo.setAttribute('aria-label', `${circle.name} community`);
    const copy = document.createElement('div');
    const title = document.createElement('strong');
    title.textContent = circle.name;
    const meta = document.createElement('small');
    meta.textContent = `${circle.memberCount} member${circle.memberCount === 1 ? '' : 's'} · ${readableCircleCategory(circle.category)}`;
    copy.append(title, meta);
    const action = document.createElement('button');
    action.className = circle.joined ? 'small-action joined' : 'small-action';
    action.type = 'button';
    action.textContent = circle.owner ? 'Owner' : circle.joined ? 'Joined' : 'Join';
    action.disabled = circle.owner;
    action.addEventListener('click', () => updateCircleMembership(circle, action));
    row.append(photo, copy, action);
    dashboardCircleList.append(row);
  });
  if (!loadedCircles.length) {
    const row = document.createElement('li');
    row.className = 'loading-note';
    row.textContent = 'No circles match your current filters.';
    dashboardCircleList.append(row);
  }
}

function renderCircles() {
  if (!circleCatalogue) return;
  circleCatalogue.replaceChildren();
  loadedCircles.forEach((circle) => {
    const [photoClass, pillClass, iconName] = circlePresentation(circle.category);
    const card = document.createElement('article');
    card.className = 'feature-card circle-card';
    const visual = document.createElement('div');
    visual.className = `feature-avatar campus-photo ${photoClass}`;
    visual.setAttribute('role', 'img');
    visual.setAttribute('aria-label', `${circle.name} community`);
    const pill = document.createElement('span');
    pill.className = `pill ${pillClass}`;
    pill.textContent = readableCircleCategory(circle.category);
    const title = document.createElement('h3');
    title.textContent = circle.name;
    const description = document.createElement('p');
    description.textContent = circle.description;
    const meta = document.createElement('div');
    meta.className = 'feature-meta';
    const members = document.createElement('span');
    members.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#users"/></svg>`;
    members.append(document.createTextNode(`${circle.memberCount} member${circle.memberCount === 1 ? '' : 's'}`));
    const status = document.createElement('span');
    status.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${iconName}"/></svg>`;
    status.append(document.createTextNode(circle.owner ? 'You created this' : circle.joined ? 'Member' : 'Open to join'));
    meta.append(members, status);
    const action = document.createElement('button');
    action.className = circle.joined ? 'secondary-button joined' : 'primary-compact';
    action.type = 'button';
    action.textContent = circle.owner ? 'Circle owner' : circle.joined ? 'Leave circle' : 'Join circle';
    action.disabled = circle.owner;
    action.setAttribute('aria-pressed', String(circle.joined));
    action.addEventListener('click', () => updateCircleMembership(circle, action));
    card.append(visual, pill, title, description, meta, action);
    circleCatalogue.append(card);
  });
  if (!loadedCircles.length) {
    circleCatalogue.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#search"/></svg><h3>No matching circles</h3><p>Try another search or create a new community.</p></div>';
  }
  renderDashboardCircles();
}

async function loadCircles() {
  if (!circleCatalogue && !dashboardCircleList) return;
  const parameters = new URLSearchParams();
  const query = circleSearch?.value.trim();
  if (query) parameters.set('query', query);
  if (selectedCircleCategory) parameters.set('category', selectedCircleCategory);
  if (joinedCirclesOnly) parameters.set('joined', 'true');
  try {
    const response = await fetch(`/api/community/circles?${parameters}`, { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      if (circleCatalogue) circleCatalogue.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#lock"/></svg><h3>Sign in to join campus circles</h3><p>Circles are kept within each university community.</p><a class="primary-button" href="/sign-in">Sign in</a></div>';
      if (dashboardCircleList) dashboardCircleList.innerHTML = '<li class="loading-note">Sign in to discover campus circles.</li>';
      return;
    }
    if (!response.ok) throw new Error('Campus circles are temporarily unavailable.');
    loadedCircles = await response.json();
    renderCircles();
  } catch (error) {
    if (circleCatalogue) circleCatalogue.innerHTML = '<div class="empty-state compact"><h3>Circles are temporarily unavailable</h3><p>Please try again shortly.</p></div>';
    if (dashboardCircleList) dashboardCircleList.innerHTML = '<li class="loading-note">Circles are temporarily unavailable.</li>';
  }
}

async function updateCircleMembership(circle, button) {
  button.disabled = true;
  try {
    await socialPost(`/api/community/circles/${circle.id}/${circle.joined ? 'leave' : 'join'}`);
    showToast(circle.joined ? `You left ${circle.name}.` : `You joined ${circle.name}.`);
    await loadCircles();
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
}

document.querySelector('#circleCreateButton')?.addEventListener('click', async () => {
  try {
    const response = await fetch('/api/profile/me', { headers: { Accept: 'application/json' } });
    if (!response.ok) {
      window.location.assign('/sign-in');
      return;
    }
    document.querySelector('#circleFormStatus').textContent = '';
    openModal(circleDialog);
    document.querySelector('#circleName')?.focus();
  } catch {
    window.location.assign('/sign-in');
  }
});

['#circleDialogClose', '#circleDialogCancel'].forEach((selector) => {
  document.querySelector(selector)?.addEventListener('click', () => closeModal(circleDialog));
});

circleForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const submit = circleForm.querySelector('button[type="submit"]');
  const status = document.querySelector('#circleFormStatus');
  submit.disabled = true;
  status.textContent = 'Creating your circle…';
  try {
    await socialPost('/api/community/circles', {
      name: document.querySelector('#circleName').value,
      description: document.querySelector('#circleDescription').value,
      category: document.querySelector('#circleCategory').value
    });
    circleForm.reset();
    closeModal(circleDialog);
    showToast('Your new campus circle is ready.');
    await loadCircles();
  } catch (error) {
    status.textContent = error.message;
  } finally {
    submit.disabled = false;
  }
});

circleSearch?.addEventListener('input', () => {
  clearTimeout(circleSearchTimer);
  circleSearchTimer = setTimeout(() => void loadCircles(), 250);
});

document.querySelectorAll('#circleFilters [data-circle-category], #circleFilters [data-circle-view]').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('#circleFilters .filter-chip').forEach((other) => other.classList.remove('active'));
    button.classList.add('active');
    selectedCircleCategory = button.dataset.circleCategory || '';
    joinedCirclesOnly = button.dataset.circleView === 'joined';
    void loadCircles();
  });
});

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
  bookingName.textContent = facilityMode ? 'Available facilities' : 'Available appointments';
  bookingDetail.textContent = 'Choose a verified fictional service slot';
  bookButton.textContent = facilityMode ? 'Book facility' : 'Book appointment';
  document.querySelector('#bookingStatus').textContent = '';
  renderDashboardBookingWidget();
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

function shortEventDate(value) {
  return new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short' })
    .format(new Date(value)).toUpperCase();
}

function fullCampusDate(value) {
  return new Intl.DateTimeFormat('en-GB', {
    weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
  }).format(new Date(value));
}

function campusTime(value) {
  return new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit' }).format(new Date(value));
}

function campusDayKey(value) {
  const date = new Date(value);
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
}

function eventPresentation(category) {
  const presentations = {
    CAREERS: ['careers', 'briefcase', 'success'],
    WELLBEING: ['wellbeing', 'mindfulness', 'info'],
    SOCIAL: ['social', 'food', 'warning'],
    ACADEMIC: ['academic', 'presentation', 'info']
  };
  return presentations[category] || presentations.ACADEMIC;
}

function renderDashboardEvents() {
  if (!dashboardEventList) return;
  dashboardEventList.replaceChildren();
  loadedCampusEvents.slice(0, 3).forEach((event) => {
    const [visualClass] = eventPresentation(event.category);
    const item = document.createElement('li');
    const date = document.createElement('time');
    const parsed = new Date(event.startsAt);
    date.innerHTML = `<strong>${parsed.getDate()}</strong><small>${parsed.toLocaleString('en-GB', { month: 'short' }).toUpperCase()}</small>`;
    const visual = document.createElement('div');
    visual.className = `event-thumb event-photo ${visualClass}`;
    visual.setAttribute('role', 'img');
    visual.setAttribute('aria-label', `${event.title} campus event photograph`);
    const copy = document.createElement('div');
    const title = document.createElement('strong');
    title.textContent = event.title;
    const when = document.createElement('small');
    when.textContent = `${fullCampusDate(event.startsAt)}–${campusTime(event.endsAt)}`;
    const venue = document.createElement('small');
    venue.className = 'meta-with-icon';
    venue.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#map-pin"/></svg>';
    venue.append(document.createTextNode(event.venue));
    copy.append(title, when, venue);
    const tag = document.createElement('span');
    tag.className = 'tag green-tag';
    tag.textContent = event.registered ? 'Registered' : event.category.toLowerCase();
    item.append(date, visual, copy, tag);
    dashboardEventList.append(item);
  });
}

function renderCampusEvents() {
  if (!eventCatalogue) return;
  eventCatalogue.replaceChildren();
  const visibleEvents = selectedEventCategory
    ? loadedCampusEvents.filter((event) => event.category === selectedEventCategory)
    : loadedCampusEvents;
  document.querySelector('#eventCountBadge').textContent = selectedEventCategory
    ? `${visibleEvents.length} matching event${visibleEvents.length === 1 ? '' : 's'}`
    : `${visibleEvents.length} available events`;
  visibleEvents.forEach((event) => {
    const [visualClass, iconName, pillClass] = eventPresentation(event.category);
    const card = document.createElement('article');
    card.className = 'event-card-large';
    card.dataset.category = event.category.toLowerCase();
    const visual = document.createElement('div');
    visual.className = `event-visual ${visualClass}`;
    visual.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${iconName}"/></svg><span>${shortEventDate(event.startsAt)}</span>`;
    const details = document.createElement('div');
    const pill = document.createElement('span');
    pill.className = `pill ${pillClass}`;
    pill.textContent = event.category.charAt(0) + event.category.slice(1).toLowerCase();
    const title = document.createElement('h3');
    title.textContent = event.title;
    const summary = document.createElement('p');
    summary.textContent = event.summary;
    const meta = document.createElement('ul');
    meta.className = 'meta-list';
    meta.innerHTML = `<li><svg class="icon" aria-hidden="true"><use href="#clock"/></svg>${fullCampusDate(event.startsAt)}–${campusTime(event.endsAt)}</li><li><svg class="icon" aria-hidden="true"><use href="#map-pin"/></svg></li>`;
    meta.lastElementChild.append(document.createTextNode(event.venue));
    const places = document.createElement('p');
    places.className = 'availability-copy';
    places.textContent = `${event.placesRemaining} of ${event.capacity} places remaining`;
    const action = document.createElement('button');
    action.className = event.registered ? 'secondary-button registered-action' : 'primary-compact';
    action.type = 'button';
    action.textContent = event.registered ? 'Cancel registration' : 'Register';
    action.disabled = !event.registered && event.placesRemaining === 0;
    action.addEventListener('click', async () => {
      action.disabled = true;
      try {
        const operation = event.registered ? 'cancel' : 'register';
        await socialPost(`/api/campus-life/events/${event.id}/${operation}`);
        showToast(event.registered ? 'Your event registration has been cancelled.' : `${event.title} is now in your schedule.`);
        await Promise.all([loadCampusEvents(), loadCampusSchedule(), loadNotifications()]);
      } catch (error) {
        showToast(error.message);
      } finally {
        action.disabled = false;
      }
    });
    details.append(pill, title, summary, meta, places, action);
    card.append(visual, details);
    eventCatalogue.append(card);
  });
}

async function loadCampusEvents() {
  if (!eventCatalogue) return;
  try {
    const response = await fetch('/api/campus-life/events', {
      headers: { Accept: 'application/json' }
    });
    if (response.status === 401 || response.status === 403) {
      eventCatalogue.innerHTML = '<div class="empty-state compact"><h3>Sign in to view campus events</h3><p>Events are matched to your verified university account.</p></div>';
      document.querySelector('#eventCountBadge').textContent = 'Sign in required';
      if (dashboardEventList) dashboardEventList.innerHTML = '<li class="signed-out-card-state"><svg class="icon" aria-hidden="true"><use href="#lock"/></svg><div><strong>Sign in to view pilot events</strong><small>No fictional dates are presented as your upcoming events.</small></div></li>';
      return;
    }
    if (!response.ok) throw new Error();
    loadedCampusEvents = await response.json();
    renderCampusEvents();
    renderDashboardEvents();
  } catch {
    eventCatalogue.innerHTML = '<div class="empty-state compact"><h3>Events are temporarily unavailable</h3><p>Please try again shortly.</p></div>';
    document.querySelector('#eventCountBadge').textContent = 'Unavailable';
    if (dashboardEventList) dashboardEventList.innerHTML = '<li class="signed-out-card-state"><svg class="icon" aria-hidden="true"><use href="#alert"/></svg><div><strong>Events temporarily unavailable</strong><small>Refresh the page or try again shortly.</small></div></li>';
  }
}

document.querySelectorAll('[data-event-category]').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('[data-event-category]').forEach((other) => other.classList.remove('active'));
    button.classList.add('active');
    selectedEventCategory = button.dataset.eventCategory;
    renderCampusEvents();
  });
});

function serviceIcon(type) {
  if (type === 'FACILITY') return ['room', 'green'];
  if (type === 'CAMPUS') return ['id-badge', 'violet'];
  return ['graduation', 'blue'];
}

function renderServiceCatalogue() {
  if (!serviceCatalogue) return;
  serviceCatalogue.replaceChildren();
  loadedCampusServices.forEach((service) => {
    const [iconName, colour] = serviceIcon(service.type);
    const card = document.createElement('article');
    card.className = 'service-booking-card';
    const header = document.createElement('header');
    header.innerHTML = `<span class="view-icon ${colour}"><svg class="icon" aria-hidden="true"><use href="#${iconName}"/></svg></span>`;
    const heading = document.createElement('div');
    const title = document.createElement('h3');
    title.textContent = service.title;
    const provider = document.createElement('p');
    provider.textContent = `${service.providerName} · ${service.location}`;
    heading.append(title, provider);
    header.append(heading);
    const description = document.createElement('p');
    description.textContent = service.description;
    const slots = document.createElement('div');
    slots.className = 'service-slot-list';
    service.slots.forEach((slot) => {
      const row = document.createElement('div');
      row.className = 'service-slot';
      const copy = document.createElement('div');
      const when = document.createElement('strong');
      when.textContent = `${fullCampusDate(slot.startsAt)}–${campusTime(slot.endsAt)}`;
      const remaining = document.createElement('span');
      remaining.textContent = `${slot.placesRemaining} place${slot.placesRemaining === 1 ? '' : 's'} available`;
      copy.append(when, remaining);
      const action = document.createElement('button');
      action.className = slot.booked ? 'secondary-button registered-action' : 'secondary-button';
      action.type = 'button';
      action.textContent = slot.booked ? 'Booked' : 'Book';
      action.disabled = slot.booked || slot.placesRemaining === 0;
      action.addEventListener('click', () => bookServiceSlot(slot.id, service.title, action));
      row.append(copy, action);
      slots.append(row);
    });
    card.append(header, description, slots);
    serviceCatalogue.append(card);
  });
}

function renderDashboardBookingWidget() {
  if (!bookingDateStrip || !bookingTimeStrip) return;
  const mode = document.querySelector('.tabs .tab[aria-selected="true"]')?.dataset.mode || 'appointment';
  const type = mode === 'facility' ? 'FACILITY' : 'APPOINTMENT';
  const service = loadedCampusServices.find((item) => item.type === type);
  bookingDateStrip.replaceChildren();
  bookingTimeStrip.replaceChildren();
  selectedBookingSlotId = null;
  if (!service || !service.slots.length) {
    bookingDateStrip.innerHTML = '<span class="loading-copy">No available times.</span>';
    bookButton.disabled = true;
    return;
  }
  bookingName.textContent = service.providerName;
  bookingDetail.textContent = `${service.title} · ${service.location}`;
  const dates = [...new Map(service.slots.map((slot) => [campusDayKey(slot.startsAt), slot])).values()];
  const selectDate = (key, dateButton) => {
    bookingDateStrip.querySelectorAll('button').forEach((button) => {
      button.classList.toggle('selected', button === dateButton);
      button.setAttribute('aria-pressed', String(button === dateButton));
    });
    bookingTimeStrip.replaceChildren();
    service.slots.filter((slot) => campusDayKey(slot.startsAt) === key).forEach((slot, index) => {
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = campusTime(slot.startsAt);
      button.disabled = slot.booked || slot.placesRemaining === 0;
      button.setAttribute('aria-pressed', 'false');
      button.addEventListener('click', () => {
        bookingTimeStrip.querySelectorAll('button').forEach((other) => {
          other.classList.toggle('selected', other === button);
          other.setAttribute('aria-pressed', String(other === button));
        });
        selectedBookingSlotId = slot.id;
        bookButton.disabled = false;
      });
      bookingTimeStrip.append(button);
      if (index === 0 && !button.disabled) button.click();
    });
  };
  dates.forEach((slot, index) => {
    const date = new Date(slot.startsAt);
    const button = document.createElement('button');
    button.type = 'button';
    button.innerHTML = `${date.toLocaleString('en-GB', { weekday: 'short' })}<small>${date.getDate()} ${date.toLocaleString('en-GB', { month: 'short' })}</small>`;
    button.setAttribute('aria-pressed', 'false');
    button.addEventListener('click', () => selectDate(campusDayKey(slot.startsAt), button));
    bookingDateStrip.append(button);
    if (index === 0) button.click();
  });
}

async function bookServiceSlot(slotId, serviceTitle, button) {
  button.disabled = true;
  try {
    await socialPost(`/api/campus-life/service-slots/${slotId}/book`);
    showToast(`${serviceTitle} has been confirmed and added to your schedule.`);
    await Promise.all([loadCampusServices(), loadCampusBookings(), loadCampusSchedule(), loadNotifications()]);
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
}

async function loadCampusServices() {
  if (!serviceCatalogue) return;
  try {
    const response = await fetch('/api/campus-life/services', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      serviceCatalogue.innerHTML = '<div class="empty-state compact"><h3>Sign in to book a service</h3><p>Availability is account-specific.</p></div>';
      renderSignedOutAccountStates();
      return;
    }
    if (!response.ok) throw new Error();
    loadedCampusServices = await response.json();
    renderServiceCatalogue();
    renderDashboardBookingWidget();
  } catch {
    serviceCatalogue.innerHTML = '<div class="empty-state compact"><h3>Services are temporarily unavailable</h3><p>Please try again shortly.</p></div>';
  }
}

function renderCampusBookings(bookings) {
  if (!bookingActivity) return;
  const confirmed = bookings.filter((booking) => booking.status === 'CONFIRMED');
  document.querySelector('#bookingCountBadge').textContent = `${confirmed.length} confirmed`;
  bookingActivity.replaceChildren();
  if (!confirmed.length) {
    bookingActivity.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#calendar"/></svg><h3>No bookings yet</h3><p>Confirmed service bookings will appear here.</p></div>';
    return;
  }
  confirmed.forEach((booking) => {
    const item = document.createElement('article');
    item.className = 'booking-record';
    const icon = document.createElement('span');
    icon.className = 'view-icon green';
    icon.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#check"/></svg>';
    const copy = document.createElement('div');
    const title = document.createElement('h4');
    title.textContent = booking.serviceTitle;
    const details = document.createElement('p');
    details.textContent = `${booking.providerName} · ${fullCampusDate(booking.startsAt)} · ${booking.location}`;
    copy.append(title, details);
    const cancel = document.createElement('button');
    cancel.className = 'secondary-button';
    cancel.type = 'button';
    cancel.textContent = 'Cancel booking';
    cancel.addEventListener('click', async () => {
      cancel.disabled = true;
      try {
        await socialPost(`/api/campus-life/bookings/${booking.bookingId}/cancel`);
        showToast('Your service booking has been cancelled.');
        await Promise.all([loadCampusServices(), loadCampusBookings(), loadCampusSchedule()]);
      } catch (error) {
        showToast(error.message);
      } finally {
        cancel.disabled = false;
      }
    });
    item.append(icon, copy, cancel);
    bookingActivity.append(item);
  });
}

async function loadCampusBookings() {
  if (!bookingActivity) return;
  try {
    const response = await fetch('/api/campus-life/bookings', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      document.querySelector('#bookingCountBadge').textContent = 'Sign in required';
      bookingActivity.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#lock"/></svg><h3>Sign in to view bookings</h3><p>Booking history is private to your account.</p></div>';
      return;
    }
    if (!response.ok) throw new Error('Bookings unavailable');
    renderCampusBookings(await response.json());
  } catch {
    document.querySelector('#bookingCountBadge').textContent = 'Unavailable';
    bookingActivity.innerHTML = '<div class="empty-state compact"><h3>Booking activity is temporarily unavailable</h3></div>';
  }
}

function isToday(value) {
  const date = new Date(value);
  const today = new Date();
  return date.getFullYear() === today.getFullYear()
    && date.getMonth() === today.getMonth()
    && date.getDate() === today.getDate();
}

function schedulePresentation(resourceType) {
  if (resourceType === 'EVENT') return { icon: 'events', colour: 'orange', dot: 'orange-dot', short: 'Registered', long: 'Registered event' };
  if (resourceType === 'MEETING') return { icon: 'monitor-play', colour: 'blue', dot: 'blue-dot', short: 'Session', long: 'Assigned session' };
  return { icon: 'calendar', colour: 'green', dot: 'green-dot', short: 'Booked', long: 'Confirmed booking' };
}

function appendScheduleRow(container, item) {
  const style = schedulePresentation(item.resourceType);
  const row = document.createElement('li');
  const time = document.createElement('time');
  time.innerHTML = `${campusTime(item.startsAt)}<small>${campusTime(item.endsAt)}</small>`;
  const dot = document.createElement('span');
  dot.className = `time-dot ${style.dot}`;
  const copy = document.createElement('div');
  const title = document.createElement('strong');
  title.textContent = item.title;
  const location = document.createElement('small');
  location.textContent = item.location;
  copy.append(title, location);
  const status = document.createElement('span');
  status.className = 'status upcoming';
  status.textContent = style.short;
  row.append(time, dot, copy, status);
  container.append(row);
}

function appendAgendaRow(item) {
  if (!agendaList) return;
  const style = schedulePresentation(item.resourceType);
  const row = document.createElement('li');
  const time = document.createElement('time');
  time.innerHTML = `${campusTime(item.startsAt)}<span>${campusTime(item.endsAt)}</span>`;
  const marker = document.createElement('span');
  marker.className = `agenda-marker ${style.dot}`;
  const copy = document.createElement('div');
  const title = document.createElement('strong');
  title.textContent = item.title;
  const location = document.createElement('p');
  location.textContent = item.location;
  const status = document.createElement('span');
  status.className = `pill ${style.colour === 'orange' ? 'warning' : style.colour === 'green' ? 'success' : 'info'}`;
  status.textContent = style.long;
  copy.append(title, location, status);
  row.append(time, marker, copy);
  agendaList.append(row);
}

async function loadCampusSchedule() {
  if (!scheduleList && !todayCommitmentList && !agendaList) return;
  try {
    const response = await fetch('/api/campus-life/schedule', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      renderSignedOutAccountStates();
      if (todayCommitmentList) todayCommitmentList.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#lock"/></svg><h3>Sign in to view commitments</h3><p>Your registrations and bookings are private account records.</p></div>';
      return;
    }
    if (!response.ok) throw new Error('Schedule unavailable');
    const activity = await response.json();
    loadedScheduleActivity = activity;
    const todayActivity = activity.filter((item) => isToday(item.startsAt));
    if (scheduleList) scheduleList.replaceChildren();
    if (agendaList) agendaList.replaceChildren();
    if (todayCommitmentList) todayCommitmentList.replaceChildren();
    if (todayPlannedCount) todayPlannedCount.textContent = String(todayActivity.length);
    if (todayCompletedCount) todayCompletedCount.textContent = String(todayActivity.filter((item) => new Date(item.endsAt) < new Date()).length);
    if (todayCampusCommitmentCount) todayCampusCommitmentCount.textContent = String(activity.length);
    const endTimes = todayActivity.map((item) => new Date(item.endsAt)).filter((date) => !Number.isNaN(date.getTime()));
    if (todayFinishTime) todayFinishTime.textContent = endTimes.length ? campusTime(new Date(Math.max(...endTimes))) : '—';
    todayActivity.forEach((item) => {
      if (scheduleList) appendScheduleRow(scheduleList, item);
      appendAgendaRow(item);
    });
    activity.forEach((item) => {
      if (todayCommitmentList) {
        const style = schedulePresentation(item.resourceType);
        const card = document.createElement('article');
        card.className = 'booking-record schedule-commitment';
        const icon = document.createElement('span');
        icon.className = `view-icon ${style.colour}`;
        icon.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${style.icon}"/></svg>`;
        const copy = document.createElement('div');
        const title = document.createElement('h4');
        title.textContent = item.title;
        const details = document.createElement('p');
        details.textContent = `${fullCampusDate(item.startsAt)}–${campusTime(item.endsAt)} · ${item.location}`;
        copy.append(title, details);
        const status = document.createElement('span');
        status.className = `pill ${style.colour === 'orange' ? 'warning' : style.colour === 'green' ? 'success' : 'info'}`;
        status.textContent = style.long;
        card.append(icon, copy, status);
        if (item.resourceType === 'MEETING') {
          card.tabIndex = 0;
          card.setAttribute('role', 'button');
          card.setAttribute('aria-label', `Open live session ${item.title}`);
          card.addEventListener('click', () => { showPage('live-classes'); void loadMeetingRoom(item.resourceId); });
          card.addEventListener('keydown', (event) => {
            if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); card.click(); }
          });
        }
        todayCommitmentList.append(card);
      }
    });
    if (scheduleList && !todayActivity.length) {
      scheduleList.innerHTML = '<li class="signed-out-card-state"><svg class="icon" aria-hidden="true"><use href="#calendar"/></svg><div><strong>No activities scheduled today</strong><small>Assigned sessions, registered events and confirmed bookings will appear here automatically.</small></div></li>';
    }
    if (agendaList && !todayActivity.length) {
      agendaList.innerHTML = '<li class="signed-out-agenda"><svg class="icon" aria-hidden="true"><use href="#calendar"/></svg><div><strong>Your day is clear</strong><p>No account-specific activities are scheduled for today.</p></div></li>';
    }
    if (todayCommitmentList && !activity.length) {
      todayCommitmentList.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#calendar"/></svg><h3>No upcoming commitments</h3><p>Assigned sessions, registered events and confirmed bookings will appear here automatically.</p></div>';
    }
  } catch {
    if (scheduleList) scheduleList.innerHTML = '<li class="signed-out-card-state"><svg class="icon" aria-hidden="true"><use href="#alert"/></svg><div><strong>Schedule temporarily unavailable</strong><small>Refresh the page or try again shortly.</small></div></li>';
    if (agendaList) agendaList.innerHTML = '<li class="signed-out-agenda"><svg class="icon" aria-hidden="true"><use href="#alert"/></svg><div><strong>Schedule temporarily unavailable</strong><p>Refresh the page or try again shortly.</p></div></li>';
    if (todayCommitmentList) todayCommitmentList.innerHTML = '<div class="empty-state compact"><h3>Your UniCircle schedule is temporarily unavailable</h3><p>Please try again shortly.</p></div>';
  }
}

document.querySelector('#calendarDownloadButton')?.addEventListener('click', async (event) => {
  const button = event.currentTarget;
  button.disabled = true;
  try {
    const response = await fetch('/api/campus-life/schedule', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      window.location.assign('/sign-in');
      return;
    }
    if (!response.ok) throw new Error('Your calendar could not be prepared.');
    loadedScheduleActivity = await response.json();
    if (!loadedScheduleActivity.length) throw new Error('Register for an event or book a service before downloading your calendar.');
    const download = document.createElement('a');
    download.href = '/api/campus-life/schedule.ics';
    download.hidden = true;
    document.body.append(download);
    download.click();
    setTimeout(() => download.remove(), 1000);
    showToast('Your UniCircle calendar has been downloaded.');
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
});

function opportunityPresentation(type) {
  if (type === 'PLACEMENT') return ['code', 'navy-photo', 'Placement'];
  if (type === 'VOLUNTEERING') return ['volunteer', 'green-photo', 'Volunteering'];
  return ['briefcase', 'blue-photo', 'Job'];
}

function readableOpportunityValue(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function opportunityDeadline(value) {
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(`${value}T12:00:00`));
}

function visibleOpportunities() {
  const source = showingOpportunityTracker ? trackedOpportunities : loadedOpportunities;
  const query = opportunitySearch?.value.trim().toLowerCase() || '';
  return source.filter((opportunity) => {
    const matchesType = !selectedOpportunityType || opportunity.type === selectedOpportunityType;
    const haystack = [opportunity.title, opportunity.summary, opportunity.publisherName,
      opportunity.location, ...opportunity.skills].join(' ').toLowerCase();
    return matchesType && (!query || haystack.includes(query));
  });
}

function renderDashboardOpportunities() {
  if (!dashboardOpportunityList) return;
  dashboardOpportunityList.replaceChildren();
  loadedOpportunities.slice(0, 3).forEach((opportunity) => {
    const button = document.createElement('button');
    button.type = 'button';
    const title = document.createElement('strong');
    title.textContent = opportunity.title;
    const publisher = document.createElement('small');
    publisher.textContent = opportunity.publisherName;
    const detail = document.createElement('span');
    detail.textContent = `${readableOpportunityValue(opportunity.workMode)} · closes ${opportunityDeadline(opportunity.closesAt)}`;
    button.append(title, publisher, detail);
    button.addEventListener('click', () => openOpportunityDetails(opportunity.id));
    dashboardOpportunityList.append(button);
  });
}

function renderOpportunities() {
  if (!opportunityCatalogue) return;
  const opportunities = visibleOpportunities();
  opportunityCatalogue.replaceChildren();
  document.querySelector('#opportunityCountBadge').textContent = showingOpportunityTracker
    ? `${opportunities.length} tracked opportunit${opportunities.length === 1 ? 'y' : 'ies'}`
    : `${opportunities.length} verified opportunit${opportunities.length === 1 ? 'y' : 'ies'}`;
  opportunities.forEach((opportunity) => {
    const [iconName, colour, typeLabel] = opportunityPresentation(opportunity.type);
    const card = document.createElement('article');
    card.dataset.opportunityId = opportunity.id;
    const mark = document.createElement('span');
    mark.className = `company-mark ${colour}`;
    mark.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${iconName}"/></svg>`;
    const details = document.createElement('div');
    const badges = document.createElement('span');
    badges.className = 'opportunity-badges';
    const typeBadge = document.createElement('span');
    typeBadge.className = 'pill info';
    typeBadge.textContent = typeLabel;
    const verified = document.createElement('span');
    verified.className = 'verified-label';
    verified.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#check"/></svg> Verified';
    badges.append(typeBadge, verified);
    const title = document.createElement('h3');
    title.textContent = opportunity.title;
    const summary = document.createElement('p');
    summary.textContent = opportunity.summary;
    const meta = document.createElement('span');
    meta.className = 'opportunity-meta';
    meta.textContent = `${opportunity.publisherName} · ${readableOpportunityValue(opportunity.workMode)} · ${opportunity.location}`;
    const deadline = document.createElement('small');
    deadline.textContent = `Closes ${opportunityDeadline(opportunity.closesAt)}`;
    details.append(badges, title, summary, meta, deadline);
    const actions = document.createElement('span');
    actions.className = 'opportunity-actions';
    const view = document.createElement('button');
    view.className = 'secondary-button';
    view.type = 'button';
    view.textContent = 'View details';
    view.addEventListener('click', () => openOpportunityDetails(opportunity.id));
    const save = document.createElement('button');
    save.className = opportunity.saved ? 'secondary-button registered-action' : 'secondary-button';
    save.type = 'button';
    save.textContent = opportunity.saved ? 'Saved' : 'Save';
    save.setAttribute('aria-pressed', String(opportunity.saved));
    save.addEventListener('click', () => toggleOpportunitySaved(opportunity.id, save));
    actions.append(view, save);
    if (opportunity.trackingStatus) {
      const stage = document.createElement('span');
      stage.className = 'pill success';
      stage.textContent = readableOpportunityValue(opportunity.trackingStatus);
      actions.prepend(stage);
    }
    card.append(mark, details, actions);
    opportunityCatalogue.append(card);
  });
  if (!opportunities.length) {
    opportunityCatalogue.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#briefcase"/></svg><h3>No matching opportunities</h3><p>Try another search or filter. Only approved listings from verified publishers are shown.</p></div>';
  }
}

async function loadOpportunities() {
  if (!opportunityCatalogue) return;
  try {
    const [catalogueResponse, trackerResponse] = await Promise.all([
      fetch('/api/opportunities', { headers: { Accept: 'application/json' } }),
      fetch('/api/opportunities/tracker', { headers: { Accept: 'application/json' } })
    ]);
    if ([catalogueResponse.status, trackerResponse.status].some((status) => status === 401 || status === 403)) {
      opportunityCatalogue.innerHTML = '<div class="empty-state compact"><h3>Sign in to view verified opportunities</h3><p>Listings and tracking are scoped to your university account.</p></div>';
      document.querySelector('#opportunityCountBadge').textContent = 'Sign in required';
      if (dashboardOpportunityList) dashboardOpportunityList.innerHTML = '<p class="loading-note">Sign in to load university-scoped opportunities.</p>';
      return;
    }
    if (!catalogueResponse.ok || !trackerResponse.ok) throw new Error();
    loadedOpportunities = await catalogueResponse.json();
    trackedOpportunities = await trackerResponse.json();
    renderOpportunities();
    renderDashboardOpportunities();
  } catch {
    opportunityCatalogue.innerHTML = '<div class="empty-state compact"><h3>Opportunities are temporarily unavailable</h3><p>Please try again shortly.</p></div>';
    document.querySelector('#opportunityCountBadge').textContent = 'Unavailable';
    if (dashboardOpportunityList) dashboardOpportunityList.innerHTML = '<p class="loading-note">Opportunities are temporarily unavailable.</p>';
  }
}

function opportunityById(id) {
  return loadedOpportunities.find((opportunity) => opportunity.id === id)
    || trackedOpportunities.find((opportunity) => opportunity.id === id);
}

function openOpportunityDetails(id) {
  const opportunity = opportunityById(id);
  if (!opportunity) return;
  activeOpportunityId = id;
  document.querySelector('#opportunityDialogType').textContent = `${readableOpportunityValue(opportunity.type)} · ${readableOpportunityValue(opportunity.workMode)}`;
  document.querySelector('#opportunityDialogTitle').textContent = opportunity.title;
  document.querySelector('#opportunityDialogSummary').textContent = opportunity.summary;
  document.querySelector('#opportunityPublisher').textContent = opportunity.publisherName;
  document.querySelector('#opportunityPublisherType').textContent = `${readableOpportunityValue(opportunity.publisherType)} · fictional verified source`;
  document.querySelector('#opportunityDescription').textContent = opportunity.description;
  document.querySelector('#opportunityLocation').textContent = opportunity.location;
  document.querySelector('#opportunityHours').textContent = opportunity.hoursLabel;
  document.querySelector('#opportunityCompensation').textContent = opportunity.compensationLabel;
  document.querySelector('#opportunityDeadline').textContent = opportunityDeadline(opportunity.closesAt);
  document.querySelector('#opportunityEligibility').textContent = opportunity.eligibility;
  const skills = document.querySelector('#opportunitySkills');
  skills.replaceChildren();
  opportunity.skills.forEach((skill) => {
    const chip = document.createElement('span');
    chip.className = 'module-chip';
    chip.textContent = skill;
    skills.append(chip);
  });
  const saveButton = document.querySelector('#opportunitySaveButton');
  saveButton.textContent = opportunity.saved ? 'Remove saved item' : 'Save opportunity';
  saveButton.classList.toggle('registered-action', opportunity.saved);
  saveButton.setAttribute('aria-pressed', String(opportunity.saved));
  document.querySelector('#opportunityTrackingStatus').value = opportunity.trackingStatus || 'PLANNING';
  openModal(opportunityDialog);
}

async function refreshOpportunityDetails(id) {
  await loadOpportunities();
  if (!opportunityDialog.hidden) openOpportunityDetails(id);
}

async function toggleOpportunitySaved(id, button) {
  const opportunity = opportunityById(id);
  if (!opportunity) return;
  button.disabled = true;
  try {
    await socialPost(`/api/opportunities/${id}/${opportunity.saved ? 'unsave' : 'save'}`);
    showToast(opportunity.saved ? 'Opportunity removed from your saved list.' : 'Opportunity saved to your private tracker.');
    await refreshOpportunityDetails(id);
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
}

document.querySelectorAll('[data-opportunity-type]').forEach((button) => {
  button.addEventListener('click', () => {
    showingOpportunityTracker = false;
    selectedOpportunityType = button.dataset.opportunityType;
    renderOpportunities();
  });
});

document.querySelector('[data-opportunity-view="tracker"]')?.addEventListener('click', () => {
  showingOpportunityTracker = true;
  selectedOpportunityType = '';
  renderOpportunities();
});

opportunitySearch?.addEventListener('input', renderOpportunities);
document.querySelector('#opportunityDialogClose')?.addEventListener('click', () => closeModal(opportunityDialog));
document.querySelector('#opportunitySaveButton')?.addEventListener('click', (event) => {
  if (activeOpportunityId) void toggleOpportunitySaved(activeOpportunityId, event.currentTarget);
});
document.querySelector('#opportunityTrackButton')?.addEventListener('click', async (event) => {
  if (!activeOpportunityId) return;
  const button = event.currentTarget;
  button.disabled = true;
  try {
    const status = document.querySelector('#opportunityTrackingStatus').value;
    await socialPost(`/api/opportunities/${activeOpportunityId}/track`, { status });
    showToast(`Application tracker updated to ${readableOpportunityValue(status)}.`);
    await refreshOpportunityDetails(activeOpportunityId);
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
});

function lostFoundPresentation(category) {
  if (category === 'ELECTRONICS') return ['laptop', 'navy-photo'];
  if (category === 'KEYS') return ['key', 'blue-photo'];
  if (category === 'BAGS') return ['briefcase', 'violet-photo'];
  if (category === 'DOCUMENTS') return ['id-badge', 'green-photo'];
  if (category === 'ACCESSORIES') return ['bottle', 'orange-photo'];
  if (category === 'CLOTHING') return ['users', 'coral-photo'];
  return ['search', 'blue-photo'];
}

function lostFoundDate(value) {
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
    .format(new Date(`${value}T12:00:00`));
}

function lostFoundReportById(id) {
  return loadedLostFoundReports.find((report) => report.id === id)
    || myLostFoundReports.find((report) => report.id === id);
}

function visibleLostFoundReports() {
  const source = lostFoundView === 'mine' ? myLostFoundReports : loadedLostFoundReports;
  const query = lostFoundSearch?.value.trim().toLowerCase() || '';
  const category = lostFoundCategoryFilter?.value || '';
  return source.filter((report) => {
    const haystack = [report.itemName, report.colour, report.publicDescription, report.lastSeenLocation]
      .filter(Boolean).join(' ').toLowerCase();
    return (!selectedLostFoundType || report.reportType === selectedLostFoundType)
      && (!category || report.category === category)
      && (!query || haystack.includes(query));
  });
}

function createLostFoundVisual(report) {
  const [iconName, colour] = lostFoundPresentation(report.category);
  const visual = document.createElement('div');
  visual.className = `found-visual ${colour}`;
  if (report.hasPhoto) {
    const image = document.createElement('img');
    image.src = report.photoUrl;
    image.alt = '';
    image.loading = 'lazy';
    visual.append(image);
  } else {
    visual.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${iconName}"/></svg>`;
  }
  return visual;
}

function renderLostFoundClaims() {
  lostFoundCatalogue.replaceChildren();
  document.querySelector('#lostFoundCountBadge').textContent = `${myLostFoundClaims.length} private claim${myLostFoundClaims.length === 1 ? '' : 's'}`;
  myLostFoundClaims.forEach((claim) => {
    const card = document.createElement('article');
    card.className = 'lost-found-card claim-record';
    const visual = document.createElement('span');
    visual.className = 'found-visual green-photo';
    visual.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#shield"/></svg>';
    const details = document.createElement('div');
    const badge = document.createElement('span');
    badge.className = 'pill success';
    badge.textContent = readableOpportunityValue(claim.claimStatus);
    const title = document.createElement('h3');
    title.textContent = claim.itemName;
    const copy = document.createElement('p');
    copy.textContent = `Private claim submitted ${new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium' }).format(new Date(claim.createdAt))}.`;
    const note = document.createElement('small');
    note.textContent = 'Your evidence is not displayed publicly.';
    details.append(badge, title, copy, note);
    card.append(visual, details);
    lostFoundCatalogue.append(card);
  });
  if (!myLostFoundClaims.length) {
    lostFoundCatalogue.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#shield"/></svg><h3>No ownership claims</h3><p>Claims you submit for found items will appear privately here.</p></div>';
  }
}

function renderLostFoundReports() {
  if (!lostFoundCatalogue) return;
  if (lostFoundView === 'claims') {
    renderLostFoundClaims();
    return;
  }
  const reports = visibleLostFoundReports();
  lostFoundCatalogue.replaceChildren();
  const label = lostFoundView === 'mine' ? 'your report' : 'moderated report';
  document.querySelector('#lostFoundCountBadge').textContent = `${reports.length} ${label}${reports.length === 1 ? '' : 's'}`;
  reports.forEach((report) => {
    const card = document.createElement('article');
    card.className = 'lost-found-card';
    card.dataset.reportId = report.id;
    const visual = createLostFoundVisual(report);
    const details = document.createElement('div');
    const badges = document.createElement('span');
    badges.className = 'opportunity-badges';
    const type = document.createElement('span');
    type.className = report.reportType === 'FOUND' ? 'pill success' : 'pill warning';
    type.textContent = readableOpportunityValue(report.reportType);
    const category = document.createElement('span');
    category.className = 'pill info';
    category.textContent = readableOpportunityValue(report.category);
    badges.append(type, category);
    if (report.mine && report.moderationStatus !== 'APPROVED') {
      const moderation = document.createElement('span');
      moderation.className = 'pill neutral';
      moderation.textContent = `${readableOpportunityValue(report.moderationStatus)} moderation`;
      badges.append(moderation);
    }
    const title = document.createElement('h3');
    title.textContent = report.itemName;
    const description = document.createElement('p');
    description.textContent = report.publicDescription;
    const meta = document.createElement('span');
    meta.className = 'opportunity-meta';
    meta.textContent = `${report.lastSeenLocation} · ${lostFoundDate(report.occurredOn)}`;
    const custody = document.createElement('small');
    custody.textContent = report.reportType === 'FOUND'
      ? `Custody: ${readableOpportunityValue(report.custodyStatus)}`
      : 'Location shared without publishing private ownership details';
    details.append(badges, title, description, meta, custody);
    const actions = document.createElement('span');
    actions.className = 'opportunity-actions';
    if (report.myClaimStatus) {
      const claim = document.createElement('span');
      claim.className = 'pill success';
      claim.textContent = `Claim ${readableOpportunityValue(report.myClaimStatus)}`;
      actions.append(claim);
    }
    const view = document.createElement('button');
    view.type = 'button';
    view.className = 'secondary-button';
    view.textContent = 'View secure details';
    view.addEventListener('click', () => openLostFoundDetails(report.id));
    actions.append(view);
    card.append(visual, details, actions);
    lostFoundCatalogue.append(card);
  });
  if (!reports.length) {
    lostFoundCatalogue.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#search"/></svg><h3>No matching reports</h3><p>Try another search or filter. New student reports remain private until moderation.</p></div>';
  }
}

async function loadLostFound() {
  if (!lostFoundCatalogue) return;
  try {
    const [catalogueResponse, mineResponse, claimsResponse] = await Promise.all([
      fetch('/api/lost-found', { headers: { Accept: 'application/json' } }),
      fetch('/api/lost-found/mine', { headers: { Accept: 'application/json' } }),
      fetch('/api/lost-found/claims', { headers: { Accept: 'application/json' } })
    ]);
    if ([catalogueResponse.status, mineResponse.status, claimsResponse.status].some((status) => status === 401 || status === 403)) {
      lostFoundCatalogue.innerHTML = '<div class="empty-state compact"><h3>Sign in to use Lost and Found</h3><p>Reports and private claims are scoped to your university account.</p></div>';
      return;
    }
    if (!catalogueResponse.ok || !mineResponse.ok || !claimsResponse.ok) throw new Error();
    loadedLostFoundReports = await catalogueResponse.json();
    myLostFoundReports = await mineResponse.json();
    myLostFoundClaims = await claimsResponse.json();
    renderLostFoundReports();
  } catch {
    lostFoundCatalogue.innerHTML = '<div class="empty-state compact"><h3>Lost and Found is temporarily unavailable</h3><p>Please try again shortly.</p></div>';
  }
}

function openLostFoundDetails(id) {
  const report = lostFoundReportById(id);
  if (!report) return;
  activeLostFoundReportId = id;
  const moderationLabel = {
    APPROVED: 'MODERATED',
    PENDING: 'AWAITING MODERATION',
    REJECTED: 'NOT PUBLISHED'
  }[report.moderationStatus] || 'REVIEW STATUS UNAVAILABLE';
  document.querySelector('#lostFoundDetailType').textContent = `${readableOpportunityValue(report.reportType)} ITEM · ${moderationLabel}`;
  document.querySelector('#lostFoundDetailTitle').textContent = report.itemName;
  document.querySelector('#lostFoundDetailDescription').textContent = report.publicDescription;
  document.querySelector('#lostFoundDetailCategory').textContent = readableOpportunityValue(report.category);
  const detailStatus = document.querySelector('#lostFoundDetailStatus');
  detailStatus.textContent = report.moderationStatus === 'APPROVED'
    ? readableOpportunityValue(report.reportStatus)
    : readableOpportunityValue(report.moderationStatus);
  detailStatus.className = report.moderationStatus === 'APPROVED' ? 'pill success' : 'pill warning';
  document.querySelector('#lostFoundDetailColour').textContent = report.colour || 'Not stated';
  document.querySelector('#lostFoundDetailLocation').textContent = report.lastSeenLocation;
  document.querySelector('#lostFoundDetailDate').textContent = lostFoundDate(report.occurredOn);
  document.querySelector('#lostFoundDetailCustody').textContent = report.reportType === 'FOUND'
    ? readableOpportunityValue(report.custodyStatus) : 'Not applicable';
  const photo = document.querySelector('#lostFoundDetailPhoto');
  photo.replaceChildren();
  if (report.hasPhoto) {
    const image = document.createElement('img');
    image.src = report.photoUrl;
    image.alt = `Photograph provided with the ${report.itemName} report`;
    photo.append(image);
  } else {
    const [iconName] = lostFoundPresentation(report.category);
    photo.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${iconName}"/></svg>`;
  }
  const claimPanel = document.querySelector('#lostFoundClaimPanel');
  const canClaim = report.reportType === 'FOUND' && !report.mine && report.reportStatus === 'OPEN';
  claimPanel.hidden = !canClaim;
  const claimForm = document.querySelector('#lostFoundClaimForm');
  claimForm.hidden = Boolean(report.myClaimStatus);
  const claimStatus = document.querySelector('#lostFoundClaimStatus');
  claimStatus.textContent = report.myClaimStatus
    ? `Your private claim is ${readableOpportunityValue(report.myClaimStatus).toLowerCase()}.`
    : '';
  document.querySelector('#lostFoundClaimEvidence').value = '';
  const reviewNote = document.querySelector('#lostFoundReviewNote');
  reviewNote.hidden = !report.mine || !report.myReviewNote;
  document.querySelector('#lostFoundReviewNoteText').textContent = report.myReviewNote || '';
  const withdraw = document.querySelector('#lostFoundWithdrawButton');
  withdraw.hidden = !report.mine || ['RETURNED', 'WITHDRAWN'].includes(report.reportStatus);
  openModal(lostFoundDetailDialog);
}

function configureLostFoundCustody() {
  const type = document.querySelector('#lostFoundReportType').value;
  const label = document.querySelector('#lostFoundCustodyLabel');
  const select = document.querySelector('#lostFoundCustody');
  label.hidden = type === 'LOST';
  select.disabled = type === 'LOST';
  if (type === 'LOST') select.value = 'WITH_REPORTER';
}

document.querySelector('#lostFoundReportButton')?.addEventListener('click', () => {
  const form = document.querySelector('#lostFoundReportForm');
  form.reset();
  document.querySelector('#lostFoundOccurredOn').value = new Date().toISOString().slice(0, 10);
  document.querySelector('#lostFoundOccurredOn').max = new Date().toISOString().slice(0, 10);
  configureLostFoundCustody();
  openModal(lostFoundReportDialog);
});
document.querySelector('#lostFoundReportType')?.addEventListener('change', configureLostFoundCustody);
document.querySelector('#lostFoundReportClose')?.addEventListener('click', () => closeModal(lostFoundReportDialog));
document.querySelector('#lostFoundReportCancel')?.addEventListener('click', () => closeModal(lostFoundReportDialog));
document.querySelector('#lostFoundDetailClose')?.addEventListener('click', () => closeModal(lostFoundDetailDialog));
document.querySelector('#lostFoundDetailDone')?.addEventListener('click', () => closeModal(lostFoundDetailDialog));

document.querySelectorAll('[data-lost-found-type]').forEach((button) => {
  button.addEventListener('click', () => {
    lostFoundView = 'catalogue';
    selectedLostFoundType = button.dataset.lostFoundType;
    renderLostFoundReports();
  });
});
document.querySelector('[data-lost-found-view="mine"]')?.addEventListener('click', () => {
  lostFoundView = 'mine';
  selectedLostFoundType = '';
  renderLostFoundReports();
});
document.querySelector('[data-lost-found-view="claims"]')?.addEventListener('click', () => {
  lostFoundView = 'claims';
  selectedLostFoundType = '';
  renderLostFoundReports();
});
lostFoundSearch?.addEventListener('input', renderLostFoundReports);
lostFoundCategoryFilter?.addEventListener('change', renderLostFoundReports);

document.querySelector('#lostFoundReportForm')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const button = document.querySelector('#lostFoundReportSubmit');
  const photo = document.querySelector('#lostFoundPhoto').files[0];
  if (photo && (!['image/png', 'image/jpeg'].includes(photo.type) || photo.size > 5 * 1024 * 1024)) {
    showToast('Choose a PNG or JPEG photograph no larger than 5 MB.');
    return;
  }
  button.disabled = true;
  button.textContent = 'Submitting…';
  const type = document.querySelector('#lostFoundReportType').value;
  try {
    const report = await socialPost('/api/lost-found', {
      reportType: type,
      category: document.querySelector('#lostFoundReportCategory').value,
      itemName: document.querySelector('#lostFoundItemName').value.trim(),
      colour: document.querySelector('#lostFoundColour').value.trim(),
      publicDescription: document.querySelector('#lostFoundPublicDescription').value.trim(),
      privateIdentifyingDetails: document.querySelector('#lostFoundPrivateDetails').value.trim(),
      lastSeenLocation: document.querySelector('#lostFoundLocation').value.trim(),
      occurredOn: document.querySelector('#lostFoundOccurredOn').value,
      custodyStatus: type === 'LOST' ? 'NOT_APPLICABLE' : document.querySelector('#lostFoundCustody').value
    });
    let photoWarning = '';
    if (photo) {
      try {
        await securePhotoUpload(`/api/lost-found/${report.id}/photo`, photo);
      } catch (error) {
        photoWarning = ` The optional photograph was not added: ${error.message}`;
      }
    }
    closeModal(lostFoundReportDialog);
    lostFoundView = 'mine';
    selectedLostFoundType = '';
    lostFoundSearch.value = '';
    lostFoundCategoryFilter.value = '';
    document.querySelectorAll('#lostFoundFilters .filter-chip').forEach((item) => item.classList.remove('active'));
    document.querySelector('[data-lost-found-view="mine"]').classList.add('active');
    await loadLostFound();
    showToast(`Your report was saved privately and submitted for moderation.${photoWarning}`);
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
    button.textContent = 'Submit for moderation';
  }
});

document.querySelector('#lostFoundClaimForm')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!activeLostFoundReportId) return;
  const button = event.currentTarget.querySelector('button');
  button.disabled = true;
  try {
    await socialPost(`/api/lost-found/${activeLostFoundReportId}/claims`, {
      privateOwnershipEvidence: document.querySelector('#lostFoundClaimEvidence').value.trim()
    });
    await loadLostFound();
    openLostFoundDetails(activeLostFoundReportId);
    showToast('Your ownership evidence was submitted privately.');
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
});

document.querySelector('#lostFoundWithdrawButton')?.addEventListener('click', async (event) => {
  if (!activeLostFoundReportId) return;
  event.currentTarget.disabled = true;
  try {
    await socialPost(`/api/lost-found/${activeLostFoundReportId}/withdraw`);
    closeModal(lostFoundDetailDialog);
    await loadLostFound();
    showToast('Your report was withdrawn and removed from the public catalogue.');
  } catch (error) {
    showToast(error.message);
  } finally {
    event.currentTarget.disabled = false;
  }
});

function renderStaffReview() {
  if (!staffReviewList) return;
  staffReviewList.replaceChildren();
  document.querySelector('#staffReviewCount').textContent = `${staffPendingReports.length} awaiting review`;
  if (!staffPendingReports.length) {
    staffReviewList.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#check"/></svg><h3>Review queue is clear</h3><p>New reports will appear here after a student submits them.</p></div>';
    return;
  }
  staffPendingReports.forEach((report) => {
    const card = document.createElement('article');
    card.className = 'staff-review-card';
    const icon = document.createElement('span');
    icon.className = 'view-icon green';
    icon.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#shield"/></svg>';
    const summary = document.createElement('div');
    summary.className = 'staff-review-summary';
    const eyebrow = document.createElement('span');
    eyebrow.className = 'eyebrow';
    eyebrow.textContent = `${readableOpportunityValue(report.reportType)} · ${readableOpportunityValue(report.category)}`;
    const title = document.createElement('h3');
    title.textContent = report.itemName;
    const details = document.createElement('p');
    details.textContent = `${report.reporterName} · ${report.lastSeenLocation} · ${lostFoundDate(report.occurredOn)}`;
    const copy = document.createElement('small');
    copy.textContent = report.publicDescription;
    summary.append(eyebrow, title, details, copy);
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'secondary-button';
    button.textContent = 'Review securely';
    button.addEventListener('click', () => openStaffReview(report.id));
    card.append(icon, summary, button);
    staffReviewList.append(card);
  });
}

async function loadStaffReview() {
  if (!staffReviewAccess || !staffReviewList) return;
  try {
    const response = await fetch('/api/lost-found/staff/reports', { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error();
    staffPendingReports = await response.json();
    renderStaffReview();
  } catch {
    staffReviewList.innerHTML = '<div class="empty-state compact"><h3>Review queue unavailable</h3><p>Refresh or sign in with the fictional staff account.</p></div>';
  }
}

function openStaffReview(id) {
  const report = staffPendingReports.find((item) => item.id === id);
  if (!report) return;
  activeStaffReportId = id;
  document.querySelector('#staffReviewForm').reset();
  document.querySelector('#staffReviewDialogTitle').textContent = report.itemName;
  document.querySelector('#staffReviewReporter').textContent = `Reported by ${report.reporterName} · ${readableOpportunityValue(report.reportType)} item`;
  document.querySelector('#staffReviewPublic').textContent = report.publicDescription;
  document.querySelector('#staffReviewPrivate').textContent = report.privateIdentifyingDetails;
  document.querySelector('#staffReviewLocation').textContent = `${report.lastSeenLocation} · ${lostFoundDate(report.occurredOn)} · Custody: ${readableOpportunityValue(report.custodyStatus)}`;
  const visual = document.querySelector('#staffReviewPhoto');
  visual.replaceChildren();
  if (report.hasPhoto) {
    const image = document.createElement('img');
    image.src = report.photoUrl;
    image.alt = `Private item photograph for ${report.itemName}`;
    visual.append(image);
  } else {
    visual.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#camera"/></svg>';
  }
  openModal(staffReviewDialog);
}

document.querySelector('#staffReviewClose')?.addEventListener('click', () => closeModal(staffReviewDialog));
document.querySelector('#staffReviewCancel')?.addEventListener('click', () => closeModal(staffReviewDialog));
document.querySelector('#staffReviewRefresh')?.addEventListener('click', () => {
  if (staffReviewAccess) void loadStaffReview();
});
document.querySelector('#staffReviewForm')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!activeStaffReportId || !staffReviewAccess) return;
  const button = document.querySelector('#staffReviewSubmit');
  const choice = document.querySelector('input[name="staffDecision"]:checked');
  if (!choice) return;
  button.disabled = true;
  button.textContent = 'Recording decision…';
  try {
    const decision = choice.value;
    await socialPost(`/api/lost-found/staff/reports/${activeStaffReportId}/decision`, {
      decision,
      reason: document.querySelector('#staffReviewReason').value.trim()
    });
    closeModal(staffReviewDialog);
    await Promise.all([loadStaffReview(), loadLostFound()]);
    showToast(decision === 'APPROVED' ? 'Report approved and published.' : 'Report rejected; the reporter has a private review note.');
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
    button.textContent = 'Confirm decision';
  }
});

bookButton?.addEventListener('click', () => {
  if (!selectedBookingSlotId) return;
  void bookServiceSlot(selectedBookingSlotId, bookingName.textContent, bookButton);
});

let activeAssistantMode = 'GENERAL';
let assistantStatusLoaded = false;
let activeAssistantConversationId = null;
let assistantConversationsLoaded = false;
const assistantModeLabels = new Map([
  ['GENERAL', 'Smart help · reviewed local guidance'],
  ['UNIVERSITY_RESEARCH', 'University research · planning and source guidance'],
  ['STUDY_COACH', 'Study coach · explanations, practice and feedback'],
  ['CODE_LAB', 'Code lab · design, debug and test programs'],
  ['IMAGE_STUDIO', 'Image studio · private no-cost illustration creator']
]);
const assistantPlaceholders = new Map([
  ['GENERAL', 'Ask UniCircle anything…'],
  ['UNIVERSITY_RESEARCH', 'Ask about a university, course, fees, entry requirements or student life…'],
  ['STUDY_COACH', 'What would you like to understand or practise?'],
  ['CODE_LAB', 'Describe the program, error or code you want help with…'],
  ['IMAGE_STUDIO', 'Describe the original image you want to create…']
]);

async function answerQuestion(question) {
  const response = document.querySelector('#assistantResponse');
  response.textContent = 'Thinking…';
  try {
    const result = await socialPost('/api/assistant/ask', { message: question, mode: activeAssistantMode, allowPublicLookup: false });
    response.textContent = result.answer;
  } catch (error) {
    response.textContent = error.message;
  }
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

function appendAssistantBubble(responsePanel, kind, text) {
  const bubble = document.createElement('div');
  bubble.className = `bubble ${kind}`;
  bubble.textContent = text;
  responsePanel.append(bubble);
  responsePanel.scrollTop = responsePanel.scrollHeight;
  return bubble;
}

function appendAssistantResult(responsePanel, result) {
  appendAssistantBubble(responsePanel, 'received', result.answer);
  if (result.imageDataUrl) {
    const figure = document.createElement('figure');
    figure.className = 'assistant-generated-image';
    const image = document.createElement('img');
    image.src = result.imageDataUrl;
    image.alt = 'Original image generated by Ask UniCircle';
    const download = document.createElement('a');
    download.href = result.imageDataUrl;
    const vectorImage = result.imageDataUrl.startsWith('data:image/svg+xml');
    download.download = vectorImage ? 'unicircle-illustration.svg' : 'unicircle-generated-image.png';
    download.textContent = vectorImage ? 'Download editable SVG' : 'Download image';
    figure.append(image, download);
    responsePanel.append(figure);
  }
  if (result.sources?.length) {
    const sourceSection = document.createElement('section');
    sourceSection.className = 'assistant-sources';
    const heading = document.createElement('strong');
    heading.textContent = 'Sources';
    const list = document.createElement('ol');
    result.sources.forEach((source) => {
      const item = document.createElement('li');
      const link = document.createElement('a');
      link.href = source.url;
      link.target = '_blank';
      link.rel = 'noopener noreferrer';
      link.textContent = source.title || new URL(source.url).hostname;
      item.append(link);
      list.append(item);
    });
    sourceSection.append(heading, list);
    responsePanel.append(sourceSection);
  }
  if (result.notice) {
    const notice = document.createElement('p');
    notice.className = 'assistant-answer-notice';
    notice.textContent = result.notice;
    responsePanel.append(notice);
  }
  responsePanel.scrollTop = responsePanel.scrollHeight;
}

async function answerPageQuestion(question) {
  const responsePanel = document.querySelector('#pageAssistantResponse');
  responsePanel.querySelector('.assistant-welcome')?.remove();
  appendAssistantBubble(responsePanel, 'sent', question);
  const thinking = appendAssistantBubble(responsePanel, 'received assistant-thinking', 'Thinking through your question…');
  const form = document.querySelector('#pageAssistantForm');
  const submit = form?.querySelector('button');
  if (submit) submit.disabled = true;
  try {
    if (!activeAssistantConversationId) {
      const created = await socialPost('/api/assistant/conversations', {
        title: 'New chat', mode: activeAssistantMode
      });
      activeAssistantConversationId = created.conversation.id;
    }
    const allowPublicLookup = document.querySelector('#assistantPublicLookup')?.checked === true;
    const detail = await socialPost(`/api/assistant/conversations/${activeAssistantConversationId}/messages`, {
      message: question, mode: activeAssistantMode, allowPublicLookup
    });
    renderAssistantConversation(detail);
    await loadAssistantConversationList(true);
  } catch (error) {
    thinking.textContent = error.message;
    thinking.classList.remove('assistant-thinking');
  } finally {
    if (submit) submit.disabled = false;
  }
}

function assistantWelcome() {
  return '<div class="assistant-welcome"><span><svg class="icon" aria-hidden="true"><use href="#sparkles"/></svg></span><h3>How can I help?</h3><p>Ask a question, research a university, work through a topic, debug code or describe an image to create.</p></div>';
}

function renderAssistantConversation(detail) {
  const responsePanel = document.querySelector('#pageAssistantResponse');
  const title = document.querySelector('#assistantChatTitle');
  responsePanel.replaceChildren();
  title.textContent = detail.conversation.title;
  activeAssistantConversationId = detail.conversation.id;
  selectAssistantMode(detail.conversation.mode);
  if (!detail.messages.length) {
    responsePanel.innerHTML = assistantWelcome();
  } else {
    detail.messages.forEach((message) => {
      if (message.role === 'USER') appendAssistantBubble(responsePanel, 'sent', message.content);
      else appendAssistantResult(responsePanel, {
        answer: message.content,
        imageDataUrl: message.imageDataUrl,
        sources: message.sources,
        notice: message.notice
      });
    });
  }
  responsePanel.scrollTop = responsePanel.scrollHeight;
  document.querySelectorAll('.assistant-conversation-button').forEach((button) => {
    button.classList.toggle('active', button.dataset.conversationId === activeAssistantConversationId);
  });
}

async function fetchAssistantJson(url) {
  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('Your Ask UniCircle chats could not be loaded. Please try again.');
  return response.json();
}

function renderAssistantConversationList(conversations) {
  const list = document.querySelector('#assistantConversationList');
  const status = document.querySelector('#assistantConversationStatus');
  list.replaceChildren();
  status.hidden = conversations.length > 0;
  status.textContent = conversations.length ? '' : 'No saved chats yet. Start a new conversation when you are ready.';
  conversations.forEach((conversation) => {
    const item = document.createElement('li');
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'assistant-conversation-button';
    button.dataset.conversationId = conversation.id;
    button.classList.toggle('active', conversation.id === activeAssistantConversationId);
    const title = document.createElement('strong');
    title.textContent = conversation.title;
    const preview = document.createElement('span');
    preview.textContent = conversation.preview || assistantModeLabels.get(conversation.mode);
    const time = document.createElement('time');
    time.dateTime = conversation.updatedAt;
    time.textContent = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' })
      .format(new Date(conversation.updatedAt));
    button.append(title, preview, time);
    button.addEventListener('click', () => void openAssistantConversation(conversation.id));
    item.append(button);
    list.append(item);
  });
}

async function loadAssistantConversationList(force = false) {
  if (assistantConversationsLoaded && !force) return [];
  const conversations = await fetchAssistantJson('/api/assistant/conversations');
  assistantConversationsLoaded = true;
  renderAssistantConversationList(conversations);
  return conversations;
}

async function openAssistantConversation(conversationId) {
  const status = document.querySelector('#assistantConversationStatus');
  status.hidden = false;
  status.textContent = 'Opening conversation…';
  try {
    const detail = await fetchAssistantJson(`/api/assistant/conversations/${conversationId}`);
    status.hidden = true;
    renderAssistantConversation(detail);
  } catch (error) {
    status.hidden = false;
    status.textContent = error.message;
  }
}

async function createAssistantConversation() {
  const button = document.querySelector('#assistantNewChat');
  button.disabled = true;
  try {
    const detail = await socialPost('/api/assistant/conversations', {
      title: 'New chat', mode: activeAssistantMode
    });
    assistantConversationsLoaded = false;
    renderAssistantConversation(detail);
    await loadAssistantConversationList(true);
    document.querySelector('#pageAssistantInput')?.focus();
  } catch (error) {
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
}

async function loadAssistantWorkspace() {
  try {
    const conversations = await loadAssistantConversationList(true);
    if (activeAssistantConversationId) {
      await openAssistantConversation(activeAssistantConversationId);
    } else if (conversations.length) {
      await openAssistantConversation(conversations[0].id);
    }
  } catch (error) {
    const status = document.querySelector('#assistantConversationStatus');
    status.hidden = false;
    status.textContent = error.message;
  }
}

document.querySelector('#assistantNewChat')?.addEventListener('click', () => void createAssistantConversation());

async function loadAssistantStatus() {
  if (assistantStatusLoaded) return;
  const badge = document.querySelector('#assistantStatusBadge');
  const indicator = document.querySelector('#assistantLiveIndicator');
  try {
    const response = await fetch('/api/assistant/status', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      badge.textContent = 'Sign in required';
      indicator.innerHTML = '<i></i>Sign in';
      return;
    }
    if (!response.ok) throw new Error();
    const status = await response.json();
    assistantStatusLoaded = true;
    const advanced = status.level === 'ADVANCED';
    badge.textContent = advanced ? 'Advanced AI workspace' : 'Knowledge assistant active';
    indicator.classList.toggle('offline', false);
    indicator.innerHTML = `<i></i>${advanced ? 'Advanced provider' : 'Local guidance'}`;
  } catch {
    badge.textContent = 'Service unavailable';
    indicator.classList.add('offline');
    indicator.innerHTML = '<i></i>Offline';
  }
}

document.querySelectorAll('[data-page-question]').forEach((button) => {
  button.addEventListener('click', () => {
    const mode = button.dataset.questionMode;
    if (mode) selectAssistantMode(mode);
    void answerPageQuestion(button.dataset.pageQuestion);
  });
});

function selectAssistantMode(mode) {
  activeAssistantMode = mode;
  document.querySelectorAll('[data-assistant-mode]').forEach((button) => {
    const active = button.dataset.assistantMode === mode;
    button.classList.toggle('active', active);
    button.setAttribute('aria-pressed', String(active));
  });
  const label = document.querySelector('#assistantModeLabel');
  const input = document.querySelector('#pageAssistantInput');
  const publicLookup = document.querySelector('#assistantPublicLookup');
  if (label) label.textContent = assistantModeLabels.get(mode);
  if (input) input.placeholder = assistantPlaceholders.get(mode);
  if (mode === 'IMAGE_STUDIO' && publicLookup) publicLookup.checked = false;
}

document.querySelectorAll('[data-assistant-mode]').forEach((button) => {
  button.addEventListener('click', () => selectAssistantMode(button.dataset.assistantMode));
});

document.querySelector('#pageAssistantForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const input = document.querySelector('#pageAssistantInput');
  if (!input.value.trim()) return;
  const question = input.value.trim();
  input.value = '';
  void answerPageQuestion(question);
});

const profileDialog = document.querySelector('#profileDialog');
const profileForm = document.querySelector('#profileForm');
const profileFields = document.querySelector('#profileFields');
const profileStatus = document.querySelector('#profileStatus');
const profilePhotoInput = document.querySelector('#profilePhoto');
const profilePhotoPreview = document.querySelector('#profilePhotoPreview');
const universityContext = document.querySelector('#universityContext');
const academicModules = document.querySelector('#academicModules');
const academicCoursemates = document.querySelector('#academicCoursemates');
const profileSignIn = document.querySelector('#profileSignIn');
const profileSignOut = document.querySelector('#profileSignOut');
const messagesAccountBadge = document.querySelector('#messagesAccountBadge');
const studentDirectoryIdentity = document.querySelector('#studentDirectoryIdentity');

function setProfileStatus(message, state = '') {
  profileStatus.textContent = message;
  profileStatus.className = `profile-status${state ? ` ${state}` : ''}`;
}

function showSignedOutActions() {
  renderSignedOutAccountStates();
  profileSignIn.hidden = false;
  profileSignOut.hidden = true;
  if (messagesAccountBadge) messagesAccountBadge.hidden = true;
  if (studentDirectoryIdentity) {
    studentDirectoryIdentity.textContent = 'Sign in to find other student accounts. Your own account is never listed as a friend suggestion.';
  }
  staffReviewAccess = false;
  staffReviewNav.hidden = true;
  adminAccess = false;
  adminNav.hidden = true;
  if (announcementPublisherPanel) announcementPublisherPanel.hidden = true;
  clearInterval(campusRefreshTimer);
}

function showSignedInActions() {
  profileSignIn.hidden = true;
  profileSignOut.hidden = false;
}

function initialsFor(name) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || 'U';
}

function showMemberPhoto(avatar, accountId) {
  const photo = new Image();
  photo.onload = () => {
    avatar.style.backgroundImage = `url("${photo.src}")`;
    avatar.style.backgroundSize = 'cover';
    avatar.style.backgroundPosition = 'center';
    avatar.textContent = '';
  };
  photo.src = `/api/profile/${encodeURIComponent(accountId)}/photo`;
}

function updateVisibleIdentity(profile) {
  document.querySelectorAll('[data-current-user-name]').forEach((node) => { node.textContent = profile.displayName; });
  document.querySelectorAll('[data-current-user-course]').forEach((node) => { node.textContent = profile.courseName || profile.role; });
  document.querySelectorAll('[data-current-user-first-name]').forEach((node) => {
    node.textContent = profile.displayName.trim().split(/\s+/)[0] || 'Student';
  });
  document.querySelectorAll('[data-current-university]').forEach((node) => { node.textContent = profile.university; });
  document.querySelectorAll('[data-current-account-detail]').forEach((node) => { node.textContent = profile.email; });
  document.querySelectorAll('.avatar-michael').forEach((avatar) => {
    avatar.textContent = initialsFor(profile.displayName);
    if (profile.hasPhoto) {
      avatar.style.backgroundImage = `url("${profile.photoUrl}?v=${Date.now()}")`;
      avatar.style.backgroundSize = 'cover';
      avatar.style.backgroundPosition = 'center';
      avatar.textContent = '';
    }
  });
}

function populateProfile(profile) {
  renderSignedInAccountStates();
  staffReviewAccess = ['STAFF', 'ADMIN'].includes(profile.role);
  adminAccess = profile.role === 'ADMIN';
  staffReviewNav.hidden = !staffReviewAccess;
  adminNav.hidden = !adminAccess;
  if (announcementPublisherPanel) announcementPublisherPanel.hidden = !staffReviewAccess;
  document.querySelector('#profileDisplayName').value = profile.displayName || '';
  document.querySelector('#profileCourse').value = profile.courseName || '';
  document.querySelector('#profileYear').value = profile.yearOfStudy || '';
  document.querySelector('#profileVisibility').value = profile.visibility;
  document.querySelector('#profileBiography').value = profile.biography || '';
  document.querySelector('#profileUniversity').textContent = profile.university;
  document.querySelector('#profileEmail').textContent = profile.email;
  document.querySelector('#profileIdentity').hidden = false;
  profileFields.disabled = false;
  showSignedInActions();
  updateVisibleIdentity(profile);
  if (messagesAccountBadge) messagesAccountBadge.hidden = false;
  if (studentDirectoryIdentity) {
    studentDirectoryIdentity.textContent = `You are signed in as ${profile.displayName}. Your own account is not listed here because you cannot connect to yourself. Use a different account on another phone, browser or laptop to test friend requests.`;
  }
  if (pilotMode) {
    document.querySelector('#universityAccountButton .verified-dot').hidden = true;
  }
  connectSocialRealtime();
  clearInterval(campusRefreshTimer);
  campusRefreshTimer = setInterval(refreshActiveCampusData, 10000);
  clearInterval(meetingRoomRefreshTimer);
  meetingRoomRefreshTimer = setInterval(() => {
    if (!document.hidden && document.body.dataset.activePage === 'live-classes'
        && selectedMeetingId && currentMeetingRoom?.meeting?.joined) {
      void loadMeetingRoom(selectedMeetingId, { quiet: true });
    }
  }, 3000);
  void Promise.all([
    loadNotifications(),
    refreshConversationSummary(),
    loadCampusEvents(),
    loadCampusServices(),
    loadCampusBookings(),
    loadCampusSchedule(),
    loadOpportunities(),
    loadLostFound(),
    loadAnnouncements(activeAnnouncementCategory),
    loadMeetings(),
    loadSupportCases(),
    loadCircles(),
    ...(adminAccess ? [loadAdminWorkspace()] : []),
    ...(staffReviewAccess ? [loadStaffReview()] : [])
  ]);
}

function refreshActiveCampusData() {
  if (!signedIn || document.hidden) return;
  void loadNotifications();
  const page = document.body.dataset.activePage;
  if (page === 'campus') void Promise.all([loadAnnouncements(activeAnnouncementCategory), loadCampusSchedule(), loadCampusEvents(), loadOpportunities()]);
  else if (page === 'today') void loadCampusSchedule();
  else if (page === 'updates') void loadAnnouncements(activeAnnouncementCategory);
  else if (page === 'events') void loadCampusEvents();
  else if (page === 'services') void Promise.all([loadCampusServices(), loadCampusBookings()]);
  else if (page === 'opportunities') void loadOpportunities();
  else if (page === 'live-classes') void loadMeetings();
}

document.addEventListener('visibilitychange', () => {
  if (!document.hidden) refreshActiveCampusData();
});

function clearUniversityContext() {
  universityContext.hidden = true;
  academicModules.replaceChildren();
  academicCoursemates.replaceChildren();
}

function connectionLabel(state) {
  if (state === 'CONNECTED') return 'Connected';
  if (state === 'PENDING') return 'Request pending';
  return 'Available to connect';
}

function renderUniversityContext(context) {
  document.querySelector('#academicSource').textContent = context.dataSource === 'FICTIONAL_DEVELOPMENT_FIXTURE'
    ? 'Fictional sandbox'
    : context.dataSource === 'SELF_DECLARED_PILOT_PROFILE'
      ? 'Pilot profile'
      : 'Approved university source';
  document.querySelector('#academicProgramme').textContent = `${context.student.qualification} ${context.student.programmeTitle}`;
  document.querySelector('#academicProgrammeMeta').textContent = `${context.student.programmeCode} · Year ${context.student.yearOfStudy}`;
  document.querySelector('#academicNotice').textContent = context.dataNotice;

  context.modules.forEach((module) => {
    const item = document.createElement('span');
    item.className = 'module-chip';
    item.textContent = `${module.code} · ${module.title}`;
    academicModules.append(item);
  });

  context.coursemates.forEach((coursemate) => {
    const item = document.createElement('article');
    item.className = 'coursemate-item';
    const avatar = document.createElement('span');
    avatar.className = 'coursemate-avatar';
    avatar.textContent = initialsFor(coursemate.displayName);
    const details = document.createElement('span');
    const name = document.createElement('strong');
    name.textContent = coursemate.displayName;
    const shared = document.createElement('small');
    shared.textContent = `${coursemate.sharedModuleCodes.length} shared modules · ${connectionLabel(coursemate.connectionState)}`;
    details.append(name, shared);
    const actions = document.createElement('span');
    actions.className = 'coursemate-actions';
    appendConnectionActions(actions, coursemate);
    item.append(avatar, details, actions);
    academicCoursemates.append(item);
  });

  if (!context.modules.length) academicModules.textContent = 'No current modules were supplied.';
  if (!context.coursemates.length) academicCoursemates.textContent = 'No permitted coursemates were supplied.';
  universityContext.hidden = false;
}

async function loadUniversityContext() {
  clearUniversityContext();
  try {
    const response = await fetch('/api/campus/me/context', { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('Your university academic-data connection is not available yet.');
    renderUniversityContext(await response.json());
  } catch (error) {
    document.querySelector('#academicSource').textContent = 'Source unavailable';
    document.querySelector('#academicProgramme').textContent = 'University data is not connected';
    document.querySelector('#academicProgrammeMeta').textContent = 'Your editable UniCircle profile remains available.';
    document.querySelector('#academicNotice').textContent = error.message;
    academicModules.textContent = 'No verified module data is available.';
    academicCoursemates.textContent = 'No verified coursemate data is available.';
    universityContext.hidden = false;
  }
}

async function loadProfile(redirectWhenSignedOut = false) {
  profileFields.disabled = true;
  clearUniversityContext();
  setProfileStatus('Checking your secure session…');
  try {
    const response = await fetch('/api/profile/me', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      showSignedOutActions();
      setProfileStatus(pilotMode ? 'Sign in with your pilot account to continue.'
        : 'This profile is protected. Sign in with the fictional student account to test the complete experience safely.', 'error');
      if (redirectWhenSignedOut) window.location.assign('/sign-in');
      return false;
    }
    if (!response.ok) throw new Error('Your profile is temporarily unavailable.');
    const profile = await response.json();
    populateProfile(profile);
    await loadUniversityContext();
    setProfileStatus(pilotMode ? 'Your pilot profile is ready to edit.' : 'Your verified profile is ready to edit.', 'success');
    return true;
  } catch (error) {
    showSignedOutActions();
    setProfileStatus(error.message || 'Your profile could not be loaded.', 'error');
    showToast(error.message || 'Your profile could not be loaded.');
    return false;
  }
}

async function csrfToken() {
  const response = await fetch('/api/security/csrf', { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('The security token could not be prepared. Please try again.');
  const responseBody = await response.text();
  return responseBody ? JSON.parse(responseBody) : null;
}

async function socialPost(url, body) {
  const csrf = await csrfToken();
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      [csrf.headerName]: csrf.token,
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' })
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) })
  });
  const responseBody = await response.text();
  let responseData = null;
  if (responseBody) {
    try { responseData = JSON.parse(responseBody); } catch { /* Non-JSON error pages use the fallback below. */ }
  }
  if (!response.ok) {
    const fieldMessage = responseData?.fields && Object.values(responseData.fields)[0];
    const serverMessage = responseData?.detail || responseData?.message || fieldMessage;
    if (response.status === 400) throw new Error(serverMessage || 'Check the information supplied and try again.');
    if (response.status === 401) throw new Error('Sign in to continue.');
    if (response.status === 403) throw new Error('This action is not permitted for your account.');
    if (response.status === 409) throw new Error('This request has already been handled or is no longer available.');
    if (response.status === 429) throw new Error('You have reached the request limit. Please wait a few minutes and try again.');
    if (response.status === 502) throw new Error('The connected service is temporarily unavailable. Please try again shortly.');
    throw new Error('The request could not be completed. Please refresh and try again.');
  }
  return responseData;
}

async function socialPatch(url, body) {
  const csrf = await csrfToken();
  const response = await fetch(url, {
    method: 'PATCH',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token
    },
    body: JSON.stringify(body)
  });
  if (!response.ok) {
    if (response.status === 400) throw new Error('Check the information supplied and try again.');
    if (response.status === 401) throw new Error('Sign in to continue.');
    if (response.status === 403) throw new Error('This action requires Test Campus administrator access.');
    if (response.status === 409) throw new Error('This change conflicts with the current record. Refresh and try again.');
    throw new Error('The change could not be saved. Please refresh and try again.');
  }
  const responseBody = await response.text();
  return responseBody ? JSON.parse(responseBody) : null;
}

async function securePhotoUpload(url, photo) {
  const csrf = await csrfToken();
  const form = new FormData();
  form.append('photo', photo);
  const response = await fetch(url, {
    method: 'POST',
    headers: { Accept: 'application/json', [csrf.headerName]: csrf.token },
    body: form
  });
  if (!response.ok) {
    if (response.status === 400) throw new Error('Use a PNG or JPEG photograph between 240 and 4096 pixels, no larger than 5 MB.');
    if (response.status === 401 || response.status === 403) throw new Error('Sign in to upload an item photograph.');
    throw new Error('The photograph could not be uploaded safely.');
  }
  return response.json();
}

function connectionButton(label, className, handler, disabled = false) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = `connection-action ${className}`.trim();
  button.textContent = label;
  button.disabled = disabled;
  if (handler) button.addEventListener('click', handler);
  return button;
}

async function runConnectionAction(button, action, successMessage) {
  button.disabled = true;
  const original = button.textContent;
  button.textContent = 'Working…';
  try {
    await action();
    showToast(successMessage);
    await Promise.all([loadUniversityContext(), loadStudentDirectory()]);
  } catch (error) {
    showToast(error.message || 'The connection could not be updated.');
    button.disabled = false;
    button.textContent = original;
  }
}

function appendConnectionActions(container, person) {
  if (person.connectionStatus === 'BLOCKED') {
    container.append(connectionButton('Blocked', 'danger', null, true));
    return;
  }
  if (person.connectionState === 'CONNECTED' || person.connectionStatus === 'ACCEPTED') {
    container.append(connectionButton('Message', 'primary', () => startConversation(person.accountId)));
    return;
  }
  const pending = person.connectionState === 'PENDING' || person.connectionStatus === 'PENDING';
  if (pending && person.connectionDirection === 'INCOMING') {
    const accept = connectionButton('Accept', 'primary');
    accept.addEventListener('click', () => runConnectionAction(
      accept,
      () => socialPost(`/api/social/connections/${person.connectionId}/accept`),
      `You and ${person.displayName} are now connected.`
    ));
    const decline = connectionButton('Decline', 'danger');
    decline.addEventListener('click', () => runConnectionAction(
      decline,
      () => socialPost(`/api/social/connections/${person.connectionId}/decline`),
      'Connection request declined.'
    ));
    container.append(accept, decline);
    return;
  }
  if (pending) {
    container.append(connectionButton('Pending', '', null, true));
    return;
  }
  const connect = connectionButton('Connect', 'primary');
  connect.addEventListener('click', () => runConnectionAction(
    connect,
    () => socialPost(`/api/social/connections/${person.accountId}`),
    `Connection request sent to ${person.displayName}.`
  ));
  container.append(connect);
}

async function openProfilePanel() {
  if (!profileDialog) return;
  if (await loadProfile(true)) openModal(profileDialog);
}

document.querySelectorAll('[data-open-profile]').forEach((button) => {
  button.addEventListener('click', openProfilePanel);
});

document.querySelector('#profileDialogClose')?.addEventListener('click', () => closeModal(profileDialog));
document.querySelector('#profileCancel')?.addEventListener('click', () => closeModal(profileDialog));

profileSignOut?.addEventListener('click', async () => {
  profileSignOut.disabled = true;
  setProfileStatus('Signing out securely…');
  try {
    const csrf = await csrfToken();
    const form = document.createElement('form');
    form.method = 'post';
    form.action = '/logout';
    const csrfInput = document.createElement('input');
    csrfInput.type = 'hidden';
    csrfInput.name = csrf.parameterName;
    csrfInput.value = csrf.token;
    form.append(csrfInput);
    document.body.append(form);
    form.submit();
  } catch (error) {
    profileSignOut.disabled = false;
    setProfileStatus(error.message || 'Sign out could not be completed.', 'error');
  }
});

profilePhotoInput?.addEventListener('change', () => {
  const file = profilePhotoInput.files[0];
  if (!file) return;
  if (file.size > 5 * 1024 * 1024) {
    profilePhotoInput.value = '';
    setProfileStatus('Choose a photograph smaller than 5 MB.', 'error');
    return;
  }
  if (!['image/png', 'image/jpeg'].includes(file.type)) {
    profilePhotoInput.value = '';
    setProfileStatus('Choose a PNG or JPEG photograph.', 'error');
    return;
  }
  const reader = new FileReader();
  reader.addEventListener('load', () => {
    profilePhotoPreview.style.backgroundImage = `url("${reader.result}")`;
    profilePhotoPreview.style.backgroundSize = 'cover';
    profilePhotoPreview.style.backgroundPosition = 'center';
    profilePhotoPreview.textContent = '';
  }, { once: true });
  reader.addEventListener('error', () => {
    profilePhotoInput.value = '';
    setProfileStatus('That photograph could not be previewed.', 'error');
  }, { once: true });
  reader.readAsDataURL(file);
  setProfileStatus('Photograph selected. It will be checked and processed when you save.', 'success');
});

profileForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (profileFields.disabled || !profileForm.reportValidity()) return;
  const saveButton = document.querySelector('#profileSave');
  saveButton.disabled = true;
  setProfileStatus('Saving your profile securely…');
  try {
    const csrf = await csrfToken();
    const yearValue = document.querySelector('#profileYear').value;
    const response = await fetch('/api/profile/me', {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        [csrf.headerName]: csrf.token
      },
      body: JSON.stringify({
        displayName: document.querySelector('#profileDisplayName').value,
        biography: document.querySelector('#profileBiography').value,
        courseName: document.querySelector('#profileCourse').value,
        yearOfStudy: yearValue ? Number(yearValue) : null,
        visibility: document.querySelector('#profileVisibility').value
      })
    });
    if (!response.ok) throw new Error('The profile details were not accepted. Check each field and try again.');
    let profile = await response.json();

    const photo = profilePhotoInput.files[0];
    if (photo) {
      const photoData = new FormData();
      photoData.append('photo', photo);
      const photoResponse = await fetch('/api/profile/me/photo', {
        method: 'POST',
        headers: { Accept: 'application/json', [csrf.headerName]: csrf.token },
        body: photoData
      });
      if (!photoResponse.ok) throw new Error('Your details were saved, but the photograph did not pass the upload checks.');
      profile = await photoResponse.json();
      profilePhotoInput.value = '';
    }

    populateProfile(profile);
    setProfileStatus('Profile saved successfully.', 'success');
  } catch (error) {
    setProfileStatus(error.message || 'Your profile could not be saved.', 'error');
  } finally {
    saveButton.disabled = false;
  }
});

const conversationList = document.querySelector('#conversationList');
const conversationListStatus = document.querySelector('#conversationListStatus');
const conversationSearch = document.querySelector('#conversationSearch');
const conversationMessages = document.querySelector('#conversationMessages');
const conversationName = document.querySelector('#conversationName');
const conversationAvatar = document.querySelector('#conversationAvatar');
const conversationTrust = document.querySelector('#conversationTrust');
const messageComposer = document.querySelector('#messageComposer');
const messageInput = document.querySelector('#messageInput');
const messageSend = document.querySelector('#messageSend');
const conversationCallActions = document.querySelector('#conversationCallActions');
const messageWorkspace = document.querySelector('#messages-page .message-workspace');
const messageBackButton = document.querySelector('#messageBackButton');
const audioCallButton = document.querySelector('#audioCallButton');
const videoCallButton = document.querySelector('#videoCallButton');
const callDialog = document.querySelector('#callDialog');
const callPeerName = document.querySelector('#callPeerName');
const callPeerAvatar = document.querySelector('#callPeerAvatar');
const callDirection = document.querySelector('#callDirection');
const callStatus = document.querySelector('#callStatus');
const callNetworkNotice = document.querySelector('#callNetworkNotice');
const remoteCallVideo = document.querySelector('#remoteCallVideo');
const remoteCallAudio = document.querySelector('#remoteCallAudio');
const callEnableSound = document.querySelector('#callEnableSound');
const callAudioNotice = document.querySelector('#callAudioNotice');
const localCallVideo = document.querySelector('#localCallVideo');
const incomingCallActions = document.querySelector('#incomingCallActions');
const activeCallActions = document.querySelector('#activeCallActions');
const callAccept = document.querySelector('#callAccept');
const callDecline = document.querySelector('#callDecline');
const callMute = document.querySelector('#callMute');
const callCamera = document.querySelector('#callCamera');
const callEnd = document.querySelector('#callEnd');
const studentDirectoryDialog = document.querySelector('#studentDirectoryDialog');
const studentDirectoryList = document.querySelector('#studentDirectoryList');
const studentDirectorySearch = document.querySelector('#studentDirectorySearch');
let loadedConversations = [];
let activeConversationId = null;
let activeConversationAccountId = null;
let socialSocket = null;
let realtimeReconnectTimer = null;
let directoryTimer;
let callConfiguration = null;
let activeCall = null;

function formatMessageTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const today = new Date();
  return date.toDateString() === today.toDateString()
    ? new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit' }).format(date)
    : new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short' }).format(date);
}

function recentlyJoined(value) {
  const joined = new Date(value);
  return !Number.isNaN(joined.getTime()) && Date.now() - joined.getTime() <= 30 * 24 * 60 * 60 * 1000;
}

function updateNotificationBadge(unread) {
  notificationBadge.textContent = String(unread);
  notificationBadge.hidden = unread === 0;
  notificationButton.setAttribute('aria-label', unread
    ? `Notifications, ${unread} unread`
    : 'Notifications, none unread');
  if (signedIn) notificationSummary.textContent = unread ? `${unread} new update${unread === 1 ? '' : 's'}` : 'You are up to date';
}

function notificationVisual(type) {
  if (type === 'MESSAGE') return ['message', 'message-type'];
  if (type === 'SAFETY') return ['shield', 'opportunity-type'];
  if (type === 'EVENT') return ['events', 'opportunity-type'];
  if (type === 'BOOKING') return ['calendar', 'booking-type'];
  if (type === 'ANNOUNCEMENT') return ['megaphone', 'opportunity-type'];
  if (type === 'MEETING') return ['monitor-play', 'message-type'];
  if (type === 'OPPORTUNITY') return ['briefcase', 'opportunity-type'];
  if (type === 'SUPPORT') return ['heart', 'booking-type'];
  return ['users', 'booking-type'];
}

function makeNotificationActionable(item, action) {
  item.classList.add('notification-actionable');
  item.tabIndex = 0;
  item.setAttribute('role', 'button');
  item.addEventListener('click', action);
  item.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      action();
    }
  });
}

async function loadNotifications() {
  if (!notificationList) return;
  try {
    const response = await fetch('/api/social/notifications', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      notificationList.innerHTML = '<li class="notification-empty">Sign in to load your notifications.</li>';
      updateNotificationBadge(0);
      notificationSummary.textContent = 'Sign in to view updates';
      markAllRead.disabled = true;
      return;
    }
    if (!response.ok) throw new Error();
    const notifications = await response.json();
    markAllRead.disabled = false;
    const unread = notifications.filter((item) => !item.read).length;
    updateNotificationBadge(unread);
    notificationList.replaceChildren();
    notifications.forEach((notification) => {
      const item = document.createElement('li');
      if (!notification.read) item.className = 'unread-notification';
      const [iconName, visualClass] = notificationVisual(notification.type);
      const visual = document.createElement('span');
      visual.className = `notification-type ${visualClass}`;
      visual.setAttribute('aria-hidden', 'true');
      visual.innerHTML = `<svg class="icon"><use href="#${iconName}"/></svg>`;
      const copy = document.createElement('div');
      const title = document.createElement('strong');
      title.textContent = notification.title;
      const body = document.createElement('p');
      body.textContent = notification.body;
      const time = document.createElement('time');
      time.dateTime = notification.createdAt;
      time.textContent = formatMessageTime(notification.createdAt);
      copy.append(title, body, time);
      item.append(visual, copy);
      if (!notification.read) {
        const dot = document.createElement('span');
        dot.className = 'notification-unread';
        dot.setAttribute('aria-label', 'Unread');
        item.append(dot);
      }
      if (notification.resourceType === 'CONVERSATION' && notification.resourceId) {
        makeNotificationActionable(item, async () => {
          showPage('messages');
          await loadMessagingWorkspace(notification.resourceId);
        });
      } else if (notification.resourceType === 'CONNECTION') {
        makeNotificationActionable(item, () => {
          openModal(studentDirectoryDialog);
          void loadStudentDirectory();
        });
      } else if (notification.resourceType === 'ANNOUNCEMENT') {
        makeNotificationActionable(item, () => showPage('updates'));
      } else if (notification.resourceType === 'EVENT') {
        makeNotificationActionable(item, () => showPage('events'));
      } else if (['BOOKING', 'SERVICE'].includes(notification.resourceType)) {
        makeNotificationActionable(item, () => showPage('services'));
      } else if (notification.resourceType === 'MEETING' && notification.resourceId) {
        makeNotificationActionable(item, () => {
          showPage('live-classes');
          void loadMeetingRoom(notification.resourceId);
        });
      } else if (notification.resourceType === 'OPPORTUNITY') {
        makeNotificationActionable(item, () => showPage('opportunities'));
      } else if (notification.resourceType === 'CIRCLE') {
        makeNotificationActionable(item, () => showPage('circles'));
      } else if (notification.resourceType === 'SUPPORT_CASE') {
        makeNotificationActionable(item, () => showPage('wellbeing'));
      }
      notificationList.append(item);
    });
    if (!notifications.length) {
      notificationList.innerHTML = '<li class="notification-empty">No notifications yet.</li>';
    }
  } catch {
    notificationList.innerHTML = '<li class="notification-empty">Notifications are temporarily unavailable.</li>';
  }
}

const CALL_SIGNAL_TYPES = new Set([
  'CALL_OFFER', 'CALL_ACCEPTING', 'CALL_ANSWER', 'CALL_ICE_CANDIDATE', 'CALL_REJECTED',
  'CALL_CANCELLED', 'CALL_ENDED', 'CALL_BUSY', 'CALL_UNAVAILABLE',
  'CALL_FORBIDDEN', 'CALL_INVALID', 'CALL_RESOLVED_ELSEWHERE'
]);

function callId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
    const random = Math.random() * 16 | 0;
    const value = character === 'x' ? random : (random & 0x3) | 0x8;
    return value.toString(16);
  });
}

function setCallButtonsAvailable(available) {
  if (!conversationCallActions) return;
  conversationCallActions.hidden = !available;
  audioCallButton.disabled = !available || Boolean(activeCall);
  videoCallButton.disabled = !available || Boolean(activeCall);
}

async function loadCallConfiguration() {
  // Obtain fresh temporary credentials for each call, including after a long signed-in session.
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), 10000);
  let result;
  try {
    const response = await fetch('/api/social/calls/configuration', {
      headers: { Accept: 'application/json' }, cache: 'no-store', signal: controller.signal
    });
    if (response.status === 401 || response.status === 403) {
      throw new Error('Your sign-in expired. Sign in again before calling.');
    }
    if (response.status === 503) {
      throw new Error('The call relay is unavailable. Its free allowance or settings may need attention.');
    }
    if (!response.ok) throw new Error('Call configuration is temporarily unavailable.');
    result = await response.json();
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw new Error('Call setup could not reach the server. Check your internet connection and try again.');
    }
    throw error;
  } finally {
    clearTimeout(timer);
  }
  callConfiguration = {
    iceServers: (result.iceServers || []).map((server) => ({
      urls: server.urls,
      ...(server.username ? { username: server.username } : {}),
      ...(server.credential ? { credential: server.credential } : {})
    })),
    relayConfigured: Boolean(result.relayConfigured),
    iceTransportPolicy: result.iceTransportPolicy === 'relay' ? 'relay' : 'all',
    privacyNotice: result.privacyNotice
  };
  return callConfiguration;
}

function waitForRealtime() {
  if (socialSocket?.readyState === WebSocket.OPEN) return Promise.resolve();
  connectSocialRealtime();
  return new Promise((resolve, reject) => {
    const started = Date.now();
    const timer = setInterval(() => {
      if (socialSocket?.readyState === WebSocket.OPEN) {
        clearInterval(timer);
        resolve();
      } else if (Date.now() - started > 4000) {
        clearInterval(timer);
        reject(new Error('The live call service is not connected. Please try again.'));
      }
    }, 100);
  });
}

async function sendCallSignal(type, details = {}, call = activeCall) {
  if (!call) return;
  await waitForRealtime();
  if (['CALL_OFFER', 'CALL_ACCEPTING', 'CALL_ANSWER', 'CALL_ICE_CANDIDATE'].includes(type)
      && activeCall !== call) return;
  socialSocket.send(JSON.stringify({
    type,
    callId: call.id,
    targetAccountId: call.peerAccountId,
    ...details
  }));
}

async function queueLocalCallCandidate(call, candidate) {
  if (activeCall !== call) return;
  if (!call.descriptionSignalled) {
    call.pendingLocalCandidates.push(candidate);
    return;
  }
  call.localCandidateChain = call.localCandidateChain.then(async () => {
    if (activeCall === call) await sendCallSignal('CALL_ICE_CANDIDATE', candidate, call);
  });
  await call.localCandidateChain;
}

async function signalCallDescription(type, details, call) {
  await sendCallSignal(type, details, call);
  if (activeCall !== call) return;
  call.descriptionSignalled = true;
  const pending = call.pendingLocalCandidates.splice(0);
  for (const candidate of pending) await queueLocalCallCandidate(call, candidate);
}

function updateCallControl(button, pressed, activeIcon, inactiveIcon, activeLabel, inactiveLabel) {
  button.setAttribute('aria-pressed', String(pressed));
  button.querySelector('use').setAttribute('href', `#${pressed ? activeIcon : inactiveIcon}`);
  button.querySelector('span').textContent = pressed ? activeLabel : inactiveLabel;
}

function showCallInterface(call, mode) {
  callPeerName.textContent = call.peerName;
  callPeerAvatar.textContent = initialsFor(call.peerName);
  callDirection.textContent = mode === 'incoming'
    ? `INCOMING ${call.media === 'VIDEO' ? 'VIDEO' : 'VOICE'} CALL`
    : `${call.media === 'VIDEO' ? 'VIDEO' : 'VOICE'} CALL`;
  callStatus.textContent = mode === 'incoming' ? `${call.peerName} is calling you.` : `Calling ${call.peerName}…`;
  incomingCallActions.hidden = mode !== 'incoming';
  activeCallActions.hidden = mode === 'incoming';
  callCamera.hidden = call.media !== 'VIDEO';
  callMute.disabled = false;
  callCamera.disabled = false;
  callEnd.disabled = false;
  callAccept.disabled = false;
  callDecline.disabled = false;
  updateCallControl(callMute, false, 'mic-off', 'mic', 'Unmute', 'Mute');
  updateCallControl(callCamera, false, 'video-off', 'video', 'Camera on', 'Camera');
  remoteCallVideo.hidden = true;
  remoteCallVideo.controls = false;
  remoteCallVideo.srcObject = null;
  remoteCallAudio.srcObject = null;
  remoteCallAudio.muted = false;
  remoteCallAudio.volume = 1;
  callEnableSound.hidden = true;
  callAudioNotice.hidden = true;
  callAudioNotice.textContent = '';
  localCallVideo.hidden = true;
  localCallVideo.srcObject = null;
  callPeerAvatar.hidden = false;
  callNetworkNotice.textContent = call.media === 'VIDEO'
    ? 'Your browser will ask before sharing your microphone and camera. UniCircle does not record calls.'
    : 'Your browser will ask before sharing your microphone. UniCircle does not record calls.';
  openModal(callDialog);
  (mode === 'incoming' ? callAccept : callEnd).focus();
  setCallButtonsAvailable(Boolean(activeConversationAccountId));
}

function stopCallMedia(call) {
  clearTimeout(call?.ringTimer);
  clearTimeout(call?.disconnectTimer);
  clearTimeout(call?.connectTimer);
  call?.localStream?.getTracks().forEach((track) => track.stop());
  if (call?.peerConnection) {
    call.peerConnection.ontrack = null;
    call.peerConnection.onicecandidate = null;
    call.peerConnection.onconnectionstatechange = null;
    call.peerConnection.close();
  }
  // A late permission result from a cancelled call must not clear a newer call's players.
  if (activeCall && activeCall !== call) return;
  remoteCallAudio.pause();
  remoteCallAudio.srcObject = null;
  callEnableSound.hidden = true;
  callAudioNotice.hidden = true;
  remoteCallVideo.srcObject = null;
  localCallVideo.srcObject = null;
}

function finishCall(message, delay = 1300) {
  const finished = activeCall;
  if (!finished) return;
  stopCallMedia(finished);
  activeCall = null;
  incomingCallActions.hidden = true;
  activeCallActions.hidden = true;
  callStatus.textContent = message;
  setCallButtonsAvailable(Boolean(activeConversationAccountId));
  window.setTimeout(() => {
    if (!activeCall && !callDialog.hidden) closeModal(callDialog);
  }, delay);
}

async function requestCallMedia(media) {
  if (!navigator.mediaDevices?.getUserMedia || !globalThis.RTCPeerConnection) {
    throw new Error('This browser does not support secure voice and video calls.');
  }
  if (!window.isSecureContext) {
    throw new Error('Calls require a secure HTTPS address.');
  }
  try {
    // Start capture from the user's click, before waiting on signalling or configuration.
    const capture = navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
      video: media === 'VIDEO' ? { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 720 } } : false
    });
    return await new Promise((resolve, reject) => {
      let settled = false;
      const timer = window.setTimeout(() => {
        settled = true;
        const error = new Error('Microphone or camera permission is still waiting. Check this site\'s browser permissions, then try the call again.');
        error.name = 'CallMediaTimeoutError';
        reject(error);
      }, 25000);
      capture.then((stream) => {
        if (settled) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        settled = true;
        clearTimeout(timer);
        resolve(stream);
      }, (error) => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        reject(error);
      });
    });
  } catch (error) {
    if (error?.name === 'CallMediaTimeoutError') throw error;
    if (error?.name === 'NotAllowedError') throw new Error('Microphone or camera permission was not granted. Allow it in this site\'s browser settings, then try again.');
    if (error?.name === 'NotFoundError') throw new Error('A required microphone or camera was not found.');
    throw new Error('Your microphone or camera could not be started.');
  }
}

async function playRemoteCallAudio(call) {
  if (activeCall !== call || !remoteCallAudio.srcObject) return;
  try {
    await remoteCallAudio.play();
    if (activeCall !== call) return;
    callEnableSound.hidden = true;
    callAudioNotice.hidden = true;
  } catch {
    if (activeCall !== call) return;
    callEnableSound.hidden = false;
    callAudioNotice.hidden = false;
    callAudioNotice.textContent = 'Call sound has not started. Tap Enable call sound, then check your device volume and speaker or headset output.';
  }
}

async function createCallPeer(call) {
  const configuration = await loadCallConfiguration();
  if (activeCall !== call) throw new Error('The call was cancelled.');
  call.relayConfigured = configuration.relayConfigured;
  callNetworkNotice.textContent = configuration.relayConfigured
    ? `${configuration.privacyNotice} A relay is configured for calls across different networks; connectivity still depends on the relay and both internet connections.`
    : `${configuration.privacyNotice} This pilot currently uses direct connections only, so some mobile or restricted Wi-Fi networks may block the call.`;
  const peer = new RTCPeerConnection({
    iceServers: configuration.iceServers,
    iceTransportPolicy: configuration.iceTransportPolicy
  });
  call.peerConnection = peer;
  call.pendingLocalCandidates = [];
  call.localCandidateChain = Promise.resolve();
  call.descriptionSignalled = false;
  call.remoteStream = new MediaStream();
  call.localStream.getTracks().forEach((track) => peer.addTrack(track, call.localStream));
  peer.addEventListener('icecandidate', (event) => {
    if (!event.candidate || activeCall !== call) return;
    void queueLocalCallCandidate(call, {
      candidate: event.candidate.candidate,
      sdpMid: event.candidate.sdpMid,
      sdpMLineIndex: event.candidate.sdpMLineIndex
    }).catch(() => {
      if (activeCall === call) finishCall('The live call connection was lost.');
    });
  });
  peer.addEventListener('track', (event) => {
    if (activeCall !== call) return;
    // Some browsers deliver audio and video in separate streamless track events.
    // Accumulate tracks instead of replacing the previous track (and losing sound).
    if (!call.remoteStream.getTracks().some((track) => track.id === event.track.id)) {
      call.remoteStream.addTrack(event.track);
    }
    if (event.track.kind === 'audio') {
      remoteCallAudio.srcObject = new MediaStream(call.remoteStream.getAudioTracks());
      void playRemoteCallAudio(call);
    }
    if (call.media === 'VIDEO' && event.track.kind === 'video') {
      remoteCallVideo.srcObject = call.remoteStream;
      void remoteCallVideo.play().catch(() => {
        if (activeCall === call) remoteCallVideo.controls = true;
      });
      remoteCallVideo.hidden = false;
      callPeerAvatar.hidden = true;
    }
  });
  peer.addEventListener('connectionstatechange', () => {
    if (activeCall !== call) return;
    if (peer.connectionState === 'connected') {
      clearTimeout(call.ringTimer);
      clearTimeout(call.disconnectTimer);
      clearTimeout(call.connectTimer);
      call.phase = 'connected';
      callStatus.textContent = 'Connected';
    } else if (peer.connectionState === 'disconnected') {
      callStatus.textContent = 'Reconnecting…';
      clearTimeout(call.disconnectTimer);
      call.disconnectTimer = window.setTimeout(() => {
        if (activeCall === call && peer.connectionState === 'disconnected') {
          void sendCallSignal('CALL_ENDED').catch(() => {});
          finishCall('Call ended because the connection was lost.');
        }
      }, 8000);
    } else if (peer.connectionState === 'failed' || peer.connectionState === 'closed') {
      void sendCallSignal('CALL_ENDED').catch(() => {});
      finishCall(call.relayConfigured === false
        ? 'A direct call could not connect across these networks. A TURN relay must be configured for reliable mobile and restricted Wi-Fi calls.'
        : 'The call connection ended.');
    }
  });
  return peer;
}

function startCallConnectionDeadline(call) {
  clearTimeout(call.connectTimer);
  call.connectTimer = window.setTimeout(() => {
    if (activeCall !== call || call.phase === 'connected') return;
    void sendCallSignal('CALL_ENDED').catch(() => {});
    finishCall(call.phase === 'answering'
      ? 'Answering timed out. Check the microphone and camera permissions on the receiving device.'
      : call.relayConfigured === false
      ? 'The devices could not connect directly. The service owner needs to configure a working TURN relay for calls across restricted networks.'
      : 'The secure media connection timed out. Please try again.');
  }, 45000);
}

async function flushCallCandidates(call) {
  if (!call.peerConnection?.remoteDescription) return;
  const pending = [...call.pendingCandidates];
  call.pendingCandidates.length = 0;
  for (const candidate of pending) {
    try { await call.peerConnection.addIceCandidate(candidate); } catch { /* Ignore obsolete candidates. */ }
  }
}

async function beginCall(media) {
  if (!activeConversationAccountId || activeCall) return;
  const call = {
    id: callId(),
    peerAccountId: activeConversationAccountId,
    peerName: conversationName.textContent,
    media,
    direction: 'outgoing',
    phase: 'preparing',
    pendingCandidates: []
  };
  activeCall = call;
  showCallInterface(call, 'outgoing');
  try {
    call.localStream = await requestCallMedia(media);
    if (activeCall !== call) return stopCallMedia(call);
    localCallVideo.srcObject = call.localStream;
    localCallVideo.hidden = media !== 'VIDEO';
    if (media === 'VIDEO') void localCallVideo.play().catch(() => {});
    const peer = await createCallPeer(call);
    const offer = await peer.createOffer();
    await peer.setLocalDescription(offer);
    call.phase = 'ringing';
    await signalCallDescription('CALL_OFFER', { media, sdp: offer.sdp }, call);
    if (activeCall !== call) return stopCallMedia(call);
    if (call.phase === 'ringing') {
      callStatus.textContent = `Ringing ${call.peerName}…`;
      call.ringTimer = window.setTimeout(() => {
        if (activeCall === call && call.phase === 'ringing') {
          void sendCallSignal('CALL_CANCELLED').catch(() => {});
          finishCall('No answer.');
        }
      }, 30000);
    }
  } catch (error) {
    if (activeCall === call) finishCall(error.message || 'The call could not be started.', 8000);
    else stopCallMedia(call);
  }
}

async function acceptIncomingCall() {
  const call = activeCall;
  if (!call || call.direction !== 'incoming' || call.phase !== 'ringing') return;
  call.phase = 'answering';
  callAccept.disabled = true;
  callDecline.disabled = true;
  incomingCallActions.hidden = true;
  activeCallActions.hidden = false;
  callMute.disabled = true;
  callCamera.disabled = true;
  callStatus.textContent = call.media === 'VIDEO'
    ? 'Allow your microphone and camera to answer…' : 'Allow your microphone to answer…';
  try {
    const capture = requestCallMedia(call.media);
    // This acknowledgement changes the caller's state without waiting for a permission prompt.
    void sendCallSignal('CALL_ACCEPTING', {}, call).catch(() => {});
    call.localStream = await capture;
    if (activeCall !== call) return stopCallMedia(call);
    localCallVideo.srcObject = call.localStream;
    localCallVideo.hidden = call.media !== 'VIDEO';
    if (call.media === 'VIDEO') void localCallVideo.play().catch(() => {});
    const peer = await createCallPeer(call);
    await peer.setRemoteDescription({ type: 'offer', sdp: call.offerSdp });
    await flushCallCandidates(call);
    const answer = await peer.createAnswer();
    await peer.setLocalDescription(answer);
    call.phase = 'connecting';
    incomingCallActions.hidden = true;
    activeCallActions.hidden = false;
    callCamera.hidden = call.media !== 'VIDEO';
    callStatus.textContent = 'Connecting securely…';
    callMute.disabled = false;
    callCamera.disabled = false;
    await signalCallDescription('CALL_ANSWER', { sdp: answer.sdp }, call);
    startCallConnectionDeadline(call);
  } catch (error) {
    if (activeCall === call) {
      await sendCallSignal('CALL_REJECTED', {}, call).catch(() => {});
      if (activeCall === call) finishCall(error.message || 'The call could not be answered.', 8000);
    } else stopCallMedia(call);
  } finally {
    if (activeCall === call) {
      callAccept.disabled = false;
      callDecline.disabled = false;
    }
  }
}

async function handleCallSignal(signal) {
  if (signal.type === 'CALL_OFFER') {
    if (activeCall) {
      const busyCall = {
        id: signal.callId,
        peerAccountId: signal.fromAccountId,
        peerName: signal.fromDisplayName,
        media: signal.media,
        pendingCandidates: []
      };
      await sendCallSignal('CALL_BUSY', {}, busyCall).catch(() => {});
      return;
    }
    activeCall = {
      id: signal.callId,
      peerAccountId: signal.fromAccountId,
      peerName: signal.fromDisplayName,
      media: signal.media,
      direction: 'incoming',
      phase: 'ringing',
      offerSdp: signal.sdp,
      pendingCandidates: []
    };
    showCallInterface(activeCall, 'incoming');
    return;
  }
  if (!activeCall || (signal.callId && signal.callId !== activeCall.id)) return;
  if (signal.type === 'CALL_ACCEPTING') {
    if (activeCall.direction !== 'outgoing' || activeCall.phase !== 'ringing') return;
    clearTimeout(activeCall.ringTimer);
    activeCall.phase = 'answering';
    callStatus.textContent = 'Answered. Waiting for the other device\'s microphone or camera…';
    startCallConnectionDeadline(activeCall);
  } else if (signal.type === 'CALL_ANSWER') {
    if (activeCall.direction !== 'outgoing' || !activeCall.peerConnection) return;
    clearTimeout(activeCall.ringTimer);
    activeCall.phase = 'connecting';
    callStatus.textContent = 'Connecting securely…';
    await activeCall.peerConnection.setRemoteDescription({ type: 'answer', sdp: signal.sdp });
    await flushCallCandidates(activeCall);
    startCallConnectionDeadline(activeCall);
  } else if (signal.type === 'CALL_ICE_CANDIDATE') {
    const candidate = new RTCIceCandidate({
      candidate: signal.candidate,
      sdpMid: signal.sdpMid ?? null,
      sdpMLineIndex: signal.sdpMLineIndex ?? null
    });
    if (activeCall.peerConnection?.remoteDescription) {
      try { await activeCall.peerConnection.addIceCandidate(candidate); } catch { /* Candidate may be obsolete. */ }
    } else {
      activeCall.pendingCandidates.push(candidate);
    }
  } else if (signal.type === 'CALL_REJECTED') {
    finishCall(`${activeCall.peerName} declined the call.`);
  } else if (signal.type === 'CALL_BUSY') {
    finishCall(`${activeCall.peerName} is already on another call.`);
  } else if (signal.type === 'CALL_UNAVAILABLE') {
    finishCall(signal.reason || `${activeCall.peerName} is not currently online.`, 2200);
  } else if (signal.type === 'CALL_FORBIDDEN' || signal.type === 'CALL_INVALID') {
    finishCall(signal.reason || 'The call was not permitted.', 2200);
  } else if (signal.type === 'CALL_CANCELLED') {
    finishCall('The caller cancelled.');
  } else if (signal.type === 'CALL_ENDED') {
    finishCall('Call ended.');
  } else if (signal.type === 'CALL_RESOLVED_ELSEWHERE') {
    finishCall('This call was answered on another tab.');
  }
}

async function declineOrEndCall() {
  if (!activeCall) return;
  const type = activeCall.direction === 'incoming' && activeCall.phase === 'ringing'
    ? 'CALL_REJECTED'
    : activeCall.direction === 'outgoing' && activeCall.phase === 'ringing'
      ? 'CALL_CANCELLED'
      : 'CALL_ENDED';
  await sendCallSignal(type).catch(() => {});
  finishCall(type === 'CALL_REJECTED' ? 'Call declined.' : 'Call ended.', 700);
}

audioCallButton?.addEventListener('click', () => void beginCall('AUDIO'));
videoCallButton?.addEventListener('click', () => void beginCall('VIDEO'));
callAccept?.addEventListener('click', () => void acceptIncomingCall());
callDecline?.addEventListener('click', () => void declineOrEndCall());
callEnd?.addEventListener('click', () => void declineOrEndCall());
callEnableSound?.addEventListener('click', () => {
  if (activeCall) void playRemoteCallAudio(activeCall);
});
callMute?.addEventListener('click', () => {
  if (!activeCall?.localStream) return;
  const muted = callMute.getAttribute('aria-pressed') !== 'true';
  activeCall.localStream.getAudioTracks().forEach((track) => { track.enabled = !muted; });
  updateCallControl(callMute, muted, 'mic-off', 'mic', 'Unmute', 'Mute');
});
callCamera?.addEventListener('click', () => {
  if (!activeCall?.localStream || activeCall.media !== 'VIDEO') return;
  const disabled = callCamera.getAttribute('aria-pressed') !== 'true';
  activeCall.localStream.getVideoTracks().forEach((track) => { track.enabled = !disabled; });
  updateCallControl(callCamera, disabled, 'video-off', 'video', 'Camera on', 'Camera');
});
window.addEventListener('beforeunload', () => {
  if (activeCall && socialSocket?.readyState === WebSocket.OPEN) {
    socialSocket.send(JSON.stringify({
      type: activeCall.phase === 'ringing' ? 'CALL_CANCELLED' : 'CALL_ENDED',
      callId: activeCall.id,
      targetAccountId: activeCall.peerAccountId
    }));
  }
  stopCallMedia(activeCall);
});

function connectSocialRealtime() {
  if (socialSocket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socialSocket.readyState)) return;
  clearTimeout(realtimeReconnectTimer);
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  socialSocket = new WebSocket(`${protocol}//${location.host}/ws/social`);
  socialSocket.addEventListener('message', async (event) => {
    let update;
    try { update = JSON.parse(event.data); } catch { return; }
    if (CALL_SIGNAL_TYPES.has(update.type)) {
      await handleCallSignal(update);
      return;
    }
    await loadNotifications();
    if (['MESSAGE_RECEIVED', 'MESSAGE_SENT'].includes(update.type)) {
      const messagesPageOpen = !document.querySelector('#messages-page').hidden;
      if (messagesPageOpen) await loadMessagingWorkspace(activeConversationId || update.resourceId);
      else await refreshConversationSummary();
    }
    if (update.type === 'CONNECTIONS_CHANGED') {
      await Promise.all([loadUniversityContext(), loadStudentDirectory(), refreshConversationSummary()]);
    }
  });
  socialSocket.addEventListener('close', () => {
    socialSocket = null;
    realtimeReconnectTimer = setTimeout(connectSocialRealtime, 3000);
  });
}

function emptyConversation(message = 'Select a conversation or find a student and connect.', resetSelection = true) {
  if (resetSelection) activeConversationAccountId = null;
  document.querySelector('#conversationSafetyButton').hidden = true;
  setCallButtonsAvailable(!resetSelection && Boolean(activeConversationAccountId));
  conversationMessages.replaceChildren();
  const empty = document.createElement('div');
  empty.className = 'conversation-empty';
  const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  icon.classList.add('icon');
  const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
  use.setAttribute('href', '#message');
  icon.append(use);
  const heading = document.createElement('strong');
  heading.textContent = 'Your private messages appear here';
  const detail = document.createElement('span');
  detail.textContent = message;
  empty.append(icon, heading, detail);
  if (message.toLowerCase().startsWith('sign in')) {
    const signIn = document.createElement('a');
    signIn.className = 'empty-action';
    signIn.href = '/sign-in';
    signIn.textContent = 'Sign in to start messaging';
    empty.append(signIn);
  }
  conversationMessages.append(empty);
  messageInput.disabled = true;
  messageSend.disabled = true;
}

function renderConversationList() {
  const query = conversationSearch.value.trim().toLowerCase();
  conversationList.replaceChildren();
  const visible = loadedConversations.filter((conversation) =>
    !query || conversation.participantDisplayName.toLowerCase().includes(query)
      || conversation.lastMessage.toLowerCase().includes(query));
  visible.forEach((conversation) => {
    const item = document.createElement('li');
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `thread-button${conversation.conversationId === activeConversationId ? ' active' : ''}`;
    button.dataset.conversationId = conversation.conversationId;
    const avatar = document.createElement('span');
    avatar.className = 'avatar navy-avatar';
    avatar.textContent = conversation.participantInitials;
    showMemberPhoto(avatar, conversation.participantAccountId);
    const details = document.createElement('div');
    const name = document.createElement('strong');
    name.textContent = conversation.participantDisplayName;
    const preview = document.createElement('span');
    preview.textContent = conversation.lastMessage;
    details.append(name, preview);
    const time = document.createElement('time');
    time.dateTime = conversation.lastMessageAt || '';
    time.textContent = formatMessageTime(conversation.lastMessageAt);
    const threadMeta = document.createElement('span');
    threadMeta.className = 'thread-meta';
    threadMeta.append(time);
    if (conversation.unreadCount > 0) {
      const unread = document.createElement('span');
      unread.className = 'thread-unread';
      unread.textContent = String(conversation.unreadCount);
      unread.setAttribute('aria-label', `${conversation.unreadCount} unread messages`);
      threadMeta.append(unread);
    }
    button.append(avatar, details, threadMeta);
    button.addEventListener('click', () => selectConversation(conversation.conversationId));
    item.append(button);
    conversationList.append(item);
  });
  conversationListStatus.textContent = visible.length ? '' : query
    ? 'No conversations match your search.'
    : 'No conversations yet. Find a student and send a connection request.';
}

function renderSignedOutConversationList() {
  conversationList.replaceChildren();
  const item = document.createElement('li');
  item.className = 'message-signin-card';
  const icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  icon.classList.add('icon');
  const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
  use.setAttribute('href', '#users');
  icon.append(use);
  const heading = document.createElement('strong');
  heading.textContent = 'Connect with real test accounts';
  const detail = document.createElement('span');
  detail.textContent = 'Sign in, find another student, send a request and start a live conversation after they accept.';
  const action = document.createElement('a');
  action.href = '/sign-in';
  action.textContent = 'Sign in to continue';
  item.append(icon, heading, detail, action);
  conversationList.append(item);
}

function updateMessageUnreadBadge() {
  const unread = loadedConversations.reduce((total, conversation) => total + Number(conversation.unreadCount || 0), 0);
  messageUnreadBadge.textContent = String(unread);
  messageUnreadBadge.hidden = unread === 0;
  if (mobileMessageUnreadBadge) {
    mobileMessageUnreadBadge.textContent = String(unread);
    mobileMessageUnreadBadge.hidden = unread === 0;
  }
}

function renderDashboardMessages() {
  if (!pilotMode) return;
  const list = document.querySelector('#dashboardMessagesList');
  list.replaceChildren();
  if (!loadedConversations.length) {
    const empty = document.createElement('li');
    empty.textContent = 'No conversations yet. Find a friend and connect to start chatting.';
    list.append(empty);
    return;
  }
  loadedConversations.slice(0, 3).forEach((conversation) => {
    const item = document.createElement('li');
    const avatar = document.createElement('span');
    avatar.className = 'avatar navy-avatar';
    avatar.textContent = conversation.participantInitials;
    showMemberPhoto(avatar, conversation.participantAccountId);
    const info = document.createElement('div');
    const name = document.createElement('strong');
    name.textContent = conversation.participantDisplayName;
    const preview = document.createElement('small');
    preview.textContent = conversation.lastMessage || 'Open your conversation';
    info.append(name, preview);
    item.append(avatar, info);
    item.addEventListener('click', () => {
      showPage('messages');
      void loadMessagingWorkspace(conversation.conversationId);
    });
    list.append(item);
  });
}

async function refreshConversationSummary() {
  try {
    const response = await fetch('/api/social/conversations', { headers: { Accept: 'application/json' } });
    if (!response.ok) return;
    loadedConversations = await response.json();
    updateMessageUnreadBadge();
    renderConversationList();
    renderDashboardMessages();
  } catch {
    // A temporary realtime refresh failure does not interrupt the current screen.
  }
}

async function loadMessagingWorkspace(preferredConversationId = null) {
  if (!conversationList) return;
  conversationListStatus.textContent = 'Loading your private conversations…';
  try {
    const response = await fetch('/api/social/conversations', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      loadedConversations = [];
      messageWorkspace?.classList.remove('conversation-open');
      conversationListStatus.textContent = '';
      renderSignedOutConversationList();
      emptyConversation('Sign in to use consent-based student messaging.');
      return;
    }
    if (!response.ok) throw new Error('Conversations are temporarily unavailable.');
    loadedConversations = await response.json();
    updateMessageUnreadBadge();
    renderDashboardMessages();
    const preferred = preferredConversationId || activeConversationId;
    const selected = loadedConversations.find((item) => item.conversationId === preferred)
      || loadedConversations[0];
    const shouldOpenConversation = !matchMedia('(max-width: 620px)').matches || Boolean(preferred);
    activeConversationId = shouldOpenConversation ? selected?.conversationId || null : null;
    renderConversationList();
    if (selected && shouldOpenConversation) await selectConversation(selected.conversationId);
    else {
      messageWorkspace?.classList.remove('conversation-open');
      emptyConversation(selected ? 'Choose a conversation to open it.' : undefined);
    }
  } catch (error) {
    conversationListStatus.textContent = error.message;
    emptyConversation('Please refresh the page and try again.');
  }
}

async function selectConversation(conversationId) {
  activeConversationId = conversationId;
  messageWorkspace?.classList.add('conversation-open');
  renderConversationList();
  conversationMessages.replaceChildren();
  const loading = document.createElement('div');
  loading.className = 'directory-empty';
  loading.textContent = 'Loading messages…';
  conversationMessages.append(loading);
  try {
    const response = await fetch(`/api/social/conversations/${conversationId}/messages`, {
      headers: { Accept: 'application/json' }
    });
    if (!response.ok) throw new Error('This conversation could not be opened.');
    const result = await response.json();
    activeConversationAccountId = result.conversation.participantAccountId;
    conversationName.textContent = result.conversation.participantDisplayName;
    conversationAvatar.textContent = result.conversation.participantInitials;
    conversationAvatar.style.backgroundImage = '';
    showMemberPhoto(conversationAvatar, result.conversation.participantAccountId);
    conversationTrust.innerHTML = '<i class="online-dot"></i> Accepted connection · private conversation';
    document.querySelector('#conversationSafetyButton').hidden = false;
    conversationMessages.replaceChildren();
    if (!result.messages.length) {
      emptyConversation('You are connected. Send the first message.', false);
    } else {
      result.messages.forEach((message) => {
        const bubble = document.createElement('p');
        bubble.className = `bubble ${message.sentByCurrentAccount ? 'sent' : 'received'}`;
        bubble.textContent = message.body;
        const metadata = document.createElement('span');
        metadata.className = 'message-meta';
        metadata.textContent = `${message.sentByCurrentAccount ? 'You' : message.senderDisplayName} · ${formatMessageTime(message.sentAt)}`;
        bubble.append(metadata);
        conversationMessages.append(bubble);
      });
    }
    messageInput.disabled = false;
    messageSend.disabled = false;
    document.querySelector('#conversationSafetyButton').hidden = false;
    setCallButtonsAvailable(true);
    messageInput.placeholder = `Message ${result.conversation.participantDisplayName}`;
    const selected = loadedConversations.find((item) => item.conversationId === conversationId);
    if (selected) selected.unreadCount = 0;
    updateMessageUnreadBadge();
    renderConversationList();
    conversationMessages.scrollTop = conversationMessages.scrollHeight;
  } catch (error) {
    emptyConversation(error.message);
  }
}

messageBackButton?.addEventListener('click', () => {
  messageWorkspace?.classList.remove('conversation-open');
  activeConversationId = null;
  renderConversationList();
  conversationSearch?.focus();
});

messageComposer?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const body = messageInput.value.trim();
  if (!activeConversationId || !body) return;
  messageInput.disabled = true;
  messageSend.disabled = true;
  try {
    await socialPost(`/api/social/conversations/${activeConversationId}/messages`, { body });
    messageInput.value = '';
    await loadMessagingWorkspace(activeConversationId);
    messageInput.focus();
  } catch (error) {
    showToast(error.message);
    messageInput.disabled = false;
    messageSend.disabled = false;
  }
});

conversationSearch?.addEventListener('input', renderConversationList);

async function startConversation(accountId) {
  try {
    const conversation = await socialPost(`/api/social/conversations/direct/${accountId}`);
    if (!studentDirectoryDialog.hidden) closeModal(studentDirectoryDialog);
    if (!profileDialog.hidden) closeModal(profileDialog);
    showPage('messages');
    await loadMessagingWorkspace(conversation.conversationId);
  } catch (error) {
    showToast(error.message);
  }
}

async function loadStudentDirectory() {
  if (!studentDirectoryList || studentDirectoryDialog.hidden) return;
  studentDirectoryList.innerHTML = '<div class="directory-empty">Loading verified student profiles…</div>';
  try {
    const query = encodeURIComponent(studentDirectorySearch.value.trim());
    const response = await fetch(`/api/social/directory?query=${query}`, { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      studentDirectoryList.innerHTML = '<div class="directory-empty">Sign in to browse students from your university.</div>';
      return;
    }
    if (!response.ok) throw new Error('The student directory is temporarily unavailable.');
    const students = await response.json();
    studentDirectoryList.replaceChildren();
    students.forEach((student) => {
      const person = document.createElement('article');
      person.className = 'directory-person';
      const avatar = document.createElement('span');
      avatar.className = 'avatar navy-avatar';
      avatar.textContent = initialsFor(student.displayName);
      if (student.hasPhoto) showMemberPhoto(avatar, student.accountId);
      const details = document.createElement('span');
      details.className = 'directory-person-details';
      const name = document.createElement('strong');
      name.textContent = student.displayName;
      const course = document.createElement('span');
      course.textContent = student.courseName || 'University student';
      const meta = document.createElement('small');
      const status = recentlyJoined(student.joinedAt) ? 'New to UniCircle'
        : pilotMode ? 'Pilot member · identity not university-verified' : 'Verified fictional student';
      meta.textContent = student.yearOfStudy ? `Year ${student.yearOfStudy} · ${status}` : status;
      details.append(name, course, meta);
      const actions = document.createElement('span');
      actions.className = 'coursemate-actions';
      appendConnectionActions(actions, student);
      person.append(avatar, details, actions);
      studentDirectoryList.append(person);
    });
    if (!students.length) studentDirectoryList.innerHTML = '<div class="directory-empty">No permitted student profiles match this search.</div>';
  } catch (error) {
    studentDirectoryList.innerHTML = `<div class="directory-empty">${error.message}</div>`;
  }
}

document.querySelector('#studentDirectoryButton')?.addEventListener('click', () => {
  openModal(studentDirectoryDialog);
  studentDirectorySearch.value = '';
  void loadStudentDirectory();
});
document.querySelector('#pilotFindFriends')?.addEventListener('click', () => {
  document.querySelector('#studentDirectoryButton')?.click();
});
document.querySelector('#studentDirectoryClose')?.addEventListener('click', () => closeModal(studentDirectoryDialog));
studentDirectorySearch?.addEventListener('input', () => {
  clearTimeout(directoryTimer);
  directoryTimer = setTimeout(loadStudentDirectory, 220);
});

const safetyDialog = document.querySelector('#safetyDialog');
const safetyButton = document.querySelector('#conversationSafetyButton');
const blockAccountButton = document.querySelector('#blockAccountButton');
const reportAccountForm = document.querySelector('#reportAccountForm');

safetyButton?.addEventListener('click', () => {
  if (!activeConversationAccountId) return;
  document.querySelector('#safetyPerson').textContent = `Control contact with ${conversationName.textContent} or confidentially report a concern.`;
  blockAccountButton.dataset.confirming = 'false';
  blockAccountButton.textContent = 'Block account';
  reportAccountForm.reset();
  openModal(safetyDialog);
});

document.querySelector('#safetyClose')?.addEventListener('click', () => closeModal(safetyDialog));

blockAccountButton?.addEventListener('click', async () => {
  if (!activeConversationAccountId) return;
  if (blockAccountButton.dataset.confirming !== 'true') {
    blockAccountButton.dataset.confirming = 'true';
    blockAccountButton.textContent = `Confirm block ${conversationName.textContent}`;
    return;
  }
  blockAccountButton.disabled = true;
  try {
    const result = await socialPost(`/api/social/safety/block/${activeConversationAccountId}`);
    closeModal(safetyDialog);
    showToast(`${result.displayName} has been blocked. You will no longer receive messages from this account.`);
    activeConversationId = null;
    await Promise.all([loadMessagingWorkspace(), loadUniversityContext(), loadNotifications()]);
  } catch (error) {
    showToast(error.message);
  } finally {
    blockAccountButton.disabled = false;
  }
});

reportAccountForm?.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (!activeConversationAccountId || !reportAccountForm.reportValidity()) return;
  const submit = reportAccountForm.querySelector('button[type="submit"]');
  submit.disabled = true;
  try {
    await socialPost(`/api/social/safety/report/${activeConversationAccountId}`, {
      reason: document.querySelector('#reportReason').value,
      details: document.querySelector('#reportDetails').value.trim()
    });
    closeModal(safetyDialog);
    showToast('Your confidential report has been recorded for safeguarding review.');
    await loadNotifications();
  } catch (error) {
    showToast(error.message);
  } finally {
    submit.disabled = false;
  }
});

document.querySelectorAll('.message-button').forEach((button) => {
  button.addEventListener('click', () => {
    showPage('messages');
    openModal(studentDirectoryDialog);
    studentDirectorySearch.value = button.dataset.person || '';
    void loadStudentDirectory();
  });
});

const locationStatus = document.querySelector('#locationStatus');
const locationStart = document.querySelector('#locationStart');
const locationStop = document.querySelector('#locationStop');
const locationRefresh = document.querySelector('#locationRefresh');
const friendLocations = document.querySelector('#friendLocations');
// Campus information, private support and live learning
const announcementFeed = document.querySelector('#announcementFeed');
const dashboardAnnouncementList = document.querySelector('#dashboardAnnouncementList');
const announcementPublisherPanel = document.querySelector('#announcementPublisherPanel');
const meetingList = document.querySelector('#meetingList');
const dashboardMeetingList = document.querySelector('#dashboardMeetingList');
const supportCaseList = document.querySelector('#supportCaseList');
let loadedAnnouncements = [];
let loadedMeetings = [];
let selectedMeetingId = null;
let currentMeetingRoom = null;
let selectedClassMessageType = 'CHAT';

function informationLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function informationDate(value, options = {}) {
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', ...options
  }).format(new Date(value));
}

function renderDashboardAnnouncements() {
  if (!dashboardAnnouncementList) return;
  dashboardAnnouncementList.replaceChildren();
  loadedAnnouncements.slice(0, 3).forEach((announcement) => {
    const row = document.createElement('button');
    row.type = 'button';
    row.className = `dashboard-update ${announcement.read ? '' : 'unread'}`;
    const icon = document.createElement('span');
    icon.className = announcement.urgent ? 'update-icon urgent' : 'update-icon';
    icon.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${announcement.urgent ? 'alert' : 'megaphone'}"/></svg>`;
    const copy = document.createElement('span');
    const title = document.createElement('strong');
    title.textContent = announcement.title;
    const meta = document.createElement('small');
    meta.textContent = `${informationLabel(announcement.category)} · ${announcement.authorName}`;
    copy.append(title, meta);
    row.append(icon, copy);
    row.addEventListener('click', () => {
      showPage('updates');
      document.querySelector(`[data-announcement-id="${announcement.id}"]`)?.focus();
    });
    dashboardAnnouncementList.append(row);
  });
}

function announcementCard(announcement) {
  const card = document.createElement('article');
  card.className = `announcement-card ${announcement.read ? '' : 'unread'} ${announcement.urgent ? 'urgent' : ''}`;
  card.dataset.announcementId = announcement.id;
  card.tabIndex = -1;
  const visual = document.createElement('span');
  visual.className = `announcement-visual ${announcement.urgent ? 'urgent' : ''}`;
  visual.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${announcement.urgent ? 'alert' : 'megaphone'}"/></svg>`;
  const content = document.createElement('div');
  content.className = 'announcement-copy';
  const badges = document.createElement('div');
  badges.className = 'announcement-badges';
  [informationLabel(announcement.category), announcement.audienceLabel].filter(Boolean).forEach((label, index) => {
    const badge = document.createElement('span');
    badge.className = index === 0 ? 'pill info' : 'pill neutral';
    badge.textContent = label;
    badges.append(badge);
  });
  if (announcement.pinned) {
    const pinned = document.createElement('span');
    pinned.className = 'pill success';
    pinned.textContent = 'Pinned';
    badges.append(pinned);
  }
  const title = document.createElement('h3');
  title.textContent = announcement.title;
  const body = document.createElement('p');
  body.textContent = announcement.body;
  const author = document.createElement('small');
  author.textContent = `${announcement.authorName} · ${announcement.authorRole} · ${informationDate(announcement.publishedAt)}`;
  content.append(badges, title, body, author);
  const actions = document.createElement('div');
  actions.className = 'announcement-actions';
  if (announcement.actionUrl?.startsWith('#')) {
    const action = document.createElement('button');
    action.className = 'secondary-button';
    action.type = 'button';
    action.textContent = announcement.actionLabel || 'Open';
    action.addEventListener('click', () => {
      const pageName = hashToPage.get(announcement.actionUrl.slice(1));
      if (pageName) showPage(pageName);
    });
    actions.append(action);
  }
  if (!announcement.read) {
    const read = document.createElement('button');
    read.className = 'secondary-button';
    read.type = 'button';
    read.textContent = 'Mark as read';
    read.addEventListener('click', async () => {
      read.disabled = true;
      try {
        await socialPost(`/api/campus-information/announcements/${announcement.id}/read`);
        await loadAnnouncements();
      } catch (error) {
        showToast(error.message);
        read.disabled = false;
      }
    });
    actions.append(read);
  }
  card.append(visual, content, actions);
  return card;
}

function renderAnnouncements() {
  if (!announcementFeed) return;
  announcementFeed.replaceChildren();
  loadedAnnouncements.forEach((announcement) => announcementFeed.append(announcementCard(announcement)));
  if (!loadedAnnouncements.length) {
    announcementFeed.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#megaphone"/></svg><h3>No matching updates</h3><p>There are no current announcements in this category.</p></div>';
  }
  const unread = loadedAnnouncements.filter((item) => !item.read).length;
  const badge = document.querySelector('#announcementCountBadge');
  if (badge) badge.textContent = `${loadedAnnouncements.length} update${loadedAnnouncements.length === 1 ? '' : 's'} · ${unread} unread`;
  renderDashboardAnnouncements();
}

async function loadAnnouncements(category = '') {
  if (!announcementFeed && !dashboardAnnouncementList) return;
  activeAnnouncementCategory = category || '';
  try {
    const response = await fetch(`/api/campus-information/announcements?category=${encodeURIComponent(category)}`, { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      loadedAnnouncements = [];
      if (announcementFeed) announcementFeed.innerHTML = '<div class="empty-state compact"><h3>Sign in to view campus updates</h3><p>Announcements are targeted to your university account.</p></div>';
      const badge = document.querySelector('#announcementCountBadge');
      if (badge) badge.textContent = 'Sign in required';
      if (dashboardAnnouncementList) dashboardAnnouncementList.innerHTML = '<p class="loading-note">Sign in to load account-specific pilot updates.</p>';
      return;
    }
    if (!response.ok) throw new Error('Campus updates are temporarily unavailable.');
    loadedAnnouncements = await response.json();
    renderAnnouncements();
  } catch (error) {
    if (announcementFeed) announcementFeed.innerHTML = `<div class="empty-state compact"><h3>Updates unavailable</h3><p>${error.message}</p></div>`;
    const badge = document.querySelector('#announcementCountBadge');
    if (badge) badge.textContent = 'Unavailable';
    if (dashboardAnnouncementList) dashboardAnnouncementList.innerHTML = '<p class="loading-note">Campus updates are temporarily unavailable.</p>';
  }
}

document.querySelectorAll('[data-announcement-category]').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('[data-announcement-category]').forEach((item) => item.classList.remove('active'));
    button.classList.add('active');
    activeAnnouncementCategory = button.dataset.announcementCategory || '';
    void loadAnnouncements(activeAnnouncementCategory);
  });
});

const publisherAudience = document.querySelector('#publisherAudience');
const publisherAudienceLabel = document.querySelector('#publisherAudienceLabel');
publisherAudience?.addEventListener('change', () => {
  const allStudents = publisherAudience.value === 'UNIVERSITY';
  publisherAudienceLabel.disabled = allStudents;
  publisherAudienceLabel.required = !allStudents;
  if (allStudents) publisherAudienceLabel.value = '';
});

document.querySelector('#announcementPublisherForm')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const submit = form.querySelector('button[type="submit"]');
  const status = document.querySelector('#publisherStatus');
  const actionUrl = document.querySelector('#publisherActionUrl').value;
  submit.disabled = true;
  status.textContent = 'Publishing the verified announcement…';
  try {
    const published = await socialPost('/api/campus-information/announcements', {
      category: document.querySelector('#publisherCategory').value,
      audienceType: publisherAudience.value,
      audienceLabel: publisherAudienceLabel.value || null,
      title: document.querySelector('#publisherTitle').value,
      body: document.querySelector('#publisherBody').value,
      actionLabel: actionUrl ? 'Open details' : null,
      actionUrl: actionUrl || null,
      pinned: document.querySelector('#publisherPinned').checked,
      urgent: document.querySelector('#publisherUrgent').checked,
      expiresAt: document.querySelector('#publisherExpiry').value
        ? new Date(document.querySelector('#publisherExpiry').value).toISOString() : null
    });
    form.reset();
    publisherAudience.dispatchEvent(new Event('change'));
    status.textContent = `Published “${published.title}” to ${published.audienceLabel}.`;
    await loadAnnouncements();
  } catch (error) {
    status.textContent = error.message;
  } finally {
    submit.disabled = false;
  }
});

function renderDashboardMeetings() {
  if (!dashboardMeetingList) return;
  dashboardMeetingList.replaceChildren();
  loadedMeetings.slice(0, 2).forEach((meeting) => {
    const row = document.createElement('button');
    row.type = 'button';
    row.className = 'dashboard-meeting';
    const icon = document.createElement('span');
    icon.className = 'meeting-time-icon';
    icon.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#video"/></svg>';
    const copy = document.createElement('span');
    const title = document.createElement('strong');
    title.textContent = meeting.title;
    const meta = document.createElement('small');
    meta.textContent = `${informationDate(meeting.startsAt)} · ${meeting.hostName}`;
    copy.append(title, meta);
    row.append(icon, copy);
    row.addEventListener('click', () => { showPage('live-classes'); void loadMeetingRoom(meeting.id); });
    dashboardMeetingList.append(row);
  });
}

function renderMeetings() {
  if (!meetingList) return;
  meetingList.replaceChildren();
  loadedMeetings.forEach((meeting) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `meeting-list-item ${meeting.id === selectedMeetingId ? 'active' : ''}`;
    const date = document.createElement('time');
    date.dateTime = meeting.startsAt;
    date.textContent = new Intl.DateTimeFormat('en-GB', { day: '2-digit', month: 'short' }).format(new Date(meeting.startsAt));
    const copy = document.createElement('span');
    const title = document.createElement('strong');
    title.textContent = meeting.title;
    const meta = document.createElement('small');
    meta.textContent = `${informationDate(meeting.startsAt, { weekday: 'short' })} · ${meeting.hostName}`;
    copy.append(title, meta);
    const status = document.createElement('span');
    status.className = meeting.joined ? 'pill success' : 'pill info';
    status.textContent = meeting.joined ? 'Joined' : informationLabel(meeting.meetingType);
    button.append(date, copy, status);
    button.addEventListener('click', () => void loadMeetingRoom(meeting.id));
    meetingList.append(button);
  });
  if (!loadedMeetings.length) meetingList.innerHTML = '<p class="loading-note">No scheduled classes are available.</p>';
  const badge = document.querySelector('#meetingCountBadge');
  if (badge) badge.textContent = `${loadedMeetings.length} scheduled`;
  renderDashboardMeetings();
}

async function loadMeetings() {
  if (!meetingList && !dashboardMeetingList) return;
  try {
    const response = await fetch('/api/campus-information/meetings', { headers: { Accept: 'application/json' } });
    if (response.status === 401) {
      if (meetingList) meetingList.innerHTML = '<p class="loading-note">Sign in to view your scheduled classes.</p>';
      const badge = document.querySelector('#meetingCountBadge');
      if (badge) badge.textContent = 'Sign in required';
      if (dashboardMeetingList) dashboardMeetingList.innerHTML = '<p class="loading-note">Sign in to load your scheduled pilot sessions.</p>';
      return;
    }
    if (!response.ok) throw new Error('Scheduled classes are temporarily unavailable.');
    loadedMeetings = await response.json();
    renderMeetings();
  } catch (error) {
    if (meetingList) meetingList.innerHTML = `<p class="loading-note">${error.message}</p>`;
    const badge = document.querySelector('#meetingCountBadge');
    if (badge) badge.textContent = 'Unavailable';
    if (dashboardMeetingList) dashboardMeetingList.innerHTML = '<p class="loading-note">Scheduled classes are temporarily unavailable.</p>';
  }
}

function renderMeetingMessages(messages) {
  const list = document.querySelector('#classMessageList');
  list.replaceChildren();
  messages.filter((message) => message.messageType === selectedClassMessageType).forEach((message) => {
    const article = document.createElement('article');
    article.className = `class-message ${message.messageType.toLowerCase()}`;
    const heading = document.createElement('div');
    const author = document.createElement('strong');
    author.textContent = message.senderName;
    const time = document.createElement('time');
    time.textContent = new Date(message.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    heading.append(author, time);
    const body = document.createElement('p');
    body.textContent = message.body;
    article.append(heading, body);
    list.append(article);
  });
  if (!list.children.length) list.innerHTML = `<p>No ${selectedClassMessageType === 'QUESTION' ? 'questions' : 'messages'} yet.</p>`;
}

function renderMeetingRoom(room) {
  currentMeetingRoom = room;
  selectedMeetingId = room.meeting.id;
  document.querySelector('#classroomEmpty').hidden = true;
  document.querySelector('#classroom').hidden = false;
  document.querySelector('#classroomModule').textContent = room.meeting.moduleCode || informationLabel(room.meeting.meetingType);
  document.querySelector('#classroomTitle').textContent = room.meeting.title;
  document.querySelector('#classroomHost').textContent = `${room.meeting.hostName} · ${informationDate(room.meeting.startsAt)}`;
  document.querySelector('#classroomAgenda').textContent = room.meeting.agenda || 'The session agenda will be added by the host.';
  document.querySelector('#classroomProviderStatus').textContent = room.providerStatus;
  document.querySelector('#classroomStageTitle').textContent = room.meeting.joined ? 'You are in the class' : 'Ready to join';
  document.querySelector('#classroomStatus').textContent = room.meeting.joined ? 'In class' : 'Not joined';
  document.querySelector('#classroomStatus').className = room.meeting.joined ? 'pill success' : 'pill neutral';
  const join = document.querySelector('#meetingJoin');
  const leave = document.querySelector('#meetingLeave');
  const hand = document.querySelector('#meetingHand');
  join.disabled = room.meeting.joined;
  leave.disabled = !room.meeting.joined;
  hand.disabled = !room.meeting.joined;
  hand.classList.toggle('active', room.meeting.handRaised);
  hand.querySelector('span').textContent = room.meeting.handRaised ? 'Lower hand' : 'Raise hand';
  const input = document.querySelector('#classMessageInput');
  input.disabled = !room.meeting.joined;
  input.placeholder = room.meeting.joined
    ? (selectedClassMessageType === 'QUESTION' ? 'Ask a question…' : 'Write to the class…')
    : 'Join the class to participate';
  document.querySelector('#classMessageForm button').disabled = !room.meeting.joined;
  const provider = document.querySelector('#classroomProviderLink');
  provider.replaceChildren();
  if (room.providerJoinUrl) {
    const link = document.createElement('a');
    link.className = 'primary-button meeting-provider-button';
    link.href = room.providerJoinUrl;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.textContent = 'Open secure video room';
    provider.append(link);
  } else {
    const note = document.createElement('span');
    note.className = 'provider-pending';
    note.textContent = 'Video room awaiting an approved university provider';
    provider.append(note);
  }
  const participants = document.querySelector('#participantList');
  participants.replaceChildren();
  room.participants.forEach((person) => {
    const item = document.createElement('li');
    const avatar = document.createElement('span');
    avatar.className = 'participant-avatar';
    avatar.textContent = initialsFor(person.displayName);
    const copy = document.createElement('span');
    const name = document.createElement('strong');
    name.textContent = person.displayName;
    const state = document.createElement('small');
    state.textContent = `${informationLabel(person.role)} · ${person.present ? 'In class' : 'Left class'}`;
    copy.append(name, state);
    item.append(avatar, copy);
    if (person.handRaised) {
      const raised = document.createElement('span');
      raised.className = 'hand-raised';
      raised.innerHTML = '<svg class="icon" aria-hidden="true"><use href="#hand"/></svg><span>Hand raised</span>';
      item.append(raised);
    }
    participants.append(item);
  });
  document.querySelector('#participantCount').textContent = `${room.participants.filter((person) => person.present).length} present`;
  renderMeetingMessages(room.messages);
  renderMeetings();
}

async function loadMeetingRoom(meetingId, { quiet = false } = {}) {
  try {
    const response = await fetch(`/api/campus-information/meetings/${meetingId}`, { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('The classroom could not be opened.');
    renderMeetingRoom(await response.json());
  } catch (error) {
    if (!quiet) showToast(error.message);
  }
}

document.querySelector('#meetingJoin')?.addEventListener('click', async () => {
  try {
    renderMeetingRoom(await socialPost(`/api/campus-information/meetings/${selectedMeetingId}/join`));
    await loadMeetings();
  } catch (error) { showToast(error.message); }
});

document.querySelector('#meetingLeave')?.addEventListener('click', async () => {
  try {
    renderMeetingRoom(await socialPost(`/api/campus-information/meetings/${selectedMeetingId}/leave`));
    await loadMeetings();
  } catch (error) { showToast(error.message); }
});

document.querySelector('#meetingHand')?.addEventListener('click', async () => {
  try {
    const raised = !currentMeetingRoom.meeting.handRaised;
    renderMeetingRoom(await socialPost(`/api/campus-information/meetings/${selectedMeetingId}/hand`, { raised }));
  } catch (error) { showToast(error.message); }
});

document.querySelectorAll('[data-class-message-type]').forEach((button) => {
  button.addEventListener('click', () => {
    selectedClassMessageType = button.dataset.classMessageType;
    document.querySelectorAll('[data-class-message-type]').forEach((item) => item.classList.remove('active'));
    button.classList.add('active');
    if (currentMeetingRoom) renderMeetingRoom(currentMeetingRoom);
  });
});

document.querySelector('#classMessageForm')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const input = document.querySelector('#classMessageInput');
  const body = input.value.trim();
  if (!body || !selectedMeetingId) return;
  try {
    await socialPost(`/api/campus-information/meetings/${selectedMeetingId}/messages`, {
      messageType: selectedClassMessageType, body
    });
    input.value = '';
    await loadMeetingRoom(selectedMeetingId);
  } catch (error) { showToast(error.message); }
});

function renderSupportCases(cases) {
  if (!supportCaseList) return;
  supportCaseList.replaceChildren();
  cases.forEach((supportCase) => {
    const card = document.createElement('article');
    card.className = 'support-case-card';
    const heading = document.createElement('div');
    const title = document.createElement('h4');
    title.textContent = supportCase.subject;
    const status = document.createElement('span');
    status.className = supportCase.status === 'RESOLVED' ? 'pill success' : 'pill info';
    status.textContent = informationLabel(supportCase.status);
    heading.append(title, status);
    const meta = document.createElement('small');
    meta.textContent = `${supportCase.referenceCode} · ${supportCase.assignedTeam} · ${informationDate(supportCase.updatedAt)}`;
    const description = document.createElement('p');
    description.textContent = supportCase.description;
    const replies = document.createElement('div');
    replies.className = 'support-replies';
    supportCase.replies.forEach((reply) => {
      const replyRow = document.createElement('p');
      const author = document.createElement('strong');
      author.textContent = `${reply.authorName}: `;
      replyRow.append(author, document.createTextNode(reply.body));
      replies.append(replyRow);
    });
    card.append(heading, meta, description, replies);
    supportCaseList.append(card);
  });
  if (!cases.length) supportCaseList.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#mail"/></svg><h3>No support requests</h3><p>Create a private request when you need the right university team.</p></div>';
}

async function loadSupportCases() {
  if (!supportCaseList) return;
  try {
    const response = await fetch('/api/campus-information/support-cases', { headers: { Accept: 'application/json' } });
    if (response.status === 401) {
      supportCaseList.innerHTML = '<div class="empty-state compact"><h3>Sign in to contact support</h3><p>Your requests are private to your account.</p></div>';
      return;
    }
    if (!response.ok) throw new Error('Support tracking is temporarily unavailable.');
    renderSupportCases(await response.json());
  } catch (error) {
    supportCaseList.innerHTML = `<div class="empty-state compact"><p>${error.message}</p></div>`;
  }
}

document.querySelector('#supportFormFocus')?.addEventListener('click', () => {
  document.querySelector('#supportCategory')?.focus();
});

document.querySelectorAll('.support-route').forEach((button) => {
  button.addEventListener('click', () => {
    const category = document.querySelector('#supportCategory');
    if (category) category.value = button.dataset.supportCategory || '';
    document.querySelector('#supportSubject')?.focus();
    document.querySelector('#supportCaseForm')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  });
});

document.querySelector('#supportRefresh')?.addEventListener('click', () => void loadSupportCases());

document.querySelector('#supportCaseForm')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  const status = document.querySelector('#supportFormStatus');
  const form = event.currentTarget;
  const submit = form.querySelector('button[type="submit"]');
  submit.disabled = true;
  status.textContent = 'Creating your private request…';
  try {
    const created = await socialPost('/api/campus-information/support-cases', {
      category: document.querySelector('#supportCategory').value,
      subject: document.querySelector('#supportSubject').value,
      description: document.querySelector('#supportDescription').value
    });
    form.reset();
    status.textContent = `${created.referenceCode} created and assigned to ${created.assignedTeam}.`;
    await loadSupportCases();
  } catch (error) {
    status.textContent = error.message;
  } finally {
    submit.disabled = false;
  }
});

let sharingLocation = false;

function locationText(value) {
  locationStatus.textContent = value;
}

function geolocate() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('This browser does not support location.'));
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, () => reject(new Error(
      'Location permission was denied or your position could not be determined. Check browser settings.')),
    { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 });
  });
}

async function publishLocation() {
  const position = await geolocate();
  const result = await socialPost('/api/location/me', {
    latitude: position.coords.latitude,
    longitude: position.coords.longitude,
    accuracyMetres: Math.min(10000, Math.max(0, Math.round(position.coords.accuracy)))
  });
  sharingLocation = true;
  locationStart.disabled = true;
  locationStop.disabled = false;
  locationText(`Sharing with accepted connections who are also sharing, until ${new Date(result.expiresAt).toLocaleTimeString()}.`);
  await loadFriendLocations();
}

async function loadFriendLocations() {
  const response = await fetch('/api/location/connections', { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('Friend locations could not be loaded.');
  const friends = await response.json();
  friendLocations.replaceChildren();
  if (!friends.length) {
    const empty = document.createElement('p');
    empty.textContent = sharingLocation ? 'No accepted connections are sharing right now.'
      : 'Choose to share first. Only people who are also sharing will appear.';
    friendLocations.append(empty);
    return;
  }
  friends.forEach((friend) => {
    const row = document.createElement('div');
    row.className = 'friend-location';
    const title = document.createElement('strong');
    title.textContent = friend.displayName;
    const details = document.createElement('small');
    details.textContent = `Shared ${new Date(friend.updatedAt).toLocaleTimeString()} · device accuracy about ${friend.accuracyMetres} m`;
    const map = document.createElement('a');
    map.href = `https://www.openstreetmap.org/?mlat=${friend.latitude}&mlon=${friend.longitude}#map=17/${friend.latitude}/${friend.longitude}`;
    map.target = '_blank';
    map.rel = 'noopener noreferrer';
    map.textContent = 'Open position on map';
    const mapIcon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    mapIcon.classList.add('icon');
    mapIcon.setAttribute('aria-hidden', 'true');
    const mapUse = document.createElementNS('http://www.w3.org/2000/svg', 'use');
    mapUse.setAttribute('href', '#chevron-right');
    mapIcon.append(mapUse);
    map.append(mapIcon);
    row.append(title, details, map);
    friendLocations.append(row);
  });
}

async function loadLocationPanel() {
  if (!pilotMode) return;
  try {
    const response = await fetch('/api/location/me', { headers: { Accept: 'application/json' } });
    if (response.status === 401) {
      locationText('Sign in with your pilot account to use location sharing.');
      return;
    }
    if (!response.ok) throw new Error('Location controls are unavailable.');
    const current = await response.json();
    sharingLocation = current.sharing;
    locationStart.disabled = current.sharing;
    locationStop.disabled = !current.sharing;
    locationRefresh.disabled = false;
    locationText(current.sharing
      ? `Sharing until ${new Date(current.expiresAt).toLocaleTimeString()}. Turn it off at any time.`
      : 'Your location is not being shared.');
    await loadFriendLocations();
  } catch (error) {
    locationText(error.message);
  }
}

locationStart?.addEventListener('click', async () => {
  locationStart.disabled = true;
  locationText('Asking your browser for location permission…');
  try {
    await publishLocation();
    clearInterval(locationRefreshTimer);
    locationRefreshTimer = setInterval(() => {
      if (document.visibilityState === 'visible' && !document.querySelector('#map-page').hidden)
        void publishLocation().catch((error) => locationText(error.message));
    }, 60000);
  } catch (error) {
    locationStart.disabled = false;
    locationText(error.message);
  }
});

locationStop?.addEventListener('click', async () => {
  try {
    const csrf = await csrfToken();
    const response = await fetch('/api/location/me', {
      method: 'DELETE', headers: { [csrf.headerName]: csrf.token }
    });
    if (!response.ok) throw new Error('Could not stop sharing. Try again.');
    clearInterval(locationRefreshTimer);
    sharingLocation = false;
    locationStart.disabled = false;
    locationStop.disabled = true;
    locationText('Location sharing is off. Your saved position was deleted.');
    await loadFriendLocations();
  } catch (error) {
    locationText(error.message);
  }
});

locationRefresh?.addEventListener('click', () => void loadLocationPanel());

const pilotTestList = document.querySelector('#pilotTestList');

function pilotTestStatusLabel(status) {
  return { PASS: 'Passed', FAIL: 'Failed', BLOCKED: 'Blocked', NOT_RUN: 'Not tested' }[status] || 'Not tested';
}

function updatePilotTestSummary(plan) {
  document.querySelector('#pilotTestTotal').textContent = plan.total;
  document.querySelector('#pilotTestPassed').textContent = plan.passed;
  document.querySelector('#pilotTestFailed').textContent = plan.failed;
  document.querySelector('#pilotTestRemaining').textContent = plan.blocked + plan.notRun;
}

function pilotTestCard(scenario, index) {
  const card = document.createElement('article');
  card.className = 'pilot-test-card';
  card.dataset.status = scenario.status;

  const number = document.createElement('span');
  number.className = 'pilot-test-number';
  number.textContent = String(index + 1).padStart(2, '0');

  const copy = document.createElement('div');
  copy.className = 'pilot-test-copy';
  const heading = document.createElement('div');
  heading.className = 'pilot-test-heading';
  const titleWrap = document.createElement('div');
  const area = document.createElement('span');
  area.className = 'eyebrow';
  area.textContent = scenario.area;
  const title = document.createElement('h3');
  title.textContent = scenario.title;
  titleWrap.append(area, title);
  const status = document.createElement('span');
  status.className = `pilot-test-status ${scenario.status.toLowerCase().replace('_', '-')}`;
  status.textContent = pilotTestStatusLabel(scenario.status);
  heading.append(titleWrap, status);

  const instruction = document.createElement('p');
  instruction.textContent = scenario.instruction;
  const expected = document.createElement('div');
  expected.className = 'pilot-test-expected';
  const expectedTitle = document.createElement('strong');
  expectedTitle.textContent = 'Expected result: ';
  expected.append(expectedTitle, document.createTextNode(scenario.expectedResult));

  const form = document.createElement('form');
  form.className = 'pilot-test-form';
  const statusLabel = document.createElement('label');
  statusLabel.append(document.createTextNode('Result'));
  const select = document.createElement('select');
  select.setAttribute('aria-label', `${scenario.title} result`);
  [['NOT_RUN', 'Not tested'], ['PASS', 'Pass'], ['FAIL', 'Fail'], ['BLOCKED', 'Blocked']].forEach(([value, label]) => {
    const option = document.createElement('option');
    option.value = value;
    option.textContent = label;
    option.selected = value === scenario.status;
    select.append(option);
  });
  statusLabel.append(select);
  const deviceLabel = document.createElement('label');
  deviceLabel.append(document.createTextNode('Device or browser'));
  const device = document.createElement('input');
  device.maxLength = 180;
  device.placeholder = 'e.g. iPhone and Chrome';
  device.value = scenario.deviceLabel || '';
  deviceLabel.append(device);
  const noteLabel = document.createElement('label');
  noteLabel.append(document.createTextNode('Evidence note'));
  const note = document.createElement('input');
  note.maxLength = 2000;
  note.placeholder = 'What happened? Do not enter sensitive information.';
  note.value = scenario.evidenceNote || '';
  noteLabel.append(note);
  const save = document.createElement('button');
  save.type = 'submit';
  save.textContent = 'Save result';
  form.append(statusLabel, deviceLabel, noteLabel, save);

  const metadata = document.createElement('small');
  metadata.className = 'pilot-test-meta';
  metadata.textContent = scenario.testedAt
    ? `Last tested ${new Date(scenario.testedAt).toLocaleString()}`
    : 'No result recorded for this account.';

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    save.disabled = true;
    try {
      const csrf = await csrfToken();
      const response = await fetch(`/api/pilot-tests/${encodeURIComponent(scenario.code)}`, {
        method: 'PUT',
        headers: {
          Accept: 'application/json', 'Content-Type': 'application/json',
          [csrf.headerName]: csrf.token
        },
        body: JSON.stringify({ status: select.value, evidenceNote: note.value, deviceLabel: device.value })
      });
      if (response.status === 401) throw new Error('Sign in with a staging account to save results.');
      if (!response.ok) throw new Error('The test result could not be saved. Check the information and try again.');
      renderPilotTests(await response.json());
      showToast(`${scenario.title} was recorded as ${pilotTestStatusLabel(select.value).toLowerCase()}.`);
    } catch (error) {
      showToast(error.message);
    } finally {
      save.disabled = false;
    }
  });

  copy.append(heading, instruction, expected, form, metadata);
  card.append(number, copy);
  return card;
}

function renderPilotTests(plan) {
  updatePilotTestSummary(plan);
  pilotTestList.replaceChildren(...plan.scenarios.map(pilotTestCard));
}

async function loadPilotTests() {
  if (!pilotTestList) return;
  try {
    const response = await fetch('/api/pilot-tests', { headers: { Accept: 'application/json' } });
    if (response.status === 401) throw new Error('Sign in with a staging account to use the Pilot Test Centre.');
    if (!response.ok) throw new Error('The pilot test plan is temporarily unavailable.');
    renderPilotTests(await response.json());
  } catch (error) {
    pilotTestList.replaceChildren();
    const state = document.createElement('div');
    state.className = 'empty-state compact';
    const heading = document.createElement('h3');
    heading.textContent = 'Test plan unavailable';
    const message = document.createElement('p');
    message.textContent = error.message;
    state.append(heading, message);
    pilotTestList.append(state);
  }
}

const adminMetrics = document.querySelector('#adminMetrics');
const adminUserList = document.querySelector('#adminUserList');
const adminSupportList = document.querySelector('#adminSupportList');
const adminSafetyList = document.querySelector('#adminSafetyList');
const adminAuditList = document.querySelector('#adminAuditList');
const adminCatalogueList = document.querySelector('#adminCatalogueList');
const adminOverviewCatalogue = document.querySelector('#adminOverviewCatalogue');
const adminMeetingAssignees = document.querySelector('#adminMeetingAssignees');
const adminMeetingAssigneesWrap = document.querySelector('#adminMeetingAssigneesWrap');
const adminMeetingAudience = document.querySelector('#adminMeetingAudience');
const adminMeetingAudienceLabel = document.querySelector('#adminMeetingAudienceLabel');
const adminCourseOptions = document.querySelector('#adminCourseOptions');
const adminOpportunityPublisher = document.querySelector('#adminOpportunityPublisher');
const adminModuleProgramme = document.querySelector('#adminModuleProgramme');
const adminActivityStatus = document.querySelector('#adminActivityStatus');
let adminUsers = [];
let adminLoadInProgress = false;

function adminEmpty(title, message) {
  const state = document.createElement('div');
  state.className = 'empty-state compact';
  const heading = document.createElement('h3');
  heading.textContent = title;
  const copy = document.createElement('p');
  copy.textContent = message;
  state.append(heading, copy);
  return state;
}

function adminLabel(value) {
  return String(value || '').toLowerCase().replaceAll('_', ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function adminDate(value) {
  return value ? new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : 'Not recorded';
}

function renderAdminMetrics(overview) {
  const metrics = [
    [overview.activeUsers, 'Active campus accounts'], [overview.publishedAnnouncements, 'Current announcements'],
    [overview.upcomingEvents, 'Upcoming events'], [overview.upcomingMeetings, 'Live classes'],
    [overview.activeServices, 'Bookable services'], [overview.confirmedBookings, 'Confirmed bookings'],
    [overview.activeOpportunities, 'Open opportunities'],
    [overview.openSupportCases + overview.openSafetyReports + overview.pendingLostFoundReports, 'Items needing attention']
  ];
  adminMetrics.replaceChildren(...metrics.map(([value, label]) => {
    const card = document.createElement('article');
    const strong = document.createElement('strong');
    strong.textContent = value;
    const span = document.createElement('span');
    span.textContent = label;
    card.append(strong, span);
    return card;
  }));
}

function adminCatalogueCard(record) {
  const card = document.createElement('article');
  card.className = 'admin-record admin-catalogue-record';
  const copy = document.createElement('div');
  copy.className = 'admin-record-copy';
  const type = document.createElement('span');
  type.className = 'admin-record-type';
  type.textContent = adminLabel(record.recordType);
  const title = document.createElement('h4');
  title.textContent = record.title;
  const detail = document.createElement('p');
  detail.textContent = record.detail;
  const date = document.createElement('small');
  date.textContent = record.effectiveAt ? `Relevant date: ${adminDate(record.effectiveAt)}` : 'No future date has been added';
  copy.append(type, title, detail, date);
  const status = document.createElement('span');
  status.className = 'admin-record-status';
  status.textContent = adminLabel(record.status);
  card.append(copy, status);
  return card;
}

function renderAdminCatalogue(records) {
  const empty = () => adminEmpty('No campus records yet', 'Create an announcement, event, service, session, opportunity or circle to see it here.');
  adminCatalogueList?.replaceChildren(...(records.length ? records.map(adminCatalogueCard) : [empty()]));
  adminOverviewCatalogue?.replaceChildren(...(records.length ? records.slice(0, 5).map(adminCatalogueCard) : [empty()]));
}

function renderAdminUsers(users) {
  adminUsers = users;
  const courses = [...new Set(users.map((user) => user.courseName?.trim()).filter(Boolean))]
    .sort((left, right) => left.localeCompare(right));
  adminCourseOptions?.replaceChildren(...courses.map((course) => new Option(course)));
  adminUserList.replaceChildren();
  if (!users.length) {
    adminUserList.append(adminEmpty('No matching testers', 'Try a different name, email address or course.'));
    return;
  }
  users.forEach((user) => {
    const card = document.createElement('article');
    card.className = 'admin-record';
    const identity = document.createElement('div');
    identity.className = 'admin-record-copy';
    const title = document.createElement('h4');
    title.textContent = user.displayName;
    const detail = document.createElement('p');
    detail.textContent = `${user.email} · ${user.courseName || 'Course not supplied'}${user.yearOfStudy ? ` · Year ${user.yearOfStudy}` : ''}`;
    const created = document.createElement('small');
    created.textContent = `Created ${adminDate(user.createdAt)}`;
    identity.append(title, detail, created);
    const controls = document.createElement('div');
    controls.className = 'admin-record-controls';
    const role = document.createElement('select');
    role.setAttribute('aria-label', `Role for ${user.displayName}`);
    ['STUDENT', 'STAFF', 'SERVICE_PROVIDER', 'ADMIN'].forEach((value) => role.add(new Option(adminLabel(value), value, false, user.role === value)));
    const status = document.createElement('select');
    status.setAttribute('aria-label', `Account status for ${user.displayName}`);
    ['ACTIVE', 'SUSPENDED'].forEach((value) => status.add(new Option(adminLabel(value), value, false, user.status === value)));
    const save = document.createElement('button');
    save.type = 'button';
    save.className = 'secondary-button';
    save.textContent = 'Save access';
    save.addEventListener('click', async () => {
      save.disabled = true;
      try {
        await socialPatch(`/api/admin/users/${user.id}`, { role: role.value, status: status.value });
        showToast(`${user.displayName}'s Test Campus access was updated.`);
        await loadAdminWorkspace();
      } catch (error) {
        showToast(error.message);
      } finally { save.disabled = false; }
    });
    controls.append(role, status, save);
    card.append(identity, controls);
    adminUserList.append(card);
  });
  adminMeetingAssignees.replaceChildren(...users.filter((user) => user.status === 'ACTIVE').map((user) => new Option(`${user.displayName} — ${adminLabel(user.role)}`, user.id)));
}

function adminCaseCard(item, safety = false) {
  const card = document.createElement('article');
  card.className = 'admin-record admin-case';
  const copy = document.createElement('div');
  copy.className = 'admin-record-copy';
  const title = document.createElement('h4');
  title.textContent = safety ? `${adminLabel(item.reason)} report` : `${item.referenceCode} · ${item.subject}`;
  const reporter = document.createElement('p');
  reporter.textContent = safety
    ? `${item.reporterName} reported ${item.reportedAccountName}`
    : `${item.reporterName} · ${adminLabel(item.category)}`;
  const description = document.createElement('p');
  description.textContent = safety ? (item.details || 'No additional details supplied.') : item.description;
  copy.append(title, reporter, description);
  const controls = document.createElement('div');
  controls.className = 'admin-record-controls';
  const status = document.createElement('select');
  const options = safety ? ['OPEN', 'REVIEWING', 'RESOLVED', 'DISMISSED'] : ['OPEN', 'IN_PROGRESS', 'WAITING_FOR_STUDENT', 'RESOLVED', 'CLOSED'];
  options.forEach((value) => status.add(new Option(adminLabel(value), value, false, item.status === value)));
  const save = document.createElement('button');
  save.type = 'button';
  save.className = 'secondary-button';
  save.textContent = 'Update';
  save.addEventListener('click', async () => {
    save.disabled = true;
    try {
      if (safety) await socialPatch(`/api/admin/safety-reports/${item.id}`, { status: status.value, note: 'Status updated through the Test Campus administrator dashboard.' });
      else await socialPatch(`/api/admin/support-cases/${item.id}`, { status: status.value, priority: item.priority, assignedTeam: item.assignedTeam || 'Test Campus Support', reply: '' });
      showToast('The case was updated and recorded in the audit trail.');
      await loadAdminWorkspace();
    } catch (error) { showToast(error.message); }
    finally { save.disabled = false; }
  });
  controls.append(status, save);
  card.append(copy, controls);
  return card;
}

function renderAdminQueues(supportCases, safetyReports) {
  adminSupportList.replaceChildren(...(supportCases.length ? supportCases.map((item) => adminCaseCard(item)) : [adminEmpty('No open support requests', 'New Test Campus support requests will appear here.') ]));
  adminSafetyList.replaceChildren(...(safetyReports.length ? safetyReports.map((item) => adminCaseCard(item, true)) : [adminEmpty('No safety reports', 'Reports from real tester interactions will appear here.') ]));
}

function renderAdminAudit(entries) {
  adminAuditList.replaceChildren(...(entries.length ? entries.map((entry) => {
    const card = document.createElement('article');
    card.className = 'admin-record audit-record';
    const copy = document.createElement('div');
    copy.className = 'admin-record-copy';
    const title = document.createElement('h4');
    title.textContent = adminLabel(entry.actionType);
    const detail = document.createElement('p');
    detail.textContent = entry.summary;
    const meta = document.createElement('small');
    meta.textContent = `${entry.actorName} · ${adminLabel(entry.resourceType)} · ${adminDate(entry.createdAt)}`;
    copy.append(title, detail, meta);
    card.append(copy);
    return card;
  }) : [adminEmpty('No administrator actions yet', 'Sensitive changes will be recorded here.')]));
}

async function adminJson(url) {
  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (response.status === 403) throw new Error('This workspace requires Test Campus administrator access.');
  if (!response.ok) throw new Error('Administrator records are temporarily unavailable.');
  return response.json();
}

async function loadAdminWorkspace() {
  if (!adminAccess || adminLoadInProgress || !adminMetrics) return;
  adminLoadInProgress = true;
  try {
    const [overview, users, supportCases, safetyReports, audit, publishers, programmes, catalogue] = await Promise.all([
      adminJson('/api/admin/overview'), adminJson('/api/admin/users'), adminJson('/api/admin/support-cases'),
      adminJson('/api/admin/safety-reports'), adminJson('/api/admin/audit'), adminJson('/api/admin/opportunity-publishers'),
      adminJson('/api/admin/programmes'), adminJson('/api/admin/catalogue')
    ]);
    renderAdminMetrics(overview);
    renderAdminUsers(users);
    renderAdminQueues(supportCases, safetyReports);
    renderAdminAudit(audit);
    renderAdminCatalogue(catalogue);
    adminOpportunityPublisher.replaceChildren(...publishers.map((publisher) => new Option(`${publisher.name} — ${adminLabel(publisher.type)}`, publisher.id)));
    adminModuleProgramme.replaceChildren(...programmes.map((programme) => new Option(`${programme.code} — ${programme.title}`, programme.id)));
  } catch (error) {
    [adminUserList, adminSupportList, adminSafetyList, adminAuditList, adminCatalogueList, adminOverviewCatalogue]
      .forEach((list) => list?.replaceChildren(adminEmpty('Administrator workspace unavailable', error.message)));
  } finally { adminLoadInProgress = false; }
}

function openAdminPanel(panelName) {
  const selected = document.querySelector(`[data-admin-tab="${panelName}"]`);
  document.querySelectorAll('[data-admin-tab]').forEach((item) => item.classList.toggle('active', item === selected));
  document.querySelectorAll('[data-admin-panel]').forEach((panel) => { panel.hidden = panel.dataset.adminPanel !== panelName; });
  selected?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
}

document.querySelectorAll('[data-admin-tab]').forEach((button) => button.addEventListener('click', () => {
  openAdminPanel(button.dataset.adminTab);
}));
document.querySelectorAll('[data-admin-open]').forEach((button) => button.addEventListener('click', () => {
  openAdminPanel(button.dataset.adminOpen);
}));

let adminUserSearchTimer;
document.querySelector('#adminUserSearch')?.addEventListener('input', (event) => {
  clearTimeout(adminUserSearchTimer);
  adminUserSearchTimer = setTimeout(async () => {
    try { renderAdminUsers(await adminJson(`/api/admin/users?query=${encodeURIComponent(event.target.value.trim())}`)); }
    catch (error) { showToast(error.message); }
  }, 250);
});
document.querySelector('#adminRefresh')?.addEventListener('click', () => loadAdminWorkspace());

function adminIso(id) {
  const value = document.querySelector(id).value;
  return value ? new Date(value).toISOString() : null;
}

function adminTimeRange(startsId, endsId, label) {
  const starts = new Date(document.querySelector(startsId).value);
  const ends = new Date(document.querySelector(endsId).value);
  if (Number.isNaN(starts.getTime()) || Number.isNaN(ends.getTime())) {
    throw new Error(`Choose a valid start and end time for the ${label}.`);
  }
  if (starts.getTime() <= Date.now()) {
    throw new Error(`The ${label} start time must be in the future.`);
  }
  if (ends.getTime() <= starts.getTime()) {
    throw new Error(`The ${label} end time must be after its start time.`);
  }
  return { startsAt: starts.toISOString(), endsAt: ends.toISOString() };
}

async function submitAdminForm(form, button, action) {
  button.disabled = true;
  adminActivityStatus.textContent = 'Saving the Test Campus record…';
  try {
    const message = await action();
    adminActivityStatus.textContent = message;
    form.reset();
    if (form.id === 'adminAnnouncementForm') syncAdminAnnouncementAudience();
    if (form.id === 'adminMeetingForm') syncAdminMeetingAudience();
    await loadAdminWorkspace();
    showToast(message);
  } catch (error) {
    adminActivityStatus.textContent = error.message;
  } finally { button.disabled = false; }
}

const adminAnnouncementAudience = document.querySelector('#adminAnnouncementAudience');
const adminAnnouncementAudienceLabel = document.querySelector('#adminAnnouncementAudienceLabel');
function syncAdminAnnouncementAudience() {
  const targeted = adminAnnouncementAudience.value !== 'UNIVERSITY';
  adminAnnouncementAudienceLabel.disabled = !targeted;
  adminAnnouncementAudienceLabel.required = targeted;
  adminAnnouncementAudienceLabel.placeholder = adminAnnouncementAudience.value === 'COURSE'
    ? 'Exact course name' : adminAnnouncementAudience.value === 'YEAR'
      ? 'Year number, for example 3' : adminAnnouncementAudience.value === 'MODULE'
        ? 'Module code, for example COMP6001' : 'Not needed for all users';
  if (adminAnnouncementAudience.value === 'COURSE') adminAnnouncementAudienceLabel.setAttribute('list', 'adminCourseOptions');
  else adminAnnouncementAudienceLabel.removeAttribute('list');
  if (!targeted) adminAnnouncementAudienceLabel.value = '';
}
adminAnnouncementAudience?.addEventListener('change', syncAdminAnnouncementAudience);

function syncAdminMeetingAudience() {
  const type = adminMeetingAudience.value;
  const selectedPeople = type === 'SELECTED';
  const labelledAudience = !['CAMPUS', 'SELECTED'].includes(type);
  adminMeetingAssigneesWrap.hidden = !selectedPeople;
  adminMeetingAssignees.disabled = !selectedPeople;
  adminMeetingAudienceLabel.disabled = !labelledAudience;
  adminMeetingAudienceLabel.required = labelledAudience;
  adminMeetingAudienceLabel.placeholder = type === 'COURSE'
    ? 'Exact course name' : type === 'YEAR'
      ? 'Year number, for example 3' : type === 'MODULE'
        ? 'Module code, for example COMP6001' : type === 'CIRCLE'
          ? 'Exact circle name' : 'Not needed for all users';
  if (type === 'COURSE') adminMeetingAudienceLabel.setAttribute('list', 'adminCourseOptions');
  else adminMeetingAudienceLabel.removeAttribute('list');
  if (!labelledAudience) adminMeetingAudienceLabel.value = '';
  if (!selectedPeople) [...adminMeetingAssignees.options].forEach((option) => { option.selected = false; });
}
adminMeetingAudience?.addEventListener('change', syncAdminMeetingAudience);
syncAdminMeetingAudience();

document.querySelector('#adminAnnouncementForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  submitAdminForm(form, form.querySelector('[type="submit"]'), async () => {
    const actionUrl = document.querySelector('#adminAnnouncementAction').value;
    const published = await socialPost('/api/admin/announcements', {
      category: document.querySelector('#adminAnnouncementCategory').value,
      audienceType: adminAnnouncementAudience.value,
      audienceLabel: adminAnnouncementAudienceLabel.value,
      title: document.querySelector('#adminAnnouncementTitle').value,
      body: document.querySelector('#adminAnnouncementBody').value,
      actionLabel: actionUrl ? 'Open in UniCircle' : '',
      actionUrl,
      pinned: document.querySelector('#adminAnnouncementPinned').checked,
      urgent: document.querySelector('#adminAnnouncementUrgent').checked,
      expiresAt: adminIso('#adminAnnouncementExpiry')
    });
    activeAnnouncementCategory = '';
    document.querySelectorAll('[data-announcement-category]').forEach((item) => {
      item.classList.toggle('active', (item.dataset.announcementCategory || '') === '');
    });
    await loadAnnouncements(activeAnnouncementCategory);
    const recipients = Number(published.notifiedUsers || 0);
    return `Published successfully. ${recipients} active account${recipients === 1 ? '' : 's'} received a notification, and future matching accounts will see this update automatically.`;
  });
});

document.querySelector('#adminProgrammeForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  submitAdminForm(form, form.querySelector('[type="submit"]'), async () => {
    await socialPost('/api/admin/programmes', {
      code: document.querySelector('#adminProgrammeCode').value,
      title: document.querySelector('#adminProgrammeTitle').value,
      qualification: document.querySelector('#adminProgrammeQualification').value
    });
    return 'The academic programme was created and is ready for modules.';
  });
});

document.querySelector('#adminModuleForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  submitAdminForm(form, form.querySelector('[type="submit"]'), async () => {
    await socialPost('/api/admin/modules', {
      programmeId: adminModuleProgramme.value,
      code: document.querySelector('#adminModuleCode').value,
      title: document.querySelector('#adminModuleTitle').value,
      studyLevel: Number(document.querySelector('#adminModuleLevel').value),
      yearOfStudy: Number(document.querySelector('#adminModuleYear').value)
    });
    return 'The module was created and linked to the selected programme.';
  });
});

document.querySelector('#adminCircleForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  submitAdminForm(form, form.querySelector('[type="submit"]'), async () => {
    await socialPost('/api/admin/circles', {
      name: document.querySelector('#adminCircleName').value,
      description: document.querySelector('#adminCircleDescription').value,
      category: document.querySelector('#adminCircleCategory').value
    });
    return 'The campus circle was created and users were notified.';
  });
});

document.querySelector('#adminEventForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  submitAdminForm(form, form.querySelector('[type="submit"]'), async () => {
    const time = adminTimeRange('#adminEventStarts', '#adminEventEnds', 'event');
    await socialPost('/api/admin/events', { title: document.querySelector('#adminEventTitle').value, summary: document.querySelector('#adminEventSummary').value, category: document.querySelector('#adminEventCategory').value, venue: document.querySelector('#adminEventVenue').value, ...time, capacity: Number(document.querySelector('#adminEventCapacity').value), published: document.querySelector('#adminEventPublished').checked });
    return 'The Test Campus event was created.';
  });
});

document.querySelector('#adminServiceForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  submitAdminForm(form, form.querySelector('[type="submit"]'), async () => {
    const time = adminTimeRange('#adminSlotStarts', '#adminSlotEnds', 'bookable slot');
    const service = await socialPost('/api/admin/services', { serviceType: document.querySelector('#adminServiceType').value, title: document.querySelector('#adminServiceTitle').value, providerName: document.querySelector('#adminServiceProvider').value, description: document.querySelector('#adminServiceDescription').value, location: document.querySelector('#adminServiceLocation').value });
    await socialPost(`/api/admin/services/${service.id}/slots`, { ...time, capacity: Number(document.querySelector('#adminSlotCapacity').value) });
    return 'The service and first bookable slot were created.';
  });
});

document.querySelector('#adminMeetingForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  submitAdminForm(form, form.querySelector('[type="submit"]'), async () => {
    const time = adminTimeRange('#adminMeetingStarts', '#adminMeetingEnds', 'live session');
    const providerJoinUrl = document.querySelector('#adminMeetingProviderUrl').value.trim();
    if (providerJoinUrl) {
      const provider = new URL(providerJoinUrl);
      if (provider.origin === window.location.origin) {
        throw new Error('That is a UniCircle page, not a video-room link. Leave this field blank for the built-in classroom, or enter an approved Teams, Zoom or Google Meet HTTPS link.');
      }
    }
    const meeting = await socialPost('/api/admin/meetings', {
      title: document.querySelector('#adminMeetingTitle').value,
      moduleCode: document.querySelector('#adminMeetingModule').value,
      meetingType: document.querySelector('#adminMeetingType').value,
      ...time,
      locationLabel: document.querySelector('#adminMeetingLocation').value,
      agenda: document.querySelector('#adminMeetingAgenda').value,
      providerJoinUrl,
      audienceType: adminMeetingAudience.value,
      audienceLabel: adminMeetingAudienceLabel.value,
      assigneeIds: [...adminMeetingAssignees.selectedOptions].map((option) => option.value),
      published: document.querySelector('#adminMeetingPublished').checked
    });
    const recipients = Number(meeting.assignedUsers || 0);
    return `The live session was published to ${recipients} active account${recipients === 1 ? '' : 's'} and added to their schedules.`;
  });
});

document.querySelector('#adminOpportunityForm')?.addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  submitAdminForm(form, form.querySelector('[type="submit"]'), async () => {
    await socialPost('/api/admin/opportunities', { publisherId: adminOpportunityPublisher.value, title: document.querySelector('#adminOpportunityTitle').value, summary: document.querySelector('#adminOpportunitySummary').value, description: document.querySelector('#adminOpportunityDescription').value, opportunityType: document.querySelector('#adminOpportunityType').value, workMode: document.querySelector('#adminOpportunityMode').value, location: document.querySelector('#adminOpportunityLocation').value, hoursLabel: document.querySelector('#adminOpportunityHours').value, compensationLabel: document.querySelector('#adminOpportunityCompensation').value, skillsText: document.querySelector('#adminOpportunitySkills').value, eligibility: document.querySelector('#adminOpportunityEligibility').value, closesAt: document.querySelector('#adminOpportunityCloses').value, published: document.querySelector('#adminOpportunityPublished').checked });
    return 'The Test Campus opportunity was created.';
  });
});

fetch('/api/platform/status')
  .then((response) => {
    if (!response.ok) throw new Error('Status endpoint unavailable');
    return response.json();
  })
  .then((status) => {
    document.documentElement.dataset.platformStage = status.stage;
    pilotMode = status.stage === 'UniCircle Staging Campus';
    if (pilotMode) {
      document.body.classList.add('pilot-mode');
      document.querySelector('#pilotWelcome').hidden = false;
      document.querySelector('#pilotTestNav').hidden = false;
      document.querySelector('#notificationStorageNote').textContent =
        'Account-specific pilot updates. Do not share sensitive information while testing.';
      document.querySelector('#messagePrivacyNote').textContent =
        'Messages are stored for the signed-in pilot accounts and update across open sessions. Voice and video calls are live, are not recorded by UniCircle, and require both people to have the app open.';
      document.querySelector('#mapDashboardNote').textContent =
        'Share a five-minute position with accepted connections only when you choose. Both people must opt in.';
      void refreshConversationSummary();
      if (!document.querySelector('#map-page').hidden) void loadLocationPanel();
    } else {
      locationText('Real location sharing is available in the invitation-only pilot, not the fictional sandbox.');
    }
  })
  .catch(() => document.documentElement.dataset.platformStage = 'offline');

const initialPage = hashToPage.get(location.hash.slice(1)) ?? 'campus';
updateCurrentDateAndGreeting();
renderSignedOutAccountStates();
showPage(initialPage, { updateHistory: false, focus: false });

async function restoreSessionIdentity() {
  try {
    const response = await fetch('/api/profile/me', { headers: { Accept: 'application/json' } });
    if (!response.ok) return;
    populateProfile(await response.json());
  } catch {
    // The public preview remains usable when no authenticated session exists.
  }
}

const journeyParams = new URLSearchParams(location.search);
if (journeyParams.has('signed-in')) {
  openProfilePanel();
  history.replaceState(null, '', `${location.pathname}${location.hash || '#campus'}`);
} else {
  restoreSessionIdentity();
}

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/service-worker.js').catch(() => {
      // The full online experience remains available if installation is unsupported.
    });
  });
}
