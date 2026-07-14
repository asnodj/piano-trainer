package dev.asnodj.pianotrainer.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A local player profile. */
data class Profile(val id: String, val displayName: String)

/** The two family profiles of v1. */
val PROFILES = listOf(
    Profile(id = "papa", displayName = "Papa"),
    Profile(id = "lara", displayName = "Lara"),
)

private val Context.profileDataStore by preferencesDataStore(name = "profiles")

/**
 * Persists the selected profile and each profile's best scores per song and
 * practice mode (DataStore preferences).
 */
class ProfileRepository(private val context: Context) {

    private val selectedProfileKey = stringPreferencesKey("selected_profile")

    /** Id of the selected profile (defaults to the first profile). */
    val selectedProfileId: Flow<String> = context.profileDataStore.data.map { preferences ->
        preferences[selectedProfileKey] ?: PROFILES.first().id
    }

    /**
     * All persisted best scores, keyed by "songId/mode" for the given profile.
     *
     * @param profileId Profile whose scores to observe.
     * @return Map of "songId/mode" to best score 0..100.
     */
    fun bestScores(profileId: String): Flow<Map<String, Int>> {
        val prefix = "best_${profileId}_"
        return context.profileDataStore.data.map { preferences ->
            preferences.asMap().entries
                .filter { entry -> entry.key.name.startsWith(prefix) }
                .associate { entry -> entry.key.name.removePrefix(prefix) to (entry.value as Int) }
        }
    }

    /**
     * Selects the active profile.
     *
     * @param profileId One of [PROFILES] ids.
     */
    suspend fun selectProfile(profileId: String) {
        context.profileDataStore.edit { preferences ->
            preferences[selectedProfileKey] = profileId
        }
    }

    /**
     * Saves a finished-run score, keeping the best one.
     *
     * @param profileId Profile that played.
     * @param songId Song identifier.
     * @param mode "wait" or "tempo".
     * @param score Accuracy/score 0..100.
     */
    suspend fun saveBestScore(profileId: String, songId: String, mode: String, score: Int) {
        val key = intPreferencesKey("best_${profileId}_${songId}/${mode}")
        context.profileDataStore.edit { preferences ->
            val previousBest = preferences[key] ?: 0
            if (score > previousBest) {
                preferences[key] = score
            }
        }
    }
}
