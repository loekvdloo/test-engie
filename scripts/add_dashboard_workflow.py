#!/usr/bin/env python3
"""Add workflow lists and dashboard to existing Trello board."""
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
    
    def get_board_lists(self, board_id: str) -> list:
        """Get all lists on board with their IDs."""
        return self._request("GET", f"/boards/{board_id}/lists", {"fields": "id,name"})
    
    def create_list(self, board_id: str, name: str, position: str = "bottom") -> str:
        params = {"name": name, "idBoard": board_id, "pos": position}
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
    
    # Get labels
    labels = client.get_labels(board_id)
    
    # Get existing lists to find position
    existing_lists = client.get_board_lists(board_id)
    print(f"Bestaande lijsten: {[l['name'] for l in existing_lists]}")
    
    # Create Dashboard list with one card
    print("\n=== Dashboard (Fase 4 - Groen) ===")
    dashboard_list_id = client.create_list(board_id, "Dashboard")
    print(f"Lijst aangemaakt: Dashboard")
    
    card_id = client.create_card(
        dashboard_list_id,
        "Dashboard & Monitoring - Fase 4",
        "Real-time monitoring dashboard voor de 29-staps pipeline.\n\nFunctionaliteit:\n- Live pipeline progress indicator\n- Message status overview (RECEIVED/PROCESSING/COMPLETED/FAILED/PARKED)\n- Error code distribution chart\n- Performance metrics (throughput, avg. processing time)\n- Rate limit status per IP\n- Recent messages timeline\n\nTechnologie:\n- Backend: REST API endpoints (GET /api/messages, GET /api/messages/status)\n- Frontend: React/Vue dashboard component\n- Real-time: WebSocket updates voor live status changes",
        [labels.get("fase-4"), labels.get("groen")]
    )
    client.add_checklist(card_id, "Definition of Done", [
        "Dashboard toont alle 29 stappen in real-time",
        "Message filtering werkt per status",
        "Performance metrics zijn nauwkeurig",
        "Error codes worden grafisch weergegeven"
    ])
    print("  Kaart aangemaakt: Dashboard & Monitoring - Fase 4")
    
    # Create workflow lists (at the end)
    print("\n=== Workflow Lijsten ===")
    
    workflow_lists = ["Backlog", "To Do", "Doing", "Testing", "Done"]
    
    for list_name in workflow_lists:
        list_id = client.create_list(board_id, list_name)
        print(f"Lijst aangemaakt: {list_name}")
    
    print("\n✅ Klaar! Dashboard-card en workflow-lijsten zijn toegevoegd.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
