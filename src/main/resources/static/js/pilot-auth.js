const message = document.querySelector('#authMessage');
const signInButton = document.querySelector('#signInButton');
const registerButton = document.querySelector('#registerButton');
let csrf;

function showMessage(text, state = '') {
  message.textContent = text;
  message.className = `auth-message${state ? ` ${state}` : ''}`;
}

async function preparePilot() {
  try {
    const response = await fetch('/api/security/csrf', { headers: { Accept: 'application/json' } });
    if (!response.ok) throw new Error('Could not prepare secure forms.');
    csrf = await response.json();
    const field = document.querySelector('#loginCsrfField');
    field.name = csrf.parameterName;
    field.value = csrf.token;
    signInButton.disabled = false;
    registerButton.disabled = false;
    showMessage(new URLSearchParams(location.search).has('error')
      ? 'That staging email or password was not accepted.' : 'Secure UniCircle sign-in is ready.',
    new URLSearchParams(location.search).has('error') ? 'error' : 'ready');
  } catch {
    showMessage('Could not prepare the secure forms. Refresh this page.', 'error');
  }
}

document.querySelector('#registrationForm')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  registerButton.disabled = true;
  showMessage('Creating your account…');
  try {
    const email = document.querySelector('#registerEmail').value.trim().toLowerCase();
    const response = await fetch('/api/pilot/register', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
      body: JSON.stringify({
        displayName: document.querySelector('#registerName').value.trim(),
        courseName: document.querySelector('#registerCourse').value.trim(),
        yearOfStudy: Number(document.querySelector('#registerYear').value),
        email,
        password: document.querySelector('#registerPassword').value,
        inviteCode: document.querySelector('#registerInvite').value.trim()
      })
    });
    if (response.status === 409) throw new Error('This email already has a staging account. Sign in above.');
    if (response.status === 400) throw new Error('Check your details and invitation code. Passwords need at least 12 characters.');
    if (!response.ok) throw new Error('Account creation is unavailable. Please try again.');
    document.querySelector('#username').value = email;
    document.querySelector('#password').focus();
    document.querySelector('#registrationForm').reset();
    showMessage('Account created. Enter your new password above to sign in.', 'ready');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    registerButton.disabled = false;
  }
});

preparePilot();
