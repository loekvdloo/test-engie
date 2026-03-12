# Market Message Processor

Pipeline-gebaseerde verwerker voor marktberichten in de Nederlandse energiemarkt (allocatiegegevens).
Berichten doorlopen **29 stappen** in **6 fases**, strikt op volgorde (1A → 1B → ... → 6B).

---

## Inhoudsopgave

1. [Vereisten](#vereisten)
2. [Snel starten (H2)](#snel-starten-h2)
3. [Database opzetten (MySQL)](#database-opzetten-mysql)
4. [Database schema](#database-schema)
5. [Seed data (automatisch)](#seed-data-automatisch)
6. [API Endpoints](#api-endpoints)
7. [Pipeline: alle 29 stappen](#pipeline-alle-29-stappen)
8. [Foutcodes](#foutcodes)
9. [Berichttypes](#berichttypes)
10. [Message statussen](#message-statussen)
11. [Testen van begin tot einde](#testen-van-begin-tot-einde)
12. [Unit tests draaien](#unit-tests-draaien)
13. [Postman collectie](#postman-collectie)
14. [Configuratie](#configuratie)
15. [Projectstructuur](#projectstructuur)

---

## Vereisten

| Tool         | Versie    | Opmerkingen                          |
|--------------|-----------|--------------------------------------|
| Java (JDK)   | 21+       | Getest met OpenJDK Temurin 25        |
| Maven        | 3.9+      | Wrapper aanwezig in project          |
| Docker       | 20+       | Alleen nodig voor MySQL-profiel      |

---

## Snel starten (H2)

De standaard configuratie gebruikt een **embedded H2 database** — geen extra setup nodig.

```powershell
# 1. Clone/open het project
cd C:\Users\loek\test-engie

# 2. Compileer
mvn compile

# 3. Start de applicatie
mvn spring-boot:run

# 4. De API draait nu op http://localhost:8080
# 5. H2 Console: http://localhost:8080/h2-console
```

**H2 Console inloggen:**

| Veld      | Waarde                                        |
|-----------|-----------------------------------------------|
| JDBC URL  | `jdbc:h2:file:./data/market_messages`         |
| Username  | `sa`                                          |
| Password  | *(leeg laten)*                                |

De database wordt opgeslagen als bestand: `data/market_messages.mv.db`

---

## Database opzetten (MySQL)

### Stap 1: Start MySQL via Docker

```powershell
docker compose up -d
```

Dit start een MySQL 8.0 container met:
- **Database:** `market_messages`
- **User:** `engie` / **Password:** `engie123`
- **Root password:** `root`
- **Poort:** `3306`

### Stap 2: Start de applicatie met MySQL-profiel

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Of via environment variabele:
```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"; mvn spring-boot:run
```

### Stap 3: Schema wordt automatisch aangemaakt

Hibernate `ddl-auto: update` maakt alle tabellen automatisch aan bij de eerste start. **Je hoeft geen SQL-migraties handmatig uit te voeren.**

### MySQL stoppen

```powershell
docker compose down          # Stop container (data blijft)
docker compose down -v       # Stop container + verwijder data
```

---

## Database schema

### Tabellen overzicht

```
market_messages (1) ──< (N) processing_steps      FK: message_id
market_messages (1) ──< (N) processing_logs        FK: message_id
market_messages (1) ──< (N) validation_results     FK: message_id
market_messages (1) ──  (1) market_responses       FK: message_id
market_messages (1) ──  (1) delivery_records       FK: message_id
validation_rules (1) ──< (N) validation_results    FK: rule_id (nullable)
brp_register                                       standalone
```

### `market_messages` — Hoofdtabel voor alle berichten

| Kolom              | Type          | Nullable | Omschrijving                         |
|--------------------|---------------|----------|--------------------------------------|
| `id`               | BIGINT PK     | nee      | Auto-increment ID                    |
| `message_uuid`     | VARCHAR(36)   | nee      | Uniek UUID (unique constraint)       |
| `message_type`     | VARCHAR(50)   | ja       | Enum: berichttype                    |
| `xml_content`      | MEDIUMTEXT    | nee      | Originele XML inhoud                 |
| `status`           | VARCHAR(20)   | nee      | Huidige verwerkingsstatus            |
| `priority`         | INT           | ja       | Prioriteit (1=hoog, 5=normaal)       |
| `current_step`     | VARCHAR(10)   | ja       | Huidige pipeline stap                |
| `ean_code`         | VARCHAR(18)   | ja       | EAN-code afzender                    |
| `product_type`     | VARCHAR(10)   | ja       | Productsoort (bijv. 023=elektra)     |
| `allocation_group` | VARCHAR(10)   | ja       | Allocatiegroep (PRF/TMT/SMA/etc.)    |
| `allocation_run_id`| VARCHAR(36)   | ja       | Run ID                               |
| `start_date_time`  | TIMESTAMP     | ja       | Start datum/tijd allocatieperiode    |
| `end_date_time`    | TIMESTAMP     | ja       | Eind datum/tijd allocatieperiode     |
| `received_at`      | TIMESTAMP     | nee      | Tijdstip van ontvangst               |
| `completed_at`     | TIMESTAMP     | ja       | Tijdstip van afronding               |
| `is_manual_entry`  | BOOLEAN       | ja       | Handmatig opgevoerd?                 |
| `created_at`       | TIMESTAMP     | nee      | Aanmaak timestamp                    |
| `updated_at`       | TIMESTAMP     | nee      | Laatste wijziging                    |

### `processing_steps` — Status per pipeline stap

| Kolom              | Type          | Nullable | Omschrijving                         |
|--------------------|---------------|----------|--------------------------------------|
| `id`               | BIGINT PK     | nee      | Auto-increment ID                    |
| `message_id`       | BIGINT FK     | nee      | → market_messages.id                 |
| `step_code`        | VARCHAR(10)   | nee      | Enum: STEP_1A t/m STEP_6B           |
| `step_name`        | VARCHAR(100)  | nee      | Naam van de stap                     |
| `phase_name`       | VARCHAR(50)   | nee      | Naam van de fase                     |
| `step_order`       | INT           | nee      | Volgnummer (1–29)                    |
| `status`           | VARCHAR(20)   | nee      | PENDING/IN_PROGRESS/COMPLETED/FAILED/SKIPPED |
| `started_at`       | TIMESTAMP     | ja       | Start tijdstip                       |
| `completed_at`     | TIMESTAMP     | ja       | Eind tijdstip                        |
| `result_message`   | TEXT          | ja       | Resultaat melding                    |
| `error_message`    | TEXT          | ja       | Foutmelding                          |

### `processing_logs` — Gedetailleerde logging per stap

| Kolom              | Type          | Nullable | Omschrijving                         |
|--------------------|---------------|----------|--------------------------------------|
| `id`               | BIGINT PK     | nee      | Auto-increment ID                    |
| `message_id`       | BIGINT FK     | nee      | → market_messages.id                 |
| `step_code`        | VARCHAR(10)   | nee      | Stap code                            |
| `log_level`        | VARCHAR(10)   | nee      | INFO / ERROR / WARN                  |
| `message`          | TEXT          | nee      | Log bericht                          |
| `logged_at`        | TIMESTAMP     | nee      | Tijdstip                             |

### `validation_rules` — Configureerbare validatieregels

| Kolom              | Type          | Nullable | Omschrijving                         |
|--------------------|---------------|----------|--------------------------------------|
| `id`               | BIGINT PK     | nee      | Auto-increment ID                    |
| `rule_code`        | VARCHAR(50)   | nee      | Unieke regelcode (unique)            |
| `rule_name`        | VARCHAR(100)  | nee      | Naam van de regel                    |
| `rule_description` | TEXT          | ja       | Beschrijving                         |
| `message_type`     | VARCHAR(50)   | ja       | Voor welk berichttype (null = alle)  |
| `rule_expression`  | TEXT          | nee      | Expressie: `CONTAINS:x`, `NOT_CONTAINS:x`, `REGEX:x` |
| `error_code`       | VARCHAR(50)   | nee      | Foutcode bij falen                   |
| `error_message`    | VARCHAR(255)  | nee      | Foutmelding bij falen                |
| `is_active`        | BOOLEAN       | nee      | Regel actief? (default: true)        |
| `created_at`       | TIMESTAMP     | nee      | Aanmaak timestamp                    |

### `validation_results` — Resultaten per uitgevoerde validatie

| Kolom              | Type          | Nullable | Omschrijving                         |
|--------------------|---------------|----------|--------------------------------------|
| `id`               | BIGINT PK     | nee      | Auto-increment ID                    |
| `message_id`       | BIGINT FK     | nee      | → market_messages.id                 |
| `rule_id`          | BIGINT FK     | ja       | → validation_rules.id               |
| `rule_code`        | VARCHAR(50)   | nee      | Code van de regel                    |
| `is_valid`         | BOOLEAN       | nee      | Geslaagd?                            |
| `error_code`       | VARCHAR(50)   | ja       | Foutcode indien ongeldig             |
| `error_message`    | TEXT          | ja       | Foutmelding indien ongeldig          |
| `validated_at`     | TIMESTAMP     | nee      | Tijdstip validatie                   |

### `market_responses` — ACK/NACK respons berichten

| Kolom              | Type          | Nullable | Omschrijving                         |
|--------------------|---------------|----------|--------------------------------------|
| `id`               | BIGINT PK     | nee      | Auto-increment ID                    |
| `message_id`       | BIGINT FK     | nee      | → market_messages.id (one-to-one)    |
| `response_uuid`    | VARCHAR(36)   | nee      | UUID van respons (unique)            |
| `response_type`    | VARCHAR(10)   | nee      | ACK of NACK                          |
| `error_codes`      | TEXT          | ja       | Komma-gescheiden foutcodes           |
| `error_messages`   | TEXT          | ja       | Foutmeldingen                        |
| `xml_response`     | MEDIUMTEXT    | nee      | Volledige XML respons                |
| `sent_at`          | TIMESTAMP     | ja       | Verstuurd op                         |
| `created_at`       | TIMESTAMP     | nee      | Aangemaakt op                        |

### `delivery_records` — Afleverregistratie raw-layer

| Kolom              | Type          | Nullable | Omschrijving                         |
|--------------------|---------------|----------|--------------------------------------|
| `id`               | BIGINT PK     | nee      | Auto-increment ID                    |
| `message_id`       | BIGINT FK     | nee      | → market_messages.id (one-to-one)    |
| `delivery_target`  | VARCHAR(50)   | nee      | Bestemming (default: RAW_LAYER)      |
| `delivery_status`  | VARCHAR(20)   | nee      | PENDING / DELIVERED / NACK_SENT / NOT_APPLICABLE |
| `raw_layer_path`   | VARCHAR(255)  | ja       | Pad naar raw-layer bestand           |
| `delivered_at`     | TIMESTAMP     | ja       | Afgeleverd op                        |
| `created_at`       | TIMESTAMP     | nee      | Aangemaakt op                        |

### `brp_register` — BRP (Programmaverantwoordelijke) register

| Kolom              | Type          | Nullable | Omschrijving                         |
|--------------------|---------------|----------|--------------------------------------|
| `id`               | BIGINT PK     | nee      | Auto-increment ID                    |
| `ean_code`         | VARCHAR(18)   | nee      | EAN-code marktpartij (unique)        |
| `party_name`       | VARCHAR(100)  | nee      | Naam partij                          |
| `market_role`      | VARCHAR(10)   | nee      | Rol: DDK, DDQ, DDM                   |
| `is_active`        | BOOLEAN       | nee      | Actief? (default: true)              |
| `valid_from`       | DATE          | nee      | Geldig vanaf                         |
| `valid_to`         | DATE          | ja       | Geldig tot                           |
| `created_at`       | TIMESTAMP     | nee      | Aangemaakt op                        |

---

## Seed data (automatisch)

Bij de eerste opstart worden automatisch testdata geladen via `DataInitializer`:

### BRP Register (5 entries)

| EAN-code             | Partij            | Rol  |
|----------------------|-------------------|------|
| `871686700000000001` | Engie Energie NL  | DDK  |
| `871686700000000002` | Vattenfall NL     | DDK  |
| `871686700000000003` | Essent BV         | DDQ  |
| `871686700000000004` | Liander NB        | DDM  |
| `871686700000000005` | Stedin NB         | DDM  |

### Validatieregels (5 regels)

| Code   | Naam                         | Expressie                                | Berichttype                    |
|--------|------------------------------|------------------------------------------|--------------------------------|
| GEN001 | XML root element aanwezig    | `CONTAINS:<?xml`                         | alle                           |
| GEN002 | Productsoort elektriciteit   | `CONTAINS:023`                           | ALLOCATION_SERIES              |
| GEN003 | Resolutie PT15M              | `CONTAINS:PT15M`                         | alle                           |
| AGG001 | Allocatiegroep aanwezig      | `REGEX:.*(?:PRF\|TMT\|SMA\|NVL\|DIM).*` | AGGREGATED_ALLOCATION_SERIES   |
| RCF001 | RCF datumversie aanwezig     | `CONTAINS:dateRCF_version`               | ALLOCATION_FACTOR_SERIES       |

---

## API Endpoints

Basis-URL: `http://localhost:8080`

### 1. Bericht insturen (JSON)

```
POST /api/messages
Content-Type: application/json
```

**Request body:**
```json
{
    "xmlContent": "<?xml version=\"1.0\"?><AllocationSeriesNotification>...</AllocationSeriesNotification>",
    "manualEntry": false,
    "eanCode": "871686700000000001"
}
```

| Veld          | Type    | Verplicht | Omschrijving                              |
|---------------|---------|-----------|-------------------------------------------|
| `xmlContent`  | string  | ja        | Het XML marktbericht                      |
| `manualEntry` | boolean | nee       | `true` = handmatig opgevoerd              |
| `eanCode`     | string  | nee       | EAN-code voor BRP register validatie      |

**Response (202 Accepted):**
```json
{
    "messageUuid": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACCEPTED",
    "message": "Bericht ontvangen en wordt verwerkt door de pipeline"
}
```

---

### 2. Bericht insturen (raw XML)

```
POST /api/messages/xml
Content-Type: application/xml
```

**Request body:** Het ruwe XML bericht als plain text.

**Response (202 Accepted):** Zelfde formaat als hierboven.

---

### 3. Berichtstatus ophalen (met alle pipeline stappen)

```
GET /api/messages/{uuid}
```

**Response (200 OK):**
```json
{
    "messageUuid": "550e8400-e29b-41d4-a716-446655440000",
    "messageType": "ALLOCATION_SERIES",
    "status": "COMPLETED",
    "currentStep": "STEP_6B",
    "receivedAt": "2026-03-12T08:40:07.153839",
    "completedAt": "2026-03-12T08:40:07.544320",
    "priority": 1,
    "responseType": "ACK",
    "responseXml": "<AcknowledgementDocument>...</AcknowledgementDocument>",
    "steps": [
        {
            "stepCode": "STEP_1A",
            "stepName": "Ontvangen marktbericht",
            "phaseName": "Ontvang marktbericht",
            "stepOrder": 1,
            "status": "COMPLETED",
            "startedAt": "2026-03-12T08:40:07.200000",
            "completedAt": "2026-03-12T08:40:07.210000",
            "resultMessage": "Bericht ontvangen (156 bytes)",
            "errorMessage": null
        }
    ]
}
```

---

### 4. Alle berichten ophalen (overzicht)

```
GET /api/messages
```

**Response (200 OK):** Array van berichten (zonder gedetailleerde stappen).

---

### 5. Berichten filteren op status

```
GET /api/messages/status/{status}
```

Geldige statussen: `RECEIVED`, `PROCESSING`, `COMPLETED`, `FAILED`, `PARKED`, `ACK_GENERATED`, `NACK_GENERATED`, `RESPONSE_SENT`, `DELIVERED`

**Voorbeeld:** `GET /api/messages/status/FAILED`

---

### 6. Bericht opnieuw verwerken

```
POST /api/messages/{uuid}/reprocess
```

> Alleen mogelijk voor berichten met status `FAILED` of `PARKED`.

**Response (200 OK):**
```json
{
    "messageUuid": "550e8400-...",
    "status": "REPROCESSING",
    "message": "Bericht wordt opnieuw verwerkt"
}
```

---

## Pipeline: alle 29 stappen

Het bericht doorloopt **strikt in volgorde** alle 29 stappen. Als een stap faalt voor fase 4, wordt de pipeline gehalted.

### Fase 1: Ontvang marktbericht

| Stap | Code    | Naam                              | Omschrijving                                      |
|------|---------|-----------------------------------|---------------------------------------------------|
| 1    | STEP_1A | Ontvangen marktbericht            | Controleert of XML content niet leeg is            |
| 2    | STEP_1B | Technische ontvangstbevestiging   | Genereert unieke receipt ID                        |
| 3    | STEP_1C | Technische validatie              | Parset XML, zet technicallyValid flag              |
| 4    | STEP_1D | Logging van ontvangsttijd         | Logt het tijdstip van ontvangst                    |
| 5    | STEP_1E | Identificatie berichttype         | Detecteert type o.b.v. XML root element            |
| 6    | STEP_1F | Handmatige opvoer berichten       | Markeert handmatig opgevoerde berichten             |

### Fase 2: Classificeer bericht

| Stap | Code    | Naam                              | Omschrijving                                      |
|------|---------|-----------------------------------|---------------------------------------------------|
| 7    | STEP_2A | Classificeer van berichttype      | Classificeert per MessageType                      |
| 8    | STEP_2B | Bepalen prioriteit per type       | Kent prioriteit toe (1-5)                          |
| 9    | STEP_2C | Plaatsen in wachtrij              | Zet queue naam op context                          |
| 10   | STEP_2D | Uitzondering parkeren             | Markeert technisch ongeldige berichten als PARKED  |
| 11   | STEP_2E | Uitval opnieuw verwerken          | Beheert retry teller                               |

### Fase 3: Valideer bericht

| Stap | Code    | Naam                              | Omschrijving                                      |
|------|---------|-----------------------------------|---------------------------------------------------|
| 12   | STEP_3A | Operational BRP register          | Valideert EAN-code tegen BRP register              |
| 13   | STEP_3B | Markt business validaties         | Controleert productsoort en allocatiegroep          |
| 14   | STEP_3C | Controle verplichte velden        | Checkt of mRID, product, datums en resolutie aanwezig zijn |
| 15   | STEP_3D | Configureerbare validatieregels   | Voert actieve regels uit DB uit (CONTAINS/REGEX)   |
| 16   | STEP_3E | Tijdvenster-validaties            | Controleert start < eind, niet te ver in toekomst  |
| 17   | STEP_3F | Controle op volgordelijkheid      | Valideert dat posities sequentieel zijn (1,2,3...) |
| 18   | STEP_3G | Herbruikbare validatieregels      | Valideert UUID-formaat en ISO 8601 datumformaat     |

### Fase 4: Bepaal uitkomst

| Stap | Code    | Naam                              | Omschrijving                                      |
|------|---------|-----------------------------------|---------------------------------------------------|
| 19   | STEP_4A | Genereren ACK bij succes          | Maakt ACK respons als er geen fouten zijn          |
| 20   | STEP_4B | Genereren NACK bij fouten         | Maakt NACK respons met foutcodes                   |
| 21   | STEP_4C | Toevoegen foutcodes bij NACK      | Valideert en logt de foutcodes                     |
| 22   | STEP_4D | Vastleggen validatieresultaat     | Slaat overall resultaat op in DB                   |
| 23   | STEP_4E | NACK configuratie                 | Bepaalt of NACK intern doorgestuurd wordt          |

### Fase 5: Verstuur marktrespons

| Stap | Code    | Naam                              | Omschrijving                                      |
|------|---------|-----------------------------------|---------------------------------------------------|
| 24   | STEP_5A | Versturen ACK/NACK               | Simuleert het versturen, zet sentAt                |
| 25   | STEP_5B | Geconfigureerd respons versturen  | Checkt of NACK doorgestuurd mag worden              |
| 26   | STEP_5C | Logging verzendtijd               | Logt het tijdstip van verzending                   |
| 27   | STEP_5D | Zelfstandig versturen             | Toekomstige functionaliteit (wordt overgeslagen)   |

### Fase 6: Lever bericht aan MDP

| Stap | Code    | Naam                              | Omschrijving                                      |
|------|---------|-----------------------------------|---------------------------------------------------|
| 28   | STEP_6A | Doorzetten naar raw-layer         | Slaat bericht op in raw-layer met pad              |
| 29   | STEP_6B | Vastleggen afleverstatus          | Registreert definitieve afleverstatus              |

---

## Foutcodes

### Validatiefouten (pipeline stap 3)

| Code   | Stap | Betekenis                                                |
|--------|------|----------------------------------------------------------|
| BRP001 | 3A   | EAN-code niet gevonden in BRP register                   |
| BIZ001 | 3B   | Ongeldige productsoort (moet 023 of 8716867000016 zijn)  |
| BIZ002 | 3B   | Allocatiegroep ontbreekt bij geaggregeerd bericht        |
| VLD001 | 3C   | Verplicht veld ontbreekt (product/startDateTime/endDateTime/resolution) |
| TVL001 | 3E   | Einddatum ligt voor startdatum                           |
| TVL002 | 3E   | Startdatum ligt meer dan 3 dagen in de toekomst          |
| TVL003 | 3E   | Bericht is ouder dan 30 dagen                            |
| VOL001 | 3F   | Posities zijn niet sequentieel (bijv. 1,3 i.p.v. 1,2)   |
| HBR001 | 3G   | mRID is geen geldig UUID-formaat                         |
| HBR002 | 3G   | Datum is niet in ISO 8601 formaat                        |

### Configureerbare foutcodes (uit `validation_rules` tabel)

| Code | Regel  | Betekenis                                                |
|------|--------|----------------------------------------------------------|
| 001  | GEN001 | XML header ontbreekt                                     |
| 002  | GEN002 | Productsoort moet elektriciteit (023) zijn               |
| 003  | GEN003 | Resolutie moet PT15M zijn                                |
| 010  | AGG001 | Geldige allocatiegroep (PRF/TMT/SMA/NVL/DIM) vereist    |
| 020  | RCF001 | Datumversie RCF is verplicht voor RCF-berichten          |

### Pipeline fouten

| Code             | Betekenis                                         |
|------------------|---------------------------------------------------|
| VALIDATION_FAILED| Overall validatie gefaald (opgeslagen in validation_results) |
| OVERALL          | Overall validatie geslaagd                         |

---

## Berichttypes

| Type                            | XML Root Element                       | Omschrijving                            | Prioriteit |
|---------------------------------|----------------------------------------|-----------------------------------------|------------|
| `ALLOCATION_SERIES`             | `AllocationSeriesNotification`         | Allocatiegegevens individueel punt      | 1          |
| `AGGREGATED_ALLOCATION_SERIES`  | `AggregatedAllocationSeriesNotification`| Geaggregeerde allocatiegegevens        | 2          |
| `ALLOCATION_FACTOR_SERIES`      | `AllocationFactorSeriesNotification`   | RCF en Profielfracties                  | 3          |
| `MANUAL_ENTRY`                  | `ManualEntry`                          | Handmatig opgevoerd bericht             | 5          |

---

## Message statussen

| Status           | Betekenis                                          |
|------------------|-----------------------------------------------------|
| `RECEIVED`       | Bericht ontvangen, wacht op verwerking              |
| `PROCESSING`     | Pipeline is bezig met verwerking                    |
| `CLASSIFIED`     | Bericht is geclassificeerd (fase 2)                 |
| `VALIDATED`      | Bericht is gevalideerd (fase 3)                     |
| `ACK_GENERATED`  | ACK respons is aangemaakt                           |
| `NACK_GENERATED` | NACK respons is aangemaakt (validatiefouten)        |
| `RESPONSE_SENT`  | ACK/NACK is verstuurd                               |
| `DELIVERED`      | Bericht is afgeleverd aan raw-layer                 |
| `COMPLETED`      | Pipeline volledig doorlopen (succes)                |
| `FAILED`         | Pipeline gefaald (fout voor fase 4)                 |
| `PARKED`         | Bericht geparkeerd (technisch ongeldig, retry later)|

---

## Testen van begin tot einde

### Test 1: Geldig bericht → ACK (alle 29 stappen doorlopen)

**1. Stuur een geldig bericht in:**

```powershell
$body = @{
    xmlContent = '<?xml version="1.0" encoding="UTF-8"?><AllocationSeriesNotification><mRID>550e8400-e29b-41d4-a716-446655440000</mRID><product><identification>8716867000016</identification><measureUnit>KWH</measureUnit></product><group_businessType>PRF</group_businessType><DateAndOrTime><startDateTime>2026-03-10T23:00:00Z</startDateTime><endDateTime>2026-03-11T23:00:00Z</endDateTime></DateAndOrTime><Detail_Series><resolution>PT15M</resolution><MarketEvaluationPoint><mRID>871686700000000001</mRID></MarketEvaluationPoint><MarketParticipant><mRID>871686700000000001</mRID><MarketRole><type>DDK</type></MarketRole></MarketParticipant><Product><identification>8716867000016</identification></Product><Point><position>1</position><quantity>123.456</quantity></Point><Point><position>2</position><quantity>234.567</quantity></Point></Detail_Series></AllocationSeriesNotification>'
    manualEntry = $false
    eanCode = "871686700000000001"
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/messages -Method POST -ContentType "application/json" -Body $body
```

**2. Bekijk het resultaat (vervang UUID):**

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/messages/{uuid} -Method GET | ConvertTo-Json -Depth 5
```

**Verwacht resultaat:**
- `status`: `COMPLETED`
- `responseType`: `ACK`
- Alle 29 stappen: `COMPLETED` of `SKIPPED`
- Geen `FAILED` stappen

---

### Test 2: Ongeldig bericht → NACK (validatiefouten)

**Stuur een bericht met lege XML:**

```powershell
$body = @{
    xmlContent = ""
    manualEntry = $false
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/messages -Method POST -ContentType "application/json" -Body $body
```

**Verwacht resultaat:**
- `status`: `COMPLETED` (pipeline loopt door tot fase 4)
- `responseType`: `NACK`
- Stap 1A: `FAILED` met foutmelding "XML content is leeg"
- Pipeline halted na 1A

---

### Test 3: Onbekende EAN → validatiefout in stap 3A

```powershell
$body = @{
    xmlContent = '<?xml version="1.0" encoding="UTF-8"?><AllocationSeriesNotification><mRID>550e8400-e29b-41d4-a716-446655440000</mRID><product><identification>8716867000016</identification></product><DateAndOrTime><startDateTime>2026-03-10T23:00:00Z</startDateTime><endDateTime>2026-03-11T23:00:00Z</endDateTime></DateAndOrTime><Detail_Series><resolution>PT15M</resolution><Point><position>1</position><quantity>100</quantity></Point></Detail_Series></AllocationSeriesNotification>'
    manualEntry = $false
    eanCode = "999999999999999999"
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/messages -Method POST -ContentType "application/json" -Body $body
```

**Verwacht resultaat:**
- `responseType`: `NACK`
- Foutcode `BRP001` in de respons

---

### Test 4: Bericht opnieuw verwerken

```powershell
# Alleen voor berichten met status FAILED of PARKED:
Invoke-RestMethod -Uri http://localhost:8080/api/messages/{uuid}/reprocess -Method POST
```

---

### Test 5: Alle berichten bekijken

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/messages -Method GET | ConvertTo-Json -Depth 3
```

### Test 6: Filteren op status

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/messages/status/COMPLETED -Method GET | ConvertTo-Json
```

### Test 7: Database inspecteren via H2 Console

1. Open http://localhost:8080/h2-console
2. JDBC URL: `jdbc:h2:file:./data/market_messages`
3. Username: `sa`, Password: *(leeg)*
4. Handige queries:

```sql
-- Alle berichten
SELECT * FROM MARKET_MESSAGES;

-- Alle stappen van een specifiek bericht
SELECT * FROM PROCESSING_STEPS WHERE MESSAGE_ID = 1 ORDER BY STEP_ORDER;

-- Validatiefouten
SELECT * FROM VALIDATION_RESULTS WHERE IS_VALID = FALSE;

-- ACK/NACK responses
SELECT * FROM MARKET_RESPONSES;

-- BRP register
SELECT * FROM BRP_REGISTER;

-- Actieve validatieregels
SELECT * FROM VALIDATION_RULES WHERE IS_ACTIVE = TRUE;

-- Delivery status
SELECT * FROM DELIVERY_RECORDS;
```

---

## Unit tests draaien

```powershell
mvn test
```

**140 tests** verdeeld over 9 test classes:

| Test class                    | Tests | Dekking                              |
|-------------------------------|-------|--------------------------------------|
| Phase1StepTests               | 13    | Stappen 1A-1F                        |
| Phase2StepTests               | 13    | Stappen 2A-2E                        |
| Phase3StepTests               | 19    | Stappen 3A-3G (validatie)            |
| Phase4StepTests               | 14    | Stappen 4A-4E (ACK/NACK)            |
| Phase5StepTests               | 8     | Stappen 5A-5D (versturen)           |
| Phase6StepTests               | 7     | Stappen 6A-6B (aflevering)          |
| PipelineOrchestratorTest      | 11    | Registratie, uitvoering, foutafhandeling |
| MarketMessageServiceTest      | 12    | Service laag                         |
| MarketMessageControllerTest   | 10    | REST endpoints (MockMvc)             |

---

## Postman collectie

Importeer `postman/Market_Message_Processor.postman_collection.json` in Postman.

Bevat 12 voorbeeldverzoeken in 3 mappen:

| Map                    | Requests                                           |
|------------------------|----------------------------------------------------|
| Valid Messages (ACK)   | AllocationSeries, Aggregated, met EAN, handmatig   |
| Invalid Messages (NACK)| Lege XML, ongeldige XML, onbekende EAN             |
| Status & Management    | Alle berichten, op UUID, op status, reprocess      |

---

## Configuratie

In `application.yml`:

| Property                          | Default   | Omschrijving                                   |
|-----------------------------------|-----------|-------------------------------------------------|
| `server.port`                     | `8080`    | Poort van de applicatie                         |
| `pipeline.forward-nack-internally`| `false`   | NACK berichten doorsturen naar raw-layer?       |
| `pipeline.async-enabled`          | `true`    | Asynchrone verwerking aan/uit                   |
| `spring.jpa.hibernate.ddl-auto`   | `update`  | Schema automatisch aanmaken/updaten             |
| `spring.h2.console.enabled`       | `true`    | H2 web console aan/uit                          |

---

## Projectstructuur

```
src/main/java/nl/engie/allocation/
├── config/
│   └── DataInitializer.java          # Seed data bij eerste start
├── controller/
│   └── MarketMessageController.java  # REST endpoints
├── dto/
│   ├── MessageSubmitRequest.java     # Input DTO
│   ├── MessageStatusResponse.java    # Output DTO (record)
│   └── StepStatusDto.java           # Stap detail DTO (record)
├── model/
│   ├── entity/                       # JPA entities (8 tabellen)
│   └── enums/                        # MessageType, MessageStatus, StepCode, etc.
├── pipeline/
│   ├── PipelineContext.java          # Context object door pipeline
│   ├── PipelineOrchestrator.java     # Voert 29 stappen uit op volgorde
│   ├── PipelineStep.java            # Interface voor een stap
│   ├── StepResult.java              # Resultaat van een stap
│   └── step/                        # 29 individuele stap classes
│       ├── Step1aOntvangBericht.java
│       ├── Step1bTechnischeOntvangst.java
│       ├── ...
│       └── Step6bAfleverstatus.java
├── repository/                       # Spring Data JPA repositories (8)
└── service/
    └── MarketMessageService.java     # Business logic
```
