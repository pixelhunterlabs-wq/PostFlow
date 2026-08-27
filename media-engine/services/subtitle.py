import re
from pathlib import Path


def _format_srt_time(seconds: float) -> str:
    ms = int(round(seconds * 1000))
    hours, ms = divmod(ms, 3_600_000)
    minutes, ms = divmod(ms, 60_000)
    secs, ms = divmod(ms, 1000)
    return f"{hours:02}:{minutes:02}:{secs:02},{ms:03}"


def split_sentences(text: str) -> list[str]:
    parts = re.split(r"(?<=[.!?…])\s+|\n+", text.strip())
    return [p.strip() for p in parts if p.strip()]


def create_srt(text: str, duration: float, output_path: Path) -> Path:
    sentences = split_sentences(text) or [text.strip()]
    weights = [max(1, len(s)) for s in sentences]
    total_weight = sum(weights)
    cursor = 0.0
    rows: list[str] = []

    for index, (sentence, weight) in enumerate(zip(sentences, weights), start=1):
        segment_duration = duration * (weight / total_weight)
        end = duration if index == len(sentences) else cursor + segment_duration
        rows.extend([
            str(index),
            f"{_format_srt_time(cursor)} --> {_format_srt_time(end)}",
            sentence,
            "",
        ])
        cursor = end

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text("\n".join(rows), encoding="utf-8")
    return output_path
