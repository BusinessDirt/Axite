package github.businessdirt.axite.processor

import com.google.devtools.ksp.processing.KSPLogger

class ProcessorConfig(options: Map<String, String>) {

    val settings: Map<String, String> = options
        .filterKeys { it.startsWith("processor.") }
        .mapKeys { it.key.removePrefix("processor.") }

    val debug = options["processor.debug"]?.toBoolean() ?: false

    val moduleAnnotations: List<String> = options["processor.moduleAnnotations"]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf()

    val methodAnnotations: List<String> = options["processor.methodAnnotations"]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf()

    fun getSetting(key: String, default: String = ""): String {
        return settings[key] ?: default
    }

    fun getInterface(annotationName: String): String? {
        val simpleName = annotationName.substringAfterLast('.')
        return settings["$simpleName.interface"] ?: settings["$annotationName.interface"]
    }

    fun debugLog(logger: KSPLogger) {
        if (!debug) return
        if (settings.isNotEmpty()) {
            logger.warn("--- KSP Options Found ---")
            settings.forEach { (key, value) ->
                logger.warn("Option: $key = $value")
            }
            logger.warn("-------------------------")
        }
    }
}