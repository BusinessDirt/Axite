package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.CommandDsl
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.SingleRedirectModifier
import github.businessdirt.axite.commands.arguments.ArgumentType
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.nodes.RootCommandNode
import java.util.function.Predicate

typealias ArgumentBlock<S, T> = T.() -> Unit

@CommandDsl
abstract class ArgumentBuilder<S, T : ArgumentBuilder<S, T>> {

    private val arguments = RootCommandNode<S>()

    val allArguments: Collection<CommandNode<S>>
        get() = arguments.allChildren

    protected var command: Command<S>? = null
    protected var requirement: Predicate<S> = Predicate { true }
    protected var redirect: CommandNode<S>? = null
    protected var redirectModifier: RedirectModifier<S>? = null
    protected var isFork: Boolean = false

    protected abstract val self: T

    fun literal(name: String, block: ArgumentBlock<S, LiteralArgumentBuilder<S>> = {}): T {
        val child = LiteralArgumentBuilder<S>(name).apply(block)
        arguments.addChild(child.build())
        return self
    }

    fun <V> argument(name: String, type: ArgumentType<V>, block: ArgumentBlock<S, RequiredArgumentBuilder<S, V>> = {}): T {
        val child = RequiredArgumentBuilder<S, V>(name, type).apply(block)
        arguments.addChild(child.build())
        return self
    }

    fun executes(cmd: Command<S>): T {
        this.command = cmd
        return self
    }

    fun requires(predicate: Predicate<S>): T {
        this.requirement = predicate
        return self
    }

    fun requires(predicate: (S) -> Boolean): T {
        this.requirement = Predicate { predicate(it) }
        return self
    }

    fun redirect(target: CommandNode<S>?, modifier: SingleRedirectModifier<S>? = null): T {
        val mod = modifier?.let { m -> RedirectModifier { listOf(m.apply(it)) } }
        return forward(target, mod, false)
    }

    fun fork(target: CommandNode<S>?, modifier: RedirectModifier<S>?): T =
        forward(target, modifier, true)

    fun forward(target: CommandNode<S>?, modifier: RedirectModifier<S>?, fork: Boolean): T {
        check(arguments.allChildren.isEmpty()) { "Cannot forward a node with children" }
        this.redirect = target
        this.redirectModifier = modifier
        this.isFork = fork
        return self
    }

    abstract fun build(): CommandNode<S>
}