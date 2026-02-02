package github.businessdirt.axite.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@AutoService(SymbolProcessorProvider::class)
class ModuleProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = ModuleProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
        config = ProcessorConfig(environment.options),
    )
}

@AutoService(SymbolProcessorProvider::class)
class MethodProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = MethodProcessor(
        codeGenerator = environment.codeGenerator,
        logger = environment.logger,
        config = ProcessorConfig(environment.options),
    )
}