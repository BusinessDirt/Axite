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
    ksp.arg("processor.debug", "true")
}

kotlin {
    jvmToolchain(25)
}

ksp {
    arg("processor.prefix", "Networking")
    // TODO: remove this to support global Annotations or support settings this from other projects too
    arg("processor.rootPackage", "github.businessdirt.axite.networking")
    arg("processor.moduleAnnotations", "github.businessdirt.axite.networking.annotations.RegisterPacket")
    arg("processor.RegisterPacket.interface", "github.businessdirt.axite.networking.packet.PacketRegistryProvider")
}