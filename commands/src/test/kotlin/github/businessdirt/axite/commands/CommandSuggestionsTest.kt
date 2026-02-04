package github.businessdirt.axite.commands

import github.businessdirt.axite.commands.arguments.IntegerArgumentType
import github.businessdirt.axite.commands.arguments.StringArgumentType
import github.businessdirt.axite.commands.builder.literal
import github.businessdirt.axite.commands.strings.StringRange
import github.businessdirt.axite.commands.strings.StringReader
import github.businessdirt.axite.commands.suggestions.StringSuggestion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandSuggestionsTest {
    private lateinit var subject: CommandDispatcher<Any>
    private lateinit var source: Any

    @BeforeEach
    fun setUp() {
        subject = CommandDispatcher()
        source = mock(Any::class.java)
    }

    private fun testSuggestions(contents: String, cursor: Int, range: StringRange, vararg suggestions: String) {
        val result = subject.getCompletionSuggestions(subject.parse(contents, source), cursor).join()
        assertEquals(range, result.range)

        val expected = suggestions.map { StringSuggestion(range, it) }
        assertEquals(expected, result.list)
    }

    private fun inputWithOffset(input: String, offset: Int): StringReader {
        val result = StringReader(input)
        result.cursor = offset
        return result
    }

    @Test
    fun getCompletionSuggestions_rootCommands() {
        subject.register(literal("foo"))
        subject.register(literal("bar"))
        subject.register(literal("baz"))

        val result = subject.getCompletionSuggestions(subject.parse("", source)).join()

        assertEquals(StringRange.at(0), result.range)
        assertEquals(
            listOf(
                StringSuggestion(StringRange.at(0), "bar"),
                StringSuggestion(StringRange.at(0), "baz"),
                StringSuggestion(StringRange.at(0), "foo")
            ),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_rootCommands_withInputOffset() {
        subject.register(literal("foo"))
        subject.register(literal("bar"))
        subject.register(literal("baz"))

        val result = subject.getCompletionSuggestions(subject.parse(inputWithOffset("OOO", 3), source)).join()

        assertEquals(StringRange.at(3), result.range)
        assertEquals(
            listOf(
                StringSuggestion(StringRange.at(3), "bar"),
                StringSuggestion(StringRange.at(3), "baz"),
                StringSuggestion(StringRange.at(3), "foo")
            ),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_rootCommands_partial() {
        subject.register(literal("foo"))
        subject.register(literal("bar"))
        subject.register(literal("baz"))

        val result = subject.getCompletionSuggestions(subject.parse("b", source)).join()

        assertEquals(StringRange.between(0, 1), result.range)
        assertEquals(
            listOf(
                StringSuggestion(StringRange.between(0, 1), "bar"),
                StringSuggestion(StringRange.between(0, 1), "baz")
            ),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_rootCommands_partial_withInputOffset() {
        subject.register(literal("foo"))
        subject.register(literal("bar"))
        subject.register(literal("baz"))

        val result = subject.getCompletionSuggestions(subject.parse(inputWithOffset("Zb", 1), source)).join()

        assertEquals(StringRange.between(1, 2), result.range)
        assertEquals(
            listOf(
                StringSuggestion(StringRange.between(1, 2), "bar"),
                StringSuggestion(StringRange.between(1, 2), "baz")
            ),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_subCommands() {
        subject.register(literal("parent") {
            literal("foo")
            literal("bar")
            literal("baz")
        })

        val result = subject.getCompletionSuggestions(subject.parse("parent ", source)).join()

        assertEquals(StringRange.at(7), result.range)
        assertEquals(
            listOf(
                StringSuggestion(StringRange.at(7), "bar"),
                StringSuggestion(StringRange.at(7), "baz"),
                StringSuggestion(StringRange.at(7), "foo")
            ),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_movingCursor_subCommands() {
        subject.register(literal("parent_one") {
            literal("faz")
            literal("fbz")
            literal("gaz")
        })

        subject.register(literal("parent_two"))

        testSuggestions("parent_one faz ", 0, StringRange.at(0), "parent_one", "parent_two")
        testSuggestions("parent_one faz ", 1, StringRange.between(0, 1), "parent_one", "parent_two")
        testSuggestions("parent_one faz ", 7, StringRange.between(0, 7), "parent_one", "parent_two")
        testSuggestions("parent_one faz ", 8, StringRange.between(0, 8), "parent_one")
        testSuggestions("parent_one faz ", 10, StringRange.at(0))
        testSuggestions("parent_one faz ", 11, StringRange.at(11), "faz", "fbz", "gaz")
        testSuggestions("parent_one faz ", 12, StringRange.between(11, 12), "faz", "fbz")
        testSuggestions("parent_one faz ", 13, StringRange.between(11, 13), "faz")
        testSuggestions("parent_one faz ", 14, StringRange.at(0))
        testSuggestions("parent_one faz ", 15, StringRange.at(0))
    }

    @Test
    fun getCompletionSuggestions_subCommands_partial() {
        subject.register(literal("parent") {
            literal("foo")
            literal("bar")
            literal("baz")
        })

        val parse = subject.parse("parent b", source)
        val result = subject.getCompletionSuggestions(parse).join()

        assertEquals(StringRange.between(7, 8), result.range)
        assertEquals(
            listOf(
                StringSuggestion(StringRange.between(7, 8), "bar"),
                StringSuggestion(StringRange.between(7, 8), "baz")
            ),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_subCommands_partial_withInputOffset() {
        subject.register(literal("parent") {
            literal("foo")
            literal("bar")
            literal("baz")
        })

        val parse = subject.parse(inputWithOffset("junk parent b", 5), source)
        val result = subject.getCompletionSuggestions(parse).join()

        assertEquals(StringRange.between(12, 13), result.range)
        assertEquals(
            listOf(
                StringSuggestion(StringRange.between(12, 13), "bar"),
                StringSuggestion(StringRange.between(12, 13), "baz")
            ),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_redirect() {
        val actual = subject.register(literal("actual") { literal("sub") })
        subject.register(literal("redirect") { redirect(actual) })

        val parse = subject.parse("redirect ", source)
        val result = subject.getCompletionSuggestions(parse).join()

        assertEquals(StringRange.at(9), result.range)
        assertEquals(
            listOf(StringSuggestion(StringRange.at(9), "sub")),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_redirectPartial() {
        val actual = subject.register(literal("actual") { literal("sub") })
        subject.register(literal("redirect") { redirect(actual) })

        val parse = subject.parse("redirect s", source)
        val result = subject.getCompletionSuggestions(parse).join()

        assertEquals(StringRange.between(9, 10), result.range)
        assertEquals(
            listOf(StringSuggestion(StringRange.between(9, 10), "sub")),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_movingCursor_redirect() {
        val actualOne = subject.register(literal("actual_one") {
            literal("faz")
            literal("fbz")
            literal("gaz")
        })

        subject.register(literal("actual_two"))
        subject.register(literal("redirect_one") { redirect(actualOne) })
        subject.register(literal("redirect_two") { redirect(actualOne) })

        testSuggestions("redirect_one faz ", 0, StringRange.at(0), "actual_one", "actual_two", "redirect_one", "redirect_two")
        testSuggestions("redirect_one faz ", 9, StringRange.between(0, 9), "redirect_one", "redirect_two")
        testSuggestions("redirect_one faz ", 10, StringRange.between(0, 10), "redirect_one")
        testSuggestions("redirect_one faz ", 12, StringRange.at(0))
        testSuggestions("redirect_one faz ", 13, StringRange.at(13), "faz", "fbz", "gaz")
        testSuggestions("redirect_one faz ", 14, StringRange.between(13, 14), "faz", "fbz")
        testSuggestions("redirect_one faz ", 15, StringRange.between(13, 15), "faz")
        testSuggestions("redirect_one faz ", 16, StringRange.at(0))
        testSuggestions("redirect_one faz ", 17, StringRange.at(0))
    }

    @Test
    fun getCompletionSuggestions_redirectPartial_withInputOffset() {
        val actual = subject.register(literal("actual") { literal("sub") })
        subject.register(literal("redirect") { redirect(actual) })

        val parse = subject.parse(inputWithOffset("/redirect s", 1), source)
        val result = subject.getCompletionSuggestions(parse).join()

        assertEquals(StringRange.between(10, 11), result.range)
        assertEquals(
            listOf(StringSuggestion(StringRange.between(10, 11), "sub")),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_redirect_lots() {
        val loop = subject.register(literal("redirect"))
        subject.register(literal("redirect") {
            literal("loop") {
                argument("loop", IntegerArgumentType()) {
                    redirect(loop)
                }
            }
        })

        val result = subject.getCompletionSuggestions(subject.parse("redirect loop 1 loop 02 loop 003 ", source)).join()

        assertEquals(StringRange.at(33), result.range)
        assertEquals(
            listOf(StringSuggestion(StringRange.at(33), "loop")),
            result.list
        )
    }

    @Test
    fun getCompletionSuggestions_execute_simulation() {
        val execute = subject.register(literal("execute"))
        subject.register(literal("execute") {
            literal("as") {
                argument("name", StringArgumentType.Word) { redirect(execute) }
            }
            literal("store") {
                argument("name", StringArgumentType.Word) { redirect(execute) }
            }

            literal("run") { executes { 0 } }
        })

        val parse = subject.parse("execute as Dinnerbone as", source)
        val result = subject.getCompletionSuggestions(parse).join()

        assertTrue(result.isEmpty)
    }

    @Test
    fun getCompletionSuggestions_execute_simulation_partial() {
        val execute = subject.register(literal("execute"))
        subject.register(literal("execute") {
            literal("as") {
                literal("bar") { redirect(execute) }
                literal("baz") { redirect(execute) }
            }
            literal("store") {
                argument("name", StringArgumentType.Word) { redirect(execute) }
            }
            literal("run") { executes { 0 } }
        })

        val parse = subject.parse("execute as bar as ", source)
        val result = subject.getCompletionSuggestions(parse).join()

        assertEquals(StringRange.at(18), result.range)
        assertEquals(
            listOf(
                StringSuggestion(StringRange.at(18), "bar"),
                StringSuggestion(StringRange.at(18), "baz")
            ),
            result.list
        )
    }
}