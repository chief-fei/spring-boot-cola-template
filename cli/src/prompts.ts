import * as p from "@clack/prompts";
import { validateKebabCase, validateJavaPackage } from "./utils.js";
import type { InitializrData } from "./initializr.js";
import type { TemplateMeta } from "./downloader.js";

export interface ProjectConfig {
  projectName: string;
  groupId: string;
  artifactId: string;
  packageName: string;
  bootVersion: string;
  javaVersion: string;
  modules: string[];
  dependencies: string[];
  outputDir: string;
}

export async function promptProjectConfig(initializrData: InitializrData, meta: TemplateMeta): Promise<ProjectConfig> {
  const projectName = await p.text({
    message: "项目名称",
    placeholder: "my-service",
    validate: validateKebabCase,
  });
  if (p.isCancel(projectName)) process.exit(0);

  const groupId = await p.text({
    message: "Group ID",
    placeholder: "com.chief",
    defaultValue: "com.chief",
    validate: validateJavaPackage,
  });
  if (p.isCancel(groupId)) process.exit(0);

  const artifactId = await p.text({
    message: "Artifact ID",
    placeholder: projectName,
    defaultValue: projectName,
    validate: validateKebabCase,
  });
  if (p.isCancel(artifactId)) process.exit(0);

  const packageName = await p.text({
    message: "包名",
    placeholder: groupId,
    defaultValue: groupId,
    validate: validateJavaPackage,
  });
  if (p.isCancel(packageName)) process.exit(0);

  const bootVersion = await p.select({
    message: "Spring Boot 版本",
    options: initializrData.bootVersions.map((v) => ({
      value: v.id,
      label: v.name,
    })),
    initialValue: "2.7.18",
  });
  if (p.isCancel(bootVersion)) process.exit(0);

  const javaVersion = await p.select({
    message: "Java 版本",
    options: initializrData.javaVersions.map((v) => ({
      value: v.id,
      label: v.name,
    })),
    initialValue: "1.8",
  });
  if (p.isCancel(javaVersion)) process.exit(0);

  const moduleOptions = Object.entries(meta.modules).map(([key, val]) => ({
    value: key,
    label: `${key} (${val.description})`,
  }));
  const defaultModules = Object.entries(meta.modules)
    .filter(([, val]) => val.default)
    .map(([key]) => key);

  const modules = await p.multiselect({
    message: "选择 COLA 模块",
    options: moduleOptions,
    initialValues: defaultModules,
    required: true,
  });
  if (p.isCancel(modules)) process.exit(0);

  const depOptions = Object.entries(meta.dependencies).map(([key, val]) => ({
    value: key,
    label: `${key} - ${val.description}`,
  }));
  const defaultDeps = Object.entries(meta.dependencies)
    .filter(([, val]) => val.default)
    .map(([key]) => key);

  const deps = await p.multiselect({
    message: "选择依赖",
    options: depOptions,
    initialValues: defaultDeps,
    required: false,
  });
  if (p.isCancel(deps)) process.exit(0);

  const outputDir = await p.text({
    message: "输出目录",
    placeholder: "./",
    defaultValue: "./",
  });
  if (p.isCancel(outputDir)) process.exit(0);

  return {
    projectName,
    groupId,
    artifactId,
    packageName,
    bootVersion: bootVersion as string,
    javaVersion: javaVersion as string,
    modules: modules as string[],
    dependencies: deps as string[],
    outputDir,
  };
}
