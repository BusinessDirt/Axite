package github.businessdirt.axite.vanadium.assets

import github.businessdirt.axite.vanadium.assets.loaders.AssetSerializer
import github.businessdirt.axite.vanadium.assets.metadata.AssetMetadata
import github.businessdirt.axite.vanadium.assets.types.Asset
import github.businessdirt.axite.vanadium.core.profiling.Profiler
import kotlinx.coroutines.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class AssetManager(private val scope: CoroutineScope) {

    private val logger: Logger = LogManager.getLogger(AssetManager::class.java)

    private val cache = ConcurrentHashMap<String, Asset<*>>()
    private val loadingJobs = ConcurrentHashMap<String, Deferred<Asset<*>>>()

    val serializers = ConcurrentHashMap<KClass<out Asset<*>>, AssetSerializer<out Asset<*>, out AssetMetadata>>()

    // Hot Reloading
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val watchKeys = ConcurrentHashMap<WatchKey, Path>()
    private val assetPathsByFile = ConcurrentHashMap<Path, MutableSet<String>>()

    init {
        startWatcher()
    }

    private fun startWatcher() = scope.launch(Dispatchers.IO) {
        try {
            while (isActive) {
                val key = watchService.take() ?: continue
                val dir = watchKeys[key] ?: continue

                for (event in key.pollEvents()) {
                    val context = event.context() as? Path ?: continue
                    val fullPath = dir.resolve(context).toAbsolutePath()
                    
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        val assetPaths = assetPathsByFile[fullPath] ?: continue
                        for (assetPath in assetPaths) {
                            logger.info("Detected change in asset file: [{}]. Reloading...", assetPath)
                            reload(assetPath)
                        }
                    }
                }
                key.reset()
            }
        } catch (_: ClosedWatchServiceException) {
            // normal shutdown
        } catch (e: Exception) {
            logger.error("Asset watcher error: {}", e.message)
        }
    }

    internal fun reload(path: String) = scope.launch(Dispatchers.IO) {
        val asset = cache[path] ?: return@launch
        val loader = serializers[asset::class] ?: return@launch

        try {
            val loaded = loader.load(path)
            performUpdate(asset, loaded)
            logger.info("Successfully hot-reloaded asset: [{}]", path)
        } catch (e: Exception) {
            logger.error("Failed to hot-reload asset [{}]: {}", path, e.message ?: "Unknown error")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Asset<T>> performUpdate(current: Asset<*>, loaded: Asset<*>) {
        val target = current as Asset<T>
        val source = loaded as T
        target.update(source)
    }

    private fun registerWatcher(path: String) = try {
        val file = File(path).absoluteFile
        val dir = file.parentFile.toPath()
        val filePath = file.toPath().toAbsolutePath()

        assetPathsByFile.getOrPut(filePath) { ConcurrentHashMap.newKeySet() }.add(path)

        if (watchKeys.values.none { it == dir }) {
            val key = dir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
            watchKeys[key] = dir
        } else {}
    } catch (e: Exception) {
        logger.warn("Failed to register watcher for [{}]: {}", path, e.message)
    }

    fun configure(block: AssetManager.() -> Unit): AssetManager = Profiler.profile("AssetManager Configuration") {
        this.apply { block.invoke(this) }
    }

    /**
     * Pre-register how to load a specific type of asset.
     */
    inline fun <reified T : Asset<T>> registerLoader(loader: AssetSerializer<out Asset<T>, out AssetMetadata>) {
        serializers[T::class] = loader
    }

    /**
     * Loads an asset using the registered loader for type T.
     */
    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified T : Asset<T>> load(path: String): T {
        val clazz = T::class
        val loader = serializers[clazz] ?: throw IllegalStateException("No loader registered for ${clazz.simpleName}")

        return loadWithLoader(path, loader) as T
    }

    /**
     * Internal logic to handle deduplication and async execution.
     */
    @Suppress("UNCHECKED_CAST", "DeferredResultUnused")
    suspend fun <T : Asset<T>> loadWithLoader(path: String, loader: AssetSerializer<out Asset<T>, out AssetMetadata>): T {
        // Return from cache if available
        cache[path]?.let {
            it.retain()
            return it as T
        }

        var wasCreated = false
        // Return the existing job if it's already loading
        val job = loadingJobs.getOrPut(path) {
            wasCreated = true
            scope.async(Dispatchers.IO) {
                try {
                    val asset = loader.load(path)
                    cache[path] = asset
                    registerWatcher(path)
                    asset
                } finally {
                    loadingJobs.remove(path)
                }
            }
        }

        val asset = job.await() as T
        if (!wasCreated) asset.retain()
        return asset
    }

    @Suppress("unused")
    fun unload(path: String) {
        cache[path]?.let { asset ->
            if (asset.release()) {
                cache.remove(path)
                // We don't easily unregister from WatchService, but it's fine for now.
            }
        }
    }
}
