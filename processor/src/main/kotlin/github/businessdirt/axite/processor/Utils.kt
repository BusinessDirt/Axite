package github.businessdirt.axite.processor

import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR

object Utils {

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