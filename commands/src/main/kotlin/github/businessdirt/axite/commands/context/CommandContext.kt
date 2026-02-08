package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange

/**
 * Represents the context of a command execution.
 * Contains information about the source, the input, parsed arguments, and the command node tree path.
 *
 * @param S The type of the command source.
 * @property source The command source.
 * @property input The full input string.
 * @property arguments A map of parsed arguments.
 * @property command The command to execute, if any.
 * @property rootNode The root node of the command tree.
 * @property nodes The list of parsed nodes leading to this context.
 * @property range The range of input covered by this context.
 * @property child The child context, if any (e.g. from a redirect).
 * @property modifier The redirect modifier, if any.
 * @property isForked Whether this context represents a forked execution path.
 */
data class CommandContext<S>(
    val source: S,
    val input: String,
    val arguments: Map<String, ParsedArgument<S, *>>,
    val command: Command<S>?,
    val rootNode: CommandNode<S>,
    val nodes: List<ParsedCommandNode<S>>,
    val range: StringRange,
    val child: CommandContext<S>?,
    val modifier: RedirectModifier<S>?,
    val isForked: Boolean
) {

    /**
     * Creates a copy of this context with a new source.
     *
     * @param newSource The new command source.
     * @return A new [CommandContext] with the updated source.
     */
    fun copyFor(newSource: S): CommandContext<S> = when {
        this.source === newSource -> this
        else -> this.copy(source = newSource)
    }

    /**
     * Retrieves a parsed argument by name.
     *
     * @param name The name of the argument.
     * @return The argument value.
     * @throws IllegalArgumentException If the argument does not exist or is of the wrong type.
     */
    inline fun <reified V : Any> getArgument(name: String): V {
        val argument = arguments[name] ?:
            throw IllegalArgumentException("No such argument '$name' exists on this command")

        val result = argument.result

        return result as? V
            ?: throw IllegalArgumentException(
                "Argument '$name' is defined as ${result?.let { it::class.simpleName } ?: "null"}, not ${V::class.simpleName}"
            )
    }
}
