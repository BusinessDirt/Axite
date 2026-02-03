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
    var command: Command<S>?,
    val requirement: Predicate<S>,
    val redirect: CommandNode<S>?,
    val modifier: RedirectModifier<S>?,
    val isFork: Boolean
) : Comparable<CommandNode<S>> {

    private val children = mutableMapOf<String, CommandNode<S>>()
    private val literals = mutableMapOf<String, LiteralCommandNode<S>>()
    private val arguments = mutableMapOf<String, ArgumentCommandNode<S, *>>()

    val allChildren: Collection<CommandNode<S>> get() = children.values

    fun getChild(name: String): CommandNode<S>? = children[name]

    fun canUse(source: S): Boolean = requirement.test(source)

    open fun addChild(node: CommandNode<S>) {
        if (node is RootCommandNode) {
            throw UnsupportedOperationException("Cannot add a RootCommandNode as a child to any other CommandNode")
        }

        val child = children[node.name]
        if (child != null) {
            // Merge logic
            node.command?.let { child.command = it }
            node.allChildren.forEach { child.addChild(it) }
        } else {
            children[node.name] = node
            when (node) {
                is LiteralCommandNode -> literals[node.name] = node
                is ArgumentCommandNode<S, *> -> arguments[node.name] = node
            }
        }
    }

    fun findAmbiguities(consumer: AmbiguityConsumer<S>) {
        children.values.forEach { child ->
            children.values.filter { it != child }.forEach { sibling ->
                val matches = child.examples.filter { sibling.isValidInput(it) }.toSet()
                if (matches.isNotEmpty()) {
                    consumer.ambiguous(this, child, sibling, matches)
                }
            }
            child.findAmbiguities(consumer)
        }
    }

    fun getRelevantNodes(input: StringReader): Collection<CommandNode<S>> {
        if (literals.isNotEmpty()) {
            val cursor = input.cursor
            while (input.canRead() && input.peek() != ' ') {
                input.skip()
            }
            val text = input.string.substring(cursor, input.cursor)
            input.cursor = cursor

            val literal = literals[text]
            return if (literal != null) listOf(literal) else arguments.values
        }
        return arguments.values
    }

    override fun compareTo(other: CommandNode<S>): Int {
        if (this is LiteralCommandNode == other is LiteralCommandNode) {
            return sortedKey.compareTo(other.sortedKey)
        }
        return if (other is LiteralCommandNode) 1 else -1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandNode<*>) return false
        return children == other.children && command == other.command
    }

    override fun hashCode(): Int = 31 * children.hashCode() + (command?.hashCode() ?: 0)

    // Abstract members
    protected abstract fun isValidInput(input: String): Boolean
    abstract val name: String
    abstract val usageText: String
    abstract val examples: Collection<String>
    protected abstract val sortedKey: String

    @Throws(CommandSyntaxException::class)
    abstract fun parse(reader: StringReader, contextBuilder: CommandContextBuilder<S>)

    abstract fun listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions>
    abstract fun createBuilder(): ArgumentBuilder<S, *>
}