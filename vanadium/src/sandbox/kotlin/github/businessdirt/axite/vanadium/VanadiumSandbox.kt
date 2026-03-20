package github.businessdirt.axite.vanadium

import github.businessdirt.axite.vanadium.assets.model.MeshData
import github.businessdirt.axite.vanadium.assets.model.ModelData
import github.businessdirt.axite.vanadium.scene.Entity
import org.joml.Vector3f


class VanadiumSandbox : VanadiumAdapter {

    private val rotatingAngle = Vector3f(1f, 1f, 1f)
    private var angle = 0f
    private lateinit var cubeEntity: Entity

    override fun configure(config: VanadiumConfig) {
        config.applicationName = "Sandbox"
    }

    override fun initialize(): InitData {
        val positions = floatArrayOf(
            -0.5f, 0.5f, 0.5f,
            -0.5f, -0.5f, 0.5f,
            0.5f, -0.5f, 0.5f,
            0.5f, 0.5f, 0.5f,
            -0.5f, 0.5f, -0.5f,
            0.5f, 0.5f, -0.5f,
            -0.5f, -0.5f, -0.5f,
            0.5f, -0.5f, -0.5f,
        )
        val textCoords = floatArrayOf(
            0.0f, 0.0f,
            0.5f, 0.0f,
            1.0f, 0.0f,
            1.0f, 0.5f,
            1.0f, 1.0f,
            0.5f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.5f,
        )
        val indices = intArrayOf(
            // Front face
            0, 1, 3, 3, 1, 2,  // Top Face
            4, 0, 3, 5, 4, 3,  // Right face
            3, 2, 7, 5, 3, 7,  // Left face
            6, 1, 0, 6, 0, 4,  // Bottom face
            2, 1, 6, 2, 6, 7,  // Back face
            7, 6, 4, 7, 4, 5,
        )

        val modelId = "CubeModel"
        val meshData = MeshData("cube-mesh", positions, textCoords, indices)
        val meshDataList: MutableList<MeshData> = mutableListOf(meshData)
        val modelData = ModelData(modelId, meshDataList)
        val models: MutableList<ModelData> = mutableListOf(modelData)

        cubeEntity = Entity("CubeEntity", modelId, Vector3f(0.0f, 0.0f, -2.0f))
        Vanadium.scene.addEntity(cubeEntity)

        return InitData(models)
    }

    override fun update(deltaTime: Long) {
        angle += 1.0f
        if (angle >= 360) angle -= 360
        cubeEntity.rotation.identity().rotateAxis(Math.toRadians(angle.toDouble()).toFloat(), rotatingAngle)
        cubeEntity.updateModelMatrix()
    }

    override fun shutdown() {}
}