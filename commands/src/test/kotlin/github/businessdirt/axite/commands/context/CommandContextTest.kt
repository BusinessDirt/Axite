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

    private lateinit var builder: CommandContextBuilder<Any>
    private val source: Any = mock()
    private val dispatcher: CommandDispatcher<Any> = mock()
    private val rootNode: CommandNode<Any> = mock()

    @BeforeEach
    fun setUp() {
        builder = CommandContextBuilder(dispatcher, source, rootNode, 0)
    }

    @Test
    @DisplayName("getArgument should throw when argument name is missing")
    fun testGetArgument_nonexistent() {
        val context = builder.build("")
        assertThrows(IllegalArgumentException::class.java) {
            context.getArgument("foo", Any::class.java)
        }
    }

    @Test
    @DisplayName("getArgument should throw when type requested doesn't match actual type")
    fun testGetArgument_wrongType() {
        val context = builder
            .withArgument("foo", ParsedArgument(0, 1, 123))
            .build("123")

        assertThrows(IllegalArgumentException::class.java) {
            context.getArgument("foo", String::class.java)
        }
    }

    @Test
    @DisplayName("getArgument should return correctly cast value")
    fun testGetArgument() {
        val context = builder
            .withArgument("foo", ParsedArgument(0, 1, 123))
            .build("123")

        assertEquals(123, context.getArgument("foo", Int::class.java))
    }

    @Test
    @DisplayName("Context should preserve the source and root node")
    fun testMetadata() {
        val context = builder.build("")
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
                    builder.build(""),
                    CommandContextBuilder(dispatcher, source, rootNode, 0).build("")
                )
            },
            {
                assertNotEquals(
                    builder.build(""),
                    CommandContextBuilder(dispatcher, source, otherRootNode, 0).build("")
                )
            },
            {
                assertNotEquals(
                    builder.build(""),
                    CommandContextBuilder(dispatcher, otherSource, rootNode, 0).build("")
                )
            },
            {
                val c1 = CommandContextBuilder(dispatcher, source, rootNode, 0).withCommand(command).build("")
                val c2 = CommandContextBuilder(dispatcher, source, rootNode, 0).withCommand(otherCommand).build("")
                assertNotEquals(c1, c2)
            }
        )
    }
}