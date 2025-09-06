document.addEventListener('DOMContentLoaded', () => {
    const togglePassword = document.querySelector('.toggle-password');
    const password = document.querySelector('#password');

    togglePassword.addEventListener('click', () => {
        // Toggle password
        const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
        password.setAttribute('type', type);

        const icon = type === 'text' ? 'eye' : 'eye-off';
        togglePassword.innerHTML = `<i data-feather='${icon}'></i>`;
        feather.replace();
    })
})