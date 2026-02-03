package github.businessdirt.axite.commands.builder

import github.businessdirt.axite.commands.arguments.IntegerArgumentType
import github.businessdirt.axite.commands.nodes.CommandNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.mockito.Mockito.mock
import kotlin.test.Test

@DisplayName("ArgumentBuilder logic tests")
class ArgumentBuilderTest {
    private lateinit var builder: TestableArgumentBuilder<Any>

    @BeforeEach
    fun setUp() {
        builder = TestableArgumentBuilder()
    }

    @Test
    @DisplayName("then() should successfully add a child argument")
    fun testArguments() {
        val argument = RequiredArgumentBuilder.argument<Any, Int>("bar", IntegerArgumentType())
        builder.then(argument)

        val builtArgument = argument.build()
        assertEquals(1, builder.allArguments.size)
        assertTrue(builder.allArguments.contains(builtArgument), "Builder should contain the added argument")
    }

    @Test
    @DisplayName("redirect() should correctly set the target node")
    fun testRedirect() {
        val target: CommandNode<Any> = mock()
        builder.redirect(target)
        assertEquals(target, builder.redirect)
    }

    @Test
    @DisplayName("redirect() should throw if children already exist")
    fun testRedirect_withChild() {
        val target: CommandNode<Any> = mock()
        builder.then(LiteralArgumentBuilder.literal("foo"))

        assertThrows(IllegalStateException::class.java) {
            builder.redirect(target)
        }
    }

    @Test
    @DisplayName("then() should throw if a redirect is already set")
    fun testThen_withRedirect() {
        val target: CommandNode<Any> = mock()
        builder.redirect(target)

        assertThrows(IllegalStateException::class.java) {
            builder.then(LiteralArgumentBuilder.literal("foo"))
        }
    }

    /**
     * Test implementation of the abstract ArgumentBuilder.
     */
    private class TestableArgumentBuilder<S> : ArgumentBuilder<S, TestableArgumentBuilder<S>>() {
        override val `this`: TestableArgumentBuilder<S>
            get() = this

        override fun build(): CommandNode<S> = mock()
    }
}