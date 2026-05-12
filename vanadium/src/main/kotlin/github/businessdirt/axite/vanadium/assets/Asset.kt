package github.businessdirt.axite.vanadium.assets

interface Asset : AutoCloseable {
    fun release()
    override fun close() = release()
}

interface AssetLoader<T : Asset> {
    suspend fun load(path: String): T
}