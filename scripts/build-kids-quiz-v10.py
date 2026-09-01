import asyncio
import importlib.util
import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageOps

BASE=Path(__file__).resolve().parent
V8_PATH=BASE/'build-kids-quiz-v8.py'
ROOT=Path(r'D:\PostFlowData\kids-assets')
REAL_ROOTS=[ROOT/'realistic-openimages',ROOT/'realistic']


def load_v8():
    spec=importlib.util.spec_from_file_location('quiz_v8',V8_PATH)
    m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m


def real_photo(key):
    aliases={'lion':['lion'],'elephant':['elephant'],'panda':['panda','giant panda'],'giraffe':['giraffe'],'monkey':['monkey','macaque','baboon']}
    terms=aliases.get(key,[key]); found=[]
    for root in REAL_ROOTS:
        if not root.exists(): continue
        for ext in ('*.jpg','*.jpeg','*.png','*.webp'):
            for p in root.rglob(ext):
                s=str(p).lower()
                if any(t in s for t in terms):
                    try:
                        with Image.open(p) as im:
                            if im.width>=500 and im.height>=400: found.append(p)
                    except: pass
    return random.choice(found) if found else None


def clean_silhouette(key,emoji_code,v7):
    # Silhouette MUST come from a transparent cutout, never a rectangular real photo.
    src=v7.choose_animal(key,emoji_code)
    im=Image.open(src).convert('RGBA')
    alpha=im.getchannel('A')
    bbox=alpha.getbbox()
    if not bbox: raise RuntimeError('Bos silhouette: '+key)
    im=im.crop(bbox); alpha=im.getchannel('A')
    # hard clean mask: removes semi-transparent glow/animation-looking edges
    alpha=alpha.point(lambda a: 255 if a>=55 else 0)
    out=Image.new('RGBA',im.size,(5,18,30,255)); out.putalpha(alpha)
    out.thumbnail((610,660),Image.Resampling.LANCZOS)
    return out


def paste_real_photo(base,path):
    # answer photo: clean rounded photo card; never used as silhouette
    photo=Image.open(path).convert('RGB')
    photo=ImageOps.fit(photo,(690,690),method=Image.Resampling.LANCZOS,centering=(0.5,0.45))
    mask=Image.new('L',(690,690),0); md=ImageDraw.Draw(mask); md.rounded_rectangle((0,0,689,689),52,fill=255)
    framed=Image.new('RGBA',(722,722),(255,255,255,255)); fm=Image.new('L',(722,722),0); ImageDraw.Draw(fm).rounded_rectangle((0,0,721,721),62,fill=255); framed.putalpha(fm)
    framed.alpha_composite(photo.convert('RGBA'),(16,16),mask)
    base.alpha_composite(framed,((1080-722)//2,445))


def make_scene_factory(v7):
    original=v7.make_scene
    def make_scene(bg_path,animal_path,answer,clue,idx,reveal=False):
        key=answer.lower()
        if reveal:
            rp=real_photo(key)
            if rp:
                # Build normal scene, then cover central old animal card with polished real-photo card.
                im=v7.fit_background(bg_path)
                d=ImageDraw.Draw(im,'RGBA'); v7.wood_panel(d,'CORRECT!')
                v7.soft_panel(d,(75,350,1005,1285),fill=(255,255,255,90),outline=(255,255,255,180),radius=58,width=5)
                im=Image.alpha_composite(im,v7.glow_layer((1080,1920),(540,820),340,(255,221,70,150)))
                d=ImageDraw.Draw(im,'RGBA'); v7.side_particles(d,1000+idx)
                paste_real_photo(im,rp)
                d=ImageDraw.Draw(im,'RGBA')
                v7.soft_panel(d,(145,1320,935,1518),fill=(255,255,255,238),outline=(255,230,138,255))
                d.text((540,1418),answer,anchor='mm',font=v7.font(88),fill=(28,142,74),stroke_width=2,stroke_fill='white')
                d.text((540,1608),'AMAZING!',anchor='mm',font=v7.font(58),fill='white',stroke_width=3,stroke_fill=(45,68,92))
                return im.convert('RGB')
            return original(bg_path,animal_path,answer,clue,idx,True)

        # question scene rebuilt with a guaranteed transparent, recognizable animal shape
        im=v7.fit_background(bg_path); d=ImageDraw.Draw(im,'RGBA'); v7.wood_panel(d,f'QUESTION {idx+1}')
        v7.soft_panel(d,(75,350,1005,1285),fill=(255,255,255,115),outline=(255,255,255,210),radius=58,width=5)
        code={'lion':'1f981','elephant':'1f418','panda':'1f43c','giraffe':'1f992','monkey':'1f412'}[key]
        sil=clean_silhouette(key,code,v7)
        x=(1080-sil.width)//2; y=500+(650-sil.height)//2
        # soft shadow behind, silhouette itself remains crisp
        a=sil.getchannel('A').filter(ImageFilter.GaussianBlur(14)); sh=Image.new('RGBA',sil.size,(0,0,0,80)); sh.putalpha(a)
        im.alpha_composite(sh,(x+12,y+20)); im.alpha_composite(sil,(x,y))
        d=ImageDraw.Draw(im,'RGBA')
        d.text((540,820),'?',anchor='mm',font=v7.font(155),fill=(255,255,255,230),stroke_width=5,stroke_fill=(20,35,48))
        v7.soft_panel(d,(120,1320,960,1510),fill=(255,255,255,235),outline=(255,255,255,250))
        v7.wrap(d,clue,1370)
        d.text((540,1608),'GUESS BEFORE ENERGY RUNS OUT!',anchor='mm',font=v7.font(39),fill='white',stroke_width=3,stroke_fill=(35,65,85))
        # V8 draws animated bar over this fixed centered shell.
        d.rounded_rectangle((140,1690,940,1805),50,fill=(18,34,42,240),outline=(255,255,255,250),width=7)
        return im.convert('RGB')
    return make_scene


async def main():
    v8=load_v8(); v7=v8.load_v7()
    v7.make_scene=make_scene_factory(v7)
    v7.question_clip=v8.question_clip
    await v7.main()

if __name__=='__main__': asyncio.run(main())
