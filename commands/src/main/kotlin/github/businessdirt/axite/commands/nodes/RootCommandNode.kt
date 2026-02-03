package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.builder.ArgumentBuilder
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

class RootCommandNode<S> : CommandNode<S>(
    command = null,
    requirement = { true },
    redirect = null,
    modifier = { listOf(it.source) },
    isFork = false
) {
    override val name: String = ""
    override val usageText: String = ""
    override val examples: Collection<String> = emptyList()
    override val sortedKey: String = ""

    override fun parse(reader: StringReader, contextBuilder: CommandContextBuilder<S>) {
        // Root nodes don't parse anything; they are the starting point.
    }

    override fun listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = Suggestions.empty()

    override fun isValidInput(input: String): Boolean = false

    override fun createBuilder(): ArgumentBuilder<S, *> {
        throw IllegalStateException("Cannot convert root into a builder")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RootCommandNode<*>) return false
        return super.equals(other)
    }

    override fun toString(): String = "<root>"
    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + usageText.hashCode()
        result = 31 * result + examples.hashCode()
        result = 31 * result + sortedKey.hashCode()
        return result
    }
}