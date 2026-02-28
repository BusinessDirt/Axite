package github.businessdirt.axite.api

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

abstract class BaseApiClient(override val baseUrl: String) : ApiService {

    override val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            url(baseUrl)
        }
    }

    suspend inline fun <reified T> safeRequest(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        crossinline block: HttpRequestBuilder.() -> Unit = {}
    ): NetworkResult<T> {
        return try {
            val response = client.request(path) {
                this.method = method
                block()
            }
            NetworkResult.Success(response.body<T>())
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Unknown Connection Error")
        }
    }
}