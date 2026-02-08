package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.builder.ArgumentBuilder
import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder

/**
 * The root node of the command tree.
 * Does not match any input itself but serves as the entry point.
 *
 * @param S The type of the command source.
 */
@Suppress("EqualsOrHashCode")
class RootCommandNode<S> : CommandNode<S>(
    command = null,
    requirement = { true },
    redirect = null,
    modifier = { listOf(it.source) },
    forks = false
) {
    override val name: String = ""
    override val usageText: String = ""
    override val examples: Collection<String> = emptyList()
    override val sortedKey: String = ""

    override fun parse(reader: StringReader, contextBuilder: CommandContextBuilder<S>) {
        // Root nodes don't parse anything; they are the starting point.
    }

    override suspend fun listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): Suggestions = Suggestions.empty()

    override fun isValidInput(input: String): Boolean = false

    override fun createBuilder(): ArgumentBuilder<S, *> =
        throw IllegalStateException("Cannot convert root into a builder")

    override fun toString(): String = "<root>"
    override fun equals(other: Any?): Boolean =
        this === other || (other is RootCommandNode<*> && super.equals(other))
}
