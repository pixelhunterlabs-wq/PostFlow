import csv
import html
import json
import random
import re
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(r"D:\PostFlowData\kids-assets\realistic")
API = "https://commons.wikimedia.org/w/api.php"
USER_AGENT = "PostFlowKidsQuiz/1.1 (contact: local PostFlow project; respectful Commons downloader)"
THUMB_WIDTH = 1280
PER_SPECIES = 2
REQUEST_DELAY = 1.25

CATEGORIES = {
    "animals": ["lion","tiger","elephant","giraffe","zebra","giant panda","brown bear","polar bear","wolf","red fox","cheetah","leopard","jaguar","cougar","gorilla","chimpanzee","orangutan","baboon","kangaroo","koala","rhinoceros","hippopotamus","camel","llama","alpaca","horse","donkey","cow","goat","sheep","pig","rabbit","hare","deer","moose","reindeer","bison","buffalo","wild boar","hedgehog","porcupine","raccoon","otter","beaver","badger","skunk","squirrel","chipmunk","sloth","anteater","armadillo","meerkat","hyena","lemur","tapir","okapi","gazelle","antelope","warthog","capybara","cat","dog","hamster","guinea pig","mouse","rat","bat","seal","walrus","sea lion"],
    "insects": ["monarch butterfly","swallowtail butterfly","blue morpho butterfly","moth","honey bee","bumblebee","wasp","hornet","ladybird beetle","stag beetle","rhinoceros beetle","scarab beetle","firefly","dragonfly","damselfly","grasshopper","cricket insect","katydid","praying mantis","stick insect","leaf insect","ant insect","termite","cockroach","cicada","aphid","weevil","longhorn beetle","click beetle","dung beetle","ground beetle","water beetle","lacewing","mayfly","stonefly","caddisfly","earwig","silverfish","flea","mosquito","house fly","horse fly","hoverfly","robber fly","fruit fly","mantidfly","bee fly","shield bug","stink bug","assassin bug","water strider","leafhopper","planthopper","thrips","booklouse","antlion","dobsonfly","scorpionfly","webspinner","glowworm beetle"],
    "fish": ["clownfish","blue tang fish","yellow tang fish","angelfish","butterflyfish","parrotfish","lionfish","pufferfish","triggerfish","surgeonfish","grouper fish","snapper fish","barracuda","tuna fish","mackerel fish","sardine fish","anchovy fish","salmon fish","trout fish","carp fish","koi fish","goldfish","catfish","tilapia fish","bass fish","perch fish","pike fish","sturgeon fish","eel fish","moray eel","seahorse","pipefish","stingray","manta ray","sawfish","hammerhead shark","great white shark","tiger shark","whale shark","reef shark","dogfish shark","skate fish","halibut","flounder","sole fish","cod fish","haddock fish","hake fish","swordfish","marlin fish","sailfish","mahi mahi","arowana","betta fish","guppy fish","discus fish","cichlid fish","oscar fish","neon tetra","zebrafish"]
}
BLOCKED_LICENSE_TOKENS = ("BY-SA", "NC", "ND")

def clean_html(text):
    if not text: return ""
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", text))).strip()

def safe_name(s):
    return re.sub(r"[^a-z0-9]+", "-", s.lower().strip()).strip("-")

def request_json(params, retries=7):
    params = {**params, "format":"json", "formatversion":2, "maxlag":5}
    url = API + "?" + urllib.parse.urlencode(params)
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent":USER_AGENT,"Accept":"application/json"})
            with urllib.request.urlopen(req, timeout=60) as r:
                data = json.loads(r.read().decode("utf-8"))
            time.sleep(REQUEST_DELAY)
            return data
        except urllib.error.HTTPError as e:
            if e.code not in (429, 503): raise
            retry = e.headers.get("Retry-After")
            wait = float(retry) if retry and retry.isdigit() else min(90, 8 * (2 ** attempt))
            wait += random.uniform(0.5, 2.0)
            print(f"  Wikimedia yogun (HTTP {e.code}); {wait:.0f} sn bekleniyor...")
            time.sleep(wait)
        except Exception:
            if attempt == retries - 1: raise
            time.sleep(min(45, 4 * (2 ** attempt)))
    raise RuntimeError("Wikimedia API tekrar deneme siniri")

def acceptable_license(meta):
    lic = clean_html(meta.get("LicenseShortName", {}).get("value", ""))
    copyrighted = clean_html(meta.get("Copyrighted", {}).get("value", ""))
    if copyrighted.lower() == "false" or "public domain" in lic.lower(): return True, lic or "Public domain"
    upper = lic.upper()
    if any(tok in upper for tok in BLOCKED_LICENSE_TOKENS): return False, lic
    return (upper.startswith("CC0") or upper.startswith("CC BY")), lic

def search_candidates(query):
    data = request_json({"action":"query","generator":"search","gsrsearch":query,"gsrnamespace":6,"gsrlimit":12,"prop":"imageinfo","iiprop":"url|size|mime|extmetadata","iiurlwidth":THUMB_WIDTH,"iiextmetadatafilter":"LicenseShortName|LicenseUrl|Artist|Credit|Attribution|Copyrighted|ImageDescription"})
    return data.get("query", {}).get("pages", [])

def pick_images(species):
    pages = search_candidates(species + " animal photograph")
    picked, seen = [], set()
    for p in pages:
        info = (p.get("imageinfo") or [{}])[0]
        if info.get("mime", "") not in ("image/jpeg","image/png","image/webp"): continue
        if info.get("width",0) < 900 or info.get("height",0) < 600: continue
        meta = info.get("extmetadata") or {}
        ok, lic = acceptable_license(meta)
        if not ok: continue
        url = info.get("thumburl") or info.get("url")
        if not url or url in seen: continue
        seen.add(url)
        picked.append({"title":p.get("title",""),"url":url,"description_url":info.get("descriptionurl",""),"license":lic,"license_url":clean_html(meta.get("LicenseUrl",{}).get("value","")),"artist":clean_html(meta.get("Artist",{}).get("value","")),"credit":clean_html(meta.get("Credit",{}).get("value","")),"attribution":clean_html(meta.get("Attribution",{}).get("value","")),"description":clean_html(meta.get("ImageDescription",{}).get("value",""))})
        if len(picked) >= PER_SPECIES: break
    return picked

def download(url, dest, retries=5):
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent":USER_AGENT})
            with urllib.request.urlopen(req, timeout=90) as r, open(dest,"wb") as f:
                while True:
                    chunk=r.read(1024*512)
                    if not chunk: break
                    f.write(chunk)
            time.sleep(0.8)
            return
        except urllib.error.HTTPError as e:
            if e.code not in (429,503): raise
            wait=min(90,10*(2**attempt))+random.uniform(0.5,2)
            print(f"  indirme yogun (HTTP {e.code}); {wait:.0f} sn bekleniyor...")
            time.sleep(wait)
    raise RuntimeError("indirme tekrar deneme siniri")

def load_old_manifest():
    p=ROOT/"manifest.json"
    if not p.exists(): return []
    try: return json.loads(p.read_text(encoding="utf-8"))
    except Exception: return []

def save_manifest(rows):
    fields=["category","species","file","title","artist","credit","attribution","license","license_url","description_url","description","url"]
    unique={r.get("file"):r for r in rows if r.get("file")}
    rows=list(unique.values())
    with open(ROOT/"ATTRIBUTION.csv","w",newline="",encoding="utf-8-sig") as f:
        w=csv.DictWriter(f,fieldnames=fields); w.writeheader()
        for r in rows: w.writerow({k:r.get(k,"") for k in fields})
    (ROOT/"manifest.json").write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding="utf-8")

def main():
    ROOT.mkdir(parents=True,exist_ok=True)
    manifest=load_old_manifest()
    total=sum(len(v) for v in CATEGORIES.values()); done=0; downloaded=0; skipped=0
    print(f"PostFlow Realistic Nature Library RESUME - {total} tur")
    print("Mevcut fotograflar korunur; sadece eksikler tamamlanir. 429 olursa otomatik bekler.\n")
    for category,species_list in CATEGORIES.items():
        for species in species_list:
            done+=1; slug=safe_name(species); sp=ROOT/category/slug; sp.mkdir(parents=True,exist_ok=True)
            existing=[p for p in sp.iterdir() if p.suffix.lower() in (".jpg",".jpeg",".png",".webp") and p.stat().st_size>20000]
            if len(existing)>=PER_SPECIES:
                skipped+=1; print(f"[{done}/{total}] {category}/{species}: TAMAM ({len(existing)} var)"); continue
            need=PER_SPECIES-len(existing)
            try: picks=pick_images(species)
            except Exception as e:
                print(f"[{done}/{total}] {species}: arama ertelendi: {e}"); continue
            used_urls={r.get("url") for r in manifest if r.get("species")==species}
            picks=[x for x in picks if x.get("url") not in used_urls][:need]
            if not picks:
                print(f"[{done}/{total}] {species}: uygun yeni fotograf bulunamadi"); continue
            start=len(existing)+1
            for offset,item in enumerate(picks):
                ext=".png" if ".png" in item["url"].lower() else ".webp" if ".webp" in item["url"].lower() else ".jpg"
                dest=sp/f"{slug}_{start+offset:02d}{ext}"
                try:
                    download(item["url"],dest); downloaded+=1
                    manifest.append({"category":category,"species":species,"file":str(dest.relative_to(ROOT)),**item})
                    save_manifest(manifest)
                    print(f"[{done}/{total}] {category}/{species}: {dest.name} OK")
                except Exception as e:
                    if dest.exists(): dest.unlink(missing_ok=True)
                    print(f"  indirme ertelendi: {e}")
    save_manifest(manifest)
    count=sum(1 for p in ROOT.rglob("*") if p.is_file() and p.suffix.lower() in (".jpg",".jpeg",".png",".webp"))
    print(f"\nTAMAM. Bu tur yeni: {downloaded} | mevcut/korunan tur: {skipped} | toplam fotograf: {count}")
    print(f"Kutuphane: {ROOT}")
    print("Eksik kalirsa ayni CMD'yi tekrar calistir; kaldigi yerden devam eder.")

if __name__=="__main__": main()
