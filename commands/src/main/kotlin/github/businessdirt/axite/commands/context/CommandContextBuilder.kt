package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.CommandDsl
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange

/**
 * Builds a [CommandContext] during the parsing phase.
 *
 * @param S The type of the command source.
 * @property dispatcher The command dispatcher.
 * @property source The command source.
 * @property rootNode The root node.
 * @param start The starting cursor position.
 */
@CommandDsl
class CommandContextBuilder<S>(
    val dispatcher: CommandDispatcher<S>,
    var source: S,
    val rootNode: CommandNode<S>,
    start: Int
) {
    /** Map of parsed arguments. */
    val arguments = mutableMapOf<String, ParsedArgument<S, *>>()
    /** List of parsed nodes. */
    val nodes = mutableListOf<ParsedCommandNode<S>>()

    /** The command to execute. */
    var command: Command<S>? = null
    /** Child context builder. */
    var child: CommandContextBuilder<S>? = null
    /** Range covered by this context. */
    var range: StringRange = StringRange.at(start)
    /** Redirect modifier. */
    var modifier: RedirectModifier<S>? = null
    /** Whether execution is forked. */
    var isForked: Boolean = false

    /**
     * Adds a node to the context.
     *
     * @param node The command node.
     * @param range The range of input matching this node.
     */
    fun addNode(node: CommandNode<S>, range: StringRange) {
        nodes.add(ParsedCommandNode(node, range))
        this.range = StringRange.encompassing(this.range, range)
        this.modifier = node.modifier
        this.isForked = node.forks
    }

    /**
     * Sets the command to execute.
     */
    fun command(command: Command<S>) {
        this.command = command
    }

    /**
     * Adds a parsed argument to the context.
     *
     * @param name The argument name.
     * @param argument The parsed argument data.
     */
    fun argument(name: String, argument: ParsedArgument<S, *>) {
        arguments[name] = argument
    }

    /**
     * Builds the final [CommandContext].
     *
     * @param input The full input string.
     * @return The immutable command context.
     */
    fun build(input: String): CommandContext<S> = CommandContext(
        source, input, arguments, command, rootNode, nodes, range,
        child?.build(input), modifier, isForked
    )

    /**
     * Creates a shallow copy of this builder.
     */
    fun copy(): CommandContextBuilder<S> = CommandContextBuilder(dispatcher, source, rootNode, range.start).apply {
        arguments.putAll(this@CommandContextBuilder.arguments)
        nodes.addAll(this@CommandContextBuilder.nodes)
        child = this@CommandContextBuilder.child
        range = this@CommandContextBuilder.range
        isForked = this@CommandContextBuilder.isForked
    }

    /**
     * Finds the [SuggestionContext] for a given cursor position.
     *
     * @param cursor The cursor position.
     * @return The suggestion context containing the parent node and start position.
     */
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

/**
 * DSL helper to create a child context.
 */
fun <S> CommandContextBuilder<S>.childContext(
    start: Int,
    init: CommandContextBuilder<S>.() -> Unit
) {
    val childBuilder = CommandContextBuilder(dispatcher, source, rootNode, start)
    childBuilder.init()
    this.child = childBuilder
}

/**
 * DSL helper to add a node with a block.
 */
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

/**
 * DSL entry point for building a context.
 */
fun <S> String.buildContext(
    dispatcher: CommandDispatcher<S>,
    source: S,
    rootNode: CommandNode<S>,
    start: Int = 0,
    init: CommandContextBuilder<S>.() -> Unit
): CommandContext<S> = CommandContextBuilder(dispatcher, source, rootNode, start)
    .apply(init)
    .build(this)
