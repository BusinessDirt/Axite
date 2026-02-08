package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.SuggestionProvider
import github.businessdirt.axite.commands.arguments.ArgumentType
import github.businessdirt.axite.commands.nodes.ArgumentCommandNode
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import github.businessdirt.axite.commands.suggestions.suggestions

/**
 * Builder for [ArgumentCommandNode].
 *
 * @param S The type of the command source.
 * @param T The type of the argument.
 * @property name The name of the argument.
 * @property type The type of the argument.
 */
class RequiredArgumentBuilder<S, T>(
    val name: String,
    val type: ArgumentType<T>
) : ArgumentBuilder<S, RequiredArgumentBuilder<S, T>>() {

    var suggestionsProvider: SuggestionProvider<S>? = null
        private set

    override val self: RequiredArgumentBuilder<S, T> get() = this

    /**
     * Sets a custom suggestion provider for this argument.
     *
     * @param provider The suggestion provider.
     * @return This builder.
     */
    fun suggests(provider: SuggestionProvider<S>?): RequiredArgumentBuilder<S, T> =
        self.apply { this.suggestionsProvider = provider }

    /**
     * Sets a custom suggestion provider for this argument using a DSL block.
     *
     * @param block The suggestion logic.
     * @return This builder.
     */
    fun suggests(block: SuggestionsBuilder.() -> Unit): RequiredArgumentBuilder<S, T> = self.apply {
        this.suggestionsProvider = SuggestionProvider { _, builder ->
            suggestions(builder.input, builder.start, block)
        }
    }

    override fun build(): ArgumentCommandNode<S, T> = ArgumentCommandNode(
        name = name,
        type = type,
        command = command,
        requirement = requirement,
        redirect = redirect,
        modifier = modifier,
        forks = this@RequiredArgumentBuilder.forks,
        customSuggestions = suggestionsProvider
    ).apply { allArguments.forEach { addChild(it) } }
}

/**
 * Helper function to create an [ArgumentCommandNode] using a builder block.
 */
fun <S, T> argument(
    name: String,
    type: ArgumentType<T>,
    block: ArgumentBlock<S, RequiredArgumentBuilder<S, T>> = {}
): ArgumentCommandNode<S, T> = RequiredArgumentBuilder<S, T>(name, type).apply(block).build()
