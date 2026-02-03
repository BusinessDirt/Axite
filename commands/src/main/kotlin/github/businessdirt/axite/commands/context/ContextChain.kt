package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.ResultConsumer
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import java.util.*

class ContextChain<S>(
    private val modifiers: List<CommandContext<S>>,
    private val executable: CommandContext<S>
) {
    private var nextStageCache: ContextChain<S>? = null

    init {
        requireNotNull(executable.command) { "Last command in chain must be executable" }
    }

    enum class Stage { MODIFY, EXECUTE }

    val stage: Stage get() = if (modifiers.isEmpty()) Stage.EXECUTE else Stage.MODIFY

    val topContext: CommandContext<S> get() = modifiers.firstOrNull() ?: executable

    fun nextStage(): ContextChain<S>? {
        if (modifiers.isEmpty()) return null

        if (nextStageCache == null) {
            nextStageCache = ContextChain(modifiers.drop(1), executable)
        }
        return nextStageCache
    }

    @Throws(CommandSyntaxException::class)
    fun executeAll(source: S, resultConsumer: ResultConsumer<S>): Int {
        if (modifiers.isEmpty()) {
            return runExecutable(executable, source, resultConsumer, false)
        }

        var forkedMode = false
        var currentSources = listOf(source)

        for (modifier in modifiers) {
            forkedMode = forkedMode or modifier.isForked

            val nextSources = currentSources.flatMap { src ->
                runModifier(modifier, src, resultConsumer, forkedMode)
            }

            if (nextSources.isEmpty()) return 0
            currentSources = nextSources
        }

        return currentSources.sumOf { src ->
            runExecutable(executable, src, resultConsumer, forkedMode)
        }
    }

    companion object {
        fun <S> tryFlatten(rootContext: CommandContext<S>): Optional<ContextChain<S>> {
            val modifiers = mutableListOf<CommandContext<S>>()
            var current = rootContext

            while (true) {
                val child = current.child
                if (child == null) {
                    if (current.command == null) return Optional.empty()
                    return Optional.of(ContextChain(modifiers, current))
                }
                modifiers.add(current)
                current = child
            }
        }

        @Throws(CommandSyntaxException::class)
        fun <S> runModifier(
            modifier: CommandContext<S>,
            source: S,
            resultConsumer: ResultConsumer<S>,
            forkedMode: Boolean
        ): Collection<S> {
            val sourceModifier = modifier.modifier ?: return listOf(source)
            val contextToUse = modifier.copyFor(source)

            return try {
                sourceModifier.apply(contextToUse)
            } catch (ex: CommandSyntaxException) {
                resultConsumer.onCommandComplete(contextToUse, false, 0)
                if (forkedMode) emptyList() else throw ex
            }
        }

        @Throws(CommandSyntaxException::class)
        fun <S> runExecutable(
            executable: CommandContext<S>,
            source: S,
            resultConsumer: ResultConsumer<S>,
            forkedMode: Boolean
        ): Int {
            val contextToUse = executable.copyFor(source)
            return try {
                val result = executable.command!!.run(contextToUse)
                resultConsumer.onCommandComplete(contextToUse, true, result)
                if (forkedMode) 1 else result
            } catch (ex: CommandSyntaxException) {
                resultConsumer.onCommandComplete(contextToUse, false, 0)
                if (forkedMode) 0 else throw ex
            }
        }
    }
}