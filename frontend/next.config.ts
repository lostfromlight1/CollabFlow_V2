import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Client JWT session SPA — no Cache Components / instant-nav validation needed yet
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
