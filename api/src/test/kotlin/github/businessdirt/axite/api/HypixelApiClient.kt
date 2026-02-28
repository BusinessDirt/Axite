package github.businessdirt.axite.api

import kotlinx.serialization.Serializable

@Serializable
data class SkyBlockItemsResponse(
    val success: Boolean,
    val lastUpdated: Long,
    val items: List<SkyBlockItem>
)

@Serializable
data class SkyBlockItem(
    val id: String,
    val name: String,
    val tier: String? = null,
    val category: String? = null,
    val npc_sell_price: Double? = null
)

class HypixelClient : BaseApiClient("https://api.hypixel.net/") {

    suspend fun getSkyBlockItems(): NetworkResult<SkyBlockItemsResponse> {
        return safeRequest<SkyBlockItemsResponse>("v2/resources/skyblock/items") {
            // Optional: You could add headers here if needed
            // header("API-Key", "your-api-key-here")
        }
    }
}