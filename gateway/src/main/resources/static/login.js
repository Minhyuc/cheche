const state = { mode: 'user', accessToken: null };
const tabs = [...document.querySelectorAll('[role="tab"]')];
const loginForm = document.querySelector('#login-panel');
const regionForm = document.querySelector('#region-form');
const completePanel = document.querySelector('#complete-panel');
const registerButton = document.querySelector('#register-button');
const feedback = document.querySelector('#feedback');

function setMode(mode) {
  state.mode = mode;
  tabs.forEach((tab) => {
    const selected = tab.dataset.mode === mode;
    tab.classList.toggle('active', selected);
    tab.setAttribute('aria-selected', String(selected));
  });
  document.querySelector('#mode-description').textContent = mode === 'user'
    ? '사용자 로그인은 현재 인증 기능만 제공됩니다.'
    : '관리자 로그인 후 담당 지역의 시설과 안전점검을 관리할 수 있습니다.';
  document.querySelector('#submit-button').textContent = mode === 'user' ? '사용자로 로그인' : '관리자로 로그인';
  registerButton.classList.toggle('hidden', mode !== 'admin');
  feedback.textContent = '';
}

tabs.forEach((tab) => tab.addEventListener('click', () => setMode(tab.dataset.mode)));

async function request(url, options) {
  const response = await fetch(url, options);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.detail || data.message || '요청을 처리하지 못했습니다.');
  return data;
}

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  feedback.textContent = '';
  const submit = document.querySelector('#submit-button');
  submit.disabled = true;
  const credentials = {
    username: document.querySelector('#username').value,
    password: document.querySelector('#password').value,
  };
  try {
    const endpoint = state.mode === 'admin' ? '/auth/admin/login' : '/auth/user/login';
    const result = await request(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    });
    state.accessToken = result.accessToken;
    if (state.mode === 'admin' && result.initialSetupRequired) {
      loginForm.classList.add('hidden');
      document.querySelector('.tabs').classList.add('hidden');
      regionForm.classList.remove('hidden');
    } else {
      showComplete(
        state.mode === 'admin' ? '관리자 로그인 완료' : '사용자 로그인 완료',
        state.mode === 'admin'
          ? `${result.regionName || '전체 지역'} 관리 권한으로 로그인했습니다.`
          : result.message,
      );
    }
  } catch (error) {
    feedback.textContent = error.message;
  } finally {
    submit.disabled = false;
  }
});

registerButton.addEventListener('click', async () => {
  feedback.textContent = '';
  try {
    const result = await request('/auth/admin/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: document.querySelector('#username').value,
        password: document.querySelector('#password').value,
      }),
    });
    feedback.textContent = `${result.username} 관리자 계정이 생성되었습니다. 로그인해 주세요.`;
    feedback.classList.add('success');
  } catch (error) {
    feedback.classList.remove('success');
    feedback.textContent = error.message;
  }
});

regionForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const regionFeedback = document.querySelector('#region-feedback');
  try {
    const result = await request('/api/admins/me/region', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${state.accessToken}`,
      },
      body: JSON.stringify({
        regionCode: document.querySelector('#region-code').value,
        regionName: document.querySelector('#region-name').value,
      }),
    });
    regionForm.classList.add('hidden');
    showComplete('지역 설정 완료', `${result.regionName}의 체육시설 관리자로 설정되었습니다.`);
  } catch (error) {
    regionFeedback.textContent = error.message;
  }
});

function showComplete(title, message) {
  loginForm.classList.add('hidden');
  document.querySelector('.tabs').classList.add('hidden');
  completePanel.classList.remove('hidden');
  document.querySelector('#complete-title').textContent = title;
  document.querySelector('#complete-message').textContent = message;
}

document.querySelector('#back-button').addEventListener('click', () => window.location.reload());
