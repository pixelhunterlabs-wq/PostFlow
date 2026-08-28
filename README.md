# PostFlow

Private/local-first AI video production studio.

## Windows: MoneyPrinterTurbo portable (no Docker / WSL)

PostFlow integrates the official MoneyPrinterTurbo **v1.3.5** portable Windows package. The adapter uses its real WebUI API: `GET /ping`, `POST /api/v1/videos`, and `GET /api/v1/tasks/{task_id}`. The first run downloads the official archive, verifies its exact SHA-256 and size, tests the 7z archive, extracts it under `C:\PostFlow\MoneyPrinterTurbo`, then starts MPT and PostFlow in separate visible terminals.

From a checkout of this repository, run:

```text
installer\windows\Start-PostFlow-Local.cmd
```

It opens PostFlow only after MPT responds with `pong` on its API port (normally `8080`) and PostFlow is available on `http://127.0.0.1:3000`. Logs are retained in `C:\PostFlow\logs`.

The launcher sets the local-only variables automatically:

```env
POSTFLOW_LOCAL_RUNTIME=1
POSTFLOW_MPT_API=http://127.0.0.1:8080
```

For an existing local MPT install, the launcher finds `start.bat` recursively and uses its own directory as the working directory. It will not overwrite an existing incomplete install; its error identifies the exact folder to inspect.

### Important runtime boundary

Vercel production cannot reach `127.0.0.1` on a creator's Windows machine. “Videoyu Oluştur” is intentionally enabled only when PostFlow and MoneyPrinterTurbo run locally. It returns a clear Turkish message if the local service is unavailable.

Character names and descriptions are included in the submitted script context, but MoneyPrinterTurbo v1.3.5 does **not** document a reference-image/character-identity API. PostFlow therefore does not claim that MPT will preserve a Character Bible reference image or lock a face across scenes.

## Validation

```bash
npm install
npm run typecheck
npm run build
```

See `THIRD_PARTY_NOTICES.md` for MoneyPrinterTurbo attribution and license details.
