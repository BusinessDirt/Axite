import org.gradle.internal.os.OperatingSystem

val lwjglVersion = "3.4.1"
val jomlVersion = "1.10.8"
val jomlPrimitivesVersion = "1.10.0"

plugins {
    kotlin("plugin.serialization")
}

val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else if (arch.startsWith("ppc"))
                "natives-linux-ppc64le"
            else if (arch.startsWith("riscv"))
                "natives-linux-riscv64"
            else
                "natives-linux"
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) }     ->
            "natives-macos-arm64"
        arrayOf("Windows").any { name.startsWith(it) }                ->
            if (arch.contains("64"))
                "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            else
                "natives-windows-x86"
        else                                                                            ->
            throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
    }
}

val imguiNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else
                "natives-linux"

        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos" // ImGui uses a universal binary, no -arm64 suffix needed!

        arrayOf("Windows").any { name.startsWith(it) } ->
            if (arch.contains("64")) "natives-windows" else "natives-windows-x86"

        else -> throw Error("Unsupported platform for ImGui.")
    }
}

dependencies {
    // Project Modules
    implementation(project(":logging"))

    // LWJGL Bill of Materials (BOM)
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    // LWJGL API Libraries
    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-assimp")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-shaderc")
    implementation("org.lwjgl:lwjgl-spvc")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-vma")
    implementation("org.lwjgl:lwjgl-vulkan")

    // LWJGL Native Runtimes
    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-assimp::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-openal::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-shaderc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-spvc::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-vma::$lwjglNatives")

    // Vulkan macOS native arm64 conditional check
    if (lwjglNatives == "natives-macos-arm64") {
        runtimeOnly("org.lwjgl:lwjgl-vulkan::$lwjglNatives")
    }

    // Math & Primitives (JOML)
    implementation("org.joml:joml:$jomlVersion")
    implementation("org.joml:joml-primitives:$jomlPrimitivesVersion")

    // Kotlinx Ecosystem
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // ECS Framework
    implementation("io.github.quillraven.fleks:Fleks:2.13")

    // ImGui Bindings
    implementation("io.github.spair:imgui-java-binding:1.92.0")
    implementation("io.github.spair:imgui-java-lwjgl3:1.92.0")
    runtimeOnly("io.github.spair:imgui-java-$imguiNatives:1.92.0")
}

sourceSets {
    create("sandbox") {
        kotlin.srcDir("src/sandbox/kotlin")
        resources.srcDir("src/sandbox/resources")
        // Allow sandbox to see the engine code
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

tasks.named<ProcessResources>("processSandboxResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

configurations {
    named("sandboxImplementation") { extendsFrom(configurations.implementation.get()) }
    named("sandboxRuntimeOnly") { extendsFrom(configurations.runtimeOnly.get()) }
}

tasks.register<JavaExec>("run") {
    group = "application"
    description = "Runs the Vulkan Engine with OS-specific LWJGL and LunarG SDK configurations"
    mainClass.set("github.businessdirt.axite.vanadium.MainKt")
    classpath = sourceSets["sandbox"].runtimeClasspath

    if (OperatingSystem.current().isMacOsX) {
        println("⚙️  Configuring Vulkan Environment for macOS...")

        jvmArgs(
            "-XstartOnFirstThread",
            "--enable-native-access=ALL-UNNAMED",
            "-Dorg.lwjgl.vulkan.libname=libvulkan.1.dylib",
            "-Djoml.nounsafe=true"
        )

    } else {
        println("⚙️  Configuring Vulkan Environment for Windows/Linux...")

        jvmArgs(
            "--enable-native-access=ALL-UNNAMED",
            "-Djoml.nounsafe=true"
        )
    }
}