package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange

class CommandContextBuilder<S>(
    val dispatcher: CommandDispatcher<S>,
    var source: S,
    val rootNode: CommandNode<S>,
    start: Int
) {
    val arguments = mutableMapOf<String, ParsedArgument<S, *>>()
    val nodes = mutableListOf<ParsedCommandNode<S>>()

    // Public properties for direct assignment in DSL
    var command: Command<S>? = null
    var child: CommandContextBuilder<S>? = null
    var range: StringRange = StringRange.at(start)
    var modifier: RedirectModifier<S>? = null
    var isForked: Boolean = false

    // Computed property
    val lastChild: CommandContextBuilder<S>
        get() = generateSequence(this) { it.child }.last()

    /**
     * DSL-style node addition.
     * Usage: addNode(node, range)
     */
    fun addNode(node: CommandNode<S>, range: StringRange) {
        nodes.add(ParsedCommandNode(node, range))
        this.range = StringRange.encompassing(this.range, range)
        this.modifier = node.modifier
        this.isForked = node.isFork
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

    fun copy(): CommandContextBuilder<S> {
        return CommandContextBuilder(dispatcher, source, rootNode, range.start).apply {
            command = this@CommandContextBuilder.command
            arguments.putAll(this@CommandContextBuilder.arguments)
            nodes.addAll(this@CommandContextBuilder.nodes)
            child = this@CommandContextBuilder.child
            range = this@CommandContextBuilder.range
            isForked = this@CommandContextBuilder.isForked
        }
    }

    fun findSuggestionContext(cursor: Int): SuggestionContext<S> {
        check(range.start <= cursor) { "Can't find node before cursor" }

        if (range.end < cursor) {
            return when {
                child != null -> child!!.findSuggestionContext(cursor)
                nodes.isNotEmpty() -> {
                    val last = nodes.last()
                    SuggestionContext(last.node, last.range.end + 1)
                }
                else -> SuggestionContext(rootNode, range.start)
            }
        }

        var prev = rootNode
        for (node in nodes) {
            val nodeRange = node.range
            if (nodeRange.start <= cursor && cursor <= nodeRange.end) {
                return SuggestionContext(prev, nodeRange.start)
            }
            prev = node.node
        }

        return SuggestionContext(prev, range.start)
    }
}

fun <S> buildCommandContext(
    dispatcher: CommandDispatcher<S>,
    source: S,
    rootNode: CommandNode<S>,
    start: Int,
    input: String,
    init: CommandContextBuilder<S>.() -> Unit
): CommandContext<S> {
    val builder = CommandContextBuilder(dispatcher, source, rootNode, start)
    builder.init()
    return builder.build(input)
}

fun <S> CommandContextBuilder<S>.childContext(
    start: Int,
    init: CommandContextBuilder<S>.() -> Unit
) {
    val childBuilder = CommandContextBuilder(dispatcher, source, rootNode, start)
    childBuilder.init()
    this.child = childBuilder
}

/**
 * Automatically handles node registration and range calculations.
 */
fun <S> CommandContextBuilder<S>.node(
    node: CommandNode<S>,
    start: Int,
    end: Int,
    action: (CommandContextBuilder<S>.() -> Unit)? = null
) {
    val range = StringRange(start, end)
    addNode(node, range) // Uses the helper we created earlier
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