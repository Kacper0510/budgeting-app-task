import { defineConfig } from "vite";
import react, { reactCompilerPreset } from "@vitejs/plugin-react";
import babel from "@rolldown/plugin-babel";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig((cfg) => {
  const plugins = [react(), babel({ presets: [reactCompilerPreset()] }), tailwindcss()];
  if (cfg.command === "serve") {
    return {
      plugins,
      server: {
        proxy: {
          "/api": {
            target: "http://127.0.0.1:8080",
            changeOrigin: true,
            secure: false,
          },
        },
      },
    };
  } else {
    return {
      plugins,
    };
  }
});
