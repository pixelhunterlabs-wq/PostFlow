import asyncio
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


def make_card(icon_path, title, clue, answer, reveal, out_path, idx):
    width, height = 1080, 1920
    colors = [(32, 72, 190), (105, 45, 180), (5, 135, 120), (195, 70, 85), (220, 125, 20)]
    image = Image.new("RGB", (width, height), colors[idx % len(colors)])
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((60, 80, 1020, 290), 42, fill="white")
    draw.text((540, 185), title, anchor="mm", font=font(72), fill=(25, 30, 55))

    icon = Image.open(icon_path).convert("RGBA")
    icon.thumbnail((650, 650))
    if not reveal:
        alpha = icon.getchannel("A")
        silhouette = Image.new("RGBA", icon.size, (20, 20, 28, 255))
        silhouette.putalpha(alpha)
        icon = silhouette
    image.paste(icon, ((width - icon.width) // 2, 430), icon)

    draw = ImageDraw.Draw(image)
    if reveal:
        draw.rounded_rectangle((110, 1230, 970, 1470), 45, fill="white")
        draw.text((540, 1350), answer, anchor="mm", font=font(96), fill=(25, 145, 70))
        draw.text((540, 1570), "GREAT JOB!", anchor="mm", font=font(58), fill="white")
    else:
        draw.rounded_rectangle((85, 1210, 995, 1540), 45, fill="white")
        words = clue.split()
        lines, current = [], ""
        clue_font = font(50)
        for word in words:
            candidate = (current + " " + word).strip()
            if current and draw.textbbox((0, 0), candidate, font=clue_font)[2] > 820:
                lines.append(current)
                current = word
            else:
                current = candidate
        if current:
            lines.append(current)
        y = 1310
        for line in lines[:4]:
            draw.text((540, y), line, anchor="mm", font=clue_font, fill=(25, 30, 55))
            y += 66
        draw.text((540, 1690), "3   2   1", anchor="mm", font=font(100), fill="white")

    image.save(out_path)


async def speak(text, out_path):
    await edge_tts.Communicate(text, "en-US-AriaNeural", rate="+8%").save(str(out_path))


def make_clip(ffmpeg, image, audio, duration, out_path):
    subprocess.run(
        [
            ffmpeg, "-y", "-loop", "1", "-i", str(image), "-i", str(audio),
            "-t", str(duration), "-vf", "scale=1080:1920,format=yuv420p", "-r", "30",
            "-c:v", "libx264", "-preset", "veryfast", "-c:a", "aac", "-shortest", str(out_path),
        ],
        check=True,
    )


async def main():
    ffmpeg = find_ffmpeg()
    job = uuid.uuid4().hex[:10]
    work = OUT / f"work_{job}"
    work.mkdir(exist_ok=True)
    clips = []

    intro_image = work / "intro.png"
    image = Image.new("RGB", (1080, 1920), (45, 65, 190))
    draw = ImageDraw.Draw(image)
    draw.text((540, 760), "GUESS THE ANIMAL!", anchor="mm", font=font(84), fill="white")
    draw.text((540, 910), "5 QUESTIONS - 3 SECONDS", anchor="mm", font=font(50), fill=(255, 240, 110))
    image.save(intro_image)

    intro_audio = work / "intro.mp3"
    await speak("Guess the animal! You have three seconds for each question. Ready? Let us go!", intro_audio)
    intro_clip = work / "intro.mp4"
    make_clip(ffmpeg, intro_image, intro_audio, 4, intro_clip)
    clips.append(intro_clip)

    for i, (answer, code, clue) in enumerate(ANIMALS, 1):
        icon = NTO / f"emoji_u{code}.png"
        if not icon.exists():
            raise RuntimeError(f"Eksik asset: {icon}")
        question_image = work / f"q{i}.png"
        answer_image = work / f"a{i}.png"
        make_card(icon, f"QUESTION {i}", clue, answer, False, question_image, i - 1)
        make_card(icon, f"ANSWER {i}", clue, answer, True, answer_image, i - 1)

        question_audio = work / f"q{i}.mp3"
        answer_audio = work / f"a{i}.mp3"
        await speak(f"Question {i}. {clue}", question_audio)
        await speak(f"The answer is {answer.lower()}!", answer_audio)

        question_clip = work / f"q{i}.mp4"
        answer_clip = work / f"a{i}.mp4"
        make_clip(ffmpeg, question_image, question_audio, 6, question_clip)
        make_clip(ffmpeg, answer_image, answer_audio, 2.5, answer_clip)
        clips.extend([question_clip, answer_clip])

    concat_file = work / "concat.txt"
    concat_file.write_text("".join([f"file '{str(p).replace(chr(92), '/')}'\n" for p in clips]), encoding="utf-8")
    output = OUT / f"kids-quiz-{job}.mp4"
    subprocess.run([ffmpeg, "-y", "-f", "concat", "-safe", "0", "-i", str(concat_file), "-c", "copy", str(output)], check=True)
    print(output)


if __name__ == "__main__":
    asyncio.run(main())
