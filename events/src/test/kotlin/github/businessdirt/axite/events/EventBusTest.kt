package github.businessdirt.axite.events

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventBusTest {

    // Helper classes and objects for testing
    open class TestEvent1 : Event()
    class TestEvent2 : TestEvent1()
    class TestCancelableEvent : CancelableEvent() {
        // Overriding to make it visible for tests
        public override fun cancel() {
            super.cancel()
        }
    }

    companion object TestListeners {
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

    private fun clearEventBusState() {
        val listenersField = EventBus::class.java.getDeclaredField("listeners")
        listenersField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (listenersField.get(EventBus) as HashMap<KClass<out Event>, MutableList<EventListener>>).clear()

        val handlersField = EventBus::class.java.getDeclaredField("handlers")
        handlersField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (handlersField.get(EventBus) as MutableMap<KClass<out Event>, EventHandler>).clear()
    }
    
    @Test
    fun `posting a simple event calls listeners in correct priority order`() {
        clearEventBusState()
        eventLog.clear()
        
        // This relies on the KSP processor to find the methods in TestListeners
        // and for initialize() to register them.
        EventBus.initialize()
        
        TestEvent1().post()
        
        assertEquals(
            listOf("TestEvent1High", "TestEvent1Low"),
            eventLog,
            "Listeners should be called in priority order (HIGH then LOW)"
        )
    }

    @Test
    fun `posting a subclass event calls listeners for both subclass and superclass`() {
        clearEventBusState()
        eventLog.clear()
        EventBus.initialize()

        TestEvent2().post()

        // Order should be: TestEvent1High, TestEvent2, TestEvent1Low
        val expectedOrder = listOf("TestEvent1High", "TestEvent2", "TestEvent1Low")
        assertEquals(expectedOrder, eventLog, "Listeners for superclass and subclass should be called in correct order")
    }

    @Test
    fun `cancelled events are not propagated to listeners unless receiveCancelled is true`() {
        clearEventBusState()
        eventLog.clear()
        EventBus.initialize()

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
