package com.antigravity.tuner;

import com.antigravity.agent.JfrTelemetryListener;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TunerApplication {

  public static void main(String[] args) {
    System.out.println("==========================================================");
    System.out.println("🚀 Autonomous Performance Tuner - Pure Java ML Service");
    System.out.println("==========================================================");

    try (JfrTelemetryListener listener = new JfrTelemetryListener()) {
      // Simulate live JFR telemetry stream ingestion
      listener.recordGcEvent(140);
      listener.recordGcEvent(155);
      listener.recordGcEvent(160);

      double avgPause = listener.getAverageGcPauseMs();
      System.out.printf(
          "📊 Live Telemetry Ingested: Avg GC Pause = %.2f ms (Events: %d)%n",
          avgPause, listener.getGcEventCount());

      // Run Response Surface Optimization
      OptimizerEngine optimizer = new OptimizerEngine();
      OptimizerEngine.JvmTuningRecommendation rec = optimizer.optimizeJvmParams(avgPause);

      System.out.println("\n🧠 Optimization Calculated:");
      System.out.printf("   -XX:MaxGCPauseMillis=%d%n", rec.maxGcPauseMillisMb());
      System.out.printf("   -XX:G1HeapRegionSize=%dMB%n", rec.g1HeapRegionSizeMb());

      // Generate PR configuration artifact
      GithubPrService prService = new GithubPrService();
      Path deploymentsDir = Paths.get("deployments");
      Path updatedConfig = prService.updateJvmFlagsConfig(deploymentsDir, rec);

      System.out.printf("%n📝 Generated Config Artifact: %s%n", updatedConfig.toAbsolutePath());
      System.out.println("\n--- Proposed GitHub Pull Request Payload ---");
      System.out.println(prService.generatePullRequestDescription(rec, avgPause));
      System.out.println("==========================================================");

    } catch (Exception e) {
      System.err.println("Execution error: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
