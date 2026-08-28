import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // The local launcher intentionally opens 127.0.0.1. Next 16 otherwise
  // rejects the dev client chunks as cross-origin and leaves the UI unhydrated.
  allowedDevOrigins: ["127.0.0.1", "localhost"],
  reactStrictMode: true,
};

export default nextConfig;
