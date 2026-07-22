plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "autonomous-performance-tuner"

include("agent")
include("ml-tuner")
include("target-app")
