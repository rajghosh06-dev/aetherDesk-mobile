document.addEventListener('DOMContentLoaded', () => {
  renderModelList();
});

const MODELS = [
  { id: 'gemma-2b-it', name: 'Gemma 2B Instruct', size: '1.4 GB', url: 'https://example.com/gemma-2b-it.bin' },
  { id: 'llama-3-8b-instruct', name: 'Llama 3 8B Instruct', size: '4.2 GB', url: 'https://example.com/llama-3-8b.bin' },
  { id: 'mistral-7b-v0.1', name: 'Mistral 7B', size: '3.8 GB', url: 'https://example.com/mistral-7b.bin' }
];

function getDownloadedModels() {
  const data = localStorage.getItem('downloadedModels');
  return data ? JSON.parse(data) : {};
}

function saveDownloadedModels(data) {
  localStorage.setItem('downloadedModels', JSON.stringify(data));
}

function renderModelList() {
  const grid = document.getElementById('model-grid');
  grid.innerHTML = '';
  const downloaded = getDownloadedModels();

  MODELS.forEach(model => {
    const isDownloaded = !!downloaded[model.id];

    const card = document.createElement('div');
    card.className = 'model-card';
    card.innerHTML = `
      <div class="model-header">
        <div>
          <h3 class="model-name">${model.name}</h3>
          <span class="model-size">${model.size}</span>
        </div>
        ${isDownloaded 
          ? `<button class="btn-delete" onclick="deleteModel('${model.id}')">Delete</button>`
          : `<button class="btn-download" id="btn-dl-${model.id}" onclick="downloadModel('${model.id}')">Download</button>`
        }
      </div>
      <div class="progress-container" id="prog-cont-${model.id}">
        <div class="progress-bar" id="prog-bar-${model.id}"></div>
      </div>
      <div id="status-text-${model.id}" style="font-size: 0.75rem; color: var(--text-muted); display: none;"></div>
    `;
    grid.appendChild(card);
  });
}

function downloadModel(id) {
  const model = MODELS.find(m => m.id === id);
  if (!model) return;

  const btn = document.getElementById(`btn-dl-${id}`);
  const progCont = document.getElementById(`prog-cont-${id}`);
  const progBar = document.getElementById(`prog-bar-${id}`);
  const statusText = document.getElementById(`status-text-${id}`);

  if (btn) btn.style.display = 'none';
  if (progCont) progCont.style.display = 'block';
  if (statusText) {
    statusText.style.display = 'block';
    statusText.textContent = 'Starting download...';
  }

  // Native Android Download Integration
  if (window.AndroidBridge && window.AndroidBridge.startDownload) {
    // Setup a global callback for this download
    window[`onDownloadProgress_${id}`] = (percent) => {
      if (progBar) progBar.style.width = `${percent}%`;
      if (statusText) statusText.textContent = `Downloading... ${percent}%`;
    };
    
    window[`onDownloadComplete_${id}`] = (success, finalSize) => {
      if (success) {
        completeDownload(id, finalSize || model.size);
      } else {
        if (statusText) statusText.textContent = 'Download failed.';
        if (btn) btn.style.display = 'block';
        if (progCont) progCont.style.display = 'none';
      }
      delete window[`onDownloadProgress_${id}`];
      delete window[`onDownloadComplete_${id}`];
    };

    window.AndroidBridge.startDownload(model.url, `${id}.bin`, `onDownloadProgress_${id}`, `onDownloadComplete_${id}`);
  } else {
    // Simulated Download for Web / iOS fallback
    let progress = 0;
    const interval = setInterval(() => {
      progress += Math.floor(Math.random() * 10) + 5;
      if (progress >= 100) {
        progress = 100;
        clearInterval(interval);
        setTimeout(() => {
          completeDownload(id, model.size);
        }, 500);
      }
      if (progBar) progBar.style.width = `${progress}%`;
      if (statusText) statusText.textContent = `Downloading... ${progress}%`;
    }, 200);
  }
}

function completeDownload(id, size) {
  const downloaded = getDownloadedModels();
  downloaded[id] = {
    downloadedAt: new Date().toISOString(),
    size: size
  };
  saveDownloadedModels(downloaded);
  renderModelList();
}

function deleteModel(id) {
  const downloaded = getDownloadedModels();
  delete downloaded[id];
  saveDownloadedModels(downloaded);
  
  if (window.AndroidBridge && window.AndroidBridge.deleteFile) {
    window.AndroidBridge.deleteFile(`${id}.bin`);
  }
  
  renderModelList();
}
