import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    kotlin("plugin.allopen") version "2.3.0" // Matches your Kotlin version
    id("org.jetbrains.kotlinx.benchmark") version "0.4.13"
}

group = "github.businessdirt"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // --- Core Dependencies ---
    api("org.slf4j:slf4j-api:2.0.12")

    // Kotlin Standard Library & Coroutines
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // --- KSP Processor ---
    ksp(project(":processor"))
    kspTest(project(":processor"))

    // --- Test Dependencies ---
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.13")

    // Mocking
    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")
}

// --- Toolchain Configuration ---
kotlin {
    jvmToolchain(25)
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

// --- KSP Configuration ---
ksp {
    arg("processor.prefix", "Commands")
    arg("processor.moduleAnnotations", "github.businessdirt.axite.commands.registry.RegisterCommand")
    arg("processor.RegisterCommand.interface", "github.businessdirt.axite.commands.registry.CommandRegistryProvider")
}

// --- Sources Jar Task ---
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// --- Test Configuration ---
tasks.test {
    useJUnitPlatform()

    // Pass arguments to KSP and JVM during tests
    systemProperty("ksp.processor.debug", "true")
    jvmArgs("-XX:+EnableDynamicAgentLoading")

    // Enhanced logging
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showExceptions = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

// --- Artifacts ---
artifacts {
    add("archives", sourcesJar)
}