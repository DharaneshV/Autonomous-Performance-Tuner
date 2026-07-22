plugins {
    java
}

dependencies {
    // Testcontainers for testing JFR agent telemetry against containerized target app
    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
}
