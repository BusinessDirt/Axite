package github.businessdirt.axite.vanadium.core.profiling

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.MarkerManager
import java.util.*

object Profiler {
    val logger: Logger = LogManager.getLogger(Profiler::class.java)
    const val SEPARATOR: String = " > "

    /**
     * A thread-local stack to keep track of active markers for nested profiling.
     */
    @PublishedApi
    internal val markerStack = ThreadLocal.withInitial { ArrayDeque<Marker>() }

    inline fun <T> profile(name: String, block: () -> T): T {
        val stack = markerStack.get()
        val parent = stack.peek()

        // Create a full path name for the marker (e.g., "Parent/Child")
        val fullName = if (parent != null) "${parent.name}$SEPARATOR$name" else name
        val currentMarker = MarkerManager.getMarker(fullName)

        // We don't add parents via Log4j2's addParents() to avoid the default 
        // "Child[Parent]" formatting in logs, as the hierarchy is now in the name.
        
        stack.push(currentMarker)

        val start = System.nanoTime()
        try {
            return block()
        } finally {
            stack.pop()
            val end = System.nanoTime()
            val durationMs = (end - start) / 1_000_000.0
            logger.atInfo().withMarker(currentMarker).log("Took ${"%.3f".format(durationMs)}ms")
        }
    }
}
