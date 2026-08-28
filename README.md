# PostFlow

Private/local-first AI video production studio.

## Windows: MoneyPrinterTurbo portable (no Docker / WSL)

PostFlow integrates the official MoneyPrinterTurbo **v1.3.5** portable Windows package as a local background production API. PostFlow is the only end-user interface; the adapter uses `GET /ping`, `POST /api/v1/videos`, and `GET /api/v1/tasks/{task_id}`.

From a checkout of this repository, run:

```text
installer\windows\Start-PostFlow-Local.cmd
```

It starts `MoneyPrinterTurbo/main.py` with the portable Python runtime, waits for `pong` on its API port (normally `8080`), then opens only PostFlow at `http://127.0.0.1:3000`. MPT's `8501` WebUI is not started or opened; it remains outside the normal PostFlow experience. Logs are retained in `C:\PostFlow\logs`.

The launcher sets the local-only variables automatically:

```env
POSTFLOW_LOCAL_RUNTIME=1
POSTFLOW_MPT_API=http://127.0.0.1:8080
```

The launcher verifies the expected portable layout directly and runs only `C:\PostFlow\MoneyPrinterTurbo\MoneyPrinterTurbo\main.py`; it never recursively discovers `main.py` or `start.bat`, so Python library files such as `lib2to3/main.py` cannot be selected.

### Important runtime boundary

Vercel production cannot reach `127.0.0.1` on a creator's Windows machine. “Videoyu Oluştur” is intentionally enabled only when PostFlow and MoneyPrinterTurbo run locally. It returns a clear Turkish message if the local service is unavailable.

Character Bible data stays in PostFlow. MoneyPrinterTurbo v1.3.5 does **not** document a reference-image/character-identity API, so PostFlow does not claim that MPT will preserve a reference image or lock a face across scenes; that guarantee depends on the selected image provider.

## Validation

```bash
npm install
npm run typecheck
npm run build
```

See `THIRD_PARTY_NOTICES.md` for MoneyPrinterTurbo attribution and license details.
