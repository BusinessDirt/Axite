package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringReader

interface ArgumentType<T> {

    @Throws(CommandSyntaxException::class)
    fun parse(reader: StringReader): T

    @Throws(CommandSyntaxException::class)
    fun <S> parse(reader: StringReader, source: S?): T = parse(reader)

    //fun <S> listSuggestions(
    //    context: CommandContext<S>,
    //    builder: SuggestionsBuilder
    //): CompletableFuture<Suggestions> = Suggestions.empty()

    fun getExamples(): Collection<String> = emptyList()
}