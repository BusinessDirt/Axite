package github.businessdirt.axite.events

object TestListeners {
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

    @HandleEvent(eventType = TestCancelableEvent::class, priority = HandleEvent.LOWEST, receiveCancelled = true)
    fun onCancellableCalled() {
        eventLog.add("onCancellableCalled")
    }

    @HandleEvent(eventType = TestEvent1::class)
    fun onTestEventNoParams() {
        // Just to test 0-param signature, doesn't log to avoid messing up other tests order unless needed
    }
    
    @HandleEvent(eventType = ExceptionEvent::class)
    fun onExceptionEvent(event: ExceptionEvent) {
        throw RuntimeException("Intentional Exception")
    }
}
