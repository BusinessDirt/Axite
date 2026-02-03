package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.nodes.CommandNode

class SuggestionContext<S>(
    val parent: CommandNode<S>,
    val startPos: Int
)