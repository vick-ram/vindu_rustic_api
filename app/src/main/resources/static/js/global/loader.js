// static/js/loader.js
class PageLoader {
    constructor() {
        this.loader = null;
        this.progressBar = null;
        this.isLoading = false;
        this.progress = 0;
        this.progressInterval = null;
        this.init();
    }

    init() {
        this.createLoader();
        this.setupEventListeners();
    }

    createLoader() {
        // Create main loader
        this.loader = document.createElement('div');
        this.loader.className = 'app-loader';
        this.loader.innerHTML = `
            <div class="loader-content">
                <div class="loader-spinner"></div>
                <div class="loader-text">Loading...</div>
                <div class="loader-subtext">Please wait</div>
            </div>
        `;

        // Create progress bar loader
        this.progressBar = document.createElement('div');
        this.progressBar.className = 'progress-loader';
        this.progressBar.innerHTML = '<div class="progress-bar"></div>';

        document.body.appendChild(this.progressBar);
        document.body.appendChild(this.loader);
    }

    setupEventListeners() {
        // Listen for navigation events
        window.addEventListener('routeChanged', (event) => {
            this.showWithTheme(event.detail.isAdminRoute ? 'admin' : 'ecommerce');
        });

        // Listen for page content updates
        window.addEventListener('pageContentUpdated', () => {
            this.simulateProgress();
        });

        // Listen for beforeunload to show loader on page refresh
        window.addEventListener('beforeunload', () => {
            this.show();
        });

        // Listen for when all resources are loaded
        window.addEventListener('load', () => {
            this.hide();
        });
    }

    show(theme = 'default') {
        if (this.isLoading) return;

        this.isLoading = true;
        this.loader.className = `app-loader ${theme}-loader`;
        this.progressBar.className = `progress-loader ${theme}-progress`;
        this.loader.classList.remove('hidden');
        this.progressBar.style.display = 'block';

        this.simulateProgress();
    }

    hide() {
        this.isLoading = false;

        // Complete progress
        this.updateProgress(100);

        // Hide with delay for smooth transition
        setTimeout(() => {
            this.loader.classList.add('hidden');
            this.progressBar.style.display = 'none';
            this.progress = 0;

            if (this.progressInterval) {
                clearInterval(this.progressInterval);
                this.progressInterval = null;
            }
        }, 500);
    }

    showWithTheme(isAdmin = false) {
        const theme = isAdmin ? 'admin' : 'ecommerce';
        this.show(theme);
    }

    simulateProgress() {
        this.progress = 0;
        this.updateProgress(0);

        if (this.progressInterval) {
            clearInterval(this.progressInterval);
        }

        this.progressInterval = setInterval(() => {
            if (this.progress < 90) {
                this.progress += Math.random() * 15;
                this.updateProgress(this.progress);
            }
        }, 200);
    }

    updateProgress(percent) {
        this.progress = Math.min(100, Math.max(0, percent));
        const progressElement = this.progressBar.querySelector('.progress-bar');
        if (progressElement) {
            progressElement.style.width = `${this.progress}%`;
        }
    }

    setText(text, subtext = '') {
        const textElement = this.loader.querySelector('.loader-text');
        const subtextElement = this.loader.querySelector('.loader-subtext');

        if (textElement) textElement.textContent = text;
        if (subtextElement) subtextElement.textContent = subtext;
    }

    // Skeleton loading for specific content areas
    showSkeletonLoading(container) {
        if (!container) return;

        container.classList.add('skeleton-loading');

        // Create skeleton elements if they don't exist
        if (!container.querySelector('.skeleton-card')) {
            const skeletonHtml = `
                <div class="skeleton-card">
                    <div class="skeleton-line" style="height: 20px; margin-bottom: 1rem;"></div>
                    <div class="skeleton-line short"></div>
                    <div class="skeleton-line medium"></div>
                    <div class="skeleton-line" style="width: 40%;"></div>
                </div>
            `;
            container.innerHTML = skeletonHtml + container.innerHTML;
        }
    }

    hideSkeletonLoading(container) {
        if (!container) return;

        container.classList.remove('skeleton-loading');
        const skeletonCard = container.querySelector('.skeleton-card');
        if (skeletonCard) {
            skeletonCard.remove();
        }
    }

    // For AJAX operations
    startLoading(theme = 'default') {
        this.show(theme);
    }

    stopLoading() {
        this.hide();
    }

    // For form submissions
    bindToForms(selector = 'form') {
        document.addEventListener('submit', (e) => {
            const form = e.target.closest(selector);
            if (form) {
                this.showWithTheme(form.classList.contains('admin-form') ? 'admin' : 'ecommerce');
                this.setText('Processing...', 'Please wait while we handle your request');
            }
        });
    }

    // For image loading
    waitForImages(container = document) {
        const images = container.querySelectorAll('img');
        let loadedCount = 0;
        const totalCount = images.length;

        if (totalCount === 0) {
            this.hide();
            return;
        }

        images.forEach(img => {
            if (img.complete) {
                loadedCount++;
            } else {
                img.addEventListener('load', () => {
                    loadedCount++;
                    this.updateProgress(10 + (loadedCount / totalCount) * 80);

                    if (loadedCount === totalCount) {
                        setTimeout(() => this.hide(), 300);
                    }
                });

                img.addEventListener('error', () => {
                    loadedCount++;
                    if (loadedCount === totalCount) {
                        setTimeout(() => this.hide(), 300);
                    }
                });
            }
        });

        // If all images are already loaded
        if (loadedCount === totalCount) {
            setTimeout(() => this.hide(), 300);
        }
    }
}

// Initialize page loader
if (typeof window !== 'undefined') {
    window.pageLoader = new PageLoader();

    // Also add to App namespace for consistency
    if (!window.App) window.App = {};
    window.App.Loader = window.pageLoader;
}