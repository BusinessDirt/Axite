package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.builder.LiteralArgumentBuilder
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.context.ContextChain
import github.businessdirt.axite.commands.context.SuggestionContext
import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.exceptions.error
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.nodes.LiteralCommandNode
import github.businessdirt.axite.commands.nodes.RootCommandNode
import github.businessdirt.axite.commands.strings.ImmutableStringReader
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.Suggestions
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import java.util.*
import java.util.concurrent.CompletableFuture

class ParseResults<S>(
    val context: CommandContextBuilder<S>,
    val reader: ImmutableStringReader = StringReader(""),
    val exceptions: Map<CommandNode<S>, CommandSyntaxException> = emptyMap()
)

class CommandDispatcher<S>(val root: RootCommandNode<S> = RootCommandNode()) {

    private var consumer: ResultConsumer<S> = ResultConsumer { _, _, _ -> }

    fun register(command: LiteralArgumentBuilder<S>): LiteralCommandNode<S> {
        val build = command.build()
        root.addChild(build)
        return build
    }

    fun setConsumer(consumer: ResultConsumer<S>) {
        this.consumer = consumer
    }

    @Throws(CommandSyntaxException::class)
    fun execute(input: String, source: S): Int = execute(StringReader(input), source)

    @Throws(CommandSyntaxException::class)
    fun execute(input: StringReader, source: S): Int {
        val parse = parse(input, source)
        return execute(parse)
    }

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

        val commandString = parse.reader.string
        val original = parse.context.build(commandString)
        val flatContext = ContextChain.tryFlatten(original)

        if (flatContext.isEmpty) {
            consumer.onCommandComplete(original, false, 0)
            throw parse.reader.error(CommandError.UnknownCommand)
        }

        return flatContext.get().executeAll(original.source, consumer)
    }

    fun parse(command: String, source: S): ParseResults<S> = parse(StringReader(command), source)

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
        var errors: MutableMap<CommandNode<S>, CommandSyntaxException>? = null
        var potentials: MutableList<ParseResults<S>>? = null
        val cursor = originalReader.cursor

        for (child in node.getRelevantNodes(originalReader)) {
            if (!child.canUse(source)) continue

            val context = contextSoFar.copy()
            val reader = StringReader(originalReader)
            try {
                try {
                    child.parse(reader, context)
                } catch (ex: CommandSyntaxException) {
                    throw ex
                }

                if (reader.canRead() && reader.peek() != ARGUMENT_SEPARATOR_CHAR) {
                    throw reader.error(CommandError.ExpectedSeparator)
                }
            } catch (ex: CommandSyntaxException) {
                if (errors == null) errors = LinkedHashMap()
                errors[child] = ex
                reader.cursor = cursor
                continue
            }

            context.withCommand(child.command)

            val skipLength = if (child.redirect == null) 2 else 1
            if (reader.canRead(skipLength)) {
                reader.skip()
                if (child.redirect != null) {
                    val childContext = CommandContextBuilder(this, source, child.redirect!!, reader.cursor)
                    val parse = parseNodes(child.redirect!!, reader, childContext)
                    context.withChild(parse.context)
                    return ParseResults(context, parse.reader, parse.exceptions)
                } else {
                    val parse = parseNodes(child, reader, context)
                    if (potentials == null) potentials = ArrayList(1)
                    potentials.add(parse)
                }
            } else {
                if (potentials == null) potentials = ArrayList(1)
                potentials.add(ParseResults(context, reader, emptyMap()))
            }
        }

        if (potentials != null) {
            if (potentials.size > 1) {
                potentials.sortWith { a, b ->
                    when {
                        !a.reader.canRead() && b.reader.canRead() -> -1
                        a.reader.canRead() && !b.reader.canRead() -> 1
                        a.exceptions.isEmpty() && b.exceptions.isNotEmpty() -> -1
                        a.exceptions.isNotEmpty() && b.exceptions.isEmpty() -> 1
                        else -> 0
                    }
                }
            }
            return potentials[0]
        }

        return ParseResults(contextSoFar, originalReader, errors ?: emptyMap())
    }

    fun getAllUsage(node: CommandNode<S>, source: S, restricted: Boolean): Array<String> {
        val result = ArrayList<String>()
        getAllUsageInternal(node, source, result, "", restricted)
        return result.toTypedArray()
    }

    private fun getAllUsageInternal(node: CommandNode<S>, source: S, result: MutableList<String>, prefix: String, restricted: Boolean) {
        if (restricted && !node.canUse(source)) return

        if (node.command != null) result.add(prefix)

        if (node.redirect != null) {
            val redirectMsg = if (node.redirect == root) "..." else "-> ${node.redirect.usageText}"
            result.add(if (prefix.isEmpty()) "${node.usageText} $redirectMsg" else "$prefix $redirectMsg")
        } else if (node.allChildren.isNotEmpty()) {
            for (child in node.allChildren) {
                val nextPrefix = if (prefix.isEmpty()) child.usageText else "$prefix $ARGUMENT_SEPARATOR${child.usageText}"
                getAllUsageInternal(child, source, result, nextPrefix, restricted)
            }
        }
    }

    fun getSmartUsage(node: CommandNode<S>, source: S): Map<CommandNode<S>, String> {
        val result = LinkedHashMap<CommandNode<S>, String>()
        val optional = node.command != null
        for (child in node.allChildren) {
            getSmartUsageInternal(child, source, optional, false)?.let {
                result[child] = it
            }
        }
        return result
    }

    private fun getSmartUsageInternal(node: CommandNode<S>, source: S, optional: Boolean, deep: Boolean): String? {
        if (!node.canUse(source)) return null

        val self = if (optional) "$USAGE_OPTIONAL_OPEN${node.usageText}$USAGE_OPTIONAL_CLOSE" else node.usageText
        val childOptional = node.command != null
        val open = if (childOptional) USAGE_OPTIONAL_OPEN else USAGE_REQUIRED_OPEN
        val close = if (childOptional) USAGE_OPTIONAL_CLOSE else USAGE_REQUIRED_CLOSE

        if (!deep) {
            if (node.redirect != null) {
                val redirectMsg = if (node.redirect == root) "..." else "-> ${node.redirect.usageText}"
                return "$self $redirectMsg"
            }
            val children = node.allChildren.filter { it.canUse(source) }
            if (children.size == 1) {
                getSmartUsageInternal(children.first(), source, childOptional, childOptional)?.let {
                    return "$self $it"
                }
            } else if (children.size > 1) {
                val childUsage = children.mapNotNull { getSmartUsageInternal(it, source, childOptional, true) }.toSet()
                if (childUsage.size == 1) {
                    val usage = childUsage.first()
                    return "$self ${if (childOptional) "$USAGE_OPTIONAL_OPEN$usage$USAGE_OPTIONAL_CLOSE" else usage}"
                } else if (childUsage.size > 1) {
                    val builder = StringBuilder(open)
                    children.forEachIndexed { i, child ->
                        if (i > 0) builder.append(USAGE_OR)
                        builder.append(child.usageText)
                    }
                    if (children.isNotEmpty()) {
                        builder.append(close)
                        return "$self $builder"
                    }
                }
            }
        }
        return self
    }

    fun getCompletionSuggestions(parse: ParseResults<S>, cursor: Int = parse.reader.totalLength()): CompletableFuture<Suggestions> {
        val context: CommandContextBuilder<S> = parse.context
        val nodeBeforeCursor: SuggestionContext<S> = context.findSuggestionContext(cursor)
        val parent: CommandNode<S> = nodeBeforeCursor.parent
        val start: Int = minOf(nodeBeforeCursor.startPos, cursor)

        val fullInput = parse.reader.string
        val truncatedInput = fullInput.substring(0, cursor)
        val truncatedInputLower = truncatedInput.lowercase(Locale.ROOT)

        val futures = parent.allChildren.map { node ->
            runCatching {
                node.listSuggestions(context.build(truncatedInput),
                    SuggestionsBuilder(truncatedInput, truncatedInputLower, start)
                )
            }.getOrDefault(Suggestions.empty())
        }

        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
            val suggestions = futures.map { it.join() }
            Suggestions.merge(fullInput, suggestions)
        }
    }

    fun getPath(target: CommandNode<S>): Collection<String> {
        val paths = mutableListOf<List<CommandNode<S>>>()
        addPaths(root, paths, mutableListOf())

        return paths.find { it.last() == target }
            ?.filter { it != root }
            ?.map { it.name }
            ?: emptyList()
    }

    fun findNode(path: Collection<String>): CommandNode<S>? {
        var node: CommandNode<S>? = root
        for (name in path) {
            node = node?.getChild(name)
            if (node == null) return null
        }
        return node
    }

    fun findAmbiguities(consumer: AmbiguityConsumer<S>) = root.findAmbiguities(consumer)

    private fun addPaths(node: CommandNode<S>, result: MutableList<List<CommandNode<S>>>, parents: List<CommandNode<S>>) {
        val current = parents + node
        result.add(current)
        node.allChildren.forEach { addPaths(it, result, current) }
    }

    companion object {
        const val ARGUMENT_SEPARATOR = " "
        const val ARGUMENT_SEPARATOR_CHAR = ' '
        private const val USAGE_OPTIONAL_OPEN = "["
        private const val USAGE_OPTIONAL_CLOSE = "]"
        private const val USAGE_REQUIRED_OPEN = "("
        private const val USAGE_REQUIRED_CLOSE = ")"
        private const val USAGE_OR = "|"
    }
}