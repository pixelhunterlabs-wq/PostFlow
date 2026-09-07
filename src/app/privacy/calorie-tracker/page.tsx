export const metadata = {
  title: "Kalori Takip - Gizlilik Politikası",
  description: "Kalori Takip uygulaması gizlilik politikası ve veri silme bilgileri.",
};

export default function CalorieTrackerPrivacyPage() {
  return (
    <main style={{ minHeight: "100vh", background: "#08110f", color: "#f3f7f5", padding: "48px 20px" }}>
      <article style={{ maxWidth: 760, margin: "0 auto", lineHeight: 1.7 }}>
        <h1 style={{ fontSize: 34, marginBottom: 8 }}>Kalori Takip Gizlilik Politikası</h1>
        <p style={{ color: "#9da9a5" }}><strong style={{ color: "#fff" }}>Son güncelleme:</strong> 7 Eylül 2026</p>
        <p>Kalori Takip, kullanıcıların günlük kalori ve makro besin kayıtlarını, kilo geçmişini, su tüketimini, fiziksel aktivite verilerini ve kişisel hedeflerini takip etmelerine yardımcı olan bir sağlık ve beslenme uygulamasıdır.</p>

        <Section title="Toplanan ve işlenen veriler">
          Google ile giriş yaptığınızda hesap kimliği ve e-posta adresi gibi temel hesap bilgileri alınabilir. Uygulamada oluşturduğunuz kalori kayıtları, yiyecek adı, gram, kalori, protein, karbonhidrat ve yağ değerleri; kilo kayıtları ve günlük kalori hedefleri hesabınıza bağlı olarak saklanabilir. Su takibi, favori yiyecekler, kayıtlı öğünler ve bildirim tercihleri cihaz üzerinde yerel olarak tutulabilir.
        </Section>
        <Section title="Health Connect ve adım verisi">
          Kullanıcı açıkça izin verirse uygulama Android Health Connect üzerinden yalnızca adım sayısı verisini okuyabilir. Bu veri günlük aktivite bilgisini göstermek amacıyla kullanılır. Health Connect izni kullanıcı tarafından her zaman geri alınabilir. Adım verisi reklam hedefleme veya veri brokerı amacıyla kullanılmaz.
        </Section>
        <Section title="Sesle yemek ekleme">
          Kullanıcı isteğe bağlı olarak mikrofon izni verip Android konuşma tanıma arayüzünü başlatabilir. Uygulama ham ses dosyasını kendi sunucusuna yüklemez veya kalıcı olarak saklamaz. Konuşma tanıma işlemi cihazda veya cihazın seçili konuşma tanıma sağlayıcısında gerçekleştirilebilir. Tanınan metin, Türk yiyecek kataloğuyla eşleştirilerek kullanıcı onayıyla yemek kaydına dönüştürülür.
        </Section>
        <Section title="Fotoğraftan AI yemek analizi">
          Kullanıcı bu özelliği açıkça başlatırsa seçtiği yemek fotoğrafı, porsiyon ve yaklaşık besin değerlerini tahmin etmek amacıyla kimliği doğrulanmış Supabase Edge Function üzerinden AI analiz sağlayıcısına gönderilebilir. Uygulama fotoğrafı kendi veritabanında kalıcı olarak saklamaz. AI tarafından üretilen sonuçlar tahmindir ve kullanıcı tarafından kontrol edilmelidir.
        </Section>
        <Section title="Öğün hatırlatmaları">
          Kullanıcı isteğe bağlı olarak öğün bildirimlerini açabilir. Bildirim izni yalnızca kahvaltı, öğle ve akşam yemek kayıtlarını hatırlatmak amacıyla kullanılır. Hatırlatma tercihi cihaz üzerinde tutulur.
        </Section>
        <Section title="Dinamik kalori hedefi">
          Dinamik hedef özelliği kullanıcının kendi kilo kayıtlarından hesaplanan kilo trendini, kullanıcının seçtiği haftalık hedefle karşılaştırarak günlük kalori hedefi için bir öneri üretir. Öneri otomatik uygulanmaz; kullanıcı onayıyla uygulanır. Bu hesap tıbbi öneri değildir.
        </Section>
        <Section title="Verilerin kullanım amacı">
          Veriler yalnızca uygulamanın temel özelliklerini sağlamak, kayıtlarınızı cihazlar arasında senkronize etmek, geçmiş kalori ve kilo takibini göstermek, aktivite ve kişisel hedefleri hesaplamak, öğün hatırlatmaları sağlamak ve hesabınızı yönetmek amacıyla kullanılır.
        </Section>
        <Section title="Barkod ve Open Food Facts">
          Barkod tarama özelliği Google Code Scanner kullanabilir. Okunan ürün barkodu, ürünün besin bilgilerini bulmak amacıyla Open Food Facts hizmetine gönderilebilir.
        </Section>
        <Section title="Veri saklama ve güvenlik">
          Hesaba bağlı uygulama verileri Supabase altyapısında saklanır. Kullanıcıya ait satırlar kullanıcı kimliğiyle sınırlandırılan erişim kurallarıyla korunur. Uygulama istemcisinde Supabase service-role veya AI sağlayıcı API anahtarı tutulmaz. Android sistem yedeği kapalıdır ve uygulama şifresiz HTTP trafiğine izin vermez.
        </Section>
        <Section title="Hesap ve veri silme">
          Uygulamadaki “Hesabımı ve verilerimi sil” seçeneğiyle bu uygulamaya bağlı kalori, makro, kilo, hedef ve profil kayıtlarınızı ve Kalori Takip hesabınızı kalıcı olarak silebilirsiniz.
        </Section>
        <Section title="Sağlık bilgileri hakkında">
          Kalori Takip tıbbi teşhis, tedavi veya acil sağlık hizmeti sunmaz. Kalori hedefi, kilo trendi ve fotoğraf analizi gibi hesaplamalar genel tahmin niteliğindedir ve profesyonel tıbbi tavsiyenin yerine geçmez.
        </Section>
        <Section title="İletişim">
          Gizlilik veya veri silme talepleri için Google Play mağaza kaydında belirtilen geliştirici iletişim adresi kullanılabilir.
        </Section>

        <footer style={{ marginTop: 48, borderTop: "1px solid #26312e", paddingTop: 20, color: "#9da9a5" }}>
          Kalori Takip • Gizlilik Politikası
        </footer>
      </article>
    </main>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section style={{ marginTop: 28 }}>
      <h2 style={{ color: "#21f38a", fontSize: 20 }}>{title}</h2>
      <p style={{ color: "#c8d2ce" }}>{children}</p>
    </section>
  );
}
