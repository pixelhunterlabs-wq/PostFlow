"use client";
import { useEffect, useMemo, useState } from "react";

type Character={name:string;desc:string;file:string};
type Project={id:string;title:string;mode:string;topic:string;status:string;createdAt:string};
type Settings={language:string;voice:string;subtitle:boolean;music:boolean;material:string;aspect:string;duration:string};

const menu=["Dashboard","İçerik Radarı","AI Studio","Projeler","Hazır Videolar","Ayarlar"];
const modes=[
 {id:"short",icon:"⚡",title:"Kısa Video",sub:"TikTok · Reels · Shorts",ratio:"9:16",desc:"15–90 saniyelik hızlı sosyal medya videoları."},
 {id:"long",icon:"▶",title:"Uzun Video",sub:"YouTube",ratio:"16:9",desc:"5–20+ dakikalık bölümlü ve anlatımlı videolar."},
 {id:"story",icon:"✦",title:"Hikaye Videosu",sub:"Karakterli · Sahneli",ratio:"9:16 / 16:9",desc:"Sabit karakterler ve referans görsellerle hikaye üret."},
 {id:"radar",icon:"◎",title:"İçerik Radarı",sub:"Fikir keşfi",ratio:"Trend",desc:"Kaynakları tara, fikirleri puanla ve üretime gönder."}
];

const radarIdeas=[
 {title:"Gece vardiyasında güvenlik kamerasındaki anormallik",score:94,tag:"Korku / Gerilim"},
 {title:"Bir dakikada şaşırtıcı teknoloji hikayesi",score:89,tag:"Bilgi"},
 {title:"Terk edilmiş otelde bulunan gizemli telefon",score:87,tag:"Hikaye"},
 {title:"Günlük hayatta fark etmediğimiz 5 psikoloji detayı",score:84,tag:"Bilgi"},
];

export default function Home(){
 const [section,setSection]=useState("Dashboard");
 const [mode,setMode]=useState<string|null>(null);
 const [topic,setTopic]=useState("");
 const [ready,setReady]=useState(false);
 const [renderMsg,setRenderMsg]=useState("");
 const [chars,setChars]=useState<Character[]>([{name:"",desc:"",file:""}]);
 const [projects,setProjects]=useState<Project[]>([]);
 const [settings,setSettings]=useState<Settings>({language:"Türkçe",voice:"Otomatik seç",subtitle:true,music:true,material:"Stok + Yerel",aspect:"9:16",duration:"60 sn"});
 const [category,setCategory]=useState("Hikaye / Eğlence");

 useEffect(()=>{try{setProjects(JSON.parse(localStorage.getItem("postflow-projects")||"[]"));const s=localStorage.getItem("postflow-settings");if(s)setSettings(JSON.parse(s));}catch{}},[]);
 useEffect(()=>{localStorage.setItem("postflow-projects",JSON.stringify(projects))},[projects]);
 useEffect(()=>{localStorage.setItem("postflow-settings",JSON.stringify(settings))},[settings]);

 const activeMode=useMemo(()=>modes.find(x=>x.id===mode),[mode]);
 const resetStudio=()=>{setMode(null);setReady(false);setRenderMsg("");setTopic("");};
 const addChar=()=>setChars([...chars,{name:"",desc:"",file:""}]);
 const removeChar=(i:number)=>setChars(chars.filter((_,x)=>x!==i));
 const generate=()=>{if(!topic.trim())return;setReady(true);setRenderMsg("");};
 const saveProject=()=>{if(!topic.trim()||!mode)return;const p:Project={id:crypto.randomUUID(),title:topic.slice(0,55),mode:activeMode?.title||mode,topic,status:ready?"Plan hazır":"Taslak",createdAt:new Date().toLocaleString("tr-TR")};setProjects([p,...projects]);};
 const renderVideo=async()=>{setRenderMsg("Media Engine bağlantısı hazırlanıyor. Web arayüzü ve proje verisi hazır; final MP4 için render sunucusu bağlanmalı.");};

 if(mode==="radar") return <main><header className="topbar"><div><div className="brand">POSTFLOW</div><div className="muted">AI Video Production Studio</div></div><button className="ghost" onClick={resetStudio}>← Dashboard</button></header><section className="studio"><div className="eyebrow">İÇERİK RADARI</div><h1>Yeni içerik fikirlerini keşfet</h1><p className="lead">Fikir havuzunu puanla, seç ve doğrudan üretime gönder.</p><div className="radarGrid">{radarIdeas.map((r,i)=><article className="radarCard" key={i}><div><span className="score">{r.score}</span><b>{r.tag}</b></div><h3>{r.title}</h3><button className="ghost" onClick={()=>{setTopic(r.title);setMode("short")}}>Üretime Gönder →</button></article>)}</div></section></main>;

 if(mode&&mode!=="radar") return <main><header className="topbar"><div><div className="brand">POSTFLOW</div><div className="muted">AI Video Production Studio</div></div><button className="ghost" onClick={resetStudio}>← Dashboard</button></header><section className="studio">
  <div className="studioTitle"><div><div className="eyebrow">AI STUDIO</div><h1>{activeMode?.title}</h1><p>{activeMode?.sub}</p></div><span className="pill">{activeMode?.ratio}</span></div>
  <div className="formGrid">
   <label>Video Türü<select value={mode} onChange={e=>{setMode(e.target.value);setReady(false)}}><option value="short">Kısa Video</option><option value="long">Uzun Video</option><option value="story">Hikaye Videosu</option></select></label>
   <label>Kategori<select value={category} onChange={e=>setCategory(e.target.value)}><option>Hikaye / Eğlence</option><option>Korku / Gerilim</option><option>Bilgi</option><option>Motivasyon</option><option>Çocuk</option></select></label>
   <label>Dil<select value={settings.language} onChange={e=>setSettings({...settings,language:e.target.value})}><option>Türkçe</option><option>English</option><option>العربية</option></select></label>
   <label>Ses<select value={settings.voice} onChange={e=>setSettings({...settings,voice:e.target.value})}><option>Otomatik seç</option><option>Kadın</option><option>Erkek</option></select></label>
   <label>Format<select value={settings.aspect} onChange={e=>setSettings({...settings,aspect:e.target.value})}><option>9:16</option><option>16:9</option><option>1:1</option></select></label>
   <label>Süre<select value={settings.duration} onChange={e=>setSettings({...settings,duration:e.target.value})}><option>30 sn</option><option>60 sn</option><option>90 sn</option><option>5 dk</option><option>10 dk</option><option>20 dk</option></select></label>
  </div>

  {mode==="story"&&<section className="characters"><div className="sectionHead"><div><div className="eyebrow">KARAKTERLER</div><h2>Karakter ve referans görselleri</h2><p>Karakteri bir kez ekle; sahnelerde aynı kimliği korumak için referans olarak kullanılır.</p></div><button className="ghost" onClick={addChar}>+ Karakter Ekle</button></div>{chars.map((c,i)=><div className="charRow" key={i}><input placeholder="Karakter adı" value={c.name} onChange={e=>{const n=[...chars];n[i].name=e.target.value;setChars(n)}}/><input placeholder="Görünüş / kıyafet / özellikler" value={c.desc} onChange={e=>{const n=[...chars];n[i].desc=e.target.value;setChars(n)}}/><label className="upload">📷 {c.file||"Referans görsel yükle"}<input type="file" accept="image/*" onChange={e=>{const n=[...chars];n[i].file=e.target.files?.[0]?.name||"";setChars(n)}}/></label>{chars.length>1&&<button className="danger" onClick={()=>removeChar(i)}>Sil</button>}</div>)}</section>}

  <section className="creator"><div className="eyebrow">1. ADIM · İÇERİK PLANI</div><h2>Ne ile ilgili içerik üretmek istiyorsun?</h2><textarea value={topic} onChange={e=>setTopic(e.target.value)} placeholder="Konuyu, olay örgüsünü veya üretmek istediğin içeriği anlat..."/>
   <div className="switches"><label><input type="checkbox" checked={settings.subtitle} onChange={e=>setSettings({...settings,subtitle:e.target.checked})}/> Altyazı</label><label><input type="checkbox" checked={settings.music} onChange={e=>setSettings({...settings,music:e.target.checked})}/> Arka plan müziği</label><label>Materyal <select value={settings.material} onChange={e=>setSettings({...settings,material:e.target.value})}><option>Stok + Yerel</option><option>Yalnız Yerel</option><option>Pexels / Pixabay</option></select></label></div>
   <div className="actionsRow"><button className="ghost" onClick={saveProject}>Taslak Kaydet</button><button className="primary" disabled={!topic.trim()} onClick={generate}>Hikaye İçeriğini Oluştur</button></div>
   {ready&&<div className="blueprint"><div className="planHead"><b>✓ İçerik planı hazır</b><span>{mode==="long"?"8 bölüm / sahne":"6 sahne"}</span></div><p><b>Konu:</b> {topic}</p><p><b>Kategori:</b> {category} · <b>Dil:</b> {settings.language} · <b>Süre:</b> {settings.duration}</p>{mode==="story"&&<p><b>Karakterler:</b> {chars.filter(c=>c.name).map(c=>c.name).join(", ")||"Henüz isim verilmedi"}</p>}<div className="sceneList">{Array.from({length:mode==="long"?8:6}).map((_,i)=><div key={i}><span>{i+1}</span><p><b>Sahne {i+1}</b><br/>Konunun akışına uygun görsel, anlatım ve geçiş planı.</p></div>)}</div></div>}
   <div className="finalStage"><div><div className="eyebrow">2. ADIM · VİDEO ÜRETİMİ</div><h2>Final videoyu oluştur</h2><p>Seslendirme, altyazı, materyaller, karakter referansları ve müzik Media Engine üzerinden birleştirilir.</p></div><button className="videoBtn" disabled={!ready} onClick={renderVideo}>Videoyu Oluştur</button></div>
   {renderMsg&&<div className="notice">{renderMsg}</div>}
  </section>
 </section></main>;

 const renderSection=()=>{
  if(section==="Dashboard"||section==="AI Studio") return <div className="modeGrid">{modes.map(m=><button className="modeCard" key={m.id} onClick={()=>setMode(m.id)}><div className="modeIcon">{m.icon}</div><div className="modeTop"><h2>{m.title}</h2><span>{m.ratio}</span></div><b>{m.sub}</b><p>{m.desc}</p><div className="go">Başlat →</div></button>)}</div>;
  if(section==="İçerik Radarı") return <div className="radarGrid">{radarIdeas.map((r,i)=><article className="radarCard" key={i}><div><span className="score">{r.score}</span><b>{r.tag}</b></div><h3>{r.title}</h3><button className="ghost" onClick={()=>{setTopic(r.title);setMode("short")}}>Üretime Gönder →</button></article>)}</div>;
  if(section==="Projeler") return projects.length?<div className="tableList">{projects.map(p=><article key={p.id}><div><b>{p.title}</b><p>{p.mode} · {p.createdAt}</p></div><span>{p.status}</span><button className="danger" onClick={()=>setProjects(projects.filter(x=>x.id!==p.id))}>Sil</button></article>)}</div>:<div className="empty">Henüz kayıtlı proje yok. AI Studio’dan bir taslak kaydet.</div>;
  if(section==="Hazır Videolar") return <div className="empty">Render edilen videolar burada görünecek. Media Engine sunucusu bağlandıktan sonra MP4 çıktıları bu ekranda listelenecek.</div>;
  if(section==="Ayarlar") return <div className="settingsGrid"><label>Varsayılan dil<select value={settings.language} onChange={e=>setSettings({...settings,language:e.target.value})}><option>Türkçe</option><option>English</option><option>العربية</option></select></label><label>Varsayılan ses<select value={settings.voice} onChange={e=>setSettings({...settings,voice:e.target.value})}><option>Otomatik seç</option><option>Kadın</option><option>Erkek</option></select></label><label>Varsayılan materyal<select value={settings.material} onChange={e=>setSettings({...settings,material:e.target.value})}><option>Stok + Yerel</option><option>Yalnız Yerel</option><option>Pexels / Pixabay</option></select></label><div className="toggleBox"><label><input type="checkbox" checked={settings.subtitle} onChange={e=>setSettings({...settings,subtitle:e.target.checked})}/> Altyazı varsayılan açık</label><label><input type="checkbox" checked={settings.music} onChange={e=>setSettings({...settings,music:e.target.checked})}/> Müzik varsayılan açık</label></div><div className="engineStatus"><b>Media Engine</b><span className="offline">Bağlantı bekleniyor</span><p>FastAPI + FFmpeg render motoru repo içinde hazır. Web’den erişilebilir sunucuya deploy edilmesi gerekiyor.</p></div></div>;
  return null;
 };

 return <main><aside className="sidebar"><div className="brand sideBrand">POSTFLOW</div>{menu.map(x=><button key={x} className={section===x?"nav active":"nav"} onClick={()=>setSection(x)}>{x}</button>)}</aside><div className="shell"><header className="mobileTop"><div className="brand">POSTFLOW</div><div className="status"><span/> Web aktif</div></header><section className="dash"><div className="eyebrow">{section.toUpperCase()}</div><h1>{section==="Dashboard"?"Bugün ne üretmek istiyorsun?":section}</h1><p className="lead">{section==="Dashboard"?"Video türünü seç ve PostFlow üretim akışını başlat.":"PostFlow çalışma alanı."}</p>{renderSection()}</section></div></main>;
}