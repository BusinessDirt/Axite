package github.businessdirt.axite.events

import org.junit.jupiter.api.BeforeAll
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventBusTest {

    // Helper classes and objects for testing
    open class TestEvent1 : Event()
    class TestEvent2 : TestEvent1()
    class TestCancelableEvent : CancelableEvent() {}

    companion object TestListeners {
        @JvmStatic
        @BeforeAll
        fun beforeAll(): Unit {
            EventBus.initialize()
        }

        val eventLog = mutableListOf<String>()

        @HandleEvent(priority = 1) // LOW
        fun onTestEvent1Low(event: TestEvent1) {
            eventLog.add("TestEvent1Low")
        }

        @HandleEvent(priority = -1) // HIGH
        fun onTestEvent1High(event: TestEvent1) {
            eventLog.add("TestEvent1High")
        }

        @HandleEvent(priority = 0) // MEDIUM
        fun onTestEvent2(event: TestEvent2) {
            eventLog.add("TestEvent2")
        }

        @HandleEvent(priority = -2) // HIGHEST
        fun onCancellable(event: TestCancelableEvent) {
            eventLog.add("onCancellable")
            event.cancel()
        }

        @HandleEvent(priority = 0, receiveCancelled = false) // MEDIUM
        fun onCancellableNotCalled(event: TestCancelableEvent) {
            eventLog.add("onCancellableNotCalled")
        }

        @HandleEvent(priority = 2, receiveCancelled = true) // LOWEST
        fun onCancellableCalled(event: TestCancelableEvent) {
            eventLog.add("onCancellableCalled")
        }

        @HandleEvent
        fun onTestEventNoParams(eventType: TestEvent1) {
            // This is just to have another listener
        }
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
