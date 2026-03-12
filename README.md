# Market Message Processor

Pipeline-gebaseerde verwerker voor marktberichten in de Nederlandse energiemarkt (allocatiegegevens).
Berichten doorlopen **29 stappen** in **6 fases**, strikt op volgorde (1A → 1B → ... → 6B).

Bevat een **live dashboard** op `http://localhost:8080` met statusoverzicht, pipeline-visualisatie en test-data seeder.

---

## Inhoudsopgave

1. [Vereisten](#vereisten)
2. [Snel starten (H2)](#snel-starten-h2)
3. [Database opzetten (PostgreSQL)](#database-opzetten-postgresql)
4. [Dashboard](#dashboard)
5. [Database schema](#database-schema)
6. [Seed data (automatisch)](#seed-data-automatisch)
7. [API Endpoints](#api-endpoints)
8. [Pipeline: alle 29 stappen](#pipeline-alle-29-stappen)
9. [Foutcodes](#foutcodes)
10. [Berichttypes](#berichttypes)
11. [Message statussen](#message-statussen)
12. [Beveiliging](#beveiliging)
13. [Testen van begin tot einde](#testen-van-begin-tot-einde)
14. [Unit tests draaien](#unit-tests-draaien)
15. [Postman collectie](#postman-collectie)
16. [Configuratie](#configuratie)
17. [Projectstructuur](#projectstructuur)
18. [Specificatiedocument](#specificatiedocument)

---

## Vereisten

| Tool         | Versie    | Opmerkingen                          |
|--------------|-----------|--------------------------------------|
| Java (JDK)   | 21+       | Getest met OpenJDK Temurin 25        |
| Maven        | 3.9+      | Wrapper aanwezig in project          |
| Docker       | 20+       | Alleen nodig voor PostgreSQL-profiel |

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

# 4. Open het dashboard op http://localhost:8080
```

> **Let op:** De H2 console is uitgeschakeld vanwege beveiliging. Gebruik het dashboard of de API endpoints om data te bekijken.

De database wordt opgeslagen als bestand: `data/market_messages.mv.db`

---

## Database opzetten (PostgreSQL)

### Stap 1: Start PostgreSQL via Docker

```powershell
docker compose up -d
```

Dit start een **PostgreSQL 16** container met:
- **Database:** `market_messages`
- **User:** `engie` / **Password:** `engie123`
- **Poort:** `5433` (host) → `5432` (container)
- **Volume:** `postgres-data` (data blijft bewaard)
- **Healthcheck:** automatisch via `pg_isready`

### Stap 2: Start de applicatie met PostgreSQL-profiel

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Of via environment variabele:
```powershell
$env:SPRING_PROFILES_ACTIVE="postgres"; mvn spring-boot:run
```

### Stap 3: Schema wordt automatisch aangemaakt

Hibernate `ddl-auto: update` maakt alle tabellen automatisch aan bij de eerste start. **Je hoeft geen SQL-migraties handmatig uit te voeren.**

### PostgreSQL stoppen

```powershell
docker compose down          # Stop container (data blijft)
docker compose down -v       # Stop container + verwijder data
```

### Database legen

```powershell
docker exec engie-postgres psql -U engie -d market_messages -c "TRUNCATE processing_steps, processing_logs, validation_results, market_responses, delivery_records, market_messages RESTART IDENTITY CASCADE;"
```

---

## Dashboard

De applicatie bevat een **volledig interactief dashboard** op `http://localhost:8080`.

### Functies

- **Statistieken:** Totaal berichten, ACK/NACK percentages, gemiddelde verwerkingstijd
- **Berichtenlijst:** Alle berichten met status, type, EAN-code en respons type
- **Filters:** Filter op status (COMPLETED, FAILED, PARKED, etc.)
- **Pipeline-visualisatie:** Klik op een bericht om alle 29 stappen te zien per fase
- **Test Data Seeder:** "Laad Testdata" knop om snel testberichten aan te maken (mix van ACK en NACK)
- **NACK-indicatie:** Stappen met warnings worden oranje gemarkeerd in de pipeline

### Techniek

Het dashboard is gebouwd met vanilla HTML, CSS en JavaScript (geen framework).

| Onderdeel | Bestanden |
|-----------|-----------|
| HTML      | `index.html` |
| CSS       | 10 modulaire bestanden (`base.css`, `components.css`, `detail-panel.css`, `errors.css`, `filters.css`, `header.css`, `messages.css`, `pipeline.css`, `stats.css`, `variables.css`) |
| JavaScript | 4 bestanden (`api.js` voor API calls, `app.js` voor initialisatie, `render.js` voor DOM rendering, `utils.js` voor hulpfuncties) |

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
| `xml_content`      | TEXT          | nee      | Originele XML inhoud                 |
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
| `xml_response`     | TEXT          | nee      | Volledige XML respons                |
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

### Test data via dashboard of API

Via het dashboard (knop "Laad Testdata") of via de API:

```powershell
# Database wissen
Invoke-RestMethod -Uri http://localhost:8080/api/test/clear -Method POST

# Testdata laden (20 berichten)
Invoke-RestMethod -Uri http://localhost:8080/api/test/seed -Method POST
```

Dit maakt testberichten aan met diverse scenario's (ACK en NACK). Zie de sectie [Test Data Seeder](#7-test-data-seeder-apitestseed) voor details.

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
    "xmlContent": "<?xml version=\"1.0\"?><AllocationSeries>...</AllocationSeries>",
    "manualEntry": false,
    "eanCode": "871686700000000001"
}
```

| Veld          | Type    | Verplicht | Validatie                                  |
|---------------|---------|-----------|-------------------------------------------|
| `xmlContent`  | string  | ja        | Niet leeg, max 2 MB                       |
| `manualEntry` | boolean | nee       | `true` = handmatig opgevoerd              |
| `eanCode`     | string  | nee       | 13 of 18 cijfers (EAN-13/EAN-18 formaat)  |

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

**Request body:** Het ruwe XML bericht als plain text (max 2 MB).

**Response (202 Accepted):** Zelfde formaat als hierboven.

---

### 3. Berichtstatus ophalen (met alle pipeline stappen)

```
GET /api/messages/{uuid}
```

> UUID moet geldig UUID-formaat zijn (bijv. `550e8400-e29b-41d4-a716-446655440000`).

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

### 7. Test Data Seeder (`/api/test`)

```
POST /api/test/seed    — 20 testberichten laden
POST /api/test/clear   — alle data wissen (TRUNCATE CASCADE)
```

> Alleen beschikbaar in niet-productie profielen (`default`, `dev`, `test`, `postgres`).
> Rate limited: max 5 requests per minuut per IP.

Maakt de volgende testberichten aan:

| # | Scenario                                    | EAN-code             | Verwacht resultaat        |
|---|---------------------------------------------|----------------------|---------------------------|
| 1 | Geldig allocatiebericht (Engie)             | `871686700000000001` | ACK                       |
| 2 | Geldig allocatiebericht (Vattenfall)        | `871686700000000002` | ACK                       |
| 3 | Geldig bericht, handmatige opvoer (Essent)  | `871686700000000003` | ACK                       |
| 4 | Geaggregeerd allocatiebericht met PRF       | `871686700000000001` | ACK                       |
| 5 | RCF / Profielfracties bericht               | `871686700000000002` | ACK                       |
| 6 | Negatief volume (PRF groep, toegestaan)     | `871686700000000001` | ACK                       |
| 7 | Groot bericht met 96 posities (volledig dag)| `871686700000000001` | ACK                       |
| 8 | Meerdere tijdseries in één bericht          | `871686700000000003` | ACK                       |
| 9 | Ongeldig productcode                        | `871686700000000001` | NACK (validatiefout)      |
| 10| Geen EAN-code meegegeven                    | *(geen)*             | NACK (BRP onbekend)       |
| 11| Ongeldige XML (niet parseerbaar)            | `871686700000000001` | NACK (technisch)          |
| 12| Onbekende BRP EAN                           | `999999999999999999` | NACK (765: BRP niet gevonden) |
| 13| Foutieve resolutie (PT1H i.p.v. PT15M)     | `871686700000000001` | NACK (773: resolutie)     |
| 14| Periode fout (eind voor start)              | `871686700000000001` | NACK (663: periode onjuist) |
| 15| Ongeldige UUID in mRID                      | `871686700000000001` | NACK (669: ongeldig mRID) |
| 16| Negatief volume (niet-PRF, niet toegestaan) | `871686700000000001` | NACK (686: negatief volume) |
| 17| Volgorde posities fout (begint bij 2)       | `871686700000000001` | NACK (676/782: volgorde)  |
| 18| Lege XML content                            | `871686700000000001` | FAILED (stap 1A)          |
| 19| Toekomstdata (ver in toekomst)              | `871686700000000001` | NACK (772: toekomst)      |
| 20| Dubbele EAN-codes in bericht                | `871686700000000004` | ACK (Liander, DDM rol)    |

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

Alle foutcodes komen uit de officiële specificatie: **Business-Service-Uitwisselen-allocatiegegevens-elektriciteit-v4.0.pdf** (pagina's 17-19).

### Officiële foutcodes – Tijdserie-niveau

| Code | Foutmelding                                        | Controle                                               |
|------|----------------------------------------------------|--------------------------------------------------------|
| 651  | Ongeldige EAN-codelijst                            | Controle of EAN-18 code geldig is                      |
| 663  | Periode niet juist                                 | Controle of einddatum na startdatum ligt               |
| 669  | MessageID niet uniek / ongeldig formaat            | Controle of mRID een geldig UUID is                    |
| 676  | Eerste positie is niet '1'                         | Controle of eerste tijdserie-positie = 1               |
| 686  | Volume negatief (niet toegestaan)                  | Controle op negatieve volumes (excl. PRF)              |
| 758  | Ontbrekende/ongeldige EAN-13 code meetpunt         | Controle of EAN-13 meetpunt geldig is                  |
| 763  | Oplevering te laat (dagrapport)                    | Controle of bericht binnen geldige leverdatum valt     |
| 764  | Tijdserie past niet bij allocatiegroep             | Controle of allocatiegroep overeenkomt                 |
| 765  | BRP/leverancier niet bekend/erkend                 | Controle of BRP in register staat                      |
| 772  | Data heeft betrekking op de toekomst               | Controle of data niet in de toekomst ligt              |
| 773  | Resolutie past niet bij productsoort               | Controle of resolutie klopt (PT15M bij elektriciteit)  |
| 776  | Volume onjuist aantal decimalen                    | Controle of volumes exact 3 decimalen hebben           |
| 782  | Increment is niet '1'                              | Controle of position-increment altijd 1 is             |
| 999  | Overige fout / generieke validatiefout             | Diverse controles die niet onder specifiek code vallen |

### Waar worden foutcodes gegenereerd?

| Stap | Bestand                            | Foutcodes die gegenereerd worden                                       |
|------|------------------------------------|------------------------------------------------------------------------|
| 3A   | Step3aBrpRegister.java             | 765 (BRP niet bekend)                                                  |
| 3B   | Step3bMarktBusinessValidaties.java | 999 (productsoort), 764 (allocatiegroep), 773 (resolutie), 758 (EAN-13), 686 (negatief volume) |
| 3C   | Step3cControleVerplicht.java       | 999 (verplicht veld ontbreekt)                                         |
| 3E   | Step3eTijdvenster.java             | 663 (periode onjuist), 772 (toekomst), 763 (te laat)                   |
| 3F   | Step3fVolgordelijkheid.java        | 676 (eerste positie ≠ 1), 782 (increment ≠ 1)                         |
| 3G   | Step3gHerbruikbareRegels.java      | 669 (MessageID), 999 (datetime formaat), 776 (decimalen), 651 (EAN-18) |

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

## Beveiliging

De applicatie bevat uitgebreide beveiligingsmaatregelen voor productiegericht gebruik.

### Security headers

| Header                    | Waarde                                         |
|---------------------------|-------------------------------------------------|
| Content-Security-Policy   | `default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:` |
| X-Frame-Options           | `DENY` (voorkomt clickjacking)                  |
| X-Content-Type-Options    | `nosniff`                                       |
| Strict-Transport-Security | `max-age=31536000; includeSubDomains` (HSTS)    |
| Referrer-Policy           | `strict-origin-when-cross-origin`               |
| Permissions-Policy        | `camera=(), microphone=(), geolocation=(), payment=()` |

### Input validatie

- **XML content:** Verplicht, max 2 MB
- **EAN-code:** Optioneel, moet 13 of 18 cijfers zijn (EAN-13/EAN-18 formaat)
- **UUID parameters:** Worden gevalideerd op geldig UUID-formaat
- **XXE bescherming:** XML parsing met `disallow-doctype-decl` in alle 4 de XML-parsers

### Rate limiting (per IP-adres)

| Endpoint             | Limiet       |
|----------------------|--------------|
| `POST /api/messages` | 30 per minuut |
| `POST /api/test/*`   | 5 per minuut  |
| `GET /api/*`         | 120 per minuut |

Overschrijding geeft een `429 Too Many Requests` respons met `X-RateLimit-Remaining` header.

### CORS

- Alleen `GET` en `POST` methoden toegestaan
- Alleen `Content-Type` en `Accept` headers toegestaan
- Geconfigureerd voor `/api/**` endpoints

### Overige maatregelen

- **Geen interne foutdetails:** Stack traces en exception messages worden nooit aan clients getoond
- **H2 console uitgeschakeld:** Voorkomt directe database-toegang via browser
- **Test endpoints afgeschermd:** `/api/test/seed` en `/api/test/clear` zijn alleen beschikbaar in niet-productie profielen
- **Request size limiet:** Maximum 2 MB op servlet- en Tomcat-niveau
- **Logging:** Productieniveau (INFO), security events op WARN

---

## Testen van begin tot einde

### Test 1: Geldig bericht → ACK (alle 29 stappen doorlopen)

**1. Stuur een geldig bericht in:**

```powershell
$body = @{
    xmlContent = '<?xml version="1.0" encoding="UTF-8"?><AllocationSeries><mRID>550e8400-e29b-41d4-a716-446655440000</mRID><sender_MarketParticipant.mRID codingScheme="A10">871686700000000001</sender_MarketParticipant.mRID><receiver_MarketParticipant.mRID codingScheme="A10">8716867000013</receiver_MarketParticipant.mRID><product><identification>023</identification></product><startDateTime>2025-01-01T00:00:00Z</startDateTime><endDateTime>2025-01-02T00:00:00Z</endDateTime><resolution>PT15M</resolution><position>1</position><quantity>150.000</quantity><position>2</position><quantity>200.000</quantity></AllocationSeries>'
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

### Test 2: Onbekende EAN → NACK met foutcode 765

```powershell
$body = @{
    xmlContent = '<?xml version="1.0" encoding="UTF-8"?><AllocationSeries><mRID>550e8400-e29b-41d4-a716-446655440001</mRID><sender_MarketParticipant.mRID codingScheme="A10">999999999999999999</sender_MarketParticipant.mRID><product><identification>023</identification></product><startDateTime>2025-01-01T00:00:00Z</startDateTime><endDateTime>2025-01-02T00:00:00Z</endDateTime><resolution>PT15M</resolution><position>1</position><quantity>100.000</quantity></AllocationSeries>'
    manualEntry = $false
    eanCode = "999999999999999999"
} | ConvertTo-Json

Invoke-RestMethod -Uri http://localhost:8080/api/messages -Method POST -ContentType "application/json" -Body $body
```

**Verwacht resultaat:**
- `responseType`: `NACK`
- Foutcode `765` in de respons (BRP/leverancier niet bekend/erkend)

---

### Test 3: Test data seeden (dashboard of API)

```powershell
# Database wissen
Invoke-RestMethod -Uri http://localhost:8080/api/test/clear -Method POST

# Via API: 20 testberichten laden
Invoke-RestMethod -Uri http://localhost:8080/api/test/seed -Method POST

# Bekijk alle berichten
Invoke-RestMethod -Uri http://localhost:8080/api/messages -Method GET | ConvertTo-Json -Depth 3
```

Of open het dashboard op `http://localhost:8080` en klik op **"Laad Testdata"**.

---

### Test 4: Bericht opnieuw verwerken

```powershell
# Alleen voor berichten met status FAILED of PARKED:
Invoke-RestMethod -Uri http://localhost:8080/api/messages/{uuid}/reprocess -Method POST
```

---

### Test 5: Filteren op status

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/messages/status/COMPLETED -Method GET | ConvertTo-Json
```

---

### Test 6: Database inspecteren (PostgreSQL)

```powershell
# Alle berichten
docker exec engie-postgres psql -U engie -d market_messages -c "SELECT message_uuid, message_type, status, ean_code FROM market_messages;"

# Stappen van een bericht
docker exec engie-postgres psql -U engie -d market_messages -c "SELECT step_code, step_name, status FROM processing_steps WHERE message_id = 1 ORDER BY step_order;"

# ACK/NACK responses
docker exec engie-postgres psql -U engie -d market_messages -c "SELECT response_type, error_codes FROM market_responses;"

# BRP register
docker exec engie-postgres psql -U engie -d market_messages -c "SELECT ean_code, party_name, market_role FROM brp_register;"
```

---

## Unit tests draaien

```powershell
mvn test
```

**142 tests** verdeeld over 9 test classes:

| Test class                    | Tests | Dekking                              |
|-------------------------------|-------|--------------------------------------|
| Phase1StepTests               | 21    | Stappen 1A-1F                        |
| Phase2StepTests               | 18    | Stappen 2A-2E                        |
| Phase3StepTests               | 28    | Stappen 3A-3G (validatie)            |
| Phase4StepTests               | 17    | Stappen 4A-4E (ACK/NACK)            |
| Phase5StepTests               | 11    | Stappen 5A-5D (versturen)           |
| Phase6StepTests               | 8     | Stappen 6A-6B (aflevering)          |
| PipelineOrchestratorTest      | 13    | Registratie, uitvoering, foutafhandeling |
| MarketMessageServiceTest      | 14    | Service laag                         |
| MarketMessageControllerTest   | 12    | REST endpoints (MockMvc) + beveiligingstests |

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
| `spring.h2.console.enabled`       | `false`   | H2 web console (uitgeschakeld voor beveiliging) |
| `spring.servlet.multipart.max-file-size` | `2MB` | Maximum upload grootte                     |
| `server.error.include-message`    | `never`   | Verberg interne foutmeldingen                   |
| `server.error.include-stacktrace` | `never`   | Verberg stack traces                            |

---

## Projectstructuur

```
src/main/java/nl/engie/allocation/
├── config/
│   ├── DataInitializer.java          # Seed data bij eerste start (BRP register + validatieregels)
│   ├── InputSanitizer.java           # Input validatie utilities (UUID, EAN, size checks)
│   ├── RateLimitFilter.java          # Rate limiting per IP-adres
│   └── SecurityConfig.java           # Spring Security configuratie (headers, CORS)
├── controller/
│   ├── MarketMessageController.java  # REST API endpoints
│   └── TestDataController.java       # Test data seeder (alleen niet-productie)
├── dto/
│   ├── MessageSubmitRequest.java     # Input DTO met validatie-annotaties
│   ├── MessageStatusResponse.java    # Output DTO (record)
│   ├── StepStatusDto.java           # Stap detail DTO (record)
│   └── ValidationErrorDto.java       # Validatie fout DTO
├── exception/
│   ├── GlobalExceptionHandler.java   # Centrale foutafhandeling (geen interne details lekken)
│   └── PipelineException.java        # Custom exception voor pipeline fouten
├── model/
│   ├── entity/                       # JPA entities (8 tabellen)
│   │   ├── BrpRegisterEntry.java
│   │   ├── DeliveryRecord.java
│   │   ├── MarketMessage.java
│   │   ├── MarketResponse.java
│   │   ├── ProcessingLog.java
│   │   ├── ProcessingStep.java
│   │   ├── ValidationResult.java
│   │   └── ValidationRule.java
│   └── enums/                        # Enumeraties
│       ├── ErrorCode.java
│       ├── MessageStatus.java
│       ├── MessageType.java
│       ├── ResponseType.java
│       ├── StepCode.java
│       └── StepStatus.java
├── pipeline/
│   ├── PipelineContext.java          # Context object door pipeline
│   ├── PipelineOrchestrator.java     # Voert 29 stappen uit op volgorde
│   ├── PipelineStep.java            # Interface voor een stap
│   ├── StepResult.java              # Resultaat van een stap
│   └── step/                        # 28 individuele stap classes
│       ├── Step1aOntvangBericht.java ... Step6bAfleverstatus.java
├── repository/                       # Spring Data JPA repositories (8)
└── service/
    └── MarketMessageService.java     # Business logic

src/main/resources/
├── application.yml                   # Configuratie (H2 default + PostgreSQL profiel)
└── static/                           # Dashboard frontend
    ├── index.html
    ├── css/                          # 10 modulaire CSS bestanden
    └── js/                           # 4 JavaScript bestanden (api, app, render, utils)

src/test/java/nl/engie/allocation/
├── controller/
│   └── MarketMessageControllerTest.java  # 12 tests (REST + beveiliging)
├── service/
│   └── MarketMessageServiceTest.java     # 14 tests
├── pipeline/
│   ├── PipelineOrchestratorTest.java     # 13 tests
│   └── step/
│       ├── Phase1StepTests.java          # 21 tests
│       ├── Phase2StepTests.java          # 18 tests
│       ├── Phase3StepTests.java          # 28 tests
│       ├── Phase4StepTests.java          # 17 tests
│       ├── Phase5StepTests.java          # 11 tests
│       └── Phase6StepTests.java          # 8 tests
```

---

## Specificatiedocument

Het volledige specificatiedocument met alle foutcodes, berichtdefinities, procesregels en validatievoorschriften is opgenomen in het project:


Dit document is de bron voor:
- Alle foutcodes en hun betekenis
- De volledige pipeline-stappen en hun functie
- Validatieregels per berichttype
- Berichtformaten en veldspecificaties
- Procesafspraken tussen marktpartijen
