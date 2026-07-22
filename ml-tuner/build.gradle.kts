plugins {
    java
    application
}

application {
    mainClass.set("com.antigravity.tuner.TunerApplication")
}

dependencies {
    // Apache Commons Math for Gaussian Process / Response Surface Optimization
    implementation("org.apache.commons:commons-math3:3.6.1")
    // Jackson for JSON parsing and config handling
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    
    // Dependent on agent module for JFR data models
    implementation(project(":agent"))
}
