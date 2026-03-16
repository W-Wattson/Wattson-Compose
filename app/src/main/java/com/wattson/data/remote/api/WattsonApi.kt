package com.wattson.data.remote.api

import com.wattson.BuildConfig
import com.wattson.data.remote.dto.AuthResponse
import com.wattson.data.remote.dto.DocumentListResponse
import com.wattson.data.remote.dto.DocumentResponse
import com.wattson.data.remote.dto.DownloadUrlResponse
import com.wattson.data.remote.dto.LoginRequest
import com.wattson.data.remote.dto.ProductDto
import com.wattson.data.remote.dto.RefreshTokenRequest
import com.wattson.data.remote.dto.RegisterRequest
import com.wattson.data.remote.dto.ScanHistoryResponse
import com.wattson.data.remote.dto.ScanRequest
import com.wattson.data.remote.dto.ScanResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for Wattson backend services.
 */
interface WattsonApi {

    // =====================
    // Authentication API
    // =====================

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Unit>

    // =====================
    // Product Catalog API
    // =====================

    @GET("api/v1/products/ean/{ean}")
    suspend fun getProductByEan(
        @Path("ean") ean: String
    ): Response<ResponseBody>

    @GET("api/v1/products/{id}")
    suspend fun getProductById(
        @Path("id") id: String
    ): Response<ResponseBody>

    @GET("api/v1/products/eprel/{category}/{registrationNumber}")
    suspend fun getProductByEprelId(
        @Path("category") category: String,
        @Path("registrationNumber") registrationNumber: String
    ): Response<ResponseBody>

    // =====================
    // Scan API
    // =====================

    @POST("api/v1/scans")
    suspend fun createScan(
        @Header("X-User-Id") userId: String,
        @Body request: ScanRequest
    ): Response<ResponseBody>

    @GET("api/v1/scans/history")
    suspend fun getScanHistory(
        @Header("X-User-Id") userId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ScanHistoryResponse>

    @GET("api/v1/scans/{scanId}")
    suspend fun getScanById(
        @Header("X-User-Id") userId: String,
        @Path("scanId") scanId: String
    ): Response<ScanResponse>

    // =====================
    // Warranty Vault API
    // =====================

    @GET("api/v1/vault/documents")
    suspend fun getDocuments(
        @Header("Authorization") authorization: String,
        @Header("X-User-Id") userId: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<DocumentListResponse>

    @GET("api/v1/vault/documents/{documentId}")
    suspend fun getDocumentById(
        @Header("Authorization") authorization: String,
        @Header("X-User-Id") userId: String,
        @Path("documentId") documentId: String
    ): Response<DocumentResponse>

    @GET("api/v1/vault/documents/{documentId}/download")
    suspend fun getDocumentDownloadUrl(
        @Header("Authorization") authorization: String,
        @Header("X-User-Id") userId: String,
        @Path("documentId") documentId: String
    ): Response<DownloadUrlResponse>

    @Multipart
    @POST("api/v1/vault/documents")
    suspend fun uploadDocument(
        @Header("Authorization") authorization: String,
        @Header("X-User-Id") userId: String,
        @Part file: MultipartBody.Part,
        @Part("documentType") documentType: RequestBody
    ): Response<DocumentResponse>

    @DELETE("api/v1/vault/documents/{documentId}")
    suspend fun deleteDocument(
        @Header("Authorization") authorization: String,
        @Header("X-User-Id") userId: String,
        @Path("documentId") documentId: String
    ): Response<Unit>

    companion object {
        val BASE_URL_DEFAULT = com.wattson.BuildConfig.API_BASE_URL
    }
}
