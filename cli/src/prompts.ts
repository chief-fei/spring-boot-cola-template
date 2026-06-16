import * as p from "@clack/prompts";
import { validateKebabCase, validateJavaPackage } from "./utils.js";
import type { InitializrData } from "./initializr.js";

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

const MODULE_OPTIONS = [
  { value: "adapter", label: "adapter (Web层/Controller)", default: true },
  { value: "app", label: "app (应用服务层)", default: true },
  { value: "client", label: "client (客户端DTO/接口)", default: true },
  { value: "domain", label: "domain (领域层)", default: true },
  { value: "infrastructure", label: "infrastructure (基础设施层)", default: true },
  { value: "start", label: "start (启动模块)", default: true },
  { value: "generator", label: "generator (代码生成器)", default: false },
] as const;

const BUILTIN_DEPS = [
  { id: "mybatis-plus", name: "MyBatis-Plus + Druid + MySQL", description: "MyBatis-Plus 持久层 + Druid 连接池 + MySQL 驱动", default: true },
  { id: "redis", name: "Redis", description: "Redis 缓存支持", default: true },
  { id: "elasticsearch", name: "Elasticsearch", description: "Elasticsearch 搜索引擎", default: false },
  { id: "dubbo", name: "Dubbo", description: "Apache Dubbo RPC 框架", default: false },
  { id: "rocketmq", name: "RocketMQ", description: "RocketMQ 消息队列", default: false },
  { id: "xxl-job", name: "XXL-Job", description: "XXL-Job 分布式任务调度", default: false },
  { id: "easyexcel", name: "EasyExcel", description: "EasyExcel 表格处理", default: false },
  { id: "springdoc", name: "SpringDoc OpenAPI", description: "SpringDoc OpenAPI 接口文档", default: true },
] as const;

export async function promptProjectConfig(initializrData: InitializrData): Promise<ProjectConfig> {
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

  const modules = await p.multiselect({
    message: "选择 COLA 模块",
    options: MODULE_OPTIONS.map((m) => ({
      value: m.value,
      label: m.label,
    })),
    initialValues: MODULE_OPTIONS.filter((m) => m.default).map((m) => m.value),
    required: true,
  });
  if (p.isCancel(modules)) process.exit(0);

  const deps = await p.multiselect({
    message: "选择依赖",
    options: BUILTIN_DEPS.map((d) => ({
      value: d.id,
      label: `${d.name} - ${d.description}`,
    })),
    initialValues: BUILTIN_DEPS.filter((d) => d.default).map((d) => d.id),
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
