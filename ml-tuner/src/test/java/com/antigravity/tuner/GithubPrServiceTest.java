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
  @DisplayName("Verify GithubPrService updates jvm-flags.env with correct syntax")
  void testJvmFlagsConfigGeneration(@TempDir Path tempDir) throws IOException {
    GithubPrService prService = new GithubPrService();
    OptimizerEngine.JvmTuningRecommendation rec =
        new OptimizerEngine.JvmTuningRecommendation(180, 16);

    Path flagsFile = prService.updateJvmFlagsConfig(tempDir, rec);

    assertThat(flagsFile).exists();
    String content = Files.readString(flagsFile);
    assertThat(content)
        .contains("JAVA_OPTS=-XX:+UseG1GC -XX:MaxGCPauseMillis=180 -XX:G1HeapRegionSize=16MB");
  }
}
