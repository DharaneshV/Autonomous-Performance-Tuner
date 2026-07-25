package com.antigravity.tuner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GithubPrServiceTest {

  @Test
  @DisplayName("Verify GithubPrService updates jvm-flags.env when status is MEASURED_VERIFIED")
  void testJvmFlagsConfigGenerationMeasured(@TempDir Path tempDir) throws IOException {
    GithubPrService prService = new GithubPrService();
    OptimizerEngine.JvmTuningRecommendation rec =
        new OptimizerEngine.JvmTuningRecommendation(180, 16);

    TelemetryEvaluator.EvaluationResult evalResult =
        new TelemetryEvaluator.EvaluationResult(
            TelemetryEvaluator.TelemetryStatus.MEASURED_VERIFIED,
            150.0,
            2.0,
            135.0,
            1.0,
            10.0,
            "JAVA_OPTS=-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:G1HeapRegionSize=16MB",
            tempDir.resolve("report.json"),
            "Passed dual statistical gate");

    Path flagsFile = prService.updateJvmFlagsConfig(tempDir, rec, evalResult);

    assertThat(flagsFile).isNotNull();
    assertThat(flagsFile).exists();
    String content = Files.readString(flagsFile);
    assertThat(content)
        .contains("JAVA_OPTS=-XX:+UseG1GC -XX:MaxGCPauseMillis=180 -XX:G1HeapRegionSize=16MB");
  }

  @Test
  @DisplayName(
      "Verify GithubPrService refuses to update jvm-flags.env when status is SIMULATED_OR_UNVERIFIED")
  void testJvmFlagsConfigGenerationUnverifiedRefusal(@TempDir Path tempDir) throws IOException {
    GithubPrService prService = new GithubPrService();
    OptimizerEngine.JvmTuningRecommendation rec =
        new OptimizerEngine.JvmTuningRecommendation(180, 16);

    TelemetryEvaluator.EvaluationResult evalResult =
        new TelemetryEvaluator.EvaluationResult(
            TelemetryEvaluator.TelemetryStatus.SIMULATED_OR_UNVERIFIED,
            0,
            0,
            0,
            0,
            0,
            "JAVA_OPTS=-XX:+UseG1GC",
            tempDir.resolve("report.json"),
            "Synthetic telemetry mode");

    Path flagsFile = prService.updateJvmFlagsConfig(tempDir, rec, evalResult);

    assertThat(flagsFile).isNull();
    Path expectedFlagsPath = tempDir.resolve("deployments").resolve("jvm-flags.env");
    assertThat(expectedFlagsPath).doesNotExist();
  }

  @Test
  @DisplayName(
      "Verify write protection strictly respects status invariant even if improvement percentage is artificially high")
  void testWriteProtectionStrictStatusInvariant(@TempDir Path tempDir) throws IOException {
    GithubPrService prService = new GithubPrService();
    OptimizerEngine.JvmTuningRecommendation rec =
        new OptimizerEngine.JvmTuningRecommendation(180, 16);

    // Manipulated result: high improvement percentage (50%), but status remains
    // SIMULATED_OR_UNVERIFIED
    TelemetryEvaluator.EvaluationResult evalResult =
        new TelemetryEvaluator.EvaluationResult(
            TelemetryEvaluator.TelemetryStatus.SIMULATED_OR_UNVERIFIED,
            100.0,
            1.0,
            50.0,
            1.0,
            50.0,
            "JAVA_OPTS=-XX:+UseG1GC",
            tempDir.resolve("report.json"),
            "Artificially high improvement in unverified run");

    Path flagsFile = prService.updateJvmFlagsConfig(tempDir, rec, evalResult);

    assertThat(flagsFile).isNull();
    Path expectedFlagsPath = tempDir.resolve("deployments").resolve("jvm-flags.env");
    assertThat(expectedFlagsPath).doesNotExist();
  }

  @Test
  @DisplayName("Verify PR description includes bold warning when unverified")
  void testUnverifiedPrDescription() {
    GithubPrService prService = new GithubPrService();
    OptimizerEngine.JvmTuningRecommendation rec =
        new OptimizerEngine.JvmTuningRecommendation(180, 16);

    TelemetryEvaluator.EvaluationResult evalResult =
        new TelemetryEvaluator.EvaluationResult(
            TelemetryEvaluator.TelemetryStatus.SIMULATED_OR_UNVERIFIED,
            0,
            0,
            0,
            0,
            0,
            "JAVA_OPTS=-XX:+UseG1GC",
            Path.of("report.json"),
            "Synthetic telemetry mode");

    String description = prService.generatePullRequestDescription(rec, evalResult);
    assertThat(description).contains("SIMULATED - not measured");
    assertThat(description).contains("No production JVM flags were updated");
  }
}
