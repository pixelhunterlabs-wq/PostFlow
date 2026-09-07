export const metadata = {
  title: "Kalori Takip - Hesap ve Veri Silme",
  description: "Kalori Takip hesabı ve uygulama verilerinin silinmesi hakkında bilgi.",
};

export default function CalorieTrackerDeleteAccountPage() {
  return (
    <main style={{ minHeight: "100vh", background: "#08110f", color: "#f3f7f5", padding: "48px 20px" }}>
      <article style={{ maxWidth: 760, margin: "0 auto", lineHeight: 1.7 }}>
        <h1 style={{ fontSize: 34, marginBottom: 8 }}>Kalori Takip Hesap ve Veri Silme</h1>
        <p style={{ color: "#c8d2ce" }}>
          Kalori Takip hesabınızı ve hesabınıza bağlı uygulama verilerini uygulama içinden kalıcı olarak silebilirsiniz.
        </p>

        <section style={{ marginTop: 28 }}>
          <h2 style={{ color: "#21f38a", fontSize: 20 }}>Uygulama içinden silme</h2>
          <ol style={{ color: "#c8d2ce", paddingLeft: 22 }}>
            <li>Kalori Takip uygulamasını açın ve hesabınıza giriş yapın.</li>
            <li>Alt menüden <strong style={{ color: "#fff" }}>Profil</strong> bölümünü açın.</li>
            <li><strong style={{ color: "#fff" }}>Hesabımı ve verilerimi sil</strong> seçeneğine dokunun.</li>
            <li>Kalıcı silme uyarısını onaylayın.</li>
          </ol>
        </section>

        <section style={{ marginTop: 28 }}>
          <h2 style={{ color: "#21f38a", fontSize: 20 }}>Silinen veriler</h2>
          <p style={{ color: "#c8d2ce" }}>
            Silme işlemi hesabınıza bağlı Kalori Takip profilini, kalori ve makro yemek kayıtlarını, kilo kayıtlarını, günlük hedef kayıtlarını ve Kalori Takip kimlik doğrulama hesabını kalıcı olarak siler.
          </p>
        </section>

        <section style={{ marginTop: 28 }}>
          <h2 style={{ color: "#21f38a", fontSize: 20 }}>Cihazda tutulan veriler</h2>
          <p style={{ color: "#c8d2ce" }}>
            Su takibi, favoriler, kayıtlı öğünler ve bazı uygulama tercihleri cihaz üzerinde yerel olarak tutulabilir. Uygulamanın kaldırılması bu yerel uygulama verilerini de cihazdan kaldırır.
          </p>
        </section>

        <section style={{ marginTop: 28 }}>
          <h2 style={{ color: "#21f38a", fontSize: 20 }}>Uygulamaya erişemiyorsanız</h2>
          <p style={{ color: "#c8d2ce" }}>
            Veri silme talebi için Google Play mağaza kaydında yer alan geliştirici iletişim adresini kullanabilirsiniz. Talebin doğru hesaba uygulanabilmesi için Kalori Takip'e girişte kullandığınız e-posta adresini belirtmeniz gerekebilir.
          </p>
        </section>

        <p style={{ marginTop: 32, color: "#9da9a5", fontSize: 14 }}>
          Hesap silme işlemi geri alınamaz. Kalori Takip tıbbi kayıt sistemi değildir ve tıbbi veri saklama hizmeti sunmaz.
        </p>
      </article>
    </main>
  );
}
