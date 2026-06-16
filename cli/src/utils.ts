import fs from "node:fs";
import path from "node:path";

export const KEBAB_CASE_RE = /^[a-z][a-z0-9]*(-[a-z0-9]+)*$/;
export const JAVA_PACKAGE_RE = /^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$/;

export function validateKebabCase(value: string): string | undefined {
  if (!value) return "不能为空";
  if (!KEBAB_CASE_RE.test(value))
    return "必须以小写字母开头，使用 kebab-case 格式（如 my-service）";
  return undefined;
}

export function validateJavaPackage(value: string): string | undefined {
  if (!value) return "不能为空";
  if (!JAVA_PACKAGE_RE.test(value))
    return "必须是合法的 Java 包名（如 com.example）";
  return undefined;
}

export function rmDir(dirPath: string): void {
  if (fs.existsSync(dirPath)) {
    fs.rmSync(dirPath, { recursive: true, force: true });
  }
}

export function ensureDir(dirPath: string): void {
  fs.mkdirSync(dirPath, { recursive: true });
}

export function walkDir(dir: string, callback: (filePath: string) => void): void {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walkDir(fullPath, callback);
    } else {
      callback(fullPath);
    }
  }
}

export function replaceInFile(filePath: string, search: string | RegExp, replace: string): void {
  const content = fs.readFileSync(filePath, "utf-8");
  const updated = content.replace(search, replace);
  if (content !== updated) {
    fs.writeFileSync(filePath, updated, "utf-8");
  }
}
