function addDimensions() {
    const dimensionsList = document.getElementById('dimensionsList');
    const addDimensionBtn = document.getElementById('addDimension');

    addDimensionBtn.addEventListener('click', function () {
        const newItem = document.createElement('div');
        newItem.className = 'dynamic-item dimension-group';
        newItem.innerHTML = `
                    <div class="form-group">
                        <label>Width</label>
                        <input type="number" name="width" min="0" placeholder="Width">
                    </div>
                    <div class="form-group">
                        <label>Height</label>
                        <input type="number" name="height" min="0" placeholder="Height">
                    </div>
                    <div class="form-group">
                        <label>Depth</label>
                        <input type="number" name="depth" min="0" placeholder="Depth">
                    </div>
                    <div class="form-group">
                        <label>Unit</label>
                        <select name="unit">
                            <option value="CM">CM</option>
                            <option value="INCH">Inch</option>
                        </select>
                    </div>
                    <button type="button" class="remove-btn"><i data-feather="x"></i></button>
                `;

        dimensionsList.appendChild(newItem);

        newItem.querySelector('.remove-btn').addEventListener('click', function () {
            dimensionsList.removeChild(newItem);
        });
    });

    document.querySelectorAll('#dimensionsList .remove-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            dimensionsList.removeChild(btn.parentElement);
        });
    });
}

document.addEventListener('DOMContentLoaded', function () {
    addDimensions();
});