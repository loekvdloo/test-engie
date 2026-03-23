import json
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

DOCX = Path("test2.docx")
OUT = Path("docs/test2-step-colors.json")

ns = {
    "w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
}

if not DOCX.exists():
    raise SystemExit("test2.docx not found")

with zipfile.ZipFile(DOCX, "r") as zf:
    xml_data = zf.read("word/document.xml")

root = ET.fromstring(xml_data)

items = []
for para in root.findall(".//w:p", ns):
    text_parts = []
    colors = []
    for run in para.findall(".//w:r", ns):
        t = run.find("w:t", ns)
        if t is None or t.text is None:
            continue
        text = t.text.strip()
        if not text:
            continue
        text_parts.append(text)

        color = None
        rpr = run.find("w:rPr", ns)
        if rpr is not None:
            c = rpr.find("w:color", ns)
            if c is not None:
                color = c.attrib.get(f"{{{ns['w']}}}val")
        colors.append(color)

    if text_parts:
        joined = " ".join(text_parts).strip()
        items.append({"text": joined, "colors": colors})

# filter likely step lines
keywords = [
    "Ontvang", "Ontvangen", "Technische", "Classificeer", "wachtrij", "Uitzondering",
    "Valideer", "BRP", "validatie", "Bepaal", "ACK", "NACK", "Verstuur", "Lever", "raw-layer", "afleverstatus"
]
step_lines = [
    item for item in items if any(k.lower() in item["text"].lower() for k in keywords)
]

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps(step_lines, indent=2, ensure_ascii=False), encoding="utf-8")
print(f"Saved {len(step_lines)} lines to {OUT}")
for item in step_lines:
    uniq = sorted({c for c in item['colors'] if c})
    print(f"{item['text']} | colors={uniq}")
