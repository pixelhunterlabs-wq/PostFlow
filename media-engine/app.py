from pathlib import Path
import os

from fastapi import FastAPI, HTTPException

from models import RenderRequest, SubtitleRequest, VoiceRequest
from services.subtitle import create_srt
from services.video import render_video
from services.voice import synthesize


app = FastAPI(title="PostFlow Media Engine", version="0.1.0")
WORK_DIR = Path(os.getenv("POSTFLOW_MEDIA_DIR", "./data")).resolve()
WORK_DIR.mkdir(parents=True, exist_ok=True)


def safe_output_path(name: str) -> Path:
    clean = Path(name).name
    if not clean:
        raise HTTPException(status_code=400, detail="Invalid output name")
    return WORK_DIR / clean


@app.get("/health")
def health():
    return {"ok": True, "service": "postflow-media-engine", "version": "0.1.0"}


@app.post("/v1/voice")
async def create_voice(request: VoiceRequest):
    try:
        path = safe_output_path(request.output_name)
        await synthesize(request.text, request.voice, request.rate, request.volume, path)
        return {"ok": True, "path": str(path)}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/v1/subtitles")
def create_subtitles(request: SubtitleRequest):
    try:
        path = safe_output_path(request.output_name)
        create_srt(request.text, request.duration, path)
        return {"ok": True, "path": str(path)}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/v1/render")
def create_render(request: RenderRequest):
    try:
        path = safe_output_path(request.output_name)
        render_video(
            materials=request.materials,
            output_path=path,
            aspect=request.aspect,
            fps=request.fps,
            clip_duration=request.clip_duration,
            audio_path=request.audio_path,
            background_music_path=request.background_music_path,
            background_music_volume=request.background_music_volume,
        )
        return {"ok": True, "path": str(path)}
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
