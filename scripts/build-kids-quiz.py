import asyncio
import math
import random
import shutil
import subprocess
import uuid
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import edge_tts

ROOT = Path(r"D:\PostFlowData\kids-assets")
OUT = Path(r"D:\PostFlowData\output\final-videos")
NTO = ROOT / "noto"
OUT.mkdir(parents=True, exist_ok=True)

ANIMALS = [
    ("LION", "1f981", "I am the king of the jungle. Who am I?", "savanna"),
    ("ELEPHANT", "1f418", "I have a long trunk and very big ears. Who am I?", "meadow"),
    ("PANDA", "1f43c", "I am black and white and I love bamboo. Who am I?", "bamboo"),
    ("GIRAFFE", "1f992", "I have a very long neck. Who am I?", "sunset"),
    ("MONKEY", "1f412", "I love climbing trees and eating bananas. Who am I?", "jungle"),
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


def lerp(a, b, t):
    return int(a * (1 - t) + b * t)


def sky_gradient(c1, c2):
    W, H = 1080, 1920
    im = Image.new("RGB", (W, H), c1)
    d = ImageDraw.Draw(im)
    for y in range(H):
        t = y / H
        c = tuple(lerp(c1[i], c2[i], t) for i in range(3))
        d.line((0, y, W, y), fill=c)
    return im


def add_clouds(draw, seed, y0=150, count=5):
    rng = random.Random(seed)
    for _ in range(count):
        x = rng.randint(40, 900)
        y = rng.randint(y0, y0 + 430)
        s = rng.randint(55, 105)
        col = (255, 255, 255, 225)
        draw.ellipse((x, y, x+s*2, y+s), fill=col)
        draw.ellipse((x+s//2, y-s//2, x+s*3//2, y+s), fill=col)
        draw.ellipse((x+s, y, x+s*3, y+s), fill=col)


def add_hills(draw, palette):
    W, H = 1080, 1920
    draw.ellipse((-450, 760, 900, 2050), fill=palette[0])
    draw.ellipse((280, 830, 1530, 2050), fill=palette[1])
    draw.rectangle((0, 1280, W, H), fill=palette[2])


def add_tree(draw, x, y, scale=1.0, trunk=(126, 78, 42), leaf=(57, 160, 73)):
    tw = int(38*scale)
    th = int(180*scale)
    draw.rounded_rectangle((x-tw//2, y-th, x+tw//2, y), 16, fill=trunk)
    r = int(90*scale)
    for ox, oy in [(-55,-170),(20,-190),(-10,-240),(65,-145)]:
        cx = x + int(ox*scale)
        cy = y + int(oy*scale)
        draw.ellipse((cx-r, cy-r, cx+r, cy+r), fill=leaf)


def add_flowers(draw, seed, y0=1380, count=22):
    rng = random.Random(seed)
    cols = [(255,110,150),(255,220,70),(120,200,255),(255,255,255),(180,120,255)]
    for _ in range(count):
        x = rng.randint(40, 1040)
        y = rng.randint(y0, 1840)
        c = rng.choice(cols)
        r = rng.randint(7, 14)
        draw.line((x, y, x, y+35), fill=(70,140,60), width=4)
        draw.ellipse((x-r, y-r, x+r, y+r), fill=c)
        draw.ellipse((x-r*2, y, x, y+r*2), fill=c)
        draw.ellipse((x, y, x+r*2, y+r*2), fill=c)
        draw.ellipse((x-r*2, y-r*2, x, y), fill=c)
        draw.ellipse((x, y-r*2, x+r*2, y), fill=c)


def add_bamboo(draw):
    for x in [70, 160, 900, 990]:
        draw.rounded_rectangle((x, 250, x+28, 1640), 12, fill=(68,140,55))
        for y in range(330, 1600, 160):
            draw.line((x+14, y, x+95, y-45), fill=(62,130,48), width=12)
            draw.ellipse((x+70, y-90, x+150, y-30), fill=(88,170,72))


def add_vines(draw):
    for x in [90, 980]:
        draw.arc((x-130, 80, x+80, 680), 70, 290, fill=(45,130,60), width=16)
    for i in range(9):
        y = 150 + i*150
        draw.ellipse((30, y, 110, y+40), fill=(65,165,80))
        draw.ellipse((950, y+30, 1035, y+70), fill=(65,165,80))


def scene_background(theme, seed):
    if theme == "sunset":
        im = sky_gradient((255,160,80), (105,55,190))
        d = ImageDraw.Draw(im, "RGBA")
        d.ellipse((760, 130, 980, 350), fill=(255,235,135,255))
        add_hills(d, ((106,177,78,255),(82,150,70,255),(72,136,62,255)))
        add_tree(d, 120, 1490, 1.25, leaf=(64,125,63))
        add_tree(d, 940, 1510, 1.4, leaf=(64,125,63))
    elif theme == "bamboo":
        im = sky_gradient((130,220,190), (50,155,110))
        d = ImageDraw.Draw(im, "RGBA")
        add_clouds(d, seed, 100, 4)
        add_hills(d, ((95,185,105,255),(70,160,90,255),(57,145,76,255)))
        add_bamboo(d)
    elif theme == "jungle":
        im = sky_gradient((75,190,205), (28,105,90))
        d = ImageDraw.Draw(im, "RGBA")
        add_hills(d, ((50,155,90,255),(39,125,76,255),(29,105,65,255)))
        add_tree(d, 170, 1530, 1.6, leaf=(36,130,63))
        add_tree(d, 900, 1520, 1.55, leaf=(36,130,63))
        add_vines(d)
    elif theme == "savanna":
        im = sky_gradient((80,190,255), (255,205,100))
        d = ImageDraw.Draw(im, "RGBA")
        add_clouds(d, seed, 120, 4)
        d.ellipse((800, 120, 990, 310), fill=(255,226,96,255))
        add_hills(d, ((183,196,92,255),(148,176,78,255),(121,155,63,255)))
        add_tree(d, 160, 1510, 1.15, leaf=(87,135,59))
        add_tree(d, 930, 1500, 1.0, leaf=(87,135,59))
    else:
        im = sky_gradient((85,195,255), (175,235,255))
        d = ImageDraw.Draw(im, "RGBA")
        add_clouds(d, seed, 100, 5)
        d.ellipse((820, 100, 1000, 280), fill=(255,230,90,255))
        add_hills(d, ((115,198,96,255),(87,176,83,255),(70,158,72,255)))
        add_tree(d, 120, 1510, 1.2)
        add_tree(d, 940, 1500, 1.25)
        add_flowers(d, seed)

    # soft depth haze and floating sparkles
    overlay = Image.new("RGBA", im.size, (0,0,0,0))
    od = ImageDraw.Draw(overlay)
    rng = random.Random(seed+1000)
    for _ in range(24):
        x=rng.randint(60,1020); y=rng.randint(260,1700); r=rng.randint(4,12)
        od.ellipse((x-r,y-r,x+r,y+r), fill=(255,255,255,rng.randint(45,110)))
    im = Image.alpha_composite(im.convert("RGBA"), overlay)
    return im


def wood_panel(draw, box, text, size=66):
    x1,y1,x2,y2 = box
    draw.rounded_rectangle(box, 38, fill=(123,73,40,255), outline=(255,211,124,255), width=7)
    draw.rounded_rectangle((x1+18,y1+18,x2-18,y2-18), 30, fill=(173,107,58,255))
    for yy in range(y1+50, y2-20, 62):
        draw.line((x1+35,yy,x2-35,yy), fill=(140,82,46,140), width=3)
    draw.text(((x1+x2)//2,(y1+y2)//2), text, anchor="mm", font=font(size), fill="white", stroke_width=3, stroke_fill=(90,45,20))


def wrap_text(draw, text, y, f, fill, maxw=820, step=60):
    words=text.split(); lines=[]; cur=""
    for w in words:
        t=(cur+" "+w).strip()
        if cur and draw.textbbox((0,0),t,font=f)[2] > maxw:
            lines.append(cur); cur=w
        else:
            cur=t
    if cur: lines.append(cur)
    for line in lines[:3]:
        draw.text((540,y),line,anchor="mm",font=f,fill=fill,stroke_width=2,stroke_fill=(255,255,255,180))
        y += step


def make_scene(icon_path, answer, clue, theme, idx, reveal=False):
    im = scene_background(theme, idx+20).convert("RGBA")
    d = ImageDraw.Draw(im, "RGBA")
    wood_panel(d, (90,80,990,285), f"QUESTION {idx+1}" if not reveal else "CORRECT!", 68)

    # center glass card
    d.rounded_rectangle((95,360,985,1290), 58, fill=(255,255,255,72), outline=(255,255,255,150), width=5)

    icon = Image.open(icon_path).convert("RGBA")
    icon.thumbnail((610,610))
    if not reveal:
        a=icon.getchannel("A")
        sil=Image.new("RGBA",icon.size,(18,25,35,255)); sil.putalpha(a); icon=sil
    # shadow
    shadow = Image.new("RGBA", icon.size, (0,0,0,0))
    sa = icon.getchannel("A").filter(ImageFilter.GaussianBlur(18))
    sh = Image.new("RGBA", icon.size, (0,0,0,95)); sh.putalpha(sa)
    x=(1080-icon.width)//2; y=500
    im.alpha_composite(sh,(x+18,y+30))
    im.alpha_composite(icon,(x,y))
    d = ImageDraw.Draw(im, "RGBA")

    if reveal:
        d.rounded_rectangle((155,1330,925,1515), 50, fill=(255,255,255,235), outline=(255,226,130,255), width=7)
        d.text((540,1422),answer,anchor="mm",font=font(88),fill=(34,135,73),stroke_width=2,stroke_fill="white")
        d.text((540,1605),"AMAZING!",anchor="mm",font=font(58),fill="white",stroke_width=3,stroke_fill=(58,75,110))
        # confetti / stars
        rng=random.Random(900+idx)
        cols=[(255,80,120,255),(255,220,70,255),(80,210,255,255),(155,95,255,255),(80,225,120,255)]
        for _ in range(55):
            cx=rng.randint(70,1010); cy=rng.randint(320,1770); s=rng.randint(8,22); c=rng.choice(cols)
            if rng.random()<0.5:
                d.ellipse((cx-s,cy-s,cx+s,cy+s),fill=c)
            else:
                d.rounded_rectangle((cx-s,cy-s,cx+s,cy+s),4,fill=c)
    else:
        d.rounded_rectangle((120,1320,960,1515), 50, fill=(255,255,255,235), outline=(255,255,255,190), width=5)
        wrap_text(d, clue, 1380, font(46), (38,44,70))
        d.text((540,1615),"GUESS BEFORE ENERGY RUNS OUT!",anchor="mm",font=font(42),fill="white",stroke_width=3,stroke_fill=(40,75,90))
        # energy bar frame only; fill is animated by ffmpeg
        d.rounded_rectangle((130,1710,950,1795), 38, fill=(25,45,48,220), outline=(255,255,255,230), width=6)

    return im.convert("RGB")


async def speak(text, out_path):
    await edge_tts.Communicate(text, "en-US-AriaNeural", rate="+7%").save(str(out_path))


def make_question_clip(ffmpeg, image_path, audio_path, out_path, duration=5.0):
    # smooth 5-second energy drain from full green to empty, with gentle camera motion
    filt = (
        "scale=1188:2112,"
        "zoompan=z='1.035+0.012*sin(on/18)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=150:s=1080x1920:fps=30,"
        "drawbox=x=148:y=1728:w='max(0,784*(1-t/5))':h=49:color=0x4BE26D@1:t=fill,"
        "drawbox=x=148:y=1728:w='max(0,784*(1-t/5))':h=14:color=0x9CFFB0@0.95:t=fill,"
        "format=yuv420p"
    )
    subprocess.run([
        ffmpeg,"-y","-loop","1","-i",str(image_path),"-i",str(audio_path),"-t",str(duration),
        "-vf",filt,"-r","30","-c:v","libx264","-preset","veryfast","-c:a","aac","-shortest",str(out_path)
    ],check=True)


def make_reveal_clip(ffmpeg, image_path, audio_path, out_path, duration=3.0):
    filt=(
        "scale=1188:2112,"
        "zoompan=z='min(1.16,1+0.0035*on)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=90:s=1080x1920:fps=30,"
        "format=yuv420p"
    )
    subprocess.run([
        ffmpeg,"-y","-loop","1","-i",str(image_path),"-i",str(audio_path),"-t",str(duration),
        "-vf",filt,"-r","30","-c:v","libx264","-preset","veryfast","-c:a","aac","-shortest",str(out_path)
    ],check=True)


async def main():
    ffmpeg=find_ffmpeg()
    job=uuid.uuid4().hex[:10]
    work=OUT/f"work_{job}"; work.mkdir(exist_ok=True)
    clips=[]

    # intro uses the first rich environment, not a flat background
    intro = scene_background("meadow", 77).convert("RGBA")
    d=ImageDraw.Draw(intro,"RGBA")
    wood_panel(d,(95,600,985,1050),"GUESS THE ANIMAL!",72)
    d.text((540,1140),"5 QUESTIONS",anchor="mm",font=font(60),fill="white",stroke_width=3,stroke_fill=(50,80,90))
    d.text((540,1235),"5 SECOND ENERGY BAR",anchor="mm",font=font(44),fill=(255,245,120),stroke_width=2,stroke_fill=(50,80,90))
    intro_path=work/"intro.png"; intro.convert("RGB").save(intro_path)
    intro_audio=work/"intro.mp3"; await speak("Guess the animal! Five questions, and five seconds for each one. Ready?",intro_audio)
    intro_clip=work/"intro.mp4"; make_reveal_clip(ffmpeg,intro_path,intro_audio,intro_clip,4.0); clips.append(intro_clip)

    for i,(answer,code,clue,theme) in enumerate(ANIMALS):
        icon=NTO/f"emoji_u{code}.png"
        if not icon.exists(): raise RuntimeError(f"Eksik asset: {icon}")

        qimg=work/f"q{i}.png"; aimg=work/f"a{i}.png"
        make_scene(icon,answer,clue,theme,i,False).save(qimg,quality=95)
        make_scene(icon,answer,clue,theme,i,True).save(aimg,quality=95)

        qa=work/f"q{i}.mp3"; aa=work/f"a{i}.mp3"
        await speak(f"Question {i+1}. {clue}",qa)
        await speak(f"The answer is {answer.lower()}! Amazing!",aa)

        qc=work/f"q{i}.mp4"; ac=work/f"a{i}.mp4"
        make_question_clip(ffmpeg,qimg,qa,qc,5.0)
        make_reveal_clip(ffmpeg,aimg,aa,ac,3.0)
        clips.extend([qc,ac])

    concat=work/"concat.txt"
    concat.write_text("".join([f"file '{str(p).replace(chr(92), '/')}'\n" for p in clips]),encoding="utf-8")
    output=OUT/f"kids-quiz-v5-{job}.mp4"
    subprocess.run([ffmpeg,"-y","-f","concat","-safe","0","-i",str(concat),"-c","copy",str(output)],check=True)
    print(output)


if __name__=="__main__":
    asyncio.run(main())
