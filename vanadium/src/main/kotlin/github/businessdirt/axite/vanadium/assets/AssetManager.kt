package github.businessdirt.axite.vanadium.assets

import github.businessdirt.axite.vanadium.core.profiling.Profiler
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class AssetManager(private val scope: CoroutineScope) {

    private val cache = ConcurrentHashMap<String, Asset>()
    private val loadingJobs = ConcurrentHashMap<String, Deferred<Asset>>()

    val loaders = ConcurrentHashMap<KClass<out Asset>, AssetLoader<out Asset>>()

    fun configure(block: AssetManager.() -> Unit): AssetManager = Profiler.profile("AssetManager Configuration") {
        this.apply { block.invoke(this) }
    }

    /**
     * Pre-register how to load a specific type of asset.
     */
    inline fun <reified T : Asset> registerLoader(loader: AssetLoader<out Asset>) {
        loaders[T::class] = loader
    }

    /**
     * Loads an asset using the registered loader for type T.
     */
    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T : Asset> load(path: String): T {
        val clazz = T::class
        val loader = loaders[clazz] ?: throw IllegalStateException("No loader registered for ${clazz.simpleName}")

        return loadWithLoader(path, loader) as T
    }

    /**
     * Internal logic to handle deduplication and async execution.
     */
    @Suppress("UNCHECKED_CAST", "DeferredResultUnused")
    suspend fun <T : Asset> loadWithLoader(path: String, loader: AssetLoader<out Asset>): T {
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