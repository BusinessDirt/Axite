val ktorVersion = "3.4.0" // Use the latest stable version

plugins {
    kotlin("plugin.serialization")
}

dependencies {
    // Core Ktor Client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion") // CIO is a fast coroutine-based engine

    // JSON Serialization
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Optional: Logging
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
}