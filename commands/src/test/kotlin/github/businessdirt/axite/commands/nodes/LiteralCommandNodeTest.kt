package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.builder.literal
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.exceptions.CommandError
import github.businessdirt.axite.commands.exceptions.CommandSyntaxException
import github.businessdirt.axite.commands.strings.StringRange
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.StringSuggestion
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

@DisplayName("LiteralCommandNode Implementation Tests")
class LiteralCommandNodeTest : AbstractCommandNodeTest() {
    private lateinit var node: LiteralCommandNode<Any>
    private lateinit var contextBuilder: CommandContextBuilder<Any>

    override fun createCommandNode(): CommandNode<Any> = node

    @BeforeEach
    fun setUp() {
        node = literal("foo")
        contextBuilder = CommandContextBuilder(CommandDispatcher(), Any(), RootCommandNode(), 0)
    }

    @Test
    @DisplayName("parse() should consume literal and stop at separator")
    fun testParse() {
        val reader = StringReader("foo bar")
        node.parse(reader, contextBuilder)
        assertEquals(" bar", reader.remaining())
    }

    @Test
    @DisplayName("parse() should consume exact literal match")
    fun testParseExact() {
        val reader = StringReader("foo")
        node.parse(reader, contextBuilder)
        assertEquals("", reader.remaining())
    }

    @Test
    @DisplayName("parse() should fail on similar prefixes without separators")
    fun testParseSimilar() {
        val reader = StringReader("foobar")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            node.parse(reader, contextBuilder)
        }

        assertEquals(CommandError.InvalidLiteral::class, ex.type::class)
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("parse() should fail on completely different literals")
    fun testParseInvalid() {
        val reader = StringReader("bar")
        val ex = assertThrows(CommandSyntaxException::class.java) {
            node.parse(reader, contextBuilder)
        }
        assertEquals(0, ex.cursor)
    }

    @Test
    @DisplayName("usageText should be the literal itself")
    fun testUsage() {
        assertEquals("foo", node.usageText)
    }

    @Test
    @DisplayName("listSuggestions() logic for literals")
    fun testSuggestions() {
        // Empty input suggests the literal
        val empty = node.listSuggestions(contextBuilder.build(""), SuggestionsBuilder("", "", 0)).join()
        assertEquals(listOf(StringSuggestion(StringRange.at(0), "foo")), empty.list)

        // Exact match or partial non-match should result in no suggestions
        assertTrue(node.listSuggestions(contextBuilder.build("foo"), SuggestionsBuilder("foo", "foo", 0)).join().isEmpty)
        assertTrue(node.listSuggestions(contextBuilder.build("food"), SuggestionsBuilder("food", "food", 0)).join().isEmpty)
        assertTrue(node.listSuggestions(contextBuilder.build("b"), SuggestionsBuilder("b", "b", 0)).join().isEmpty)
    }

    @Test
    @DisplayName("Equality and structure matching")
    @Suppress("UNCHECKED_CAST")
    fun testEquals() {
        val command = mock(Command::class.java) as Command<Any>

        val nodeA = literal<Any>("foo")
        val nodeB = literal<Any>("foo")
        val nodeC = literal("bar") { executes(command) }
        val nodeD = literal("bar") { executes(command) }

        val nestedA = literal<Any>("foo") { literal("bar") }
        val nestedB = literal<Any>("foo") { literal("bar") }

        assertAll(
            { assertEquals(nodeA, nodeB) },
            { assertEquals(nodeC, nodeD) },
            { assertEquals(nestedA, nestedB) },
            { assertNotEquals(nodeA, nodeC) }
        )
    }

    @Test
    @DisplayName("createBuilder() should transfer all literal properties")
    fun testCreateBuilder() {
        val builder = node.createBuilder()
        assertEquals(node.literal, builder.literal)
        assertEquals(node.requirement, builder.requirement)
        assertEquals(node.command, builder.command)
    }
}