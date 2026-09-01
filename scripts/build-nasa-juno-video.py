from __future__ import annotations

import asyncio
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(r"D:\PostFlowData\nasa-video")
MEDIA = ROOT / "media"
WORK = ROOT / "work"
OUT = Path(r"D:\PostFlowData\output\nasa")
for p in (MEDIA, WORK, OUT):
    p.mkdir(parents=True, exist_ok=True)

TITLE = "What NASA Found Inside Jupiter"
VOICE = "en-US-GuyNeural"
W, H, FPS = 1920, 1080, 30

SCRIPT = """
Jupiter looks calm from millions of miles away, but NASA's Juno mission revealed a world that is violent, deep, and far more complicated than scientists expected.

Jupiter is the largest planet in our solar system. Its familiar stripes are enormous bands of clouds moving through an atmosphere made mostly of hydrogen and helium.

NASA launched Juno in 2011. After a journey of almost five years, the spacecraft reached Jupiter in July 2016. Juno follows a stretched polar orbit that repeatedly carries it close to the cloud tops and then far away again.

That orbit matters because Jupiter is surrounded by an extremely powerful magnetic field and intense radiation. Juno dives in, collects measurements, and moves back out.

One of Juno's most important jobs is to look beneath the clouds. Its instruments investigate the deep atmosphere, gravity, magnetic fields, auroras, and the hidden structure inside the planet.

JunoCam has also returned spectacular views of turbulent clouds, cyclones, and Jupiter's polar regions. The poles look completely different from the familiar striped view seen from Earth.

The Great Red Spot is more than a colorful mark. It is a gigantic storm observed for centuries, and Juno measurements help scientists understand how far giant storms can extend below the visible clouds.

Jupiter also produces powerful auroras and has an enormous magnetic environment driven by the planet's rapid rotation and interactions with material around its moons.

Why does all of this matter? Jupiter contains more mass than all the other planets in the solar system combined. Understanding how it formed can help scientists reconstruct the earliest stages of our planetary system.

Juno transformed Jupiter from a distant striped sphere into a deep, dynamic world. Every close pass gives scientists another piece of the puzzle, and the largest planet in our solar system still has secrets left to uncover.
""".strip()


def run(cmd):
    print(">", " ".join(map(str, cmd)))
    subprocess.run(cmd, check=True)


def require(name):
    if shutil.which(name) is None:
        raise SystemExit(f"HATA: {name} bulunamadi. FFmpeg PATH ayarini kontrol et.")


def get_json(url, timeout=45):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 PostFlowNASA/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.load(r)


def download(url, dest):
    if dest.exists() and dest.stat().st_size > 500000:
        return True
    tmp = dest.with_suffix(dest.suffix + ".part")
    for attempt in range(1, 4):
        try:
            print("INDIRILIYOR:", url)
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 PostFlowNASA/1.0"})
            with urllib.request.urlopen(req, timeout=60) as r, open(tmp, "wb") as f:
                while True:
                    block = r.read(1024 * 1024)
                    if not block:
                        break
                    f.write(block)
            if tmp.stat().st_size < 200000:
                raise RuntimeError("dosya cok kucuk")
            tmp.replace(dest)
            return True
        except Exception as e:
            print(f"Indirme denemesi {attempt}/3 basarisiz: {e}")
            time.sleep(attempt * 2)
    return False


def discover_nasa_videos():
    # Official NASA Image and Video Library public API.
    query = urllib.parse.urlencode({"q": "Juno Jupiter", "media_type": "video", "page_size": 25})
    data = get_json("https://images-api.nasa.gov/search?" + query)
    items = data.get("collection", {}).get("items", [])
    found = []
    seen = set()
    for item in items:
        meta = (item.get("data") or [{}])[0]
        title = meta.get("title", "NASA Juno Jupiter")
        nasa_id = meta.get("nasa_id", "")
        collection_url = None
        for link in item.get("links", []):
            if link.get("rel") == "preview" and link.get("href"):
                pass
        if item.get("href"):
            collection_url = item["href"]
        if not collection_url:
            continue
        try:
            assets = get_json(collection_url)
        except Exception:
            continue
        mp4s = [x for x in assets if isinstance(x, str) and re.search(r"\.mp4($|\?)", x, re.I)]
        if not mp4s:
            continue
        def rank(u):
            s = u.lower()
            score = 0
            if "~medium" in s or "~small" in s or "~mobile" in s:
                score += 20
            if "~large" in s:
                score += 10
            if "~orig" in s:
                score -= 10
            return score
        mp4s.sort(key=rank, reverse=True)
        url = mp4s[0]
        if url in seen:
            continue
        seen.add(url)
        found.append((nasa_id, title, url))
        if len(found) >= 6:
            break
    return found


async def make_voice():
    try:
        import edge_tts
    except ImportError:
        run([sys.executable, "-m", "pip", "install", "-q", "edge-tts"])
        import edge_tts
    out = WORK / "narration.mp3"
    print("SESLENDIRME HAZIRLANIYOR...")
    c = edge_tts.Communicate(SCRIPT, VOICE, rate="-2%", pitch="-2Hz")
    await c.save(str(out))
    return out


def duration(path):
    text = subprocess.check_output([
        "ffprobe", "-v", "error", "-show_entries", "format=duration",
        "-of", "default=nk=1:nw=1", str(path)
    ], text=True).strip()
    return float(text)


def make_segments(files, target):
    segments = []
    current = 0.0
    i = 0
    while current < target + 0.5:
        src = files[i % len(files)]
        try:
            sd = duration(src)
        except Exception:
            sd = 12.0
        seglen = min(9.0, max(2.0, target - current))
        room = max(0.0, sd - seglen - 0.2)
        start = ((i * 5.7) % room) if room > 0 else 0
        seg = WORK / f"seg_{i:03d}.mp4"
        vf = (
            f"scale={W}:{H}:force_original_aspect_ratio=increase,"
            f"crop={W}:{H},eq=contrast=1.03:saturation=1.08,format=yuv420p"
        )
        run([
            "ffmpeg", "-y", "-loglevel", "error", "-ss", f"{start:.2f}", "-t", f"{seglen:.2f}",
            "-i", str(src), "-an", "-vf", vf, "-r", str(FPS),
            "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", str(seg)
        ])
        segments.append(seg)
        current += seglen
        i += 1
    concat = WORK / "concat.txt"
    concat.write_text("".join(f"file '{p.as_posix()}'\n" for p in segments), encoding="utf-8")
    out = WORK / "visual.mp4"
    run(["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(concat), "-c", "copy", str(out)])
    return out


def render(visual, voice, seconds):
    final = OUT / "WHAT_NASA_FOUND_INSIDE_JUPITER.mp4"
    # Burn only a simple intro title and final source credit for maximum compatibility.
    vf = (
        "drawtext=text='What NASA Found Inside Jupiter':fontcolor=white:fontsize=64:"
        "borderw=4:bordercolor=black@0.7:x=(w-text_w)/2:y=80:enable='between(t,0,6)',"
        "drawtext=text='Visual sources: NASA Image and Video Library':fontcolor=white:fontsize=28:"
        f"borderw=3:bordercolor=black@0.7:x=(w-text_w)/2:y=h-80:enable='gte(t,{max(0, seconds-7):.2f})'"
    )
    run([
        "ffmpeg", "-y", "-loglevel", "error", "-i", str(visual), "-i", str(voice),
        "-vf", vf, "-map", "0:v:0", "-map", "1:a:0", "-c:v", "libx264", "-preset", "veryfast",
        "-crf", "19", "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart", str(final)
    ])
    return final


def main():
    require("ffmpeg")
    require("ffprobe")
    print("NASA resmi video arsivi taraniyor...")
    choices = discover_nasa_videos()
    if len(choices) < 2:
        raise SystemExit("HATA: NASA API yeterli Juno videosu dondurmedi. Internet baglantisini kontrol et.")
    files = []
    credits = []
    for idx, (nasa_id, title, url) in enumerate(choices, 1):
        dest = MEDIA / f"nasa_juno_{idx:02d}.mp4"
        if download(url, dest):
            files.append(dest)
            credits.append(f"{nasa_id} | {title} | {url}")
    if len(files) < 2:
        raise SystemExit("HATA: En az iki NASA videosu indirilemedi.")
    voice = asyncio.run(make_voice())
    seconds = duration(voice)
    print(f"Ses suresi: {seconds:.1f} saniye")
    visual = make_segments(files, seconds)
    final = render(visual, voice, seconds)
    (OUT / "SOURCE_CREDITS.txt").write_text(
        "NASA Image and Video Library assets used in this edit:\n\n" + "\n".join(credits) +
        "\n\nNarration and edit are original for this production. Source clip audio is not used.\n",
        encoding="utf-8"
    )
    print("\nVIDEO HAZIR:", final)
    try:
        os.startfile(str(OUT))
    except Exception:
        pass


if __name__ == "__main__":
    main()
