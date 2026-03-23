#!/usr/bin/env python3
"""Complete the new Trello board with all missing lists."""
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
    
    board_id = "69ba74cd0dd470607940d454"
    client = TrelloClient(api_key, token)
    
    # Get existing labels
    labels = client.get_labels(board_id)
    print(f"Bestaande labels: {list(labels.keys())}\n")
    
    # === Lever bericht aan MDP ===
    print("=== Lever bericht aan MDP ===")
    list_id = client.create_list(board_id, "Lever bericht aan MDP")
    
    card_id = client.create_card(
        list_id,
        "US-28 - STEP_6A - Doorzetten naar raw-layer",
        "Als data-platform wil ik origineel bericht in raw-layer zodat brondata behouden blijft.",
        [labels.get("fase-1"), labels.get("blauw")]
    )
    client.add_checklist(card_id, "Acceptance Criteria", [
        "Origineel bericht wordt naar raw-layer geschreven.",
        "Pad/locatie wordt vastgelegd in resultaat."
    ])
    print("  ✅ US-28 - STEP_6A - Doorzetten naar raw-layer")
    
    card_id = client.create_card(
        list_id,
        "US-29 - STEP_6B - Vastleggen afleverstatus",
        "Als operations wil ik definitieve afleverstatus vastleggen zodat ketenstatus compleet is.",
        [labels.get("fase-1"), labels.get("blauw")]
    )
    client.add_checklist(card_id, "Acceptance Criteria", [
        "Definitieve afleverstatus wordt opgeslagen.",
        "Pipeline eindigt met actuele eindstatus."
    ])
    print("  ✅ US-29 - STEP_6B - Vastleggen afleverstatus")
    
    # === Project Scope (Code-backed) ===
    print("\n=== Project Scope (Code-backed) ===")
    scope_list_id = client.create_list(board_id, "Project Scope (Code-backed)")
    
    scope_items = [
        ("Project Infrastructure - Java + PostgreSQL", "Spring Boot 3.x + Maven + PostgreSQL database met 29-step pipeline"),
        ("API Contract - POST /api/messages", "Submit JSON bericht (MarketMessageController.java:32-39)"),
        ("API Contract - POST /api/messages/xml", "Submit raw XML bericht (MarketMessageController.java:46-59)"),
        ("API Contract - GET /api/messages/{uuid}", "Fetch volledige berichtstatus (MarketMessageController.java:67-73)"),
        ("API Contract - GET /api/messages/status/{status}", "Filter berichten op status (MarketMessageController.java:87-95)"),
        ("API Contract - POST /api/messages/{uuid}/reprocess", "Retry FAILED/PARKED berichten (MarketMessageController.java:102-113)"),
        ("Reprocess Logic Implementation", "Reset en reinitializeer pipeline (MarketMessageService.java:177-200)"),
        ("Error Translation Layer", "Centralized exception handling (GlobalExceptionHandler.java:24-121)"),
        ("Security Headers Implementation", "CSP, X-Frame-Options, HSTS, Referrer-Policy, Permissions-Policy (SecurityConfig.java:29-105)"),
        ("Rate Limiting per IP", "30/60s submit, 5/60s seed, 120/60s general (RateLimitFilter.java:34-112)"),
        ("Input Validation & Sanitization", "UUID/EAN/2MB validation (InputSanitizer.java:13-66)")
    ]
    
    for title, desc in scope_items:
        card_id = client.create_card(scope_list_id, title, desc, [labels.get("fase-1"), labels.get("blauw")])
        client.add_checklist(card_id, "Definition of Done", [
            "Code is geïmplementeerd en getest",
            "Acceptatie criteria zijn vervuld"
        ])
        print(f"  ✅ {title}")
    
    # === Dashboard ===
    print("\n=== Dashboard ===")
    dashboard_list_id = client.create_list(board_id, "Dashboard")
    
    card_id = client.create_card(
        dashboard_list_id,
        "Dashboard & Monitoring - Fase 4",
        "Real-time monitoring dashboard voor alle 29 pipeline-stappen.\n- Live progress indicator\n- Message status overview\n- Error code distribution\n- Performance metrics\n- Rate limit monitoring",
        [labels.get("fase-4"), labels.get("groen")]
    )
    client.add_checklist(card_id, "Definition of Done", [
        "Dashboard toont alle 29 stappen in real-time",
        "Message filtering werkt per status",
        "Performance metrics zijn nauwkeurig",
        "Error codes worden grafisch weergegeven"
    ])
    print("  ✅ Dashboard & Monitoring - Fase 4")
    
    # === Foutcodes referentie ===
    print("\n=== Foutcodes referentie ===")
    ref_list_id = client.create_list(board_id, "Foutcodes referentie")
    
    card_id = client.create_card(
        ref_list_id,
        "Officiële foutcodes (Business-Service v4.0)",
        "32 officiële foutcodes van 650 t/m 999 uit Business-Service spec",
        [labels.get("fase-2"), labels.get("geel")]
    )
    client.add_checklist(card_id, "Reference", [
        "Alle officiële foutcodes zijn aanwezig",
        "Betekenis per foutcode is eenduidig"
    ])
    print("  ✅ Officiële foutcodes (Business-Service v4.0)")
    
    card_id = client.create_card(
        ref_list_id,
        "Foutcodes per validatiestap (3A-3G)",
        "Mapping van ErrorCode.java enum naar validatiestappen 3A-3G",
        [labels.get("fase-2"), labels.get("geel")]
    )
    client.add_checklist(card_id, "Reference", [
        "Per validatiestap is de foutcode-set expliciet",
        "Koppeling tussen US-kaarten en foutcodes is traceerbaar"
    ])
    print("  ✅ Foutcodes per validatiestap (3A-3G)")
    
    # === Workflow Lijsten ===
    print("\n=== Workflow Lijsten ===")
    for workflow_name in ["Backlog", "To Do", "Doing", "Testing", "Done"]:
        client.create_list(board_id, workflow_name)
        print(f"  ✅ {workflow_name}")
    
    print(f"\n✅ Klaar! Nieuwe board is compleet: {board_id}")
    print(f"https://trello.com/b/{board_id}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
