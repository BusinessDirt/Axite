val ktorVersion = "2.3.x" // Use the latest stable version

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