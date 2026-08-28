"use client";

import { useEffect, useState } from "react";

type View = "dashboard" | "studio" | "ready" | "engine";
type EngineState = "checking" | "connected" | "offline";
type TaskStatus = "queued" | "processing" | "completed" | "failed";
type Workflow = "short" | "long" | "story" | "quiz";
type Aspect = "9:16" | "16:9" | "1:1";

type Scene = { id: string; number: number; text: string; visualPrompt: string; characters: string[] };
type Task = { taskId: string; status: TaskStatus; progress: number; videos: string[]; error?: string; failedStage?: string };
type ReadyVideo = { id: string; taskId: string; title: string; url: string; createdAt: string };
type StoryResponse = { title: string; summary: string; script: string; scenes: Array<{ number: number; text: string; visualPrompt: string; characters: string[] }>; error?: string };

const workflowDefaults: Record<Workflow, { label: string; aspect: Aspect; duration: number; scenes: number }> = {
  short: { label: "Kısa Video", aspect: "9:16", duration: 45, scenes: 4 },
  long: { label: "Uzun Video", aspect: "16:9", duration: 300, scenes: 12 },
  story: { label: "Hikaye Videosu", aspect: "9:16", duration: 60, scenes: 6 },
  quiz: { label: "Quiz Video", aspect: "9:16", duration: 45, scenes: 5 },
};

function taskLabel(status: TaskStatus) {
  return status === "queued" ? "Beklemede" : status === "processing" ? "İşleniyor" : status === "completed" ? "Tamamlandı" : "Hata";
}

export default function Home() {
  const [view, setView] = useState<View>("dashboard");
  const [engine, setEngine] = useState<EngineState>("checking");
  const [engineDetail, setEngineDetail] = useState("Yerel motor kontrol ediliyor…");
  const [workflow, setWorkflow] = useState<Workflow>("short");
  const [topic, setTopic] = useState("");
  const [genre, setGenre] = useState("Bilgi / Belgesel");
  const [language, setLanguage] = useState("tr");
  const [instruction, setInstruction] = useState("");
  const [summary, setSummary] = useState("");
  const [script, setScript] = useState("");
  const [scenes, setScenes] = useState<Scene[]>([]);
  const [task, setTask] = useState<Task | null>(null);
  const [readyVideos, setReadyVideos] = useState<ReadyVideo[]>([]);
  const [notice, setNotice] = useState("");
  const [planning, setPlanning] = useState(false);
  const [startingTest, setStartingTest] = useState(false);

  const activeTask = Boolean(task && !["completed", "failed"].includes(task.status));
  const hasPlan = Boolean(script.trim()) && scenes.length > 0;
  const settings = workflowDefaults[workflow];

  const checkEngine = async () => {
    setEngine("checking");
    try {
      const response = await fetch("/api/local/mpt/health", { cache: "no-store" });
      const data = await response.json() as { ok?: boolean; apiUrl?: string; error?: string };
      if (!response.ok || !data.ok) throw new Error(data.error || "Yerel motora bağlanılamadı.");
      setEngine("connected");
      setEngineDetail(`Bağlı · ${data.apiUrl ?? "127.0.0.1:8080"}`);
    } catch (error) {
      setEngine("offline");
      setEngineDetail(error instanceof Error ? error.message : "Yerel motor kapalı.");
    }
  };

  useEffect(() => {
    try {
      setReadyVideos(JSON.parse(localStorage.getItem("postflow-ready-videos") || "[]") as ReadyVideo[]);
    } catch {
      setReadyVideos([]);
    }
    void checkEngine();
  }, []);

  useEffect(() => {
    localStorage.setItem("postflow-ready-videos", JSON.stringify(readyVideos));
  }, [readyVideos]);

  useEffect(() => {
    if (!task || !activeTask) return;
    const timer = window.setTimeout(async () => {
      try {
        const response = await fetch(`/api/local/mpt/jobs/${encodeURIComponent(task.taskId)}`, { cache: "no-store" });
        const data = await response.json() as Task & { error?: string };
        if (!response.ok) throw new Error(data.error || "Görev durumu okunamadı.");
        setTask(data);
      } catch (error) {
        setTask((current) => current ? { ...current, status: "failed", error: error instanceof Error ? error.message : "Görev okunamadı." } : current);
      }
    }, 2000);
    return () => window.clearTimeout(timer);
  }, [task, activeTask]);

  useEffect(() => {
    if (!task || task.status !== "completed" || !task.videos.length) return;
    setReadyVideos((current) => {
      const additions = task.videos
        .filter((url) => !current.some((item) => item.taskId === task.taskId && item.url === url))
        .map((url, index) => ({
          id: `${task.taskId}-${index}`,
          taskId: task.taskId,
          title: topic.trim() || "PostFlow İlk Test Videosu",
          url,
          createdAt: new Date().toLocaleString("tr-TR"),
        }));
      return additions.length ? [...additions, ...current] : current;
    });
    setNotice("✅ MP4 hazır. Hazır Videolar bölümünde izleyebilirsiniz.");
  }, [task, topic]);

  const resetForWorkflow = (next: Workflow) => {
    setWorkflow(next);
    setSummary("");
    setScript("");
    setScenes([]);
    setTask(null);
  };

  const createContent = async () => {
    if (!topic.trim()) {
      setNotice("Önce video konusunu yazın.");
      return;
    }
    setPlanning(true);
    setNotice("İçerik hazırlanıyor…");
    try {
      const response = await fetch("/api/local/mpt/story", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          subject: topic.trim(),
          language,
          sceneCount: settings.scenes,
          genre,
          instruction,
          durationSeconds: settings.duration,
          characterBible: [],
        }),
      });
      const data = await response.json() as StoryResponse;
      if (!response.ok || !data.script || !Array.isArray(data.scenes)) throw new Error(data.error || "İçerik oluşturulamadı.");
      setSummary(data.summary);
      setScript(data.script);
      setScenes(data.scenes.map((scene) => ({ ...scene, id: crypto.randomUUID() })));
      setNotice("✅ İçerik hazır. Şimdi sadece Videoyu Oluştur'a basın.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "İçerik oluşturulamadı.");
    } finally {
      setPlanning(false);
    }
  };

  const createVideo = async () => {
    if (!hasPlan) return;
    setNotice("Video üretimi başlatılıyor…");
    try {
      const response = await fetch("/api/local/mpt/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          subject: topic.trim(),
          script,
          aspect: settings.aspect,
          language,
          voice: "Otomatik",
          materialSource: "local",
          subtitleEnabled: true,
        }),
      });
      const data = await response.json() as { taskId?: string; error?: string };
      if (!response.ok || !data.taskId) throw new Error(data.error || "Video görevi oluşturulamadı.");
      setTask({ taskId: data.taskId, status: "queued", progress: 0, videos: [] });
      setNotice("Video üretiliyor. İlerleme aşağıda görünecek.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Video üretimi başlatılamadı.");
    }
  };

  const startFirstTest = async () => {
    setStartingTest(true);
    setTask(null);
    setTopic("PostFlow İlk Test Videosu");
    setNotice("İlk test MP4'ü oluşturuluyor…");
    try {
      const response = await fetch("/api/local/mpt/test", { method: "POST" });
      const data = await response.json() as { taskId?: string; error?: string };
      if (!response.ok || !data.taskId) throw new Error(data.error || "Test görevi başlatılamadı.");
      setTask({ taskId: data.taskId, status: "queued", progress: 0, videos: [] });
      setNotice("Test başladı. Sonucu bu ekranda bekleyin.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Test videosu başlatılamadı.");
    } finally {
      setStartingTest(false);
    }
  };

  const taskPanel = task ? (
    <section className="task-panel">
      <div>
        <span className="eyebrow">VİDEO ÜRETİMİ</span>
        <h2>{taskLabel(task.status)}</h2>
        <p>{task.error || (task.status === "completed" ? "MP4 hazır." : `Görev: ${task.taskId}`)}</p>
      </div>
      <div className="progress"><span style={{ width: `${task.status === "processing" && task.progress === 0 ? 35 : task.progress}%` }} /></div>
      {task.status === "completed" && task.videos.map((url) => <video key={url} controls src={url} />)}
    </section>
  ) : null;

  const dashboard = (
    <>
      <section className="hero">
        <div>
          <span className="eyebrow">POSTFLOW</span>
          <h1>Önce ilk gerçek MP4'ü alalım.</h1>
          <p>Karmaşık seçimleri kaldırdım. Bu ekranda tek üretim butonu var. Test; yerel materyal, Edge TTS, MoneyPrinterTurbo ve FFmpeg zincirini dener.</p>
          <button className="button primary" disabled={startingTest || activeTask || engine !== "connected"} onClick={startFirstTest}>
            {startingTest ? "Başlatılıyor…" : activeTask ? "Video üretiliyor…" : "▶ İlk Deneme Videosunu Oluştur"}
          </button>
          {engine !== "connected" && <small className="disabled-note">Önce yerel motorun HAZIR olması gerekiyor.</small>}
        </div>
        <div className={`hero-status ${engine}`}>
          <span>MOTOR</span>
          <strong>{engine === "connected" ? "HAZIR" : engine === "checking" ? "KONTROL" : "KAPALI"}</strong>
          <small>{engineDetail}</small>
        </div>
      </section>
      {taskPanel}
    </>
  );

  const studio = (
    <>
      <section className="page-heading">
        <span className="eyebrow">AI STUDIO</span>
        <h1>Sadece 2 adım.</h1>
        <p>1: İçeriği hazırla. 2: Videoyu oluştur. Başka üretim butonu yok.</p>
      </section>

      <section className="production-card">
        <div className="card-head"><span>01</span><div><h2>İçeriği Hazırla</h2><p>Senaryo ve sahneler hazırlanır. Video başlamaz.</p></div></div>
        <div className="field-pair">
          <label>Video türü<select value={workflow} onChange={(event) => resetForWorkflow(event.target.value as Workflow)}><option value="short">Kısa Video</option><option value="long">Uzun Video</option><option value="story">Hikaye Videosu</option><option value="quiz">Quiz Video</option></select></label>
          <label>Dil<select value={language} onChange={(event) => setLanguage(event.target.value)}><option value="tr">Türkçe</option><option value="en">English</option><option value="ar">العربية</option></select></label>
        </div>
        <label>Video konusu<textarea value={topic} onChange={(event) => setTopic(event.target.value)} placeholder="Örn. 30 saniyede şaşırtıcı bir teknoloji gerçeği" /></label>
        <label>Kategori<select value={genre} onChange={(event) => setGenre(event.target.value)}><option>Bilgi / Belgesel</option><option>Hikâye / Gerilim</option><option>Korku / Gerilim</option><option>Çocuk / Eğitici</option><option>Eğlence</option></select></label>
        <label>Ek talimat<input value={instruction} onChange={(event) => setInstruction(event.target.value)} placeholder="İstersen boş bırak" /></label>
        <button className="button primary full" disabled={planning || activeTask} onClick={createContent}>{planning ? "Hazırlanıyor…" : "1. İçeriği Hazırla"}</button>
      </section>

      {hasPlan && (
        <section className="production-card" style={{ marginTop: 18 }}>
          <div className="card-head"><span>02</span><div><h2>Videoyu Oluştur</h2><p>{settings.label} · {settings.aspect} · yaklaşık {settings.duration} sn</p></div></div>
          <label>İçerik özeti<textarea value={summary} onChange={(event) => setSummary(event.target.value)} /></label>
          <button className="button primary full" disabled={activeTask || engine !== "connected"} onClick={createVideo}>{activeTask ? "Video üretiliyor…" : "2. Videoyu Oluştur"}</button>
        </section>
      )}

      {taskPanel}
    </>
  );

  const ready = (
    <>
      <section className="page-heading"><span className="eyebrow">HAZIR VİDEOLAR</span><h1>MP4 çıktıları.</h1></section>
      <div className="video-grid">
        {readyVideos.map((video) => <article key={video.id}><video controls src={video.url} /><b>{video.title}</b><span>{video.createdAt}</span></article>)}
        {!readyVideos.length && <div className="empty-state"><h3>Henüz video yok</h3><p>Ana Sayfa'daki tek deneme butonuna basarak ilk MP4'ü üretin.</p></div>}
      </div>
    </>
  );

  const engineView = (
    <>
      <section className="page-heading"><span className="eyebrow">MOTOR</span><h1>{engine === "connected" ? "Üretime hazır." : "Bağlantı gerekli."}</h1><p>{engineDetail}</p></section>
      <button className="button secondary" onClick={() => void checkEngine()}>Bağlantıyı Kontrol Et</button>
    </>
  );

  const content = view === "dashboard" ? dashboard : view === "studio" ? studio : view === "ready" ? ready : engineView;

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <button className="brand" onClick={() => setView("dashboard")}><span>PF</span><div><b>PostFlow</b><small>Private Studio</small></div></button>
        <nav>
          <button className={view === "dashboard" ? "active" : ""} onClick={() => setView("dashboard")}><span>◈</span>Ana Sayfa</button>
          <button className={view === "studio" ? "active" : ""} onClick={() => setView("studio")}><span>＋</span>AI Studio</button>
          <button className={view === "ready" ? "active" : ""} onClick={() => setView("ready")}><span>▶</span>Hazır Videolar</button>
          <button className={view === "engine" ? "active" : ""} onClick={() => setView("engine")}><span>⚡</span>Motor</button>
        </nav>
        <div className={`engine-mini ${engine}`}><span>●</span><div><b>{engine === "connected" ? "Motor hazır" : "Motor kapalı"}</b><small>{engine === "connected" ? "Üretim yapılabilir" : "Başlatıcı gerekli"}</small></div></div>
      </aside>
      <section className="content-shell">
        <div className="content">
          {notice && <div className="notice"><span>{notice}</span><button onClick={() => setNotice("")}>×</button></div>}
          {content}
        </div>
      </section>
    </main>
  );
}
