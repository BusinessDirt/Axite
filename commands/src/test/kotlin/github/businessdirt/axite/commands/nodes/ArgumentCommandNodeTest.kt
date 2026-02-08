package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.CommandDispatcher
import github.businessdirt.axite.commands.arguments.IntegerArgumentType
import github.businessdirt.axite.commands.builder.argument
import github.businessdirt.axite.commands.builder.testCommand
import github.businessdirt.axite.commands.builder.testRequirement
import github.businessdirt.axite.commands.context.CommandContextBuilder
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.SuggestionsBuilder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

@DisplayName("ArgumentCommandNode Implementation Tests")
class ArgumentCommandNodeTest : AbstractCommandNodeTest() {
    private lateinit var node: ArgumentCommandNode<Any, Int>
    private lateinit var contextBuilder: CommandContextBuilder<Any>

    override fun createCommandNode(): CommandNode<Any> = node

    @BeforeEach
    fun setUp() {
        node = argument("foo", IntegerArgumentType())
        contextBuilder = CommandContextBuilder(CommandDispatcher(), Any(), RootCommandNode(), 0)
    }

    @Test
    @DisplayName("parse() should extract the correct value and store it in the context")
    fun testParse() {
        val reader = StringReader("123 456")
        node.parse(reader, contextBuilder)

        val argument = contextBuilder.arguments["foo"]
        assertNotNull(argument, "Argument 'foo' should be present in context")
        assertEquals(123, argument!!.result, "Parsed result should be 123")
    }

    @Test
    @DisplayName("usageText should be wrapped in angle brackets")
    fun testUsage() {
        assertEquals("<foo>", node.usageText)
    }

    @Test
    @DisplayName("listSuggestions() should default to empty for integers")
    fun testSuggestions() = runTest {
        val context = contextBuilder.build("")
        val result = node.listSuggestions(context, SuggestionsBuilder("", "", 0))
        assertTrue(result.isEmpty, "Suggestions should be empty by default for IntegerArgumentType")
    }

    @Test
    @DisplayName("createBuilder() should preserve all node properties")
    fun testCreateBuilder() {
        val builder = node.createBuilder()

        assertAll(
            { assertEquals(node.name, builder.name) },
            { assertEquals(node.type, builder.type) },
            { assertEquals(node.requirement, builder.testRequirement) },
            { assertEquals(node.command, builder.testCommand) }
        )
    }

    @Test
    @DisplayName("Equality and Identity checks")
    @Suppress("UNCHECKED_CAST")
    fun testEquals() {
        val command = mock(Command::class.java) as Command<Any>

        // Group 1: Identical simple nodes
        val nodeA = argument<Any, Int>("foo", IntegerArgumentType())
        val nodeB = argument<Any, Int>("foo", IntegerArgumentType())

        // Group 2: Nodes with same command
        val nodeC = argument("foo", IntegerArgumentType()) {
            executes(command)
        }
        val nodeD = argument("foo", IntegerArgumentType()) {
            executes(command)
        }

        // Group 3: Different name/range
        val nodeE = argument<Any, Int>("bar", IntegerArgumentType(-100, 100))

        assertAll(
            { assertEquals(nodeA, nodeB, "Simple nodes should be equal") },
            { assertEquals(nodeC, nodeD, "Nodes with same command should be equal") },
            { assertNotEquals(nodeA, nodeC, "Nodes with different commands should not be equal") },
            { assertNotEquals(nodeA, nodeE, "Nodes with different names/types should not be equal") }
        )
    }
}