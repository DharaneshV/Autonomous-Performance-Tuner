package com.antigravity.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JfrTelemetryListenerTest {

  @Test
  @DisplayName(
      "Verify JfrTelemetryListener accurately calculates GC pause total, event count, average, and throughput")
  void testJfrTelemetryListenerCalculations() {
    try (JfrTelemetryListener listener = new JfrTelemetryListener()) {
      listener.recordGcEvent(85);
      listener.recordGcEvent(95);

      assertThat(listener.getGcEventCount()).isEqualTo(2);
      assertThat(listener.getAverageGcPauseMs()).isEqualTo(90.0);
      assertThat(listener.getTotalGcPauseMs()).isEqualTo(180L);
      assertThat(listener.getGcThroughputPercent(10000)).isEqualTo(98.2);
    }
  }
}
