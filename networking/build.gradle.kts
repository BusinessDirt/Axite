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
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}

ksp {
    arg("processor.debug", "true")
    arg("processor.prefix", "Networking")
    arg("processor.rootPackage", "github.businessdirt")
    arg("processor.moduleAnnotations", "github.businessdirt.networking.annotations.RegisterPacket")
}