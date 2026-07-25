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
        .isIn(1, 2, 4, 8, 16, 32);
  }

  @Test
  @DisplayName("Verify G1HeapRegionSize quantizes strictly to nearest valid power of two")
  void testG1HeapRegionSizeQuantization() {
    OptimizerEngine optimizer = new OptimizerEngine();
    assertThat(optimizer.quantizeG1HeapRegionSize(5)).isEqualTo(4);
    assertThat(optimizer.quantizeG1HeapRegionSize(7)).isEqualTo(8);
    assertThat(optimizer.quantizeG1HeapRegionSize(14)).isEqualTo(16);
    assertThat(optimizer.quantizeG1HeapRegionSize(25)).isEqualTo(32);
  }

  @Test
  @DisplayName(
      "Verify G1HeapRegionSize quantization boundary conditions (extreme inputs & exact midpoints)")
  void testG1HeapRegionSizeQuantizationBoundaries() {
    OptimizerEngine optimizer = new OptimizerEngine();
    // Below lower bound
    assertThat(optimizer.quantizeG1HeapRegionSize(0)).isEqualTo(1);
    assertThat(optimizer.quantizeG1HeapRegionSize(-5)).isEqualTo(1);

    // Exact valid powers of 2
    assertThat(optimizer.quantizeG1HeapRegionSize(1)).isEqualTo(1);
    assertThat(optimizer.quantizeG1HeapRegionSize(2)).isEqualTo(2);
    assertThat(optimizer.quantizeG1HeapRegionSize(4)).isEqualTo(4);
    assertThat(optimizer.quantizeG1HeapRegionSize(8)).isEqualTo(8);
    assertThat(optimizer.quantizeG1HeapRegionSize(16)).isEqualTo(16);
    assertThat(optimizer.quantizeG1HeapRegionSize(32)).isEqualTo(32);

    // Above upper bound
    assertThat(optimizer.quantizeG1HeapRegionSize(50)).isEqualTo(32);
    assertThat(optimizer.quantizeG1HeapRegionSize(100)).isEqualTo(32);
  }
}
