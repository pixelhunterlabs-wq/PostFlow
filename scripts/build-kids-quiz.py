import asyncio
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
    ((30, 80, 255), (20, 220, 255)),
    ((130, 45, 240), (255, 70, 190)),
    ((0, 160, 125), (120, 240, 125)),
    ((245, 60, 105), (255, 175, 65)),
    ((255, 140, 10), (255, 225, 55)),
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
    draw = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        color = tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(3))
        draw.line((0, y, w, y), fill=color)
    return img


def add_bubbles(draw, seed):
    rng = random.Random(seed)
    for _ in range(28):
        x = rng.randint(30, 1050)
        y = rng.randint(320, 1840)
        r = rng.randint(7, 30)
        a = rng.randint(35, 95)
        draw.ellipse((x-r, y-r, x+r, y+r), fill=(255, 255, 255, a))


def add_confetti(draw, seed):
    rng = random.Random(seed)
    colors = [(255,245,80,255),(255,80,130,255),(70,230,255,255),(100,255,120,255),(255,150,45,255)]
    for _ in range(70):
        x = rng.randint(25, 1055)
        y = rng.randint(300, 1820)
        w = rng.randint(8, 18)
        h = rng.randint(18, 42)
        draw.rounded_rectangle((x, y, x+w, y+h), 4, fill=rng.choice(colors))


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


def base_card(idx, seed=0):
    W, H = 1080, 1920
    c1, c2 = PALETTES[idx % len(PALETTES)]
    image = gradient((W, H), c1, c2).convert("RGBA")
    deco = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    add_bubbles(ImageDraw.Draw(deco), idx * 100 + seed)
    return Image.alpha_composite(image, deco)


def place_icon(image, icon_path, silhouette=False, y=410, size=660):
    icon = Image.open(icon_path).convert("RGBA")
    icon.thumbnail((size, size))
    if silhouette:
        alpha = icon.getchannel("A")
        sil = Image.new("RGBA", icon.size, (20, 22, 35, 255))
        sil.putalpha(alpha)
        icon = sil
    image.alpha_composite(icon, ((1080 - icon.width) // 2, y))


def make_question_card(icon_path, title, clue, out_path, idx):
    image = base_card(idx, 1)
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((55, 75, 1025, 295), 44, fill=(255,255,255,238))
    draw.text((540,185), title, anchor="mm", font=font(72), fill=(22,30,60))
    place_icon(image, icon_path, silhouette=True)
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((80,1190,1000,1515),48,fill=(255,255,255,240))
    wrapped(draw, clue, 1285, font(50), (24,30,55))
    draw.text((540,1650), "GET READY!", anchor="mm", font=font(72), fill="white")
    image.convert("RGB").save(out_path, quality=95)


def make_countdown_card(icon_path, title, clue, number, out_path, idx):
    image = base_card(idx, 10 + number)
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((55,75,1025,295),44,fill=(255,255,255,238))
    draw.text((540,185),title,anchor="mm",font=font(72),fill=(22,30,60))
    place_icon(image, icon_path, silhouette=True, y=395, size=620)
    draw = ImageDraw.Draw(image)
    draw.ellipse((335,1200,745,1610), fill=(255,255,255,242))
    draw.text((540,1405), str(number), anchor="mm", font=font(230), fill=(50,45,150))
    draw.text((540,1720), "GUESS NOW!", anchor="mm", font=font(58), fill=(255,248,130))
    image.convert("RGB").save(out_path, quality=95)


def make_answer_card(icon_path, title, answer, out_path, idx):
    image = base_card(idx, 50)
    conf = Image.new("RGBA", image.size, (0,0,0,0))
    add_confetti(ImageDraw.Draw(conf), idx + 999)
    image = Image.alpha_composite(image, conf)
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((55,75,1025,295),44,fill=(255,255,255,240))
    draw.text((540,185),title,anchor="mm",font=font(72),fill=(22,30,60))
    place_icon(image, icon_path, silhouette=False, y=380, size=700)
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((105,1190,975,1495),52,fill=(255,255,255,245))
    draw.text((540,1330),answer,anchor="mm",font=font(108),fill=(20,150,75))
    draw.text((540,1590),"CORRECT!",anchor="mm",font=font(72),fill="white")
    draw.text((540,1705),"★  ★  ★",anchor="mm",font=font(78),fill=(255,245,105))
    image.convert("RGB").save(out_path, quality=95)


async def speak(text, out_path):
    await edge_tts.Communicate(text, "en-US-AriaNeural", rate="+12%").save(str(out_path))


def make_tone(ffmpeg, out_path, frequency, duration=0.22):
    subprocess.run([
        ffmpeg, "-y", "-f", "lavfi", "-i", f"sine=frequency={frequency}:duration={duration}",
        "-af", "volume=0.18", "-c:a", "aac", str(out_path)
    ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def make_clip(ffmpeg, image, audio, duration, out_path, mode="float"):
    frames = max(1, int(duration * 30))
    if mode == "pop":
        zoom = "min(1.18,1+0.006*on)"
    elif mode == "count":
        zoom = "1.08+0.035*sin(on/3)"
    else:
        zoom = "1.04+0.018*sin(on/7)"
    vf = (
        f"scale=1200:2133,zoompan=z='{zoom}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':"
        f"d={frames}:s=1080x1920:fps=30,format=yuv420p"
    )
    subprocess.run([
        ffmpeg, "-y", "-loop", "1", "-i", str(image), "-i", str(audio),
        "-t", str(duration), "-vf", vf, "-r", "30", "-c:v", "libx264",
        "-preset", "veryfast", "-c:a", "aac", "-ar", "44100", "-ac", "2",
        "-shortest", str(out_path)
    ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


async def main():
    ffmpeg = find_ffmpeg()
    job = uuid.uuid4().hex[:10]
    work = OUT / f"work_v3_{job}"
    work.mkdir(exist_ok=True)
    clips = []

    intro = work / "intro.png"
    image = gradient((1080,1920),(45,40,220),(30,220,255)).convert("RGBA")
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((80,590,1000,1130),60,fill=(255,255,255,238))
    draw.text((540,735),"GUESS THE ANIMAL!",anchor="mm",font=font(86),fill=(35,35,90))
    draw.text((540,885),"5 QUESTIONS",anchor="mm",font=font(64),fill=(100,55,195))
    draw.text((540,1000),"3 SECONDS EACH",anchor="mm",font=font(54),fill=(0,150,185))
    image.convert("RGB").save(intro)
    intro_audio = work / "intro.mp3"
    await speak("Guess the animal! Five questions. Three seconds each. Ready? Let's go!", intro_audio)
    intro_clip = work / "intro.mp4"
    make_clip(ffmpeg, intro, intro_audio, 3.8, intro_clip, "pop")
    clips.append(intro_clip)

    for i, (answer, code, clue) in enumerate(ANIMALS, 1):
        icon = NTO / f"emoji_u{code}.png"
        if not icon.exists():
            raise RuntimeError(f"Eksik asset: {icon}")

        qimg = work / f"q{i}.png"
        make_question_card(icon, f"QUESTION {i}", clue, qimg, i-1)
        qa = work / f"q{i}.mp3"
        await speak(f"Question {i}. {clue}", qa)
        qc = work / f"q{i}.mp4"
        make_clip(ffmpeg, qimg, qa, 3.8, qc, "float")
        clips.append(qc)

        for number, freq in [(3,660),(2,760),(1,900)]:
            cimg = work / f"q{i}_{number}.png"
            caud = work / f"q{i}_{number}.m4a"
            cclip = work / f"q{i}_{number}.mp4"
            make_countdown_card(icon, f"QUESTION {i}", clue, number, cimg, i-1)
            make_tone(ffmpeg, caud, freq, 0.20)
            make_clip(ffmpeg, cimg, caud, 0.78, cclip, "count")
            clips.append(cclip)

        aimg = work / f"a{i}.png"
        make_answer_card(icon, f"ANSWER {i}", answer, aimg, i-1)
        aa = work / f"a{i}.mp3"
        await speak(f"The answer is {answer.lower()}! Correct!", aa)
        ac = work / f"a{i}.mp4"
        make_clip(ffmpeg, aimg, aa, 2.7, ac, "pop")
        clips.append(ac)

    concat = work / "concat.txt"
    concat.write_text("".join([f"file '{str(p).replace(chr(92), '/')}'\n" for p in clips]), encoding="utf-8")
    output = OUT / f"kids-quiz-v3-{job}.mp4"
    subprocess.run([
        ffmpeg, "-y", "-f", "concat", "-safe", "0", "-i", str(concat),
        "-c:v", "libx264", "-preset", "veryfast", "-c:a", "aac", "-ar", "44100", "-ac", "2", str(output)
    ], check=True)
    print(output)


if __name__ == "__main__":
    asyncio.run(main())
