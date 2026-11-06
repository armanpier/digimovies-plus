package com.digimovie.tv.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// --- Models ---

data class QrLoginResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("token") val token: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("qr_image") val qrImage: String?,
    @SerializedName("expires_in") val expiresIn: Int?
)

data class CheckQrResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("login_status") val loginStatus: String?,
    @SerializedName("msg") val msg: String?,
    @SerializedName("auth_token") val authToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("user_info") val userInfo: UserInfo?
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("msg") val msg: String?,
    @SerializedName("auth_token") val authToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("user_info") val userInfo: UserInfo?
)

data class UserInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("user_login") val userLogin: String?,
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("user_email") val userEmail: String?,
    @SerializedName("has_subscription") val hasSubscription: Boolean?
)

data class ShowcaseSection(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("items") val items: List<MovieItem>?
)

data class MovieItem(
    @SerializedName("id") val id: Int?,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("title_en") val titleEn: String?,
    @SerializedName("genre") val genre: List<String>?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("featured_image_url") val featuredImageUrl: String?,
    @SerializedName("imdb_rate") val imdbRate: Double?
)

data class MovieDetailResponse(
    @SerializedName("status") val status: Boolean,
    @SerializedName("movie_info") val movieInfo: MovieInfoDetail?,
    @SerializedName("trailer_url") val trailerUrl: String?,
    @SerializedName("need_subscription") val needSubscription: Boolean?,
    @SerializedName("share_link") val shareLink: String?,
    @SerializedName("serie_download_urls") val serieDownloadUrls: List<SeasonItem>?,
    @SerializedName("movie_download_urls") val movieDownloadUrls: List<DownloadQualityItem>?
)

data class MovieInfoDetail(
    @SerializedName("id") val id: Int?,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("title_en") val titleEn: String?,
    @SerializedName("genre") val genre: List<String>?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("featured_image_url") val featuredImageUrl: String?,
    @SerializedName("image_back") val imageBack: String?,
    @SerializedName("imdb_rate") val imdbRate: Double?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("release_year") val releaseYear: String?,
    @SerializedName("short_description") val shortDescription: String?,
    @SerializedName("description") val description: String?
)

data class SeasonItem(
    @SerializedName("season_name") val seasonName: String?,
    @SerializedName("episodes") val episodes: List<EpisodeItem>?
)

data class EpisodeItem(
    @SerializedName("episode_name") val episodeName: String?,
    @SerializedName("download_urls") val downloadUrls: List<DownloadQualityItem>?
)

data class DownloadQualityItem(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url") val url: String?
)

// --- Retrofit Interface ---

interface DigiMovieApi {
    @GET("create_qr_login")
    suspend fun createQrLogin(): Response<QrLoginResponse>

    @GET("check_qr_login")
    suspend fun checkQrLogin(
        @Query("token") token: String
    ): Response<CheckQrResponse>

    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("get_showcase")
    suspend fun getShowcase(): Response<List<ShowcaseSection>>

    @GET("get_movie_detail")
    suspend fun getMovieDetail(
        @Query("movie_id") movieId: Int,
        @Header("Authorization") authHeader: String? = null
    ): Response<MovieDetailResponse>
}
