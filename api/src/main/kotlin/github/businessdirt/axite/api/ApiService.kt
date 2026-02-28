package github.businessdirt.axite.api

import io.ktor.client.HttpClient

interface ApiService {
    val baseUrl: String
    val client: HttpClient
}