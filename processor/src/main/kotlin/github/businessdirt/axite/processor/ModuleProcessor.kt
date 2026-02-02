package github.businessdirt.axite.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import github.businessdirt.axite.processor.Utils.wildcardParameter
import kotlin.reflect.KClass

class ModuleProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val config: ProcessorConfig
) : SymbolProcessor {

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
        val packageName = Utils.generatePackageName(config, classes)
        val className = Utils.generateClassName(annotationName, config)
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

        val registryType = if (interfaceName != null) {
            TypeSpec.classBuilder(className)
                .addSuperinterface(ClassName.bestGuess(interfaceName))
        } else {
            TypeSpec.objectBuilder(className)
        }

        registryType.addProperty(modulesProperty)

        FileSpec.builder(packageName, className)
            .addType(registryType.build())
            .build()
            .writeTo(codeGenerator, Dependencies(false, *classes.mapNotNull { it.containingFile }.toTypedArray()))

        if (interfaceName != null) {
            val resourceFile = codeGenerator.createNewFile(
                Dependencies(false, *classes.mapNotNull { it.containingFile }.toTypedArray()),
                "META-INF.services",
                interfaceName,
                ""
            )
            resourceFile.write("$packageName.$className".toByteArray())
            resourceFile.close()
        }
    }
}