import csv
import json
import os
import re
import time
import urllib.request
from collections import defaultdict
from pathlib import Path

ROOT = Path(r"D:\PostFlowData\kids-assets\realistic-openimages")
CACHE = Path(r"D:\PostFlowData\kids-assets\_openimages-cache")
CLASS_URLS = [
    "https://storage.googleapis.com/openimages/v5/class-descriptions-boxable.csv",
]
ANN_URLS = [
    "https://storage.googleapis.com/openimages/v6/oidv6-validation-annotations-bbox.csv",
    "https://storage.googleapis.com/openimages/v5/validation-annotations-bbox.csv",
]
IMAGE_URL = "https://open-images-dataset.s3.amazonaws.com/validation/{image_id}.jpg"
PER_CLASS = 12
USER_AGENT = "PostFlowKidsQuiz/1.0 OpenImages downloader"

WANTED = {
    "animals": [
        "Lion","Tiger","Elephant","Giraffe","Zebra","Bear","Brown bear","Polar bear","Wolf","Fox","Cheetah","Leopard","Jaguar",
        "Gorilla","Monkey","Kangaroo","Koala","Rhinoceros","Hippopotamus","Camel","Llama","Horse","Donkey","Cattle","Goat","Sheep",
        "Pig","Rabbit","Deer","Squirrel","Otter","Raccoon","Hedgehog","Bat","Cat","Dog","Hamster","Mouse","Rat"
    ],
    "insects": [
        "Butterfly","Moth","Bee","Beetle","Dragonfly","Grasshopper","Cricket","Ant","Caterpillar","Ladybug","Insect"
    ],
    "fish": [
        "Fish","Shark","Goldfish","Ray","Seahorse"
    ]
}

def safe_name(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-")

def download(url, dest, timeout=120):
    dest.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=timeout) as r, open(dest, "wb") as f:
        while True:
            b = r.read(1024 * 1024)
            if not b:
                break
            f.write(b)

def ensure_file(urls, dest):
    if dest.exists() and dest.stat().st_size > 1000:
        return dest
    last = None
    for url in urls:
        try:
            print("indiriliyor:", url)
            download(url, dest)
            return dest
        except Exception as e:
            last = e
            if dest.exists():
                dest.unlink(missing_ok=True)
    raise RuntimeError(f"metadata indirilemedi: {last}")

def load_classes(path):
    by_name = {}
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.reader(f):
            if len(row) >= 2:
                by_name[row[1].strip().lower()] = row[0].strip()
    return by_name

def load_needed_ids(class_map):
    out = {}
    for category, names in WANTED.items():
        for name in names:
            mid = class_map.get(name.lower())
            if mid:
                out[mid] = (category, name)
    return out

def collect_images(ann_path, needed):
    per_mid = defaultdict(list)
    seen = defaultdict(set)
    with open(ann_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            mid = row.get("LabelName")
            if mid not in needed:
                continue
            image_id = row.get("ImageID")
            if not image_id or image_id in seen[mid]:
                continue
            # Prefer reasonably large bounding boxes so the animal is visible.
            try:
                area = (float(row.get("XMax", 1)) - float(row.get("XMin", 0))) * (float(row.get("YMax", 1)) - float(row.get("YMin", 0)))
            except Exception:
                area = 1.0
            if area < 0.08:
                continue
            seen[mid].add(image_id)
            per_mid[mid].append((image_id, area))
    for mid in per_mid:
        per_mid[mid].sort(key=lambda x: x[1], reverse=True)
    return per_mid

def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    CACHE.mkdir(parents=True, exist_ok=True)
    class_csv = ensure_file(CLASS_URLS, CACHE / "class-descriptions-boxable.csv")
    ann_csv = ensure_file(ANN_URLS, CACHE / "validation-annotations-bbox.csv")

    class_map = load_classes(class_csv)
    needed = load_needed_ids(class_map)
    missing = []
    for category, names in WANTED.items():
        for name in names:
            if name.lower() not in class_map:
                missing.append(name)
    print(f"Bulunan hedef sinif: {len(needed)} | bulunamayan sinif: {len(missing)}")
    if missing:
        print("Open Images'da birebir bulunamayanlar:", ", ".join(missing))

    candidates = collect_images(ann_csv, needed)
    manifest = []
    total_new = 0
    for mid, (category, label) in needed.items():
        folder = ROOT / category / safe_name(label)
        folder.mkdir(parents=True, exist_ok=True)
        existing = [p for p in folder.glob("*.jpg") if p.stat().st_size > 30000]
        need = max(0, PER_CLASS - len(existing))
        if need == 0:
            print(f"{category}/{label}: TAMAM ({len(existing)})")
            continue
        items = candidates.get(mid, [])
        if not items:
            print(f"{category}/{label}: uygun validation resmi yok")
            continue
        downloaded = 0
        for image_id, area in items:
            if downloaded >= need:
                break
            dest = folder / f"{safe_name(label)}_{image_id}.jpg"
            if dest.exists() and dest.stat().st_size > 30000:
                continue
            try:
                download(IMAGE_URL.format(image_id=image_id), dest)
                if dest.stat().st_size < 30000:
                    dest.unlink(missing_ok=True)
                    continue
                downloaded += 1
                total_new += 1
                manifest.append({
                    "category": category,
                    "label": label,
                    "image_id": image_id,
                    "bbox_area": round(area, 4),
                    "file": str(dest.relative_to(ROOT)),
                    "source": "Open Images",
                    "source_url": f"https://storage.googleapis.com/openimages/web/index.html",
                    "license_note": "Open Images images carry per-image Creative Commons licenses; retain image IDs and verify attribution metadata before publication."
                })
                print(f"{category}/{label}: {dest.name} OK")
                time.sleep(0.12)
            except Exception as e:
                print(f"{category}/{label}: {image_id} hata: {e}")
                dest.unlink(missing_ok=True)

    manifest_path = ROOT / "manifest.json"
    old = []
    if manifest_path.exists():
        try:
            old = json.loads(manifest_path.read_text(encoding="utf-8"))
        except Exception:
            old = []
    merged = {x.get("file"): x for x in old + manifest if x.get("file")}
    manifest_path.write_text(json.dumps(list(merged.values()), ensure_ascii=False, indent=2), encoding="utf-8")
    (ROOT / "README.txt").write_text(
        "PostFlow Open Images Nature Supplement\n"
        "Real photos downloaded from the Open Images validation set.\n"
        "Folders: animals / insects / fish\n"
        "IMPORTANT: Open Images photos use per-image Creative Commons licenses. Keep image IDs and verify attribution metadata before final publication.\n",
        encoding="utf-8"
    )
    count = sum(1 for p in ROOT.rglob("*.jpg") if p.stat().st_size > 30000)
    print(f"\nTAMAM. Yeni indirilen: {total_new} | toplam Open Images fotografi: {count}")
    print("Kutuphane:", ROOT)

if __name__ == "__main__":
    main()
