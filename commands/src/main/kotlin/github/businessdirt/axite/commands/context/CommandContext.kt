package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange

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

    fun copyFor(newSource: S): CommandContext<S> = when {
        this.source === newSource -> this
        else -> this.copy(source = newSource)
    }

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