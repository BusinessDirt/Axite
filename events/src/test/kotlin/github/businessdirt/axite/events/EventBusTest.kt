package github.businessdirt.axite.events

import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventBusTest {

    // Helper classes and objects for testing
    open class TestEvent1 : Event()
    class TestEvent2 : TestEvent1()
    class TestCancelableEvent : CancelableEvent()

    companion object TestListeners {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            EventBus.initialize()
        }

        val eventLog = mutableListOf<String>()

        @HandleEvent(eventType = TestEvent1::class, priority = HandleEvent.LOW)
        fun onTestEvent1Low() {
            eventLog.add("TestEvent1Low")
        }

        @HandleEvent(eventType = TestEvent1::class, priority = HandleEvent.HIGH)
        fun onTestEvent1High() {
            eventLog.add("TestEvent1High")
        }

        @HandleEvent(eventType = TestEvent2::class, priority = HandleEvent.MEDIUM)
        fun onTestEvent2() {
            eventLog.add("TestEvent2")
        }

        @HandleEvent(priority = HandleEvent.HIGHEST)
        fun onCancellable(event: TestCancelableEvent) {
            eventLog.add("onCancellable")
            event.cancel()
        }

        @HandleEvent(eventType = TestCancelableEvent::class, priority = HandleEvent.MEDIUM, receiveCancelled = false)
        fun onCancellableNotCalled() {
            eventLog.add("onCancellableNotCalled")
        }

        @HandleEvent(eventType = TestCancelableEvent::class, priority = HandleEvent.LOWEST, receiveCancelled = true) // LOWEST
        fun onCancellableCalled() {
            eventLog.add("onCancellableCalled")
        }

        @HandleEvent(eventType = TestEvent1::class)
        fun onTestEventNoParams() { }
    }

    @Test
    fun `posting a simple event calls listeners in correct priority order`() {
        eventLog.clear()
        
        TestEvent1().post()
        
        assertEquals(
            listOf("TestEvent1High", "TestEvent1Low"),
            eventLog,
            "Listeners should be called in priority order (HIGH then LOW)"
        )
    }

    @Test
    fun `posting a subclass event calls listeners for both subclass and superclass`() {
        eventLog.clear()

        TestEvent2().post()

        // Order should be: TestEvent1High, TestEvent2, TestEvent1Low
        val expectedOrder = listOf("TestEvent1High", "TestEvent2", "TestEvent1Low")
        assertEquals(expectedOrder, eventLog, "Listeners for superclass and subclass should be called in correct order")
    }

    @Test
    fun `cancelled events are not propagated to listeners unless receiveCancelled is true`() {
        eventLog.clear()

        val event = TestCancelableEvent()
        event.post()
        
        assertTrue(event.isCancelled, "Event should be cancelled")
        assertEquals(
            listOf("onCancellable", "onCancellableCalled"), 
            eventLog,
            "Only listeners with receiveCancelled=true or those before cancellation should be called"
        )
    }
}
