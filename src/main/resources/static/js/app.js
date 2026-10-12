const menuButton = document.querySelector('#menuButton');
const mobileMoreButton = document.querySelector('#mobileMoreButton');
const sidebar = document.querySelector('#sidebar');
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
const searchEmpty = document.querySelector('#searchEmpty');
let pilotMode = false;
let locationRefreshTimer;

function openModal(dialog) {
  if (dialog.matches('dialog')) {
    if (typeof dialog.showModal === 'function' && !dialog.open) dialog.showModal();
    else dialog.setAttribute('open', '');
  } else {
    dialog.hidden = false;
  }
  dialog.classList.add('dialog-fallback');
  document.body.classList.add('modal-open');
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
}

menuButton?.addEventListener('click', () => {
  const open = sidebar.classList.toggle('open');
  menuButton.setAttribute('aria-expanded', String(open));
  mobileMoreButton?.setAttribute('aria-expanded', String(open));
});

mobileMoreButton?.addEventListener('click', () => {
  const open = sidebar.classList.toggle('open');
  mobileMoreButton.setAttribute('aria-expanded', String(open));
  menuButton?.setAttribute('aria-expanded', String(open));
});

const pageSurfaces = [...document.querySelectorAll('[data-page]')];
const pageLinks = [...document.querySelectorAll('[data-page-link]')];
const hashToPage = new Map([
  ['campus', 'campus'], ['today', 'today'], ['schedule', 'today'],
  ['circles-page', 'circles'], ['messages-page', 'messages'], ['events-page', 'events'],
  ['services-page', 'services'], ['opportunities-page', 'opportunities'], ['map-page', 'map'],
  ['wellbeing-page', 'wellbeing'], ['lost-found-page', 'lost-found'],
  ['lost-found-review-page', 'lost-found-review'], ['assistant-page', 'assistant']
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
  mobileMoreButton?.setAttribute('aria-expanded', 'false');
  notificationPanel.hidden = true;
  notificationButton?.setAttribute('aria-expanded', 'false');
  if (updateHistory) {
    const targetLink = pageLinks.find((link) => link.dataset.pageLink === nextPage.dataset.page);
    history.pushState({ page: nextPage.dataset.page }, '', targetLink?.hash || '#campus');
  }
  window.scrollTo({ top: 0, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
  if (focus && nextPage !== document.querySelector('#campus')) nextPage.focus({ preventScroll: true });
  if (nextPage.dataset.page === 'messages') void loadMessagingWorkspace();
  if (nextPage.dataset.page === 'lost-found-review' && staffReviewAccess) void loadStaffReview();
  if (nextPage.dataset.page === 'map' && pilotMode) void loadLocationPanel();
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
const todayCampusCommitmentCount = document.querySelector('#todayCampusCommitmentCount');
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
const staffReviewDialog = document.querySelector('#staffReviewDialog');
const staffReviewList = document.querySelector('#staffReviewList');
const baseScheduleMarkup = scheduleList?.innerHTML || '';
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
let staffPendingReports = [];
let activeStaffReportId = null;

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
    const [visualClass, iconName] = eventPresentation(event.category);
    const item = document.createElement('li');
    const date = document.createElement('time');
    const parsed = new Date(event.startsAt);
    date.innerHTML = `<strong>${parsed.getDate()}</strong><small>${parsed.toLocaleString('en-GB', { month: 'short' }).toUpperCase()}</small>`;
    const visual = document.createElement('div');
    visual.className = `event-thumb ${visualClass}`;
    visual.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${iconName}"/></svg>`;
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
      return;
    }
    if (!response.ok) throw new Error();
    loadedCampusEvents = await response.json();
    renderCampusEvents();
    renderDashboardEvents();
  } catch {
    eventCatalogue.innerHTML = '<div class="empty-state compact"><h3>Events are temporarily unavailable</h3><p>Please try again shortly.</p></div>';
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
    if (!response.ok) return;
    renderCampusBookings(await response.json());
  } catch {
    bookingActivity.innerHTML = '<div class="empty-state compact"><h3>Booking activity is temporarily unavailable</h3></div>';
  }
}

async function loadCampusSchedule() {
  if (!scheduleList && !todayCommitmentList) return;
  try {
    const response = await fetch('/api/campus-life/schedule', { headers: { Accept: 'application/json' } });
    if (!response.ok) return;
    const activity = await response.json();
    if (scheduleList) scheduleList.innerHTML = baseScheduleMarkup;
    if (todayCommitmentList) todayCommitmentList.replaceChildren();
    if (todayPlannedCount) todayPlannedCount.textContent = String(4 + activity.length);
    if (todayCampusCommitmentCount) todayCampusCommitmentCount.textContent = String(activity.length);
    activity.forEach((item) => {
      if (scheduleList) {
        const row = document.createElement('li');
        const time = document.createElement('time');
        time.innerHTML = `${campusTime(item.startsAt)}<small>${campusTime(item.endsAt)}</small>`;
        const dot = document.createElement('span');
        dot.className = `time-dot ${item.resourceType === 'EVENT' ? 'orange-dot' : 'green-dot'}`;
        const copy = document.createElement('div');
        const title = document.createElement('strong');
        title.textContent = item.title;
        const location = document.createElement('small');
        location.textContent = item.location;
        copy.append(title, location);
        const status = document.createElement('span');
        status.className = 'status attended';
        status.textContent = item.resourceType === 'EVENT' ? 'Registered' : 'Confirmed';
        row.append(time, dot, copy, status);
        scheduleList.append(row);
      }
      if (todayCommitmentList) {
        const card = document.createElement('article');
        card.className = 'booking-record schedule-commitment';
        const icon = document.createElement('span');
        icon.className = `view-icon ${item.resourceType === 'EVENT' ? 'orange' : 'green'}`;
        icon.innerHTML = `<svg class="icon" aria-hidden="true"><use href="#${item.resourceType === 'EVENT' ? 'events' : 'calendar'}"/></svg>`;
        const copy = document.createElement('div');
        const title = document.createElement('h4');
        title.textContent = item.title;
        const details = document.createElement('p');
        details.textContent = `${fullCampusDate(item.startsAt)}–${campusTime(item.endsAt)} · ${item.location}`;
        copy.append(title, details);
        const status = document.createElement('span');
        status.className = `pill ${item.resourceType === 'EVENT' ? 'warning' : 'success'}`;
        status.textContent = item.resourceType === 'EVENT' ? 'Registered event' : 'Confirmed booking';
        card.append(icon, copy, status);
        todayCommitmentList.append(card);
      }
    });
    if (todayCommitmentList && !activity.length) {
      todayCommitmentList.innerHTML = '<div class="empty-state compact"><svg class="icon" aria-hidden="true"><use href="#calendar"/></svg><h3>No additional commitments</h3><p>Events you register for and services you book will appear here automatically.</p></div>';
    }
  } catch {
    if (scheduleList) scheduleList.innerHTML = baseScheduleMarkup;
    if (todayCommitmentList) todayCommitmentList.innerHTML = '<div class="empty-state compact"><h3>Your UniCircle schedule is temporarily unavailable</h3><p>Please try again shortly.</p></div>';
  }
}

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
      return;
    }
    if (!catalogueResponse.ok || !trackerResponse.ok) throw new Error();
    loadedOpportunities = await catalogueResponse.json();
    trackedOpportunities = await trackerResponse.json();
    renderOpportunities();
    renderDashboardOpportunities();
  } catch {
    opportunityCatalogue.innerHTML = '<div class="empty-state compact"><h3>Opportunities are temporarily unavailable</h3><p>Please try again shortly.</p></div>';
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

const assistantAnswers = new Map([
  ['find a quiet study space', 'The prototype would show live room availability from an approved university service. In this fixture, try Library Study Room 3.'],
  ['how do i book a sports facility?', 'Open Book a Service, choose Room / Facility, then select the Sports Centre and an available time.'],
  ['tell me about career opportunities', 'The Opportunities area brings together approved jobs, internships, volunteering and careers events.']
]);

function answerQuestion(question) {
  const normalised = question.trim().toLowerCase();
  const exact = assistantAnswers.get(normalised);
  const response = exact ?? 'This campus guide currently answers the three demonstrated questions. A later approved retrieval service will provide sourced university information.';
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
  const response = assistantAnswers.get(normalised) ?? 'This campus guide currently answers the three demonstrated questions. A later approved retrieval service will provide sourced university information.';
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

function setProfileStatus(message, state = '') {
  profileStatus.textContent = message;
  profileStatus.className = `profile-status${state ? ` ${state}` : ''}`;
}

function showSignedOutActions() {
  profileSignIn.hidden = false;
  profileSignOut.hidden = true;
  staffReviewAccess = false;
  staffReviewNav.hidden = true;
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
  staffReviewAccess = ['STAFF', 'ADMIN'].includes(profile.role);
  staffReviewNav.hidden = !staffReviewAccess;
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
  if (pilotMode) {
    document.querySelector('#universityAccountButton .verified-dot').hidden = true;
  }
  connectSocialRealtime();
  void Promise.all([
    loadNotifications(),
    refreshConversationSummary(),
    loadCampusEvents(),
    loadCampusServices(),
    loadCampusBookings(),
    loadCampusSchedule(),
    loadOpportunities(),
    loadLostFound(),
    ...(staffReviewAccess ? [loadStaffReview()] : [])
  ]);
}

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

async function loadProfile() {
  profileFields.disabled = true;
  clearUniversityContext();
  setProfileStatus('Checking your secure session…');
  try {
    const response = await fetch('/api/profile/me', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      showSignedOutActions();
      setProfileStatus(pilotMode ? 'Sign in with your invited pilot account to continue.'
        : 'This profile is protected. Sign in with the fictional student account to test the complete experience safely.', 'error');
      return;
    }
    if (!response.ok) throw new Error('Your profile is temporarily unavailable.');
    const profile = await response.json();
    populateProfile(profile);
    await loadUniversityContext();
    setProfileStatus(pilotMode ? 'Your pilot profile is ready to edit.' : 'Your verified profile is ready to edit.', 'success');
  } catch (error) {
    showSignedOutActions();
    setProfileStatus(error.message || 'Your profile could not be loaded.', 'error');
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
  if (!response.ok) {
    if (response.status === 400) throw new Error('Check the information supplied and try again.');
    if (response.status === 401) throw new Error('Sign in to continue.');
    if (response.status === 403) throw new Error('This action is not permitted for your account.');
    if (response.status === 409) throw new Error('This request has already been handled or is no longer available.');
    throw new Error('The request could not be completed. Please refresh and try again.');
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

function openProfilePanel() {
  if (!profileDialog) return;
  openModal(profileDialog);
  loadProfile();
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
const audioCallButton = document.querySelector('#audioCallButton');
const videoCallButton = document.querySelector('#videoCallButton');
const callDialog = document.querySelector('#callDialog');
const callPeerName = document.querySelector('#callPeerName');
const callPeerAvatar = document.querySelector('#callPeerAvatar');
const callDirection = document.querySelector('#callDirection');
const callStatus = document.querySelector('#callStatus');
const callNetworkNotice = document.querySelector('#callNetworkNotice');
const remoteCallVideo = document.querySelector('#remoteCallVideo');
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
  notificationSummary.textContent = unread ? `${unread} new update${unread === 1 ? '' : 's'}` : 'You are up to date';
}

function notificationVisual(type) {
  if (type === 'MESSAGE') return ['message', 'message-type'];
  if (type === 'SAFETY') return ['shield', 'opportunity-type'];
  if (type === 'EVENT') return ['events', 'opportunity-type'];
  if (type === 'BOOKING') return ['calendar', 'booking-type'];
  return ['users', 'booking-type'];
}

async function loadNotifications() {
  if (!notificationList) return;
  try {
    const response = await fetch('/api/social/notifications', { headers: { Accept: 'application/json' } });
    if (response.status === 401 || response.status === 403) {
      notificationList.innerHTML = '<li class="notification-empty">Sign in to load your notifications.</li>';
      notificationSummary.textContent = 'Sign in to view updates';
      updateNotificationBadge(0);
      return;
    }
    if (!response.ok) throw new Error();
    const notifications = await response.json();
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
        item.classList.add('notification-actionable');
        item.addEventListener('click', async () => {
          showPage('messages');
          await loadMessagingWorkspace(notification.resourceId);
        });
      } else if (notification.resourceType === 'CONNECTION') {
        item.classList.add('notification-actionable');
        item.addEventListener('click', () => {
          openModal(studentDirectoryDialog);
          void loadStudentDirectory();
        });
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
  'CALL_OFFER', 'CALL_ANSWER', 'CALL_ICE_CANDIDATE', 'CALL_REJECTED',
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
  if (callConfiguration) return callConfiguration;
  const response = await fetch('/api/social/calls/configuration', { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('Call configuration is temporarily unavailable.');
  const result = await response.json();
  callConfiguration = {
    iceServers: (result.iceServers || []).map((server) => ({
      urls: server.urls,
      ...(server.username ? { username: server.username } : {}),
      ...(server.credential ? { credential: server.credential } : {})
    })),
    relayConfigured: Boolean(result.relayConfigured),
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
  socialSocket.send(JSON.stringify({
    type,
    callId: call.id,
    targetAccountId: call.peerAccountId,
    ...details
  }));
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
  updateCallControl(callMute, false, 'mic-off', 'mic', 'Unmute', 'Mute');
  updateCallControl(callCamera, false, 'video-off', 'video', 'Camera on', 'Camera');
  remoteCallVideo.hidden = true;
  remoteCallVideo.srcObject = null;
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
  call?.localStream?.getTracks().forEach((track) => track.stop());
  if (call?.peerConnection) {
    call.peerConnection.ontrack = null;
    call.peerConnection.onicecandidate = null;
    call.peerConnection.onconnectionstatechange = null;
    call.peerConnection.close();
  }
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
    return await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true },
      video: media === 'VIDEO' ? { facingMode: 'user', width: { ideal: 1280 }, height: { ideal: 720 } } : false
    });
  } catch (error) {
    if (error?.name === 'NotAllowedError') throw new Error('Microphone or camera permission was not granted.');
    if (error?.name === 'NotFoundError') throw new Error('A required microphone or camera was not found.');
    throw new Error('Your microphone or camera could not be started.');
  }
}

async function createCallPeer(call) {
  const configuration = await loadCallConfiguration();
  const peer = new RTCPeerConnection({ iceServers: configuration.iceServers });
  call.peerConnection = peer;
  call.localStream.getTracks().forEach((track) => peer.addTrack(track, call.localStream));
  peer.addEventListener('icecandidate', (event) => {
    if (!event.candidate || activeCall !== call) return;
    void sendCallSignal('CALL_ICE_CANDIDATE', {
      candidate: event.candidate.candidate,
      sdpMid: event.candidate.sdpMid,
      sdpMLineIndex: event.candidate.sdpMLineIndex
    }).catch(() => finishCall('The live call connection was lost.'));
  });
  peer.addEventListener('track', (event) => {
    if (activeCall !== call) return;
    const stream = event.streams[0] || new MediaStream([event.track]);
    remoteCallVideo.srcObject = stream;
    void remoteCallVideo.play().catch(() => {});
    if (call.media === 'VIDEO') {
      remoteCallVideo.hidden = false;
      callPeerAvatar.hidden = true;
    }
  });
  peer.addEventListener('connectionstatechange', () => {
    if (activeCall !== call) return;
    if (peer.connectionState === 'connected') {
      clearTimeout(call.ringTimer);
      clearTimeout(call.disconnectTimer);
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
      finishCall('The call connection ended.');
    }
  });
  return peer;
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
    await sendCallSignal('CALL_OFFER', { media, sdp: offer.sdp });
    callStatus.textContent = `Ringing ${call.peerName}…`;
    call.ringTimer = window.setTimeout(() => {
      if (activeCall === call && call.phase === 'ringing') {
        void sendCallSignal('CALL_CANCELLED').catch(() => {});
        finishCall('No answer.');
      }
    }, 30000);
  } catch (error) {
    finishCall(error.message || 'The call could not be started.', 2200);
  }
}

async function acceptIncomingCall() {
  const call = activeCall;
  if (!call || call.direction !== 'incoming') return;
  callAccept.disabled = true;
  callDecline.disabled = true;
  callStatus.textContent = 'Starting your microphone and camera…';
  try {
    call.localStream = await requestCallMedia(call.media);
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
    await sendCallSignal('CALL_ANSWER', { sdp: answer.sdp });
  } catch (error) {
    await sendCallSignal('CALL_REJECTED').catch(() => {});
    finishCall(error.message || 'The call could not be answered.', 2200);
  } finally {
    callAccept.disabled = false;
    callDecline.disabled = false;
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
  if (signal.type === 'CALL_ANSWER') {
    if (activeCall.direction !== 'outgoing' || !activeCall.peerConnection) return;
    clearTimeout(activeCall.ringTimer);
    activeCall.phase = 'connecting';
    callStatus.textContent = 'Connecting securely…';
    await activeCall.peerConnection.setRemoteDescription({ type: 'answer', sdp: signal.sdp });
    await flushCallCandidates(activeCall);
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
      renderConversationList();
      conversationListStatus.textContent = 'Sign in to load your private conversations.';
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
    activeConversationId = selected?.conversationId || null;
    renderConversationList();
    if (selected) await selectConversation(selected.conversationId);
    else emptyConversation();
  } catch (error) {
    conversationListStatus.textContent = error.message;
    emptyConversation('Please refresh the page and try again.');
  }
}

async function selectConversation(conversationId) {
  activeConversationId = conversationId;
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
    map.textContent = 'Open position on map ↗';
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

fetch('/api/platform/status')
  .then((response) => {
    if (!response.ok) throw new Error('Status endpoint unavailable');
    return response.json();
  })
  .then((status) => {
    document.documentElement.dataset.platformStage = status.stage;
    pilotMode = status.stage === 'Invitation-only campus pilot';
    if (pilotMode) {
      document.body.classList.add('pilot-mode');
      document.querySelector('#pilotWelcome').hidden = false;
      document.querySelector('#environmentBanner').textContent =
        'Invitation-only campus pilot — real test accounts and workflows; no university identity or official campus systems are connected.';
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
