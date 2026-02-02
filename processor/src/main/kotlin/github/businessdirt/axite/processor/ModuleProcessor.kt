package github.businessdirt.axite.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

class ModuleProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    config: ProcessorConfig
) : AbstractProcessor(codeGenerator, logger, config) {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        config.debugLog(logger)
        config.moduleAnnotations.forEach { annotationName ->
            val symbols = resolver.getSymbolsWithAnnotation(annotationName)
            val classes = symbols.filterIsInstance<KSClassDeclaration>().toList()

            // Only generate a file if there are actually classes using this annotation
            if (classes.isNotEmpty()) {
                generateRegistryForAnnotation(classes, annotationName)
            }
        }

        return emptyList()
    }

    private fun generateRegistryForAnnotation(classes: List<KSClassDeclaration>, annotationName: String) {
        logger.warn("Axite: Generating modules for $annotationName")
        val packageName = this.generatePackageName(config, classes)
        val className = this.generateClassName(annotationName, config)
        val interfaceName = config.getInterface(annotationName)

        // List<KClass> type
        val kClassType = KClass::class.asClassName().wildcardParameter()
        val listType = List::class.asClassName().parameterizedBy(kClassType)

        val listCodeBlock = CodeBlock.builder().apply {
            add("listOf(\n")
            indent()
            classes.forEachIndexed { index, cls ->
                // Convert the KSClassDeclaration into a KotlinPoet ClassName
                // This automatically handles the imports in the generated file
                val classType = cls.toClassName()

                val suffix = if (index == classes.lastIndex) "" else ","
                add("%T::class%L\n", classType, suffix)
            }
            unindent()
            add(")")
        }.build()

        val modulesProperty = PropertySpec.builder("modules", listType)
            .addModifiers(KModifier.PUBLIC)
            .apply { if (interfaceName != null) addModifiers(KModifier.OVERRIDE) }
            .initializer(listCodeBlock)
            .build()

        val registryType = this.getRegistryType(className, interfaceName)
        registryType.addProperty(modulesProperty)

        this.generateRegistryCode(packageName, className, registryType, classes)
        this.writeRegistryCodeToFile(interfaceName ?: return, packageName, className, classes)
    }
}