package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.builder.LiteralArgumentBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@DisplayName("Command Node Base Logic")
abstract class AbstractCommandNodeTest {

    @Mock
    lateinit var command: Command<Any>

    /**
     * Factory method for subclasses to provide the specific node type being tested.
     */
    protected abstract fun createCommandNode(): CommandNode<Any>

    @BeforeEach
    open fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @DisplayName("addChild() should deduplicate nodes with the same name")
    fun testAddChild() {
        val node = createCommandNode()

        node.addChild(LiteralArgumentBuilder.literal<Any>("child1").build())
        node.addChild(LiteralArgumentBuilder.literal<Any>("child2").build())
        node.addChild(LiteralArgumentBuilder.literal<Any>("child1").build())

        assertEquals(2, node.allChildren.size, "Children should be deduplicated by name")
    }

    @Test
    @DisplayName("addChild() should merge grandchildren if the child already exists")
    fun testAddChildMergesGrandchildren() {
        val node = createCommandNode()

        node.addChild(
            LiteralArgumentBuilder.literal<Any>("child")
                .then(LiteralArgumentBuilder.literal("grandchild1"))
                .build()
        )

        node.addChild(
            LiteralArgumentBuilder.literal<Any>("child")
                .then(LiteralArgumentBuilder.literal("grandchild2"))
                .build()
        )

        assertEquals(1, node.allChildren.size, "Parent nodes should merge")
        val child = node.allChildren.first()
        assertEquals(2, child.allChildren.size, "Grandchildren should be merged into the existing child")
    }

    @Test
    @DisplayName("addChild() should preserve the existing command if the new node has none")
    fun testAddChildPreservesCommand() {
        val node = createCommandNode()

        node.addChild(LiteralArgumentBuilder.literal<Any>("child").executes(command).build())
        node.addChild(LiteralArgumentBuilder.literal<Any>("child").build())

        assertSame(command, node.allChildren.first().command, "Command should not be cleared by a node without one")
    }

    @Test
    @DisplayName("addChild() should overwrite the command if the new node provides one")
    fun testAddChildOverwritesCommand() {
        val node = createCommandNode()

        node.addChild(LiteralArgumentBuilder.literal<Any>("child").build())
        node.addChild(LiteralArgumentBuilder.literal<Any>("child").executes(command).build())

        assertSame(command, node.allChildren.first().command, "Newer command should overwrite the old one")
    }
}