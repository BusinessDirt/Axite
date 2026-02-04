package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.SuggestionProvider
import github.businessdirt.axite.commands.arguments.ArgumentType
import github.businessdirt.axite.commands.nodes.ArgumentCommandNode
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import github.businessdirt.axite.commands.suggestions.suggestionsFuture

class RequiredArgumentBuilder<S, T>(
    val name: String,
    val type: ArgumentType<T>
) : ArgumentBuilder<S, RequiredArgumentBuilder<S, T>>() {

    var suggestionsProvider: SuggestionProvider<S>? = null
        private set

    override val self: RequiredArgumentBuilder<S, T> get() = this

    fun suggests(provider: SuggestionProvider<S>?): RequiredArgumentBuilder<S, T> {
        this.suggestionsProvider = provider
        return self
    }

    fun suggests(block: SuggestionsBuilder.() -> Unit): RequiredArgumentBuilder<S, T> {
        this.suggestionsProvider = SuggestionProvider { _, builder ->
            suggestionsFuture(builder.input, builder.start, block)
        }
        return self
    }

    override fun build(): ArgumentCommandNode<S, T> {
        val result = ArgumentCommandNode(
            name = name,
            type = type,
            command = command,
            requirement = requirement,
            redirect = redirect,
            modifier = redirectModifier,
            forks = isFork,
            customSuggestions = suggestionsProvider
        )

        allArguments.forEach { result.addChild(it) }

        return result
    }
}

fun <S, T> argument(
    name: String,
    type: ArgumentType<T>,
    block: ArgumentBlock<S, RequiredArgumentBuilder<S, T>> = {}
): ArgumentCommandNode<S, T> = RequiredArgumentBuilder<S, T>(name, type).apply(block).build()