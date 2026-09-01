import asyncio
import importlib.util
import random
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont, ImageOps

BASE = Path(__file__).resolve().parent
V7_PATH = BASE / 'build-kids-quiz-v7.py'
V8_PATH = BASE / 'build-kids-quiz-v8.py'
ROOT = Path(r'D:\PostFlowData\kids-assets')
NTO = ROOT / 'noto'
REAL_ROOTS = [ROOT / 'realistic-openimages', ROOT / 'realistic']
FPS = 30
W, H = 1080, 1920

CODES = {
    'lion': '1f981',
    'elephant': '1f418',
    'panda': '1f43c',
    'giraffe': '1f992',
    'monkey': '1f412',
}


def load_py(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def find_real(key):
    aliases = {
        'lion': ['lion'], 'elephant': ['elephant'], 'panda': ['panda'],
        'giraffe': ['giraffe'], 'monkey': ['monkey', 'macaque', 'baboon']
    }
    out = []
    for root in REAL_ROOTS:
        if not root.exists():
            continue
        for ext in ('*.jpg', '*.jpeg', '*.png', '*.webp'):
            for p in root.rglob(ext):
                s = str(p).lower()
                if any(a in s for a in aliases.get(key, [key])):
                    try:
                        with Image.open(p) as im:
                            if im.width >= 700 and im.height >= 500:
                                out.append(p)
                    except Exception:
                        pass
    return random.choice(out) if out else None


def noto_silhouette(key):
    p = NTO / f'emoji_u{CODES[key]}.png'
    if not p.exists():
        raise RuntimeError(f'Noto silhouette eksik: {p}')
    im = Image.open(p).convert('RGBA')
    a = im.getchannel('A')
    bbox = a.getbbox()
    if bbox:
        im = im.crop(bbox)
        a = im.getchannel('A')
    # Crisp binary outline: no glow, no strange semi-transparent edges.
    a = a.point(lambda v: 255 if v >= 80 else 0)
    sil = Image.new('RGBA', im.size, (5, 22, 38, 255))
    sil.putalpha(a)
    sil.thumbnail((610, 610), Image.Resampling.LANCZOS)
    return sil


def brighten_bg(v7, bg_path, idx):
    im = v7.fit_background(bg_path).convert('RGBA')
    rgb = im.convert('RGB')
    rgb = ImageEnhance.Color(rgb).enhance(1.22)
    rgb = ImageEnhance.Brightness(rgb).enhance(1.08)
    rgb = ImageEnhance.Contrast(rgb).enhance(1.05)
    im = rgb.convert('RGBA')
    # cheerful sky wash + soft vignette while preserving the real background
    ov = Image.new('RGBA', (W, H), (0,0,0,0)); d = ImageDraw.Draw(ov, 'RGBA')
    washes = [(55,180,255,38),(55,210,150,32),(255,185,65,30),(130,105,255,28),(255,105,160,26)]
    d.rectangle((0,0,W,H), fill=washes[idx % len(washes)])
    d.rectangle((0,0,W,350), fill=(70,190,255,42))
    return Image.alpha_composite(im, ov)


def draw_header(d, idx):
    # Reference-inspired glossy blue title card; not a large white frame.
    d.rounded_rectangle((155,70,925,250), 64, fill=(26,137,225,245), outline=(255,255,255,245), width=8)
    d.rounded_rectangle((180,92,900,150), 28, fill=(112,207,255,95))
    d.text((540,160), f'QUESTION {idx+1}', anchor='mm', font=font(66), fill='white', stroke_width=4, stroke_fill=(13,76,145))


def font(size):
    for p in [r'C:\Windows\Fonts\arialbd.ttf', r'C:\Windows\Fonts\segoeuib.ttf', r'C:\Windows\Fonts\arial.ttf']:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def draw_clue(d, clue):
    # compact blue rounded card like the reference, no giant empty area
    box=(110,1350,970,1545)
    d.rounded_rectangle(box, 54, fill=(18,121,205,236), outline=(255,255,255,245), width=7)
    words=clue.split(); lines=[]; cur=''; f=font(43)
    for w in words:
        t=(cur+' '+w).strip()
        if cur and d.textbbox((0,0),t,font=f)[2] > 760:
            lines.append(cur); cur=w
        else: cur=t
    if cur: lines.append(cur)
    y=1405
    for line in lines[:3]:
        d.text((540,y),line,anchor='mm',font=f,fill='white',stroke_width=2,stroke_fill=(10,78,140)); y+=55


def rounded_real_photo(path):
    src = Image.open(path).convert('RGB')
    src = ImageOps.fit(src, (670, 610), method=Image.Resampling.LANCZOS, centering=(0.5,0.43))
    mask=Image.new('L',(670,610),0); ImageDraw.Draw(mask).rounded_rectangle((0,0,669,609),56,fill=255)
    card=Image.new('RGBA',(706,646),(0,0,0,0))
    ImageDraw.Draw(card,'RGBA').rounded_rectangle((0,0,705,645),66,fill=(255,255,255,245),outline=(255,221,70,255),width=8)
    card.paste(src,(18,18),mask)
    return card


def make_scene_factory(v7):
    def make_scene(bg_path, animal_path, answer, clue, idx, reveal=False):
        key=answer.lower()
        im=brighten_bg(v7,bg_path,idx)
        d=ImageDraw.Draw(im,'RGBA')
        draw_header(d,idx)

        if reveal:
            # bright answer burst behind the real photo
            for r,a in [(360,30),(300,45),(240,65)]:
                d.ellipse((540-r,720-r,540+r,720+r),fill=(255,225,65,a))
            rp=find_real(key)
            if rp:
                card=rounded_real_photo(rp); im.alpha_composite(card,(187,405))
            else:
                sil=noto_silhouette(key); x=(W-sil.width)//2; y=505
                im.alpha_composite(sil,(x,y))
            d=ImageDraw.Draw(im,'RGBA')
            d.rounded_rectangle((210,1110,870,1285),50,fill=(255,197,32,245),outline=(255,255,255,250),width=7)
            d.text((540,1198),answer,anchor='mm',font=font(78),fill=(20,88,65),stroke_width=2,stroke_fill='white')
            d.rounded_rectangle((325,1325,755,1455),46,fill=(47,205,91,245),outline=(255,255,255,245),width=6)
            d.text((540,1390),'CORRECT!',anchor='mm',font=font(52),fill='white',stroke_width=3,stroke_fill=(21,122,57))
            # restrained side confetti only
            rng=random.Random(900+idx)
            cols=[(255,78,112,230),(255,216,55,230),(80,205,255,230),(113,235,129,230)]
            for _ in range(28):
                x=rng.choice([rng.randint(40,160),rng.randint(920,1040)]); y=rng.randint(420,1450); s=rng.randint(6,12)
                d.ellipse((x-s,y-s,x+s,y+s),fill=rng.choice(cols))
        else:
            # silhouette floats directly on the landscape, as in the reference.
            sil=noto_silhouette(key)
            x=(W-sil.width)//2; y=560+(610-sil.height)//2
            shadow=sil.getchannel('A').filter(ImageFilter.GaussianBlur(16))
            sh=Image.new('RGBA',sil.size,(0,0,0,80)); sh.putalpha(shadow)
            im.alpha_composite(sh,(x+14,y+22)); im.alpha_composite(sil,(x,y))
            d=ImageDraw.Draw(im,'RGBA')
            d.text((540,850),'?',anchor='mm',font=font(150),fill=(255,255,255,245),stroke_width=6,stroke_fill=(10,57,96))
            draw_clue(d,clue)
            d.text((540,1610),'GUESS THE ANIMAL!',anchor='mm',font=font(44),fill='white',stroke_width=4,stroke_fill=(20,80,125))
            # IMPORTANT: no timer shell here. V12 adds ONE animated timer in question_clip.
        return im.convert('RGB')
    return make_scene


def make_audio_with_ticks(v8, ffmpeg, voice, out, duration):
    try:
        v8.make_audio_with_ticks(ffmpeg, voice, out, duration)
    except Exception:
        shutil.copyfile(voice,out)


def question_clip_factory(v8):
    def question_clip(ffmpeg, img, audio, out, duration=5.0):
        frame_dir=Path(out).parent/(Path(out).stem+'_v12frames'); frame_dir.mkdir(parents=True,exist_ok=True)
        base=Image.open(img).convert('RGBA')
        total=int(duration*FPS)
        # One centered energy bar under title, close to reference.
        x,y,w,h=185,320,710,78
        for i in range(total):
            ratio=max(0.0,1.0-i/(total-1))
            fr=base.copy(); d=ImageDraw.Draw(fr,'RGBA')
            d.rounded_rectangle((x,y,x+w,y+h),38,fill=(15,54,78,235),outline=(255,255,255,245),width=6)
            inner_w=max(1,int((w-22)*ratio))
            if inner_w>1:
                col=(54,228,91,255) if ratio>.33 else (255,184,45,255)
                d.rounded_rectangle((x+11,y+11,x+11+inner_w,y+h-11),28,fill=col)
                hi=max(1,min(inner_w, w-22))
                d.rounded_rectangle((x+16,y+15,x+16+max(1,hi-10),y+31),12,fill=(190,255,205,175))
            fr.convert('RGB').save(frame_dir/f'frame_{i:04d}.jpg',quality=94)
        mixed=Path(out).with_name(Path(out).stem+'_ticks.m4a')
        make_audio_with_ticks(v8,ffmpeg,audio,mixed,duration)
        subprocess.run([ffmpeg,'-y','-framerate',str(FPS),'-i',str(frame_dir/'frame_%04d.jpg'),'-i',str(mixed),'-t',str(duration),'-c:v','libx264','-preset','veryfast','-pix_fmt','yuv420p','-c:a','aac','-shortest',str(out)],check=True)
    return question_clip


async def main():
    v7=load_py(V7_PATH,'v7')
    v8=load_py(V8_PATH,'v8')
    v7.make_scene=make_scene_factory(v7)
    v7.question_clip=question_clip_factory(v8)
    await v7.main()

if __name__=='__main__':
    asyncio.run(main())
