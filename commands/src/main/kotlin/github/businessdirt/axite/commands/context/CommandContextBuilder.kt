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
    var command: Command<S>? = null
    var child: CommandContextBuilder<S>? = null
    var range: StringRange = StringRange.at(start)
    var modifier: RedirectModifier<S>? = null
    var isForked: Boolean = false

    val lastChild: CommandContextBuilder<S>
        get() {
            var result = this
            while (result.child != null) {
                result = result.child!!
            }
            return result
        }

    fun withSource(source: S): CommandContextBuilder<S> {
        this.source = source
        return this
    }

    fun withArgument(name: String, argument: ParsedArgument<S, *>): CommandContextBuilder<S> {
        arguments[name] = argument
        return this
    }

    fun withCommand(command: Command<S>?): CommandContextBuilder<S> {
        this.command = command
        return this
    }

    fun withNode(node: CommandNode<S>, range: StringRange): CommandContextBuilder<S> {
        nodes.add(ParsedCommandNode(node, range))
        this.range = StringRange.encompassing(this.range, range)
        this.modifier = node.modifier
        this.isForked = node.isFork
        return this
    }

    fun withChild(child: CommandContextBuilder<S>): CommandContextBuilder<S> {
        this.child = child
        return this
    }

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

    fun build(input: String): CommandContext<S> {
        return CommandContext(
            source, input, arguments, command, rootNode, nodes, range,
            child?.build(input), modifier, isForked
        )
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