package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * The core interface for command arguments.
 */
interface ArgumentType<T> {
    @Throws(CommandSyntaxException::class)
    fun parse(reader: StringReader): T

    /**
     * Optional: Override this if the argument needs the command source to parse
     * (e.g., checking if a player name exists).
     */
    @Throws(CommandSyntaxException::class)
    fun <S> parse(reader: StringReader, source: S): T = parse(reader)

    /**
     * Returns suggestions to the user as they type.
     */
    fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = Suggestions.empty()

    /**
     * Used by the dispatcher to test for command ambiguities.
     */
    val examples: Collection<String>
        get() = emptyList()
}