package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.context.ContextChain
import github.businessdirt.axite.commands.context.SuggestionContext
import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.exceptions.error
import github.businessdirt.axite.commands.exceptions.expect
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.nodes.LiteralCommandNode
import github.businessdirt.axite.commands.nodes.RootCommandNode
import github.businessdirt.axite.commands.strings.ImmutableStringReader
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.*

/**
 * Holds the result of a command parse.
 *
 * @param S The type of the command source.
 * @property context The command context builder containing the parsed state.
 * @property reader The string reader used during parsing, potentially advanced to the end of the input.
 * @property exceptions A map of exceptions encountered during parsing, keyed by the node where they occurred.
 */
data class ParseResults<S>(
    val context: CommandContextBuilder<S>,
    val reader: ImmutableStringReader = StringReader(""),
    val exceptions: Map<CommandNode<S>, CommandSyntaxException> = emptyMap()
)

/**
 * The core command dispatcher, responsible for registering, parsing, and executing commands.
 *
 * It manages the command tree starting from a [RootCommandNode] and provides methods to
 * dispatch input strings to the appropriate command handlers.
 *
 * @param S The type of the command source (e.g., a player, a console, or an entity).
 * @property root The root node of the command tree. Defaults to a new [RootCommandNode].
 */
class CommandDispatcher<S>(val root: RootCommandNode<S> = RootCommandNode()) {

    private var consumer: ResultConsumer<S> = ResultConsumer { _, _, _ -> }

    /**
     * Registers a new command.
     *
     * @param command The literal command node to register.
     * @return The registered command node.
     */
    fun register(command: LiteralCommandNode<S>): LiteralCommandNode<S> {
        root.addChild(command)
        return command
    }

    /**
     * Sets a consumer to be notified when a command completes (successfully or not).
     *
     * @param consumer The result consumer.
     */
    fun setConsumer(consumer: ResultConsumer<S>) {
        this.consumer = consumer
    }

    /**
     * Parses and executes a command given a string input.
     *
     * @param input The command string to execute.
     * @param source The command source initiating the execution.
     * @return The integer result returned by the command.
     * @throws CommandSyntaxException If the command input is invalid.
     */
    @Throws(CommandSyntaxException::class)
    fun execute(input: String, source: S): Int = execute(StringReader(input), source)

    /**
     * Parses and executes a command given a [StringReader].
     *
     * @param input The string reader containing the command input.
     * @param source The command source initiating the execution.
     * @return The integer result returned by the command.
     * @throws CommandSyntaxException If the command input is invalid.
     */
    @Throws(CommandSyntaxException::class)
    fun execute(input: StringReader, source: S): Int {
        val parse: ParseResults<S> = parse(input, source)
        return execute(parse)
    }

    /**
     * Executes a previously parsed command.
     *
     * @param parse The results of the parse phase.
     * @return The integer result returned by the command.
     * @throws CommandSyntaxException If the parse results indicate failure or if execution fails.
     */
    @Throws(CommandSyntaxException::class)
    fun execute(parse: ParseResults<S>): Int {
        if (parse.reader.canRead()) {
            val exceptions = parse.exceptions
            when {
                exceptions.size == 1 -> throw exceptions.values.iterator().next()
                parse.context.range.isEmpty -> throw parse.reader.error(CommandError.UnknownCommand)
                else -> throw parse.reader.error(CommandError.UnknownArgument)
            }
        }

        val commandString: String = parse.reader.string
        val original: CommandContext<S> = parse.context.build(commandString)
        val flatContext: Optional<ContextChain<S>> = ContextChain.tryFlatten(original)

        if (flatContext.isEmpty) {
            consumer.onCommandComplete(original, false, 0)
            throw parse.reader.error(CommandError.UnknownCommand)
        }

        return flatContext.get().executeAll(original.source, consumer)
    }

    /**
     * Parses a command string without executing it.
     *
     * @param command The command string to parse.
     * @param source The source to use for parsing (checking requirements, etc.).
     * @return The results of the parse.
     */
    fun parse(command: String, source: S): ParseResults<S> = parse(StringReader(command), source)

    /**
     * Parses a command from a [StringReader] without executing it.
     *
     * @param command The reader containing the command input.
     * @param source The source to use for parsing.
     * @return The results of the parse.
     */
    fun parse(command: StringReader, source: S): ParseResults<S> {
        val context = CommandContextBuilder(this, source, root, command.cursor)
        return parseNodes(root, command, context)
    }

    private fun parseNodes(
        node: CommandNode<S>,
        originalReader: StringReader,
        contextSoFar: CommandContextBuilder<S>
    ): ParseResults<S> {
        val source = contextSoFar.source
        val errors = mutableMapOf<CommandNode<S>, CommandSyntaxException>()
        val potentials = mutableListOf<ParseResults<S>>()
        val cursor = originalReader.cursor

        for (child in node.getRelevantNodes(originalReader)) {
            if (!child.canUse(source)) continue

            val context = contextSoFar.copy()
            val reader = StringReader(originalReader)

            try {
                child.parse(reader, context)
                reader.expect(CommandError.ExpectedSeparator) {
                    !canRead() || (canRead() && peek() == ARGUMENT_SEPARATOR)
                }
            } catch (ex: CommandSyntaxException) {
                errors[child] = ex
                reader.cursor = cursor
                continue
            }

            child.command?.let { context.command(it) }

            val redirect = child.redirect
            val skipLength = if (redirect == null) 2 else 1

            if (reader.canRead(skipLength)) {
                reader.skip()
                if (redirect != null) {
                    val childContext = CommandContextBuilder(this, source, redirect, reader.cursor)
                    val parse = parseNodes(redirect, reader, childContext)
                    context.child = parse.context
                    return ParseResults(context, parse.reader, parse.exceptions)
                } else {
                    potentials.add(parseNodes(child, reader, context))
                }
            } else {
                potentials.add(ParseResults(context, reader, emptyMap()))
            }
        }

        return potentials.bestOrFallback(contextSoFar, originalReader, errors)
    }

    private fun List<ParseResults<S>>.bestOrFallback(
        contextSoFar: CommandContextBuilder<S>,
        reader: StringReader,
        errors: Map<CommandNode<S>, CommandSyntaxException>
    ): ParseResults<S> {
        if (isEmpty()) return ParseResults(contextSoFar, reader, errors)

        return minWithOrNull(
            compareBy<ParseResults<S>> { it.reader.canRead() } // Finished readers first
                .thenBy { it.exceptions.isNotEmpty() }        // No exceptions first
        ) ?: first()
    }

    /**
     * Generates all possible usage strings for a given node.
     *
     * @param node The node to generate usage for.
     * @param source The command source (for permission checks).
     * @param restricted If true, only include paths the source can access.
     * @return An array of usage strings.
     */
    fun getAllUsage(
        node: CommandNode<S>,
        source: S,
        restricted: Boolean
    ): Array<String> = mutableListOf<String>().apply {
        fillUsage(node, source, "", restricted)
    }.toTypedArray()

    private fun MutableList<String>.fillUsage(
        node: CommandNode<S>,
        source: S,
        prefix: String,
        restricted: Boolean
    ) {
        if (restricted && !node.canUse(source)) return

        node.command?.let { add(prefix) }
        node.redirect?.let { redirect ->
            val redirectMsg = if (redirect == root) "..." else "-> ${redirect.usageText}"
            add(if (prefix.isEmpty()) "${node.usageText} $redirectMsg" else "$prefix $redirectMsg")
        } ?: run {
            for (child in node.allChildren) {
                val separator = if (prefix.isEmpty()) "" else ARGUMENT_SEPARATOR
                fillUsage(child, source, "$prefix$separator${child.usageText}", restricted)
            }
        }
    }

    /**
     * Generates a map of smart usage strings for the children of a given node.
     * This is useful for displaying help messages that summarize available subcommands.
     *
     * @param node The node to generate usage for.
     * @param source The command source.
     * @return A map where keys are child nodes and values are their usage strings.
     */
    fun getSmartUsage(node: CommandNode<S>, source: S): Map<CommandNode<S>, String> = buildMap {
        val isOptional = node.command != null

        for (child in node.allChildren) {
            if (child.canUse(source)) {
                getSmartUsageInternal(child, source, isOptional, false)?.let { usage ->
                    put(child, usage)
                }
            }
        }
    }

    private fun getSmartUsageInternal(node: CommandNode<S>, source: S, optional: Boolean, deep: Boolean): String? {
        if (!node.canUse(source)) return null

        val self = node.usageText.wrapIf(optional, USAGE_OPTIONAL_OPEN, USAGE_OPTIONAL_CLOSE)
        if (deep) return self

        // Handle Redirects
        node.redirect?.let {
            val redirectMsg = if (it == root) "..." else "-> ${it.usageText}"
            return "$self$ARGUMENT_SEPARATOR$redirectMsg"
        }

        val children = node.allChildren.filter { it.canUse(source) }
        val childOptional = node.command != null

        return when (children.size) {
            0 -> self
            1 -> {
                val usage = getSmartUsageInternal(children.first(), source, childOptional, childOptional)
                if (usage != null) "$self$ARGUMENT_SEPARATOR$usage" else self
            }
            else -> {
                val distinctUsages = children.mapNotNull { getSmartUsageInternal(it, source, childOptional, true) }.toSet()

                if (distinctUsages.size == 1) {
                    val usage = distinctUsages.first().wrapIf(childOptional, USAGE_OPTIONAL_OPEN, USAGE_OPTIONAL_CLOSE)
                    "$self $usage"
                } else {
                    val open = if (childOptional) USAGE_OPTIONAL_OPEN else USAGE_REQUIRED_OPEN
                    val close = if (childOptional) USAGE_OPTIONAL_CLOSE else USAGE_REQUIRED_CLOSE
                    val list = children.joinToString(USAGE_OR, prefix = open, postfix = close) { it.usageText }
                    "$self $list"
                }
            }
        }
    }

    private fun String.wrapIf(condition: Boolean, open: String, close: String) =
        if (condition) "$open$this$close" else this

    /**
     * Gets tab completion suggestions for a given parse result.
     *
     * @param parse The result of a previous parse.
     * @param cursor The cursor position to generate suggestions for (defaults to the end of input).
     * @return A [Suggestions] object containing the list of suggestions.
     */
    suspend fun getCompletionSuggestions(parse: ParseResults<S>, cursor: Int = parse.reader.totalLength()): Suggestions = coroutineScope {
        val context: CommandContextBuilder<S> = parse.context
        val nodeBeforeCursor: SuggestionContext<S> = context.findSuggestionContext(cursor)
        val parent: CommandNode<S> = nodeBeforeCursor.parent
        val start: Int = minOf(nodeBeforeCursor.startPos, cursor)

        val fullInput = parse.reader.string
        val truncatedInput = fullInput.substring(0, cursor)
        val truncatedInputLower = truncatedInput.lowercase(Locale.ROOT)

        val suggestions = parent.allChildren.map { node ->
            async {
                try {
                    node.listSuggestions(context.build(truncatedInput),
                        SuggestionsBuilder(truncatedInput, truncatedInputLower, start)
                    )
                } catch (_: Exception) {
                    Suggestions.empty()
                }
            }
        }.awaitAll()

        Suggestions.merge(fullInput, suggestions)
    }

    /**
     * Finds the path to a specific node from the root.
     *
     * @param target The target node.
     * @return A collection of strings representing the path (names of nodes) to the target.
     */
    fun getPath(target: CommandNode<S>): Collection<String> {
        val paths = mutableListOf<List<CommandNode<S>>>()
        addPaths(root, paths, mutableListOf())

        return paths.find { it.last() == target }
            ?.filter { it != root }
            ?.map { it.name }
            ?: emptyList()
    }

    /**
     * Finds a node given a path of names.
     *
     * @param path The path to search for.
     * @return The node if found, or null.
     */
    fun findNode(path: Collection<String>): CommandNode<S>? {
        var node: CommandNode<S>? = root
        for (name in path) {
            node = node?.getChild(name)
            if (node == null) return null
        }
        return node
    }

    /**
     * Scans the command tree for ambiguous nodes and reports them to the consumer.
     *
     * @param consumer The consumer to handle ambiguity reports.
     */
    fun findAmbiguities(consumer: AmbiguityConsumer<S>) = root.findAmbiguities(consumer)

    private fun addPaths(node: CommandNode<S>, result: MutableList<List<CommandNode<S>>>, parents: List<CommandNode<S>>) {
        val current = parents + node
        result.add(current)
        node.allChildren.forEach { addPaths(it, result, current) }
    }

    companion object {
        private const val ARGUMENT_SEPARATOR = ' '
        private const val USAGE_OPTIONAL_OPEN = "["
        private const val USAGE_OPTIONAL_CLOSE = "]"
        private const val USAGE_REQUIRED_OPEN = "("
        private const val USAGE_REQUIRED_CLOSE = ")"
        private const val USAGE_OR = "|"
    }
}
