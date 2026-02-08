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
import java.util.function.Predicate

/**
 * A command node that matches an argument using an [ArgumentType].
 *
 * @param S The type of the command source.
 * @param T The type of the argument value.
 * @property name The name of the argument.
 * @property type The type of the argument.
 * @property customSuggestions Optional custom suggestion provider.
 */
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

    override suspend fun listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): Suggestions = customSuggestions?.getSuggestions(context, builder)
        ?: type.listSuggestions(context, builder)

    override fun createBuilder(): RequiredArgumentBuilder<S, T> = RequiredArgumentBuilder<S, T>(name, type).apply {
        requires(this@ArgumentCommandNode.requirement)
        forward(this@ArgumentCommandNode.redirect, this@ArgumentCommandNode.modifier, this@ArgumentCommandNode.forks)
        suggests(customSuggestions)
        command?.let { executes(it) }
    }

    override fun isValidInput(input: String): Boolean = try {
        val reader = StringReader(input)
        type.parse(reader)
        !reader.canRead() || reader.peek() == ' '
    } catch (_: CommandSyntaxException) {
        false
    }

    override fun toString(): String = "${USAGE_ARGUMENT_OPEN}argument $name:$type$USAGE_ARGUMENT_CLOSE"
    override fun hashCode(): Int = 31 * name.hashCode() + type.hashCode()
    override fun equals(other: Any?): Boolean =
        this === other || (other is ArgumentCommandNode<*, *> && name == other.name && type == other.type && super.equals(other))

    companion object {
        private const val USAGE_ARGUMENT_OPEN = "<"
        private const val USAGE_ARGUMENT_CLOSE = ">"
    }
}
