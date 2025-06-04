document.addEventListener('DOMContentLoaded', init);

function init() {
  initMenuToggle();
  initFileUpload();
  initSyncButton();
  initDeleteLinks();
  updateFileCount();
}

// ---------- Menu ----------
function initMenuToggle() {
  const menuToggle = document.getElementById('menu-toggle');
  const menu = document.getElementById('menu');
  if (!menuToggle || !menu) return;

  let open = false;
  menuToggle.addEventListener('click', () => {
    open = !open;
    menu.classList.toggle('hidden', !open);
    toggleMenuIcon(menuToggle, open);
    toggleMenuStyle(menu, open);
  });
}

const MENU_ICON =
  '<svg class="h-6 w-6" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"/></svg>';
const CLOSE_ICON =
  '<svg class="h-6 w-6" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>';

function toggleMenuIcon(button, open) {
  button.innerHTML = open ? CLOSE_ICON : MENU_ICON;
}

function toggleMenuStyle(menu, open) {
  const classes = ['absolute', 'top-14', 'right-0', 'bg-[#1E6EF2]', 'w-48', 'p-4'];
  if (open) {
    menu.classList.add(...classes);
  } else {
    menu.classList.remove(...classes);
  }
}

// ---------- File Upload ----------
function initFileUpload() {
  const dropZone = document.getElementById('drop-zone');
  const fileInput = document.getElementById('file-input');
  if (!dropZone || !fileInput) return;

  dropZone.addEventListener('click', () => fileInput.click());
  dropZone.addEventListener('dragover', event => {
    event.preventDefault();
    dropZone.classList.add('bg-gray-100');
  });
  dropZone.addEventListener('dragleave', () => dropZone.classList.remove('bg-gray-100'));
  dropZone.addEventListener('drop', event => {
    event.preventDefault();
    dropZone.classList.remove('bg-gray-100');
    const file = event.dataTransfer.files[0];
    if (file) uploadFile(file);
  });
  fileInput.addEventListener('change', () => {
    const file = fileInput.files[0];
    if (file) uploadFile(file);
  });
}

function uploadFile(file) {
  const formData = new FormData();
  formData.append('file', file);
  fetch('/api/files/upload', { method: 'POST', body: formData })
    .then(res => (res.ok ? window.location.reload() : res.text().then(showUploadError)))
    .catch(err => showUploadError(err));
}

function showUploadError(msg) {
  alert('Upload mislukt: ' + msg);
}

// ---------- Sync ----------
function initSyncButton() {
  const syncBtn = document.getElementById('sync-btn');
  if (syncBtn) {
    syncBtn.addEventListener('click', () => alert('Synchroniseren gestart'));
  }
}

// ---------- Delete ----------
function initDeleteLinks() {
  document.querySelectorAll('.delete-link').forEach(link => {
    link.addEventListener('click', event => {
      event.preventDefault();
      const file = link.getAttribute('data-file-name');
      if (!file) return;
      if (confirm(`Weet je zeker dat je ${file} wilt verwijderen?`)) {
        deleteFile(file, link.closest('tr'));
      }
    });
  });
}

function deleteFile(fileName, row) {
  fetch(`/api/files/delete?fileName=${encodeURIComponent(fileName)}`, { method: 'DELETE' })
    .then(res => (res.ok ? handleDeleteSuccess(row) : res.text().then(showDeleteError)))
    .catch(showDeleteError);
}

function handleDeleteSuccess(row) {
  row.remove();
  updateFileCount();
}

function showDeleteError(msg) {
  alert('Verwijderen mislukt: ' + msg);
}

// ---------- Stats ----------
function updateFileCount() {
  const body = document.getElementById('files-body');
  const count = body ? body.querySelectorAll('tr').length : 0;
  const counter = document.getElementById('total-files');
  if (counter) counter.textContent = count;

  const noFilesRow = document.getElementById('no-files-row');
  if (noFilesRow) noFilesRow.classList.toggle('hidden', count > 0);
}
