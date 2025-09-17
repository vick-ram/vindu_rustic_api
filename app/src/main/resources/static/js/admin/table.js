function updateSelectAllState(selectAll, rowChekboxes) {
    const allCheked = Array.from(rowChekboxes).every(cb => cb.checked);
    const someChecked = Array.from(rowChekboxes).some(cb => cb.checked);

    selectAll.checked = allCheked;
    selectAll.indeterminate = someChecked && !allCheked;
}

function updateSelectedCount() {
    const selectedChackboxes = document.querySelectorAll('.row-checkbox:checked');
    const selectedCount = selectedChackboxes.length;
    const selectedCountElement = document.getElementById('selectedCount');

    if (selectedCount === 0) {
        selectedCountElement.textContent = 'No rows selected';
    } else {
        selectedCountElement.textContent = `Selected ${selectedCount} row${selectedCount !== 1 ? 's' : ''}`;
    }
}

function updateRowSelectionStyle() {
    document.querySelectorAll('.row-checkbox').forEach(checkbox => {
        const row = checkbox.closest('tr');
        if (checkbox.checked) {
            row.style.backgroundColor = 'var(--color-secondary)';
        } else {
            row.style.backgroundColor = '';
        }
    })
}

document.addEventListener("DOMContentLoaded", function () {

    const selectAllCheckboxes = document.querySelectorAll('.select-all');

    selectAllCheckboxes.forEach(selectAll => {
        const table = selectAll.closest('.t-table');
        const rowCheckboxes = table.querySelectorAll('.row-checkbox');

        // Select all functionality
        selectAll.addEventListener('change', function () {
            rowCheckboxes.forEach(checkbox => {
                checkbox.checked = selectAll.checked;
                checkbox.dispatchEvent(new Event('change', { bubbles: true }));
            });
            updateSelectedCount();
            updateRowSelectionStyle();
        });

        // Update select all checkbox when individual checkboxes change
        rowCheckboxes.forEach(checkbox => {
            checkbox.addEventListener('change', function () {
                updateSelectAllState(selectAll, rowCheckboxes);
                updateSelectedCount();
                updateRowSelectionStyle();
            });
        });

        // Initializa select all state
        updateSelectAllState(selectAll, rowCheckboxes);
    });
});