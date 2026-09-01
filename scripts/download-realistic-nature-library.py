import csv
import html
import json
import os
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(r"D:\PostFlowData\kids-assets\realistic")
API = "https://commons.wikimedia.org/w/api.php"
USER_AGENT = "PostFlowKidsQuiz/1.0 (realistic nature library downloader)"
THUMB_WIDTH = 1600
PER_SPECIES = 2

CATEGORIES = {
    "animals": [
        "lion", "tiger", "elephant", "giraffe", "zebra", "giant panda", "brown bear", "polar bear", "wolf", "red fox",
        "cheetah", "leopard", "jaguar", "cougar", "gorilla", "chimpanzee", "orangutan", "baboon", "kangaroo", "koala",
        "rhinoceros", "hippopotamus", "camel", "llama", "alpaca", "horse", "donkey", "cow", "goat", "sheep",
        "pig", "rabbit", "hare", "deer", "moose", "reindeer", "bison", "buffalo", "wild boar", "hedgehog",
        "porcupine", "raccoon", "otter", "beaver", "badger", "skunk", "squirrel", "chipmunk", "sloth", "anteater",
        "armadillo", "meerkat", "hyena", "lemur", "tapir", "okapi", "gazelle", "antelope", "warthog", "capybara",
        "cat", "dog", "hamster", "guinea pig", "mouse", "rat", "bat", "seal", "walrus", "sea lion"
    ],
    "insects": [
        "monarch butterfly", "swallowtail butterfly", "blue morpho butterfly", "moth", "honey bee", "bumblebee", "wasp", "hornet",
        "ladybird beetle", "stag beetle", "rhinoceros beetle", "scarab beetle", "firefly", "dragonfly", "damselfly", "grasshopper",
        "cricket insect", "katydid", "praying mantis", "stick insect", "leaf insect", "ant insect", "termite", "cockroach",
        "cicada", "aphid", "weevil", "longhorn beetle", "click beetle", "dung beetle", "ground beetle", "water beetle",
        "lacewing", "mayfly", "stonefly", "caddisfly", "earwig", "silverfish", "flea", "mosquito",
        "house fly", "horse fly", "hoverfly", "robber fly", "fruit fly", "mantidfly", "bee fly", "shield bug",
        "stink bug", "assassin bug", "water strider", "leafhopper", "planthopper", "thrips", "booklouse", "antlion",
        "dobsonfly", "scorpionfly", "webspinner", "glowworm beetle"
    ],
    "fish": [
        "clownfish", "blue tang fish", "yellow tang fish", "angelfish", "butterflyfish", "parrotfish", "lionfish", "pufferfish",
        "triggerfish", "surgeonfish", "grouper fish", "snapper fish", "barracuda", "tuna fish", "mackerel fish", "sardine fish",
        "anchovy fish", "salmon fish", "trout fish", "carp fish", "koi fish", "goldfish", "catfish", "tilapia fish",
        "bass fish", "perch fish", "pike fish", "sturgeon fish", "eel fish", "moray eel", "seahorse", "pipefish",
        "stingray", "manta ray", "sawfish", "hammerhead shark", "great white shark", "tiger shark", "whale shark", "reef shark",
        "dogfish shark", "skate fish", "halibut", "flounder", "sole fish", "cod fish", "haddock fish", "hake fish",
        "swordfish", "marlin fish", "sailfish", "mahi mahi", "arowana", "betta fish", "guppy fish", "discus fish",
        "cichlid fish", "oscar fish", "neon tetra", "zebrafish"
    ],
}

ALLOWED_LICENSE_PREFIXES = ("CC0", "CC BY")
BLOCKED_LICENSE_TOKENS = ("BY-SA", "NC", "ND")


def api_get(params):
    params = {**params, "format": "json", "formatversion": 2}
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=45) as r:
        return json.loads(r.read().decode("utf-8"))


def clean_html(text):
    if not text:
        return ""
    text = html.unescape(re.sub(r"<[^>]+>", " ", text))
    return re.sub(r"\s+", " ", text).strip()


def safe_name(s):
    s = s.lower().strip()
    s = re.sub(r"[^a-z0-9]+", "-", s)
    return s.strip("-")


def acceptable_license(meta):
    lic = clean_html(meta.get("LicenseShortName", {}).get("value", ""))
    copyrighted = clean_html(meta.get("Copyrighted", {}).get("value", ""))
    if copyrighted.lower() == "false" or "public domain" in lic.lower():
        return True, lic or "Public domain"
    upper = lic.upper()
    if any(tok in upper for tok in BLOCKED_LICENSE_TOKENS):
        return False, lic
    if upper.startswith("CC0") or upper.startswith("CC BY"):
        return True, lic
    return False, lic


def search_candidates(query):
    data = api_get({
        "action": "query",
        "generator": "search",
        "gsrsearch": query,
        "gsrnamespace": 6,
        "gsrlimit": 24,
        "prop": "imageinfo",
        "iiprop": "url|size|mime|extmetadata",
        "iiurlwidth": THUMB_WIDTH,
        "iiextmetadatafilter": "LicenseShortName|LicenseUrl|Artist|Credit|Attribution|AttributionRequired|Copyrighted|ImageDescription",
    })
    return data.get("query", {}).get("pages", [])


def pick_images(species):
    pages = search_candidates(species + " animal photograph")
    if len(pages) < PER_SPECIES:
        pages += search_candidates(species + " wildlife")
    picked = []
    seen = set()
    for p in pages:
        title = p.get("title", "")
        info = (p.get("imageinfo") or [{}])[0]
        mime = info.get("mime", "")
        if mime not in ("image/jpeg", "image/png", "image/webp"):
            continue
        if info.get("width", 0) < 900 or info.get("height", 0) < 600:
            continue
        meta = info.get("extmetadata") or {}
        ok, license_name = acceptable_license(meta)
        if not ok:
            continue
        url = info.get("thumburl") or info.get("url")
        if not url or url in seen:
            continue
        seen.add(url)
        picked.append({
            "title": title,
            "url": url,
            "description_url": info.get("descriptionurl", ""),
            "license": license_name,
            "license_url": clean_html(meta.get("LicenseUrl", {}).get("value", "")),
            "artist": clean_html(meta.get("Artist", {}).get("value", "")),
            "credit": clean_html(meta.get("Credit", {}).get("value", "")),
            "attribution": clean_html(meta.get("Attribution", {}).get("value", "")),
            "description": clean_html(meta.get("ImageDescription", {}).get("value", "")),
        })
        if len(picked) >= PER_SPECIES:
            break
    return picked


def download(url, dest):
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=90) as r, open(dest, "wb") as f:
        while True:
            chunk = r.read(1024 * 1024)
            if not chunk:
                break
            f.write(chunk)


def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    manifest = []
    total_species = sum(len(v) for v in CATEGORIES.values())
    done_species = 0
    downloaded = 0

    print(f"PostFlow Realistic Nature Library - {total_species} tur, hedef {total_species * PER_SPECIES}+ fotograf")
    print("Kaynak: Wikimedia Commons | izin: Public Domain, CC0, CC BY")
    print()

    for category, species_list in CATEGORIES.items():
        cat_dir = ROOT / category
        cat_dir.mkdir(parents=True, exist_ok=True)
        for species in species_list:
            done_species += 1
            slug = safe_name(species)
            sp_dir = cat_dir / slug
            sp_dir.mkdir(parents=True, exist_ok=True)
            existing = list(sp_dir.glob("*.jpg")) + list(sp_dir.glob("*.png")) + list(sp_dir.glob("*.webp"))
            if len(existing) >= PER_SPECIES:
                print(f"[{done_species}/{total_species}] {category}/{species}: zaten var")
                continue
            try:
                picks = pick_images(species)
            except Exception as e:
                print(f"[{done_species}/{total_species}] {species}: arama hatasi: {e}")
                time.sleep(1.0)
                continue

            if not picks:
                print(f"[{done_species}/{total_species}] {species}: uygun lisansli fotograf bulunamadi")
                continue

            for idx, item in enumerate(picks, 1):
                ext = ".jpg"
                u = item["url"].lower()
                if ".png" in u:
                    ext = ".png"
                elif ".webp" in u:
                    ext = ".webp"
                dest = sp_dir / f"{slug}_{idx:02d}{ext}"
                try:
                    download(item["url"], dest)
                    downloaded += 1
                    row = {"category": category, "species": species, "file": str(dest.relative_to(ROOT)), **item}
                    manifest.append(row)
                    print(f"[{done_species}/{total_species}] {category}/{species}: {dest.name} OK")
                except Exception as e:
                    print(f"  indirme hatasi: {e}")
            time.sleep(0.35)

    csv_path = ROOT / "ATTRIBUTION.csv"
    fields = ["category", "species", "file", "title", "artist", "credit", "attribution", "license", "license_url", "description_url", "description", "url"]
    with open(csv_path, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        for row in manifest:
            w.writerow({k: row.get(k, "") for k in fields})

    (ROOT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    (ROOT / "README.txt").write_text(
        "PostFlow Realistic Nature Library\n"
        "Source: Wikimedia Commons\n"
        "Downloader filters for Public Domain, CC0 and CC BY only.\n"
        "For CC BY files, keep ATTRIBUTION.csv with the project and include attribution where required.\n"
        "Folders: animals / insects / fish\n",
        encoding="utf-8"
    )
    print()
    print(f"TAMAM. Bu calismada indirilen: {downloaded}")
    print(f"Kutuphane: {ROOT}")
    print(f"Atif dosyasi: {csv_path}")


if __name__ == "__main__":
    main()
