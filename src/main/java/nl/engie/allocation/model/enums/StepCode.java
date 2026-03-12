package nl.engie.allocation.model.enums;

/**
 * All pipeline step codes matching the structure from test2.docx.
 * Steps MUST be executed in strict order: 1A before 1B, 1B before 1C, etc.
 */
public enum StepCode {
    // Phase 1: Ontvang marktbericht
    STEP_1A(1, "Ontvangen marktbericht", "Ontvang marktbericht"),
    STEP_1B(2, "Technische ontvangstbevestiging", "Ontvang marktbericht"),
    STEP_1C(3, "Technische validatie", "Ontvang marktbericht"),
    STEP_1D(4, "Logging van ontvangsttijd", "Ontvang marktbericht"),
    STEP_1E(5, "Identificatie berichttype", "Ontvang marktbericht"),
    STEP_1F(6, "Handmatige opvoer berichten", "Ontvang marktbericht"),

    // Phase 2: Classificeer bericht
    STEP_2A(7, "Classificeer van berichttype", "Classificeer bericht"),
    STEP_2B(8, "Bepalen prioriteit per berichttype", "Classificeer bericht"),
    STEP_2C(9, "Plaatsen in wachtrij (event-driven)", "Classificeer bericht"),
    STEP_2D(10, "Uitzondering parkeren", "Classificeer bericht"),
    STEP_2E(11, "Uitval opnieuw verwerken", "Classificeer bericht"),

    // Phase 3: Valideer bericht
    STEP_3A(12, "Operational BRP register", "Valideer bericht"),
    STEP_3B(13, "Uitvoeren marktbusiness validaties", "Valideer bericht"),
    STEP_3C(14, "Controle op verplichte velden", "Valideer bericht"),
    STEP_3D(15, "Validatieregels configureerbaar", "Valideer bericht"),
    STEP_3E(16, "Tijdvenster-validaties", "Valideer bericht"),
    STEP_3F(17, "Controle op volgordelijkheid", "Valideer bericht"),
    STEP_3G(18, "Herbruikbare validatieregels", "Valideer bericht"),

    // Phase 4: Bepaal uitkomst
    STEP_4A(19, "Genereren ACK bij succes", "Bepaal uitkomst"),
    STEP_4B(20, "Genereren NACK bij fouten", "Bepaal uitkomst"),
    STEP_4C(21, "Toevoegen foutcodes bij NACK", "Bepaal uitkomst"),
    STEP_4D(22, "Vastleggen validatieresultaat", "Bepaal uitkomst"),
    STEP_4E(23, "Configuratie: NACK wel/niet intern doorzetten", "Bepaal uitkomst"),

    // Phase 5: Versuur marktrespons
    STEP_5A(24, "Versturen ACK/NACK richting markt", "Versuur marktrespons"),
    STEP_5B(25, "Geconfigureerd respons versturen", "Versuur marktrespons"),
    STEP_5C(26, "Logging verzendtijd", "Versuur marktrespons"),
    STEP_5D(27, "Zelfstandig versturen uitgaande berichten", "Versuur marktrespons"),

    // Phase 6: Lever bericht aan mdp
    STEP_6A(28, "Doorzetten origineel bericht naar raw-layer", "Lever bericht aan mdp"),
    STEP_6B(29, "Vastleggen afleverstatus", "Lever bericht aan mdp");

    private final int order;
    private final String stepName;
    private final String phaseName;

    StepCode(int order, String stepName, String phaseName) {
        this.order = order;
        this.stepName = stepName;
        this.phaseName = phaseName;
    }

    public int getOrder() { return order; }
    public String getStepName() { return stepName; }
    public String getPhaseName() { return phaseName; }

    /**
     * Returns the next step in the pipeline, or null if this is the last step.
     */
    public StepCode next() {
        StepCode[] codes = values();
        int nextOrdinal = this.ordinal() + 1;
        return nextOrdinal < codes.length ? codes[nextOrdinal] : null;
    }

    /**
     * Returns the previous step in the pipeline, or null if this is the first step.
     */
    public StepCode previous() {
        int prevOrdinal = this.ordinal() - 1;
        return prevOrdinal >= 0 ? values()[prevOrdinal] : null;
    }
}
