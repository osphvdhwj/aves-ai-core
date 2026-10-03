#!/usr/bin/env python3
"""
Merge multiple tag sources into a single assets/vocab.jsonl.

Output schema per line:
  {"id": str, "label": str, "aliases": [str], "category": str, "nsfw": bool}

Sources (paths can be overridden via env):
  Aves curated     assets/vocab_curated.jsonl         (base, always kept)
  Taggerine JSON   ~/AI/vocab/taggerine_vocab/*.json  (general cat only)
  Danbooru CSV     ~/AI/vocab/tag_databases/danbooru.csv  (top-N by count)
  E621 CSV         ~/AI/vocab/tag_databases/e621.csv      (top-N by count)
  COCO labels      ~/AI/vocab/tag_databases/coco_labels.txt

Filtering:
  - length 2..40, printable ASCII only
  - reject booru-style markers: '_' '(' ')' ':' '/' '\\' '|'
  - reject explicit booru-safe tokens ('1girl','1boy','solo','rating_*', etc.)
  - dedupe by normalized id
"""
import csv, json, os, re, sys
from pathlib import Path

REPO      = Path(__file__).resolve().parent.parent
OUT       = REPO / "app/src/main/assets/vocab.jsonl"
CURATED   = REPO / "app/src/main/assets/vocab_curated.jsonl"

HOME      = Path.home()
TAGGERINE = HOME / "AI/vocab/taggerine_vocab/tagger_vocab_with_categories_and_alias_updated.json"
DANBOORU  = HOME / "AI/vocab/tag_databases/danbooru.csv"
E621      = HOME / "AI/vocab/tag_databases/e621.csv"
COCO      = HOME / "AI/vocab/tag_databases/coco_labels.txt"

CAP_TAGGERINE = 5000
CAP_DANBOORU  = 5000
CAP_E621      = 3000

JUNK_TOKENS = {
    "1girl","1boy","2girls","2boys","3girls","3boys","multiple_girls","multiple_boys",
    "solo","solo_focus","duo","group","male_focus","female_focus",
    "highres","hi_res","absurdres","lowres","commentary","commentary_request",
    "bad_id","bad_pixiv_id","bad_twitter_id","bad_link","artist_name",
    "rating_safe","rating_questionable","rating_explicit","rating_general",
    "translated","check_translation","english_text","japanese_text",
    "signature","watermark","logo","web_address","twitter_username","patreon_username",
    "commission","commissioned","paid_reward",
}

SAFE_ASCII = re.compile(r'^[A-Za-z0-9 \-\'\.&!?,]+$')

def norm_id(label: str) -> str:
    s = label.strip().lower()
    s = re.sub(r"[^a-z0-9]+", "_", s).strip("_")
    return s

def clean(label: str) -> bool:
    label = label.strip()
    if not (2 <= len(label) <= 40):
        return False
    if not SAFE_ASCII.match(label):
        return False
    if "_" in label or "(" in label or ")" in label or ":" in label or "/" in label or "|" in label:
        return False
    if norm_id(label) in JUNK_TOKENS:
        return False
    return True

# ---- accumulators ----
entries = {}   # id -> entry
def add(label, category="misc", nsfw=False, aliases=None, source=""):
    if not clean(label):
        return False
    eid = norm_id(label)
    if not eid or eid in entries:
        return False
    entries[eid] = {
        "id": eid, "label": label.strip(),
        "aliases": [a for a in (aliases or []) if clean(a)][:4],
        "category": category, "nsfw": nsfw,
    }
    return True

# ---- 1. curated base ----
if CURATED.exists():
    n = 0
    for line in CURATED.read_text().splitlines():
        if not line.strip(): continue
        e = json.loads(line)
        if add(e["label"], e.get("category","misc"), e.get("nsfw",False), e.get("aliases",[]), "curated"):
            n += 1
    print(f"curated:   +{n}")
else:
    print("curated:   (missing base file)")

# ---- 2. COCO ----
if COCO.exists():
    n = 0
    for line in COCO.read_text().splitlines():
        m = re.match(r'^\d+:\s*(.+)$', line)
        if not m: continue
        name = m.group(1).strip()
        if name == "unlabeled": continue
        if add(name, "misc", False, [], "coco"):
            n += 1
    print(f"coco:      +{n}")

# ---- 3. Taggerine (general category only) ----
if TAGGERINE.exists():
    data = json.loads(TAGGERINE.read_text())
    idx2tag = data["idx2tag"]; tag2cat = data["tag2category"]; tag2alias = data["tag2alias"]
    n = 0; seen = 0
    for tag in idx2tag:
        if seen >= CAP_TAGGERINE: break
        cat = tag2cat.get(tag)
        # danbooru category codes: 0=general, 5=meta. Keep general only.
        if cat != 0: continue
        seen += 1
        al = tag2alias.get(tag, "")
        alias_list = [a.strip() for a in al.split(",") if a.strip() and a.strip() != tag]
        if add(tag, "misc", False, alias_list, "taggerine"):
            n += 1
    print(f"taggerine: +{n}  (scanned {seen} general tags)")

# ---- 4. Danbooru top-N ----
if DANBOORU.exists():
    rows = []
    with DANBOORU.open(newline='', encoding='utf-8', errors='replace') as f:
        for row in csv.reader(f):
            if len(row) < 3: continue
            tag, cat, cnt = row[0], row[1], row[2]
            aliases = row[3] if len(row) > 3 else ""
            if cat != "0": continue
            try: cnt = int(cnt)
            except: continue
            rows.append((cnt, tag, aliases))
    rows.sort(reverse=True)
    n = 0
    for cnt, tag, al in rows[:CAP_DANBOORU * 3]:
        if n >= CAP_DANBOORU: break
        alias_list = [a.strip() for a in al.split(",") if a.strip() and a.strip() != tag] if al else []
        if add(tag, "misc", False, alias_list, "danbooru"):
            n += 1
    print(f"danbooru:  +{n}")

# ---- 5. E621 top-N ----
if E621.exists():
    rows = []
    with E621.open(newline='', encoding='utf-8', errors='replace') as f:
        for row in csv.reader(f):
            if len(row) < 3: continue
            tag, cat, cnt = row[0], row[1], row[2]
            aliases = row[3] if len(row) > 3 else ""
            if cat != "0": continue
            try: cnt = int(cnt)
            except: continue
            rows.append((cnt, tag, aliases))
    rows.sort(reverse=True)
    n = 0
    for cnt, tag, al in rows[:CAP_E621 * 3]:
        if n >= CAP_E621: break
        alias_list = [a.strip() for a in al.split(",") if a.strip() and a.strip() != tag] if al else []
        if add(tag, "misc", False, alias_list, "e621"):
            n += 1
    print(f"e621:      +{n}")

# ---- output ----
OUT.parent.mkdir(parents=True, exist_ok=True)
items = sorted(entries.values(), key=lambda e: (e["nsfw"], e["category"], e["id"]))
with OUT.open("w", encoding="utf-8") as f:
    for e in items:
        f.write(json.dumps(e, ensure_ascii=False) + "\n")

print()
print(f"total:     {len(items)} entries -> {OUT}")
print(f"size:      {OUT.stat().st_size} bytes")
