package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.model.MeshData
import github.businessdirt.axite.vanadium.assets.model.ModelData


class VanadiumSandbox : VanadiumAdapter {

    override fun configure(config: VanadiumConfig) {
        config.applicationName = "Sandbox"
    }

    override fun initialize(): InitData {
        val modelId = "TriangleModel"

        val meshData = MeshData(
            "triangle-mesh", floatArrayOf(
                -0.5f, -0.5f, 0.0f,
                0.0f, 0.5f, 0.0f,
                0.5f, -0.5f, 0.0f
            ),
            intArrayOf(0, 1, 2)
        )

        val meshDataList: MutableList<MeshData> = mutableListOf(meshData)
        val modelData = ModelData(modelId, meshDataList)

        val models: MutableList<ModelData> = mutableListOf(modelData)
        return InitData(models)
    }

    override fun update(deltaTime: Long) {}
    override fun shutdown() {}
}