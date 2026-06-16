import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { processTemplate } from "./processor.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const templateDir = path.resolve(__dirname, "..", "..", "template");
const outputDir = path.resolve(__dirname, "..", "..", "demo-output");

const config = {
  projectName: "demo-service",
  groupId: "com.example",
  artifactId: "demo-service",
  packageName: "com.example.demo",
  bootVersion: "2.7.18",
  javaVersion: "1.8",
  modules: ["adapter", "app", "client", "domain", "infrastructure", "start"],
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
processTemplate(projectDir, config);

const filesToRemove = ["create.sh", ".gitignore", ".DS_Store", "template.json"];
for (const file of filesToRemove) {
  const filePath = path.join(projectDir, file);
  if (fs.existsSync(filePath)) {
    fs.unlinkSync(filePath);
  }
}

console.log("✅ Demo 项目创建成功!");
console.log(`位置: ${projectDir}`);
