/* ===================================================
   utils.js  –  Small, pure helper functions
   =================================================== */

const Utils = {

    /**
     * Format an ISO date-time string for display.
     * @param {string|null} dateStr
     * @returns {string}
     */
    formatTime(dateStr) {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        const dag   = String(d.getDate()).padStart(2, '0');
        const maand = ['jan','feb','mrt','apr','mei','jun','jul','aug','sep','okt','nov','dec'][d.getMonth()];
        const uur   = String(d.getHours()).padStart(2, '0');
        const min   = String(d.getMinutes()).padStart(2, '0');
        return `${dag} ${maand} ${uur}:${min}`;
    },

    /**
     * Escape HTML entities to prevent XSS.
     * @param {string} str
     * @returns {string}
     */
    escapeHtml(str) {
        if (!str) return '';
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return String(str).replace(/[&<>"']/g, c => map[c]);
    },

    /**
     * Show a small toast notification at the bottom-right.
     * @param {string} message
     * @param {number} [duration=3000]
     */
    showToast(message, duration = 3000) {
        const el = document.getElementById('toast');
        el.textContent = message;
        el.classList.add('show');
        setTimeout(() => el.classList.remove('show'), duration);
    },

    /**
     * Return an emoji icon for a pipeline step status.
     * @param {string} status
     * @returns {string}
     */
    getStepIcon(status) {
        const icons = {
            COMPLETED: '✅',
            FAILED:    '❌',
            SKIPPED:   '⏭️',
            RUNNING:   '⏳',
            PENDING:   '⏸️'
        };
        return icons[status] || '⚫';
    },

    /**
     * Return the response-type badge class suffix.
     * @param {string} type  e.g. "ACK" | "NACK" | null
     * @returns {string}
     */
    responseBadgeClass(type) {
        if (type === 'ACK')  return 'ack';
        if (type === 'NACK') return 'nack';
        return 'pending';
    }
};
