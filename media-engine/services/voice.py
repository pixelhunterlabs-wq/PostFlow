from pathlib import Path
import edge_tts


async def synthesize(text: str, voice: str, rate: str, volume: str, output_path: Path) -> Path:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    communicate = edge_tts.Communicate(text=text, voice=voice, rate=rate, volume=volume)
    await communicate.save(str(output_path))
    return output_path
