package nl.engie.allocation.model.enums;

/**
 * Officiële foutcodes conform Business Service Uitwisselen Allocatiegegevens Elektriciteit v4.0.
 * Bron: docs/Business-Service-Uitwisselen-allocatiegegevens-elektriciteit-v4.0.pdf (pagina 17-19)
 *
 * Bij een NACK worden de bijbehorende foutcodes meegegeven in de Reason/code van het Acknowledgement.
 */
public enum ErrorCode {

    // ── Tijdserie-controles ──────────────────────────────────────────────────
    E_650("650", "EAN-code-18 is niet valide.",
            "De EAN-code-18 van de aansluiting is een valide EAN-code.", "BRP/LNB"),

    E_651("651", "EAN-code-18 is niet valide.",
            "De EAN-code-18 van het netgebied is een valide EAN-code.", "BRP/LNB"),

    E_663("663", "De periode waarop het bericht betrekking heeft is niet juist.",
            "Bij een dagbericht elektriciteit is de periode precies 1 kalenderdag.", "BRP/LNB"),

    E_667("667", "Een of meerdere producten passen niet bij de productsoort.",
            "Alle producten passen bij de productsoort.", "BRP/LNB"),

    E_668("668", "Een of meerdere energie-eenheden passen niet bij het/de product/producten.",
            "Alle energie-eenheden passen bij de producten.", "BRP/LNB"),

    E_670("670", "Er is al eerder een bericht met dit kenmerk ontvangen.",
            "Het kenmerk van het bericht is uniek.", "BRP/LNB"),

    E_671("671", "Het aantal posities is onjuist.",
            "Het aantal posities van alle tijdseries past bij de periode en de resolutie.", "BRP/LNB"),

    E_676("676", "Eerste positie begint niet met '1'.",
            "De eerste positie in een tijdserie is '1'.", "BRP/LNB"),

    E_683("683", "De combinatie van herkomstindicatie, validatiestatus en reparatiemethodiek is geen geldige combinatie.",
            "De combinatie van herkomstindicatie, validatiestatus en reparatiemethodiek is een geldige combinatie.", "BRP/LNB"),

    E_686("686", "Volume met negatieve waarde.",
            "Alle volumes in het bericht zijn positieve getallen (of nul).", "BRP/LNB"),

    E_776("776", "Volume heeft een onjuist aantal decimalen.",
            "Het volume heeft een correct aantal decimalen.", "BRP/LNB"),

    E_777("777", "Het netgebied of het allocatiepunt i.c.m. de netbeheerder, komt niet voor in de administratie van de landelijke netbeheerder.",
            "De EAN-code-18 van het netgebied of het allocatiepunt i.c.m. de netbeheerder, is actief en geadministreerd bij de landelijke netbeheerder.", "LNB"),

    E_758("758", "Ontbrekende of onjuiste EAN-13.",
            "Het bericht bevat exact één keer de EAN-13 van een marktpartij met de verwachte marktrol.", "BRP/LNB"),

    E_759("759", "Ontbrekende EAN 13 van de BRP.",
            "Het bericht bevat de EAN 13 van de marktpartij met de marktrol BRP.", "LNB"),

    E_761("761", "De BRP-er is niet actief als BRP-er in het netgebied voor deze allocatiegroep.",
            "De BRP is in de gehele berichtperiode volgens het aansluitingenregister voor tenminste één allocatiepunt binnen deze allocatiegroep in het betreffende netgebied geregistreerd.", "BRP"),

    E_763("763", "Het dagbericht met allocatiegegevens is te laat opgeleverd.",
            "Het dagbericht met allocatiegegevens is tijdig opgeleverd.", "BRP/LNB"),

    E_764("764", "Het aantal tijdseries in het bericht past niet bij de allocatiegroep.",
            "Het aantal tijdseries in het bericht past bij de allocatiegroep.", "BRP/LNB"),

    E_765("765", "De BRP is volgens het BRP-erkenningenregister niet bekend en erkend.",
            "De BRP is volgens het BRP-erkenningenregister bekend en erkend.", "LNB"),

    E_769("769", "Er is al eerder een allocatiegegevens bericht met deze allocatierun identificatie ontvangen.",
            "De allocatierun identificatie is uniek.", "BRP/LNB"),

    E_771("771", "Het vastgesteld afnametype past niet bij de profielcategorie.",
            "Het vastgesteld afnametype past bij de profielcategorie.", "BRP/LNB"),

    E_772("772", "De gegevens hebben (deels) betrekking op de toekomst.",
            "De periode waarop het bericht betrekking heeft ligt tenminste 1 dag in het verleden.", "BRP/LNB"),

    E_773("773", "Resolutie past niet bij deze gegevens.",
            "De resolutie past bij de afspraken voor deze gegevens.", "BRP/LNB"),

    E_774("774", "Factor heeft een onjuist aantal decimalen.",
            "De factor heeft een correct aantal decimalen.", "BRP/LNB"),

    E_779("779", "Het aantal tijdseries met profielfracties past niet bij de profielcategorie.",
            "Het aantal tijdseries met profielfracties past bij de profielcategorie.", "BRP/LNB"),

    E_781("781", "De status profielfracties past niet bij de profielcategorie.",
            "De status profielfracties past bij de profielcategorie.", "BRP/LNB"),

    E_782("782", "Het increment van een tijdserie is geen '1'.",
            "De posities binnen een tijdserie lopen op met een increment van '1'.", "BRP/LNB"),

    // ── Header-controles ─────────────────────────────────────────────────────
    E_669("669", "Er is al eerder een bericht met dit MessageID ontvangen.",
            "Het MessageID in de header van het bericht is uniek.", "BRP/LNB"),

    E_681("681", "ProcessTypeID past niet bij de inhoud van het bericht.",
            "Het ProcessTypeID in de header van het bericht past bij de inhoud van het bericht.", "BRP/LNB"),

    E_704("704", "Er is al een bericht ontvangen met een recentere creatie datum/tijdstempel.",
            "Er is geen bericht ontvangen met een recentere creatie datum/tijdstempel (\"latest and greatest\").", "BRP/LNB"),

    E_701("701", "SenderID in de SOAP Header is niet gelijk aan SenderID in de Business Document Header.",
            "Het senderID in de SOAP Header is gelijk aan het SenderID in de Business Document Header.", "BRP/LNB"),

    E_745("745", "ReceiverID in de SOAP Header is niet gelijk aan ReceiverID in de Business Document Header.",
            "Het receiverID in de SOAP Header is gelijk aan het ReceiverID in de Business Document Header.", "BRP/LNB"),

    E_747("747", "ProcessTypeID past niet bij de ontvanger van het bericht.",
            "Het ProcessTypeID in de header van het bericht past bij de ontvanger van het bericht.", "BRP/LNB"),

    E_754("754", "ContentType in de SOAP Header is niet in lijn met ProcessTypeID in de Business Document Header.",
            "Het contentType in de SOAP Header is in lijn met het ProcessTypeID in de Business Document Header.", "BRP/LNB"),

    E_780("780", "Het CorrelationID in de Business Document Header is niet gelijk aan het CorrelationID in de SOAP Header.",
            "Het CorrelationID in de Business Document Header is, indien gevuld, gelijk aan het CorrelationID in de SOAP Header.", "BRP/LNB"),

    // ── Geheel bericht ───────────────────────────────────────────────────────
    E_999("999", "[vrij tekstveld]",
            "Bericht is volledig, correct en gericht aan de juiste ontvanger.", "BRP/LNB");

    private final String code;
    private final String foutmelding;
    private final String controle;
    private final String rol;

    ErrorCode(String code, String foutmelding, String controle, String rol) {
        this.code = code;
        this.foutmelding = foutmelding;
        this.controle = controle;
        this.rol = rol;
    }

    public String getCode() { return code; }
    public String getFoutmelding() { return foutmelding; }
    public String getControle() { return controle; }
    public String getRol() { return rol; }

    /**
     * Zoek een ErrorCode op basis van numerieke code.
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode ec : values()) {
            if (ec.code.equals(code)) return ec;
        }
        return null;
    }

    @Override
    public String toString() { return code; }
}
