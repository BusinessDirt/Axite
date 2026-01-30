package github.businessdirt.processor

import com.google.devtools.ksp.processing.KSPLogger

class ProcessorConfig(options: Map<String, String>) {

    val settings: Map<String, String> = options
        .filterKeys { it.startsWith("processor.") }
        .mapKeys { it.key.removePrefix("processor.") }

    val moduleAnnotations: List<String> = options["processor.moduleAnnotations"]
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf()

    fun getSetting(key: String, default: String = ""): String {
        return settings[key] ?: default
    }

    fun debugLog(logger: KSPLogger) {
        if (settings.isNotEmpty()) {
            logger.warn("--- KSP Options Found ---")
            settings.forEach { (key, value) ->
                logger.warn("Option: $key = $value")
            }
            logger.warn("-------------------------")
        }
    }
}