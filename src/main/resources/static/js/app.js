const menuButton = document.querySelector('#menuButton');
const sidebar = document.querySelector('#sidebar');
const notificationButton = document.querySelector('#notificationButton');
const notificationPanel = document.querySelector('#notificationPanel');
const notificationClose = document.querySelector('#notificationClose');
const markAllRead = document.querySelector('#markAllRead');
const notificationBadge = document.querySelector('#notificationBadge');
const globalSearch = document.querySelector('#globalSearch');
const searchEmpty = document.querySelector('#searchEmpty');

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
  if (nextPage.dataset.page === 'messages') void loadMessagingWorkspace();
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
}

function showSignedInActions() {
  profileSignIn.hidden = true;
  profileSignOut.hidden = false;
}

function initialsFor(name) {
  return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase()).join('') || 'U';
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
      setProfileStatus('This profile is protected. Sign in with the fictional student account to test the complete experience safely.', 'error');
      return;
    }
    if (!response.ok) throw new Error('Your profile is temporarily unavailable.');
    const profile = await response.json();
    populateProfile(profile);
    await loadUniversityContext();
    setProfileStatus('Your verified profile is ready to edit.', 'success');
  } catch (error) {
    showSignedOutActions();
    setProfileStatus(error.message || 'Your profile could not be loaded.', 'error');
  }
}

async function csrfToken() {
  const response = await fetch('/api/security/csrf', { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('The security token could not be prepared. Please try again.');
  return response.json();
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
    if (response.status === 401) throw new Error('Sign in to use student connections and messages.');
    if (response.status === 403) throw new Error('This action is not allowed until both students are connected.');
    if (response.status === 409) throw new Error('This request has already been handled or is no longer available.');
    throw new Error('The request could not be completed. Please refresh and try again.');
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
const studentDirectoryDialog = document.querySelector('#studentDirectoryDialog');
const studentDirectoryList = document.querySelector('#studentDirectoryList');
const studentDirectorySearch = document.querySelector('#studentDirectorySearch');
let loadedConversations = [];
let activeConversationId = null;
let directoryTimer;

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

function emptyConversation(message = 'Select a conversation or find a student and connect.') {
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
    button.append(avatar, details, time);
    button.addEventListener('click', () => selectConversation(conversation.conversationId));
    item.append(button);
    conversationList.append(item);
  });
  conversationListStatus.textContent = visible.length ? '' : query
    ? 'No conversations match your search.'
    : 'No conversations yet. Find a student and send a connection request.';
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
    conversationName.textContent = result.conversation.participantDisplayName;
    conversationAvatar.textContent = result.conversation.participantInitials;
    conversationTrust.innerHTML = '<i class="online-dot"></i> Accepted connection · private conversation';
    conversationMessages.replaceChildren();
    if (!result.messages.length) {
      emptyConversation('You are connected. Send the first message.');
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
    messageInput.placeholder = `Message ${result.conversation.participantDisplayName}`;
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
      const details = document.createElement('span');
      details.className = 'directory-person-details';
      const name = document.createElement('strong');
      name.textContent = student.displayName;
      const course = document.createElement('span');
      course.textContent = student.courseName || 'University student';
      const meta = document.createElement('small');
      const status = recentlyJoined(student.joinedAt) ? 'New to UniCircle' : 'Verified fictional student';
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
document.querySelector('#studentDirectoryClose')?.addEventListener('click', () => closeModal(studentDirectoryDialog));
studentDirectorySearch?.addEventListener('input', () => {
  clearTimeout(directoryTimer);
  directoryTimer = setTimeout(loadStudentDirectory, 220);
});

document.querySelectorAll('.message-button').forEach((button) => {
  button.addEventListener('click', () => {
    showPage('messages');
    openModal(studentDirectoryDialog);
    studentDirectorySearch.value = button.dataset.person || '';
    void loadStudentDirectory();
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
