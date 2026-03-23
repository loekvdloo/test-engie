import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

BASE_URL = "https://api.trello.com/1"
WORKFLOW_LISTS = ["Backlog", "To Do", "Doing", "Testing", "Done"]
MASTER_FLOW_LIST_NAME = "Master Volgorde (diagram)"
DEFINITION_OF_DONE_ITEMS = [
    "Code is gebouwd en werkt lokaal",
    "Acceptatiecriteria zijn gecontroleerd",
    "Tests zijn toegevoegd of bijgewerkt waar nodig",
    "Functioneel getest",
    "Documentatie bijgewerkt indien nodig",
]


class TrelloClient:
    def __init__(self, api_key: str, token: str, dry_run: bool = False):
        self.api_key = api_key
        self.token = token
        self.dry_run = dry_run
        self._dry_run_counters = {
            "boards": 0,
            "lists": 0,
            "labels": 0,
            "cards": 0,
            "checklists": 0,
            "checkitems": 0,
        }
        self._dry_run_boards: dict[str, dict[str, Any]] = {}
        self._dry_run_lists: dict[str, dict[str, Any]] = {}
        self._dry_run_labels: dict[str, dict[str, Any]] = {}
        self._dry_run_cards: dict[str, dict[str, Any]] = {}
        self._dry_run_checklists: dict[str, dict[str, Any]] = {}

    def _next_dry_run_id(self, kind: str) -> str:
        self._dry_run_counters[kind] += 1
        return f"dry-run-{kind}-{self._dry_run_counters[kind]}"

    def _dry_run_request(self, method: str, path: str, params: dict[str, Any]) -> Any:
        print(f"[DRY-RUN] {method} {BASE_URL}{path}?{urlencode(params, doseq=True)}")

        if method == "POST" and path == "/boards":
            board_id = self._next_dry_run_id("boards")
            board = {
                "id": board_id,
                "name": params.get("name", "dry-run-board"),
                "desc": params.get("desc", ""),
            }
            self._dry_run_boards[board_id] = board
            return board

        if method == "POST" and path == "/lists":
            list_id = self._next_dry_run_id("lists")
            board_id = params.get("idBoard", "")
            trello_list = {
                "id": list_id,
                "idBoard": board_id,
                "name": params.get("name", "dry-run-list"),
                "pos": params.get("pos", "bottom"),
            }
            self._dry_run_lists[list_id] = trello_list
            return trello_list

        if method == "POST" and path == "/labels":
            label_id = self._next_dry_run_id("labels")
            label = {
                "id": label_id,
                "idBoard": params.get("idBoard", ""),
                "name": params.get("name", "dry-run-label"),
                "color": params.get("color", "green"),
            }
            self._dry_run_labels[label_id] = label
            return label

        if method == "POST" and path == "/cards":
            card_id = self._next_dry_run_id("cards")
            id_labels = params.get("idLabels", "")
            card = {
                "id": card_id,
                "idList": params.get("idList", ""),
                "name": params.get("name", "dry-run-card"),
                "desc": params.get("desc", ""),
                "pos": params.get("pos", "bottom"),
                "idLabels": [label_id for label_id in str(id_labels).split(",") if label_id],
            }
            self._dry_run_cards[card_id] = card
            return card

        if method == "POST" and path == "/checklists":
            checklist_id = self._next_dry_run_id("checklists")
            checklist = {
                "id": checklist_id,
                "idCard": params.get("idCard", ""),
                "name": params.get("name", "Checklist"),
                "checkItems": [],
            }
            self._dry_run_checklists[checklist_id] = checklist
            return checklist

        if method == "POST" and path.startswith("/checklists/") and path.endswith("/checkItems"):
            checklist_id = path.split("/")[2]
            item_id = self._next_dry_run_id("checkitems")
            item = {
                "id": item_id,
                "name": params.get("name", "Checklist item"),
                "state": "complete" if params.get("checked") == "true" else "incomplete",
            }
            checklist = self._dry_run_checklists.get(checklist_id)
            if checklist is not None:
                checklist.setdefault("checkItems", []).append(item)
            return item

        if method == "GET" and path.startswith("/boards/") and path.endswith("/lists"):
            board_id = path.split("/")[2]
            return [item for item in self._dry_run_lists.values() if item.get("idBoard") == board_id]

        if method == "GET" and path.startswith("/boards/") and path.endswith("/cards"):
            board_id = path.split("/")[2]
            board_list_ids = {item["id"] for item in self._dry_run_lists.values() if item.get("idBoard") == board_id}
            return [item for item in self._dry_run_cards.values() if item.get("idList") in board_list_ids]

        if method == "GET" and path.startswith("/cards/") and path.endswith("/checklists"):
            card_id = path.split("/")[2]
            return [item for item in self._dry_run_checklists.values() if item.get("idCard") == card_id]

        if method == "GET" and path.startswith("/boards/") and path.endswith("/labels"):
            board_id = path.split("/")[2]
            return [item for item in self._dry_run_labels.values() if item.get("idBoard") == board_id]

        if method == "PUT" and path.startswith("/lists/"):
            list_id = path.split("/")[2]
            trello_list = self._dry_run_lists.get(list_id, {"id": list_id})
            trello_list.update({key: value for key, value in params.items() if key in {"pos", "name"}})
            self._dry_run_lists[list_id] = trello_list
            return trello_list

        if method == "PUT" and path.startswith("/cards/"):
            card_id = path.split("/")[2]
            card = self._dry_run_cards.get(card_id, {"id": card_id})
            updates = {key: value for key, value in params.items() if key in {"idList", "pos", "name"}}
            if "idLabels" in params:
                updates["idLabels"] = [label_id for label_id in str(params["idLabels"]).split(",") if label_id]
            card.update(updates)
            self._dry_run_cards[card_id] = card
            return card

        if method == "PUT" and path.startswith("/labels/"):
            label_id = path.split("/")[2]
            label = self._dry_run_labels.get(label_id, {"id": label_id})
            label.update({key: value for key, value in params.items() if key in {"color", "name"}})
            self._dry_run_labels[label_id] = label
            return label

        return {}

    def _request(self, method: str, path: str, params: dict[str, Any] | None = None) -> Any:
        params = params or {}
        params["key"] = self.api_key
        params["token"] = self.token

        url = f"{BASE_URL}{path}?{urlencode(params, doseq=True)}"

        if self.dry_run:
            return self._dry_run_request(method, path, params)

        request = Request(url=url, method=method)
        try:
            with urlopen(request) as response:
                data = response.read().decode("utf-8")
                return json.loads(data) if data else {}
        except HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"Trello API fout ({exc.code}) op {path}: {body}") from exc
        except URLError as exc:
            raise RuntimeError(f"Netwerkfout bij Trello API op {path}: {exc}") from exc

    def create_board(self, name: str, description: str) -> dict[str, Any]:
        return self._request(
            "POST",
            "/boards",
            {
                "name": name,
                "desc": description,
                "defaultLists": "false",
                "defaultLabels": "false",
            },
        )

    def create_list(self, board_id: str, name: str, position: str = "bottom") -> dict[str, Any]:
        return self._request(
            "POST",
            "/lists",
            {
                "idBoard": board_id,
                "name": name,
                "pos": position,
            },
        )

    def create_label(self, board_id: str, name: str, color: str) -> dict[str, Any]:
        return self._request(
            "POST",
            "/labels",
            {
                "idBoard": board_id,
                "name": name,
                "color": color,
            },
        )

    def create_card(self, list_id: str, name: str, description: str, label_ids: list[str] | None = None) -> dict[str, Any]:
        params: dict[str, Any] = {
            "idList": list_id,
            "name": name,
            "desc": description,
            "pos": "bottom",
        }
        if label_ids:
            params["idLabels"] = ",".join(label_ids)

        return self._request("POST", "/cards", params)

    def create_checklist(self, card_id: str, name: str) -> dict[str, Any]:
        return self._request(
            "POST",
            "/checklists",
            {
                "idCard": card_id,
                "name": name,
            },
        )

    def create_checkitem(self, checklist_id: str, name: str) -> dict[str, Any]:
        return self._request(
            "POST",
            f"/checklists/{checklist_id}/checkItems",
            {
                "name": name,
                "checked": "false",
            },
        )

    def get_lists(self, board_id: str) -> list[dict[str, Any]]:
        return self._request("GET", f"/boards/{board_id}/lists", {"cards": "none"})

    def get_cards(self, board_id: str) -> list[dict[str, Any]]:
        return self._request("GET", f"/boards/{board_id}/cards", {"fields": "name,id,idList,idLabels"})

    def get_checklists(self, card_id: str) -> list[dict[str, Any]]:
        return self._request("GET", f"/cards/{card_id}/checklists")

    def get_labels(self, board_id: str) -> list[dict[str, Any]]:
        return self._request("GET", f"/boards/{board_id}/labels", {"fields": "name,color,id"})

    def update_list_position(self, list_id: str, position: str) -> dict[str, Any]:
        return self._request("PUT", f"/lists/{list_id}", {"pos": position})

    def update_card(self, card_id: str, list_id: str | None = None, position: str | None = None) -> dict[str, Any]:
        params: dict[str, Any] = {}
        if list_id:
            params["idList"] = list_id
        if position:
            params["pos"] = position
        return self._request("PUT", f"/cards/{card_id}", params)

    def update_card_name(self, card_id: str, name: str) -> dict[str, Any]:
        return self._request("PUT", f"/cards/{card_id}", {"name": name})

    def update_label_color(self, label_id: str, color: str) -> dict[str, Any]:
        return self._request("PUT", f"/labels/{label_id}", {"color": color})

    def update_card_labels(self, card_id: str, label_ids: list[str]) -> dict[str, Any]:
        return self._request("PUT", f"/cards/{card_id}", {"idLabels": ",".join(label_ids)})


LABEL_COLOR_MAP = {
    "fase-1": "blue",
    "blauw": "blue",
    "fase-2": "yellow",
    "geel": "yellow",
    "fase-3": "orange",
    "oranje": "orange",
    "fase-4": "green",
    "groen": "green",
    "fase-5": "green",
    "fase-6": "green",
}


def load_board_definition(json_path: Path) -> dict[str, Any]:
    with json_path.open("r", encoding="utf-8") as file:
        return json.load(file)


def build_card_description(card: dict[str, Any]) -> str:
    story_text = card.get("description", "")
    card_id = card.get("id", "")

    parts = []
    if card_id:
        parts.append(f"Story ID: {card_id}")
    if story_text:
        parts.append(story_text)

    return "\n\n".join(parts)


def augment_board_with_workflow(client: TrelloClient, board_id: str) -> None:
    existing_lists = client.get_lists(board_id)
    existing_list_names = {item["name"] for item in existing_lists}

    for list_name in WORKFLOW_LISTS:
        if list_name not in existing_list_names:
            client.create_list(board_id, list_name)
            print(f"Workflow-lijst aangemaakt: {list_name}")

    for card in client.get_cards(board_id):
        existing_checklists = client.get_checklists(card["id"])
        checklist_names = {item.get("name") for item in existing_checklists}

        if "Definition of Done" in checklist_names:
            continue

        checklist = client.create_checklist(card["id"], "Definition of Done")
        for item in DEFINITION_OF_DONE_ITEMS:
            client.create_checkitem(checklist["id"], item)
        print(f"Definition of Done toegevoegd aan: {card['name']}")


def sync_board_order(client: TrelloClient, board_id: str, lists: list[dict[str, Any]]) -> None:
    existing_lists = client.get_lists(board_id)
    existing_list_by_name = {item["name"]: item for item in existing_lists}
    existing_cards_by_name = {item["name"]: item for item in client.get_cards(board_id)}

    list_position = 1
    for trello_list in lists:
        existing_list = existing_list_by_name.get(trello_list["name"])
        if not existing_list:
            print(f"Lijst niet gevonden, overgeslagen: {trello_list['name']}")
            continue

        client.update_list_position(existing_list["id"], str(list_position))
        print(f"Lijst geordend: {trello_list['name']} -> positie {list_position}")
        list_position += 1

        card_position = 1
        for card in trello_list.get("cards", []):
            existing_card = existing_cards_by_name.get(card["title"])
            if not existing_card:
                print(f"  Kaart niet gevonden, overgeslagen: {card['title']}")
                continue

            client.update_card(existing_card["id"], list_id=existing_list["id"], position=str(card_position))
            print(f"  Kaart geordend: {card['title']} -> positie {card_position}")
            card_position += 1


def add_or_update_master_flow(client: TrelloClient, board_id: str, board_data: dict[str, Any]) -> None:
    lists = board_data.get("lists", [])
    master_flow = board_data.get("masterFlow", [])

    story_by_id: dict[str, dict[str, Any]] = {}
    phase_by_story_id: dict[str, str] = {}
    for trello_list in lists:
        phase_name = trello_list.get("name", "Onbekende fase")
        for card in trello_list.get("cards", []):
            story_id = card.get("id")
            if not story_id:
                continue
            story_by_id[story_id] = card
            phase_by_story_id[story_id] = phase_name

    if not master_flow:
        master_flow = [card_id for card_id in story_by_id]

    existing_lists = client.get_lists(board_id)
    existing_list_by_name = {item["name"]: item for item in existing_lists}

    if MASTER_FLOW_LIST_NAME in existing_list_by_name:
        master_list = existing_list_by_name[MASTER_FLOW_LIST_NAME]
    else:
        master_list = client.create_list(board_id, MASTER_FLOW_LIST_NAME, position="1")
        print(f"Master-lijst aangemaakt: {MASTER_FLOW_LIST_NAME}")

    existing_cards = client.get_cards(board_id)
    master_cards = [item for item in existing_cards if item.get("idList") == master_list["id"]]
    master_card_by_name = {item["name"]: item for item in master_cards}

    for index, story_id in enumerate(master_flow, start=1):
        story = story_by_id.get(story_id)
        if not story:
            print(f"Master-flow item niet gevonden in stories, overgeslagen: {story_id}")
            continue

        base_title = story.get("title", story_id)
        phase_name = phase_by_story_id.get(story_id, "Onbekende fase")
        master_title = f"{index:02d} - {base_title} ({phase_name})"

        existing_master_card = master_card_by_name.get(master_title)
        if existing_master_card:
            client.update_card(existing_master_card["id"], list_id=master_list["id"], position=str(index))
            continue

        created_master_card = client.create_card(
            list_id=master_list["id"],
            name=master_title,
            description=f"Globale diagramvolgorde: {index}.\nBron story: {story_id}",
            label_ids=None,
        )
        client.update_card(created_master_card["id"], list_id=master_list["id"], position=str(index))
        print(f"Master-flow kaart toegevoegd: {master_title}")


def fix_label_colors(client: TrelloClient, board_id: str) -> None:
    labels = client.get_labels(board_id)
    for label in labels:
        name = (label.get("name") or "").strip().lower()
        if not name:
            continue

        target_color = LABEL_COLOR_MAP.get(name)
        if not target_color:
            continue

        current_color = label.get("color")
        if current_color == target_color:
            continue

        client.update_label_color(label["id"], target_color)
        print(f"Labelkleur bijgewerkt: {label.get('name')} -> {target_color}")


def sync_labels(client: TrelloClient, board_id: str, lists: list[dict[str, Any]]) -> None:
    board_labels = client.get_labels(board_id)
    label_by_name = {item.get("name"): item for item in board_labels}

    required_label_names: set[str] = set()
    for trello_list in lists:
        for card in trello_list.get("cards", []):
            for label_name in card.get("labels", []):
                required_label_names.add(label_name)

    for label_name in sorted(required_label_names):
        if label_name in label_by_name:
            current = label_by_name[label_name]
            target_color = LABEL_COLOR_MAP.get(label_name.lower(), "green")
            if current.get("color") != target_color:
                client.update_label_color(current["id"], target_color)
                current["color"] = target_color
                print(f"Labelkleur gesynchroniseerd: {label_name} -> {target_color}")
            continue

        target_color = LABEL_COLOR_MAP.get(label_name.lower(), "green")
        created = client.create_label(board_id, label_name, target_color)
        label_by_name[label_name] = created
        print(f"Label aangemaakt bij sync: {label_name} ({target_color})")

    existing_cards = client.get_cards(board_id)
    card_by_name = {item.get("name"): item for item in existing_cards}

    for trello_list in lists:
        for card in trello_list.get("cards", []):
            card_name = card.get("title")
            existing_card = card_by_name.get(card_name)
            if not existing_card:
                print(f"Kaart niet gevonden voor labelsync: {card_name}")
                continue

            desired_label_ids = [
                label_by_name[name]["id"]
                for name in card.get("labels", [])
                if name in label_by_name
            ]

            current_label_ids = existing_card.get("idLabels", [])
            if set(current_label_ids) == set(desired_label_ids):
                continue

            client.update_card_labels(existing_card["id"], desired_label_ids)
            print(f"Kaartlabels gesynchroniseerd: {card_name}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Maak een Trello-bord aan op basis van trello-user-stories.json")
    parser.add_argument(
        "--input",
        default="docs/trello-user-stories.json",
        help="Pad naar het JSON-bestand met borddefinitie",
    )
    parser.add_argument(
        "--board-name",
        default=None,
        help="Overschrijf de bordnaam uit het JSON-bestand",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Toon alleen welke API-calls gedaan zouden worden",
    )
    parser.add_argument(
        "--board-id",
        default=None,
        help="Bestaand Trello board id om uit te breiden",
    )
    parser.add_argument(
        "--augment-workflow",
        action="store_true",
        help="Voeg workflow-lijsten en Definition of Done checklists toe aan een bestaand of nieuw bord",
    )
    parser.add_argument(
        "--sync-order",
        action="store_true",
        help="Orden een bestaand bord volgens de lijst- en kaartvolgorde uit het JSON-bestand",
    )
    parser.add_argument(
        "--add-master-flow",
        action="store_true",
        help="Voeg/actualiseer een aparte master-volgorde lijst over alle fases heen",
    )
    parser.add_argument(
        "--fix-label-colors",
        action="store_true",
        help="Werk bestaande labelkleuren bij volgens de fase-kleurmapping",
    )
    parser.add_argument(
        "--sync-labels",
        action="store_true",
        help="Synchroniseer kaartlabels van bestaand bord met labels uit het JSON-bestand",
    )
    args = parser.parse_args()

    api_key = os.getenv("TRELLO_KEY")
    token = os.getenv("TRELLO_TOKEN")

    if not api_key or not token:
        print("Fout: zet eerst TRELLO_KEY en TRELLO_TOKEN in je environment variables.", file=sys.stderr)
        return 1

    json_path = Path(args.input)
    if not json_path.exists():
        print(f"Fout: bestand niet gevonden: {json_path}", file=sys.stderr)
        return 1

    data = load_board_definition(json_path)
    board = data.get("board", {})
    board_name = args.board_name or board.get("name") or "Nieuw Trello Bord"
    board_description = board.get("description", "")
    lists = board.get("lists", [])

    client = TrelloClient(api_key=api_key, token=token, dry_run=args.dry_run)

    if args.board_id:
        board_id = args.board_id
        print(f"Bestaand bord geselecteerd: {board_id}")
    else:
        created_board = client.create_board(board_name, board_description)
        board_id = created_board["id"]
        print(f"Bord aangemaakt: {board_name} ({board_id})")

    if not args.board_id:
        label_ids: dict[str, str] = {}
        seen_labels: set[str] = set()

        for trello_list in lists:
            for card in trello_list.get("cards", []):
                for label_name in card.get("labels", []):
                    if label_name in seen_labels:
                        continue
                    seen_labels.add(label_name)
                    color = LABEL_COLOR_MAP.get(label_name.lower(), "green")
                    created_label = client.create_label(board_id, label_name, color)
                    label_ids[label_name] = created_label["id"]
                    print(f"Label aangemaakt: {label_name}")

        for trello_list in lists:
            created_list = client.create_list(board_id, trello_list["name"])
            list_id = created_list["id"]
            print(f"Lijst aangemaakt: {trello_list['name']}")

            for card in trello_list.get("cards", []):
                card_label_ids = [label_ids[name] for name in card.get("labels", []) if name in label_ids]
                created_card = client.create_card(
                    list_id=list_id,
                    name=card["title"],
                    description=build_card_description(card),
                    label_ids=card_label_ids,
                )
                card_id = created_card["id"]
                print(f"  Kaart aangemaakt: {card['title']}")

                acceptance_criteria = card.get("acceptanceCriteria", [])
                if acceptance_criteria:
                    checklist = client.create_checklist(card_id, "Acceptatiecriteria")
                    checklist_id = checklist["id"]
                    for item in acceptance_criteria:
                        client.create_checkitem(checklist_id, item)
                    print(f"    Checklist toegevoegd ({len(acceptance_criteria)} items)")

    if args.augment_workflow:
        augment_board_with_workflow(client, board_id)

    if args.sync_order:
        sync_board_order(client, board_id, lists)

    if args.add_master_flow:
        add_or_update_master_flow(client, board_id, board)

    if args.fix_label_colors:
        fix_label_colors(client, board_id)

    if args.sync_labels:
        sync_labels(client, board_id, lists)

    print("Klaar.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
