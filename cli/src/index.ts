import * as p from "@clack/prompts";
import { fetchInitializrData } from "./initializr.js";
import { promptProjectConfig } from "./prompts.js";
import { generate } from "./generator.js";
import type { ProjectConfig } from "./prompts.js";

async function main() {
  p.intro("COLA Spring Boot 项目生成器");

  const s = p.spinner();
  s.start("加载依赖信息...");
  const initializrData = await fetchInitializrData();
  s.stop("依赖信息加载完成");

  const config: ProjectConfig = await promptProjectConfig(initializrData);

  const projectDir = await generate(config);

  p.outro(`项目创建成功!\n\n  位置: ${projectDir}\n\n  下一步:\n    cd ${config.projectName}\n    mvn clean install -DskipTests\n    cd ${config.artifactId}-start\n    mvn spring-boot:run`);
}

main().catch((err) => {
  p.cancel(err.message ?? "未知错误");
  process.exit(1);
});
