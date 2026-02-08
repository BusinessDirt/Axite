package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange

/**
 * Holds data about a parsed argument.
 *
 * @param S The type of the command source.
 * @param T The type of the argument value.
 * @property range The range in the input string covering this argument.
 * @property result The parsed value.
 */
data class ParsedArgument<S, T>(
    val range: StringRange,
    val result: T
) {
    constructor(start: Int, end: Int, result: T) : this(StringRange.between(start, end), result)
}

/**
 * Holds data about a parsed command node.
 *
 * @param S The type of the command source.
 * @property node The command node.
 * @property range The range in the input string covering this node.
 */
data class ParsedCommandNode<S>(
    val node: CommandNode<S>,
    val range: StringRange
)

/**
 * Context used for calculating suggestions.
 *
 * @param S The type of the command source.
 * @property parent The parent node from which suggestions are requested.
 * @property startPos The start position in the input string for suggestions.
 */
class SuggestionContext<S>(
    val parent: CommandNode<S>,
    val startPos: Int
)
