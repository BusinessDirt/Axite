package github.businessdirt.axite.commands.context

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.nodes.CommandNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertNotEquals

@DisplayName("Command Context Tests")
class CommandContextTest {

    private val source: Any = mock()
    private val dispatcher: CommandDispatcher<Any> = mock()
    private val rootNode: CommandNode<Any> = mock()

    fun String.buildTestContext(
        init: CommandContextBuilder<Any>.() -> Unit = {}
    ): CommandContext<Any> = CommandContextBuilder(dispatcher, source, rootNode, 0)
        .apply(init)
        .build(this)

    @Test
    @DisplayName("getArgument should throw when argument name is missing")
    fun testGetArgument_nonexistent() {
        val context = "".buildTestContext()
        assertThrows(IllegalArgumentException::class.java) {
            context.getArgument<Int>("foo")
        }
    }

    @Test
    @DisplayName("getArgument should throw when type requested doesn't match actual type")
    fun testGetArgument_wrongType() {
        val context = "123".buildTestContext {
            argument("foo", ParsedArgument(0, 1, 123))
        }

        assertThrows(IllegalArgumentException::class.java) {
            context.getArgument<String>("foo")
        }
    }

    @Test
    @DisplayName("getArgument should return correctly cast value")
    fun testGetArgument() {
        val context = "123".buildTestContext {
            argument("foo", ParsedArgument(0, 1, 123))
        }

        assertEquals(123, context.getArgument("foo"))
    }

    @Test
    @DisplayName("Context should preserve the source and root node")
    fun testMetadata() {
        val context = "".buildTestContext()
        assertAll(
            { assertEquals(source, context.source) },
            { assertEquals(rootNode, context.rootNode) }
        )
    }

    @Test
    @DisplayName("Manual Equality Groups")
    fun testEquals() {
        val otherSource = Any()
        val command = mock<Command<Any>>()
        val otherCommand = mock<Command<Any>>()
        val otherRootNode = mock<CommandNode<Any>>()

        assertAll(
            {
                assertEquals(
                    "".buildTestContext(),
                    "".buildTestContext()
                )
            },
            {
                assertNotEquals(
                    "".buildTestContext(),
                    "".buildContext(dispatcher, source, otherRootNode, 0) {}
                )
            },
            {
                assertNotEquals(
                    "".buildTestContext(),
                    "".buildContext(dispatcher, otherSource, rootNode, 0) {}
                )
            },
            {
                val c1 = "".buildTestContext { command(command) }
                val c2 = "".buildTestContext { command(otherCommand) }
                assertNotEquals(c1, c2)
            }
        )
    }
}