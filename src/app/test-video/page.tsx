"use client";

import { useEffect, useState } from "react";

type Task = {
  taskId: string;
  status: "queued" | "processing" | "completed" | "failed";
  progress: number;
  videos: string[];
  error?: string;
  failedStage?: string;
};

export default function TestVideoPage() {
  const [task, setTask] = useState<Task | null>(null);
  const [starting, setStarting] = useState(false);
  const [message, setMessage] = useState("Bu test Qwen'i atlar; Edge TTS + yerel materyal + MoneyPrinterTurbo + FFmpeg zincirini doğrudan dener.");

  useEffect(() => {
    if (!task || task.status === "completed" || task.status === "failed") return;
    const timer = window.setTimeout(async () => {
      try {
        const response = await fetch(`/api/local/mpt/jobs/${encodeURIComponent(task.taskId)}`, { cache: "no-store" });
        const data = await response.json() as Task & { error?: string };
        if (!response.ok) throw new Error(data.error || "Görev durumu okunamadı.");
        setTask(data);
      } catch (error) {
        setTask((current) => current ? { ...current, status: "failed", error: error instanceof Error ? error.message : "Görev durumu okunamadı." } : current);
      }
    }, 2000);
    return () => window.clearTimeout(timer);
  }, [task]);

  const startTest = async () => {
    setStarting(true);
    setTask(null);
    setMessage("Test videosu yerel motora gönderiliyor…");
    try {
      const response = await fetch("/api/local/mpt/test", { method: "POST" });
      const data = await response.json() as { taskId?: string; error?: string };
      if (!response.ok || !data.taskId) throw new Error(data.error || "Test görevi başlatılamadı.");
      setTask({ taskId: data.taskId, status: "queued", progress: 0, videos: [] });
      setMessage("Görev başladı. Sayfayı kapatmadan sonucu bekleyin.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Test videosu başlatılamadı.");
    } finally {
      setStarting(false);
    }
  };

  const progress = task?.status === "processing" && task.progress === 0 ? 35 : task?.progress ?? 0;

  return (
    <main style={{ minHeight: "100vh", padding: "48px 24px", background: "#07101d", color: "#f6f8fb" }}>
      <div style={{ maxWidth: 860, margin: "0 auto" }}>
        <a href="/" style={{ color: "#67e8b7", textDecoration: "none" }}>← PostFlow'a dön</a>
        <p style={{ marginTop: 42, color: "#8b5cf6", fontWeight: 800, letterSpacing: 1.4, fontSize: 12 }}>POSTFLOW · HIZLI TEST</p>
        <h1 style={{ fontSize: "clamp(34px,6vw,64px)", lineHeight: 1.02, margin: "10px 0 18px" }}>İlk MP4'ü şimdi üretelim.</h1>
        <p style={{ color: "#a8b3c7", fontSize: 18, lineHeight: 1.65, maxWidth: 720 }}>{message}</p>

        <section style={{ marginTop: 28, border: "1px solid #24334a", background: "#0b1626", borderRadius: 22, padding: 28 }}>
          <button
            onClick={startTest}
            disabled={starting || Boolean(task && !["completed", "failed"].includes(task.status))}
            style={{ border: 0, borderRadius: 14, padding: "14px 20px", fontWeight: 800, background: "#67e8b7", color: "#041018", cursor: "pointer", opacity: starting ? 0.7 : 1 }}
          >
            {starting ? "Başlatılıyor…" : task && !["completed", "failed"].includes(task.status) ? "Üretim sürüyor…" : "▶ İlk Test Videosunu Oluştur"}
          </button>

          {task && (
            <div style={{ marginTop: 26 }}>
              <div style={{ display: "flex", justifyContent: "space-between", gap: 12, color: "#cbd5e1" }}>
                <b>{task.status === "queued" ? "Beklemede" : task.status === "processing" ? "İşleniyor" : task.status === "completed" ? "Tamamlandı" : "Hata"}</b>
                <span>{Math.round(progress)}%</span>
              </div>
              <div style={{ height: 10, borderRadius: 999, background: "#16243a", overflow: "hidden", marginTop: 10 }}>
                <div style={{ width: `${progress}%`, height: "100%", background: "#67e8b7", transition: "width .25s ease" }} />
              </div>
              <small style={{ display: "block", marginTop: 12, color: "#77859b" }}>Görev: {task.taskId}</small>
              {task.error && <p style={{ color: "#fca5a5" }}>{task.failedStage ? `${task.failedStage}: ` : ""}{task.error}</p>}
            </div>
          )}
        </section>

        {task?.status === "completed" && task.videos.length > 0 && (
          <section style={{ marginTop: 28, border: "1px solid #2b3c56", background: "#0b1626", borderRadius: 22, padding: 22 }}>
            <h2 style={{ marginTop: 0 }}>✅ Test başarılı — MP4 hazır</h2>
            {task.videos.map((url) => <video key={url} controls src={url} style={{ width: "100%", maxHeight: 680, borderRadius: 14, background: "#000" }} />)}
          </section>
        )}
      </div>
    </main>
  );
}
