package github.businessdirt.axite.vanadium.core.profiling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProfilerTest {

    @Test
    fun `test marker path naming`() {
        Profiler.profile("Parent") {
            Profiler.profile("Child") {
                val stack = Profiler.markerStack.get()
                val currentMarker = stack.peek()
                
                assertEquals("Parent${Profiler.SEPARATOR}Child", currentMarker.name, "Marker name should contain the full path")
            }
        }
    }

    @Test
    fun `test deep marker path naming`() {
        Profiler.profile("Root") {
            Profiler.profile("Mid") {
                Profiler.profile("Leaf") {
                    val stack = Profiler.markerStack.get()
                    val currentMarker = stack.peek()

                    assertEquals("Root${Profiler.SEPARATOR}Mid${Profiler.SEPARATOR}Leaf", currentMarker.name, "Deep marker name should contain the full path")
                }
            }
        }
    }
}
