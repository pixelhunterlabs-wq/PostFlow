"use client";

import { useEffect, useMemo, useState } from "react";

type View = "dashboard" | "create" | "radar" | "projects" | "characters" | "ready" | "engine" | "settings";
type Workflow = "short" | "long" | "story" | "quiz";
type EngineState = "checking" | "connected" | "offline";
type TaskStatus = "queued" | "processing" | "completed" | "failed";
type MaterialSource = "local" | "pexels" | "pixabay";
type Aspect = "9:16" | "16:9" | "1:1";

type Character = {
  id: string;
  name: string;
  referenceImageData: string;
  referenceImageName: string;
  face: string;
  hair: string;
  ageAppearance: string;
  bodyType: string;
  outfit: string;
  styleNotes: string;
  negativePrompt: string;
};

type Scene = {
  id: string;
  number: number;
  text: string;
  visualPrompt: string;
  characters: string[];
};

type Project = {
  id: string;
  title: string;
  workflow: Workflow;
  topic: string;
  status: string;
  createdAt: string;
  summary: string;
};

type ReadyVideo = {
  id: string;
  taskId: string;
  title: string;
  url: string;
  createdAt: string;
};

type MptTask = {
  taskId: string;
  status: TaskStatus;
  progress: number;
  videos: string[];
  error?: string;
  failedStage?: string;
};

type StoryResponse = {
  title: string;
  summary: string;
  script: string;
  scenes: Array<{ number: number; text: string; visualPrompt: string; characters: string[] }>;
  error?: string;
};

const nav: Array<{ id: View; label: string; icon: string }> = [
  { id: "dashboard", label: "Kontrol Paneli", icon: "◈" },
  { id: "create", label: "AI Studio", icon: "＋" },
  { id: "radar", label: "İçerik Radarı", icon: "⌁" },
  { id: "projects", label: "Projeler", icon: "▣" },
  { id: "characters", label: "Karakterler", icon: "◉" },
  { id: "ready", label: "Hazır Videolar", icon: "▶" },
  { id: "engine", label: "Motor Durumu", icon: "⚡" },
  { id: "settings", label: "Ayarlar", icon: "⚙" },
];

const workflowCards: Array<{ id: Workflow; title: string; description: string; badge: string; aspect: Aspect; duration: number }> = [
  { id: "short", title: "Kısa Video", description: "TikTok, Reels ve Shorts için hızlı dikey içerik", badge: "9:16", aspect: "9:16", duration: 45 },
  { id: "long", title: "Uzun Video", description: "YouTube için bölümlü anlatım ve uzun senaryo", badge: "16:9", aspect: "16:9", duration: 300 },
  { id: "story", title: "Hikaye Videosu", description: "Karakter Bible destekli sahneli hikâye", badge: "SERİ", aspect: "9:16", duration: 60 },
  { id: "quiz", title: "Quiz Video", description: "Soru, cevap ve etkileşim odaklı kısa format", badge: "QUIZ", aspect: "9:16", duration: 45 },
];

const radarIdeas = [
  { title: "Gece vardiyasında kameraya yansıyan garip olay", category: "Korku / Gerilim", score: 94 },
  { title: "30 saniyede şaşırtıcı teknoloji gerçeği", category: "Bilgi", score: 89 },
  { title: "Bir mesajla başlayan kısa ilişki hikâyesi", category: "Hikâye", score: 86 },
  { title: "Çocuklar için 5 hızlı hayvan bilmecesi", category: "Çocuk / Eğitici", score: 82 },
];

const durationOptions = [30, 45, 60, 90, 300, 600, 1200];
const sceneOptions = [2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24];

function makeCharacter(): Character {
  return {
    id: crypto.randomUUID(), name: "", referenceImageData: "", referenceImageName: "", face: "", hair: "",
    ageAppearance: "", bodyType: "", outfit: "", styleNotes: "", negativePrompt: "",
  };
}

function durationLabel(seconds: number) {
  if (seconds < 60) return `${seconds} sn`;
  return `${Math.round(seconds / 60)} dk`;
}

function workflowLabel(workflow: Workflow) {
  return workflow === "short" ? "Kısa Video" : workflow === "long" ? "Uzun Video" : workflow === "story" ? "Hikaye Videosu" : "Quiz Video";
}

function taskLabel(status: TaskStatus) {
  return status === "queued" ? "Beklemede" : status === "processing" ? "İşleniyor" : status === "completed" ? "Tamamlandı" : "Hata";
}

export default function Home() {
  const [view, setView] = useState<View>("dashboard");
  const [workflow, setWorkflow] = useState<Workflow>("story");
  const [engine, setEngine] = useState<EngineState>("checking");
  const [engineDetail, setEngineDetail] = useState("Yerel motor kontrol ediliyor…");
  const [pipeline, setPipeline] = useState("Qwen 2.5 3B + Edge TTS + local media + FFmpeg");
  const [topic, setTopic] = useState("");
  const [genre, setGenre] = useState("Hikâye / Gerilim");
  const [instruction, setInstruction] = useState("");
  const [sceneCount, setSceneCount] = useState(6);
  const [duration, setDuration] = useState(60);
  const [aspect, setAspect] = useState<Aspect>("9:16");
  const [language, setLanguage] = useState("tr");
  const [voice, setVoice] = useState("Otomatik");
  const [materialSource, setMaterialSource] = useState<MaterialSource>("local");
  const [subtitle, setSubtitle] = useState(true);
  const [summary, setSummary] = useState("");
  const [script, setScript] = useState("");
  const [scenes, setScenes] = useState<Scene[]>([]);
  const [characters, setCharacters] = useState<Character[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [readyVideos, setReadyVideos] = useState<ReadyVideo[]>([]);
  const [mptTask, setMptTask] = useState<MptTask | null>(null);
  const [notice, setNotice] = useState("");
  const [planning, setPlanning] = useState(false);
  const [hasHydrated, setHasHydrated] = useState(false);

  const activeTask = Boolean(mptTask && !["completed", "failed"].includes(mptTask.status));
  const hasPlan = scenes.length > 0 && Boolean(script.trim());
  const currentWorkflow = useMemo(() => workflowCards.find((item) => item.id === workflow) ?? workflowCards[2], [workflow]);

  const checkEngine = async () => {
    setEngine("checking");
    try {
      const response = await fetch("/api/local/mpt/health", { cache: "no-store" });
      const data = await response.json() as { ok?: boolean; apiUrl?: string; pipeline?: string; error?: string };
      if (!response.ok || !data.ok) throw new Error(data.error || "Yerel motora bağlanılamadı.");
      setEngine("connected");
      setEngineDetail(`Bağlı · ${data.apiUrl ?? "127.0.0.1:8080"}`);
      if (data.pipeline) setPipeline(data.pipeline);
    } catch (error) {
      setEngine("offline");
      setEngineDetail(error instanceof Error ? error.message : "Yerel motora bağlanılamadı.");
    }
  };

  useEffect(() => {
    try {
      setCharacters(JSON.parse(localStorage.getItem("postflow-character-bible") || "[]") as Character[]);
      setProjects(JSON.parse(localStorage.getItem("postflow-projects") || "[]") as Project[]);
      setReadyVideos(JSON.parse(localStorage.getItem("postflow-ready-videos") || "[]") as ReadyVideo[]);
      setMptTask(JSON.parse(localStorage.getItem("postflow-active-mpt-task") || "null") as MptTask | null);
      const settings = JSON.parse(localStorage.getItem("postflow-settings") || "{}") as { materialSource?: MaterialSource; voice?: string; language?: string };
      if (settings.materialSource) setMaterialSource(settings.materialSource);
      if (settings.voice) setVoice(settings.voice);
      if (settings.language) setLanguage(settings.language);
    } catch {
      setNotice("Yerel kayıtlar okunamadı; temiz çalışma alanı açıldı.");
    } finally {
      setHasHydrated(true);
    }
    void checkEngine();
  }, []);

  useEffect(() => { if (hasHydrated) localStorage.setItem("postflow-character-bible", JSON.stringify(characters)); }, [characters, hasHydrated]);
  useEffect(() => { if (hasHydrated) localStorage.setItem("postflow-projects", JSON.stringify(projects)); }, [projects, hasHydrated]);
  useEffect(() => { if (hasHydrated) localStorage.setItem("postflow-ready-videos", JSON.stringify(readyVideos)); }, [readyVideos, hasHydrated]);
  useEffect(() => {
    if (!hasHydrated) return;
    localStorage.setItem("postflow-settings", JSON.stringify({ materialSource, voice, language }));
  }, [materialSource, voice, language, hasHydrated]);
  useEffect(() => {
    if (!hasHydrated) return;
    if (mptTask) localStorage.setItem("postflow-active-mpt-task", JSON.stringify(mptTask));
    else localStorage.removeItem("postflow-active-mpt-task");
  }, [mptTask, hasHydrated]);

  useEffect(() => {
    const interval = window.setInterval(() => void checkEngine(), 20000);
    return () => window.clearInterval(interval);
  }, []);

  useEffect(() => {
    if (!mptTask || !activeTask) return;
    let cancelled = false;
    const timer = window.setTimeout(async () => {
      try {
        const response = await fetch(`/api/local/mpt/jobs/${encodeURIComponent(mptTask.taskId)}`, { cache: "no-store" });
        const data = await response.json() as MptTask & { error?: string };
        if (!response.ok) throw new Error(data.error || "Üretim görevi okunamadı.");
        if (!cancelled) setMptTask(data);
      } catch (error) {
        if (!cancelled) setMptTask((current) => current ? { ...current, status: "failed", error: error instanceof Error ? error.message : "Görev durumu okunamadı." } : current);
      }
    }, 2500);
    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [mptTask, activeTask]);

  useEffect(() => {
    if (!mptTask || mptTask.status !== "completed" || !mptTask.videos.length) return;
    setReadyVideos((current) => {
      const additions = mptTask.videos
        .filter((url) => !current.some((item) => item.taskId === mptTask.taskId && item.url === url))
        .map((url, index) => ({ id: `${mptTask.taskId}-${index}`, taskId: mptTask.taskId, title: topic.trim() || "PostFlow Video", url, createdAt: new Date().toLocaleString("tr-TR") }));
      return additions.length ? [...additions, ...current] : current;
    });
    setProjects((current) => current.map((project) => project.topic === topic.trim() ? { ...project, status: "Video hazır" } : project));
    setNotice("Video üretimi tamamlandı. Hazır Videolar bölümünden izleyebilirsiniz.");
  }, [mptTask, topic]);

  const chooseWorkflow = (id: Workflow) => {
    const selected = workflowCards.find((item) => item.id === id) ?? workflowCards[2];
    setWorkflow(id);
    setAspect(selected.aspect);
    setDuration(selected.duration);
    setSceneCount(id === "long" ? 12 : id === "story" ? 6 : 4);
    setScenes([]);
    setSummary("");
    setScript("");
    setMptTask(null);
    setView("create");
  };

  const updateCharacter = (id: string, patch: Partial<Character>) => {
    setCharacters((current) => current.map((character) => character.id === id ? { ...character, ...patch } : character));
  };

  const handleReferenceImage = (id: string, file?: File) => {
    if (!file) return;
    if (!file.type.startsWith("image/")) { setNotice("Karakter referansı için bir görsel dosyası seçin."); return; }
    if (file.size > 1_500_000) { setNotice("Referans görseli tarayıcıda saklamak için 1.5 MB'dan küçük bir dosya seçin."); return; }
    const reader = new FileReader();
    reader.onload = () => updateCharacter(id, { referenceImageData: String(reader.result || ""), referenceImageName: file.name });
    reader.readAsDataURL(file);
  };

  const createContent = async () => {
    if (!topic.trim()) { setNotice("Önce ne hakkında içerik üretmek istediğinizi yazın."); return; }
    setPlanning(true);
    setNotice("Yerel Qwen modeli senaryo ve sahneleri hazırlıyor…");
    try {
      const characterBible = characters
        .filter((character) => character.name.trim())
        .map(({ name, face, hair, ageAppearance, bodyType, outfit, styleNotes, negativePrompt }) => ({ name, face, hair, ageAppearance, bodyType, outfit, styleNotes, negativePrompt }));
      const response = await fetch("/api/local/mpt/story", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ subject: topic.trim(), language, sceneCount, genre, instruction, durationSeconds: duration, characterBible }),
      });
      const data = await response.json() as StoryResponse;
      if (!response.ok || !data.script || !Array.isArray(data.scenes)) throw new Error(data.error || "Senaryo oluşturulamadı.");
      setSummary(data.summary);
      setScript(data.script);
      setScenes(data.scenes.map((scene) => ({ ...scene, id: crypto.randomUUID() })));
      const project: Project = {
        id: crypto.randomUUID(), title: data.title || topic.trim().slice(0, 60), workflow, topic: topic.trim(), status: "İçerik planı hazır",
        createdAt: new Date().toLocaleString("tr-TR"), summary: data.summary,
      };
      setProjects((current) => [project, ...current.filter((item) => item.topic !== project.topic)]);
      setNotice("İçerik hazır. Sahneleri kontrol edin; sonrasında Videoyu Oluştur'a basın.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "İçerik oluşturulamadı.");
    } finally {
      setPlanning(false);
    }
  };

  const createVideo = async () => {
    if (!hasPlan || !topic.trim()) { setNotice("Önce 1. adımda içerik planını oluşturun."); return; }
    setNotice("Yerel video motoruna üretim görevi gönderiliyor…");
    try {
      const response = await fetch("/api/local/mpt/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ subject: topic.trim(), script, aspect, language, voice, materialSource, subtitleEnabled: subtitle }),
      });
      const data = await response.json() as { taskId?: string; error?: string };
      if (!response.ok || !data.taskId) throw new Error(data.error || "Video üretim görevi oluşturulamadı.");
      setMptTask({ taskId: data.taskId, status: "queued", progress: 0, videos: [] });
      setProjects((current) => current.map((project) => project.topic === topic.trim() ? { ...project, status: "Video üretiliyor" } : project));
      setNotice("Video üretimi başladı. Bu sayfada ilerlemeyi takip edebilirsiniz.");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "Video üretimi başlatılamadı.");
    }
  };

  const dashboard = (
    <>
      <section className="hero">
        <div>
          <span className="eyebrow">POSTFLOW · LOCAL AI VIDEO STUDIO</span>
          <h1>Fikirden gerçek MP4 videoya tek akış.</h1>
          <p>Qwen senaryoyu hazırlıyor, Edge TTS seslendiriyor, MoneyPrinterTurbo ve FFmpeg yerel materyallerden videoyu oluşturuyor.</p>
          <button className="button primary" onClick={() => chooseWorkflow("story")}>Yeni Video Oluştur <span>→</span></button>
        </div>
        <div className={`hero-status ${engine}`}>
          <span>MOTOR DURUMU</span>
          <strong>{engine === "connected" ? "HAZIR" : engine === "checking" ? "KONTROL" : "BAĞLANTI YOK"}</strong>
          <small>{engineDetail}</small>
        </div>
      </section>

      <section className="metric-grid">
        <article><small>Projeler</small><strong>{projects.length}</strong><span>Yerel kayıt</span></article>
        <article><small>Karakterler</small><strong>{characters.length}</strong><span>Character Bible</span></article>
        <article><small>Hazır videolar</small><strong>{readyVideos.length}</strong><span>MP4 çıktıları</span></article>
        <article><small>API anahtarı</small><strong>0</strong><span>Yerel akış için gerekmiyor</span></article>
      </section>

      <section>
        <div className="section-title"><div><span className="eyebrow">VİDEO TÜRLERİ</span><h2>Ne üretmek istiyorsun?</h2></div></div>
        <div className="workflow-grid">
          {workflowCards.map((item) => (
            <button className="workflow-card" key={item.id} onClick={() => chooseWorkflow(item.id)}>
              <span>{item.badge}</span><h3>{item.title}</h3><p>{item.description}</p><b>Studio'yu aç →</b>
            </button>
          ))}
        </div>
      </section>
    </>
  );

  const create = (
    <>
      <section className="page-heading">
        <span className="eyebrow">AI STUDIO · {workflowLabel(workflow).toUpperCase()}</span>
        <h1>İki adım: içeriği hazırla, videoyu üret.</h1>
        <p>Aradaki gereksiz üret butonları kaldırıldı. İlk adım yalnız senaryoyu ve sahneleri hazırlar; ikinci adım gerçek MP4 renderını başlatır.</p>
      </section>

      <div className="studio-status-row">
        <span className={`status-pill ${engine}`}>{engine === "connected" ? "● Yerel motor bağlı" : "● Yerel motor bağlı değil"}</span>
        <span className="status-pill">Model: qwen2.5:3b</span>
        <span className="status-pill">TTS: Edge TTS</span>
        <span className="status-pill">Kaynak: {materialSource === "local" ? "Yerel" : materialSource}</span>
      </div>

      <div className="production-grid">
        <section className="production-card">
          <div className="card-head"><span>01</span><div><h2>{workflow === "story" ? "Hikaye İçeriğini Oluştur" : "Video İçeriğini Oluştur"}</h2><p>Qwen senaryoyu ve sahne planını hazırlar. Render başlamaz.</p></div></div>
          <label>Ne ile ilgili içerik üretmek istiyorsun?<textarea value={topic} onChange={(event) => setTopic(event.target.value)} placeholder="Örn. Gece vardiyasındaki güvenlik görevlisinin kamerada gördüğü garip olay…" /></label>
          <div className="field-pair">
            <label>Kategori<select value={genre} onChange={(event) => setGenre(event.target.value)}><option>Hikâye / Gerilim</option><option>Korku / Gerilim</option><option>Bilgi / Belgesel</option><option>Motivasyon</option><option>Çocuk / Eğitici</option><option>Eğlence</option></select></label>
            <label>Dil<select value={language} onChange={(event) => setLanguage(event.target.value)}><option value="tr">Türkçe</option><option value="en">English</option><option value="ar">العربية</option></select></label>
          </div>
          <div className="field-pair">
            <label>Sahne sayısı<select value={sceneCount} onChange={(event) => setSceneCount(Number(event.target.value))}>{sceneOptions.map((count) => <option key={count} value={count}>{count} sahne</option>)}</select></label>
            <label>Hedef süre<select value={duration} onChange={(event) => setDuration(Number(event.target.value))}>{durationOptions.map((seconds) => <option key={seconds} value={seconds}>{durationLabel(seconds)}</option>)}</select></label>
          </div>
          <label>Ek talimatlar<input value={instruction} onChange={(event) => setInstruction(event.target.value)} placeholder="Örn. İlk 3 saniye güçlü kanca, sonunda merak bıraksın…" /></label>
          {characters.length > 0 && <div className="helper-box"><b>Character Bible aktif</b><span>{characters.filter((item) => item.name.trim()).map((item) => item.name).join(", ") || "Karakter adı bekleniyor"}</span></div>}
          <button className="button primary full" disabled={planning} onClick={createContent}>{planning ? "İçerik hazırlanıyor…" : workflow === "story" ? "Hikaye İçeriğini Oluştur" : "Video İçeriğini Oluştur"}</button>
        </section>

        <section className={`production-card ${!hasPlan ? "muted-card" : ""}`}>
          <div className="card-head"><span>02</span><div><h2>Videoyu Oluştur</h2><p>Bu buton gerçek MoneyPrinterTurbo + FFmpeg üretimini başlatır.</p></div></div>
          <label>İçerik özeti<textarea value={summary} onChange={(event) => setSummary(event.target.value)} placeholder="1. adım tamamlandığında oluşur." /></label>
          <div className="field-pair">
            <label>Format<select value={aspect} onChange={(event) => setAspect(event.target.value as Aspect)}><option>9:16</option><option>16:9</option><option>1:1</option></select></label>
            <label>Ses<select value={voice} onChange={(event) => setVoice(event.target.value)}><option>Otomatik</option><option>Kadın</option><option>Erkek</option></select></label>
          </div>
          <label>Video materyali<select value={materialSource} onChange={(event) => setMaterialSource(event.target.value as MaterialSource)}><option value="local">Yerel materyaller — API anahtarı yok</option><option value="pexels">Pexels — anahtar gerekebilir</option><option value="pixabay">Pixabay — anahtar gerekebilir</option></select></label>
          <label className="toggle"><input type="checkbox" checked={subtitle} onChange={(event) => setSubtitle(event.target.checked)} /><span>Otomatik altyazı</span></label>
          <button className="button primary full" disabled={!hasPlan || activeTask || engine !== "connected"} onClick={createVideo}>{activeTask ? "Video üretiliyor…" : "Videoyu Oluştur"}</button>
          {!hasPlan && <small className="disabled-note">Önce 1. adımı tamamlayın.</small>}
          {engine !== "connected" && <small className="disabled-note">Render için PostFlow Yerel Başlatıcı açık olmalı.</small>}
        </section>
      </div>

      {scenes.length > 0 && <section className="scene-section"><div className="section-title"><div><span className="eyebrow">SAHNE PLANI</span><h2>{scenes.length} sahne hazır</h2></div></div><div className="scene-list">{scenes.map((scene) => <article className="scene-card" key={scene.id}><span>{String(scene.number).padStart(2, "0")}</span><div><textarea value={scene.text} onChange={(event) => { const text = event.target.value; setScenes((current) => current.map((item) => item.id === scene.id ? { ...item, text } : item)); setScript((current) => current); }} /><small>{scene.visualPrompt}</small></div></article>)}</div><button className="text-button" onClick={() => setScript(scenes.map((scene) => scene.text).join("\n\n"))}>Sahne değişikliklerini senaryoya uygula</button></section>}

      {mptTask && <section className="task-panel"><div><span className="eyebrow">ÜRETİM GÖREVİ</span><h2>{taskLabel(mptTask.status)}</h2><p>{mptTask.failedStage ? `${mptTask.failedStage}: ` : ""}{mptTask.error || `Görev: ${mptTask.taskId}`}</p></div><div className="progress"><span style={{ width: `${mptTask.status === "processing" && mptTask.progress === 0 ? 35 : mptTask.progress}%` }} /></div>{mptTask.status === "completed" && mptTask.videos.map((url) => <video key={url} controls src={url} />)}</section>}
    </>
  );

  const charactersView = (
    <>
      <section className="page-heading"><span className="eyebrow">CHARACTER BIBLE</span><h1>Karakterleri bir kez tanımla.</h1><p>İsim, görünüm, kıyafet ve referans görsel burada saklanır. Metinsel karakter bilgileri Qwen'in sahne planına aktarılır. Referans görsel MPT tarafından kimlik kilidi olarak kullanılmaz.</p></section>
      <button className="button primary" onClick={() => setCharacters((current) => [...current, makeCharacter()])}>＋ Karakter Ekle</button>
      <div className="character-grid">{characters.map((character) => <article className="character-card" key={character.id}><div className="character-preview">{character.referenceImageData ? <img src={character.referenceImageData} alt={character.name || "Karakter referansı"} /> : <span>Referans görsel</span>}</div><label>Karakter adı<input value={character.name} onChange={(event) => updateCharacter(character.id, { name: event.target.value })} /></label><label>Referans görsel<input type="file" accept="image/*" onChange={(event) => handleReferenceImage(character.id, event.target.files?.[0])} /></label>{character.referenceImageName && <small>{character.referenceImageName}</small>}<div className="field-pair"><label>Yüz<input value={character.face} onChange={(event) => updateCharacter(character.id, { face: event.target.value })} placeholder="Yüz şekli, gözler…" /></label><label>Saç<input value={character.hair} onChange={(event) => updateCharacter(character.id, { hair: event.target.value })} /></label></div><div className="field-pair"><label>Yaş görünümü<input value={character.ageAppearance} onChange={(event) => updateCharacter(character.id, { ageAppearance: event.target.value })} /></label><label>Vücut tipi<input value={character.bodyType} onChange={(event) => updateCharacter(character.id, { bodyType: event.target.value })} /></label></div><label>Kıyafet<input value={character.outfit} onChange={(event) => updateCharacter(character.id, { outfit: event.target.value })} /></label><label>Stil notları<textarea value={character.styleNotes} onChange={(event) => updateCharacter(character.id, { styleNotes: event.target.value })} /></label><label>Kaçınılacaklar<input value={character.negativePrompt} onChange={(event) => updateCharacter(character.id, { negativePrompt: event.target.value })} /></label><button className="danger-button" onClick={() => setCharacters((current) => current.filter((item) => item.id !== character.id))}>Karakteri Sil</button></article>)}</div>
      {!characters.length && <div className="empty-state"><h3>Henüz karakter yok</h3><p>Hikaye serilerinde karakter sürekliliği için ilk karakterinizi ekleyin.</p></div>}
    </>
  );

  const projectsView = <><section className="page-heading"><span className="eyebrow">PROJELER</span><h1>Üretim geçmişi.</h1><p>Senaryo planları bu tarayıcıda saklanır.</p></section><div className="list-panel">{projects.map((project) => <article key={project.id}><div><b>{project.title}</b><span>{workflowLabel(project.workflow)} · {project.createdAt}</span></div><strong>{project.status}</strong></article>)}{!projects.length && <p>Henüz proje yok.</p>}</div></>;

  const readyView = <><section className="page-heading"><span className="eyebrow">HAZIR VİDEOLAR</span><h1>Render çıktıları.</h1><p>Yerel MPT görevinden dönen tamamlanmış MP4 videolar burada görünür.</p></section><div className="video-grid">{readyVideos.map((video) => <article key={video.id}><video controls src={video.url} /><b>{video.title}</b><span>{video.createdAt}</span></article>)}{!readyVideos.length && <div className="empty-state"><h3>Henüz hazır video yok</h3><p>AI Studio'da iki adımı tamamladığınızda MP4 burada görünecek.</p></div>}</div></>;

  const radarView = <><section className="page-heading"><span className="eyebrow">İÇERİK RADARI</span><h1>Üretime hazır fikirler.</h1><p>Bu bölüm şu anda yerel örnek fikir havuzudur; canlı web taraması bağlı değildir.</p></section><div className="radar-grid">{radarIdeas.map((idea) => <article key={idea.title}><span className="score">{idea.score}</span><div><small>{idea.category}</small><h3>{idea.title}</h3><button className="text-button" onClick={() => { setTopic(idea.title); setGenre(idea.category); setWorkflow("short"); setDuration(45); setSceneCount(4); setView("create"); }}>Üretime Gönder →</button></div></article>)}</div></>;

  const engineView = <><section className="page-heading"><span className="eyebrow">YEREL MOTOR</span><h1>{engine === "connected" ? "PostFlow üretime hazır." : "Yerel motor bağlantısı gerekli."}</h1><p>{engineDetail}</p></section><div className="engine-grid"><article><small>LLM</small><strong>qwen2.5:3b</strong><span>Ollama · yerel</span></article><article><small>TTS</small><strong>Edge TTS</strong><span>tr-TR-AhmetNeural otomatik</span></article><article><small>Video</small><strong>MoneyPrinterTurbo</strong><span>FFmpeg · H.264 + AAC</span></article><article><small>API Key</small><strong>Gerekmez</strong><span>Yerel materyal akışında</span></article></div><div className="info-panel"><h3>Çalışan zincir</h3><p>{pipeline}</p><button className="button secondary" onClick={() => void checkEngine()}>Bağlantıyı Yeniden Kontrol Et</button></div></>;

  const settingsView = <><section className="page-heading"><span className="eyebrow">AYARLAR</span><h1>Varsayılan üretim tercihleri.</h1><p>Bu ayarlar tarayıcıda saklanır ve yeni işlerde kullanılır.</p></section><section className="settings-card"><label>Varsayılan dil<select value={language} onChange={(event) => setLanguage(event.target.value)}><option value="tr">Türkçe</option><option value="en">English</option><option value="ar">العربية</option></select></label><label>Varsayılan ses<select value={voice} onChange={(event) => setVoice(event.target.value)}><option>Otomatik</option><option>Kadın</option><option>Erkek</option></select></label><label>Varsayılan materyal kaynağı<select value={materialSource} onChange={(event) => setMaterialSource(event.target.value as MaterialSource)}><option value="local">Yerel — ücretsiz</option><option value="pexels">Pexels</option><option value="pixabay">Pixabay</option></select></label><div className="helper-box"><b>Yerel üretim modu</b><span>Vercel sitesi arayüzü gösterebilir; gerçek render bilgisayarınızdaki motor nedeniyle Yerel Başlatıcı üzerinden çalışır.</span></div></section></>;

  const content = view === "dashboard" ? dashboard : view === "create" ? create : view === "radar" ? radarView : view === "projects" ? projectsView : view === "characters" ? charactersView : view === "ready" ? readyView : view === "engine" ? engineView : settingsView;

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <button className="brand" onClick={() => setView("dashboard")}><span>PF</span><div><b>PostFlow</b><small>Local AI Studio</small></div></button>
        <nav>{nav.map((item) => <button key={item.id} className={view === item.id ? "active" : ""} onClick={() => setView(item.id)}><span>{item.icon}</span>{item.label}</button>)}</nav>
        <div className={`engine-mini ${engine}`}><span>●</span><div><b>{engine === "connected" ? "Motor hazır" : engine === "checking" ? "Kontrol ediliyor" : "Motor kapalı"}</b><small>{engine === "connected" ? "Üretim yapılabilir" : "Yerel Başlatıcı gerekli"}</small></div></div>
      </aside>
      <section className="content-shell"><header className="topbar"><div><span className={`top-dot ${engine}`} /> <b>{currentWorkflow.title}</b></div><button className="button small" onClick={() => chooseWorkflow("story")}>＋ Yeni Video</button></header><div className="content">{notice && <div className="notice"><span>{notice}</span><button onClick={() => setNotice("")}>×</button></div>}{content}</div></section>
    </main>
  );
}
