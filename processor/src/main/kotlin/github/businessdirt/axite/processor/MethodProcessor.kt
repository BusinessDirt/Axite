package github.businessdirt.axite.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KFunction

class MethodProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger,
    config: ProcessorConfig
) : AbstractProcessor(codeGenerator, logger, config) {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        config.debugLog(logger)
        config.methodAnnotations.forEach { annotationName ->
            val symbols = resolver.getSymbolsWithAnnotation(annotationName)
            val methods = symbols.filterIsInstance<KSFunctionDeclaration>()
                .filter { it.parentDeclaration is KSClassDeclaration }
                .toList()

            // Only generate a file if there are actually methods using this annotation
            if (methods.isNotEmpty()) {
                generateRegistryForAnnotation(methods, annotationName)
            }
        }

        return emptyList()
    }

    private fun generateRegistryForAnnotation(methods: List<KSFunctionDeclaration>, annotationName: String) {
        logger.warn("Axite: Generating methods for $annotationName")
        val packageName = this.generatePackageName(config, methods)
        val className = this.generateClassName(annotationName, config)
        val interfaceName = config.getInterface(annotationName)

        val kFunctionType = KFunction::class.asClassName().wildcardParameter()
        val listType = List::class.asClassName().parameterizedBy(kFunctionType)

        val listCodeBlock = CodeBlock.builder().apply {
            add("listOf(\n")
            indent()
            methods.forEachIndexed { _, method ->
                val classType = (method.parentDeclaration as KSClassDeclaration).toClassName()
                add("%T::%N,\n", classType, method.simpleName.asString())
            }
            unindent()
            add(")")
        }.build()

        val methodsProperty = PropertySpec.builder("methods", listType)
            .addModifiers(KModifier.PUBLIC)
            .apply { if (interfaceName != null) addModifiers(KModifier.OVERRIDE) }
            .initializer(listCodeBlock)
            .build()

        val registryType = this.getRegistryType(className, interfaceName)
        registryType.addProperty(methodsProperty)

        this.generateRegistryCode(packageName, className, registryType, methods)
        this.writeRegistryCodeToFile(interfaceName ?: return, packageName, className, methods)
    }
}