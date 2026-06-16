import degit from "degit";
import path from "node:path";
import os from "node:os";
import fs from "node:fs";

const REPO = "chief-fei/spring-boot-cola-template";
const TEMPLATE_SUBDIR = "template";

export async function downloadTemplate(): Promise<string> {
  const tmpDir = path.join(os.tmpdir(), `cola-template-${Date.now()}`);
  fs.mkdirSync(tmpDir, { recursive: true });

  const emitter = degit(`${REPO}/${TEMPLATE_SUBDIR}`, {
    cache: false,
    force: true,
    verbose: false,
  });

  await emitter.clone(tmpDir);
  return tmpDir;
}
