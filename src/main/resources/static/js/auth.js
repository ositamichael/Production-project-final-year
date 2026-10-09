const message = document.querySelector('#authMessage');
const form = document.querySelector('#demoLoginForm');
const csrfField = document.querySelector('#csrfField');
const signInButton = document.querySelector('#signInButton');
const password = document.querySelector('#password');

function setMessage(text, state = '') {
  message.textContent = text;
  message.className = `auth-message${state ? ` ${state}` : ''}`;
}

async function prepareSignIn() {
  const params = new URLSearchParams(location.search);
  if (params.has('error')) {
    setMessage('The fictional email or password was not accepted. Fill the demonstration account and try again.', 'error');
  }
  try {
    const response = await fetch('/api/security/csrf', { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error();
    const csrf = await response.json();
    csrfField.name = csrf.parameterName;
    csrfField.value = csrf.token;
    signInButton.disabled = false;
    if (!params.has('error')) setMessage('Secure demonstration sign-in is ready.', 'ready');
  } catch {
    setMessage('Secure sign-in could not be prepared. Refresh this page and try again.', 'error');
  }
}

document.querySelector('#fillDemoAccount')?.addEventListener('click', () => {
  password.value = 'CampusDemo!2026';
  password.focus();
  setMessage('Fictional password filled. Select “Enter fictional campus” to continue.', 'ready');
});

document.querySelector('#revealPassword')?.addEventListener('click', (event) => {
  const revealing = password.type === 'password';
  password.type = revealing ? 'text' : 'password';
  event.currentTarget.textContent = revealing ? 'Hide' : 'Show';
  event.currentTarget.setAttribute('aria-label', revealing ? 'Hide password' : 'Show password');
});

form?.addEventListener('submit', () => {
  signInButton.disabled = true;
  signInButton.textContent = 'Opening fictional campus…';
});

prepareSignIn();
