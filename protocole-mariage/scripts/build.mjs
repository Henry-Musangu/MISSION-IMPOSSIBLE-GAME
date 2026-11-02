// Construit le dossier www/ (contenu de l'application) à partir de src/.
import * as esbuild from "esbuild";
import { cpSync, mkdirSync, rmSync, copyFileSync } from "node:fs";

rmSync("www", { recursive: true, force: true });
mkdirSync("www", { recursive: true });
await esbuild.build({
  entryPoints: { app: "src/main.js" },
  bundle: true,
  minify: true,
  sourcemap: false,
  format: "iife",
  target: ["chrome90"],
  outdir: "www",
  loader: { ".woff2": "file", ".woff": "file", ".png": "file" },
  assetNames: "assets/[name]-[hash]",
  legalComments: "none",
  logLevel: "info"
});
copyFileSync("src/index.html", "www/index.html");
cpSync("src/img", "www/img", { recursive: true });
console.log("www/ prêt");
