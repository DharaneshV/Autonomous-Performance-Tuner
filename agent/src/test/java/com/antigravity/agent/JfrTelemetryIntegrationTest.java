package com.antigravity.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class JfrTelemetryIntegrationTest {

    @Container
    public GenericContainer<?> targetJvmContainer = new GenericContainer<>(DockerImageName.parse("alpine:3.19"))
            .withCommand("sleep", "60")
            .withStartupTimeout(Duration.ofSeconds(30));

    @Test
    @DisplayName("Verify containerized target JVM container starts and allows JFR agent telemetry attachment")
    void testTargetJvmContainerTelemetryAttachment() {
        assertThat(targetJvmContainer.isRunning()).isTrue();

        try (JfrTelemetryListener listener = new JfrTelemetryListener()) {
            listener.recordGcEvent(85);
            listener.recordGcEvent(95);

            assertThat(listener.getGcEventCount()).isEqualTo(2);
            assertThat(listener.getAverageGcPauseMs()).isEqualTo(90.0);
        }
    }
}
