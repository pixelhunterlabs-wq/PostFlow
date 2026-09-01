import asyncio
import random
import shutil
import subprocess
import uuid
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance
import edge_tts

ROOT = Path(r"D:\PostFlowData\kids-assets")
OUT = Path(r"D:\PostFlowData\output\final-videos")
BGROOT = ROOT / "backgrounds"
ANIMALROOT = ROOT / "animals"
NTO = ROOT / "noto"
OUT.mkdir(parents=True, exist_ok=True)

W,H,FPS = 1080,1920,30
QUESTION_SECONDS = 5.0

ANIMALS = [
    ("LION", "lion", "1f981", "I am the king of the jungle. Who am I?"),
    ("ELEPHANT", "elephant", "1f418", "I have a long trunk and very big ears. Who am I?"),
    ("PANDA", "panda", "1f43c", "I am black and white and I love bamboo. Who am I?"),
    ("GIRAFFE", "giraffe", "1f992", "I have a very long neck. Who am I?"),
    ("MONKEY", "monkey", "1f412", "I love climbing trees and eating bananas. Who am I?"),
]


def find_ffmpeg():
    exe=shutil.which("ffmpeg")
    if exe: return exe
    for base in [Path(r"D:\MoneyPrinterTurbo"),Path(r"D:\PostFlow")]:
        if base.exists():
            for p in base.rglob("ffmpeg.exe"): return str(p)
    raise RuntimeError("ffmpeg.exe bulunamadi")


def font(size):
    for p in [r"C:\Windows\Fonts\arialbd.ttf",r"C:\Windows\Fonts\segoeuib.ttf",r"C:\Windows\Fonts\arial.ttf"]:
        if Path(p).exists(): return ImageFont.truetype(p,size)
    return ImageFont.load_default()


def fit_background(path):
    im=Image.open(path).convert("RGB")
    # cover crop to 9:16, keep subject-free center as much as possible
    ratio=max(W/im.width,H/im.height)
    nw,nh=int(im.width*ratio),int(im.height*ratio)
    im=im.resize((nw,nh),Image.Resampling.LANCZOS)
    left=max(0,(nw-W)//2); top=max(0,(nh-H)//2)
    im=im.crop((left,top,left+W,top+H))
    im=ImageEnhance.Color(im).enhance(1.08)
    im=ImageEnhance.Contrast(im).enhance(1.04)
    return im.convert("RGBA")


def choose_backgrounds():
    files=[]
    if BGROOT.exists():
        for ext in ("*.png","*.jpg","*.jpeg","*.webp"):
            files.extend(BGROOT.rglob(ext))
    files=[p for p in files if p.is_file() and "license" not in p.name.lower()]
    # prefer direct HD forest/underwater images over tiny pack pieces
    ranked=[]
    for p in files:
        score=0
        n=str(p).lower()
        if "forest_hd" in n or "underwater_hd" in n: score+=100
        try:
            with Image.open(p) as im:
                if im.width>=1000 and im.height>=600: score+=50
                score+=min(im.width*im.height//100000,40)
        except: continue
        ranked.append((score,p))
    ranked.sort(reverse=True,key=lambda x:x[0])
    return [p for _,p in ranked]


def choose_animal(keyword, emoji_code):
    candidates=[]
    if ANIMALROOT.exists():
        for ext in ("*.png","*.webp"):
            for p in ANIMALROOT.rglob(ext):
                n=p.stem.lower().replace("-"," ").replace("_"," ")
                if keyword in n:
                    try:
                        with Image.open(p) as im:
                            area=im.width*im.height
                            # prefer larger transparent PNGs
                            score=area + (250000 if im.mode in ("RGBA","LA") else 0)
                            candidates.append((score,p))
                    except: pass
    if candidates:
        candidates.sort(reverse=True,key=lambda x:x[0])
        return candidates[0][1]
    fallback=NTO/f"emoji_u{emoji_code}.png"
    if fallback.exists(): return fallback
    raise RuntimeError(f"Animal asset bulunamadi: {keyword}")


def soft_panel(d,box,fill=(255,255,255,220),outline=(255,255,255,245),radius=48,width=5):
    d.rounded_rectangle(box,radius,fill=fill,outline=outline,width=width)


def wood_panel(d,text,small=False):
    box=(95,80,985,285)
    x1,y1,x2,y2=box
    d.rounded_rectangle(box,38,fill=(112,65,34,245),outline=(255,220,142,255),width=8)
    d.rounded_rectangle((x1+17,y1+17,x2-17,y2-17),30,fill=(181,112,57,250))
    for yy in range(y1+52,y2-18,58):
        d.line((x1+38,yy,x2-38,yy),fill=(130,72,37,120),width=3)
    d.text((540,183),text,anchor="mm",font=font(58 if small else 68),fill=(255,245,145),stroke_width=4,stroke_fill=(75,38,18))


def prepare_animal(path,max_size=(600,650)):
    im=Image.open(path).convert("RGBA")
    # remove uniform-ish background only when asset lacks transparency
    if im.getbbox() is None: return im
    im.thumbnail(max_size,Image.Resampling.LANCZOS)
    return im


def silhouette(im):
    alpha=im.getchannel("A")
    # tighter silhouette edge + subtle glow
    sil=Image.new("RGBA",im.size,(13,26,40,255)); sil.putalpha(alpha)
    return sil


def glow_layer(size,center,radius,color=(255,230,95,120)):
    lay=Image.new("RGBA",size,(0,0,0,0)); d=ImageDraw.Draw(lay,"RGBA")
    cx,cy=center
    for r in range(radius,20,-18):
        a=int(color[3]*(1-r/radius)*0.65)+8
        d.ellipse((cx-r,cy-r,cx+r,cy+r),fill=(color[0],color[1],color[2],a))
    return lay.filter(ImageFilter.GaussianBlur(18))


def side_particles(d,seed):
    rng=random.Random(seed)
    cols=[(255,87,115,235),(255,218,65,235),(85,205,255,235),(135,245,140,235),(170,100,255,235)]
    # edge-only zones, never on central animal
    for _ in range(44):
        side=rng.choice([0,1])
        x=rng.randint(30,185) if side==0 else rng.randint(895,1050)
        y=rng.randint(330,1640); s=rng.randint(5,14); c=rng.choice(cols)
        if rng.random()<0.5: d.ellipse((x-s,y-s,x+s,y+s),fill=c)
        else: d.rounded_rectangle((x-s,y-s,x+s,y+s),4,fill=c)


def wrap(d,text,y):
    f=font(44); words=text.split(); lines=[]; cur=""
    for w in words:
        t=(cur+" "+w).strip()
        if cur and d.textbbox((0,0),t,font=f)[2]>790:
            lines.append(cur); cur=w
        else: cur=t
    if cur: lines.append(cur)
    for line in lines[:3]:
        d.text((540,y),line,anchor="mm",font=f,fill=(34,44,65),stroke_width=2,stroke_fill=(255,255,255,170)); y+=58


def make_scene(bg_path,animal_path,answer,clue,idx,reveal=False):
    im=fit_background(bg_path)
    # cinematic depth: darken very top and brighten center
    top=Image.new("RGBA",(W,H),(0,0,0,0)); td=ImageDraw.Draw(top,"RGBA")
    td.rectangle((0,0,W,340),fill=(0,20,35,35))
    im=Image.alpha_composite(im,top)
    d=ImageDraw.Draw(im,"RGBA")
    wood_panel(d,"CORRECT!" if reveal else f"QUESTION {idx+1}")

    # main floating card
    soft_panel(d,(75,350,1005,1285),fill=(255,255,255,90),outline=(255,255,255,180),radius=58,width=5)

    animal=prepare_animal(animal_path)
    if not reveal: animal=silhouette(animal)
    x=(W-animal.width)//2; y=500

    if reveal:
        glow=glow_layer((W,H),(540,820),340,(255,221,70,150))
        im=Image.alpha_composite(im,glow)
        d=ImageDraw.Draw(im,"RGBA")
        side_particles(d,1000+idx)

    # shadow separated from animal
    a=animal.getchannel("A").filter(ImageFilter.GaussianBlur(22))
    sh=Image.new("RGBA",animal.size,(0,0,0,100)); sh.putalpha(a)
    im.alpha_composite(sh,(x+16,y+28)); im.alpha_composite(animal,(x,y))
    d=ImageDraw.Draw(im,"RGBA")

    if not reveal:
        # centered question mark on silhouette
        d.text((540,820),"?",anchor="mm",font=font(170),fill=(255,255,255,185),stroke_width=4,stroke_fill=(30,45,55,120))
        soft_panel(d,(120,1320,960,1510),fill=(255,255,255,230),outline=(255,255,255,245))
        wrap(d,clue,1370)
        d.text((540,1608),"GUESS BEFORE ENERGY RUNS OUT!",anchor="mm",font=font(39),fill="white",stroke_width=3,stroke_fill=(35,65,85))
        # centered fixed energy shell
        d.rounded_rectangle((140,1690,940,1805),50,fill=(18,34,42,240),outline=(255,255,255,250),width=7)
    else:
        soft_panel(d,(145,1320,935,1518),fill=(255,255,255,238),outline=(255,230,138,255))
        d.text((540,1418),answer,anchor="mm",font=font(88),fill=(28,142,74),stroke_width=2,stroke_fill="white")
        d.text((540,1608),"AMAZING!",anchor="mm",font=font(58),fill="white",stroke_width=3,stroke_fill=(45,68,92))
    return im.convert("RGB")


async def speak(text,out):
    await edge_tts.Communicate(text,"en-US-AriaNeural",rate="+7%").save(str(out))


def question_clip(ffmpeg,img,audio,out,duration=QUESTION_SECONDS):
    # 25 steps = very obvious, smooth-looking 5-second drain
    steps=25; parts=[]; inner_x,inner_y,inner_w,inner_h=170,1718,740,59
    for i in range(steps):
        start=i*duration/steps; end=(i+1)*duration/steps; remain=1-(i/steps); w=max(1,int(inner_w*remain))
        parts.append(f"drawbox=x={inner_x}:y={inner_y}:w={w}:h={inner_h}:color=0x49E66B@1:t=fill:enable='between(t,{start:.3f},{end:.3f})'")
        parts.append(f"drawbox=x={inner_x}:y={inner_y}:w={w}:h=13:color=0xB2FFC0@0.95:t=fill:enable='between(t,{start:.3f},{end:.3f})'")
    filt=",".join(parts+["zoompan=z='1.0+0.012*sin(on/30)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=150:s=1080x1920:fps=30","format=yuv420p"])
    subprocess.run([ffmpeg,"-y","-loop","1","-i",str(img),"-i",str(audio),"-t",str(duration),"-vf",filt,"-r",str(FPS),"-c:v","libx264","-preset","veryfast","-c:a","aac","-shortest",str(out)],check=True)


def reveal_clip(ffmpeg,img,audio,out,duration=3.2):
    filt="zoompan=z='min(1.095,1+0.0022*on)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=96:s=1080x1920:fps=30,format=yuv420p"
    subprocess.run([ffmpeg,"-y","-loop","1","-i",str(img),"-i",str(audio),"-t",str(duration),"-vf",filt,"-r",str(FPS),"-c:v","libx264","-preset","veryfast","-c:a","aac","-shortest",str(out)],check=True)


async def main():
    ffmpeg=find_ffmpeg(); job=uuid.uuid4().hex[:10]; work=OUT/f"work_v7_{job}"; work.mkdir(exist_ok=True)
    bgs=choose_backgrounds()
    if not bgs: raise RuntimeError(r"HD background bulunamadi: D:\PostFlowData\kids-assets\backgrounds")
    clips=[]

    intro_bg=bgs[0]
    intro=fit_background(intro_bg); d=ImageDraw.Draw(intro,"RGBA")
    wood_panel(d,"GUESS THE ANIMAL!",True)
    soft_panel(d,(120,730,960,1140),fill=(255,255,255,185),outline=(255,255,255,240),radius=55)
    d.text((540,860),"5 QUESTIONS",anchor="mm",font=font(72),fill=(38,105,168),stroke_width=3,stroke_fill="white")
    d.text((540,990),"5 SECOND ENERGY BAR",anchor="mm",font=font(46),fill=(34,145,77),stroke_width=2,stroke_fill="white")
    ip=work/"intro.png"; intro.convert("RGB").save(ip,quality=96)
    ia=work/"intro.mp3"; await speak("Guess the animal! Five questions, five seconds each. Ready?",ia)
    ic=work/"intro.mp4"; reveal_clip(ffmpeg,ip,ia,ic,4.0); clips.append(ic)

    for i,(answer,key,code,clue) in enumerate(ANIMALS):
        bg=bgs[i%len(bgs)]
        animal=choose_animal(key,code)
        print(f"Q{i+1} BG: {bg}")
        print(f"Q{i+1} ANIMAL: {animal}")
        qimg=work/f"q{i}.png"; aimg=work/f"a{i}.png"
        make_scene(bg,animal,answer,clue,i,False).save(qimg,quality=96)
        make_scene(bg,animal,answer,clue,i,True).save(aimg,quality=96)
        qa=work/f"q{i}.mp3"; aa=work/f"a{i}.mp3"
        await speak(f"Question {i+1}. {clue}",qa); await speak(f"The answer is {answer.lower()}! Amazing!",aa)
        qc=work/f"q{i}.mp4"; ac=work/f"a{i}.mp4"
        question_clip(ffmpeg,qimg,qa,qc); reveal_clip(ffmpeg,aimg,aa,ac); clips.extend([qc,ac])

    concat=work/"concat.txt"; concat.write_text("".join([f"file '{str(p).replace(chr(92), '/')}'\n" for p in clips]),encoding="utf-8")
    output=OUT/f"kids-quiz-v7-{job}.mp4"
    subprocess.run([ffmpeg,"-y","-f","concat","-safe","0","-i",str(concat),"-c","copy",str(output)],check=True)
    print(output)

if __name__=="__main__": asyncio.run(main())
