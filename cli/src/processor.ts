import fs from "node:fs";
import path from "node:path";
import type { ProjectConfig } from "./prompts.js";
import type { TemplateMeta } from "./downloader.js";
import { walkDir, rmDir, ensureDir } from "./utils.js";

const DEP_TAG_RE = /<!--\s*@dep\s+([\w-]+(?:\s*,\s*[\w-]+)*)\s*-->([\s\S]*?)<!--\s*@enddep\s*-->/g;
const CFG_TAG_RE = /#\s*@cfg\s+([\w-]+)\s*([\s\S]*?)#\s*@endcfg/g;

export function processTemplate(templateDir: string, config: ProjectConfig, meta: TemplateMeta): void {
  renamePlaceholderFiles(templateDir, config);
  removeUnselectedModules(templateDir, config);
  renameModuleDirectories(templateDir, config);
  processTags(templateDir, config);
  removeUnselectedFiles(templateDir, config, meta);
  replacePlaceholders(templateDir, config);
  restructurePackagePaths(templateDir, config);
  regenerateModulesSection(templateDir, config);
  cleanEmptyLines(templateDir);
}

function cleanEmptyLines(templateDir: string): void {
  walkDir(templateDir, (filePath) => {
    const ext = path.extname(filePath);
    if (![".xml", ".yml", ".yaml", ".properties", ".java"].includes(ext)) return;

    const content = fs.readFileSync(filePath, "utf-8");
    const cleaned = content
      .replace(/^[ \t]+$/gm, "")
      .replace(/\n{3,}/g, "\n\n");
    if (cleaned !== content) {
      fs.writeFileSync(filePath, cleaned, "utf-8");
    }
  });
}

function renamePlaceholderFiles(templateDir: string, config: ProjectConfig): void {
  walkDir(templateDir, (filePath) => {
    const dir = path.dirname(filePath);
    const name = path.basename(filePath);
    if (name.includes("__")) {
      const newName = name
        .replace(/__ARTIFACT_ID__/g, config.artifactId)
        .replace(/__GROUP_ID__/g, config.groupId)
        .replace(/__PROJECT_NAME__/g, config.projectName)
        .replace(/__PACKAGE_NAME__/g, config.packageName);
      if (newName !== name) {
        fs.renameSync(filePath, path.join(dir, newName));
      }
    }
  });
}

function removeUnselectedModules(templateDir: string, config: ProjectConfig): void {
  const allModules = ["adapter", "app", "client", "domain", "infrastructure", "start", "generator"];
  for (const mod of allModules) {
    if (!config.modules.includes(mod)) {
      const modDir = path.join(templateDir, `template-${mod}`);
      rmDir(modDir);
    }
  }
}

function renameModuleDirectories(templateDir: string, config: ProjectConfig): void {
  const entries = fs.readdirSync(templateDir, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.isDirectory() && entry.name.startsWith("template-")) {
      const suffix = entry.name.slice("template-".length);
      const oldPath = path.join(templateDir, entry.name);
      const newPath = path.join(templateDir, `${config.artifactId}-${suffix}`);
      fs.renameSync(oldPath, newPath);
    }
  }
}

function processTags(templateDir: string, config: ProjectConfig): void {
  walkDir(templateDir, (filePath) => {
    const ext = path.extname(filePath);
    if (![".xml", ".yml", ".yaml", ".properties", ".java"].includes(ext)) return;

    let content = fs.readFileSync(filePath, "utf-8");
    let changed = false;

    if (ext === ".xml" || ext === ".java") {
      const newContent = content.replace(DEP_TAG_RE, (_match, ids: string, body: string) => {
        changed = true;
        const idList = ids.split(",").map((s) => s.trim());
        if (idList.some((id) => config.dependencies.includes(id))) {
          return body.trim();
        }
        return "";
      });
      if (changed) {
        content = newContent;
      }
    }

    if (ext === ".yml" || ext === ".yaml" || ext === ".properties") {
      const newContent = content.replace(CFG_TAG_RE, (_match, id: string, body: string) => {
        changed = true;
        if (config.dependencies.includes(id)) {
          return body.trimEnd();
        }
        return "";
      });
      if (changed) {
        content = newContent;
      }
    }

    if (changed) {
      fs.writeFileSync(filePath, content, "utf-8");
    }
  });
}

function removeUnselectedFiles(templateDir: string, config: ProjectConfig, meta: TemplateMeta): void {
  walkDir(templateDir, (filePath) => {
    const fileName = path.basename(filePath);
    const depId = meta.fileMapping[fileName];
    if (depId && !config.dependencies.includes(depId)) {
      fs.unlinkSync(filePath);
    }
  });
}

function resolveSpringCloudVersion(bootVersion: string): string {
  const major = parseInt(bootVersion.split(".")[0], 10);
  const minor = parseInt(bootVersion.split(".")[1], 10);
  if (major === 2 && minor === 7) return "2021.0.5";
  if (major === 3 && minor === 0) return "2022.0.5";
  if (major === 3 && minor >= 2) return "2023.0.3";
  return "2021.0.5";
}

function resolveSpringCloudAlibabaVersion(bootVersion: string): string {
  const major = parseInt(bootVersion.split(".")[0], 10);
  const minor = parseInt(bootVersion.split(".")[1], 10);
  if (major === 2 && minor === 7) return "2021.0.6.0";
  if (major === 3 && minor === 0) return "2022.0.0.0";
  if (major === 3 && minor >= 2) return "2023.0.1.2";
  return "2021.0.6.0";
}

function replacePlaceholders(templateDir: string, config: ProjectConfig): void {
  const replacements: [string, string][] = [
    ["__GROUP_ID__", config.groupId],
    ["__ARTIFACT_ID__", config.artifactId],
    ["__PACKAGE_NAME__", config.packageName],
    ["__PROJECT_NAME__", config.projectName],
    ["__JAVA_VERSION__", config.javaVersion],
    ["__BOOT_VERSION__", config.bootVersion],
    ["__SPRING_CLOUD_VERSION__", resolveSpringCloudVersion(config.bootVersion)],
    ["__SPRING_CLOUD_ALIBABA_VERSION__", resolveSpringCloudAlibabaVersion(config.bootVersion)],
  ];

  walkDir(templateDir, (filePath) => {
    let content = fs.readFileSync(filePath, "utf-8");
    let changed = false;
    for (const [search, replace] of replacements) {
      if (content.includes(search)) {
        content = content.replaceAll(search, replace);
        changed = true;
      }
    }
    if (changed) {
      fs.writeFileSync(filePath, content, "utf-8");
    }
  });
}

function restructurePackagePaths(templateDir: string, config: ProjectConfig): void {
  const packagePath = config.packageName.replace(/\./g, "/");

  const dirs = findAllDirs(templateDir, "__PACKAGE_PATH__");
  for (const dir of dirs) {
    const parentDir = path.dirname(dir);
    const targetDir = path.join(parentDir, packagePath);
    ensureDir(targetDir);

    const entries = fs.readdirSync(dir);
    for (const entry of entries) {
      fs.renameSync(path.join(dir, entry), path.join(targetDir, entry));
    }
    rmDir(dir);
  }
}

function findAllDirs(root: string, name: string): string[] {
  const results: string[] = [];
  const entries = fs.readdirSync(root, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(root, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === name) {
        results.push(fullPath);
      } else {
        results.push(...findAllDirs(fullPath, name));
      }
    }
  }
  return results;
}

function regenerateModulesSection(templateDir: string, config: ProjectConfig): void {
  const pomPath = path.join(templateDir, "pom.xml");
  if (!fs.existsSync(pomPath)) return;

  let content = fs.readFileSync(pomPath, "utf-8");

  const moduleEntries = config.modules
    .map((mod) => `        <module>${config.artifactId}-${mod}</module>`)
    .join("\n");

  const modulesBlock = `    <modules>\n${moduleEntries}\n    </modules>`;

  content = content.replace(
    /<modules>[\s\S]*?<\/modules>/,
    modulesBlock
  );

  const moduleDeps = config.modules
    .filter((mod) => mod !== "generator")
    .map(
      (mod) => `            <dependency>
                <groupId>${config.groupId}</groupId>
                <artifactId>${config.artifactId}-${mod}</artifactId>
                <version>\${project.version}</version>
            </dependency>`
    )
    .join("\n");

  content = content.replace(
    /<!-- Project Modules -->[\s\S]*?<\/dependencies>/,
    `<!-- Project Modules -->\n${moduleDeps}\n        </dependencies>`
  );

  fs.writeFileSync(pomPath, content, "utf-8");
}
