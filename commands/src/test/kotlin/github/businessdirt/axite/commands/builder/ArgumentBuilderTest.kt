package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.Command
import github.businessdirt.axite.commands.arguments.IntegerArgumentType
import github.businessdirt.axite.commands.nodes.CommandNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.mockito.Mockito.mock
import java.util.function.Predicate
import kotlin.test.Test

@DisplayName("ArgumentBuilder logic tests")
class ArgumentBuilderTest {

    @Test
    @DisplayName("apply { argument } should successfully add a child argument")
    fun testArguments() {
        val builder = testBuilder { argument("bar", IntegerArgumentType()) }
        val builtArgument = argument<Any, Int>("bar", IntegerArgumentType())

        assertEquals(1, builder.allArguments.size)
        assertTrue(builder.allArguments.contains(builtArgument), "Builder should contain the added argument")
    }

    @Test
    @DisplayName("Should throw if a redirect is already set")
    fun testThen_withRedirect() {
        val target: CommandNode<Any> = mock()
        val builder = testBuilder { redirect(target) }

        assertThrows(IllegalStateException::class.java) { builder.literal("foo") }
    }

    @Test
    @DisplayName("redirect() should correctly set the target node")
    fun testRedirect() {
        val target: CommandNode<Any> = mock()
        val builder = testBuilder { redirect(target) }

        assertEquals(target, builder.testRedirect)
    }

    @Test
    @DisplayName("redirect() should throw if children already exist")
    fun testRedirect_withChild() {
        val target: CommandNode<Any> = mock()
        val builder = testBuilder { literal("foo") }

        assertThrows(IllegalStateException::class.java) { builder.redirect(target) }
    }

    private fun testBuilder(block: TestableArgumentBuilder.() -> Unit): TestableArgumentBuilder {
        return TestableArgumentBuilder().apply(block)
    }

    /**
     * Test implementation of the abstract ArgumentBuilder.
     */
    private class TestableArgumentBuilder : ArgumentBuilder<Any, TestableArgumentBuilder>() {
        override val self: TestableArgumentBuilder
            get() = this

        override fun build(): CommandNode<Any> = mock()
    }
}

// Helper property to access 'command'
val <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.testCommand: Command<S>?
    get() = getProtectedField("command")

// Helper property to access 'redirect'
val <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.testRedirect: CommandNode<S>?
    get() = getProtectedField("redirect")

// Helper property to access 'redirect'
val <S, T : ArgumentBuilder<S, T>> ArgumentBuilder<S, T>.testRequirement: Predicate<S>
    get() = getProtectedField("requirement")

// Generic reflection helper
@Suppress("UNCHECKED_CAST")
private fun <R> Any.getProtectedField(fieldName: String): R {
    val field = this::class.java.superclass.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(this) as R
}