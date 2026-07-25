package com.antigravity.target;

import java.util.ArrayList;
import java.util.List;

public class TargetApplication {

  public static void main(String[] args) throws InterruptedException {
    System.out.println("Starting Target JVM Workload Application...");
    long startTime = System.currentTimeMillis();
    long durationMs = 0;
    if (args.length > 0) {
      try {
        durationMs = Long.parseLong(args[0]) * 1000L;
      } catch (NumberFormatException ignored) {
      }
    }

    List<byte[]> transientList = new ArrayList<>();
    long allocations = 0;

    while (true) {
      // Allocate 512KB chunks continuously to induce garbage collection
      transientList.add(new byte[512 * 1024]);
      allocations++;

      if (transientList.size() > 50) {
        // Discard references to trigger GC collection cycles
        transientList.clear();
      }

      if (durationMs > 0 && (System.currentTimeMillis() - startTime) >= durationMs) {
        System.out.printf("Target workload complete: %d allocations performed.%n", allocations);
        break;
      }

      Thread.sleep(2);
    }
  }
}
