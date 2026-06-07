const editor = document.getElementById('description');

editor.innerHTML = '';

function formatText(command, value = null) {
    document.execCommand(command, false, value);
    editor.focus();
}

editor.addEventListener('focus', function() {
    if (this.innerHTML === '') {
        this.innerHTML = '';
    }
});

editor.addEventListener('blur', function() {
    if (this.innerHTML === '') {
        this.innerHTML = '';
    }
});

// Dialog

function openDialog(dialogId) {
    const dialog = document.getElementById(dialogId);
    if (dialog) {
        dialog.showModal(); 
    }
}

function closeDialog(dialogId) {
    const dialog = document.getElementById(dialogId);
    if (dialog) {
        dialog.close(); 
    }
}

// Close dialog while clicking outside
document.querySelectorAll("dialog").forEach(dialog => {
    dialog.addEventListener("click", e => {
        const dialogDimensions = dialog.getBoundingClientRect();
        if (
            e.clientX < dialogDimensions.left ||
            e.clientX > dialogDimensions.right
            || e.clientY < dialogDimensions.top ||
            e.clientY > dialogDimensions.bottom
        ) {
            dialog.close();
        }
    });

//    closing animation
    dialog.addEventListener('close', function() {
        this.classList.add('closing');
        setTimeout(() => {
            this.classList.remove('closing');
        }, 200);
    });
});


document.addEventListener('DOMContentLoaded', function() {
    // Handle all form submissions
    document.addEventListener('submit', (e) => {
        const form = e.target;
        const submitBtn = form.querySelector('button[type="submit"]');

        if (submitBtn && submitBtn.classList.contains('btn')) {
            submitBtn.classList.add('loading');
            submitBtn.disabled = true;

            // Remove loading state after timeout
            setTimeout(() => {
                if (submitBtn.classList.contains('loading')) {
                    submitBtn.classList.remove('loading');
                    submitBtn.disabled = false;
                }
            }, 10000);
        }
    });

    window.addEventListener('pageshow', (e) => {
        document.querySelectorAll('.btn.loading').forEach(btn => {
            btn.classList.remove('loading');
            btn.disabled = false;
        });
    });
});

