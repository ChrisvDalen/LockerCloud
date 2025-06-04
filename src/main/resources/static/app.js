// Mobile menu toggle
const menuToggle = document.getElementById('menu-toggle');
const menu = document.getElementById('menu');
let menuOpen = false;
menuToggle.addEventListener('click', () => {
  menuOpen = !menuOpen;
  menu.classList.toggle('hidden', !menuOpen);
  if (menuOpen) {
    menuToggle.innerHTML = `<svg class="h-6 w-6" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>`;
    menu.classList.add('absolute', 'top-14', 'right-0', 'bg-[#1E6EF2]', 'w-48', 'p-4');
  } else {
    menuToggle.innerHTML = `<svg class="h-6 w-6" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"/></svg>`;
    menu.classList.remove('absolute', 'top-14', 'right-0', 'bg-[#1E6EF2]', 'w-48', 'p-4');
  }
});

// Upload file handling
const dropZone = document.getElementById('drop-zone');
const fileInput = document.getElementById('file-input');

function uploadFile(file) {
  const formData = new FormData();
  formData.append('file', file);
  fetch('/api/files/upload', {
    method: 'POST',
    body: formData
  }).then(res => {
    if (res.ok) {
      window.location.reload();
    } else {
      res.text().then(t => alert('Upload mislukt: ' + t));
    }
  }).catch(err => alert('Upload mislukt: ' + err));
}

dropZone.addEventListener('click', () => fileInput.click());
dropZone.addEventListener('dragover', e => {
  e.preventDefault();
  dropZone.classList.add('bg-gray-100');
});
dropZone.addEventListener('dragleave', () => dropZone.classList.remove('bg-gray-100'));
dropZone.addEventListener('drop', e => {
  e.preventDefault();
  dropZone.classList.remove('bg-gray-100');
  if (e.dataTransfer.files.length) {
    uploadFile(e.dataTransfer.files[0]);
  }
});
fileInput.addEventListener('change', () => {
  if (fileInput.files.length) {
    uploadFile(fileInput.files[0]);
  }
});


// Sync button
document.getElementById('sync-btn').addEventListener('click', () => {
  alert('Synchroniseren gestart');
});

// Delete buttons
function updateFileCount() {
  const count = document.querySelectorAll('#files-body tr').length;
  const counter = document.getElementById('total-files');
  if (counter) counter.textContent = count;
  const noFilesRow = document.getElementById('no-files-row');
  if (noFilesRow) {
    if (count === 0) {
      noFilesRow.classList.remove('hidden');
    } else {
      noFilesRow.classList.add('hidden');
    }
  }
}

document.querySelectorAll('.delete-link').forEach(link => {
  link.addEventListener('click', e => {
    e.preventDefault();
    const file = link.getAttribute('data-file-name');
    if (!file) return;
    if (confirm(`Weet je zeker dat je ${file} wilt verwijderen?`)) {
      fetch(`/api/files/delete?fileName=${encodeURIComponent(file)}`, { method: 'DELETE' })
        .then(res => {
          if (res.ok) {
            link.closest('tr').remove();
            updateFileCount();
          } else {
            res.text().then(t => alert('Verwijderen mislukt: ' + t));
          }
        })
        .catch(err => alert('Verwijderen mislukt: ' + err));
    }
  });
});

updateFileCount();
