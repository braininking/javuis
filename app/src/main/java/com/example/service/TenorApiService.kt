package com.example.service

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface TenorApiService {
  @GET("v2/search")
  suspend fun buscarGifs(
    @Query("q") termoBusca: String,
    @Query("key") apiKey: String,
    @Query("client_key") clientKey: String = "ares_keyboard_app",
    @Query("limit") limite: Int = 15,
    @Query("media_filter") mediaFilter: String = "tinygif"
  ): TenorResponse
}

data class TenorResponse(
  @SerializedName("results") val results: List<GifObject> = emptyList()
)

data class GifObject(
  @SerializedName("media_formats") val mediaFormats: MediaFormats?
)

data class MediaFormats(
  @SerializedName("tinygif") val tinygif: GifDetails?,
  @SerializedName("gif") val gif: GifDetails?
)

data class GifDetails(
  @SerializedName("url") val url: String?
)
