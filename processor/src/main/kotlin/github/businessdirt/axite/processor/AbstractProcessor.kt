package github.businessdirt.axite.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

abstract class AbstractProcessor(
    protected val codeGenerator: CodeGenerator,
    protected val logger: KSPLogger,
    protected val config: ProcessorConfig
) : SymbolProcessor {

    fun getRegistryType(className: String, interfaceName: String? = null): TypeSpec.Builder = when {
        interfaceName != null -> TypeSpec.classBuilder(className)
            .addSuperinterface(ClassName.bestGuess(interfaceName))
        else -> TypeSpec.objectBuilder(className)
    }

    fun generateRegistryCode(
        packageName: String,
        className: String,
        registryType: TypeSpec.Builder,
        declarations: List<KSDeclaration>,
    ) {
        val dependencies = Dependencies(
            false,
            *declarations.mapNotNull { it.containingFile }.toTypedArray()
        )

        FileSpec.builder(packageName, className)
            .addType(registryType.build())
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    fun writeRegistryCodeToFile(
        interfaceName: String,
        packageName: String,
        className: String,
        declarations: List<KSDeclaration>,
    ) {
        val resourceFile = codeGenerator.createNewFile(
            Dependencies(false, *declarations.mapNotNull { it.containingFile }.toTypedArray()),
            "META-INF.services",
            interfaceName,
            ""
        )

        resourceFile.write("$packageName.$className".toByteArray())
        resourceFile.close()
    }

    fun generateClassName(annotationName: String, config: ProcessorConfig): String =
        "${config.getSetting("prefix")}${annotationName.substringAfterLast(".")}Registry"

    fun generatePackageName(config: ProcessorConfig, symbols: List<KSDeclaration>): String {
        val configuredPkg = config.settings["rootPackage"]
        if (configuredPkg != null) return "$configuredPkg.generated"

        val fallbackPkg = symbols.firstOrNull()?.packageName?.asString() ?: "com"
        return if (fallbackPkg.isEmpty()) "generated" else "$fallbackPkg.generated"
    }

    fun ClassName.wildcardParameter() =
        this.parameterizedBy(STAR)
}