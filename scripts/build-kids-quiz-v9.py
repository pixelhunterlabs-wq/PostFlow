import asyncio
import importlib.util
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps

BASE = Path(__file__).resolve().parent
V8_PATH = BASE / "build-kids-quiz-v8.py"
ROOT = Path(r"D:\PostFlowData\kids-assets")
REAL_ROOTS = [
    ROOT / "realistic-openimages",
    ROOT / "realistic",
]


def load_v8():
    spec = importlib.util.spec_from_file_location("quiz_v8", V8_PATH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def real_candidates(keyword):
    key = keyword.lower().replace("_", " ").replace("-", " ")
    out = []
    for root in REAL_ROOTS:
        if not root.exists():
            continue
        for ext in ("*.jpg", "*.jpeg", "*.png", "*.webp"):
            for p in root.rglob(ext):
                text = (p.stem + " " + p.parent.name).lower().replace("_", " ").replace("-", " ")
                if key in text:
                    try:
                        with Image.open(p) as im:
                            if im.width >= 500 and im.height >= 350:
                                score = im.width * im.height
                                if root.name == "realistic-openimages":
                                    score += 500000
                                out.append((score, p))
                    except Exception:
                        pass
    out.sort(reverse=True, key=lambda x: x[0])
    return [p for _, p in out]


def pick_real(keyword, idx=0):
    arr = real_candidates(keyword)
    if not arr:
        return None
    rng = random.Random(9100 + idx)
    top = arr[: min(12, len(arr))]
    return rng.choice(top)


def rounded_photo(path, size=(780, 780), radius=54):
    with Image.open(path) as src:
        src = ImageOps.exif_transpose(src).convert("RGB")
        src = ImageOps.fit(src, size, method=Image.Resampling.LANCZOS, centering=(0.5, 0.48))
        src = ImageEnhance.Color(src).enhance(1.08)
        src = ImageEnhance.Contrast(src).enhance(1.04)
        rgba = src.convert("RGBA")
    mask = Image.new("L", size, 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((0, 0, size[0]-1, size[1]-1), radius, fill=255)
    rgba.putalpha(mask)
    return rgba


def real_reveal(v7, bg_path, answer, keyword, idx):
    im = v7.fit_background(bg_path)
    veil = Image.new("RGBA", (v7.W, v7.H), (0, 0, 0, 0))
    vd = ImageDraw.Draw(veil, "RGBA")
    vd.rectangle((0, 0, v7.W, v7.H), fill=(10, 30, 45, 48))
    im = Image.alpha_composite(im, veil)
    d = ImageDraw.Draw(im, "RGBA")

    v7.wood_panel(d, "CORRECT!")

    # Large polished answer card
    v7.soft_panel(d, (85, 345, 995, 1335), fill=(255,255,255,225), outline=(255,235,145,255), radius=62, width=7)

    photo_path = pick_real(keyword, idx)
    if photo_path:
        photo = rounded_photo(photo_path, (780, 780), 58)
        # soft shadow behind photo
        shadow = Image.new("RGBA", photo.size, (0,0,0,0))
        sa = photo.getchannel("A").filter(ImageFilter.GaussianBlur(20))
        shadow.putalpha(sa)
        shfill = Image.new("RGBA", photo.size, (0,0,0,95))
        shfill.putalpha(sa)
        im.alpha_composite(shfill, (166, 468))
        im.alpha_composite(photo, (150, 440))
    else:
        # fallback to existing transparent quiz asset
        p = v7.choose_animal(keyword, "1f981")
        animal = v7.prepare_animal(p, (650, 720))
        x = (v7.W - animal.width)//2
        im.alpha_composite(animal, (x, 500))

    d = ImageDraw.Draw(im, "RGBA")
    # Answer label
    v7.soft_panel(d, (135, 1375, 945, 1585), fill=(255,255,255,242), outline=(255,218,70,255), radius=52, width=6)
    d.text((540, 1480), answer, anchor="mm", font=v7.font(92), fill=(28, 142, 74), stroke_width=3, stroke_fill="white")
    d.text((540, 1670), "GREAT JOB!", anchor="mm", font=v7.font(58), fill="white", stroke_width=4, stroke_fill=(38,70,96))
    d.text((540, 1760), "★  ★  ★", anchor="mm", font=v7.font(62), fill=(255,225,70), stroke_width=2, stroke_fill=(80,55,20))
    v7.side_particles(d, 12000 + idx)
    return im.convert("RGB")


async def main():
    v8 = load_v8()
    v7 = v8.load_v7()

    original_make_scene = v7.make_scene

    def make_scene(bg_path, animal_path, answer, clue, idx, reveal=False):
        if not reveal:
            return original_make_scene(bg_path, animal_path, answer, clue, idx, False)
        keyword = answer.lower()
        return real_reveal(v7, bg_path, answer, keyword, idx)

    v7.make_scene = make_scene
    v7.question_clip = v8.question_clip
    await v7.main()


if __name__ == "__main__":
    asyncio.run(main())
