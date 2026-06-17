import fetch from "node-fetch";

const INITIALIZR_URL = "http://120.26.113.116:7001";

export interface VersionOption {
  id: string;
  name: string;
}

export interface InitializrData {
  bootVersions: VersionOption[];
  javaVersions: VersionOption[];
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

    return { bootVersions, javaVersions };
  } catch {
    return FALLBACK_DATA;
  }
}
