package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder

/**
 * The core interface for command arguments.
 *
 * Defines how an argument is parsed from a string reader and how it provides suggestions.
 *
 * @param T The type of the parsed argument.
 */
interface ArgumentType<T> {
    /**
     * Parses the argument from the given string reader.
     *
     * @param reader The string reader to read from.
     * @return The parsed argument value.
     * @throws CommandSyntaxException If parsing fails.
     */
    @Throws(CommandSyntaxException::class)
    fun parse(reader: StringReader): T

    /**
     * Parses the argument from the given string reader, potentially using the source for context.
     *
     * @param reader The string reader to read from.
     * @param source The command source.
     * @return The parsed argument value.
     * @throws CommandSyntaxException If parsing fails.
     */
    @Throws(CommandSyntaxException::class)
    fun <S> parse(reader: StringReader, source: S): T = parse(reader)

    /**
     * Provides tab completion suggestions for this argument.
     *
     * @param context The command context.
     * @param builder The suggestions builder.
     * @return A [Suggestions] object containing the suggestions.
     */
    suspend fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): Suggestions = Suggestions.empty()

    /**
     * Returns a collection of examples for this argument type.
     * These are used to detect ambiguities.
     */
    val examples: Collection<String>
        get() = emptyList()
}
