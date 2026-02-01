package github.businessdirt.axite.events

open class TestEvent1 : Event()
class TestEvent2 : TestEvent1()
class TestCancelableEvent : CancelableEvent()
class ExceptionEvent : Event()
