package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.ArgumentType
import github.businessdirt.axite.commands.SuggestionProvider
import github.businessdirt.axite.commands.nodes.ArgumentCommandNode

class RequiredArgumentBuilder<S, T>(
    val name: String,
    val type: ArgumentType<T>
) : ArgumentBuilder<S, RequiredArgumentBuilder<S, T>>() {

    var suggestionsProvider: SuggestionProvider<S>? = null
        private set

    companion object {
        @JvmStatic
        fun <S, T> argument(name: String, type: ArgumentType<T>): RequiredArgumentBuilder<S, T> {
            return RequiredArgumentBuilder(name, type)
        }
    }

    override val `this`: RequiredArgumentBuilder<S, T>
        get() = this

    fun suggests(provider: SuggestionProvider<S>?): RequiredArgumentBuilder<S, T> {
        this.suggestionsProvider = provider
        return `this`
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

        for (argument in allArguments) result.addChild(argument)

        return result
    }
}