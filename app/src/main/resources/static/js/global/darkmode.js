document.addEventListener('DOMContentLoaded', function() {
    // Render feather icons
    feather.replace();

    const themeToggle = document.getElementById('themeToggle');
    // const body = document.body;
    const html = document.documentElement;

    const getPreferredTheme = () => {
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme) return savedTheme;
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? 'dark' : 'light';
    }

    const setTheme = (theme) => {
        // Prevent transition flash
        html.classList.add('theme-transitioning');

        html.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);

        // Update icon
        const icon = theme === 'dark' ? 'moon' : 'sun';
        themeToggle.innerHTML = `<i data-feather="${icon}"></i>`
        feather.replace();

        // Remove transition class after animation completes
        setTimeout(() => {
            html.classList.remove('theme-transitioning');
        }, 300);
    }

    // Initialize theme
    setTheme(getPreferredTheme());

    // Toggle theme on click
    themeToggle.addEventListener('click', () => {
        const currentTheme = html.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        setTheme(newTheme);
    });

    // Listen for system theme changes (if user hasn't manually set a preference)
    window.matchMedia("(prefers-color-scheme: dark)").addEventListener('change', (e) => {
        if (!localStorage.getItem('theme')){
            setTheme(e.matches ? 'dark' : 'light');
        }
    });
});