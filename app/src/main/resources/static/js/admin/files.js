document.addEventListener("DOMContentLoaded", function () {
  const uploadArea = document.getElementById("upload-area");
  const fileInput = document.getElementById("file-input");
  const browseBtn = document.getElementById("browse-btn");
  const previewContainer = document.getElementById("preview-container");
  const uploadBtn = document.getElementById("upload-btn");
  const successMsg = document.getElementById("success-msg");

  let files = [];

  // CLick ob brwse button to trigger file input
  browseBtn.addEventListener("click", function () {
    fileInput.click();
  });

  // Click on upload area to trigger file input
  uploadArea.addEventListener("click", function () {
    fileInput.click();
  });

  // Handle file selection via input
    fileInput.addEventListener("change", function (e) {
    handleFiles(e.target.files);
    fileInput.value = "";
  });

  function handleFiles(newFiles = []) {
    if (newFiles.length === 0) return;

    // Add new files to the existing list
    for (let i = 0; i < newFiles.length; i++) {
      const file = newFiles[i];
      if (file && file.type.startsWith("image/")) {
        if (!files.some((f) => f.name === file.name && f.size === file.size)) {
          files.push(file);
        }
      }
    }

    updatePreview();
    uploadBtn.disabled = files.length === 0;
    successMsg.style.display = "none";
  }

  // Format file size
  function formatFileSize(bytes) {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB", "TB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  }

  //Update the preview container
  function updatePreview() {
    previewContainer.innerHTML = "";

    if (files.length === 0) {
      previewContainer.innerHTML =
        '<div class="empty-state">No files selected</div>';
      return;
    }

    // Create preview for each file
    files.forEach((file, index) => {
      const reader = new FileReader();

      reader.onload = function (e) {
        const filePreview = document.createElement("div");
        filePreview.className = "file-preview";

        filePreview.innerHTML = `
                    <img class="preview-image" src="${e.target.result}" alt="${
          file.name
        }">
                    <div class="file-info">
                        <div>
                            <div class="file-name">${file.name}</div>
                            <div class="file-size">${formatFileSize(
                              file.size
                            )}</div>
                        </div>
                        <button class="file-remove-btn" data-index="${index}">Remove File</button>
                    </div>
                `;

        previewContainer.appendChild(filePreview);

        // Add event listener to remove button
        const removeBtn = filePreview.querySelector(".file-remove-btn");
        removeBtn.addEventListener("click", function () {
          const indexToRemove = parseInt(this.getAttribute("data-index"));
          files.splice(indexToRemove, 1);
          updatePreview();
          uploadBtn.disabled = files.length === 0;
        });
      };

      reader.readAsDataURL(file);
    });
  }

  // Upload button click
  uploadBtn.addEventListener("click", function () {
    uploadBtn.disabled = true;
    uploadBtn.textContent = "Uploading...";

    setTimeout(function () {
      successMsg.style.display = "block";
      uploadBtn.textContent = "Upload Images";

      // Reset after 3 seconds
      setTimeout(function () {
        files = [];
        updatePreview();
        fileInput.value = "";
        uploadBtn.disabled = true;
      }, 3000);
    }, 1500);
  });
});
