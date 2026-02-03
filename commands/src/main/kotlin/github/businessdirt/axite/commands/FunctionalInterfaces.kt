package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

interface Command<S> {
    fun run(command: CommandContext<S>): Int
}

/**
 * Transforms one source into many. Used for "forking" commands
 * (e.g., executing a command for every player in a list).
 */
fun interface RedirectModifier<S> {
    @Throws(CommandSyntaxException::class)
    fun apply(context: CommandContext<S>): Collection<S>
}

/**
 * A callback triggered when a command pipeline finishes.
 */
fun interface ResultConsumer<S> {
    fun onCommandComplete(context: CommandContext<S>, success: Boolean, result: Int)
}

/**
 * Transforms one source into exactly one other source.
 * Used for "anchoring" or simple source transformations (e.g., changing position).
 */
fun interface SingleRedirectModifier<S> {
    @Throws(CommandSyntaxException::class)
    fun apply(context: CommandContext<S>): S
}

fun interface SuggestionProvider<S> {
    @Throws(CommandSyntaxException::class)
    fun getSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions>
}

fun interface AmbiguityConsumer<S> {
    fun ambiguous(
        parent: CommandNode<S>,
        child: CommandNode<S>,
        sibling: CommandNode<S>,
        inputs: Collection<String>
    )
}