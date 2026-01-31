package github.businessdirt.axite.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import github.businessdirt.axite.processor.Utils.wildcardParameter
import kotlin.reflect.KFunction

class MethodProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val config: ProcessorConfig
) : SymbolProcessor {

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
        val packageName = Utils.generatePackageName(config)
        val className = Utils.generateClassName(annotationName, config)
        val interfaceName = config.getInterface(annotationName)

        val kFunctionType = KFunction::class.asClassName().wildcardParameter()
        val listType = List::class.asClassName().parameterizedBy(kFunctionType)

        val listCodeBlock = CodeBlock.builder().apply {
            add("listOf(\n")
            indent()
            methods.forEachIndexed { index, method ->
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

        val registryType = if (interfaceName != null) {
             TypeSpec.classBuilder(className)
                 .addSuperinterface(ClassName.bestGuess(interfaceName))
        } else {
            TypeSpec.objectBuilder(className)
        }

        registryType.addProperty(methodsProperty)

        FileSpec.builder(packageName, className)
            .addType(registryType.build())
            .build()
            .writeTo(codeGenerator, Dependencies(false, *methods.mapNotNull { it.containingFile }.toTypedArray()))

        if (interfaceName != null) {
            val resourceFile = codeGenerator.createNewFile(
                Dependencies(false, *methods.mapNotNull { it.containingFile }.toTypedArray()),
                "META-INF.services",
                interfaceName,
                ""
            )
            resourceFile.write("$packageName.$className".toByteArray())
            resourceFile.close()
        }
    }
}