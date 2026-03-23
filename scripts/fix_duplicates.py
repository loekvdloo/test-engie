#!/usr/bin/env python3
"""Remove duplicate lists and reorder properly."""
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
        """Get all lists on board."""
        return self._request("GET", f"/boards/{board_id}/lists", {"fields": "id,name,pos"})
    
    def close_list(self, list_id: str):
        """Close/archive a list."""
        params = {"closed": True}
        return self._request("PUT", f"/lists/{list_id}", params)
    
    def update_list_position(self, list_id: str, position: float):
        """Update list position."""
        params = {"pos": position}
        return self._request("PUT", f"/lists/{list_id}", params)

def main():
    api_key = os.getenv("TRELLO_KEY")
    token = os.getenv("TRELLO_TOKEN")
    
    if not api_key or not token:
        print("Fout: zet eerst TRELLO_KEY en TRELLO_TOKEN in je environment variables.", file=sys.stderr)
        return 1
    
    board_id = "69ba74cd0dd470607940d454"
    client = TrelloClient(api_key, token)
    
    # Get all lists
    lists = client.get_board_lists(board_id)
    print(f"Huidige lijsten ({len(lists)}):")
    for i, lst in enumerate(lists):
        print(f"  {i+1}. {lst['name']} (id: {lst['id'][:8]}...)")
    
    # Find and remove duplicates (keep first, remove later ones)
    seen = {}
    duplicates = []
    for lst in lists:
        if lst['name'] in seen:
            duplicates.append(lst)
            print(f"\n⚠️ Duplicate gevonden: {lst['name']} (id: {lst['id']})")
        else:
            seen[lst['name']] = lst['id']
    
    # Close duplicate lists
    if duplicates:
        print(f"\n=== Duplicates verwijderen ===")
        for lst in duplicates:
            client.close_list(lst['id'])
            print(f"  ✅ Gesloten: {lst['name']}")
    
    # Refresh lists after removing duplicates
    lists = client.get_board_lists(board_id)
    
    # Define desired order
    desired_order = [
        "MVP Fasering",
        "Ontvang marktbericht",
        "Classificeer bericht",
        "Valideer bericht",
        "Bepaal uitkomst",
        "Verstuur marktrespons",
        "Lever bericht aan MDP",
        "Project Scope (Code-backed)",
        "Dashboard",
        "Foutcodes referentie",
        "Backlog",
        "To Do",
        "Doing",
        "Testing",
        "Done"
    ]
    
    # Create a map of list names to IDs
    list_map = {lst['name']: lst['id'] for lst in lists}
    
    print(f"\n=== Herordening ({len(desired_order)} lijsten) ===")
    position = 1
    for list_name in desired_order:
        if list_name in list_map:
            list_id = list_map[list_name]
            client.update_list_position(list_id, position)
            print(f"  ✅ {list_name} -> positie {position}")
            position += 1
    
    print(f"\n✅ Klaar! Alles is gerepareerd.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
