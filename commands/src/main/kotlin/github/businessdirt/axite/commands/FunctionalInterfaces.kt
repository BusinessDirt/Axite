package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder

/**
 * Represents a command execution logic.
 *
 * @param S The type of the command source.
 */
fun interface Command<S> {
    /**
     * Executes the command.
     *
     * @param command The context containing information about the command execution.
     * @return An integer result (often used to indicate success/failure or a count).
     */
    fun run(command: CommandContext<S>): Int
}

/**
 * Transforms one source into many. Used for "forking" commands
 * (e.g., executing a command for every player in a list).
 *
 * @param S The type of the command source.
 */
fun interface RedirectModifier<S> {
    /**
     * Applies the modification.
     *
     * @param context The current command context.
     * @return A collection of new sources to execute the command for.
     * @throws CommandSyntaxException If the transformation fails.
     */
    @Throws(CommandSyntaxException::class)
    fun apply(context: CommandContext<S>): Collection<S>
}

/**
 * A callback triggered when a command pipeline finishes.
 *
 * @param S The type of the command source.
 */
fun interface ResultConsumer<S> {
    /**
     * Called when a command completes.
     *
     * @param context The command context.
     * @param success Whether the command executed successfully.
     * @param result The result value returned by the command.
     */
    fun onCommandComplete(context: CommandContext<S>, success: Boolean, result: Int)
}

/**
 * Transforms one source into exactly one other source.
 * Used for "anchoring" or simple source transformations (e.g., changing position).
 *
 * @param S The type of the command source.
 */
fun interface SingleRedirectModifier<S> {
    /**
     * Applies the modification.
     *
     * @param context The current command context.
     * @return The new source to execute the command for.
     * @throws CommandSyntaxException If the transformation fails.
     */
    @Throws(CommandSyntaxException::class)
    fun apply(context: CommandContext<S>): S
}

/**
 * Provides tab completion suggestions.
 *
 * @param S The type of the command source.
 */
fun interface SuggestionProvider<S> {
    /**
     * Computes suggestions.
     *
     * @param context The command context.
     * @param builder The suggestions builder.
     * @return A [Suggestions] object containing the computed suggestions.
     * @throws CommandSyntaxException If suggestion generation fails.
     */
    @Throws(CommandSyntaxException::class)
    suspend fun getSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): Suggestions
}

/**
 * Consumer for reporting ambiguity in command nodes.
 *
 * @param S The type of the command source.
 */
fun interface AmbiguityConsumer<S> {
    /**
     * Called when ambiguity is detected.
     *
     * @param parent The parent node.
     * @param child The first child node involved in the ambiguity.
     * @param sibling The second child node involved in the ambiguity.
     * @param inputs The input strings that trigger the ambiguity.
     */
    fun ambiguous(
        parent: CommandNode<S>,
        child: CommandNode<S>,
        sibling: CommandNode<S>,
        inputs: Collection<String>
    )
}
