package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.CommandDsl
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange

@CommandDsl
class CommandContextBuilder<S>(
    val dispatcher: CommandDispatcher<S>,
    var source: S,
    val rootNode: CommandNode<S>,
    start: Int
) {
    val arguments = mutableMapOf<String, ParsedArgument<S, *>>()
    val nodes = mutableListOf<ParsedCommandNode<S>>()

    var command: Command<S>? = null
    var child: CommandContextBuilder<S>? = null
    var range: StringRange = StringRange.at(start)
    var modifier: RedirectModifier<S>? = null
    var isForked: Boolean = false

    /**
     * DSL-style node addition.
     * Usage: addNode(node, range)
     */
    fun addNode(node: CommandNode<S>, range: StringRange) {
        nodes.add(ParsedCommandNode(node, range))
        this.range = StringRange.encompassing(this.range, range)
        this.modifier = node.modifier
        this.isForked = node.forks
    }

    fun command(command: Command<S>) {
        this.command = command
    }

    /**
     * DSL-style argument addition.
     * Usage: argument("name", value)
     */
    fun argument(name: String, argument: ParsedArgument<S, *>) {
        arguments[name] = argument
    }

    fun build(input: String): CommandContext<S> = CommandContext(
        source, input, arguments, command, rootNode, nodes, range,
        child?.build(input), modifier, isForked
    )

    fun copy(): CommandContextBuilder<S> = CommandContextBuilder(dispatcher, source, rootNode, range.start).apply {
        arguments.putAll(this@CommandContextBuilder.arguments)
        nodes.addAll(this@CommandContextBuilder.nodes)
        child = this@CommandContextBuilder.child
        range = this@CommandContextBuilder.range
        isForked = this@CommandContextBuilder.isForked
    }

    fun findSuggestionContext(cursor: Int): SuggestionContext<S> {
        if (range.end < cursor) {
            return child?.findSuggestionContext(cursor)
                ?: nodes.lastOrNull()?.let { SuggestionContext(it.node, it.range.end + 1) }
                ?: SuggestionContext(rootNode, range.start)
        }

        var result: SuggestionContext<S>? = null
        nodes.fold(rootNode) { prev, current ->
            if (result == null && cursor in current.range.start..current.range.end) {
                result = SuggestionContext(prev, current.range.start)
            }
            current.node
        }

        return result ?: SuggestionContext(nodes.lastOrNull()?.node ?: rootNode, range.start)
    }
}

fun <S> CommandContextBuilder<S>.childContext(
    start: Int,
    init: CommandContextBuilder<S>.() -> Unit
) {
    val childBuilder = CommandContextBuilder(dispatcher, source, rootNode, start)
    childBuilder.init()
    this.child = childBuilder
}

fun <S> CommandContextBuilder<S>.node(
    node: CommandNode<S>,
    start: Int,
    end: Int,
    action: (CommandContextBuilder<S>.() -> Unit)? = null
) {
    val range = StringRange(start, end)
    addNode(node, range)
    action?.invoke(this)
}

fun <S> String.buildContext(
    dispatcher: CommandDispatcher<S>,
    source: S,
    rootNode: CommandNode<S>,
    start: Int = 0,
    init: CommandContextBuilder<S>.() -> Unit
): CommandContext<S> = CommandContextBuilder(dispatcher, source, rootNode, start)
    .apply(init)
    .build(this)