package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange

data class ParsedArgument<S, T>(
    val range: StringRange,
    val result: T
) {
    constructor(start: Int, end: Int, result: T) : this(StringRange.between(start, end), result)
}

data class ParsedCommandNode<S>(
    val node: CommandNode<S>,
    val range: StringRange
)