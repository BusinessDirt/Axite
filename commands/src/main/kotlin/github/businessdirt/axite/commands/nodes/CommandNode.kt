package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.AmbiguityConsumer
import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.builder.ArgumentBuilder
import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate

abstract class CommandNode<S>(
    var command: Command<S>? = null,
    val requirement: Predicate<S>,
    val redirect: CommandNode<S>? = null,
    val modifier: RedirectModifier<S>? = null,
    val forks: Boolean = false
) : Comparable<CommandNode<S>> {

    private val children = mutableMapOf<String, CommandNode<S>>()
    private val literals = mutableMapOf<String, LiteralCommandNode<S>>()
    private val arguments = mutableMapOf<String, ArgumentCommandNode<S, *>>()

    val allChildren: Collection<CommandNode<S>> get() = children.values

    // Operator overload allows syntax: node["childName"]
    operator fun get(name: String): CommandNode<S>? = children[name]

    // Backwards compatibility alias
    fun getChild(name: String): CommandNode<S>? = this[name]

    fun canUse(source: S): Boolean = requirement.test(source)

    open fun addChild(node: CommandNode<S>) {
        require(node !is RootCommandNode) {
            "Cannot add a RootCommandNode as a child to any other CommandNode"
        }

        children[node.name]?.let { child ->
            // Merge logic
            node.command?.let { child.command = it }
            node.allChildren.forEach(child::addChild)
        } ?: run {
            // New child logic
            children[node.name] = node
            when (node) {
                is LiteralCommandNode -> literals[node.name] = node
                is ArgumentCommandNode<*, *> -> arguments[node.name] = node as ArgumentCommandNode<S, *>
            }
        }
    }

    fun findAmbiguities(consumer: AmbiguityConsumer<S>) {
        val childList = children.values.toList() // Avoid concurrent modification if consumer alters state

        for (child in childList) {
            childList.asSequence().filter { it !== child }
                .forEach { sibling ->
                    val matches = child.examples.filter(sibling::isValidInput).toSet()
                    if (matches.isNotEmpty()) {
                        consumer.ambiguous(this, child, sibling, matches)
                    }
                }
            child.findAmbiguities(consumer)
        }
    }

    fun getRelevantNodes(input: StringReader): Collection<CommandNode<S>> {
        if (literals.isEmpty()) return arguments.values

        val cursor = input.cursor
        while (input.canRead() && input.peek() != ' ') input.skip()

        val text = input.string.substring(cursor, input.cursor)
        input.cursor = cursor // Reset cursor for actual parsing later

        return literals[text]?.let { listOf(it) } ?: arguments.values
    }

    // Simplified comparison logic
    override fun compareTo(other: CommandNode<S>): Int = when {
        (this is LiteralCommandNode) == (other is LiteralCommandNode) -> sortedKey.compareTo(other.sortedKey)
        other is LiteralCommandNode -> 1
        else -> -1
    }

    override fun hashCode(): Int = 31 * children.hashCode() + (command?.hashCode() ?: 0)
    override fun equals(other: Any?): Boolean =
        this === other || (other is CommandNode<*> && children == other.children && command == other.command)

    abstract val name: String
    abstract val usageText: String
    abstract val examples: Collection<String>
    protected abstract val sortedKey: String

    protected abstract fun isValidInput(input: String): Boolean

    @Throws(CommandSyntaxException::class)
    abstract fun parse(reader: StringReader, contextBuilder: CommandContextBuilder<S>)

    abstract fun listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions>
    abstract fun createBuilder(): ArgumentBuilder<S, *>
}