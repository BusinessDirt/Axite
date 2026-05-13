package github.businessdirt.axite.vanadium.assets

import github.businessdirt.axite.vanadium.assets.metadata.ModelMetadata
import github.businessdirt.axite.vanadium.assets.types.Asset
import github.businessdirt.axite.vanadium.assets.loaders.AssetSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*

class TestAsset(path: String, uuid: String, metadata: ModelMetadata, val onDispose: () -> Unit) : Asset(uuid, path, metadata) {
    override fun dispose() {
        onDispose()
    }
}

class TestSerializer(val asset: TestAsset) : AssetSerializer<TestAsset, ModelMetadata>(ModelMetadata.serializer()) {
    override suspend fun load(path: String): TestAsset = asset
}

class AssetManagerTest {

    @Test
    fun testReferenceCounting() = runBlocking {
        val scope = CoroutineScope(SupervisorJob())
        val manager = AssetManager(scope)
        
        var disposedCount = 0
        val metadata = ModelMetadata()
        val asset = TestAsset("test", metadata.uuid, metadata) { disposedCount++ }
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
}
