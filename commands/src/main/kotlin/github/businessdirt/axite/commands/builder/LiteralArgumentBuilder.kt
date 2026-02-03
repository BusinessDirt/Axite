package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.nodes.LiteralCommandNode

class LiteralArgumentBuilder<S>(val literal: String) : ArgumentBuilder<S, LiteralArgumentBuilder<S>>() {

    companion object {
        @JvmStatic
        fun <S> literal(name: String): LiteralArgumentBuilder<S> = LiteralArgumentBuilder(name)
    }

    override val `this`: LiteralArgumentBuilder<S>
        get() = this

    override fun build(): LiteralCommandNode<S> {
        val result = LiteralCommandNode(
            literal = literal,
            command = command,
            requirement = requirement,
            redirect = redirect,
            modifier = redirectModifier,
            forks = isFork
        )

        for (argument in allArguments) result.addChild(argument)

        return result
    }
}