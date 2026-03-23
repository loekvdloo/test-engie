import json
from pathlib import Path

import fitz  # PyMuPDF

pdf_path = Path("Engie Ilionx MCA Project Kickoff.pdf")
out_path = Path("docs/kickoff-page7-spans.json")

if not pdf_path.exists():
    raise SystemExit(f"PDF not found: {pdf_path}")

doc = fitz.open(pdf_path)
page_index = 6  # page 7 (0-based)
if len(doc) <= page_index:
    raise SystemExit(f"PDF has only {len(doc)} pages")

page = doc[page_index]
text_dict = page.get_text("dict")

spans = []
for block in text_dict.get("blocks", []):
    if block.get("type") != 0:
        continue
    for line in block.get("lines", []):
        for span in line.get("spans", []):
            text = (span.get("text") or "").strip()
            if not text:
                continue
            spans.append(
                {
                    "text": text,
                    "bbox": span.get("bbox"),
                    "size": span.get("size"),
                    "font": span.get("font"),
                    "color": span.get("color"),
                }
            )

# sort top-to-bottom, then left-to-right
spans.sort(key=lambda s: (round(s["bbox"][1], 1), round(s["bbox"][0], 1)))

out_path.parent.mkdir(parents=True, exist_ok=True)
out_path.write_text(json.dumps(spans, indent=2, ensure_ascii=False), encoding="utf-8")

print(f"Saved {len(spans)} spans to {out_path}")

# quick preview of likely step lines
keywords = [
    "Ontvang", "Classificeer", "Valideer", "Bepaal", "Verstuur", "Lever",
    "bericht", "ACK", "NACK", "BRP", "validatie", "wachtrij", "raw-layer", "afleverstatus"
]
for s in spans:
    t = s["text"]
    if any(k.lower() in t.lower() for k in keywords):
        print(f"{t} | color={s['color']} | y={s['bbox'][1]:.1f} | x={s['bbox'][0]:.1f}")
