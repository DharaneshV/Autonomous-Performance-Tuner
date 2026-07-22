package com.antigravity.target;

public class TargetApplication {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting Target JVM Workload Application...");
        long counter = 0;
        while (true) {
            counter++;
            if (counter % 1_000_000 == 0) {
                byte[] allocation = new byte[1024 * 1024]; // Simulate GC allocations
            }
            Thread.sleep(1);
        }
    }
}
