import fs from "node:fs";
import path from "node:path";
import * as p from "@clack/prompts";
import { downloadTemplate } from "./downloader.js";
import { processTemplate } from "./processor.js";
import { rmDir } from "./utils.js";
import type { ProjectConfig } from "./prompts.js";

export async function generate(config: ProjectConfig): Promise<string> {
  const outputDir = path.resolve(config.outputDir);
  const projectDir = path.join(outputDir, config.projectName);

  if (fs.existsSync(projectDir)) {
    const overwrite = await p.confirm({
      message: `目录 ${projectDir} 已存在，是否覆盖？`,
      initialValue: false,
    });
    if (p.isCancel(overwrite) || !overwrite) {
      p.cancel("已取消");
      process.exit(0);
    }
    rmDir(projectDir);
  }

  const s = p.spinner();

  s.start("下载模板中...");
  const templateDir = await downloadTemplate();
  s.stop("模板下载完成");

  s.start("处理模板中...");
  processTemplate(templateDir, config);
  cleanupTemplate(templateDir);
  s.stop("模板处理完成");

  s.start("生成项目中...");
  fs.mkdirSync(outputDir, { recursive: true });
  fs.renameSync(templateDir, projectDir);
  s.stop("项目生成完成");

  return projectDir;
}

function cleanupTemplate(templateDir: string): void {
  const filesToRemove = ["create.sh", ".gitignore", ".DS_Store"];
  for (const file of filesToRemove) {
    const filePath = path.join(templateDir, file);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  }
  const templateJson = path.join(templateDir, "template.json");
  if (fs.existsSync(templateJson)) {
    fs.unlinkSync(templateJson);
  }
}
