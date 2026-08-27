"use client";

import { useState } from "react";

const steps = [
  { n: "1", title: "İçerik Radarı", text: "Kaynakları tara, içerikleri puanla ve en güçlü fikri seç." },
  { n: "2", title: "Hikaye Oluştur", text: "Konuyu yaz, senaryo ve sahneleri otomatik hazırla." },
  { n: "3", title: "Videoyu Oluştur", text: "Ses, altyazı, görsel ve müzikle final videoyu üret." },
];

export default function Home() {
  const [topic, setTopic] = useState("");
  const [generated, setGenerated] = useState(false);

  return (
    <main>
      <header className="topbar">
        <div>
          <div className="brand">POSTFLOW</div>
          <div className="muted">AI Video Production Studio</div>
        </div>
        <div className="status"><span /> Web sürümü aktif</div>
      </header>

      <section className="hero">
        <div className="eyebrow">İÇERİKTEN VİDEOYA TEK AKIŞ</div>
        <h1>Fikri bul. Hikayeyi oluştur. Videoyu üret.</h1>
        <p>PostFlow, içerik keşfinden sahne planlamaya ve video üretimine kadar süreci tek panelde toplar.</p>
      </section>

      <section className="steps">
        {steps.map((step) => (
          <article className="step" key={step.n}>
            <div className="stepNo">{step.n}</div>
            <h2>{step.title}</h2>
            <p>{step.text}</p>
          </article>
        ))}
      </section>

      <section className="workspace">
        <div className="workspaceHead">
          <div>
            <div className="eyebrow">2. HİKAYE OLUŞTUR</div>
            <h2>Ne ile ilgili içerik üretmek istiyorsun?</h2>
          </div>
          <div className="pill">9:16 · Shorts / Reels</div>
        </div>

        <textarea
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
          placeholder="Örn: Terk edilmiş bir otelde gece vardiyasında yaşanan gizemli olay..."
        />
        <div className="actions">
          <button className="primary" onClick={() => setGenerated(Boolean(topic.trim()))}>Hikaye İçeriğini Oluştur</button>
        </div>

        <div className={`storyPreview ${generated ? "visible" : ""}`}>
          <div className="previewTitle">Hikaye taslağı</div>
          <p>{generated ? `“${topic}” konusu için senaryo ve sahne planı hazır. Media Engine bağlantısı açıldığında ses, altyazı ve materyal üretimi bu akışa bağlanacak.` : ""}</p>
        </div>

        <div className="videoStage">
          <div>
            <div className="eyebrow">3. VİDEO ÜRETİMİ</div>
            <h3>Final üretime hazır</h3>
            <p>Seslendirme, altyazı, materyal ve arka plan müziği Media Engine üzerinden birleştirilecek.</p>
          </div>
          <button className="videoBtn" disabled={!generated}>Videoyu Oluştur</button>
        </div>
      </section>

      <footer>PostFlow · Web V1</footer>
    </main>
  );
}
