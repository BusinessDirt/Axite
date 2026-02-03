package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * Parses "true" or "false".
 */
data object BooleanArgumentType : ArgumentType<Boolean> {
    override fun parse(reader: StringReader): Boolean = reader.readBoolean()

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        if ("true".startsWith(builder.remainingLowerCase)) builder.suggest("true")
        if ("false".startsWith(builder.remainingLowerCase)) builder.suggest("false")
        return builder.buildFuture()
    }

    override val examples: Collection<String> = listOf("true", "false")
}