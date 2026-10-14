const message = document.querySelector('#authMessage');
const signInButton = document.querySelector('#signInButton');
const registerButton = document.querySelector('#registerButton');
const resetPasswordButton = document.querySelector('#resetPasswordButton');
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
    resetPasswordButton.disabled = false;
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
        password: document.querySelector('#registerPassword').value
      })
    });
    const problem = response.ok ? null : await response.json().catch(() => null);
    if (problem?.code === 'VALIDATION_ERROR') {
      const labels = {
        displayName: 'name', courseName: 'course', yearOfStudy: 'year of study',
        email: 'email address', password: 'password'
      };
      const first = Object.entries(problem.fields || {})[0];
      const detail = first ? `${labels[first[0]] || first[0]} ${first[1]}` : problem.message;
      throw new Error(`Check your ${detail}.`);
    }
    if (response.status === 409 || problem?.code === 'EMAIL_EXISTS') {
      throw new Error('This email already has a staging account. Sign in above.');
    }
    if (response.status === 400) throw new Error(problem?.message || 'Check your registration details.');
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

document.querySelector('#passwordResetForm')?.addEventListener('submit', async (event) => {
  event.preventDefault();
  resetPasswordButton.disabled = true;
  showMessage('Checking your single-use recovery code…');
  try {
    const email = document.querySelector('#recoveryEmail').value.trim().toLowerCase();
    const newPassword = document.querySelector('#newPassword').value;
    const confirmPassword = document.querySelector('#confirmPassword').value;
    if (newPassword !== confirmPassword) throw new Error('The two new passwords must match.');
    const response = await fetch('/api/pilot/password-reset', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
      body: JSON.stringify({
        email,
        recoveryCode: document.querySelector('#recoveryCode').value.trim(),
        newPassword,
        confirmPassword
      })
    });
    const problem = response.ok ? null : await response.json().catch(() => null);
    if (!response.ok) throw new Error(problem?.message || 'The recovery code was not accepted.');
    document.querySelector('#username').value = email;
    document.querySelector('#password').value = '';
    document.querySelector('#passwordResetForm').reset();
    document.querySelector('#password').focus();
    showMessage('Password changed. Sign in above with your new staging password.', 'ready');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    resetPasswordButton.disabled = false;
  }
});

preparePilot();
