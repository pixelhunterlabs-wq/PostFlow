from typing import Literal
from pydantic import BaseModel, Field


class SubtitleSegment(BaseModel):
    start: float = Field(ge=0)
    end: float = Field(gt=0)
    text: str


class VoiceRequest(BaseModel):
    text: str
    voice: str = "tr-TR-AhmetNeural"
    rate: str = "+0%"
    volume: str = "+0%"
    output_name: str = "voice.mp3"


class SubtitleRequest(BaseModel):
    text: str
    duration: float = Field(gt=0)
    output_name: str = "subtitles.srt"


class RenderRequest(BaseModel):
    materials: list[str]
    output_name: str = "video.mp4"
    aspect: Literal["9:16", "16:9", "1:1"] = "9:16"
    fps: int = Field(default=30, ge=15, le=60)
    clip_duration: float = Field(default=3.0, gt=0, le=30)
    audio_path: str | None = None
    subtitle_path: str | None = None
    background_music_path: str | None = None
    background_music_volume: float = Field(default=0.16, ge=0, le=1)
