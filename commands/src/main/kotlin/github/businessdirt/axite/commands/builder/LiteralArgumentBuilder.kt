package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.nodes.LiteralCommandNode

/**
 * Builder for [LiteralCommandNode].
 *
 * @param S The type of the command source.
 * @property literal The literal string that identifies this node.
 */
class LiteralArgumentBuilder<S>(val literal: String) : ArgumentBuilder<S, LiteralArgumentBuilder<S>>() {

    override val self: LiteralArgumentBuilder<S> get() = this

    override fun build(): LiteralCommandNode<S> = LiteralCommandNode(
        literal = literal,
        command = command,
        requirement = requirement,
        redirect = redirect,
        modifier = modifier,
        forks = this@LiteralArgumentBuilder.forks
    ).apply { allArguments.forEach { addChild(it) } }
}

/**
 * Helper function to create a [LiteralCommandNode] using a builder block.
 */
fun <S> literal(name: String, block: ArgumentBlock<S, LiteralArgumentBuilder<S>> = {}): LiteralCommandNode<S> =
    LiteralArgumentBuilder<S>(name).apply(block).build()
