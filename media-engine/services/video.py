from pathlib import Path
from typing import Iterable

from moviepy import AudioFileClip, CompositeAudioClip, ImageClip, VideoFileClip, concatenate_videoclips


ASPECT_SIZES = {
    "9:16": (1080, 1920),
    "16:9": (1920, 1080),
    "1:1": (1080, 1080),
}


def _fit_clip(clip, size: tuple[int, int], duration: float | None = None):
    target_w, target_h = size
    clip = clip.resized(height=target_h)
    if clip.w < target_w:
        clip = clip.resized(width=target_w)
    clip = clip.cropped(
        x_center=clip.w / 2,
        y_center=clip.h / 2,
        width=target_w,
        height=target_h,
    )
    if duration is not None:
        clip = clip.with_duration(duration)
    return clip


def _open_material(path: Path, size: tuple[int, int], clip_duration: float):
    suffix = path.suffix.lower()
    if suffix in {".jpg", ".jpeg", ".png", ".webp"}:
        return _fit_clip(ImageClip(str(path)), size, clip_duration)
    clip = VideoFileClip(str(path), audio=False)
    if clip.duration > clip_duration:
        clip = clip.subclipped(0, clip_duration)
    return _fit_clip(clip, size)


def render_video(
    materials: Iterable[str],
    output_path: Path,
    aspect: str = "9:16",
    fps: int = 30,
    clip_duration: float = 3.0,
    audio_path: str | None = None,
    background_music_path: str | None = None,
    background_music_volume: float = 0.16,
) -> Path:
    material_paths = [Path(p) for p in materials]
    if not material_paths:
        raise ValueError("At least one material is required")

    missing = [str(p) for p in material_paths if not p.exists()]
    if missing:
        raise FileNotFoundError(f"Missing media files: {', '.join(missing)}")

    size = ASPECT_SIZES[aspect]
    clips = []
    audio_clips = []

    try:
        clips = [_open_material(path, size, clip_duration) for path in material_paths]
        final = concatenate_videoclips(clips, method="compose")

        if audio_path:
            narration = AudioFileClip(audio_path)
            audio_clips.append(narration)
            if narration.duration < final.duration:
                final = final.subclipped(0, narration.duration)
            else:
                narration = narration.subclipped(0, final.duration)
                audio_clips[-1] = narration

        if background_music_path:
            bgm = AudioFileClip(background_music_path)
            if bgm.duration > final.duration:
                bgm = bgm.subclipped(0, final.duration)
            bgm = bgm.with_volume_scaled(background_music_volume)
            audio_clips.append(bgm)

        if audio_clips:
            final = final.with_audio(CompositeAudioClip(audio_clips))

        output_path.parent.mkdir(parents=True, exist_ok=True)
        final.write_videofile(
            str(output_path),
            fps=fps,
            codec="libx264",
            audio_codec="aac",
            audio_bitrate="192k",
            threads=2,
            logger=None,
        )
        final.close()
        return output_path
    finally:
        for clip in clips:
            try:
                clip.close()
            except Exception:
                pass
        for audio in audio_clips:
            try:
                audio.close()
            except Exception:
                pass
