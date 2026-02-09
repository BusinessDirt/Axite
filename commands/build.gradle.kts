plugins {
    kotlin("plugin.allopen")
    id("org.jetbrains.kotlinx.benchmark")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // Test Dependencies
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.13")
}

// Configure classes to be open for benchmarks
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("test")
    }
}