# PostFlow

PostFlow is a local-first AI video studio that turns a topic into a planned script and a rendered MP4 from one interface.

## Default local pipeline

The default Windows workflow is designed to run without a paid LLM API key:

1. **Ollama** runs `qwen2.5:3b` locally for script planning.
2. **MoneyPrinterTurbo** coordinates the video job.
3. **Edge TTS** provides narration (Turkish auto voice defaults to `tr-TR-AhmetNeural`).
4. **Local video materials** are used by default.
5. **FFmpeg / MoneyPrinterTurbo** renders the final H.264 + AAC MP4.

Pexels and Pixabay remain optional material sources and may require their own API credentials. Local video mode requires usable local clips to be present in MoneyPrinterTurbo's local material storage.

## Windows start

From a checkout of this repository, run:

```text
installer\windows\Start-PostFlow-Local.cmd
```

The PowerShell launcher then:

- finds or starts Ollama on `127.0.0.1:11434`;
- ensures `qwen2.5:3b` is installed;
- finds the existing MoneyPrinterTurbo source/portable installation, including `C:\PostFlow\MoneyPrinterTurbo-PostFlow`;
- when it starts MPT itself, configures `llm_provider = "ollama"`, `ollama_model_name = "qwen2.5:3b"`, `video_source = "local"`, and `subtitle_provider = "edge"`;
- starts/checks the MPT API (normally port `8080`);
- installs the Next.js dependencies when needed;
- starts PostFlow locally and opens `http://127.0.0.1:3000`.

Logs are written under `C:\PostFlow\logs`.

The launcher sets these variables for the local Next.js process:

```env
POSTFLOW_LOCAL_RUNTIME=1
POSTFLOW_MPT_API=http://127.0.0.1:8080
```

If MPT uses another detected local port, the launcher supplies that port automatically.

## Studio workflow

PostFlow deliberately separates creation into two actions:

### 1. Hikaye İçeriğini Oluştur / Video İçeriğini Oluştur

This calls the local MPT script endpoint backed by Ollama/Qwen and produces the script and scene plan. It does **not** start rendering.

### 2. Videoyu Oluştur

This sends the approved script to the local MoneyPrinterTurbo video endpoint and tracks the task until the MP4 is completed or the task fails.

The UI also contains:

- Short Video, Long Video, Story Video, and Quiz workflows;
- language, duration, aspect ratio, voice, subtitle, and material-source controls;
- editable scenes;
- a Character Bible;
- local project history;
- completed-video playback;
- an engine-status screen;
- a static idea radar.

## Character references

Character text fields (name, face, hair, age appearance, body type, outfit, style notes, negative prompt) are included in the Qwen planning prompt.

A selected character reference image is stored locally in the browser for the PostFlow workspace. MoneyPrinterTurbo does not provide a documented character-reference identity-lock contract in this integration, so PostFlow does not claim that the uploaded image will force the same face across generated visual material.

## Persistence and current boundaries

PostFlow currently uses browser `localStorage` for project/settings/Character Bible metadata and completed-video references. It does not require Supabase for the local production path.

The Content Radar currently contains local sample ideas; live web/RSS discovery is not connected in this build.

Social publishing is not enabled by default.

## Vercel boundary

A Vercel deployment can host the web interface, but a Vercel server cannot call `127.0.0.1` on a creator's Windows PC. Real local rendering therefore runs through the Windows launcher at:

```text
http://127.0.0.1:3000
```

The local MPT adapter intentionally rejects Vercel-side attempts to call the creator's localhost instead of pretending the render has started.

## Validation

```bash
npm ci
npm run typecheck
npm run build
```

GitHub Actions also runs typecheck and build on pushes to `main`.

See `THIRD_PARTY_NOTICES.md` for MoneyPrinterTurbo attribution and license details.
