package com.antigravity.agent;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;

/**
 * Production JFR Telemetry Listener using Java 17 RecordingStream API to capture live JVM Garbage
 * Collection events with zero external agent overhead.
 */
public class JfrTelemetryListener implements AutoCloseable {

  private final AtomicLong gcPauseTotalMs = new AtomicLong(0);
  private final AtomicLong gcEventCount = new AtomicLong(0);
  private RecordingStream recordingStream;

  /** Starts listening to live JDK Flight Recorder events. */
  public void startStreaming() {
    try {
      recordingStream = new RecordingStream();
      // Subscribe to GC Phase Pause JFR events
      recordingStream.onEvent("jdk.GCPhasePause", this::handleGcPauseEvent);
      recordingStream.onEvent("jdk.GarbageCollection", this::handleGcPauseEvent);
      recordingStream.startAsync();
    } catch (Exception e) {
      System.err.println(
          "JFR RecordingStream unavailable in current JVM context: " + e.getMessage());
    }
  }

  private void handleGcPauseEvent(RecordedEvent event) {
    if (event.hasDuration("duration")) {
      Duration duration = event.getDuration("duration");
      recordGcEvent(duration.toMillis());
    }
  }

  public void recordGcEvent(long pauseTimeMs) {
    gcPauseTotalMs.addAndGet(pauseTimeMs);
    gcEventCount.incrementAndGet();
  }

  public double getAverageGcPauseMs() {
    long count = gcEventCount.get();
    return count == 0 ? 0.0 : (double) gcPauseTotalMs.get() / count;
  }

  public long getGcEventCount() {
    return gcEventCount.get();
  }

  @Override
  public void close() {
    if (recordingStream != null) {
      recordingStream.close();
    }
  }
}
