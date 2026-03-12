/* ===================================================
   api.js  –  All backend HTTP calls
   =================================================== */

const Api = {

    BASE: '/api/messages',

    /**
     * Fetch every message (overview list).
     * @returns {Promise<Array>}
     */
    async fetchMessages() {
        const res = await fetch(this.BASE);
        if (!res.ok) throw new Error(`GET ${this.BASE} → ${res.status}`);
        return res.json();
    },

    /**
     * Fetch full detail for a single message.
     * @param {string} uuid
     * @returns {Promise<Object>}
     */
    async fetchMessageDetail(uuid) {
        const url = `${this.BASE}/${encodeURIComponent(uuid)}`;
        const res = await fetch(url);
        if (!res.ok) throw new Error(`GET ${url} → ${res.status}`);
        return res.json();
    },

    /**
     * Call the test-data seed endpoint.
     * @returns {Promise<Object>}
     */
    async seedTestData() {
        const res = await fetch('/api/test/seed', { method: 'POST' });
        if (!res.ok) throw new Error(`POST /api/test/seed → ${res.status}`);
        return res.json();
    }
};
