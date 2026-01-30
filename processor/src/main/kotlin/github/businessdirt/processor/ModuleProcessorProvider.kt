package github.businessdirt.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@AutoService(SymbolProcessorProvider::class)
class ModuleProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val config = ProcessorConfig(environment.options)

        return ModuleProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            config = config,
        )
    }
}