package com.antigravity.tuner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evaluates multi-sample baseline vs candidate GC telemetry runs, applies dual statistical safety
 * gates (>=5% improvement and variance clearance), and outputs execution reports.
 */
public class TelemetryEvaluator {

  public enum TelemetryStatus {
    MEASURED_VERIFIED,
    SIMULATED_OR_UNVERIFIED
  }

  public record SampleRun(double avgGcPauseMs, long gcCount, double throughputPercent) {}

  public record EvaluationResult(
      TelemetryStatus status,
      double baselineMedianMs,
      double baselineStdDev,
      double candidateMedianMs,
      double candidateStdDev,
      double improvementPercent,
      String rollbackFlags,
      Path reportFilePath,
      String statusReason) {}

  /** Evaluates telemetry samples and enforces dual statistical safety gates. */
  public EvaluationResult evaluateSamples(
      List<SampleRun> baselineRuns,
      List<SampleRun> candidateRuns,
      boolean isRealTelemetry,
      String currentFlags,
      Path baseOutputDir)
      throws IOException {

    if (!isRealTelemetry
        || baselineRuns == null
        || candidateRuns == null
        || baselineRuns.isEmpty()
        || candidateRuns.isEmpty()) {
      Path reportFile =
          saveReport(
              TelemetryStatus.SIMULATED_OR_UNVERIFIED,
              0,
              0,
              0,
              0,
              0,
              currentFlags,
              "Synthetic mode or missing telemetry data",
              baseOutputDir);
      return new EvaluationResult(
          TelemetryStatus.SIMULATED_OR_UNVERIFIED,
          0,
          0,
          0,
          0,
          0,
          currentFlags,
          reportFile,
          "Synthetic or missing telemetry");
    }

    // Check sample adequacy gate: each run must have >= 3 GC events
    for (SampleRun run : baselineRuns) {
      if (run.gcCount() < 3) {
        Path reportFile =
            saveReport(
                TelemetryStatus.SIMULATED_OR_UNVERIFIED,
                0,
                0,
                0,
                0,
                0,
                currentFlags,
                "Baseline run recorded insufficient GC events (< 3 GCs)",
                baseOutputDir);
        return new EvaluationResult(
            TelemetryStatus.SIMULATED_OR_UNVERIFIED,
            0,
            0,
            0,
            0,
            0,
            currentFlags,
            reportFile,
            "Insufficient baseline GC events (< 3 GCs)");
      }
    }
    for (SampleRun run : candidateRuns) {
      if (run.gcCount() < 3) {
        Path reportFile =
            saveReport(
                TelemetryStatus.SIMULATED_OR_UNVERIFIED,
                0,
                0,
                0,
                0,
                0,
                currentFlags,
                "Candidate run recorded insufficient GC events (< 3 GCs)",
                baseOutputDir);
        return new EvaluationResult(
            TelemetryStatus.SIMULATED_OR_UNVERIFIED,
            0,
            0,
            0,
            0,
            0,
            currentFlags,
            reportFile,
            "Insufficient candidate GC events (< 3 GCs)");
      }
    }

    double baselineMedian = calculateMedian(baselineRuns);
    double baselineStdDev = calculateStdDev(baselineRuns, baselineMedian);
    double candidateMedian = calculateMedian(candidateRuns);
    double candidateStdDev = calculateStdDev(candidateRuns, candidateMedian);

    double improvement = ((baselineMedian - candidateMedian) / baselineMedian) * 100.0;

    // Dual Statistical Gate check:
    // 1. Relative improvement gate: >= 5% median pause reduction
    // 2. Variance confidence gate: (baselineMedian - candidateMedian) > (baselineStdDev +
    // candidateStdDev)
    boolean passesImprovementGate = candidateMedian <= (baselineMedian * 0.95);
    boolean passesVarianceGate =
        (baselineMedian - candidateMedian) > (baselineStdDev + candidateStdDev);

    TelemetryStatus status;
    String reason;

    if (passesImprovementGate && passesVarianceGate) {
      status = TelemetryStatus.MEASURED_VERIFIED;
      reason =
          String.format(
              "Passed dual statistical gate: %.2f%% median pause improvement exceeds variance margin",
              improvement);
    } else {
      status = TelemetryStatus.SIMULATED_OR_UNVERIFIED;
      reason =
          String.format(
              "Failed dual statistical gate (Improvement: %.2f%% [min 5%% required: %b], Variance Clearance: %b)",
              improvement, passesImprovementGate, passesVarianceGate);
    }

    Path reportFile =
        saveReport(
            status,
            baselineMedian,
            baselineStdDev,
            candidateMedian,
            candidateStdDev,
            improvement,
            currentFlags,
            reason,
            baseOutputDir);

    return new EvaluationResult(
        status,
        baselineMedian,
        baselineStdDev,
        candidateMedian,
        candidateStdDev,
        improvement,
        currentFlags,
        reportFile,
        reason);
  }

  public double calculateMedian(List<SampleRun> runs) {
    List<Double> pauses = new ArrayList<>();
    for (SampleRun r : runs) {
      pauses.add(r.avgGcPauseMs());
    }
    Collections.sort(pauses);
    int size = pauses.size();
    if (size % 2 == 0) {
      return (pauses.get(size / 2 - 1) + pauses.get(size / 2)) / 2.0;
    } else {
      return pauses.get(size / 2);
    }
  }

  public double calculateStdDev(List<SampleRun> runs, double mean) {
    if (runs.size() <= 1) return 0.0;
    double sumSquaredDiff = 0.0;
    for (SampleRun r : runs) {
      sumSquaredDiff += Math.pow(r.avgGcPauseMs() - mean, 2);
    }
    return Math.sqrt(sumSquaredDiff / (runs.size() - 1));
  }

  private Path saveReport(
      TelemetryStatus status,
      double baselineMedian,
      double baselineStdDev,
      double candidateMedian,
      double candidateStdDev,
      double improvementPercent,
      String rollbackFlags,
      String reason,
      Path baseOutputDir)
      throws IOException {

    LocalDateTime now = LocalDateTime.now();
    String dateDirStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    String timeFileStr = now.format(DateTimeFormatter.ofPattern("HHmm"));

    Path runsDir = baseOutputDir.resolve("automation").resolve("runs").resolve(dateDirStr);
    Files.createDirectories(runsDir);
    Path reportFile = runsDir.resolve(timeFileStr + ".json");

    String jsonContent =
        String.format(
            """
        {
          "timestamp": "%s",
          "status": "%s",
          "statusReason": "%s",
          "baseline": {
            "medianPauseMs": %.2f,
            "stdDevMs": %.2f
          },
          "candidate": {
            "medianPauseMs": %.2f,
            "stdDevMs": %.2f
          },
          "improvementPercent": %.2f,
          "rollbackFlags": "%s"
        }
        """,
            now.toString(),
            status.name(),
            reason.replace("\"", "\\\""),
            baselineMedian,
            baselineStdDev,
            candidateMedian,
            candidateStdDev,
            improvementPercent,
            rollbackFlags.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""));

    Files.writeString(reportFile, jsonContent);
    return reportFile;
  }
}
