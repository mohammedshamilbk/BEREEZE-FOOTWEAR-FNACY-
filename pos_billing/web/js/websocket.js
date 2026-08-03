// Real-Time WebSocket Client for Live Device Sync
class WebSocketClient {
  constructor() {
    this.socket = null;
    this.listeners = [];
    this.reconnectInterval = 3000;
  }

  connect() {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const user = api.getUser();
    const userId = user ? user.user_id : 0;
    const wsUrl = `${protocol}//${window.location.host}/ws?user_id=${userId}`;

    this.socket = new WebSocket(wsUrl);

    this.socket.onopen = () => {
      console.log("WebSocket connected cleanly.");
      this.updateConnectionStatus(true);
    };

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        console.log("WebSocket Received:", message);
        this.notifyListeners(message);
      } catch (e) {
        console.error("WebSocket payload error:", e);
      }
    };

    this.socket.onclose = () => {
      console.warn("WebSocket closed. Attempting reconnect in 3s...");
      this.updateConnectionStatus(false);
      setTimeout(() => this.connect(), this.reconnectInterval);
    };

    this.socket.onerror = (err) => {
      console.error("WebSocket error:", err);
      this.socket.close();
    };
  }

  subscribe(callback) {
    this.listeners.push(callback);
  }

  notifyListeners(message) {
    this.listeners.forEach((cb) => cb(message));
  }

  updateConnectionStatus(isConnected) {
    const badge = document.getElementById("status-badge");
    const text = document.getElementById("status-text");
    if (badge && text) {
      if (isConnected) {
        badge.style.background = "rgba(16, 185, 129, 0.1)";
        badge.style.color = "#10b981";
        text.innerText = "Live Cloud Sync Active";
      } else {
        badge.style.background = "rgba(239, 68, 68, 0.1)";
        badge.style.color = "#ef4444";
        text.innerText = "Connecting / Offline";
      }
    }
  }
}

const wsClient = new WebSocketClient();
