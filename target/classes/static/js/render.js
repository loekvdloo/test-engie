/* ===================================================
   render.js  –  All DOM-rendering functions
   =================================================== */

const Render = {

    /* --------------------------------------------------
       Message list
    -------------------------------------------------- */

    /**
     * Render the full message list into #messageList.
     * @param {Array} messages
     */
    messageList(messages) {
        const container = document.getElementById('messageList');

        if (!messages.length) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="icon">📭</div>
                    <h3>Geen berichten gevonden</h3>
                    <p>Gebruik de "Laad Test Data" knop om testberichten aan te maken.</p>
                </div>`;
            return;
        }

        container.innerHTML = messages.map(m => this.messageItem(m)).join('');
    },

    /**
     * Produce the HTML for a single message row.
     * @param {Object} msg
     * @returns {string}
     */
    messageItem(msg) {
        const statusIcon = msg.responseType === 'NACK'  ? '❌'
                         : msg.responseType === 'ACK'   ? '✅'
                         : msg.status === 'FAILED'      ? '❌'
                         : msg.status === 'COMPLETED'   ? '✅'
                         :                                '⏳';

        const isFailed = msg.responseType === 'NACK' || msg.status === 'FAILED';

        const badgeClass = Utils.responseBadgeClass(msg.responseType);
        const badgeLabel = msg.responseType || 'PENDING';

        const completedSteps = (msg.steps || []).filter(s => s.status === 'COMPLETED').length;
        const totalSteps     = (msg.steps || []).length;

        const errorCount = (msg.errorCodes || []).length;
        const errorBadge = errorCount
            ? `<span class="error-count-badge" title="${errorCount} validatie fout(en)">⚠️ ${errorCount}</span>`
            : '';

        return `
        <div class="message-item ${isFailed ? 'failed' : ''}"
             onclick="App.openDetail('${Utils.escapeHtml(msg.messageUuid)}')">
            <div class="message-main">
                <span class="status-icon">${statusIcon}</span>
                <div class="message-info">
                    <div class="message-type">${Utils.escapeHtml(msg.messageType || 'ALLOCATIE')}</div>
                    <div class="message-meta">
                        ${Utils.formatTime(msg.receivedAt)} ·
                        Stap ${completedSteps}/${totalSteps}
                    </div>
                </div>
            </div>
            <div class="message-badges">
                ${errorBadge}
                <span class="response-badge ${badgeClass}">${badgeLabel}</span>
            </div>
        </div>`;
    },

    /* --------------------------------------------------
       Stats bar
    -------------------------------------------------- */

    /**
     * Update the four stat counters.
     * @param {Array} messages
     */
    updateStats(messages) {
        document.getElementById('statTotal').textContent   = messages.length;
        document.getElementById('statSuccess').textContent = messages.filter(m => m.responseType === 'ACK').length;
        document.getElementById('statFailed').textContent  = messages.filter(m => m.responseType === 'NACK' || m.status === 'FAILED').length;

        if (messages.length) {
            const sorted = [...messages].sort((a, b) =>
                new Date(b.receivedAt) - new Date(a.receivedAt));
            document.getElementById('statLastTime').textContent = Utils.formatTime(sorted[0].receivedAt);
        } else {
            document.getElementById('statLastTime').textContent = '-';
        }
    },

    /* --------------------------------------------------
       Detail panel body
    -------------------------------------------------- */

    /**
     * Fill #detailBody with meta-info, errors (if any), and pipeline.
     * @param {Object} msg  – full message detail from API
     */
    detailPanel(msg) {
        document.getElementById('detailTitle').textContent = msg.messageType || 'Bericht Details';
        document.getElementById('detailUuid').textContent  = msg.messageUuid;

        let html = '';
        html += this.metaSection(msg);
        html += this.errorSection(msg.errorCodes);
        html += this.pipelineSection(msg.steps, msg.responseType, msg.errorCodes);

        document.getElementById('detailBody').innerHTML = html;
    },

    /* --------------------------------------------------
       Meta grid
    -------------------------------------------------- */

    /**
     * Build the key/value metadata grid.
     * @param {Object} msg
     * @returns {string}
     */
    metaSection(msg) {
        const badgeClass = Utils.responseBadgeClass(msg.responseType);
        const badgeLabel = msg.responseType || 'PROCESSING';

        return `
        <div class="meta-grid">
            <div class="meta-item">
                <span class="meta-label">Status</span>
                <span class="meta-value">${Utils.escapeHtml(msg.status)}</span>
            </div>
            <div class="meta-item">
                <span class="meta-label">Huidige stap</span>
                <span class="meta-value">${Utils.escapeHtml(msg.currentStep || '-')}</span>
            </div>
            <div class="meta-item">
                <span class="meta-label">Ontvangen</span>
                <span class="meta-value">${Utils.formatTime(msg.receivedAt)}</span>
            </div>
            <div class="meta-item">
                <span class="meta-label">Voltooid</span>
                <span class="meta-value">${Utils.formatTime(msg.completedAt)}</span>
            </div>
            <div class="meta-item">
                <span class="meta-label">Prioriteit</span>
                <span class="meta-value">${msg.priority ?? '-'}</span>
            </div>
            <div class="meta-item">
                <span class="meta-label">Antwoord</span>
                <span class="meta-value"><span class="response-badge ${badgeClass}">${badgeLabel}</span></span>
            </div>
        </div>`;
    },

    /* --------------------------------------------------
       Error codes section (NACK details)
    -------------------------------------------------- */

    /**
     * Render validation error cards for NACK messages.
     * Always rendered; hidden when there are no errors.
     * @param {Array|null} errorCodes
     * @returns {string}
     */
    errorSection(errorCodes) {
        if (!errorCodes || !errorCodes.length) return '';

        const cards = errorCodes.map(e => `
            <div class="error-card">
                <span class="error-code-badge">${Utils.escapeHtml(e.code)}</span>
                <span class="error-rule-code">${Utils.escapeHtml(e.ruleCode)}</span>
                <span class="error-message-text">${Utils.escapeHtml(e.message)}</span>
            </div>`).join('');

        return `
        <div class="error-section">
            <div class="error-section-title">
                ⚠️ Validatiefouten
                <span class="error-count">${errorCodes.length}</span>
            </div>
            ${cards}
        </div>`;
    },

    /* --------------------------------------------------
       Pipeline visualisation
    -------------------------------------------------- */

    /**
     * Group steps by phase and render the pipeline.
     * @param {Array|null}  steps
     * @param {string|null} responseType  – 'ACK', 'NACK', or null
     * @param {Array|null}  errorCodes    – validation errors (for NACK)
     * @returns {string}
     */
    pipelineSection(steps, responseType, errorCodes) {
        if (!steps || !steps.length) return '<p style="color:var(--text-muted)">Geen pipeline stappen beschikbaar.</p>';

        const isNack  = responseType === 'NACK';
        // Keywords in resultMessage that signal a problem
        const warnRe  = /mislukt|fout|nack|geen ack/i;

        // Group by phaseName
        const phases = [];
        let current = null;
        for (const step of steps) {
            if (!current || current.name !== step.phaseName) {
                current = { name: step.phaseName, steps: [] };
                phases.push(current);
            }
            current.steps.push(step);
        }

        const phaseHtml = phases.map(phase => {
            const stepsHtml = phase.steps.map(s => {
                // Detect warning steps: NACK message + result message contains failure keywords
                const isWarning = isNack && s.resultMessage && warnRe.test(s.resultMessage);
                const icon = isWarning ? '⚠️' : Utils.getStepIcon(s.status);
                const statusClass = isWarning ? 'warning'
                                  : (s.status || '').toLowerCase();
                const errorLine = s.errorMessage
                    ? `<div class="step-error">${Utils.escapeHtml(s.errorMessage)}</div>`
                    : '';
                const resultLine = s.resultMessage
                    ? `<div class="step-result ${isWarning ? 'step-result-warning' : ''}">${Utils.escapeHtml(s.resultMessage)}</div>`
                    : '';
                const time = s.completedAt ? Utils.formatTime(s.completedAt) : '';

                return `
                <div class="step-item ${statusClass}">
                    <div class="step-icon">${icon}</div>
                    <div class="step-content">
                        <div class="step-header">
                            <span class="step-name">${Utils.escapeHtml(s.stepName)}</span>
                            <span class="step-code-label">${Utils.escapeHtml(s.stepCode)}</span>
                            ${time ? `<span class="step-time">${time}</span>` : ''}
                        </div>
                        ${resultLine}
                        ${errorLine}
                    </div>
                </div>`;
            }).join('');

            return `
            <div class="phase-group">
                <div class="phase-label">${Utils.escapeHtml(phase.name)}</div>
                <div class="step-list">${stepsHtml}</div>
            </div>`;
        }).join('');

        return `
        <div class="pipeline-title">Pipeline Stappen</div>
        ${phaseHtml}`;
    }
};
