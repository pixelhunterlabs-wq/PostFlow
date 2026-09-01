import asyncio
import importlib.util
import json
from pathlib import Path

from PIL import Image

BASE = Path(__file__).resolve().parent
V14_PATH = BASE / 'build-kids-quiz-v14.py'
ROOT = Path(r'D:\PostFlowData\kids-assets')
REAL_ROOTS = [ROOT / 'realistic-openimages', ROOT / 'realistic']
CACHE_FILE = ROOT / 'approved-real-images-v16.json'

ALIASES = {
    'lion': ['lion'],
    'elephant': ['elephant'],
    'panda': ['panda'],
    'giraffe': ['giraffe'],
    'monkey': ['monkey', 'macaque', 'baboon'],
}


def load_v14():
    spec = importlib.util.spec_from_file_location('v14', V14_PATH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def load_clip():
    try:
        from transformers import CLIPModel, CLIPProcessor
        import torch
    except Exception as e:
        raise RuntimeError('CLIP eksik. Once V16 kurulum CMD dosyasini calistir.') from e
    model_name = 'openai/clip-vit-base-patch32'
    processor = CLIPProcessor.from_pretrained(model_name)
    model = CLIPModel.from_pretrained(model_name)
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    model = model.to(device)
    model.eval()
    return model, processor, torch, device


def candidate_paths(key):
    out = []
    for root in REAL_ROOTS:
        if not root.exists():
            continue
        for ext in ('*.jpg', '*.jpeg', '*.png', '*.webp'):
            for p in root.rglob(ext):
                s = str(p).lower()
                if not any(a in s for a in ALIASES.get(key, [key])):
                    continue
                try:
                    with Image.open(p) as im:
                        w, h = im.size
                        if w >= 650 and h >= 450 and 0.40 <= (w / max(h, 1)) <= 2.4:
                            out.append(p)
                except Exception:
                    pass
    return out


def vision_score(model, processor, torch, device, path, key):
    image = Image.open(path).convert('RGB')
    labels = [
        f'a clear photograph of a complete {key}, full body visible, head torso and legs visible',
        f'a wildlife photograph showing most of a {key} body',
        f'a close up detail photograph of only part of a {key}',
        f'a photograph of only the head face eye foot skin trunk tail or body detail of a {key}',
        f'a photograph where the {key} is badly cropped or mostly outside the frame',
    ]
    inputs = processor(text=labels, images=image, return_tensors='pt', padding=True)
    inputs = {k: v.to(device) for k, v in inputs.items()}
    with torch.no_grad():
        outputs = model(**inputs)
        probs = outputs.logits_per_image.softmax(dim=1)[0].detach().cpu().tolist()
    full = probs[0] + 0.70 * probs[1]
    bad = probs[2] + probs[3] + probs[4]
    return full - 1.20 * bad, probs


def build_approved_map():
    if CACHE_FILE.exists():
        try:
            data = json.loads(CACHE_FILE.read_text(encoding='utf-8'))
            if all(Path(v).exists() for v in data.values()):
                return data
        except Exception:
            pass

    model, processor, torch, device = load_clip()
    approved = {}
    for key in ALIASES:
        candidates = candidate_paths(key)
        if not candidates:
            continue
        scored = []
        for p in candidates[:120]:
            try:
                score, probs = vision_score(model, processor, torch, device, p, key)
                scored.append((score, p, probs))
            except Exception:
                pass
        scored.sort(key=lambda x: x[0], reverse=True)
        if scored:
            # Do not accept obviously uncertain images. If none pass, V12 will use its safe fallback.
            best_score, best_path, _ = scored[0]
            if best_score > -0.05:
                approved[key] = str(best_path)
                print(f'[V16] {key}: {best_path.name} score={best_score:.3f}')
            else:
                print(f'[V16] {key}: uygun tam-hayvan fotografi bulunamadi; fallback kullanilacak')

    CACHE_FILE.write_text(json.dumps(approved, ensure_ascii=False, indent=2), encoding='utf-8')
    return approved


def main_find_real_factory(approved):
    def find_real(key):
        p = approved.get(key)
        return Path(p) if p and Path(p).exists() else None
    return find_real


async def main():
    approved = build_approved_map()
    v14 = load_v14()
    old_load_v13 = v14.load_v13

    def patched_load_v13():
        v13 = old_load_v13()
        old_load_v12 = v13.load_v12

        def patched_load_v12():
            v12 = old_load_v12()
            v12.find_real = main_find_real_factory(approved)
            return v12

        v13.load_v12 = patched_load_v12
        return v13

    v14.load_v13 = patched_load_v13
    await v14.main()


if __name__ == '__main__':
    asyncio.run(main())
