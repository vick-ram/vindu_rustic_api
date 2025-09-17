// Initialize charts (example with Chart.js - you'll need to add the dependency)
document.addEventListener('DOMContentLoaded', function() {
    // Elements
    const darkModeSwitch = document.getElementById('dark-mode-switch');
    const searchInput = document.getElementById('searchInput');
    const sidebarToggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');

    sidebarToggle.addEventListener('click', () => {
        sidebar.classList.toggle('open');
    });

    // Close sidebar when clicking outside on mobile
    document.addEventListener('click', (event) => {
        if (window.innerWidth < 992 &&
        !sidebar.contains(event.target) &&
        !sidebarToggle.contains(event.target) &&
        sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
        }
    });
});

function toggleNavGroup(groupId) {
    const group = document.getElementById(groupId);
    group.classList.toggle('expanded');

    // rotate arrow icon
    const arrow = group.previousElementSibling.querySelector('.nav-arrow');
    arrow.classList.toggle('rotated');
}