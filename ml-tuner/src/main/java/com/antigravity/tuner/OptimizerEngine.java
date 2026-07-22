package com.antigravity.tuner;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;

/**
 * Pure Java Optimization Engine using Apache Commons Math (BOBYQA / Response Surface Optimization)
 * to calculate optimal JVM flags (e.g. MaxGCPauseMillis, G1HeapRegionSize) based on telemetry
 * feedback.
 */
public class OptimizerEngine {

  public record JvmTuningRecommendation(long maxGcPauseMillisMb, int g1HeapRegionSizeMb) {}

  /**
   * Calculates optimal JVM tuning parameters within mathematically safe bounds.
   *
   * @param currentAvgPauseMs Current average GC pause duration from telemetry
   * @return Validated JVM tuning recommendation
   */
  public JvmTuningRecommendation optimizeJvmParams(double currentAvgPauseMs) {
    // Objective function modeling estimated latency cost based on MaxGCPauseMillis parameter
    MultivariateFunction costFunction =
        point -> {
          double targetPause = point[0];
          double regionSize = point[1];
          // Penalty calculation for deviation from telemetry baseline and invalid region sizing
          double pauseError = Math.pow(targetPause - currentAvgPauseMs * 0.8, 2);
          double regionPenalty = Math.abs(regionSize - 16.0);
          return pauseError + regionPenalty;
        };

    BOBYQAOptimizer optimizer = new BOBYQAOptimizer(5);
    // Bounds: MaxGCPauseMillis [50ms, 500ms], G1HeapRegionSize [1MB, 32MB]
    SimpleBounds bounds = new SimpleBounds(new double[] {50.0, 1.0}, new double[] {500.0, 32.0});
    InitialGuess initialGuess = new InitialGuess(new double[] {200.0, 16.0});

    PointValuePair result =
        optimizer.optimize(
            new MaxEval(1000),
            new ObjectiveFunction(costFunction),
            GoalType.MINIMIZE,
            bounds,
            initialGuess);

    long recommendedPause = Math.round(result.getPoint()[0]);
    int recommendedRegion = (int) Math.round(result.getPoint()[1]);

    return new JvmTuningRecommendation(recommendedPause, recommendedRegion);
  }
}
