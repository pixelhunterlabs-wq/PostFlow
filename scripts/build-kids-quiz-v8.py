import asyncio
import importlib.util
import shutil
import subprocess
from pathlib import Path

from PIL import Image

BASE = Path(__file__).resolve().parent
V7_PATH = BASE / "build-kids-quiz-v7.py"
ROOT = Path(r"D:\PostFlowData\kids-assets")
TIMER_DIR = ROOT / "ui" / "timer"
AUDIO_DIR = ROOT / "audio" / "timer"
FPS = 30


def load_v7():
    spec = importlib.util.spec_from_file_location("quiz_v7", V7_PATH)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def load_layer(name):
    p = TIMER_DIR / name
    if not p.exists():
        return None
    return Image.open(p).convert("RGBA")


def fit_layer(im, size):
    if im is None:
        return None
    return im.resize(size, Image.Resampling.LANCZOS)


def build_timer_frames(base_img, work, duration):
    work.mkdir(parents=True, exist_ok=True)
    base = Image.open(base_img).convert("RGBA")
    total = max(1, int(duration * FPS))

    x, y, w, h = 150, 1702, 780, 92
    bg = fit_layer(load_layer("BarBackground.png"), (w, h))
    fill = fit_layer(load_layer("GreenBar.png"), (w, h))
    glass = fit_layer(load_layer("BarGlass.png"), (w, h))

    for i in range(total):
        frame = base.copy()
        if bg:
            frame.alpha_composite(bg, (x, y))
        ratio = max(0.0, 1.0 - i / (total - 1 if total > 1 else 1))
        fw = max(1, int(w * ratio))
        if fill:
            cropped = fill.crop((0, 0, fw, h))
            frame.alpha_composite(cropped, (x, y))
        else:
            from PIL import ImageDraw
            d = ImageDraw.Draw(frame, "RGBA")
            d.rounded_rectangle((x, y, x + w, y + h), 44, fill=(12, 25, 35, 235), outline=(255, 255, 255, 245), width=6)
            inner = max(1, int((w - 26) * ratio))
            d.rounded_rectangle((x + 13, y + 13, x + 13 + inner, y + h - 13), 34, fill=(65, 230, 105, 255))
            d.rounded_rectangle((x + 13, y + 13, x + 13 + inner, y + 34), 20, fill=(170, 255, 190, 210))
        if glass:
            frame.alpha_composite(glass, (x, y))
        frame.convert("RGB").save(work / f"frame_{i:04d}.jpg", quality=93)


def make_audio_with_ticks(ffmpeg, voice, out, duration):
    tick = AUDIO_DIR / "tick.wav"
    tick2 = AUDIO_DIR / "tick2.wav"
    if not tick.exists():
        shutil.copyfile(voice, out)
        return
    tick_last = tick2 if tick2.exists() else tick
    cmd = [ffmpeg, "-y", "-i", str(voice)]
    tick_inputs = []
    for sec in range(int(duration)):
        t = tick_last if sec == int(duration) - 1 else tick
        cmd += ["-i", str(t)]
        tick_inputs.append(sec + 1)
    filters = []
    labels = []
    for sec, idx in enumerate(tick_inputs):
        filters.append(f"[{idx}:a]adelay={sec*1000}|{sec*1000},volume=0.45[t{sec}]")
        labels.append(f"[t{sec}]")
    filters.append(f"[0:a]{''.join(labels)}amix=inputs={1+len(labels)}:duration=longest:normalize=0,atrim=duration={duration}[a]")
    cmd += ["-filter_complex", ";".join(filters), "-map", "[a]", "-c:a", "aac", "-b:a", "128k", str(out)]
    subprocess.run(cmd, check=True)


def question_clip(ffmpeg, img, audio, out, duration=5.0):
    frame_dir = Path(out).with_suffix("")
    frame_dir = frame_dir.parent / (frame_dir.name + "_frames")
    build_timer_frames(img, frame_dir, duration)
    mixed = Path(out).with_name(Path(out).stem + "_ticks.m4a")
    make_audio_with_ticks(ffmpeg, audio, mixed, duration)
    subprocess.run([
        ffmpeg, "-y",
        "-framerate", str(FPS),
        "-i", str(frame_dir / "frame_%04d.jpg"),
        "-i", str(mixed),
        "-t", str(duration),
        "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-shortest", str(out)
    ], check=True)


async def main():
    v7 = load_v7()
    v7.question_clip = question_clip
    await v7.main()


if __name__ == "__main__":
    asyncio.run(main())
