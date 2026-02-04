package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.builder.literal
import github.businessdirt.axite.commands.context.CommandContext
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

@DisplayName("RootCommandNode Implementation Tests")
class RootCommandNodeTest : AbstractCommandNodeTest() {
    private lateinit var node: RootCommandNode<Any>

    override fun createCommandNode(): CommandNode<Any> = node

    @BeforeEach
    fun setUp() {
        node = RootCommandNode()
    }

    @Test
    @DisplayName("parse() should not move the cursor")
    fun testParse() {
        val reader = StringReader("hello world")
        val contextBuilder = CommandContextBuilder(CommandDispatcher(), Any(), RootCommandNode(), 0)

        node.parse(reader, contextBuilder)
        assertEquals(0, reader.cursor, "Root node should never consume input characters")
    }

    @Test
    @DisplayName("addChild() should throw when attempting to add another root node")
    fun testAddChildNoRoot() {
        assertThrows(UnsupportedOperationException::class.java) {
            node.addChild(RootCommandNode())
        }
    }

    @Test
    @DisplayName("usageText should always be empty for root")
    fun testUsage() {
        assertEquals("", node.usageText)
    }

    @Test
    @DisplayName("listSuggestions() should always return empty suggestions")
    @Suppress("UNCHECKED_CAST")
    fun testSuggestions() {
        val context = mock(CommandContext::class.java) as CommandContext<Any>
        val result = node.listSuggestions(context, SuggestionsBuilder("", "", 0)).join()
        assertTrue(result.isEmpty, "Root node should not provide suggestions directly")
    }

    @Test
    @DisplayName("createBuilder() should throw IllegalStateException")
    fun testCreateBuilder() {
        assertThrows(IllegalStateException::class.java) {
            node.createBuilder()
        }
    }

    @Test
    @DisplayName("Equality and structure matching")
    fun testEquals() {
        val nodeA = RootCommandNode<Any>()
        val nodeB = RootCommandNode<Any>()

        val nodeC = RootCommandNode<Any>().apply {
            addChild(literal("foo"))
        }
        val nodeD = RootCommandNode<Any>().apply {
            addChild(literal("foo"))
        }

        assertAll(
            { assertEquals(nodeA, nodeB, "Empty root nodes should be equal") },
            { assertEquals(nodeC, nodeD, "Root nodes with identical children should be equal") },
            { assertNotEquals(nodeA, nodeC, "Root nodes with different children should not be equal") }
        )
    }
}