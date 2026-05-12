package github.businessdirt.axite.vanadium.assets

import github.businessdirt.axite.vanadium.assets.loaders.AssetSerializer
import github.businessdirt.axite.vanadium.assets.types.Asset
import github.businessdirt.axite.vanadium.assets.metadata.AssetMetadata
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class AssetManager(private val scope: CoroutineScope) {

    private val cache = ConcurrentHashMap<String, Asset>()
    private val loadingJobs = ConcurrentHashMap<String, Deferred<Asset>>()

    val serializers = ConcurrentHashMap<KClass<out Asset>, AssetSerializer<out Asset, out AssetMetadata>>()

    fun configure(block: AssetManager.() -> Unit): AssetManager = Profiler.profile("AssetManager Configuration") {
        this.apply { block.invoke(this) }
    }

    /**
     * Pre-register how to load a specific type of asset.
     */
    inline fun <reified T : Asset> registerLoader(loader: AssetSerializer<out Asset, out AssetMetadata>) {
        serializers[T::class] = loader
    }

    /**
     * Loads an asset using the registered loader for type T.
     */
    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T : Asset> load(path: String): T {
        val clazz = T::class
        val loader = serializers[clazz] ?: throw IllegalStateException("No loader registered for ${clazz.simpleName}")

        return loadWithLoader(path, loader) as T
    }

    /**
     * Loads only the metadata for an asset of type T.
     */
    inline fun <reified T : Asset> loadMetadata(path: String): AssetMetadata? {
        val loader = serializers[T::class] ?: return null
        return loader.loadMetadata(path)
    }

    /**
     * Internal logic to handle deduplication and async execution.
     */
    @Suppress("UNCHECKED_CAST", "DeferredResultUnused")
    suspend fun <T : Asset> loadWithLoader(path: String, loader: AssetSerializer<out Asset, out AssetMetadata>): T {
        // Return from cache if available
        cache[path]?.let { return it as T }

        // Return the existing job if it's already loading
        val job = loadingJobs.getOrPut(path) {
            scope.async(Dispatchers.IO) {
                try {
                    val asset = loader.load(path)
                    cache[path] = asset
                    asset
                } finally {
                    loadingJobs.remove(path)
                }
            }
        }

        return job.await() as T
    }

    fun unload(path: String) = cache.remove(path)?.release()
}