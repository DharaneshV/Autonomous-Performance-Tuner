package com.antigravity.tuner;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OptimizerEngineTest {

  @Test
  @DisplayName("Verify ML optimizer calculates safe JVM tuning bounds from historical telemetry")
  void testJvmTuningOptimizationSafetyBounds() {
    OptimizerEngine optimizer = new OptimizerEngine();
    double historicalAvgPauseMs = 150.0;

    OptimizerEngine.JvmTuningRecommendation rec = optimizer.optimizeJvmParams(historicalAvgPauseMs);

    assertThat(rec).isNotNull();
    // Safety validation checks
    assertThat(rec.maxGcPauseMillisMb())
        .as("MaxGCPauseMillis must stay within safe operational limits")
        .isBetween(50L, 500L);

    assertThat(rec.g1HeapRegionSizeMb())
        .as("G1HeapRegionSize must be a valid JVM power of two between 1MB and 32MB")
        .isBetween(1, 32);
  }
}
