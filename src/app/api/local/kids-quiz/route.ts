import { NextResponse } from "next/server";
import { spawn } from "node:child_process";
import path from "node:path";

export const runtime = "nodejs";
export const maxDuration = 300;

export async function POST() {
  const python = "D:\\MoneyPrinterTurbo\\.venv\\Scripts\\python.exe";
  const script = path.join(process.cwd(), "scripts", "build-kids-quiz.py");

  return await new Promise((resolve) => {
    const child = spawn(python, [script], { cwd: process.cwd(), windowsHide: true });
    let out = "";
    let err = "";

    child.stdout.on("data", (d) => (out += d.toString()));
    child.stderr.on("data", (d) => (err += d.toString()));

    child.on("close", (code) => {
      if (code !== 0) {
        return resolve(NextResponse.json({ error: err || out || `Quiz render failed (${code})` }, { status: 500 }));
      }

      const file = out
        .trim()
        .split(/\r?\n/)
        .reverse()
        .find((x) => x.toLowerCase().endsWith(".mp4"));

      if (!file) {
        return resolve(NextResponse.json({ error: "MP4 output not found", log: out }, { status: 500 }));
      }

      return resolve(NextResponse.json({ ok: true, file: file.trim() }));
    });
  });
}
