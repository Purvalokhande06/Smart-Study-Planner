document.addEventListener('DOMContentLoaded', () => {
    const authContainer = document.getElementById('authContainer');
    const appContainer = document.getElementById('appContainer');

    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    const showRegister = document.getElementById('showRegister');
    const showLogin = document.getElementById('showLogin');

    const loginError = document.getElementById('loginError');
    const registerError = document.getElementById('registerError');

    // Check existing session
    checkSession();

    showRegister.addEventListener('click', (e) => {
        e.preventDefault();
        loginForm.classList.add('hidden');
        registerForm.classList.remove('hidden');
    });

    showLogin.addEventListener('click', (e) => {
        e.preventDefault();
        registerForm.classList.add('hidden');
        loginForm.classList.remove('hidden');
    });

    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.classList.add('hidden');
        
        const username = document.getElementById('loginUsername').value;
        const password = document.getElementById('loginPassword').value;

        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            const data = await res.json();
            if (res.ok) {
                checkSession();
            } else {
                loginError.textContent = data.error || 'Login failed';
                loginError.classList.remove('hidden');
            }
        } catch (err) {
            loginError.textContent = 'Server connection error';
            loginError.classList.remove('hidden');
        }
    });

    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        registerError.classList.add('hidden');

        const username = document.getElementById('regUsername').value;
        const password = document.getElementById('regPassword').value;
        const confirm = document.getElementById('regConfirmPassword').value;
        const dailyHours = parseFloat(document.getElementById('regDailyHours').value);

        if (password !== confirm) {
            registerError.textContent = 'Passwords do not match!';
            registerError.classList.remove('hidden');
            return;
        }

        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password, dailyHours })
            });
            const data = await res.json();
            if (res.ok) {
                checkSession();
            } else {
                registerError.textContent = data.error || 'Registration failed';
                registerError.classList.remove('hidden');
            }
        } catch (err) {
            registerError.textContent = 'Server connection error';
            registerError.classList.remove('hidden');
        }
    });

    document.getElementById('logoutBtn').addEventListener('click', async () => {
        await fetch('/api/auth/logout', { method: 'POST' });
        window.location.reload();
    });

    async function checkSession() {
        try {
            const res = await fetch('/api/auth/me');
            if (res.ok) {
                const user = await res.json();
                authContainer.classList.add('hidden');
                appContainer.classList.remove('hidden');
                document.getElementById('userDisplayName').textContent = user.username;
                window.currentUser = user;
                if (window.initApp) window.initApp();
            } else {
                authContainer.classList.remove('hidden');
                appContainer.classList.add('hidden');
            }
        } catch (e) {
            authContainer.classList.remove('hidden');
            appContainer.classList.add('hidden');
        }
    }
});