function toggleMenu(el) {
    const menuId = el.getAttribute("data-menu-id");
    const menu = document.getElementById(menuId);

    // Close any open menu
    document.querySelectorAll('.menu').forEach(m => {
        if (m !== menu) m.classList.remove("show");
    });

    menu.classList.toggle("show");
}

// optional: click outside to close
document.addEventListener("click", (e) => {
    if (!e.target.closest(".menu-wrapper")) {
        document.querySelectorAll(".menu").forEach(m => m.classList.remove("show"));
    }
});

// Optional: Close menu when pressing Escape key
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        document.querySelectorAll(".menu.show").forEach(m => {
            m.classList.remove("show");
        });
    }
});