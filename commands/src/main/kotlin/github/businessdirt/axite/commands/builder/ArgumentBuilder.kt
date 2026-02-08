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

/**
 * Base class for command node builders.
 *
 * @param S The type of the command source.
 * @param T The concrete type of the builder (for method chaining).
 */
@CommandDsl
abstract class ArgumentBuilder<S, T : ArgumentBuilder<S, T>> {

    private val arguments = RootCommandNode<S>()

    /**
     * Returns all child nodes added to this builder.
     */
    val allArguments: Collection<CommandNode<S>>
        get() = arguments.allChildren

    protected var command: Command<S>? = null
    protected var requirement: Predicate<S> = Predicate { true }
    protected var redirect: CommandNode<S>? = null
    protected var modifier: RedirectModifier<S>? = null
    protected var forks: Boolean = false

    protected abstract val self: T

    /**
     * Adds a literal child node.
     *
     * @param name The name of the literal.
     * @param block Configuration block for the child builder.
     * @return This builder.
     */
    fun literal(name: String, block: ArgumentBlock<S, LiteralArgumentBuilder<S>> = {}): T {
        check(redirect == null) { "Cannot add children to a redirected node" }
        val child = LiteralArgumentBuilder<S>(name).apply(block)
        arguments.addChild(child.build())
        return self
    }

    /**
     * Adds an argument child node.
     *
     * @param name The name of the argument.
     * @param type The type of the argument.
     * @param block Configuration block for the child builder.
     * @return This builder.
     */
    fun <V> argument(name: String, type: ArgumentType<V>, block: ArgumentBlock<S, RequiredArgumentBuilder<S, V>> = {}): T {
        val child = RequiredArgumentBuilder<S, V>(name, type).apply(block)
        arguments.addChild(child.build())
        return self
    }

    /**
     * Sets the command to execute when this node is reached.
     *
     * @param cmd The command to execute.
     * @return This builder.
     */
    fun executes(cmd: Command<S>): T {
        this.command = cmd
        return self
    }

    /**
     * Sets the requirement for this node.
     *
     * @param predicate The predicate that must return true for the source to use this node.
     * @return This builder.
     */
    fun requires(predicate: Predicate<S>): T {
        this.requirement = predicate
        return self
    }

    /**
     * Sets the requirement for this node using a lambda.
     *
     * @param predicate The function that must return true for the source to use this node.
     * @return This builder.
     */
    fun requires(predicate: (S) -> Boolean): T {
        this.requirement = Predicate { predicate(it) }
        return self
    }

    /**
     * Redirects execution to the target node.
     *
     * @param target The node to redirect to.
     * @param modifier Optional modifier to transform the source before redirecting.
     * @return This builder.
     */
    fun redirect(target: CommandNode<S>?, modifier: SingleRedirectModifier<S>? = null): T = forward(
        target,
        if (modifier == null) null else RedirectModifier { o -> listOf(modifier.apply(o)) },
        false
    )

    /**
     * Forks execution to the target node, splitting the source into multiple sources.
     *
     * @param target The node to fork to.
     * @param modifier The modifier to generate the new sources.
     * @return This builder.
     */
    fun fork(target: CommandNode<S>?, modifier: RedirectModifier<S>?): T =
        forward(target, modifier, true)

    /**
     * Forwards execution to the target node.
     *
     * @param target The node to forward to.
     * @param modifier The modifier to apply to the source.
     * @param fork Whether to fork the execution.
     * @return This builder.
     */
    fun forward(target: CommandNode<S>?, modifier: RedirectModifier<S>?, fork: Boolean): T {
        check(arguments.allChildren.isEmpty()) { "Cannot forward a node with children" }
        this.redirect = target
        this.modifier = modifier
        this.forks = fork
        return self
    }

    /**
     * Builds the command node.
     *
     * @return The built command node.
     */
    abstract fun build(): CommandNode<S>
}
