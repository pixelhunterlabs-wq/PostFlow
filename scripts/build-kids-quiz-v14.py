import asyncio
import importlib.util
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageOps

BASE = Path(__file__).resolve().parent
V13_PATH = BASE / 'build-kids-quiz-v13.py'
ROOT = Path(r'D:\PostFlowData\kids-assets')
SIL_ROOT = ROOT / 'silhouettes'


def load_v13():
    spec = importlib.util.spec_from_file_location('v13', V13_PATH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def _trim_alpha(im):
    a = im.getchannel('A')
    box = a.getbbox()
    return im.crop(box) if box else im


def _transparent_candidate(key):
    aliases = {
        'lion': ['lion'], 'elephant': ['elephant'], 'panda': ['panda'],
        'giraffe': ['giraffe'], 'monkey': ['monkey', 'macaque', 'baboon']
    }
    roots = [SIL_ROOT, ROOT / 'animals']
    candidates = []
    for root in roots:
        if not root.exists():
            continue
        for ext in ('*.png', '*.webp'):
            for p in root.rglob(ext):
                s = str(p).lower()
                if not any(a in s for a in aliases.get(key, [key])):
                    continue
                try:
                    im = Image.open(p).convert('RGBA')
                    a = im.getchannel('A')
                    bbox = a.getbbox()
                    if not bbox:
                        continue
                    # Prefer true cutouts: transparent corners and useful body area.
                    corners = [a.getpixel((0,0)), a.getpixel((a.width-1,0)), a.getpixel((0,a.height-1)), a.getpixel((a.width-1,a.height-1))]
                    if sum(corners) > 80:
                        continue
                    bw, bh = bbox[2]-bbox[0], bbox[3]-bbox[1]
                    if bw < 180 or bh < 150:
                        continue
                    score = bw * bh
                    candidates.append((score, p))
                except Exception:
                    pass
    if not candidates:
        return None
    candidates.sort(reverse=True)
    return candidates[0][1]


def realistic_silhouette_factory(v12):
    def realistic_silhouette(key):
        # 1) Use a real transparent full-body animal cutout whenever available.
        p = _transparent_candidate(key)
        if p:
            im = _trim_alpha(Image.open(p).convert('RGBA'))
            a = im.getchannel('A')
            # Preserve the actual outer anatomy but make the inside a solid silhouette.
            a = a.filter(ImageFilter.MaxFilter(3)).point(lambda v: 255 if v >= 35 else 0)
            sil = Image.new('RGBA', im.size, (7, 18, 29, 255))
            sil.putalpha(a)
            sil.thumbnail((700, 690), Image.Resampling.LANCZOS)
            return sil

        # 2) Fallback to the cleanest existing transparent animal asset.
        # Never generate ellipse/blob/cartoon procedural shadows.
        return v12.noto_silhouette(key)
    return realistic_silhouette


def make_scene_factory(v12):
    base_factory = v12.make_scene_factory
    original_noto = v12.noto_silhouette
    v12.noto_silhouette = realistic_silhouette_factory(v12)
    return base_factory(v12)


async def main():
    v13 = load_v13()
    # v13 loads v12 internally. Patch the v12 loader so every question uses
    # recognizable full-body contours instead of procedural 2D-looking blobs.
    original_load = v13.load_v12
    def patched_load():
        v12 = original_load()
        v12.noto_silhouette = realistic_silhouette_factory(v12)
        return v12
    v13.load_v12 = patched_load
    await v13.main()


if __name__ == '__main__':
    asyncio.run(main())
