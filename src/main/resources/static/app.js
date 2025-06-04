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
const uploadForm = document.getElementById('upload-form');
dropZone.addEventListener('click', () => fileInput.click());
fileInput.addEventListener('change', () => {
  if (uploadForm && fileInput.files.length) {
    uploadForm.submit();
  }
});


// Sync button
document.getElementById('sync-btn').addEventListener('click', () => {
  alert('Synchroniseren gestart');
});
