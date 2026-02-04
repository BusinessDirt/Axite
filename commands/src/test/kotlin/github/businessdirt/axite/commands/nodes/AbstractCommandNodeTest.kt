package github.businessdirt.axite.commands.nodes

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.builder.literal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

@DisplayName("Command Node Base Logic")
abstract class AbstractCommandNodeTest {

    val command: Command<Any> = mock()

    /**
     * Factory method for subclasses to provide the specific node type being tested.
     */
    protected abstract fun createCommandNode(): CommandNode<Any>

    @Test
    @DisplayName("addChild() should deduplicate nodes with the same name")
    fun testAddChild() {
        val node = createCommandNode()

        node.addChild(literal("child1"))
        node.addChild(literal("child2"))
        node.addChild(literal("child1"))

        assertEquals(2, node.allChildren.size, "Children should be deduplicated by name")
    }

    @Test
    @DisplayName("addChild() should merge grandchildren if the child already exists")
    fun testAddChildMergesGrandchildren() {
        val node = createCommandNode()

        node.addChild(
            literal("child") {
                literal("grandchild1")
            }
        )

        node.addChild(
            literal("child") {
                literal("grandchild2")
            }
        )

        assertEquals(1, node.allChildren.size, "Parent nodes should merge")
        val child = node.allChildren.first()
        assertEquals(2, child.allChildren.size, "Grandchildren should be merged into the existing child")
    }

    @Test
    @DisplayName("addChild() should preserve the existing command if the new node has none")
    fun testAddChildPreservesCommand() {
        val node = createCommandNode()

        node.addChild(literal("child") {
            executes(this@AbstractCommandNodeTest.command)
        })
        node.addChild(literal("child"))

        assertSame(command, node.allChildren.first().command, "Command should not be cleared by a node without one")
    }

    @Test
    @DisplayName("addChild() should overwrite the command if the new node provides one")
    fun testAddChildOverwritesCommand() {
        val node = createCommandNode()

        node.addChild(literal("child"))
        node.addChild(literal("child") {
            executes(this@AbstractCommandNodeTest.command)
        })

        assertSame(command, node.allChildren.first().command, "Newer command should overwrite the old one")
    }
}