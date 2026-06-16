# COLA CLI Design Spec

## Overview

A CLI tool (`cola-cli`) for scaffolding COLA Spring Boot projects. Users run `cola` command, answer interactive prompts, and get a fully configured COLA project downloaded from GitHub with all placeholders replaced.

## Tech Stack

- **Language**: TypeScript + Node.js
- **Build**: Vite (library mode, ESM + CJS output)
- **Interactive prompts**: `@clack/prompts`
- **Template download**: `degit` (lightweight GitHub directory download)
- **HTTP**: `node-fetch` (for Initializr API)
- **Package**: Published to npm, globally installed as `cola-cli`, command name `cola`

## Project Structure

```
cli/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── src/
│   ├── index.ts          # Entry point, bin command
│   ├── prompts.ts        # Interactive prompt definitions (Chinese)
│   ├── downloader.ts     # GitHub template download via degit
│   ├── processor.ts      # Template processing (placeholder replacement, tag parsing)
│   ├── initializr.ts     # Spring Initializr API client
│   ├── generator.ts      # Main project generation flow
│   └── utils.ts          # Utility functions
```

## Interactive Flow

All prompts are in Chinese.

```
$ cola

  COLA Spring Boot 项目生成器

? 项目名称: › my-service
? Group ID: › com.example
? Artifact ID: › my-service
? 包名: › com.example.myservice
? Spring Boot 版本: › (fetched from Initializr API)
? Java 版本: › (fetched from Initializr API)
? 选择 COLA 模块: › (multi-select)
  ✅ adapter (Web层/Controller)
  ✅ app (应用服务层)
  ✅ client (客户端DTO/接口)
  ✅ domain (领域层)
  ✅ infrastructure (基础设施层)
  ✅ start (启动模块)
  ☐ generator (代码生成器)
? 选择依赖: › (multi-select, from Initializr API, grouped by category)
  ✅ MySQL + MyBatis-Plus + Druid
  ✅ Redis
  ☐ Elasticsearch
  ☐ RocketMQ
  ☐ Dubbo
  ☐ XXL-Job
  ...
? 输出目录: › ./

  ⠙ 下载模板中...
  ⠙ 处理模板中...
  ⠙ 生成项目中...

  ✅ 项目创建成功!
```

### Default Values

| Field | Default |
|-------|---------|
| 项目名称 | (empty, required) |
| Group ID | `com.chief` |
| Artifact ID | (same as project name) |
| 包名 | (same as Group ID) |
| Spring Boot 版本 | 2.7.18 |
| Java 版本 | 1.8 |
| COLA 模块 | all except generator |
| 输出目录 | `./` |

### Validation Rules

- **项目名称**: lowercase, starts with letter, kebab-case (e.g., `my-service`)
- **Group ID**: valid Java package (e.g., `com.example`)
- **Artifact ID**: lowercase, starts with letter, kebab-case
- **包名**: valid Java package (e.g., `com.example.myservice`)

## Spring Initializr API Integration

**Endpoint**: `http://120.26.113.116:7001/`

**Purpose**: Fetch available Spring Boot versions, Java versions, and dependency list for interactive selection. Used for information only; CLI does NOT call the API to generate projects.

**API Usage**:
- `GET /` with `Accept: application/json` → HAL+JSON response with version/dependency metadata
- Parse `values` array under `bootVersion` for Spring Boot version selection (each entry has `id` and `name`)
- Parse `values` array under `javaVersion` for Java version selection
- Parse `values` array under `dependencies` for dependency selection, grouped by `name` field (filtered to relevant ones for COLA projects)
- If API is unreachable, fall back to hardcoded defaults

## Template Tag Syntax

### Module-level (directory)

No explicit tags needed. CLI deletes unselected module directories (e.g., `template-generator/`) and renames remaining ones from `template-*` to `{artifactId}-*`.

### Dependency-level (pom.xml)

```xml
<!-- @dep <dependency-id> -->
<dependency>
    <groupId>...</groupId>
    <artifactId>...</artifactId>
</dependency>
<!-- @enddep -->
```

CLI keeps the block (removing tag lines) if the dependency is selected; deletes the entire block otherwise.

### Configuration-level (application.yml, etc.)

```yaml
# @cfg <config-id>
spring:
  redis:
    host: 127.0.0.1
# @endcfg
```

Same logic as dependency tags.

### File-level (Java source files)

For optional config classes (e.g., `RedisConfig.java`), the CLI deletes the file entirely if the corresponding dependency is not selected. File-to-dependency mapping is defined in `template.json`.

### Placeholders (unchanged from existing)

| Placeholder | Description |
|-------------|-------------|
| `__GROUP_ID__` | Maven groupId |
| `__ARTIFACT_ID__` | Maven artifactId |
| `__PACKAGE_NAME__` | Java package name |
| `__PROJECT_NAME__` | Project name |
| `__PACKAGE_PATH__` | Directory name for package path (e.g., `com/chief`) |

## Processing Pipeline

1. **Download** template from GitHub (`template/` directory) to temp directory
2. **Delete** unselected module directories
3. **Rename** `template-*` directories to `{artifactId}-*`
4. **Parse** `@dep/@enddep` and `@cfg/@endcfg` tags in all files
5. **Delete** unselected optional Java files (per `template.json` mapping)
6. **Replace** all placeholders (`__GROUP_ID__`, etc.)
7. **Restructure** `__PACKAGE_PATH__` directories to actual package paths
8. **Regenerate** root `pom.xml` `<modules>` section: replace entire `<modules>` block with entries matching selected modules (e.g., `<module>{artifactId}-adapter</module>`)
9. **Move** to target output directory

## Template Modifications

### `template/pom.xml`

- Wrap optional dependencies in `<dependencyManagement>` with `@dep/@enddep` tags
- Wrap optional properties with `@dep/@enddep` tags
- `<modules>` section: CLI dynamically generates based on selected modules

### Module `pom.xml` files

- `template-infrastructure/pom.xml`: Tag Redis, ES, MyBatis-Plus dependencies
- `template-adapter/pom.xml`: Tag SpringDoc dependency

### `application.yml`

- Tag Redis config with `@cfg redis`
- Tag ES config with `@cfg elasticsearch`
- Tag Dubbo config with `@cfg dubbo`
- Tag RocketMQ config with `@cfg rocketmq`
- Tag XXL-Job config with `@cfg xxl-job`
- Tag SpringDoc config with `@cfg springdoc`

### Java source files

Optional files that get deleted if corresponding dependency not selected:
- `RedisConfig.java` → `redis`
- `ElasticsearchConfig.java` → `elasticsearch`
- `XxlJobConfig.java` → `xxl-job`

### `template.json` (new file)

```json
{
  "name": "cola-spring-boot",
  "modules": {
    "adapter": { "description": "Web层/Controller", "default": true, "required": false },
    "app": { "description": "应用服务层", "default": true, "required": true },
    "client": { "description": "客户端DTO/接口", "default": true, "required": true },
    "domain": { "description": "领域层", "default": true, "required": true },
    "infrastructure": { "description": "基础设施层", "default": true, "required": true },
    "start": { "description": "启动模块", "default": true, "required": true },
    "generator": { "description": "代码生成器", "default": false, "required": false }
  },
  "dependencies": {
    "mybatis-plus": { "description": "MyBatis-Plus + Druid + MySQL", "default": true },
    "redis": { "description": "Redis 缓存", "default": true },
    "elasticsearch": { "description": "Elasticsearch 搜索引擎", "default": false },
    "dubbo": { "description": "Apache Dubbo RPC", "default": false },
    "rocketmq": { "description": "RocketMQ 消息队列", "default": false },
    "xxl-job": { "description": "XXL-Job 任务调度", "default": false },
    "easyexcel": { "description": "EasyExcel 表格处理", "default": false },
    "springdoc": { "description": "SpringDoc OpenAPI 文档", "default": true }
  },
  "fileMapping": {
    "RedisConfig.java": "redis",
    "ElasticsearchConfig.java": "elasticsearch",
    "XxlJobConfig.java": "xxl-job"
  }
}
```

### Remove `create.sh`

Replaced by CLI functionality.

## GitHub Template Source

- **Repository**: `https://github.com/chief-fei/spring-boot-cola-template`
- **Branch**: `main`
- **Directory**: `template/`
- **Download method**: `degit` with subdirectory support (`chief-fei/spring-boot-cola-template/template`)

## Error Handling

- Network failure during download: retry once, then show error with manual download instructions
- Initializr API unavailable: fall back to hardcoded default versions/dependencies
- Target directory already exists: prompt user to overwrite or choose different directory
- Invalid input: show validation error and re-prompt
