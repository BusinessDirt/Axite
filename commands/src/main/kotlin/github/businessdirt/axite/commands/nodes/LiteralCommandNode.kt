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
import java.util.concurrent.CompletableFuture
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
        val end = parse(reader)
        if (end > -1) {
            contextBuilder.addNode(this, StringRange.between(start, end))
            return
        }

        throw reader.error(CommandError.InvalidLiteral(literal))
    }

    private fun parse(reader: StringReader): Int {
        val start = reader.cursor
        if (reader.canRead(literal.length)) {
            val end = start + literal.length
            if (reader.string.substring(start, end) == literal) {
                reader.cursor = end
                if (!reader.canRead() || reader.peek() == ' ') {
                    return end
                } else {
                    reader.cursor = start
                }
            }
        }
        return -1
    }

    override fun listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = when {
        literalLowerCase.startsWith(builder.remainingLowerCase) -> {
            builder.suggest(literal)
            builder.buildFuture()
        }
        else -> Suggestions.empty()
    }

    override fun isValidInput(input: String): Boolean = parse(StringReader(input)) > -1

    override fun createBuilder(): LiteralArgumentBuilder<S> {
        val builder = LiteralArgumentBuilder<S>(literal)
        builder.requires(requirement)
        builder.forward(redirect, modifier, isFork)
        command?.let { builder.executes(it) }
        return builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiteralCommandNode<*>) return false
        return literal == other.literal && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = literal.hashCode()
        result = 31 * result + super.hashCode()
        return result
    }

    override fun toString(): String = "<literal $literal>"
}