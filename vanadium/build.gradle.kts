import org.gradle.internal.os.OperatingSystem

val lwjglVersion = "3.4.1"
val jomlVersion = "1.10.8"
val `joml-primitivesVersion` = "1.10.0"

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

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-assimp")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-shaderc")
    implementation("org.lwjgl", "lwjgl-spvc")
    implementation("org.lwjgl", "lwjgl-stb")
    implementation("org.lwjgl", "lwjgl-vma")
    implementation("org.lwjgl", "lwjgl-vulkan")
    implementation ("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    implementation ("org.lwjgl", "lwjgl-assimp", classifier = lwjglNatives)
    implementation ("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    implementation ("org.lwjgl", "lwjgl-openal", classifier = lwjglNatives)
    implementation ("org.lwjgl", "lwjgl-shaderc", classifier = lwjglNatives)
    implementation ("org.lwjgl", "lwjgl-spvc", classifier = lwjglNatives)
    implementation ("org.lwjgl", "lwjgl-stb", classifier = lwjglNatives)
    implementation ("org.lwjgl", "lwjgl-vma", classifier = lwjglNatives)
    if (lwjglNatives == "natives-macos-arm64") implementation ("org.lwjgl", "lwjgl-vulkan", classifier = lwjglNatives)
    implementation("org.joml", "joml", jomlVersion)
    implementation("org.joml", "joml-primitives", `joml-primitivesVersion`)
}