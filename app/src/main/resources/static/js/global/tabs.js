document.addEventListener("DOMContentLoaded", () => {
    // Make tabs reusable by using data attributes
    const tabContainers = document.querySelectorAll('[data-tabs-container]');

    tabContainers.forEach(container => {
        const headers = container.querySelectorAll('.tab-header li');
        const contents = container.querySelectorAll('.tab-content > div');

        headers.forEach(header => {
            header.addEventListener("click", (e) => {
                e.preventDefault();
                const tabId = header.getAttribute("data-tab-id");

                // Update URL hash if needed
                // history.replaceState(null, null, `#${tabId}`);

                // Remove active from all
                headers.forEach(h => h.classList.remove("active"));
                contents.forEach(c => c.classList.remove("active"));

                // Update ARIA attributes
                headers.forEach(h => {
                    h.querySelector('a').setAttribute('aria-selected', 'false');
                });

                // Activate selected
                header.classList.add("active");
                header.querySelector('a').setAttribute('aria-selected', 'true');

                const targetContent = container.querySelector(`.tab-content > div[data-tab-id="${tabId}"]`);
                if (targetContent) {
                    targetContent.classList.add("active");
                }
            });
        });
    });
});