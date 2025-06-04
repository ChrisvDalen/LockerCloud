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
dropZone.addEventListener('click', () => fileInput.click());
fileInput.addEventListener('change', () => {
  console.log(fileInput.files);
});

// Files table data
const files = [
  { name: '1336screenshotDiefstal - Copy (3).png', size: '2.3 MB', type: 'image' },
  { name: '1GB.bin', size: '1 GB', type: 'binary' },
  { name: 'CV Aart Schouten.pdf', size: '156 KB', type: 'pdf' },
  { name: 'test.txt', size: '1 KB', type: 'text' }
];

function badgeColor(type) {
  switch (type) {
    case 'image':
      return 'bg-blue-500';
    case 'binary':
      return 'bg-purple-500';
    case 'pdf':
      return 'bg-rose-500';
    case 'text':
      return 'bg-slate-500';
  }
}

function renderFiles() {
  const tbody = document.getElementById('files-body');
  tbody.innerHTML = '';
  files.forEach(file => {
    const tr = document.createElement('tr');
    tr.className = 'border-t';
    tr.innerHTML = `
      <td class="py-2 pr-2">${file.name}</td>
      <td class="py-2 pr-2">${file.size}</td>
      <td class="py-2 pr-2"><span class="text-white text-xs px-2 py-0.5 rounded-full ${badgeColor(file.type)}">${file.type}</span></td>
      <td class="py-2 flex space-x-2">
        <svg class="h-5 w-5 text-green-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
        <svg class="h-5 w-5 text-green-500 cursor-pointer hover:opacity-80" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true" data-download="${file.name}"><path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3"/></svg>
        <svg class="h-5 w-5 text-red-500 cursor-pointer hover:opacity-80" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" aria-hidden="true" data-delete="${file.name}"><path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0"/></svg>
      </td>`;
    tbody.appendChild(tr);
  });
}

renderFiles();

document.getElementById('files-body').addEventListener('click', e => {
  const target = e.target.closest('svg');
  if (!target) return;
  if (target.dataset.download) {
    alert(`Download ${target.dataset.download}`);
  } else if (target.dataset.delete) {
    const name = target.dataset.delete;
    const index = files.findIndex(f => f.name === name);
    if (index > -1) {
      files.splice(index, 1);
      renderFiles();
    }
  }
});

// Sync button
document.getElementById('sync-btn').addEventListener('click', () => {
  alert('Synchroniseren gestart');
});
