package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.RedirectModifier
import github.businessdirt.axite.commands.nodes.CommandNode
import github.businessdirt.axite.commands.strings.StringRange
import java.util.*

class CommandContext<S>(
    val source: S,
    val input: String,
    val arguments: Map<String, ParsedArgument<S, *>>,
    val command: Command<S>?,
    val rootNode: CommandNode<S>,
    val nodes: List<ParsedCommandNode<S>>,
    val range: StringRange,
    val child: CommandContext<S>?,
    val modifier: RedirectModifier<S>?,
    val isForked: Boolean
) {

    /**
     * Finds the last child in the command chain.
     */
    val lastChild: CommandContext<S>
        get() {
            var result = this
            while (result.child != null) {
                result = result.child!!
            }
            return result
        }

    fun copyFor(source: S): CommandContext<S> {
        if (this.source == source) return this
        return CommandContext(source, input, arguments, command, rootNode, nodes, range, child, modifier, isForked)
    }

    /**
     * Gets an argument by name and casts it to [V].
     * Reified types allow us to skip passing the class object manually.
     */
    @Suppress("UNCHECKED_CAST")
    fun <V : Any> getArgument(name: String, clazz: Class<V>): V {
        val argument = arguments[name] ?: throw IllegalArgumentException("No such argument '$name' exists on this command")
        val result = argument.result as Any

        val targetClass = PRIMITIVE_TO_WRAPPER[clazz] ?: clazz
        if (targetClass.isAssignableFrom(result.javaClass)) {
            return result as V
        } else {
            throw IllegalArgumentException("Argument '$name' is defined as ${result.javaClass.simpleName}, not ${clazz.simpleName}")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandContext<*>) return false

        return arguments == other.arguments &&
                rootNode == other.rootNode &&
                nodes == other.nodes &&
                command == other.command &&
                source == other.source &&
                child == other.child
    }

    override fun hashCode(): Int {
        var result = source?.hashCode() ?: 0
        result = 31 * result + arguments.hashCode()
        result = 31 * result + (command?.hashCode() ?: 0)
        result = 31 * result + rootNode.hashCode()
        result = 31 * result + nodes.hashCode()
        result = 31 * result + (child?.hashCode() ?: 0)
        return result
    }

    companion object {
        private val PRIMITIVE_TO_WRAPPER = mapOf<Class<*>, Class<*>>(
            Boolean::class.javaPrimitiveType!! to Boolean::class.javaObjectType,
            Byte::class.javaPrimitiveType!! to Byte::class.javaObjectType,
            Short::class.javaPrimitiveType!! to Short::class.javaObjectType,
            Char::class.javaPrimitiveType!! to Character::class.javaObjectType,
            Int::class.javaPrimitiveType!! to Integer::class.javaObjectType,
            Long::class.javaPrimitiveType!! to java.lang.Long::class.javaObjectType,
            Float::class.javaPrimitiveType!! to java.lang.Float::class.javaObjectType,
            Double::class.javaPrimitiveType!! to java.lang.Double::class.javaObjectType
        )
    }
}