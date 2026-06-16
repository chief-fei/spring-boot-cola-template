import fetch from "node-fetch";

const INITIALIZR_URL = "http://120.26.113.116:7001";

export interface VersionOption {
  id: string;
  name: string;
}

export interface DependencyOption {
  id: string;
  name: string;
  description: string;
  group: string;
}

export interface InitializrData {
  bootVersions: VersionOption[];
  javaVersions: VersionOption[];
  dependencies: DependencyOption[];
}

const FALLBACK_DATA: InitializrData = {
  bootVersions: [
    { id: "2.7.18", name: "2.7.18" },
    { id: "3.2.5", name: "3.2.5" },
    { id: "3.3.0", name: "3.3.0" },
  ],
  javaVersions: [
    { id: "1.8", name: "1.8" },
    { id: "11", name: "11" },
    { id: "17", name: "17" },
    { id: "21", name: "21" },
  ],
  dependencies: [
    { id: "mybatis", name: "MyBatis", description: "MyBatis 持久层框架", group: "数据库" },
    { id: "mysql", name: "MySQL", description: "MySQL 数据库驱动", group: "数据库" },
    { id: "data-redis", name: "Redis", description: "Redis 缓存", group: "中间件" },
    { id: "data-elasticsearch", name: "Elasticsearch", description: "Elasticsearch 搜索引擎", group: "中间件" },
    { id: "rocketmq", name: "RocketMQ", description: "RocketMQ 消息队列", group: "中间件" },
    { id: "kafka", name: "Kafka", description: "Apache Kafka 消息系统", group: "中间件" },
  ],
};

interface HalValue {
  id: string;
  name: string;
  description?: string;
}

interface HalGroup {
  name: string;
  values: HalValue[];
}

interface HalResponse {
  bootVersion?: { values: HalValue[]; default: string };
  javaVersion?: { values: HalValue[]; default: string };
  dependencies?: { values: HalGroup[] };
}

export async function fetchInitializrData(): Promise<InitializrData> {
  try {
    const res = await fetch(INITIALIZR_URL, {
      headers: { Accept: "application/json" },
      signal: AbortSignal.timeout(5000),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = (await res.json()) as HalResponse;

    const bootVersions: VersionOption[] =
      data.bootVersion?.values?.map((v) => ({ id: v.id, name: v.name })) ?? FALLBACK_DATA.bootVersions;

    const javaVersions: VersionOption[] =
      data.javaVersion?.values?.map((v) => ({ id: v.id, name: v.name })) ?? FALLBACK_DATA.javaVersions;

    const dependencies: DependencyOption[] = [];
    if (data.dependencies?.values) {
      for (const group of data.dependencies.values) {
        for (const dep of group.values) {
          dependencies.push({
            id: dep.id,
            name: dep.name,
            description: dep.description ?? dep.name,
            group: group.name,
          });
        }
      }
    }

    return { bootVersions, javaVersions, dependencies: dependencies.length > 0 ? dependencies : FALLBACK_DATA.dependencies };
  } catch {
    return FALLBACK_DATA;
  }
}
