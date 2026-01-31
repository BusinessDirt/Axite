package github.businessdirt.axite.events

import java.util.ServiceLoader
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

/**
 * The main class for the event system.
 *
 * The EventBus discovers event listeners in a given package and registers them.
 * It is responsible for posting events to the appropriate listeners.
 */
object EventBus {
    private val listeners: HashMap<KClass<out Event>, MutableList<EventListener>> = HashMap()
    private val handlers: MutableMap<KClass<out Event>, EventHandler> = HashMap()

    fun initialize() {
        val loader = ServiceLoader.load(EventRegistryProvider::class.java)
        loader.forEach { registry ->
            registry.methods.forEach { function ->
                function.isAccessible = true
                if (function.visibility != KVisibility.PUBLIC) throw MethodNotPublicException(function)

                val name: String = ReflectionUtils.getFunctionString(function)
                val instance: Any = this.getInstance(function) // throws ClassNotInstantiableException
                val eventData = this.getEventData(function) // throws ParameterException
                val eventConsumer = this.getEventConsumer(function, instance) // throws ParameterException
                listeners.computeIfAbsent(eventData.second) { `_`: KClass<out Event> -> ArrayList() }
                    .add(EventListener(name, eventConsumer, eventData.first))
            }
        }
    }

    @Throws(ClassNotInstantiableException::class)
    private fun getInstance(
        function: KFunction<*>
    ): Any {
        val declaringClass = function.javaMethod?.declaringClass?.kotlin
            ?: throw ClassNotInstantiableException(function)

        // objectInstance returns the singleton instance if the class is a Kotlin 'object'
        return declaringClass.objectInstance
            ?: throw ClassNotInstantiableException(function)
    }

    @Throws(ParameterException::class)
    private fun getEventData(
        function: KFunction<*>
    ): Pair<HandleEvent, KClass<out Event>> {
        val options: HandleEvent = checkNotNull(function.findAnnotation<HandleEvent>())

        return when (function.parameters.size) {
            0 -> options to options.eventType
            1 -> {
                val paramType: KType = function.parameters[0].type
                val eventClass = paramType.classifier as? KClass<*>
                    ?: throw ParameterException(function, "parameter must be a class")

                if (!Event::class.isSuperclassOf(eventClass)) throw ParameterException(
                    function, "must be a subtype of " + Event::class.java.name
                )

                @Suppress("UNCHECKED_CAST")
                options to (eventClass as KClass<out Event>)
            }

            else -> throw ParameterException(function, "must have either 0 or 1 parameters")
        }
    }

    @Throws(ParameterException::class)
    private fun getEventConsumer(
        function: KFunction<*>,
        instance: Any
    ): Consumer<Event> {
        return when (function.parameters.size) {
            0 -> ReflectionUtils.createZeroParameterEventConsumer(instance, function)
            1 -> ReflectionUtils.createSingleParameterEventConsumer(instance, function)
            else -> throw ParameterException(function, "must have either 0 or 1 parameters")
        }
    }

    /**
     * Gets the [EventHandler] for the given event class.
     * If an event handler does not exist for the given event, a new one is created.
     *
     * @param event the event class.
     * @return the [EventHandler] for the given event class.
     */
    fun getEventHandler(
        event: KClass<out Event>
    ): EventHandler = handlers.getOrPut(event) {
        EventHandler(
            event,
            getEventClasses(event).mapNotNull { listeners[it] }.flatten().toMutableList()
        )
    }

    private fun getEventClasses(
        clazz: KClass<out Event>
    ): MutableList<KClass<out Event>> {
        val classes = mutableListOf<KClass<out Event>>()
        classes.add(clazz)

        var current: Class<*> = clazz.java
        @Suppress("LoopWithTooManyJumpStatements")
        while (current.superclass != null) {
            val superClass = current.superclass
            if (superClass == Event::class.java) break
            if (superClass == CancelableEvent::class.java) break
            
            @Suppress("UNCHECKED_CAST")
            classes.add(superClass.kotlin as KClass<out Event>)
            current = superClass
        }

        return classes
    }
}