// IndexedDB Offline Queue Store & Auto Sync Manager
class OfflineStore {
  constructor() {
    this.dbName = "BereezeOfflineDB";
    this.storeName = "pending_transactions";
    this.db = null;
    this.deviceId = "device_" + Math.random().toString(36).substring(2, 9);
    this.init();
  }

  async init() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, 1);
      request.onupgradeneeded = (e) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains(this.storeName)) {
          db.createObjectStore(this.storeName, { keyPath: "id", autoIncrement: true });
        }
      };
      request.onsuccess = (e) => {
        this.db = e.target.result;
        resolve();
      };
      request.onerror = (e) => reject(e);
    });
  }

  async enqueueTransaction(type, data) {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const tx = this.db.transaction([this.storeName], "readwrite");
      const store = tx.objectStore(this.storeName);
      const record = { type, data, timestamp: new Date().toISOString() };
      const req = store.add(record);
      req.onsuccess = () => resolve(req.result);
      req.onerror = (e) => reject(e);
    });
  }

  async getPendingTransactions() {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const tx = this.db.transaction([this.storeName], "readonly");
      const store = tx.objectStore(this.storeName);
      const req = store.getAll();
      req.onsuccess = () => resolve(req.result);
      req.onerror = (e) => reject(e);
    });
  }

  async clearPendingTransactions() {
    if (!this.db) await this.init();
    return new Promise((resolve, reject) => {
      const tx = this.db.transaction([this.storeName], "readwrite");
      const store = tx.objectStore(this.storeName);
      const req = store.clear();
      req.onsuccess = () => resolve();
      req.onerror = (e) => reject(e);
    });
  }

  async syncPending() {
    try {
      const pending = await this.getPendingTransactions();
      if (pending && pending.length > 0) {
        console.log(`Syncing ${pending.length} offline transactions...`);
        const res = await api.syncBatch(this.deviceId, pending);
        console.log("Sync response:", res);
        await this.clearPendingTransactions();
      }
    } catch (e) {
      console.warn("Offline sync attempt skipped or failed:", e);
    }
  }
}

const offlineStore = new OfflineStore();
window.addEventListener("online", () => offlineStore.syncPending());
