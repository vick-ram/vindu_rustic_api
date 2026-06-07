class Stepper {
    constructor() {
        this.currentStep = window.stepperConfig?.currentStep || 0;
        this.totalSteps = window.stepperConfig?.totalSteps || 4;
        this.init();
    }
    init() {
        this.bindEvents();
        this.updateStepper();
    }

    bindEvents() {
        // Step click events
        document.querySelectorAll('.step').forEach(step => {
            step.addEventListener('click', (e) => {
                const stepIndex = parseInt(e.currentTarget.getAttribute('data-step'));
                if (stepIndex <= this.currentStep) {
                    this.gotToStep(stepIndex);
                }
            });
        });

        // Form input events
        document.querySelectorAll('.form-input').forEach(input => {
            input.addEventListener('input', (e) => {
                e.target.style.borderColor = '';
            });
        });

        // Browser navigation
        window.addEventListener('popstate', () => {
            this.handleBrowserNavigation();
        });
    }

    next() {
        if (this.currentStep < this.totalSteps - 1 && this.validateCurrentStep()) {
            this.goToStep(this.currentStep + 1);
            return true;
        }
        return false;
    }

    previous() {
        if (this.currentStep > 0) {
            this.gotToStep(this.currentStep - 1);
            return true;
        }
        return false;
    }

    gotToStep(stepIndex) {
        this.currentStep = stepIndex;
        this.updateStepper();
        this.updateURL();
    }

    validateCurrentStep() {
        const currentContent = document.querySelector('.step-content.active');
        const requiredFields = currentContent?.querySelectorAll('[required]') || [];
        let isValid = true;

        requiredFields.forEach(field => {
            if (!field.value.trim()) {
                field.style.borderColor = '#dc2626';
                field.focus();
                isValid = false;

                if (!field.nextElementSibling?.classList.contains('field-error')) {
                    const errorMsg = document.createElement('div');
                    errorMsg.className = 'field-error';
                    errorMsg.style.color = '#dc2626';
                    errorMsg.style.fontSize = '0.8rem';
                    errorMsg.style.marginTop = '0.25rem';
                    errorMsg.textContent = 'This field is required';
                    field.parentNode.appendChild(errorMsg);
                }
            } else {
                field.style.borderColor = '';
                const errorMsg = field.parentNode.querySelector('.field-error');
                if (errorMsg) {
                    errorMsg.remove();
                }
            }
        });
        return isValid;
    }

    updateStepper() {
        this.updateStepCircles();
        this.updateStepContent();
        this.scrollToContent();
    }

    updateStepCircles() {
        document.querySelectorAll('.step').forEach((step, index) => {
            step.classList.remove('active', 'completed');
            if (index === this.currentStep) {
                step.classList.add('active')
            } else if (index < this.currentStep) {
                step.classList.add('completed');
            }
        });
    }

    updateStepContent() {
        document.querySelectorAll('.step-content').forEach((content, index) => {
            content.classList.remove('active');
            if (index === this.currentStep) {
                content.classList.add('active')
            }
        });
    }

    scrollToContent() {
        const contenttContainer = document.querySelector('stepper-content-container') ;
        if (contenttContainer) {
            contenttContainer.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    }

    updateURL() {
        const url = new URL(window.location);
        url.searchParams.set('step', this.currentStep);
        window.history.pushState({}, '', url);
    }

    handleBrowserNavigation() {
        const urlParams = new URLSearchParams(window.location.search);
        const stepParam = urlParams.get('step');
        if (stepParam !== null) {
            const stepIndex = parseInt(stepParam);
            if (stepIndex >= 0 && stepIndex < this.totalSteps) {
                this.currentStep = stepIndex;
                this.updateStepper();
            }
        }
    }

    getCurrentStep() {
        return this.currentStep;
    }

    setSteps(totalSteps) {
        this.totalSteps = totalSteps;
    }
}

function stepperNext() {
    window.stepper?.next();
}

function stepperPrevious() {
    window.stepper?.previous();
}

document.addEventListener('DOMContentLoaded', function() {
    window.stepper = new Stepper();
});



// function stepperNext(currentStep, totalSteps) {
//     if (currentStep < totalSteps - 1) {
//         currentStep++;
//         updateStepper();
//     }
// }

// function stepperPrevious(currentStep) {
//     if (currentStep > 0) {
//         currentStep--;
//         updateStepper();
//     }
// }

// function updateStepper(currentStep) {
//     const url = new URL(window.location);
//     url.searchParams.set('step', currentStep);
//     window.history.pushState({}, '', url);

//     // Update step circles
//     document.querySelectorAll('.step').forEach((step, index) => {
//         step.classList.remove('active', 'completed');
//         if (index === currentStep) {
//             step.classList.add('active')
//         } else if (index < currentStep) {
//             step.classList.add('completed');
//         }
//     });

//     // Update step content
//     document.querySelectorAll('.step-content').forEach((content, index) => {
//         content.classList.remove('active');
//         if (index === currentStep) {
//             content.classList.add('active')
//         }
//     });

//     // Scroll to top of step content
//     document.querySelector('.stepper-content-container').scrollIntoView({
//         behavior: 'smooth',
//         block: 'start'
//     });
// }

// document.querySelectorAll('.step').forEach(step => {
//     step.addEventListener('click', function() {
//         const stepIndex = parseInt(this.getAttribute('data-step'));
//         if (stepIndex <= currentStep) { // Only allow going to completed steps
//             currentStep = stepIndex;
//             updateStepper();
//         }
//     });
// });

// // Handle browser back/forward buttons
// window.addEventListener('popstate', function() {
//     const urlParams = new URLSearchParams(window.location.search);
//     const stepParam = urlParams.get('step');
//     if (stepParam !== null) {
//         currentStep = parseInt(stepParam);
//         updateStepper();
//     }
// });