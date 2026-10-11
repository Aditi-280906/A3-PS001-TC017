import type {
  ModelOutputs,
  RiskLevel,
} from "../types/ranscope";

export const MODEL_WEIGHTS = {
  mpvg: 0.18,
  sfim: 0.26,
  eap: 0.31,
  cait: 0.25,
};

export function calculateFinalProbability(
  models: ModelOutputs
): number {
  return (
    models.mpvg * MODEL_WEIGHTS.mpvg +
    models.sfim * MODEL_WEIGHTS.sfim +
    models.eap * MODEL_WEIGHTS.eap +
    models.cait * MODEL_WEIGHTS.cait
  );
}

export function getRiskLevel(
  probability: number
): RiskLevel {
  if (probability < 0.25) {
    return "LOW";
  }

  if (probability < 0.5) {
    return "MEDIUM";
  }

  if (probability < 0.75) {
    return "HIGH";
  }

  return "CRITICAL";
}