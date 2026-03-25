package com.wattson.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the local list of favorite product identifiers for the current app install.
 */
@Singleton
class FavoriteRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isFavorite(productId: String): Boolean {
        return getFavoriteIds().contains(productId)
    }

    fun setFavorite(productId: String, isFavorite: Boolean): Result<Boolean> {
        return try {
            val updatedFavorites = getFavoriteIds().toMutableSet().apply {
                if (isFavorite) {
                    add(productId)
                } else {
                    remove(productId)
                }
            }

            preferences.edit()
                .putStringSet(KEY_FAVORITES, updatedFavorites)
                .apply()

            Result.success(isFavorite)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private fun getFavoriteIds(): Set<String> {
        return preferences.getStringSet(KEY_FAVORITES, emptySet()).orEmpty()
    }

    private companion object {
        private const val PREFERENCES_NAME = "wattson_favorites"
        private const val KEY_FAVORITES = "favorite_product_ids"
    }
}
