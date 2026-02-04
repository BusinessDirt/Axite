plugins {
    id("java-library")
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0.0"

dependencies {
    api("io.netty:netty-all:4.2.9.Final")
    api("org.slf4j:slf4j-api:2.0.12")
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    ksp(project(":processor"))
    kspTest(project(":processor"))

    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:slf4j-simple:2.0.12")

    testImplementation("org.mockito:mockito-core:5.21.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.21.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")
}

tasks.test {
    useJUnitPlatform()
    ksp.arg("processor.debug", "true")
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

kotlin {
    jvmToolchain(25)
}

ksp {
    arg("processor.prefix", "Commands")
    arg("processor.moduleAnnotations", "github.businessdirt.axite.commands.registry.RegisterCommand")
    arg("processor.RegisterCommand.interface", "github.businessdirt.axite.commands.registry.CommandRegistryProvider")
}