class Stepper {
    constructor(options = {}) {
        this.containerSelector = options.container || '.stepper';
        this.container = document.querySelector(this.containerSelector);

        if (!this.container) {
            console.warn(`Stepper: Container "${this.containerSelector}" not found."`);
            return;
        }

        const config = window.stepperConfig || {};
        this.totalSteps = options.totalSteps || config.totalSteps || 4;
        this.currentStep = options.currentStep ?? config.currentStep ?? this.getStepFromURL() ?? 0;
        this.transitioning = false;

        this.init();
    }

    init() {
        this.bindEvents();
        this.updateStepper({immediate: true});
    }

    getStepFromURL() {
        const params = new URLSearchParams(window.location.search);
        const step = parseInt(params.get('step'), 10);
        return !isNaN(step) && step >= 0 && step < this.totalSteps ? step : null;
    }

    bindEvents() {
        // Event delegation for step indicators
        this.container.addEventListener('click', async (e) => {
            const stepEl = e.target.closest('.step');
            if (stepEl) {
                const stepIndex = parseInt(stepEl.getAttribute('data-step'), 10);
                if (!isNaN(stepIndex) && stepIndex <= this.currentStep && !this.transitioning) {
                    await this.goToStep(stepIndex);
                }
            }
        });

        this.container.addEventListener('keydown', async (e) => {
            const stepEl = e.target.closest('.step');
            if (stepEl && (e.key === 'Enter' || e.key === ' ')) {
                e.preventDefault()
                const stepIndex = parseInt(stepEl.getAttribute('data-step'), 10);
                if (!isNaN(stepIndex) && stepIndex <= this.currentStep && !this.transitioning) {
                    await this.goToStep(stepIndex);
                }
            }
        });

        // Event delegation from inputs
        this.container.addEventListener('input', (e) => {
            if (e.target.matches('.form-input, [required]')) {
                this.validateField(e.target);
            }
        });

        this.container.addEventListener('blur', (e) => {
            if (e.target.matches('.form-input, [required]')) {
                this.validateField(e.target);
            }
        }, true);

        this.handleGlobalKeydown = async (e) => {
            if (e.key === 'ArrowRight' && e.ctrlKey) {
                e.preventDefault();
                await this.next();
            } else if (e.key === 'ArrowLeft' && e.ctrlKey) {
                e.preventDefault();
                await this.previous();
            }
        }
        document.addEventListener('keydown', this.handleGlobalKeydown);

        // Browser navigation
        this.handlePopState = async () => {
            const stepIndex = this.getStepFromURL();
            if (stepIndex !== null && stepIndex !== this.currentStep) {
                const direction =
                    stepIndex > this.currentStep ? 'next' : 'previous';

                await this.animateTransition(direction);

                this.currentStep = stepIndex;
                this.updateStepper({direction, updateUrl: false});
            }
        }
        window.addEventListener('popstate', this.handlePopState);
    }

    async next() {
        if (this.currentStep < this.totalSteps - 1 && !this.transitioning) {
            const isValid = this.validateCurrentStep();
            if (isValid) {
                await this.goToStep(this.currentStep + 1);
                return true;
            }
        }
        return false;
    }

    async previous() {
        if (this.currentStep > 0 && !this.transitioning) {
            await this.goToStep(this.currentStep - 1);
            return true;
        }
        return false;
    }

    async goToStep(stepIndex) {
        if (this.transitioning || stepIndex < 0 || stepIndex >= this.totalSteps || stepIndex === this.currentStep) return;

        const direction = stepIndex > this.currentStep ? 'next' : 'previous';
        await this.animateTransition(direction);

        this.currentStep = stepIndex;
        this.updateStepper({direction});
    }

    async animateTransition(direction) {
        this.transitioning = true;
        const currentContent = this.container.querySelector('.step-content.active');

        this.container.classList.add('transitioning');

        if (currentContent) {
            const animationClass = direction === 'next' ? 'slideOutLeft' : 'slideOutRight';
            currentContent.style.animation = `${animationClass} 0.3s cubic-bezier(0.4, 0, 0.2, 1) forwards`;
        }

        await new Promise(resolve => setTimeout(resolve, 300));

        this.container.classList.remove('transitioning');
        this.transitioning = false;
    }

    validateField(field) {
        let isValid = true;

        if (field.hasAttribute('required')) {
            if (field.type === 'checkbox' || field.type === 'radio') {
                isValid = field.checked;
            } else {
                isValid = field.value.trim().length > 0;
            }
        }

        if (!isValid) {
            field.classList.add('error');
            field.classList.remove('success');
            field.setAttribute('aria-invalid', 'true');
            this.showFieldError(field);
        } else {
            field.classList.remove('error');
            field.classList.add('success');
            field.removeAttribute('aria-invalid');
            this.removeFieldError(field);
        }
        return isValid;
    }

    validateCurrentStep() {
        const currentContent = this.container.querySelector('.step-content.active');
        if (!currentContent) return true;

        const requiredFields = Array.from(currentContent.querySelectorAll('[required]'));
        let firstInvalidField = null;
        let isStepValid = true;

        requiredFields.forEach(field => {
            const valid = this.validateField(field);
            if (!valid) {
                isStepValid = false;
                if (!firstInvalidField) firstInvalidField = field;
            }
        });

        if (firstInvalidField) {
            firstInvalidField.focus();
            firstInvalidField.style.animation = 'none';
            firstInvalidField.offsetHeight;
            firstInvalidField.style.animation = 'shakeError 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97)';
        }

        return isStepValid;
    }

    showFieldError(field, message = 'This field is required') {
        this.removeFieldError(field);

        const errorId = `error-${field.id || Math.random().toString(36).substring(2, 9)}`;
        const errorMsg = document.createElement('div');
        errorMsg.className = 'field-error';
        errorMsg.id = errorId;
        errorMsg.textContent = message;

        field.setAttribute('aria-describedby', errorId);

        if (field.parentNode) {
            field.parentNode.appendChild(errorMsg);
        }
    }

    removeFieldError(field) {
        const errorId = field.getAttribute('aria-describedby');
        if (errorId) {
            const existingError = document.getElementById(errorId);
            if (existingError) existingError.remove();
            field.removeAttribute('aria-describedby');
        } else {
            const fallbackError = field.parentNode?.querySelector('.field-error');
            if (fallbackError) fallbackError.remove();
        }
    }

    updateStepper({direction = 'next', immediate = false, updateUrl = true} = {}) {
        this.updateStepCircles();
        this.updateStepContent(direction, immediate);
        this.scrollToContent();
        if (updateUrl) this.updateURL();
    }

    updateStepCircles() {
        const steps = this.container.querySelectorAll('.step');
        steps.forEach((step, index) => {
            step.classList.remove('active', 'completed');
            step.removeAttribute('aria-current');

            const circle = step.querySelector('.step-circle');

            if (index === this.currentStep) {
                step.classList.add('active')
                step.setAttribute('aria-current', 'step')

                if (circle) {
                    circle.className = 'step-circle size-7 rounded-full flex items-center justify-center font-semibold text-sm border-2 transition-all duration-300 z-10 shrink-0 bg-primary border-primary text-white ring-4 ring-primary/20';
                    circle.innerHTML = `<span>${index + 1}</span>`;
                }
            } else if (index < this.currentStep) {
                step.classList.add('completed');

                if (circle) {
                    circle.className = 'step-circle size-7 rounded-full flex items-center justify-center font-semibold text-sm border-2 transition-all duration-300 z-10 shrink-0 bg-primary border-primary text-white';
                    circle.innerHTML = '<i class="ti ti-check text-lg font-bold"></i>';
                }
            } else {
                if (circle) {
                    circle.className = 'step-circle size-7 rounded-full flex items-center justify-center font-semibold text-sm border-2 transition-all duration-300 z-10 shrink-0 bg-transparent border-border text-muted group-hover:border-primary/50';
                    circle.innerHTML = `<span>${index + 1}</span>`;
                }
            }
        });
    }

    updateStepContent(direction, immediate) {
        const contents = this.container.querySelectorAll('.step-content');
        contents.forEach((content, index) => {
            if (index === this.currentStep) {
                content.classList.remove('hidden')
                content.classList.add('block', 'active');

                if (!immediate) {
                    const animationName = direction === 'next' ? 'slideInRight' : 'slideInLeft';
                    content.style.animation = 'none';
                    content.offsetHeight;
                    content.style.animation = `${animationName} 0.4s cubic-bezier(0.4, 0, 0.2, 1)`;
                } else {
                    content.classList.remove('block', 'active');
                    content.classList.add('hidden');
                }
            }
        });
    }

    scrollToContent() {
        const contentContainer = this.container.querySelector('.stepper-content-container');
        if (contentContainer) {
            const headOffset = 20;
            const elementPosition = contentContainer.getBoundingClientRect().top;
            const offsetPosition = elementPosition + window.scrollY - headOffset;

            window.scrollTo({
                top: offsetPosition,
                behavior: 'smooth'
            });
        }
    }

    updateURL() {
        const url = new URL(window.location);
        url.searchParams.set('step', this.currentStep);
        window.history.pushState({step: this.currentStep}, '', url);
    }

    destroy() {
        if (this.handleGlobalKeydown) {
            document.removeEventListener('keydown', this.handleGlobalKeydown);
        }
        if (this.handlePopState) {
            document.removeEventListener('popstate', this.handlePopState);
        }
    }
}

function stepperNext() {
    window.stepper?.next();
}

function stepperPrevious() {
    window.stepper?.previous();
}

document.addEventListener('DOMContentLoaded', () => {
    window.stepper = new Stepper();

    let themeTimeout = null;
    const observer = new MutationObserver((mutations) => {
        for (const mutation of mutations) {
            if (mutation.attributeName === 'data-theme') {
                const stepperEl = document.querySelector('.stepper');
                stepperEl?.classList.add('theme-transitioning');

                clearTimeout(themeTimeout);
                themeTimeout = setTimeout(() => {
                    stepperEl?.classList.remove('theme-transitioning');
                }, 300);
                break;
            }
        }
    });

    observer.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['data-theme']
    });
});
