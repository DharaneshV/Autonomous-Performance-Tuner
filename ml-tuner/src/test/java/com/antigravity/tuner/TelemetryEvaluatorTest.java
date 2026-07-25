package com.antigravity.tuner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TelemetryEvaluatorTest {

  @Test
  @DisplayName(
      "Verify TelemetryEvaluator passes dual statistical gate when improvement >= 5% and exceeds variance")
  void testDualStatisticalGatePass(@TempDir Path tempDir) throws IOException {
    TelemetryEvaluator evaluator = new TelemetryEvaluator();

    // Baseline: 150ms, 152ms, 148ms (median: 150ms, stdDev ~2.0ms)
    List<TelemetryEvaluator.SampleRun> baseline =
        List.of(
            new TelemetryEvaluator.SampleRun(150.0, 5, 99.0),
            new TelemetryEvaluator.SampleRun(152.0, 5, 99.0),
            new TelemetryEvaluator.SampleRun(148.0, 5, 99.0));

    // Candidate: 135ms, 136ms, 134ms (median: 135ms [10% improvement], stdDev ~1.0ms)
    List<TelemetryEvaluator.SampleRun> candidate =
        List.of(
            new TelemetryEvaluator.SampleRun(135.0, 5, 99.1),
            new TelemetryEvaluator.SampleRun(136.0, 5, 99.1),
            new TelemetryEvaluator.SampleRun(134.0, 5, 99.1));

    TelemetryEvaluator.EvaluationResult result =
        evaluator.evaluateSamples(baseline, candidate, true, "JAVA_OPTS=-XX:+UseG1GC", tempDir);

    assertThat(result.status()).isEqualTo(TelemetryEvaluator.TelemetryStatus.MEASURED_VERIFIED);
    assertThat(result.improvementPercent()).isGreaterThanOrEqualTo(5.0);
    assertThat(result.reportFilePath()).exists();
    assertThat(Files.readString(result.reportFilePath())).contains("MEASURED_VERIFIED");
  }

  @Test
  @DisplayName(
      "Verify TelemetryEvaluator rejects candidates with >= 5% improvement if variance is high")
  void testVarianceGateRejectionWhenImprovementExceeds5PercentButVarianceHigh(@TempDir Path tempDir)
      throws IOException {
    TelemetryEvaluator evaluator = new TelemetryEvaluator();

    // Baseline: 150ms, 150ms, 150ms (median: 150ms, stdDev: 0.0ms)
    List<TelemetryEvaluator.SampleRun> baseline =
        List.of(
            new TelemetryEvaluator.SampleRun(150.0, 5, 99.0),
            new TelemetryEvaluator.SampleRun(150.0, 5, 99.0),
            new TelemetryEvaluator.SampleRun(150.0, 5, 99.0));

    // Candidate: 120ms, 138ms, 180ms (median: 138ms [8% improvement], stdDev: ~30.6ms)
    List<TelemetryEvaluator.SampleRun> candidate =
        List.of(
            new TelemetryEvaluator.SampleRun(120.0, 5, 99.1),
            new TelemetryEvaluator.SampleRun(138.0, 5, 99.1),
            new TelemetryEvaluator.SampleRun(180.0, 5, 99.1));

    TelemetryEvaluator.EvaluationResult result =
        evaluator.evaluateSamples(baseline, candidate, true, "JAVA_OPTS=-XX:+UseG1GC", tempDir);

    // Median improvement is 8.0%, but difference (12ms) is less than combined stdDev (30.6ms)
    assertThat(result.improvementPercent()).isGreaterThanOrEqualTo(5.0);
    assertThat(result.status())
        .isEqualTo(TelemetryEvaluator.TelemetryStatus.SIMULATED_OR_UNVERIFIED);
    assertThat(result.statusReason()).contains("Variance Clearance: false");
  }

  @Test
  @DisplayName("Verify TelemetryEvaluator fails when telemetry is synthetic or unverified")
  void testSyntheticModeRejection(@TempDir Path tempDir) throws IOException {
    TelemetryEvaluator evaluator = new TelemetryEvaluator();

    List<TelemetryEvaluator.SampleRun> baseline =
        List.of(new TelemetryEvaluator.SampleRun(150.0, 5, 99.0));
    List<TelemetryEvaluator.SampleRun> candidate =
        List.of(new TelemetryEvaluator.SampleRun(135.0, 5, 99.1));

    TelemetryEvaluator.EvaluationResult result =
        evaluator.evaluateSamples(baseline, candidate, false, "JAVA_OPTS=-XX:+UseG1GC", tempDir);

    assertThat(result.status())
        .isEqualTo(TelemetryEvaluator.TelemetryStatus.SIMULATED_OR_UNVERIFIED);
  }

  @Test
  @DisplayName("Verify TelemetryEvaluator rejects runs with insufficient GC events (< 3 GCs)")
  void testInsufficientGcEventsRejection(@TempDir Path tempDir) throws IOException {
    TelemetryEvaluator evaluator = new TelemetryEvaluator();

    List<TelemetryEvaluator.SampleRun> baseline =
        List.of(
            new TelemetryEvaluator.SampleRun(150.0, 1, 99.0),
            new TelemetryEvaluator.SampleRun(150.0, 1, 99.0),
            new TelemetryEvaluator.SampleRun(150.0, 1, 99.0));
    List<TelemetryEvaluator.SampleRun> candidate =
        List.of(
            new TelemetryEvaluator.SampleRun(135.0, 5, 99.1),
            new TelemetryEvaluator.SampleRun(135.0, 5, 99.1),
            new TelemetryEvaluator.SampleRun(135.0, 5, 99.1));

    TelemetryEvaluator.EvaluationResult result =
        evaluator.evaluateSamples(baseline, candidate, true, "JAVA_OPTS=-XX:+UseG1GC", tempDir);

    assertThat(result.status())
        .isEqualTo(TelemetryEvaluator.TelemetryStatus.SIMULATED_OR_UNVERIFIED);
    assertThat(result.statusReason()).contains("Insufficient baseline GC events");
  }
}
