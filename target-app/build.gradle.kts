plugins {
    java
    application
}

application {
    mainClass.set("com.antigravity.target.TargetApplication")
}

dependencies {
    // Lightweight HTTP server / workload generator dependencies
    implementation("com.sun.net.httpserver:http:20070405")
}
