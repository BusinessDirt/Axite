package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.SingleRedirectModifier
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.nodes.RootCommandNode
import java.util.function.Predicate

abstract class ArgumentBuilder<S, T : ArgumentBuilder<S, T>> {
    private val arguments = RootCommandNode<S>()
    var command: Command<S>? = null
        private set
    var requirement: Predicate<S> = Predicate { true }
        private set
    var redirect: CommandNode<S>? = null
        private set
    var redirectModifier: RedirectModifier<S>? = null
        private set
    var isFork: Boolean = false
        private set

    protected abstract val `this`: T

    fun then(argument: ArgumentBuilder<S, *>): T {
        check(redirect == null) { "Cannot add children to a redirected node" }
        arguments.addChild(argument.build())
        return `this`
    }

    fun then(argument: CommandNode<S>): T {
        check(redirect == null) { "Cannot add children to a redirected node" }
        arguments.addChild(argument)
        return `this`
    }

    val allArguments: Collection<CommandNode<S>>
        get() = arguments.allChildren

    fun executes(command: Command<S>): T {
        this.command = command
        return `this`
    }

    fun requires(requirement: Predicate<S>): T {
        this.requirement = requirement
        return `this`
    }

    fun redirect(target: CommandNode<S>?): T = forward(target, null, false)

    fun redirect(target: CommandNode<S>?, modifier: SingleRedirectModifier<S>?): T {
        return forward(target, modifier?.let { m -> RedirectModifier { listOf(m.apply(it)) } }, false)
    }

    fun fork(target: CommandNode<S>?, modifier: RedirectModifier<S>?): T = forward(target, modifier, true)

    fun forward(target: CommandNode<S>?, modifier: RedirectModifier<S>?, fork: Boolean): T {
        check(arguments.allChildren.isEmpty()) { "Cannot forward a node with children" }
        this.redirect = target
        this.redirectModifier = modifier
        this.isFork = fork
        return `this`
    }

    abstract fun build(): CommandNode<S>
}