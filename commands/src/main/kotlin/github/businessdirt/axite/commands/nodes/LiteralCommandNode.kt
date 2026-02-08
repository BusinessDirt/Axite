package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.builder.LiteralArgumentBuilder
import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.error
import github.businessdirt.axite.commands.strings.StringRange
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.*
import java.util.function.Predicate

class LiteralCommandNode<S>(
    val literal: String,
    command: Command<S>?,
    requirement: Predicate<S>,
    redirect: CommandNode<S>?,
    modifier: RedirectModifier<S>?,
    forks: Boolean
) : CommandNode<S>(command, requirement, redirect, modifier, forks) {

    private val literalLowerCase = literal.lowercase(Locale.ROOT)

    override val name: String get() = literal
    override val usageText: String get() = literal
    override val examples: Collection<String> = listOf(literal)
    override val sortedKey: String get() = literal

    override fun parse(reader: StringReader, contextBuilder: CommandContextBuilder<S>) {
        val start = reader.cursor
        val end = parseLiteral(reader)

        if (end > -1) contextBuilder.addNode(this, StringRange.between(start, end)) else
            throw reader.error(CommandError.InvalidLiteral(literal))
    }

    private fun parseLiteral(reader: StringReader): Int {
        val start = reader.cursor

        if (reader.canRead(literal.length) && reader.string.startsWith(literal, startIndex = start)) {
            val end = start + literal.length
            reader.cursor = end

            // Check if we are at the end of input or followed by a separator
            if (!reader.canRead() || reader.peek() == ' ') return end

            // Reset cursor if the boundary check fails (e.g., input is "commandX" but we wanted "command")
            reader.cursor = start
        }

        return -1
    }

    override suspend fun listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): Suggestions = when {
        literalLowerCase.startsWith(builder.remainingLowerCase) -> {
            builder.suggest(literal)
            builder.build()
        }
        else -> Suggestions.empty()
    }

    override fun isValidInput(input: String): Boolean = parseLiteral(StringReader(input)) > -1

    override fun createBuilder(): LiteralArgumentBuilder<S> = LiteralArgumentBuilder<S>(literal).apply {
        requires(requirement)
        forward(redirect, modifier, forks)
        command?.let { executes(it) }
    }

    override fun toString(): String = "<literal $literal>"
    override fun hashCode(): Int = 31 * literal.hashCode() + super.hashCode()
    override fun equals(other: Any?): Boolean =
        this === other || (other is LiteralCommandNode<*> && literal == other.literal && super.equals(other))

}