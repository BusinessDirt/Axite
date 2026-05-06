package github.businessdirt.axite.vanadium.vulkan.device

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class PhysicalDeviceSelectorTest {

    @Test
    fun `test requirement discovery and evaluation`() {
        val mockDevice = mock(PhysicalDevice::class.java)

        val provider = object {
            @PhysicalDeviceRequirement(mandatory = true)
            fun mandatoryTrue(device: PhysicalDevice) = true

            @PhysicalDeviceRequirement(weight = 100)
            fun weightedBoolean(device: PhysicalDevice) = true

            @PhysicalDeviceRequirement(weight = 10)
            fun weightedNumber(device: PhysicalDevice) = 5
        }

        PhysicalDeviceSelector.registerProvider(provider)
        val requirements = PhysicalDeviceSelector.getRequirements()

        val req1 = requirements.find { it.name == "mandatoryTrue" }!!
        val req2 = requirements.find { it.name == "weightedBoolean" }!!
        val req3 = requirements.find { it.name == "weightedNumber" }!!

        // Test discovery
        assertTrue(req1.mandatory)
        assertEquals(100, req2.weight)
        assertEquals(10, req3.weight)

        // Test evaluation logic (mimicking pickPhysicalDevice)
        assertEquals(true, req1.check(mockDevice))
        assertEquals(true, req2.check(mockDevice))
        assertEquals(5, req3.check(mockDevice))

        // Test scoring logic (mimicking pickPhysicalDevice)
        val score2 = if (req2.check(mockDevice) as Boolean) req2.weight else 0
        assertEquals(100, score2)

        val score3 = (req3.check(mockDevice) as Int) * req3.weight
        assertEquals(50, score3)
    }
}
