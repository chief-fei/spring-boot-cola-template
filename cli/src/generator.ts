import fs from "node:fs";
import path from "node:path";
import * as p from "@clack/prompts";
import { downloadTemplate, type TemplateMeta } from "./downloader.js";
import { processTemplate } from "./processor.js";
import { rmDir, walkDir } from "./utils.js";
import type { ProjectConfig } from "./prompts.js";

const GITIGNORE_CONTENT = `# IDE
.idea/
*.iml
*.iws
*.ipr

# Build
target/
*.class
*.jar
*.war

# Logs
logs/
*.log

# OS
.DS_Store
Thumbs.db

# Maven
.mvn/
`;

export async function generate(config: ProjectConfig, meta: TemplateMeta): Promise<string> {
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
  const { templateDir } = await downloadTemplate();
  s.stop("模板下载完成");

  s.start("处理模板中...");
  processTemplate(templateDir, config, meta);
  cleanupTemplate(templateDir, config);
  s.stop("模板处理完成");

  s.start("生成项目中...");
  fs.mkdirSync(outputDir, { recursive: true });
  fs.renameSync(templateDir, projectDir);
  s.stop("项目生成完成");

  return projectDir;
}

export function cleanupTemplate(templateDir: string, config: ProjectConfig): void {
  const filesToRemove = [".gitignore", "template.json", ".DS_Store"];
  for (const file of filesToRemove) {
    const filePath = path.join(templateDir, file);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  }

  const imlFile = path.join(templateDir, `${config.artifactId}.iml`);
  if (fs.existsSync(imlFile)) {
    fs.unlinkSync(imlFile);
  }

  // 清理所有残留的 .iml 文件（包括未替换占位符的）
  walkDir(templateDir, (filePath) => {
    if (filePath.endsWith(".iml")) {
      fs.unlinkSync(filePath);
    }
  });

  const ideaDir = path.join(templateDir, ".idea");
  rmDir(ideaDir);

  fs.writeFileSync(path.join(templateDir, ".gitignore"), GITIGNORE_CONTENT, "utf-8");
}
