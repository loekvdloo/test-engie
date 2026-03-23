import json
import os
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

BASE_URL = "https://api.trello.com/1"

# Load Trello credentials from environment variables to avoid storing secrets in Git.
KEY = os.getenv("TRELLO_KEY", "")
TOKEN = os.getenv("TRELLO_TOKEN", "")

LABEL_COLOR_MAP = {
    "fase-1": "blue",   "blauw":  "blue",
    "fase-2": "yellow", "geel":   "yellow",
    "fase-3": "orange", "oranje": "orange",
    "fase-4": "green",  "groen":  "green",
}

WORKFLOW_LISTS = ["Backlog", "To Do", "Doing", "Testing", "Done"]

DOD = [
    "Code is geimplementeerd en peer-reviewed.",
    "Acceptatiecriteria zijn aantoonbaar gevalideerd in test of demo.",
    "Unit/integratietests zijn groen en opgenomen in CI.",
    "Documentatie is bijgewerkt waar relevant.",
]


def api(method, path, params=None):
    p = dict(params or {})
    p["key"] = KEY
    p["token"] = TOKEN
    url = f"{BASE_URL}{path}?{urlencode(p, doseq=True)}"
    req = Request(url=url, method=method)
    try:
        with urlopen(req) as r:
            body = r.read().decode("utf-8")
            return json.loads(body) if body else {}
    except HTTPError as e:
        raise RuntimeError(f"HTTP {e.code} on {path}: {e.read().decode('utf-8', errors='replace')}") from e


def post(path, params):
    return api("POST", path, params)


def add_checklist(card_id, name, items):
    cl = post("/checklists", {"idCard": card_id, "name": name})
    for item in items:
        post(f"/checklists/{cl['id']}/checkItems", {"name": item, "checked": "false"})


def build_desc(card):
    parts = []
    cid = card.get("id", "")
    if cid:
        parts.append(f"Story ID: {cid}")
    desc = card.get("description", "")
    if desc:
        parts.append(desc)
    return "\n\n".join(parts)


def main():
    if not KEY or not TOKEN:
        raise RuntimeError("Missing Trello credentials. Set TRELLO_KEY and TRELLO_TOKEN environment variables.")

    data = json.loads(Path("docs/trello-user-stories.json").read_text("utf-8"))
    board_meta = data["board"]
    board_name = board_meta.get("name", "ENGIE Allocation Processor")
    board_desc = board_meta.get("description", "")
    lists = board_meta.get("lists", [])

    # Create board
    board = post("/boards", {
        "name": board_name,
        "desc": board_desc,
        "defaultLists": "false",
        "defaultLabels": "false",
    })
    board_id = board["id"]
    board_url = board.get("url", f"https://trello.com/b/{board_id}")
    print(f"Board aangemaakt: {board_name}")
    print(f"URL: {board_url}")

    # Create labels
    label_ids = {}
    seen = set()
    for lst in lists:
        for card in lst.get("cards", []):
            for lname in card.get("labels", []):
                if lname in seen:
                    continue
                seen.add(lname)
                color = LABEL_COLOR_MAP.get(lname.lower(), "green")
                result = post("/labels", {"idBoard": board_id, "name": lname, "color": color})
                label_ids[lname] = result["id"]
    print(f"Labels aangemaakt: {len(label_ids)}")

    # Create lists + cards
    total_cards = 0
    for lst in lists:
        skip_names = set(WORKFLOW_LISTS)
        if lst["name"] in skip_names:
            continue

        created_list = post("/lists", {"idBoard": board_id, "name": lst["name"], "pos": "bottom"})
        list_id = created_list["id"]
        print(f"  Lijst: {lst['name']}")

        for card in lst.get("cards", []):
            card_label_ids = [label_ids[n] for n in card.get("labels", []) if n in label_ids]
            created = post("/cards", {
                "idList": list_id,
                "name": card["title"],
                "desc": build_desc(card),
                "pos": "bottom",
                "idLabels": ",".join(card_label_ids) if card_label_ids else "",
            })
            cid = created["id"]
            total_cards += 1

            ac = card.get("acceptanceCriteria", [])
            if ac:
                add_checklist(cid, "Acceptatiecriteria", ac)

            dod = card.get("definitionOfDone", DOD)
            add_checklist(cid, "Definition of Done", dod)

            print(f"    Kaart: {card['title']}")

    # Workflow lists at the end
    for wname in WORKFLOW_LISTS:
        post("/lists", {"idBoard": board_id, "name": wname, "pos": "bottom"})
    print(f"Workflow-lijsten toegevoegd: {', '.join(WORKFLOW_LISTS)}")

    print(f"\nKlaar! {total_cards} kaarten aangemaakt.")
    print(f"Board URL: {board_url}")
    return board_url


if __name__ == "__main__":
    main()
