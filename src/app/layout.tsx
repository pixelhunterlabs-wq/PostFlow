import "./globals.css";

export const metadata = {
  title: "PostFlow",
  description: "AI video production studio",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="tr">
      <body>
        {children}
        <a
          href="/test-video"
          style={{
            position: "fixed",
            right: 18,
            bottom: 18,
            zIndex: 9999,
            padding: "11px 15px",
            borderRadius: 14,
            background: "#67e8b7",
            color: "#041018",
            fontSize: 13,
            fontWeight: 800,
            textDecoration: "none",
            boxShadow: "0 14px 38px rgba(0,0,0,.34)",
          }}
        >
          ▶ İlk Test MP4
        </a>
      </body>
    </html>
  );
}
