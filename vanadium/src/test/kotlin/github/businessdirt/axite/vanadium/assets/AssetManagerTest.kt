package github.businessdirt.axite.vanadium.assets

import github.businessdirt.axite.vanadium.assets.metadata.ModelMetadata
import github.businessdirt.axite.vanadium.assets.types.Asset
import github.businessdirt.axite.vanadium.assets.loaders.AssetSerializer
import github.businessdirt.axite.vanadium.assets.types.Model
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class TestAsset(path: String, uuid: String, metadata: ModelMetadata, var data: String, val onDispose: () -> Unit) : Asset<TestAsset>(uuid, path, metadata) {
    override var metadata: ModelMetadata = metadata

    override fun update(newAsset: TestAsset) {
        this.metadata = newAsset.metadata
        this.data = newAsset.data
    }

    override fun dispose() {
        onDispose()
    }
}

class TestSerializer(var currentAsset: TestAsset) : AssetSerializer<TestAsset, ModelMetadata>(ModelMetadata.serializer()) {
    override suspend fun load(path: String): TestAsset = currentAsset
}

class AssetManagerTest {

    @Test
    fun testReferenceCounting() = runBlocking {
        val scope = CoroutineScope(SupervisorJob())
        val manager = AssetManager(scope)
        
        var disposedCount = 0
        val metadata = ModelMetadata()
        val asset = TestAsset("test", metadata.uuid, metadata, "v1") { disposedCount++ }
        val serializer = TestSerializer(asset)
        
        manager.registerLoader<TestAsset>(serializer)
        
        // Load first time
        val loaded1 = manager.load<TestAsset>("test")
        assertSame(asset, loaded1)
        assertEquals(0, disposedCount)
        
        // Load second time
        val loaded2 = manager.load<TestAsset>("test")
        assertSame(asset, loaded2)
        assertEquals(0, disposedCount)
        
        // Unload once
        manager.unload("test")
        assertEquals(0, disposedCount)
        
        // Unload second time
        manager.unload("test")
        assertEquals(1, disposedCount)
    }

    @Test
    fun testHotReloading() = runBlocking {
        val scope = CoroutineScope(SupervisorJob())
        val manager = AssetManager(scope)
        
        val metadata = ModelMetadata()
        val asset = TestAsset("test", metadata.uuid, metadata, "v1") { }
        val serializer = TestSerializer(asset)
        
        manager.registerLoader<TestAsset>(serializer)
        
        val loaded = manager.load<TestAsset>("test")
        assertEquals("v1", loaded.data)
        
        // Simulate file change by updating serializer's returned asset
        val newAsset = TestAsset("test", metadata.uuid, metadata, "v2") { }
        serializer.currentAsset = newAsset
        
        // Trigger reload
        manager.reload("test").join()
        
        // Verify loaded asset (which is the SAME instance) now has updated data
        assertEquals("v2", loaded.data)
        assertSame(asset, loaded) // It should be the same object instance
    }

    @Test
    fun testHotReloadingFailureKeepsOldVersion() = runBlocking {
        val scope = CoroutineScope(SupervisorJob())
        val manager = AssetManager(scope)
        
        val metadata = ModelMetadata()
        val asset = TestAsset("test", metadata.uuid, metadata, "v1") { }
        
        val failingSerializer = object : AssetSerializer<TestAsset, ModelMetadata>(ModelMetadata.serializer()) {
            var shouldFail = false
            override suspend fun load(path: String): TestAsset {
                if (shouldFail) throw RuntimeException("Simulated load failure")
                return asset
            }
        }
        
        manager.registerLoader<TestAsset>(failingSerializer)
        
        val loaded = manager.load<TestAsset>("test")
        assertEquals("v1", loaded.data)
        
        // Enable failure
        failingSerializer.shouldFail = true
        
        // Trigger reload
        manager.reload("test").join()
        
        // Verify loaded asset still has old data
        assertEquals("v1", loaded.data)
    }
}
