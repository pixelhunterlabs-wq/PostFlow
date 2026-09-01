import asyncio
import importlib.util
from pathlib import Path
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps

BASE = Path(__file__).resolve().parent
V12_PATH = BASE / 'build-kids-quiz-v12.py'


def load_v12():
    spec = importlib.util.spec_from_file_location('v12', V12_PATH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def rounded_real_photo_fixed(path):
    # Fixed answer card: never hard-crop the animal photo.
    # A softly blurred version fills the card; the original is fitted with CONTAIN
    # so tall/wide animals remain fully visible and centered.
    cw, ch = 706, 646
    iw, ih = 650, 590
    src = Image.open(path).convert('RGB')

    bg = ImageOps.fit(src, (iw, ih), method=Image.Resampling.LANCZOS, centering=(0.5, 0.5))
    bg = bg.filter(ImageFilter.GaussianBlur(22))
    bg = ImageEnhance.Brightness(bg).enhance(0.78)

    fg = src.copy()
    fg.thumbnail((iw - 34, ih - 34), Image.Resampling.LANCZOS)

    canvas = bg.convert('RGBA')
    x = (iw - fg.width) // 2
    y = (ih - fg.height) // 2

    # Soft translucent backing behind contained photo, avoids ugly empty bands.
    backing = Image.new('RGBA', (fg.width + 24, fg.height + 24), (255, 255, 255, 42))
    canvas.alpha_composite(backing, (x - 12, y - 12))
    canvas.alpha_composite(fg.convert('RGBA'), (x, y))

    mask = Image.new('L', (iw, ih), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, iw - 1, ih - 1), 52, fill=255)

    card = Image.new('RGBA', (cw, ch), (0, 0, 0, 0))
    d = ImageDraw.Draw(card, 'RGBA')
    d.rounded_rectangle((0, 0, cw - 1, ch - 1), 66, fill=(255, 255, 255, 245), outline=(255, 221, 70, 255), width=8)
    card.paste(canvas, (28, 28), mask)
    return card


async def main():
    v12 = load_v12()
    v12.rounded_real_photo = rounded_real_photo_fixed
    await v12.main()


if __name__ == '__main__':
    asyncio.run(main())
