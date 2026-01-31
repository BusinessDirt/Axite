plugins {
    id("java-library")
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0.0"

dependencies {
    api("org.slf4j:slf4j-api:2.0.12")
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    ksp(project(":processor"))
    kspTest(project(":processor"))

    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:slf4j-simple:2.0.12")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}

ksp {
    arg("processor.debug", "true")
    arg("processor.prefix", "Events")
    // TODO: remove this to support global Annotations or support settings this from other projects too
    arg("processor.rootPackage", "github.businessdirt.events")
    arg("processor.methodAnnotations", "github.businessdirt.axite.events.HandleEvent")
    arg("processor.HandleEvent.interface", "github.businessdirt.axite.events.EventRegistryProvider")
}