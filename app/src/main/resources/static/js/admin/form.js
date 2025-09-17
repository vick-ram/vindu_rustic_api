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
    })
})
