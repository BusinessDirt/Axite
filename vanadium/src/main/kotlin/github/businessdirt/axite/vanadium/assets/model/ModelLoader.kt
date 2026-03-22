package github.businessdirt.axite.vanadium.assets.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object ModelLoader {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonFormat = Json {
        ignoreUnknownKeys = true // Prevents crashes if Assimp adds new fields we don't care about
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    fun loadMaterials(path: String): List<MaterialData> {
        logger.debug("Loading materials from [{}]", path)
        return try {
            val content = File(path).readText()

            // Look how clean this is compared to GSON's Array mapping!
            jsonFormat.decodeFromString<List<MaterialData>>(content)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load materials from: $path", e)
        }
    }

    fun loadModel(path: String): ModelData {
        logger.debug("Loading model from [{}]", path)
        return try {
            val content = File(path).readText()
            jsonFormat.decodeFromString<ModelData>(content)
        } catch (e: Exception) {
            throw RuntimeException("Failed to load model from: $path", e)
        }
    }
}