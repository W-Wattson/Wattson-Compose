package com.wattson.data.repository

import android.content.Context
import android.net.Uri
import com.wattson.data.remote.api.WattsonApi
import com.wattson.domain.model.Document
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for document operations.
 * Handles document CRUD operations with the backend API.
 */
@Singleton
class DocumentRepository @Inject constructor(
    private val api: WattsonApi,
    @ApplicationContext private val context: Context
) {
    /**
     * Fetches all documents for a user.
     */
    suspend fun getDocuments(
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Document>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDocuments(userId, limit, offset)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val documents = body.documents.map { it.toDomain(userId) }
                    Result.success(documents)
                } else {
                    Result.success(emptyList())
                }
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches a single document by ID.
     */
    suspend fun getDocumentById(
        userId: String,
        documentId: String
    ): Result<Document> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDocumentById(userId, documentId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.toDomain(userId))
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else if (response.code() == 404) {
                Result.failure(DocumentNotFoundException(documentId))
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets a download URL for a document.
     */
    suspend fun getDownloadUrl(
        userId: String,
        documentId: String
    ): Result<DocumentDownloadInfo> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDocumentDownloadUrl(userId, documentId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(
                        DocumentDownloadInfo(
                            url = body.url,
                            expiresInSeconds = body.expirationMinutes * 60
                        )
                    )
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else if (response.code() == 404) {
                Result.failure(DocumentNotFoundException(documentId))
            } else {
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Uploads a document from a content URI.
     */
    suspend fun uploadDocument(
        userId: String,
        uri: Uri,
        documentType: String = "OTHER"
    ): Result<Document> = withContext(Dispatchers.IO) {
        try {
            // Copy content URI to a temporary file
            val tempFile = copyUriToTempFile(uri)
                ?: return@withContext Result.failure(Exception("Failed to read file"))

            try {
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val fileName = getFileNameFromUri(uri) ?: "document"

                val requestFile = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", fileName, requestFile)
                val typePart = documentType.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = api.uploadDocument(userId, filePart, typePart)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Result.success(body.toDomain(userId))
                    } else {
                        Result.failure(Exception("Empty response body"))
                    }
                } else {
                    Result.failure(Exception("Upload failed: ${response.code()}"))
                }
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a document.
     */
    suspend fun deleteDocument(
        userId: String,
        documentId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteDocument(userId, documentId)
            if (response.isSuccessful || response.code() == 204) {
                Result.success(Unit)
            } else if (response.code() == 404) {
                Result.failure(DocumentNotFoundException(documentId))
            } else {
                Result.failure(Exception("Delete failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }
}

/**
 * Document download info containing URL and expiration.
 */
data class DocumentDownloadInfo(
    val url: String,
    val expiresInSeconds: Int
)

/**
 * Exception thrown when a document is not found.
 */
class DocumentNotFoundException(val documentId: String) : Exception("Document not found: $documentId")
