package github.businessdirt.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

class ModuleProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val config: ProcessorConfig
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val debug = config.getSetting("debug", "false").toBoolean()
        if (debug) config.debugLog(logger)

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
        logger.warn("Generating modules for $annotationName")
        val simpleName = annotationName.substringAfterLast(".")
        val packageName = config.getSetting("rootPackage", "com") + ".generated"
        val className = "${config.getSetting("prefix")}${simpleName}Registry"

        // List<KClass> type
        val kClassType = KClass::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(ANY))
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

        val registryObject = TypeSpec.objectBuilder(className)
            .addProperty(
                PropertySpec.builder("modules", listType)
                    .addModifiers(KModifier.PUBLIC)
                    .initializer(listCodeBlock)
                    .build()
            )
            .build()

        FileSpec.builder(packageName, className)
            .addType(registryObject)
            .build()
            .writeTo(codeGenerator, Dependencies(false, *classes.mapNotNull { it.containingFile }.toTypedArray()))
    }
}