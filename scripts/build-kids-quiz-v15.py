import asyncio
import importlib.util
import random
from pathlib import Path
from PIL import Image

BASE = Path(__file__).resolve().parent
V14_PATH = BASE / 'build-kids-quiz-v14.py'
ROOT = Path(r'D:\PostFlowData\kids-assets')
REAL_ROOTS = [ROOT / 'realistic-openimages', ROOT / 'realistic']

ALIASES = {
    'lion': ['lion'], 'elephant': ['elephant'], 'panda': ['panda'],
    'giraffe': ['giraffe'], 'monkey': ['monkey', 'macaque', 'baboon']
}

# File-name clues that usually indicate unusable quiz detail shots.
BAD_WORDS = {
    'elephant': ['trunk', 'tusk', 'foot', 'feet', 'skin', 'eye', 'ear', 'detail', 'closeup', 'close-up'],
    'lion': ['paw', 'eye', 'mane detail', 'teeth', 'tooth', 'skin', 'closeup', 'close-up'],
    'panda': ['paw', 'eye', 'fur', 'closeup', 'close-up'],
    'giraffe': ['hoof', 'eye', 'skin', 'pattern', 'closeup', 'close-up'],
    'monkey': ['hand', 'foot', 'eye', 'tail detail', 'closeup', 'close-up'],
}


def load_v14():
    spec = importlib.util.spec_from_file_location('v14', V14_PATH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def photo_score(path, key):
    s = str(path).lower().replace('_', ' ').replace('-', ' ')
    if any(w in s for w in BAD_WORDS.get(key, [])):
        return None
    try:
        with Image.open(path) as im:
            w, h = im.size
            if w < 700 or h < 500:
                return None
            ratio = w / max(h, 1)
            # Extreme strips/panoramas are poor animal cards.
            if ratio > 2.25 or ratio < 0.42:
                return None
            score = min(w, 2600) + min(h, 2200)
            # General full-body / wildlife naming hints.
            good = ['full body', 'fullbody', 'standing', 'walking', 'wildlife', 'animal', 'adult']
            score += sum(900 for g in good if g in s)
            # Favor landscape-ish and balanced photos over detail crops.
            if 0.75 <= ratio <= 1.8:
                score += 700
            return score
    except Exception:
        return None


def smart_find_real(key):
    candidates = []
    for root in REAL_ROOTS:
        if not root.exists():
            continue
        for ext in ('*.jpg', '*.jpeg', '*.png', '*.webp'):
            for p in root.rglob(ext):
                s = str(p).lower()
                if not any(a in s for a in ALIASES.get(key, [key])):
                    continue
                score = photo_score(p, key)
                if score is not None:
                    candidates.append((score, p))
    if not candidates:
        return None
    candidates.sort(key=lambda x: x[0], reverse=True)
    # Randomize only among the best validated candidates, never entire archive.
    top = candidates[:min(8, len(candidates))]
    return random.choice(top)[1]


async def main():
    v14 = load_v14()
    # V14 -> V13 -> V12. Patch V12's reveal selector at load time.
    old_load_v13 = v14.load_v13
    def patched_load_v13():
        v13 = old_load_v13()
        old_load_v12 = v13.load_v12
        def patched_load_v12():
            v12 = old_load_v12()
            v12.find_real = smart_find_real
            return v12
        v13.load_v12 = patched_load_v12
        return v13
    v14.load_v13 = patched_load_v13
    await v14.main()


if __name__ == '__main__':
    asyncio.run(main())
