package github.businessdirt.axite.api

import kotlinx.coroutines.runBlocking
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.DefaultAsserter.fail
import kotlin.test.Test
import kotlin.test.assertNotNull

class HypixelApiClientTest {

    private val api = HypixelClient()

    @Test
    fun `test fetch skyblock items returns success and data`(): Unit = runBlocking {
        when (val result = api.getSkyBlockItems()) {
            is NetworkResult.Success -> {
                val data = result.data
                assertTrue("API should report success", data.success)
                assertTrue("Items list should not be empty", data.items.isNotEmpty())

                // Spot check a common item (like SkyBlock Menu or similar)
                val firstItem = data.items.first()
                println("Successfully fetched ${data.items.size} items. First item: ${firstItem.name}")
                assertNotNull(firstItem.id)
            }
            is NetworkResult.Error -> {
                fail("API call failed with message: ${result.message}")
            }
            else -> fail("Result was not Success or Error")
        }
    }
}