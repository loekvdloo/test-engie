/* ===================================================
   app.js  –  Application state & event handlers
   =================================================== */

const App = {

    /* ---------- State ---------- */
    state: {
        messages: [],
        filter:   'all',    // 'all' | 'success' | 'failed'
        sort:     'newest'  // 'newest' | 'oldest'
    },

    /* ---------- Init ---------- */

    init() {
        this.bindEvents();
        this.refresh();
    },

    /* ---------- Event bindings (CSP-compliant, geen inline handlers) ---------- */

    bindEvents() {
        // Header buttons
        document.getElementById('seedBtn').addEventListener('click', () => this.seedTestData());
        document.getElementById('refreshBtn').addEventListener('click', () => this.refresh());
        document.getElementById('exportJsonBtn').addEventListener('click', () => this.exportErrorOverview('json'));
        document.getElementById('exportCsvBtn').addEventListener('click', () => this.exportErrorOverview('csv'));

        // Filter buttons
        document.querySelectorAll('.filter-btn[data-filter]').forEach(btn => {
            btn.addEventListener('click', () => this.setFilter(btn.dataset.filter));
        });

        // Sort buttons
        document.querySelectorAll('.filter-btn[data-sort]').forEach(btn => {
            btn.addEventListener('click', () => this.setSort(btn.dataset.sort));
        });

        // Detail panel: overlay + close button
        document.getElementById('overlay').addEventListener('click', () => this.closeDetail());
        document.getElementById('closeDetailBtn').addEventListener('click', () => this.closeDetail());

        // Message list: event delegation for dynamically rendered items
        document.getElementById('messageList').addEventListener('click', (e) => {
            const item = e.target.closest('.message-item[data-uuid]');
            if (item) this.openDetail(item.dataset.uuid);
        });
    },

    /* ---------- Data ---------- */

    async refresh() {
        try {
            this.state.messages = await Api.fetchMessages();
            this.applyAndRender();
        } catch (err) {
            console.error('Fout bij ophalen berichten:', err);
            document.getElementById('messageList').innerHTML =
                '<div class="empty-state"><div class="icon">⚠️</div><h3>Kan berichten niet laden</h3><p>' +
                Utils.escapeHtml(err.message) + '</p></div>';
        }
    },

    /* ---------- Filter / Sort ---------- */

    setFilter(value) {
        this.state.filter = value;

        document.querySelectorAll('.filter-btn[data-filter]').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.filter === value);
        });

        this.applyAndRender();
    },

    setSort(value) {
        this.state.sort = value;

        document.querySelectorAll('.filter-btn[data-sort]').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.sort === value);
        });

        this.applyAndRender();
    },

    /**
     * Apply current filter + sort to this.state.messages and re-render.
     */
    applyAndRender() {
        let list = [...this.state.messages];

        // Filter
        if (this.state.filter === 'success') {
            list = list.filter(m => m.responseType === 'ACK');
        } else if (this.state.filter === 'failed') {
            list = list.filter(m => m.responseType === 'NACK' || m.status === 'FAILED');
        }

        // Sort
        list.sort((a, b) => {
            const da = new Date(a.receivedAt);
            const db = new Date(b.receivedAt);
            return this.state.sort === 'newest' ? db - da : da - db;
        });

        Render.messageList(list);
        Render.updateStats(this.state.messages);
    },

    /* ---------- Detail panel ---------- */

    async openDetail(uuid) {
        try {
            const msg = await Api.fetchMessageDetail(uuid);

            // Show panel + overlay
            document.getElementById('detailPanel').classList.add('open');
            document.getElementById('overlay').classList.add('active');

            Render.detailPanel(msg);
        } catch (err) {
            console.error('Fout bij ophalen details:', err);
            Utils.showToast('Kan details niet laden: ' + err.message);
        }
    },

    closeDetail() {
        document.getElementById('detailPanel').classList.remove('open');
        document.getElementById('overlay').classList.remove('active');
    },

    /* ---------- Seed test data ---------- */

    async seedTestData() {
        const btn = document.getElementById('seedBtn');
        btn.disabled = true;
        btn.textContent = '⏳ Laden...';

        try {
            await Api.seedTestData();
            Utils.showToast('✅ Testdata geladen!');
            await this.refresh();
        } catch (err) {
            console.error('Seed fout:', err);
            Utils.showToast('❌ Fout bij laden testdata');
        } finally {
            btn.disabled = false;
            btn.textContent = '🧪 Laad Test Data';
        }
    },

    exportErrorOverview(format) {
        const rows = [];
        for (const message of this.state.messages) {
            const errors = message.errorCodes || [];
            for (const error of errors) {
                if (!error || !error.code || error.code === 'VALIDATION_FAILED') continue;
                rows.push({
                    messageUuid: message.messageUuid,
                    messageType: message.messageType,
                    status: message.status,
                    responseType: message.responseType,
                    receivedAt: message.receivedAt,
                    errorCode: error.code,
                    ruleCode: error.ruleCode,
                    errorMessage: error.message
                });
            }
        }

        if (!rows.length) {
            Utils.showToast('Geen foutcodes gevonden om te exporteren');
            return;
        }

        const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

        if (format === 'json') {
            const json = JSON.stringify(rows, null, 2);
            this.downloadFile(`foutoverzicht-${timestamp}.json`, 'application/json;charset=utf-8', json);
            Utils.showToast(`✅ JSON export klaar (${rows.length} regels)`);
            return;
        }

        const header = ['messageUuid', 'messageType', 'status', 'responseType', 'receivedAt', 'errorCode', 'ruleCode', 'errorMessage'];
        const csvLines = [header.join(',')];
        for (const row of rows) {
            const values = header.map(key => {
                const value = row[key] == null ? '' : String(row[key]);
                return `"${value.replace(/"/g, '""')}"`;
            });
            csvLines.push(values.join(','));
        }
        const csv = csvLines.join('\n');
        this.downloadFile(`foutoverzicht-${timestamp}.csv`, 'text/csv;charset=utf-8', csv);
        Utils.showToast(`✅ CSV export klaar (${rows.length} regels)`);
    },

    downloadFile(filename, mimeType, content) {
        const blob = new Blob([content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    }
};

/* ---------- Bootstrap ---------- */
document.addEventListener('DOMContentLoaded', () => App.init());
