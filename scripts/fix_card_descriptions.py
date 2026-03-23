#!/usr/bin/env python3
"""Update Trello card descriptions with proper Markdown formatting."""
import json
import os
import sys
import urllib.request
from urllib.parse import urlencode

class TrelloClient:
    def __init__(self, api_key: str, token: str):
        self.api_key = api_key
        self.token = token
        self.base_url = "https://api.trello.com/1"

    def _request(self, method: str, endpoint: str, params: dict):
        params["key"] = self.api_key
        params["token"] = self.token
        url = f"{self.base_url}{endpoint}"
        if method == "GET":
            url += "?" + urlencode(params)
            req = urllib.request.Request(url, method=method)
        else:
            data = json.dumps(params).encode("utf-8")
            req = urllib.request.Request(url, data=data, method=method,
                                         headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(req) as r:
            return json.loads(r.read().decode("utf-8"))

    def get_board_cards(self, board_id: str) -> list:
        return self._request("GET", f"/boards/{board_id}/cards",
                             {"fields": "id,name,desc,idList"})

    def update_card_desc(self, card_id: str, desc: str):
        return self._request("PUT", f"/cards/{card_id}", {"desc": desc})


# ─────────────────────────────────────────────────────────────────
# Markdown-formatted descriptions, keyed by card title
# ─────────────────────────────────────────────────────────────────
DESCRIPTIONS = {

    # ── MVP Fasering ──────────────────────────────────────────────
    "Fase 1 (Blauw) - End-to-end": (
        "## Fase 1 — End-to-end (Blauw)\n\n"
        "Implementeer als eerste alle **blauwe** stories zodat de volledige pipeline van bericht-ontvangst "
        "tot aflevering aantoonbaar end-to-end werkt.\n\n"
        "**Volgorde:** Fase 1 → Fase 2 (Geel) → Fase 3 (Oranje) → Fase 4 (Groen)"
    ),
    "Fase 2 (Geel) - Validaties": (
        "## Fase 2 — Validaties (Geel)\n\n"
        "Na afronding van Fase 1 volgen alle **gele** validatiestories.\n\n"
        "Doel: alle inhoudelijke validatieregels (BRP, business rules, tijdvensters) zijn geïmplementeerd en getest."
    ),
    "Fase 3 (Oranje) - Configuratie": (
        "## Fase 3 — Configuratie (Oranje)\n\n"
        "Na Fase 2 volgt het **oranje** configuratiewerk:\n\n"
        "- Prioriteitstelling per berichttype\n"
        "- Wachtrij-inrichting\n"
        "- Configureerbare validatieregels\n"
        "- NACK-configuratie"
    ),
    "Fase 4 (Groen) - Dashboarding": (
        "## Fase 4 — Dashboarding (Groen)\n\n"
        "Sluitstuk van de MVP: **groen** staat voor rapportage, monitoring en afrondend werk.\n\n"
        "- Zelfstandig versturen (STEP 5D)\n"
        "- Real-time dashboard & monitoring"
    ),

    # ── User Stories ──────────────────────────────────────────────
    "US-01 - STEP_1A - Ontvangen marktbericht": (
        "## User Story\n\n"
        "Als **systeem** wil ik een marktbericht ontvangen zodat verwerking kan starten.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_1A | **Fase:** 1 (Blauw)\n\n"
        "De eerste stap in de pipeline. Lege of ontbrekende `xmlContent` stopt de pipeline direct."
    ),
    "US-02 - STEP_1B - Technische ontvangstbevestiging": (
        "## User Story\n\n"
        "Als **systeem** wil ik technische ontvangst bevestigen zodat ontvangst traceerbaar is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_1B | **Fase:** 1 (Blauw)\n\n"
        "Legt een unieke ontvangstreferentie vast. Wordt alleen uitgevoerd als 1A succesvol was."
    ),
    "US-03 - STEP_1C - Technische validatie": (
        "## User Story\n\n"
        "Als **systeem** wil ik XML technisch valideren zodat ongeldige XML wordt afgekeurd.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_1C | **Fase:** 1 (Blauw)\n\n"
        "Parseert de XML; onparseerbare inhoud stopt alle vervolgstappen."
    ),
    "US-04 - STEP_1D - Logging van ontvangsttijd": (
        "## User Story\n\n"
        "Als **operations** wil ik ontvangsttijd loggen zodat auditing mogelijk is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_1D | **Fase:** 1 (Blauw)\n\n"
        "Slaat exacte ontvangsttijd (ISO-8601) op bij het bericht."
    ),
    "US-05 - STEP_1E - Identificatie berichttype": (
        "## User Story\n\n"
        "Als **systeem** wil ik het berichttype herkennen zodat de juiste route wordt gebruikt.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_1E | **Fase:** 1 (Blauw)\n\n"
        "Root-element van de XML bepaalt het berichttype. Onbekende types falen in 1E."
    ),
    "US-06 - STEP_1F - Handmatige opvoer berichten": (
        "## User Story\n\n"
        "Als **operations** wil ik handmatige invoer markeren zodat de herkomst zichtbaar is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_1F | **Fase:** 1 (Blauw)\n\n"
        "`manualEntry=true` wordt vastgelegd op het bericht en is zichtbaar in metadata/status."
    ),
    "US-07 - STEP_2A - Classificeer van berichttype": (
        "## User Story\n\n"
        "Als **systeem** wil ik het berichttype classificeren zodat het juiste verwerkingspad gekozen wordt.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_2A | **Fase:** 1 (Blauw)\n\n"
        "Elk ondersteund type krijgt een classificatie die beschikbaar is voor vervolgstappen."
    ),
    "US-08 - STEP_2B - Bepalen prioriteit per type": (
        "## User Story\n\n"
        "Als **systeem** wil ik prioriteit bepalen zodat urgente berichten eerder worden verwerkt.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_2B | **Fase:** 3 (Oranje)\n\n"
        "Een prioriteitswaarde wordt toegekend en opgeslagen bij het bericht."
    ),
    "US-09 - STEP_2C - Plaatsen in wachtrij": (
        "## User Story\n\n"
        "Als **systeem** wil ik berichten in een wachtrij plaatsen zodat volgordelijke verwerking gegarandeerd is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_2C | **Fase:** 3 (Oranje)\n\n"
        "Wachtrij-positie volgt uit prioriteit en ontvangstvolgorde."
    ),
    "US-10 - STEP_2D - Uitzondering parkeren": (
        "## User Story\n\n"
        "Als **operations** wil ik uitzonderingsberichten parkeren zodat handmatige beoordeling mogelijk is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_2D | **Fase:** 4 (Groen)\n\n"
        "Status wijzigt naar `PARKED`; bericht is beschikbaar voor reprocess via `POST /api/messages/{uuid}/reprocess`."
    ),
    "US-11 - STEP_2E - Uitval opnieuw verwerken": (
        "## User Story\n\n"
        "Als **operations** wil ik uitgevallen berichten opnieuw verwerken zodat verlies wordt voorkomen.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_2E | **Fase:** 4 (Groen)\n\n"
        "Trigger: `POST /api/messages/{uuid}/reprocess` — alleen toegestaan voor `FAILED` of `PARKED`."
    ),
    "US-12 - STEP_3A - Operational BRP register": (
        "## User Story\n\n"
        "Als **systeem** wil ik BRP-registergegevens controleren zodat onbekende partijen worden afgewezen.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_3A | **Fase:** 2 (Geel)\n\n"
        "## Foutcodes\n\n"
        "- **761** BRP niet actief in netgebied\n"
        "- **765** BRP niet bekend/erkend\n"
        "- **777** Netgebied/allocatiepunt niet in landelijke administratie"
    ),
    "US-13 - STEP_3B - Uitvoeren marktbusiness validaties": (
        "## User Story\n\n"
        "Als **systeem** wil ik marktbusiness-validaties uitvoeren zodat inhoudelijk onjuiste berichten worden afgekeurd.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_3B | **Fase:** 2 (Geel)\n\n"
        "## Foutcodes (selectie)\n\n"
        "- **650** EAN-18 aansluiting niet valide\n"
        "- **651** EAN-18 netgebied niet valide\n"
        "- **701** SenderID SOAP ≠ SenderID BDH\n"
        "- **745** ReceiverID SOAP ≠ ReceiverID BDH\n"
        "- **999** Generieke fout"
    ),
    "US-14 - STEP_3C - Controle op verplichte velden": (
        "## User Story\n\n"
        "Als **systeem** wil ik verplichte velden controleren zodat incomplete berichten worden geweigerd.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_3C | **Fase:** 2 (Geel)\n\n"
        "## Foutcodes\n\n"
        "- **999** Generieke fout / vrij tekstveld"
    ),
    "US-15 - STEP_3D - Validatieregels configureerbaar": (
        "## User Story\n\n"
        "Als **beheerder** wil ik validatieregels configureren zodat aanpassingen zonder herdeployment mogelijk zijn.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_3D | **Fase:** 3 (Oranje)\n\n"
        "Regels zijn per berichttype in te stellen zonder code-aanpassing."
    ),
    "US-16 - STEP_3E - Tijdvenster-validaties": (
        "## User Story\n\n"
        "Als **systeem** wil ik tijdvensters valideren zodat te late of te vroege berichten worden afgekeurd.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_3E | **Fase:** 2 (Geel)\n\n"
        "## Foutcodes\n\n"
        "- **663** Periode onjuist\n"
        "- **772** Gegevens (deels) in de toekomst\n"
        "- **763** Dagbericht te laat opgeleverd"
    ),
    "US-17 - STEP_3F - Controle op volgordelijkheid": (
        "## User Story\n\n"
        "Als **systeem** wil ik volgordelijkheid controleren zodat positioneringsfouten worden gesignaleerd.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_3F | **Fase:** 2 (Geel)\n\n"
        "## Foutcodes\n\n"
        "- **671** Aantal posities onjuist\n"
        "- **676** Eerste positie begint niet met 1\n"
        "- **782** Increment van tijdserie is geen 1"
    ),
    "US-18 - STEP_3G - Herbruikbare validatieregels": (
        "## User Story\n\n"
        "Als **ontwikkelaar** wil ik validatieregels hergebruiken zodat dubbele implementatie voorkomen wordt.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_3G | **Fase:** 4 (Groen)\n\n"
        "## Foutcodes\n\n"
        "- **669** MessageID niet uniek\n"
        "- **670** Kenmerk bericht niet uniek\n"
        "- **704** Recentere versie al ontvangen\n"
        "- **999** Generieke fout"
    ),
    "US-19 - STEP_4A - Genereren ACK bij succes": (
        "## User Story\n\n"
        "Als **systeem** wil ik een ACK genereren bij succesvolle validatie zodat de zender bevestiging ontvangt.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_4A | **Fase:** 1 (Blauw)\n\n"
        "ACK wordt alleen gegenereerd als alle voorgaande stappen `COMPLETED` zijn."
    ),
    "US-20 - STEP_4B - Genereren NACK bij fouten": (
        "## User Story\n\n"
        "Als **systeem** wil ik een NACK genereren bij validatiefouten zodat de zender weet wat er mis is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_4B | **Fase:** 2 (Geel)\n\n"
        "NACK bevat één of meer foutcodes uit de officiële Business-Service spec."
    ),
    "US-21 - STEP_4C - Toevoegen foutcodes bij NACK": (
        "## User Story\n\n"
        "Als **systeem** wil ik officiële foutcodes toevoegen aan de NACK zodat de zender precies weet wat er fout is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_4C | **Fase:** 2 (Geel)\n\n"
        "Foutcodes komen uit `ErrorCode.java` (32 codes, Business-Service v4.0 spec)."
    ),
    "US-22 - STEP_4D - Vastleggen validatieresultaat": (
        "## User Story\n\n"
        "Als **operations** wil ik het validatieresultaat vastleggen zodat auditing mogelijk is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_4D | **Fase:** 2 (Geel)\n\n"
        "Resultaat (ACK/NACK + foutcodes) wordt persistent opgeslagen in de database."
    ),
    "US-23 - STEP_4E - NACK configuratie": (
        "## User Story\n\n"
        "Als **beheerder** wil ik NACK-gedrag configureren zodat responsen aanpasbaar zijn per berichttype.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_4E | **Fase:** 3 (Oranje)\n\n"
        "NACK-inhoud en foutcode-selectie zijn configureerbaar zonder herdeployment."
    ),
    "US-24 - STEP_5A - Versturen ACK/NACK": (
        "## User Story\n\n"
        "Als **systeem** wil ik de ACK/NACK versturen zodat de zender het resultaat ontvangt.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_5A | **Fase:** 1 (Blauw)\n\n"
        "Respons wordt teruggestuurd via hetzelfde kanaal als het inkomend bericht."
    ),
    "US-25 - STEP_5B - Geconfigureerd respons versturen": (
        "## User Story\n\n"
        "Als **systeem** wil ik een geconfigureerde respons versturen zodat per type de juiste boodschap verstuurd wordt.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_5B | **Fase:** 3 (Oranje)\n\n"
        "Respons-template is configureerbaar per berichttype en omgeving."
    ),
    "US-26 - STEP_5C - Logging verzendtijd": (
        "## User Story\n\n"
        "Als **operations** wil ik verzendtijd loggen zodat SLA-bewaking mogelijk is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_5C | **Fase:** 3 (Oranje)\n\n"
        "Verzendtijd (ISO-8601) wordt samen met correlation-id opgeslagen."
    ),
    "US-27 - STEP_5D - Zelfstandig versturen": (
        "## User Story\n\n"
        "Als **systeem** wil ik zelfstandig uitgaande berichten kunnen versturen voor toekomstige functionaliteit.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_5D | **Fase:** 4 (Groen)\n\n"
        "Stap is aanwezig in de pipeline maar kan in de huidige implementatie worden overgeslagen zonder fout."
    ),
    "US-28 - STEP_6A - Doorzetten naar raw-layer": (
        "## User Story\n\n"
        "Als **data-platform** wil ik het originele bericht in de raw-layer opslaan zodat brondata behouden blijft.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_6A | **Fase:** 1 (Blauw)\n\n"
        "Originele XML wordt weggeschreven naar de raw-layer; pad/locatie wordt vastgelegd in het stapresultaat."
    ),
    "US-29 - STEP_6B - Vastleggen afleverstatus": (
        "## User Story\n\n"
        "Als **operations** wil ik de definitieve afleverstatus vastleggen zodat de ketenstatus compleet is.\n\n"
        "## Pipeline-stap\n\n"
        "**Stap:** STEP_6B | **Fase:** 1 (Blauw)\n\n"
        "De pipeline sluit af met het opslaan van de eindstatus (`COMPLETED`, `FAILED` of `PARKED`)."
    ),

    # ── Project Scope ─────────────────────────────────────────────
    "Project Infrastructure - Java + PostgreSQL": (
        "## Technologie Stack\n\n"
        "- **Framework:** Spring Boot 3.x met Spring Data JPA, Spring Security, Spring Web\n"
        "- **Build:** Maven | **Tests:** JUnit 5\n"
        "- **Database:** PostgreSQL — transactionele service-layer\n"
        "- **Pipeline:** 29-staps synchrone orchestratie met fase-blokkering\n\n"
        "## Beveiliging\n\n"
        "- CORS: same-origin only\n"
        "- Headers: CSP, X-Frame-Options SAMEORIGIN, HSTS (31536000s), Referrer-Policy, Permissions-Policy\n"
        "- XXE-bescherming op XML-parsing\n"
        "- Rate Limiting: 3 buckets per IP (30/min submit, 5/min seed, 120/min algemeen)\n"
        "- Input Validation: UUID/EAN-patronen, 2 MB-limiet, control-char sanitisatie"
    ),
    "API Contract - POST /api/messages": (
        "## Doel\n\n"
        "Submit een JSON-bericht naar de pipeline.\n\n"
        "## Technische details\n\n"
        "- **File:** `MarketMessageController.java` (regels 32–39)\n"
        "- **Response:** UUID + initiële status `RECEIVED`\n\n"
        "## HTTP-gedrag\n\n"
        "- `201 Created` — succesvol\n"
        "- `400 Bad Request` — validatiefout (met foutcode)\n"
        "- `429 Too Many Requests` — rate limit overschreden"
    ),
    "API Contract - POST /api/messages/xml": (
        "## Doel\n\n"
        "Submit een raw XML-bericht naar de pipeline (convenience endpoint).\n\n"
        "## Technische details\n\n"
        "- **File:** `MarketMessageController.java` (regels 46–59)\n"
        "- **Content-Type:** `application/xml`\n"
        "- **XXE-bescherming:** actief op XML-parser\n\n"
        "## HTTP-gedrag\n\n"
        "- `201 Created` — succesvol\n"
        "- `400 Bad Request` — parse-fout"
    ),
    "API Contract - GET /api/messages/{uuid}": (
        "## Doel\n\n"
        "Haal de volledige berichtstatus op inclusief alle 29 stapresultaten.\n\n"
        "## Technische details\n\n"
        "- **File:** `MarketMessageController.java` (regels 67–73)\n"
        "- **Response:** alle stappen met status, timestamps en evt. foutcodes\n\n"
        "## HTTP-gedrag\n\n"
        "- `200 OK` — bericht gevonden\n"
        "- `404 Not Found` — UUID onbekend\n"
        "- `429 Too Many Requests` — rate limit"
    ),
    "API Contract - GET /api/messages/status/{status}": (
        "## Doel\n\n"
        "Filter berichten op status.\n\n"
        "## Technische details\n\n"
        "- **File:** `MarketMessageController.java` (regels 87–95)\n"
        "- **Geldige waarden:** `RECEIVED`, `PROCESSING`, `COMPLETED`, `FAILED`, `PARKED`\n"
        "- **Pagination:** offset + limit parameters\n\n"
        "## HTTP-gedrag\n\n"
        "- `200 OK` — gefilterde lijst\n"
        "- `400 Bad Request` — onbekende status\n"
        "- `429 Too Many Requests` — rate limit"
    ),
    "API Contract - POST /api/messages/{uuid}/reprocess": (
        "## Doel\n\n"
        "Herverwerk een `FAILED` of `PARKED` bericht.\n\n"
        "## Technische details\n\n"
        "- **File:** `MarketMessageController.java` (regels 102–113)\n"
        "- **Toegestaan voor:** `FAILED` en `PARKED` berichten\n\n"
        "## Wat er gebeurt\n\n"
        "1. Oude stapresultaten worden verwijderd\n"
        "2. Status wordt gereset naar `RECEIVED`\n"
        "3. Pipeline wordt opnieuw geïnitialiseerd (1A → 6B)"
    ),
    "Reprocess Logic Implementation": (
        "## Doel\n\n"
        "Service-laag logica voor het herverwerken van berichten.\n\n"
        "## Technische details\n\n"
        "- **File:** `MarketMessageService.java` (regels 177–200)\n\n"
        "## Stappen\n\n"
        "1. Valideer dat status `FAILED` of `PARKED` is\n"
        "2. Verwijder alle bestaande stapresultaten\n"
        "3. Reset message-status naar `RECEIVED`\n"
        "4. Reinitializeer pipeline (stap 1A t/m 6B)\n"
        "5. Trigger `PipelineOrchestrator`\n\n"
        "**Transactioneel:** alles slaagt of alles wordt teruggedraaid."
    ),
    "Error Translation Layer": (
        "## Doel\n\n"
        "Centrale vertaling van Java-exceptions naar gestandaardiseerde JSON-foutresponses.\n\n"
        "## Technische details\n\n"
        "- **File:** `GlobalExceptionHandler.java` (regels 24–121)\n\n"
        "## Exception-mapping\n\n"
        "| Exception | HTTP | Details |\n"
        "|-----------|------|---------|\n"
        "| ValidationException | 400 | foutcode + message |\n"
        "| BusinessRuleException | 400 | foutcode + message |\n"
        "| PipelineException | 500 | generiek (geen interne details) |\n"
        "| RateLimitException | 429 | X-RateLimit headers |\n"
        "| Generic Exception | 500 | generiek |\n\n"
        "## Response-formaat\n\n"
        "`{ errorCode, message, timestamp (ISO-8601), path }`"
    ),
    "Security Headers Implementation": (
        "## Doel\n\n"
        "Beveiligingsheaders op elke HTTP-response via `SecurityConfig.java` (regels 29–105).\n\n"
        "## Headers\n\n"
        "- **Content-Security-Policy:** `default-src 'self'` — blokkeert externe resources\n"
        "- **X-Frame-Options:** `SAMEORIGIN` — voorkomt clickjacking\n"
        "- **HSTS:** `max-age=31536000` — forceert HTTPS gedurende 1 jaar\n"
        "- **Referrer-Policy:** `strict-origin-when-cross-origin`\n"
        "- **Permissions-Policy:** blokkeert camera, microfoon, geolocatie, betaling\n\n"
        "## CORS\n\n"
        "Alleen same-origin requests zijn toegestaan."
    ),
    "Rate Limiting per IP": (
        "## Doel\n\n"
        "Per-IP rate limiting via `RateLimitFilter.java` (regels 34–112).\n\n"
        "## Buckets\n\n"
        "| Endpoint | Limiet |\n"
        "|----------|--------|\n"
        "| POST /api/messages | 30 req / 60 sec |\n"
        "| POST /api/test/seed | 5 req / 60 sec |\n"
        "| Alle overige endpoints | 120 req / 60 sec |\n\n"
        "## Bij overschrijding\n\n"
        "- HTTP `429 Too Many Requests`\n"
        "- Header `X-RateLimit-Limit` — maximum\n"
        "- Header `X-RateLimit-Remaining` — resterend\n"
        "- Header `Retry-After` — seconden tot reset"
    ),
    "Input Validation & Sanitization": (
        "## Doel\n\n"
        "Gecentraliseerde inputvalidatie via `InputSanitizer.java` (regels 13–66).\n\n"
        "## Validaties\n\n"
        "- **UUID:** formaat `8-4-4-4-12` hex-digits\n"
        "- **EAN:** 13 of 18 cijfers (EAN-13 / EAN-18)\n"
        "- **Grootte:** maximaal 2 MB per bericht\n"
        "- **Sanitisatie:** verwijdert control-chars (behalve `\\t`, `\\n`, `\\r`)\n\n"
        "## Foutafhandeling\n\n"
        "Patroon-mismatch of overschrijding gooit een `ValidationException` met bijbehorende foutcode."
    ),
    "Test Data Generatie & Opschonen": (
        "## Doel\n\n"
        "Snelle setup van testberichten via `TestDataController.java`.\n\n"
        "## Endpoints\n\n"
        "- **POST `/api/test/seed`** — maakt representatieve testberichten aan (meerdere statussen)\n"
        "- **POST `/api/test/clear`** — ruimt alle testdata op voor een schone herstart\n\n"
        "## Gebruik\n\n"
        "1. Roep `/api/test/seed` aan om het dashboard direct te kunnen demonstreren\n"
        "2. Roep `/api/test/clear` aan na een testsessie\n\n"
        "**Rate limiting:** `POST /api/test/seed` is beperkt tot 5 req/min per IP."
    ),

    # ── Dashboard ─────────────────────────────────────────────────
    "Dashboard & Monitoring - Fase 4": (
        "## Doel\n\n"
        "Operationeel monitoring-dashboard voor de complete 29-staps pipeline.\n\n"
        "## Kernfunctionaliteit\n\n"
        "- **Pipeline progress** — live per bericht (stap 1A t/m 6B)\n"
        "- **Status-overzicht** — tellers per state: RECEIVED, PROCESSING, COMPLETED, FAILED, PARKED\n"
        "- **Foutcode-analyse** — top foutcodes, trend per tijdvenster, mapping naar validatiestap (3A–3G)\n"
        "- **Performance panel** — throughput, gemiddelde verwerkingstijd per stap, wachtrijdruk\n"
        "- **Rate-limit inzicht** — resterende calls en 429-incidenten per endpointgroep\n"
        "- **Tijdlijn** — recente berichten met correlation-id, type en einduitkomst\n\n"
        "## Databronnen\n\n"
        "- `GET /api/messages` — totaaloverzicht\n"
        "- `GET /api/messages/status/{status}` — gefilterde views\n"
        "- `POST /api/test/seed` — demo-data om het dashboard direct te vullen\n\n"
        "## Niet-functioneel\n\n"
        "Publiek toegankelijk in demo-context (geen authenticatie vereist)."
    ),

    # ── Foutcodes referentie ──────────────────────────────────────
    "Officiële foutcodes (Business-Service v4.0)": (
        "## Bron\n\n"
        "Business-Service-Uitwisselen-allocatiegegevens-elektriciteit-v4.0.pdf (p. 17–19)\n\n"
        "## Alle 32 foutcodes\n\n"
        "| Code | Omschrijving |\n"
        "|------|--------------|\n"
        "| 650 | EAN-18 aansluiting niet valide |\n"
        "| 651 | EAN-18 netgebied/allocatiepunt niet valide |\n"
        "| 663 | Periode onjuist |\n"
        "| 667 | Product past niet bij productsoort |\n"
        "| 668 | Energie-eenheid past niet bij product |\n"
        "| 669 | MessageID niet uniek |\n"
        "| 670 | Kenmerk bericht niet uniek |\n"
        "| 671 | Aantal posities onjuist |\n"
        "| 676 | Eerste positie begint niet met 1 |\n"
        "| 681 | ProcessTypeID past niet bij berichtinhoud |\n"
        "| 683 | Herkomst/validatie/reparatie combinatie ongeldig |\n"
        "| 686 | Negatief volume (behalve PRF-uitzondering) |\n"
        "| 701 | SenderID SOAP ≠ SenderID BDH |\n"
        "| 704 | Recentere versie al ontvangen |\n"
        "| 745 | ReceiverID SOAP ≠ ReceiverID BDH |\n"
        "| 747 | ProcessTypeID past niet bij ontvanger |\n"
        "| 754 | ContentType niet in lijn met ProcessTypeID |\n"
        "| 758 | Ontbrekende of onjuiste EAN-13 |\n"
        "| 759 | Ontbrekende EAN-13 van BRP |\n"
        "| 761 | BRP niet actief in netgebied |\n"
        "| 763 | Dagbericht te laat opgeleverd |\n"
        "| 764 | Aantal tijdseries past niet bij allocatiegroep |\n"
        "| 765 | BRP niet bekend/erkend |\n"
        "| 769 | Allocatierun identificatie niet uniek |\n"
        "| 771 | Vastgesteld afnametype past niet bij profielcategorie |\n"
        "| 772 | Gegevens (deels) in de toekomst |\n"
        "| 773 | Resolutie past niet bij afspraken |\n"
        "| 774 | Factor heeft onjuist aantal decimalen |\n"
        "| 776 | Volume heeft onjuist aantal decimalen |\n"
        "| 777 | Netgebied/allocatiepunt niet in landelijke administratie |\n"
        "| 779 | Aantal profielfractie-tijdseries onjuist |\n"
        "| 780 | CorrelationID BDH ≠ SOAP |\n"
        "| 781 | Status profielfracties past niet bij profielcategorie |\n"
        "| 782 | Increment van tijdserie is geen 1 |\n"
        "| 999 | Generieke fout / vrij tekstveld |"
    ),
    "Foutcodes per validatiestap (3A-3G)": (
        "## Mapping foutcodes → validatiestap\n\n"
        "| Stap | US | Foutcodes |\n"
        "|------|----|-----------|\n"
        "| 3A | US-12 | 761, 765, 777 |\n"
        "| 3B | US-13 | 650, 651, 667, 668, 681, 683, 686, 701, 745, 747, 754, 758, 759, 764, 771, 773, 779, 781, 999 |\n"
        "| 3C | US-14 | 999 |\n"
        "| 3E | US-16 | 663, 763, 772 |\n"
        "| 3F | US-17 | 671, 676, 782 |\n"
        "| 3G | US-18 | 651, 669, 670, 704, 769, 774, 776, 780, 999 |"
    ),
}


def main():
    api_key = os.getenv("TRELLO_KEY")
    token = os.getenv("TRELLO_TOKEN")
    if not api_key or not token:
        print("Fout: zet eerst TRELLO_KEY en TRELLO_TOKEN.", file=sys.stderr)
        return 1

    board_id = "69ba84bdddab5079d38276ee"
    client = TrelloClient(api_key, token)

    cards = client.get_board_cards(board_id)
    print(f"Gevonden {len(cards)} kaarten op het bord.\n")

    updated = 0
    skipped = 0
    for card in cards:
        name = card["name"]
        if name in DESCRIPTIONS:
            new_desc = DESCRIPTIONS[name]
            client.update_card_desc(card["id"], new_desc)
            print(f"  ✅  {name}")
            updated += 1
        else:
            skipped += 1

    print(f"\n✅ Klaar! {updated} kaarten bijgewerkt, {skipped} overgeslagen.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
