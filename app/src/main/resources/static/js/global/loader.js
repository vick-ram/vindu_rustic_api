class PageLoader {
    constructor() {
        this.loader = document.getElementById('page-loader');
        this.progressBar = document.getElementById('page-progress-bar');
        this.progressBarInner = document.getElementById('page-progress-inner');
        this.titleEl = document.getElementById('loader-title-text');
        this.statusDot = document.querySelector('.loader-status-dot');

        this.isLoading = false;
        this.progress = 0;
        this.progressInterval = null;
        this.loadingMessages = [
            'Loading',
            'Preparing your experience',
            'Almost there',
            'Finishing touches'
        ];
        this.messageIndex = 0;
        this.messageInterval = null;

        this.init();
    }

    init() {
        if (!this.loader) return;

        this.show();

        if (document.readyState === 'complete') {
            this.hide();
        } else {
            window.addEventListener('load', () => {
                setTimeout(() => this.hide(), 600);
            });
        }

        document.addEventListener('page:loading', () => this.show('loading'));
        document.addEventListener('page:loaded', () => this.hide());
    }

    show(text = 'Loading') {
        if (!this.loader) return;

        this.isLoading = true;
        this.loader.style.display = 'flex';

        void this.loader.offsetHeight;

        document.documentElement.classList.add('is-loading');

        if (this.titleEl && text) {
            this.titleEl.textContent = text;
        } else {
            this.startMessageRotation();
        }

        requestAnimationFrame(() => {
            this.loader.style.opacity = '1';
            this.loader.style.visibility = 'visible';
        });

        if (this.progressBar) {
            this.progressBar.style.opacity = '1';
        }

        this.activateStatusDot();

        this.simulateProgress();
    }

    hide() {
        if (!this.loader || !this.isLoading) return;

        this.isLoading = false;
        this.updateProgress(100);
        this.stopMessageRotation();

        this.completeStatusDot();

        this.loader.style.opacity = '0';
        this.loader.style.visibility = 'hidden';

        if (this.progressBar) {
            this.progressBar.style.opacity = '0';
        }

        setTimeout(() => {
            document.documentElement.classList.remove('is-loading');
            this.loader.style.display = 'none';
            this.updateProgress(0);
            this.resetStatusDot();

            if (this.progressInterval) {
                clearInterval(this.progressInterval);
                this.progressInterval = null;
            }
        }, 500);
    }

    simulateProgress() {
        this.updateProgress(0);
        if (this.progressInterval) clearInterval(this.progressInterval);

        this.progressInterval = setInterval(() => {
            if (this.progress < 75) {
                this.updateProgress(this.progress + Math.random() * 20);
            } else if (this.progress < 90) {
                this.updateProgress(this.progress + Math.random() * 5);
            } else if (this.progress < 97) {
                this.updateProgress(this.progress +  0.5);
            }
        }, 200);
    }

    updateProgress(percent) {
        this.progress = Math.min(100, Math.max(0, percent));
        if (this.progressBarInner) {
            this.progressBarInner.style.width = `${this.progress}%`;

            if (this.progress > 90) {
                this.progressBarInner.style.boxShadow = '0 0 10px var(--color-primary)';
            } else {
                this.progressBarInner.style.boxShadow = 'none';
            }
        }
    }

    startMessageRotation() {
        this.messageIndex = 0;

        if (this.titleEl) {
            this.titleEl.textContent = this.loadingMessages[0];
            this.titleEl.style.opacity = '1';
            this.titleEl.style.transform = 'translateY(0)';
        }

        this.stopMessageRotation();

        this.messageInterval = setInterval(() => {
            this.messageIndex = (this.messageIndex + 1) % this.loadingMessages.length;

            if (this.titleEl) {
                this.titleEl.style.opacity = '0';
                this.titleEl.style.transform = 'translateY(5px)';

                setTimeout(() => {
                    if (this.titleEl && this.isLoading) {
                        this.titleEl.textContent = this.loadingMessages[this.messageIndex];
                        // Fade in new message
                        this.titleEl.style.opacity = '1';
                        this.titleEl.style.transform = 'translateY(0)';
                    }
                }, 200);
            }
        }, 2000);
    }

    stopMessageRotation() {
        if (this.messageInterval) {
            clearInterval(this.messageInterval);
            this.messageInterval = null;
        }
    }

    activateStatusDot() {
        if (!this.statusDot) return;

        this.statusDot.style.animation = 'loader-status-blink 1.5s ease-in-out infinite';
        this.statusDot.style.background = 'var(--color-primary, #D98236)';
        this.statusDot.style.boxShadow = '0 0 8px var(--color-primary, #D98236)';
    }

    completeStatusDot() {
        if (!this.statusDot) return;

        // Flash green on completion
        this.statusDot.style.animation = 'none';
        this.statusDot.style.background = 'var(--color-success, #557A32)';
        this.statusDot.style.boxShadow = '0 0 12px var(--color-success, #557A32)';

        // Quick scale pulse
        this.statusDot.style.transform = 'scale(1.5)';
        setTimeout(() => {
            if (this.statusDot) {
                this.statusDot.style.transform = 'scale(1)';
            }
        }, 200);
    }

    resetStatusDot() {
        if (!this.statusDot) return;

        // Reset to default state
        this.statusDot.style.animation = '';
        this.statusDot.style.background = '';
        this.statusDot.style.boxShadow = '';
        this.statusDot.style.transform = '';
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.pageLoader = new PageLoader();
});

const style = document.createElement('style');
style.textContent = `
    #loader-title-text {
        transition: opacity 0.2s ease, transform 0.2s ease;
        display: inline-block;
    }
    
    .loader-status-dot {
        transition: background 0.3s ease, box-shadow 0.3s ease, transform 0.2s ease;
    }
`;
document.head.appendChild(style);