package com.braintreepayments.api.internal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "popup_bridge_preferences")

/**
 * Repository responsible for storing and retrieving the pending request.
 */
internal class PendingRequestRepository(
    context: Context,
    private val dataStore: DataStore<Preferences> = context.dataStore,
) {

    private val pendingRequestKey = stringPreferencesKey("pending_request")

    /**
     * Stores the pending request in the DataStore.
     */
    suspend fun storePendingRequest(pendingRequest: String) {
        dataStore.edit { preferences: MutablePreferences ->
            preferences[pendingRequestKey] = pendingRequest
        }
    }

    /**
     * Retrieves the pending request from the DataStore.
     */
    suspend fun getPendingRequest(): String? {
        return dataStore.data.map { preferences: Preferences ->
            preferences[pendingRequestKey]
        }.firstOrNull()
    }

    /**
     * Clears the pending request from the DataStore.
     */
    suspend fun clearPendingRequest() {
        dataStore.edit { preferences: MutablePreferences ->
            preferences.remove(pendingRequestKey)
        }
    }
}
