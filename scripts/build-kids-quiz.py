import asyncio
import math
import random
import shutil
import subprocess
import uuid
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
import edge_tts

ROOT = Path(r"D:\PostFlowData\kids-assets")
OUT = Path(r"D:\PostFlowData\output\final-videos")
NTO = ROOT / "noto"
OUT.mkdir(parents=True, exist_ok=True)

ANIMALS = [
    ("LION", "1f981", "I am the king of the jungle. Who am I?"),
    ("ELEPHANT", "1f418", "I have a long trunk and very big ears. Who am I?"),
    ("PANDA", "1f43c", "I am black and white and I love bamboo. Who am I?"),
    ("GIRAFFE", "1f992", "I have a very long neck. Who am I?"),
    ("MONKEY", "1f412", "I love climbing trees and eating bananas. Who am I?"),
]

PALETTES = [
    ((25, 55, 220), (55, 210, 255)),
    ((125, 35, 220), (245, 80, 190)),
    ((0, 145, 130), (110, 235, 145)),
    ((235, 65, 100), (255, 170, 70)),
    ((255, 135, 20), (255, 220, 65)),
]


def find_ffmpeg():
    exe = shutil.which("ffmpeg")
    if exe:
        return exe
    for base in [Path(r"D:\MoneyPrinterTurbo"), Path(r"D:\PostFlow")]:
        if base.exists():
            for p in base.rglob("ffmpeg.exe"):
                return str(p)
    raise RuntimeError("ffmpeg.exe bulunamadi")


def font(size):
    for p in [
        r"C:\Windows\Fonts\arialbd.ttf",
        r"C:\Windows\Fonts\segoeuib.ttf",
        r"C:\Windows\Fonts\arial.ttf",
    ]:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def gradient(size, c1, c2):
    w, h = size
    img = Image.new("RGB", size)
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    return img


def add_decor(draw, seed):
    rng = random.Random(seed)
    for _ in range(22):
        x = rng.randint(40, 1040)
        y = rng.randint(320, 1810)
        r = rng.randint(8, 28)
        alpha = rng.randint(45, 105)
        color = (255, 255, 255, alpha)
        draw.ellipse((x-r, y-r, x+r, y+r), fill=color)


def wrapped(draw, text, y, f, fill, maxw=820, step=64):
    words, lines, current = text.split(), [], ""
    for word in words:
        candidate = (current + " " + word).strip()
        if current and draw.textbbox((0, 0), candidate, font=f)[2] > maxw:
            lines.append(current)
            current = word
        else:
            current = candidate
    if current:
        lines.append(current)
    for line in lines[:4]:
        draw.text((540, y), line, anchor="mm", font=f, fill=fill)
        y += step


def make_card(icon_path, title, clue, answer, reveal, out_path, idx):
    W, H = 1080, 1920
    c1, c2 = PALETTES[idx % len(PALETTES)]
    image = gradient((W, H), c1, c2).convert("RGBA")
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    add_decor(od, idx + (100 if reveal else 0))
    image = Image.alpha_composite(image, overlay)
    draw = ImageDraw.Draw(image)

    draw.rounded_rectangle((55, 75, 1025, 295), 44, fill=(255, 255, 255, 238))
    draw.text((540, 185), title, anchor="mm", font=font(72), fill=(22, 30, 60))

    icon = Image.open(icon_path).convert("RGBA")
    icon.thumbnail((660, 660))
    if not reveal:
        alpha = icon.getchannel("A")
        sil = Image.new("RGBA", icon.size, (20, 22, 35, 255))
        sil.putalpha(alpha)
        icon = sil
    image.alpha_composite(icon, ((W - icon.width) // 2, 410))
    draw = ImageDraw.Draw(image)

    if reveal:
        draw.rounded_rectangle((105, 1215, 975, 1480), 48, fill=(255, 255, 255, 242))
        draw.text((540, 1340), answer, anchor="mm", font=font(102), fill=(20, 145, 75))
        draw.text((540, 1585), "GREAT JOB!", anchor="mm", font=font(58), fill="white")
        draw.text((540, 1695), "★  ★  ★", anchor="mm", font=font(74), fill=(255, 245, 120))
    else:
        draw.rounded_rectangle((80, 1200, 1000, 1535), 48, fill=(255, 255, 255, 240))
        wrapped(draw, clue, 1300, font(50), (24, 30, 55))
        draw.text((540, 1690), "3   2   1", anchor="mm", font=font(102), fill="white")
        draw.text((540, 1785), "GUESS NOW!", anchor="mm", font=font(52), fill=(255, 245, 125))

    image.convert("RGB").save(out_path, quality=95)


async def speak(text, out_path):
    await edge_tts.Communicate(text, "en-US-AriaNeural", rate="+10%").save(str(out_path))


def make_clip(ffmpeg, image, audio, duration, out_path, reveal=False):
    frames = int(duration * 30)
    if reveal:
        z = "min(1.16,1+0.0028*on)"
    else:
        z = "1.04+0.02*sin(on/8)"
    vf = (
        f"scale=1200:2133,zoompan=z='{z}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':"
        f"d={frames}:s=1080x1920:fps=30,format=yuv420p"
    )
    subprocess.run([
        ffmpeg, "-y", "-loop", "1", "-i", str(image), "-i", str(audio),
        "-t", str(duration), "-vf", vf, "-r", "30", "-c:v", "libx264",
        "-preset", "veryfast", "-c:a", "aac", "-shortest", str(out_path)
    ], check=True)


async def main():
    ffmpeg = find_ffmpeg()
    job = uuid.uuid4().hex[:10]
    work = OUT / f"work_{job}"
    work.mkdir(exist_ok=True)
    clips = []

    intro = work / "intro.png"
    image = gradient((1080, 1920), (45, 40, 210), (40, 210, 255)).convert("RGBA")
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((85, 610, 995, 1120), 55, fill=(255,255,255,235))
    draw.text((540, 760), "GUESS THE ANIMAL!", anchor="mm", font=font(84), fill=(30,35,80))
    draw.text((540, 900), "5 QUESTIONS", anchor="mm", font=font(62), fill=(80,55,190))
    draw.text((540, 1005), "3 SECONDS EACH", anchor="mm", font=font(52), fill=(0,145,180))
    image.convert("RGB").save(intro)

    intro_audio = work / "intro.mp3"
    await speak("Guess the animal! You have three seconds for each question. Ready? Let us go!", intro_audio)
    intro_clip = work / "intro.mp4"
    make_clip(ffmpeg, intro, intro_audio, 4.0, intro_clip)
    clips.append(intro_clip)

    for i, (answer, code, clue) in enumerate(ANIMALS, 1):
        icon = NTO / f"emoji_u{code}.png"
        if not icon.exists():
            raise RuntimeError(f"Eksik asset: {icon}")

        qimg = work / f"q{i}.png"
        aimg = work / f"a{i}.png"
        make_card(icon, f"QUESTION {i}", clue, answer, False, qimg, i - 1)
        make_card(icon, f"ANSWER {i}", clue, answer, True, aimg, i - 1)

        qa = work / f"q{i}.mp3"
        aa = work / f"a{i}.mp3"
        await speak(f"Question {i}. {clue}", qa)
        await speak(f"The answer is {answer.lower()}! Great job!", aa)

        qc = work / f"q{i}.mp4"
        ac = work / f"a{i}.mp4"
        make_clip(ffmpeg, qimg, qa, 6.0, qc, False)
        make_clip(ffmpeg, aimg, aa, 2.8, ac, True)
        clips.extend([qc, ac])

    concat = work / "concat.txt"
    concat.write_text("".join([f"file '{str(p).replace(chr(92), '/')}'\n" for p in clips]), encoding="utf-8")
    output = OUT / f"kids-quiz-v2-{job}.mp4"
    subprocess.run([ffmpeg, "-y", "-f", "concat", "-safe", "0", "-i", str(concat), "-c", "copy", str(output)], check=True)
    print(output)


if __name__ == "__main__":
    asyncio.run(main())
