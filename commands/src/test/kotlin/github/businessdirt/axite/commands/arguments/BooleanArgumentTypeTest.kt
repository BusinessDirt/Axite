package github.businessdirt.axite.commands.arguments

import github.businessdirt.axite.commands.strings.StringReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("BooleanArgumentType logic tests")
class BooleanArgumentTypeTest {

    @Test
    @DisplayName("parse() should delegate to StringReader.readBoolean()")
    fun parse() {
        val reader: StringReader = mock()
        whenever(reader.readBoolean()).thenReturn(true)

        val result = BooleanArgumentType().parse(reader)

        assertEquals(true, result)
        verify(reader).readBoolean()
    }
}