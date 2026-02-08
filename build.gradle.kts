import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.gradle.api.plugins.JavaPluginExtension

plugins {
    kotlin("jvm") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.3.3" apply false
    kotlin("plugin.allopen") version "2.3.0" apply false
    id("org.jetbrains.kotlinx.benchmark") version "0.4.13" apply false
}

group = "github.businessdirt"
version = "1.0.0"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "com.google.devtools.ksp")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    configure<KotlinJvmProjectExtension> {
        jvmToolchain(25)
    }

    dependencies {
        // Core Dependencies
        "implementation"(kotlin("stdlib"))
        "implementation"(kotlin("reflect"))
        "api"("org.slf4j:slf4j-api:2.0.12")

        // KSP (except for processor itself)
        if (project.name != "processor") {
            "ksp"(project(":processor"))
            "kspTest"(project(":processor"))
        }

        // Test Dependencies
        "testImplementation"(kotlin("test"))
        "testImplementation"("org.slf4j:slf4j-simple:2.0.12")
        "testImplementation"("org.mockito:mockito-core:5.21.0")
        "testImplementation"("org.mockito:mockito-junit-jupiter:5.21.0")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:6.2.3")
    }

    // Sources Jar Task
    val sourcesJar by tasks.registering(Jar::class) {
        archiveClassifier.set("sources")
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        from(javaExtension.sourceSets.getByName("main").allSource)
    }

    artifacts {
        add("archives", sourcesJar)
    }

    // Test Configuration
    tasks.withType<Test> {
        useJUnitPlatform()

        systemProperty("ksp.processor.debug", "true")
        jvmArgs("-XX:+EnableDynamicAgentLoading")

        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
            showExceptions = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}