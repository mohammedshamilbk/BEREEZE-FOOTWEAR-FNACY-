// Centralized REST API Client
class ApiClient {
  constructor() {
    this.baseUrl = "/api/v1";
    this.tokenKey = "bereeze_access_token";
    this.userKey = "bereeze_user_data";
  }

  getToken() {
    return localStorage.getItem(this.tokenKey);
  }

  setSession(token, user) {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.userKey, JSON.stringify(user));
  }

  clearSession() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }

  getUser() {
    const raw = localStorage.getItem(this.userKey);
    return raw ? JSON.parse(raw) : null;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseUrl}${endpoint}`;
    const token = this.getToken();

    const headers = {
      "Content-Type": "application/json",
      ...(options.headers || {})
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const config = {
      ...options,
      headers
    };

    try {
      const response = await fetch(url, config);
      if (response.status === 401) {
        this.clearSession();
        window.location.reload();
        throw new Error("Unauthorized");
      }
      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.detail || "API Request Failed");
      }
      if (response.status === 204) return null;
      return await response.json();
    } catch (err) {
      console.error(`API Error [${endpoint}]:`, err);
      throw err;
    }
  }

  // Auth Methods
  async login(username, password) {
    const data = await this.request("/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
    this.setSession(data.access_token, data.user);
    return data;
  }

  // Dashboard Stats
  async getDashboardStats() {
    return await this.request("/dashboard/stats");
  }

  // Sessions & Stations
  async getStations() {
    return await this.request("/sessions/stations");
  }

  async getActiveSessions() {
    return await this.request("/sessions/active");
  }

  async checkin(station_id, customer_id) {
    return await this.request("/sessions/checkin", {
      method: "POST",
      body: JSON.stringify({ station_id, customer_id })
    });
  }

  async checkoutSession(session_id, payment_mode = "CASH") {
    return await this.request("/sessions/checkout", {
      method: "POST",
      body: JSON.stringify({ session_id, payment_mode })
    });
  }

  // Customers
  async getCustomers() {
    return await this.request("/customers");
  }

  async createCustomer(data) {
    return await this.request("/customers", {
      method: "POST",
      body: JSON.stringify(data)
    });
  }

  // Inventory
  async getInventory() {
    return await this.request("/inventory");
  }

  // Expenses
  async getExpenses() {
    return await this.request("/expenses");
  }

  async createExpense(data) {
    return await this.request("/expenses", {
      method: "POST",
      body: JSON.stringify(data)
    });
  }

  // POS Checkout
  async checkoutPOS(data) {
    return await this.request("/pos/checkout", {
      method: "POST",
      body: JSON.stringify(data)
    });
  }

  async getUPIQR(amount) {
    return await this.request(`/pos/upi-qr?amount=${amount}`);
  }

  // Audit Logs
  async getAuditLogs() {
    return await this.request("/audit-logs");
  }

  // Sync Batch
  async syncBatch(deviceId, pendingTransactions) {
    return await this.request("/sync/batch", {
      method: "POST",
      body: JSON.stringify({ device_id: deviceId, pending_transactions: pendingTransactions })
    });
  }
}

const api = new ApiClient();
