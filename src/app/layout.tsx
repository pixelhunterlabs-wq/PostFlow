import "./globals.css";

export const metadata = {
  title: "PostFlow",
  description: "AI video production studio",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="tr">
      <body>{children}</body>
    </html>
  );
}
