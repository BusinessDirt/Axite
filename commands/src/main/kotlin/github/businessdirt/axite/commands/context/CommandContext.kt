package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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

    val lastChild: CommandContext<S>
        get() {
            var result = this
            while (result.child != null) result = result.child
            return result
        }

    fun copyFor(source: S): CommandContext<S> = when (this.source) {
        source -> this
        else -> CommandContext(source, input, arguments, command, rootNode, nodes, range, child, modifier, isForked)
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