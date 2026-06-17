import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { processTemplate } from "./processor.js";
import { cleanupTemplate } from "./generator.js";
import type { TemplateMeta } from "./downloader.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const templateDir = path.resolve(__dirname, "..", "..", "template");
const outputDir = path.resolve(__dirname, "..", "..", "demo-output");

const meta: TemplateMeta = JSON.parse(
  fs.readFileSync(path.join(templateDir, "template.json"), "utf-8")
);

const config = {
  projectName: "demo-service",
  groupId: "com.example",
  artifactId: "demo-service",
  packageName: "com.example.demo",
  bootVersion: "2.7.18",
  javaVersion: "1.8",
  modules: ["adapter", "app", "client", "domain", "infrastructure", "start", "generator"],
  dependencies: ["mybatis-plus", "redis", "springdoc"],
  outputDir: outputDir,
};

const projectDir = path.join(outputDir, config.projectName);

if (fs.existsSync(projectDir)) {
  fs.rmSync(projectDir, { recursive: true, force: true });
}

fs.mkdirSync(outputDir, { recursive: true });
fs.cpSync(templateDir, projectDir, { recursive: true });

console.log("处理模板...");
processTemplate(projectDir, config, meta);

console.log("清理模板...");
cleanupTemplate(projectDir, config);

console.log("✅ Demo 项目创建成功!");
console.log(`位置: ${projectDir}`);
