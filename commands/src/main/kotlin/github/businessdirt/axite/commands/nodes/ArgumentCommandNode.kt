package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.SuggestionProvider
import github.businessdirt.axite.commands.arguments.ArgumentType
import github.businessdirt.axite.commands.builder.RequiredArgumentBuilder
import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.context.ParsedArgument
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.concurrent.CompletableFuture
import java.util.function.Predicate

class ArgumentCommandNode<S, T>(
    override val name: String,
    val type: ArgumentType<T>,
    command: Command<S>?,
    requirement: Predicate<S>,
    redirect: CommandNode<S>?,
    modifier: RedirectModifier<S>?,
    forks: Boolean,
    val customSuggestions: SuggestionProvider<S>?
) : CommandNode<S>(command, requirement, redirect, modifier, forks) {

    override val usageText: String
        get() = "$USAGE_ARGUMENT_OPEN$name$USAGE_ARGUMENT_CLOSE"

    override val sortedKey: String
        get() = name

    override val examples: Collection<String>
        get() = type.examples

    override fun parse(reader: StringReader, contextBuilder: CommandContextBuilder<S>) {
        val start = reader.cursor
        val result = type.parse(reader, contextBuilder.source)
        val parsed = ParsedArgument<S, T>(start, reader.cursor, result)

        contextBuilder.argument(name, parsed)
        contextBuilder.addNode(this, parsed.range)
    }

    override fun listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return customSuggestions?.getSuggestions(context, builder)
            ?: type.listSuggestions(context, builder)
    }

    override fun createBuilder(): RequiredArgumentBuilder<S, T> {
        val builder = RequiredArgumentBuilder<S, T>(name, type)
        builder.apply {
            requires(this@ArgumentCommandNode.requirement)
            forward(this@ArgumentCommandNode.redirect, this@ArgumentCommandNode.modifier, isFork)
            suggests(customSuggestions)
            this@ArgumentCommandNode.command?.let { executes(it) }
        }

        return builder
    }

    override fun isValidInput(input: String): Boolean {
        return try {
            val reader = StringReader(input)
            type.parse(reader)
            !reader.canRead() || reader.peek() == ' '
        } catch (_: CommandSyntaxException) {
            false
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ArgumentCommandNode<*, *>) return false
        return name == other.name && type == other.type && super.equals(other)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String = "<argument $name:$type>"

    companion object {
        private const val USAGE_ARGUMENT_OPEN = "<"
        private const val USAGE_ARGUMENT_CLOSE = ">"
    }
}