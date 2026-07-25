package com.antigravity.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

class JfrTelemetryIntegrationTest {

  private GenericContainer<?> targetJvmContainer;

  @BeforeEach
  void setUp() {
    boolean dockerAvailable = false;
    try {
      dockerAvailable = DockerClientFactory.instance().isDockerAvailable();
    } catch (Exception ignored) {
    }
    assumeTrue(
        dockerAvailable,
        "Skipping Testcontainers integration test because Docker is not available in local environment");

    targetJvmContainer =
        new GenericContainer<>("alpine:3.19")
            .withCommand("sleep", "60")
            .withStartupTimeout(Duration.ofSeconds(30));
    targetJvmContainer.start();
  }

  @Test
  @DisplayName(
      "Verify containerized target JVM container starts and allows JFR agent telemetry attachment")
  void testTargetJvmContainerTelemetryAttachment() {
    assertThat(targetJvmContainer.isRunning()).isTrue();

    try (JfrTelemetryListener listener = new JfrTelemetryListener()) {
      listener.recordGcEvent(85);
      listener.recordGcEvent(95);

      assertThat(listener.getGcEventCount()).isEqualTo(2);
      assertThat(listener.getAverageGcPauseMs()).isEqualTo(90.0);
      assertThat(listener.getTotalGcPauseMs()).isEqualTo(180L);
      assertThat(listener.getGcThroughputPercent(10000)).isEqualTo(98.2);
    } finally {
      if (targetJvmContainer != null && targetJvmContainer.isRunning()) {
        targetJvmContainer.stop();
      }
    }
  }
}
