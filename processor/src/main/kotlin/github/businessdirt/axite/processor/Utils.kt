package github.businessdirt.axite.processor

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.WildcardTypeName

object Utils {

    fun generateClassName(annotationName: String, config: ProcessorConfig): String =
        "${config.getSetting("prefix")}${annotationName.substringAfterLast(".")}Registry"

    fun generatePackageName(config: ProcessorConfig): String =
        config.getSetting("rootPackage", "com") + ".generated"

    fun ClassName.wildcardParameter() =
        this.parameterizedBy(STAR)
}