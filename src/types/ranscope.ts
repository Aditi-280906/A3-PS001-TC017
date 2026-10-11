export type RiskLevel =
  | "LOW"
  | "MEDIUM"
  | "HIGH"
  | "CRITICAL";

export type Prediction =
  | "BENIGN"
  | "RANSOMWARE";

export interface ModelOutputs {
  mpvg: number;
  sfim: number;
  eap: number;
  cait: number;
}

export interface DetectionResult {
  deviceId: string;
  machineId: string;

  timestamp: string;

  runId: string;
  windowId: number;

  models: ModelOutputs;

  finalProbability: number;
  prediction: Prediction;
  riskLevel: RiskLevel;
}

export interface Device {
  id: string;
  name: string;
  machineName: string;

  status: "ONLINE" | "OFFLINE";

  riskLevel: RiskLevel;
  riskScore: number;

  lastSeen: string;
}