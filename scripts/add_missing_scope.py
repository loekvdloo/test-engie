#!/usr/bin/env python3
"""Add missing scope and reference lists to existing Trello board."""
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
            request = urllib.request.Request(url, method=method)
        else:
            data = json.dumps(params).encode('utf-8')
            request = urllib.request.Request(url, data=data, method=method,
                                            headers={"Content-Type": "application/json"})
        
        with urllib.request.urlopen(request) as response:
            return json.loads(response.read().decode('utf-8'))
    
    def create_list(self, board_id: str, name: str) -> str:
        params = {"name": name, "idBoard": board_id}
        result = self._request("POST", "/lists", params)
        return result["id"]
    
    def create_card(self, list_id: str, name: str, description: str = "", label_ids: list = None) -> str:
        params = {
            "name": name,
            "idList": list_id,
            "desc": description,
            "idLabels": label_ids or []
        }
        result = self._request("POST", "/cards", params)
        return result["id"]
    
    def get_labels(self, board_id: str) -> dict:
        """Get all labels on board, keyed by name."""
        result = self._request("GET", f"/boards/{board_id}/labels", {})
        return {label["name"]: label["id"] for label in result}
    
    def add_checklist(self, card_id: str, name: str, items: list):
        params = {"name": name}
        checklist = self._request("POST", f"/cards/{card_id}/checklists", params)
        for item in items:
            self._request("POST", f"/checklists/{checklist['id']}/checkItems", {"name": item})

def main():
    api_key = os.getenv("TRELLO_KEY")
    token = os.getenv("TRELLO_TOKEN")
    
    if not api_key or not token:
        print("Fout: zet eerst TRELLO_KEY en TRELLO_TOKEN in je environment variables.", file=sys.stderr)
        return 1
    
    board_id = "69ba73877815cfcc8f0b3900"
    client = TrelloClient(api_key, token)
    
    # Get existing labels
    labels = client.get_labels(board_id)
    print(f"Bestaande labels: {list(labels.keys())}")
    
    # Create missing lists
    print("\n=== Lever bericht aan MDP ===")
    list_id = client.create_list(board_id, "Lever bericht aan MDP")
    print(f"Lijst aangemaakt: Lever bericht aan MDP")
    
    # US-28
    card_id = client.create_card(
        list_id, 
        "US-28 - STEP_6A - Doorzetten naar raw-layer",
        "Als data-platform wil ik origineel bericht in raw-layer zodat brondata behouden blijft.\n\nAcceptance Criteria:\n- Origineel bericht wordt naar raw-layer geschreven.\n- Pad/locatie wordt vastgelegd in resultaat.",
        [labels.get("fase-1"), labels.get("blauw")]
    )
    client.add_checklist(card_id, "Acceptance Criteria", [
        "Origineel bericht wordt naar raw-layer geschreven.",
        "Pad/locatie wordt vastgelegd in resultaat."
    ])
    print("  Kaart aangemaakt: US-28 - STEP_6A - Doorzetten naar raw-layer")
    
    # US-29
    card_id = client.create_card(
        list_id,
        "US-29 - STEP_6B - Vastleggen afleverstatus",
        "Als operations wil ik definitieve afleverstatus vastleggen zodat ketenstatus compleet is.\n\nAcceptance Criteria:\n- Definitieve afleverstatus wordt opgeslagen.\n- Pipeline eindigt met actuele eindstatus.",
        [labels.get("fase-1"), labels.get("blauw")]
    )
    client.add_checklist(card_id, "Acceptance Criteria", [
        "Definitieve afleverstatus wordt opgeslagen.",
        "Pipeline eindigt met actuele eindstatus."
    ])
    print("  Kaart aangemaakt: US-29 - STEP_6B - Vastleggen afleverstatus")
    
    # Create Project Scope list
    print("\n=== Project Scope (Code-backed) ===")
    scope_list_id = client.create_list(board_id, "Project Scope (Code-backed)")
    print(f"Lijst aangemaakt: Project Scope (Code-backed)")
    
    scope_items = [
        {
            "title": "Project Infrastructure - Java + PostgreSQL",
            "desc": "Spring Boot 3.x + Maven + PostgreSQL database.\n\nTechnologie Stack:\n- Framework: Spring Boot 3.x with Spring Data JPA, Spring Security, Spring Web (REST)\n- Build: Maven with JUnit 5 test framework\n- Database: PostgreSQL with transactional service layer\n- Pipeline: 29-step synchronous orchestration"
        },
        {
            "title": "API Contract - POST /api/messages",
            "desc": "Submit JSON bericht naar pipeline (MarketMessageController.java:32-39)"
        },
        {
            "title": "API Contract - POST /api/messages/xml",
            "desc": "Submit raw XML bericht naar pipeline (MarketMessageController.java:46-59)"
        },
        {
            "title": "API Contract - GET /api/messages/{uuid}",
            "desc": "Fetch volledige berichtstatus met alle 29 stapresultaten (MarketMessageController.java:67-73)"
        },
        {
            "title": "API Contract - GET /api/messages/status/{status}",
            "desc": "Filter berichten op status (MarketMessageController.java:87-95)"
        },
        {
            "title": "API Contract - POST /api/messages/{uuid}/reprocess",
            "desc": "Retry FAILED of PARKED berichten (MarketMessageController.java:102-113)"
        },
        {
            "title": "Reprocess Logic Implementation",
            "desc": "Logica voor retry van FAILED/PARKED berichten (MarketMessageService.java:177-200)"
        },
        {
            "title": "Error Translation Layer",
            "desc": "Centralized exception handling met gestandaardiseerde JSON responses (GlobalExceptionHandler.java:24-121)"
        },
        {
            "title": "Security Headers Implementation",
            "desc": "5 security headers: CSP, X-Frame-Options, HSTS, Referrer-Policy, Permissions-Policy (SecurityConfig.java:29-105)"
        },
        {
            "title": "Rate Limiting per IP",
            "desc": "Per-IP buckets: 30/min submit, 5/min seed, 120/min general (RateLimitFilter.java:34-112)"
        },
        {
            "title": "Input Validation & Sanitization",
            "desc": "UUID/EAN/size validation, control-char sanitization (InputSanitizer.java:13-66)"
        }
    ]
    
    for item in scope_items:
        card_id = client.create_card(
            scope_list_id,
            item["title"],
            item["desc"],
            [labels.get("fase-1"), labels.get("blauw")]
        )
        client.add_checklist(card_id, "Definition of Done", [
            "Code is geïmplementeerd en getest",
            "Acceptatie criteria zijn vervuld"
        ])
        print(f"  Kaart aangemaakt: {item['title']}")
    
    # Create Foutcodes reference list
    print("\n=== Foutcodes referentie ===")
    ref_list_id = client.create_list(board_id, "Foutcodes referentie")
    print(f"Lijst aangemaakt: Foutcodes referentie")
    
    # REF-ERR-01
    card_id = client.create_card(
        ref_list_id,
        "Officiële foutcodes (Business-Service v4.0)",
        "Bron: Business-Service-Uitwisselen-allocatiegegevens-elektriciteit-v4.0.pdf\n\n32 officiële foutcodes van 650 t/m 999",
        [labels.get("fase-2"), labels.get("geel")]
    )
    client.add_checklist(card_id, "Reference", [
        "Alle officiële foutcodes zijn aanwezig",
        "Betekenis per foutcode is eenduidig opgenomen"
    ])
    print("  Kaart aangemaakt: Officiële foutcodes (Business-Service v4.0)")
    
    # REF-ERR-02
    card_id = client.create_card(
        ref_list_id,
        "Foutcodes per validatiestap (3A-3G)",
        "Mapping van ErrorCode.java enum naar validatiestappen:\n3A: 761, 765, 777\n3B: 650, 651, 667, 668, 683, 686, 701, 745, 681, 747, 754, 758, 759, 764, 771, 773, 779, 781, 999\n3C: 999\n3E: 663, 772, 763\n3F: 671, 676, 782\n3G: 651, 669, 670, 704, 769, 774, 776, 780, 999",
        [labels.get("fase-2"), labels.get("geel")]
    )
    client.add_checklist(card_id, "Reference", [
        "Per validatiestap is de set expliciet vastgelegd",
        "Koppeling tussen US-kaarten en foutcodes is traceerbaar"
    ])
    print("  Kaart aangemaakt: Foutcodes per validatiestap (3A-3G)")
    
    print("\n✅ Klaar! Alle ontbrekende lists en kaarten zijn toegevoegd.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
