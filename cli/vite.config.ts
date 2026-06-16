import { defineConfig } from "vite";
import { resolve } from "path";

export default defineConfig({
  build: {
    lib: {
      entry: resolve(__dirname, "src/index.ts"),
      formats: ["es"],
      fileName: "index",
    },
    rollupOptions: {
      external: [
        "fs",
        "path",
        "os",
        "url",
        "child_process",
        "node:fs",
        "node:path",
        "node:os",
        "node:url",
        "node:child_process",
        "@clack/prompts",
        "degit",
        "node-fetch",
      ],
      output: {
        banner: "#!/usr/bin/env node",
      },
    },
    target: "node18",
    outDir: "dist",
    minify: false,
  },
});
