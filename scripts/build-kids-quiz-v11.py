import asyncio
import importlib.util
from pathlib import Path
from PIL import Image, ImageDraw, ImageOps

BASE=Path(__file__).resolve().parent
V8=BASE/'build-kids-quiz-v8.py'
ROOT=Path(r'D:\PostFlowData\kids-assets')
REAL=[ROOT/'realistic-openimages',ROOT/'realistic']


def load_v8():
    s=importlib.util.spec_from_file_location('v8',V8)
    m=importlib.util.module_from_spec(s); s.loader.exec_module(m); return m


def find_real(key):
    aliases={'lion':['lion'],'elephant':['elephant'],'panda':['panda'],'giraffe':['giraffe'],'monkey':['monkey','macaque','baboon']}
    for root in REAL:
        if not root.exists(): continue
        for ext in ('*.jpg','*.jpeg','*.png','*.webp'):
            for p in root.rglob(ext):
                s=str(p).lower()
                if any(a in s for a in aliases[key]):
                    try:
                        with Image.open(p) as im:
                            if im.width>=700 and im.height>=500: return p
                    except: pass
    return None


def silhouette(key):
    im=Image.new('RGBA',(560,560),(0,0,0,0)); d=ImageDraw.Draw(im); c=(8,20,32,255)
    if key=='elephant':
        d.ellipse((150,170,430,400),fill=c); d.ellipse((95,155,245,325),fill=c); d.ellipse((180,130,320,300),fill=c)
        for x in (175,265,355): d.rounded_rectangle((x,375,x+48,535),18,fill=c)
        d.polygon([(115,250),(72,290),(68,430),(92,475),(118,438),(116,330)],fill=c); d.polygon([(430,245),(515,220),(480,270)],fill=c)
    elif key=='lion':
        d.ellipse((85,130,290,335),fill=c); d.ellipse((150,190,440,405),fill=c)
        d.rounded_rectangle((190,385,235,535),16,fill=c); d.rounded_rectangle((350,385,395,535),16,fill=c)
        d.polygon([(435,300),(525,250),(500,300),(530,338),(500,360),(465,330)],fill=c)
    elif key=='panda':
        d.ellipse((135,155,425,430),fill=c); d.ellipse((105,110,215,220),fill=c); d.ellipse((350,110,460,220),fill=c)
        d.rounded_rectangle((175,395,225,535),16,fill=c); d.rounded_rectangle((340,395,390,535),16,fill=c)
    elif key=='giraffe':
        d.ellipse((105,350,390,500),fill=c); d.rounded_rectangle((300,105,360,410),25,fill=c); d.ellipse((285,70,420,165),fill=c)
        d.polygon([(305,85),(290,25),(320,70)],fill=c); d.polygon([(390,85),(415,25),(395,70)],fill=c)
        d.rounded_rectangle((140,470,182,548),12,fill=c); d.rounded_rectangle((315,470,357,548),12,fill=c); d.polygon([(105,390),(48,338),(65,420)],fill=c)
    else:
        d.ellipse((120,165,360,420),fill=c); d.ellipse((80,125,235,275),fill=c)
        d.rounded_rectangle((150,390,195,530),15,fill=c); d.rounded_rectangle((285,390,330,530),15,fill=c); d.arc((315,190,535,450),10,330,fill=c,width=32)
    return im


def rounded_photo(path,w=600,h=520,r=42):
    photo=Image.open(path).convert('RGB'); photo=ImageOps.fit(photo,(w,h),method=Image.Resampling.LANCZOS,centering=(0.5,0.45))
    mask=Image.new('L',(w,h),0); ImageDraw.Draw(mask).rounded_rectangle((0,0,w-1,h-1),r,fill=255)
    out=Image.new('RGBA',(w,h),(0,0,0,0)); out.paste(photo,(0,0),mask); return out


def theme_overlay(im,idx):
    d=ImageDraw.Draw(im,'RGBA')
    colors=[(40,190,255,55),(255,155,45,55),(70,220,130,55),(180,95,255,55),(255,80,145,55)]
    c=colors[idx%len(colors)]
    d.rounded_rectangle((28,330,1052,1300),70,fill=c,outline=(255,255,255,95),width=4)
    for x,y,r in [(80,390,22),(990,430,30),(90,1180,28),(980,1200,20)]: d.ellipse((x-r,y-r,x+r,y+r),fill=(255,255,255,55))


def factory(v7):
    def make_scene(bg_path,animal_path,answer,clue,idx,reveal=False):
        key=answer.lower(); im=v7.fit_background(bg_path); theme_overlay(im,idx); d=ImageDraw.Draw(im,'RGBA')
        # smaller header
        d.rounded_rectangle((190,70,890,220),34,fill=(75,45,28,235),outline=(255,214,120,255),width=7)
        d.text((540,145),'CORRECT!' if reveal else f'QUESTION {idx+1}',anchor='mm',font=v7.font(58),fill=(255,245,145),stroke_width=3,stroke_fill=(55,30,15))
        # compact colored card, no white empty screen
        card=(180,360,900,1130)
        card_fill=(35,115,170,175) if idx%2==0 else (95,60,150,175)
        d.rounded_rectangle(card,58,fill=card_fill,outline=(255,255,255,220),width=5)
        if reveal:
            rp=find_real(key)
            if rp:
                ph=rounded_photo(rp); im.alpha_composite(ph,(240,450))
            else:
                sil=silhouette(key); im.alpha_composite(sil,(260,450))
            d=ImageDraw.Draw(im,'RGBA')
            d.rounded_rectangle((250,1170,830,1325),38,fill=(255,225,90,235),outline=(255,255,255,230),width=4)
            d.text((540,1248),answer,anchor='mm',font=v7.font(72),fill=(25,85,55),stroke_width=2,stroke_fill='white')
            d.text((540,1465),'AMAZING!',anchor='mm',font=v7.font(56),fill='white',stroke_width=3,stroke_fill=(35,60,90))
        else:
            sil=silhouette(key); im.alpha_composite(sil,(260,430)); d=ImageDraw.Draw(im,'RGBA')
            d.text((540,720),'?',anchor='mm',font=v7.font(145),fill=(255,235,90),stroke_width=5,stroke_fill=(25,35,50))
            d.rounded_rectangle((170,1170,910,1375),40,fill=(24,55,90,210),outline=(255,255,255,210),width=4)
            v7.wrap(d,clue,1215)
            d.text((540,1490),'GUESS BEFORE ENERGY RUNS OUT!',anchor='mm',font=v7.font(37),fill='white',stroke_width=3,stroke_fill=(25,45,70))
            d.rounded_rectangle((140,1635,940,1750),50,fill=(18,34,42,240),outline=(255,255,255,250),width=7)
        return im.convert('RGB')
    return make_scene


async def main():
    v8=load_v8(); v7=v8.load_v7(); v7.make_scene=factory(v7); v7.question_clip=v8.question_clip; await v7.main()

if __name__=='__main__': asyncio.run(main())
