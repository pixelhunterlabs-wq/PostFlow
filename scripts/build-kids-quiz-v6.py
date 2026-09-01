import asyncio
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

W, H = 1080, 1920
FPS = 30
QUESTION_SECONDS = 5.0

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
    for p in [r"C:\Windows\Fonts\arialbd.ttf", r"C:\Windows\Fonts\segoeuib.ttf", r"C:\Windows\Fonts\arial.ttf"]:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def lerp(a, b, t):
    return int(a * (1 - t) + b * t)


def gradient(c1, c2):
    im = Image.new("RGB", (W, H), c1)
    d = ImageDraw.Draw(im)
    for y in range(H):
        t = y / H
        d.line((0, y, W, y), fill=tuple(lerp(c1[i], c2[i], t) for i in range(3)))
    return im


def cloud(d, x, y, s):
    col = (255, 255, 255, 225)
    d.ellipse((x, y, x+s*2, y+s), fill=col)
    d.ellipse((x+s//2, y-s//2, x+s*3//2, y+s), fill=col)
    d.ellipse((x+s, y, x+s*3, y+s), fill=col)


def tree(d, x, y, scale=1.0, leaf=(55,155,70)):
    d.rounded_rectangle((x-int(22*scale), y-int(190*scale), x+int(22*scale), y), 16, fill=(128,77,40))
    r = int(88*scale)
    for ox, oy in [(-55,-170),(20,-195),(-5,-250),(65,-145)]:
        cx=x+int(ox*scale); cy=y+int(oy*scale)
        d.ellipse((cx-r,cy-r,cx+r,cy+r), fill=leaf)


def background(theme, seed):
    rng = random.Random(seed)
    palettes = {
        "savanna": ((72,188,255),(255,205,100),((185,198,92),(151,178,78),(125,158,63))),
        "meadow": ((78,195,255),(185,238,255),((118,205,100),(88,180,84),(69,158,72))),
        "bamboo": ((128,224,194),(52,157,112),((97,187,108),(72,160,91),(57,143,76))),
        "sunset": ((255,161,79),(102,55,190),((108,177,78),(83,151,70),(72,136,62))),
        "jungle": ((72,191,207),(25,105,89),((50,157,91),(39,127,77),(28,105,66))),
    }
    c1,c2,hills = palettes[theme]
    im=gradient(c1,c2).convert("RGBA")
    d=ImageDraw.Draw(im,"RGBA")
    for _ in range(5): cloud(d,rng.randint(20,850),rng.randint(90,430),rng.randint(55,95))
    if theme in ("savanna","meadow"):
        d.ellipse((815,105,1000,290), fill=(255,231,100,255))
    elif theme=="sunset":
        d.ellipse((790,120,990,320), fill=(255,236,145,255))
    d.ellipse((-460,800,900,2070), fill=hills[0]+(255,))
    d.ellipse((300,850,1550,2070), fill=hills[1]+(255,))
    d.rectangle((0,1280,W,H), fill=hills[2]+(255,))
    if theme=="bamboo":
        for x in [60,145,910,995]:
            d.rounded_rectangle((x,260,x+30,1640),14,fill=(66,140,54,255))
            for yy in range(360,1550,170):
                d.line((x+15,yy,x+100,yy-50),fill=(62,130,48,255),width=12)
                d.ellipse((x+68,yy-95,x+155,yy-35),fill=(88,174,74,255))
    else:
        tree(d,125,1520,1.25, leaf=(50,145,65))
        tree(d,945,1510,1.3, leaf=(50,145,65))
    if theme=="jungle":
        for xx in [45,950]:
            for yy in range(220,1500,150):
                d.ellipse((xx,yy,xx+85,yy+40), fill=(65,166,81,255))
    # depth sparkles stay behind UI/animal
    for _ in range(18):
        x=rng.randint(50,1030); y=rng.randint(300,1650); r=rng.randint(3,8)
        d.ellipse((x-r,y-r,x+r,y+r), fill=(255,255,255,rng.randint(35,80)))
    return im


def wood_panel(d, box, text, size=68):
    x1,y1,x2,y2=box
    d.rounded_rectangle(box,38,fill=(120,70,38,255),outline=(255,216,133,255),width=7)
    d.rounded_rectangle((x1+18,y1+18,x2-18,y2-18),30,fill=(174,108,58,255))
    for yy in range(y1+50,y2-20,60): d.line((x1+36,yy,x2-36,yy),fill=(137,78,43,130),width=3)
    d.text(((x1+x2)//2,(y1+y2)//2),text,anchor="mm",font=font(size),fill="white",stroke_width=3,stroke_fill=(86,43,20))


def wrap(d, text, y):
    f=font(46); words=text.split(); lines=[]; cur=""
    for word in words:
        test=(cur+" "+word).strip()
        if cur and d.textbbox((0,0),test,font=f)[2]>800:
            lines.append(cur); cur=word
        else: cur=test
    if cur: lines.append(cur)
    for line in lines[:3]:
        d.text((540,y),line,anchor="mm",font=f,fill=(35,45,70),stroke_width=2,stroke_fill=(255,255,255,180)); y+=58


def add_side_confetti(d, seed):
    rng=random.Random(seed)
    cols=[(255,80,120,255),(255,220,70,255),(80,210,255,255),(155,95,255,255),(80,225,120,255)]
    # Effects deliberately stay around the edges; never cover the animal/answer.
    zones=[(35,185,320,1650),(895,1045,320,1650),(180,900,300,430)]
    for _ in range(42):
        x1,x2,y1,y2=rng.choice(zones); x=rng.randint(x1,x2); y=rng.randint(y1,y2); s=rng.randint(7,16); c=rng.choice(cols)
        if rng.random()<0.5: d.ellipse((x-s,y-s,x+s,y+s),fill=c)
        else: d.rounded_rectangle((x-s,y-s,x+s,y+s),4,fill=c)


def make_scene(icon_path, answer, clue, theme, idx, reveal=False):
    im=background(theme,20+idx).convert("RGBA")
    d=ImageDraw.Draw(im,"RGBA")
    wood_panel(d,(90,80,990,285),"CORRECT!" if reveal else f"QUESTION {idx+1}")
    d.rounded_rectangle((95,360,985,1280),58,fill=(255,255,255,75),outline=(255,255,255,155),width=5)
    if reveal:
        add_side_confetti(d,900+idx)
    icon=Image.open(icon_path).convert("RGBA"); icon.thumbnail((610,610))
    if not reveal:
        a=icon.getchannel("A"); sil=Image.new("RGBA",icon.size,(18,25,35,255)); sil.putalpha(a); icon=sil
    a=icon.getchannel("A").filter(ImageFilter.GaussianBlur(18)); sh=Image.new("RGBA",icon.size,(0,0,0,95)); sh.putalpha(a)
    x=(W-icon.width)//2; y=500
    im.alpha_composite(sh,(x+16,y+28)); im.alpha_composite(icon,(x,y))
    d=ImageDraw.Draw(im,"RGBA")
    if reveal:
        d.rounded_rectangle((155,1320,925,1515),50,fill=(255,255,255,238),outline=(255,226,130,255),width=7)
        d.text((540,1418),answer,anchor="mm",font=font(88),fill=(34,135,73),stroke_width=2,stroke_fill="white")
        d.text((540,1605),"AMAZING!",anchor="mm",font=font(58),fill="white",stroke_width=3,stroke_fill=(58,75,110))
    else:
        d.rounded_rectangle((120,1310,960,1510),50,fill=(255,255,255,238),outline=(255,255,255,190),width=5)
        wrap(d,clue,1370)
        d.text((540,1605),"GUESS BEFORE ENERGY RUNS OUT!",anchor="mm",font=font(40),fill="white",stroke_width=3,stroke_fill=(40,75,90))
        # perfectly centered timer shell. Inner area is x=170..910 (740 px)
        d.rounded_rectangle((140,1690,940,1805),50,fill=(24,39,45,235),outline=(255,255,255,245),width=7)
        d.text((540,1748),"",anchor="mm",font=font(1),fill="white")
    return im.convert("RGB")


async def speak(text,out_path):
    await edge_tts.Communicate(text,"en-US-AriaNeural",rate="+7%").save(str(out_path))


def make_question_clip(ffmpeg,image_path,audio_path,out_path,duration=QUESTION_SECONDS):
    # Use time-gated steps instead of drawbox width expressions. This is deterministic on Windows FFmpeg.
    parts=[]
    steps=20
    inner_x=170; inner_y=1718; inner_w=740; inner_h=59
    for i in range(steps):
        start=i*(duration/steps); end=(i+1)*(duration/steps)
        remain=1-(i/steps)
        w=max(1,int(inner_w*remain))
        # fill remains left-aligned inside a centered fixed shell, visibly draining toward the left
        parts.append(f"drawbox=x={inner_x}:y={inner_y}:w={w}:h={inner_h}:color=0x49E66B@1:t=fill:enable='between(t,{start:.3f},{end:.3f})'")
        parts.append(f"drawbox=x={inner_x}:y={inner_y}:w={w}:h=15:color=0xA5FFB4@0.95:t=fill:enable='between(t,{start:.3f},{end:.3f})'")
    filt=",".join(parts+["format=yuv420p"])
    subprocess.run([ffmpeg,"-y","-loop","1","-i",str(image_path),"-i",str(audio_path),"-t",str(duration),"-vf",filt,"-r",str(FPS),"-c:v","libx264","-preset","veryfast","-c:a","aac","-shortest",str(out_path)],check=True)


def make_reveal_clip(ffmpeg,image_path,audio_path,out_path,duration=3.0):
    filt="zoompan=z='min(1.10,1+0.0025*on)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=90:s=1080x1920:fps=30,format=yuv420p"
    subprocess.run([ffmpeg,"-y","-loop","1","-i",str(image_path),"-i",str(audio_path),"-t",str(duration),"-vf",filt,"-r",str(FPS),"-c:v","libx264","-preset","veryfast","-c:a","aac","-shortest",str(out_path)],check=True)


async def main():
    ffmpeg=find_ffmpeg(); job=uuid.uuid4().hex[:10]; work=OUT/f"work_v6_{job}"; work.mkdir(exist_ok=True); clips=[]
    intro=background("meadow",77).convert("RGBA"); d=ImageDraw.Draw(intro,"RGBA")
    wood_panel(d,(95,600,985,1050),"GUESS THE ANIMAL!",72)
    d.text((540,1140),"5 QUESTIONS",anchor="mm",font=font(60),fill="white",stroke_width=3,stroke_fill=(50,80,90))
    d.text((540,1235),"5 SECOND ENERGY BAR",anchor="mm",font=font(44),fill=(255,245,120),stroke_width=2,stroke_fill=(50,80,90))
    ip=work/"intro.png"; intro.convert("RGB").save(ip); ia=work/"intro.mp3"; await speak("Guess the animal! Five questions, five seconds each. Ready?",ia)
    ic=work/"intro.mp4"; make_reveal_clip(ffmpeg,ip,ia,ic,4.0); clips.append(ic)
    for i,(answer,code,clue,theme) in enumerate(ANIMALS):
        icon=NTO/f"emoji_u{code}.png"
        if not icon.exists(): raise RuntimeError(f"Eksik asset: {icon}")
        qimg=work/f"q{i}.png"; aimg=work/f"a{i}.png"; make_scene(icon,answer,clue,theme,i,False).save(qimg,quality=95); make_scene(icon,answer,clue,theme,i,True).save(aimg,quality=95)
        qa=work/f"q{i}.mp3"; aa=work/f"a{i}.mp3"; await speak(f"Question {i+1}. {clue}",qa); await speak(f"The answer is {answer.lower()}! Amazing!",aa)
        qc=work/f"q{i}.mp4"; ac=work/f"a{i}.mp4"; make_question_clip(ffmpeg,qimg,qa,qc); make_reveal_clip(ffmpeg,aimg,aa,ac); clips.extend([qc,ac])
    concat=work/"concat.txt"; concat.write_text("".join([f"file '{str(p).replace(chr(92), '/')}'\n" for p in clips]),encoding="utf-8")
    output=OUT/f"kids-quiz-v6-{job}.mp4"; subprocess.run([ffmpeg,"-y","-f","concat","-safe","0","-i",str(concat),"-c","copy",str(output)],check=True); print(output)


if __name__=="__main__": asyncio.run(main())
