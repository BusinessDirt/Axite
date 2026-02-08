dependencies {
    // The KSP API itself
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.3")

    // Google AutoService to handle the META-INF/services registration
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    // This is the KSP-compatible version of AutoService that generates
    // the provider files during the KSP round
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.2.0")

    // (Optional) KotlinPoet is highly recommended for generating code
    implementation("com.squareup:kotlinpoet:1.16.0")
    implementation("com.squareup:kotlinpoet-ksp:1.16.0")
}
