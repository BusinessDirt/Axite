package github.businessdirt.axite.events

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Event Bus Tests")
class EventBusTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            EventBus.initialize()
        }
    }

    @BeforeEach
    fun setup() {
        TestListeners.eventLog.clear()
    }

    @Test
    @DisplayName("Should call listeners in correct priority order")
    fun testPriority() {
        TestEvent1().post()
        
        assertEquals(
            listOf("TestEvent1High", "TestEvent1Low"),
            TestListeners.eventLog,
            "Listeners should be called in priority order (HIGH then LOW)"
        )
    }

    @Test
    @DisplayName("Should call listeners for both subclass and superclass")
    fun testInheritance() {
        TestEvent2().post()

        // Order should be: TestEvent1High, TestEvent2, TestEvent1Low
        // TestEvent1 listeners are called because TestEvent2 extends TestEvent1.
        // Priority handles order. TestEvent1High (-1) < TestEvent2 (0) < TestEvent1Low (1)
        val expectedOrder = listOf("TestEvent1High", "TestEvent2", "TestEvent1Low")
        assertEquals(expectedOrder, TestListeners.eventLog, "Listeners for superclass and subclass should be called in correct order")
    }

    @Test
    @DisplayName("Should handle cancelled events correctly")
    fun testCancellation() {
        val event = TestCancelableEvent()
        event.post()
        
        assertTrue(event.isCancelled, "Event should be cancelled")
        assertEquals(
            listOf("onCancellable", "onCancellableCalled"), 
            TestListeners.eventLog,
            "Only listeners with receiveCancelled=true or those before cancellation should be called"
        )
    }

    @Test
    @DisplayName("Should handle exceptions in listeners")
    fun testExceptionHandling() {
        var exceptionCaught: Throwable? = null
        ExceptionEvent().post { error ->
            exceptionCaught = error
        }

        assertNotNull(exceptionCaught, "Exception should be caught via onError callback")
        assertTrue(exceptionCaught is RuntimeException, "Exception should be RuntimeException")
        assertEquals("Intentional Exception", exceptionCaught?.message)
    }
}

