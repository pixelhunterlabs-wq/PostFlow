# PostFlow

Private/local-first AI video production studio.

## Media Engine (Phase 1)

PostFlow now includes a separate Python/FastAPI media engine inspired by the MIT-licensed MoneyPrinterTurbo project. The first integration provides:

- Edge TTS voice generation
- SRT subtitle generation
- 9:16, 16:9 and 1:1 video rendering
- Image/video material concatenation
- Narration + background music mixing
- A small TypeScript client for the Next.js application
- Docker packaging with FFmpeg

### Run locally

```bash
cd media-engine
python -m venv .venv
# Windows: .venv\\Scripts\\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --host 127.0.0.1 --port 8787 --reload
```

Health check:

```text
GET http://127.0.0.1:8787/health
```

Next.js should use:

```env
MEDIA_ENGINE_URL=http://127.0.0.1:8787
```

### Next integration steps

1. Material search/download adapters (Pexels/Pixabay/local library)
2. Subtitle burn-in and style controls
3. Render task queue and progress tracking
4. Connect PostFlow video projects/scenes to `/v1/voice`, `/v1/subtitles`, `/v1/render`
5. Optional publishing adapters after render

See `THIRD_PARTY_NOTICES.md` for MoneyPrinterTurbo attribution and license details.
