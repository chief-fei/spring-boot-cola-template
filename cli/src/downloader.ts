import degit from "degit";
import path from "node:path";
import os from "node:os";
import fs from "node:fs";

const REPO = "chief-fei/spring-boot-cola-template";
const TEMPLATE_SUBDIR = "template";
const BRANCH = "feat/cola-cli";

export interface TemplateModule {
  description: string;
  default: boolean;
  required: boolean;
}

export interface TemplateDependency {
  description: string;
  default: boolean;
}

export interface TemplateMeta {
  name: string;
  version: string;
  modules: Record<string, TemplateModule>;
  dependencies: Record<string, TemplateDependency>;
  fileMapping: Record<string, string>;
}

export interface DownloadResult {
  templateDir: string;
  meta: TemplateMeta;
}

export async function downloadTemplate(): Promise<DownloadResult> {
  const tmpDir = path.join(os.tmpdir(), `cola-template-${Date.now()}`);
  fs.mkdirSync(tmpDir, { recursive: true });

  const emitter = degit(`${REPO}/${TEMPLATE_SUBDIR}#${BRANCH}`, {
    cache: false,
    force: true,
    verbose: false,
  });

  await emitter.clone(tmpDir);

  const metaPath = path.join(tmpDir, "template.json");
  const meta: TemplateMeta = JSON.parse(fs.readFileSync(metaPath, "utf-8"));

  return { templateDir: tmpDir, meta };
}
