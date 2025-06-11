const ws = new WebSocket("ws://localhost:8080/sync");
ws.addEventListener("open", () => console.log("WS open"));
ws.addEventListener("message", e => handleMessage(e.data));

const callbacks = [];
export function onMessage(cb) { callbacks.push(cb); }

function handleMessage(data) {
  console.log("From server:", data);
  callbacks.forEach(cb => cb(data));
}

export function sendGet(path) {
  ws.send(JSON.stringify({ startLine: `GET ${path}`, headers: {}, bodyBase64: "" }));
}

export function sendPost(path, headers, body, retry=0) {
  try {
  ws.send(JSON.stringify({ startLine: `POST ${path}`, headers, bodyBase64: btoa(body) }));
  } catch (e) {
    if (retry < 5) {
      const delay = Math.pow(2, retry) * 1000;
      setTimeout(() => sendPost(path, headers, body, retry+1), delay);
    } else {
      console.error("sendPost failed", e);
    }
  }
}

// UI for file upload
const input = document.getElementById('fileInput');
const progress = document.getElementById('progress');
if (input) {
  input.addEventListener('change', () => {
    const file = input.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      sendPost('/sync', {}, reader.result);
    };
    reader.readAsBinaryString(file);
  });
}

onMessage(data => {
  progress.innerText = data;
});

