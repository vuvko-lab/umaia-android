package app.umaia.android.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.umaia.android.domain.model.GameState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.gameDataStore by preferencesDataStore("game_state")

@Singleton
class GameStateStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val KEY  = stringPreferencesKey("state_json")

    val stateFlow: Flow<GameState> = context.gameDataStore.data
        .map { prefs ->
            prefs[KEY]
                ?.let { runCatching { json.decodeFromString<GameState>(it) }.getOrNull() }
                ?.let { validate(it) }
                ?: GameState()
        }
        .distinctUntilChanged()

    suspend fun load(): GameState = stateFlow.first()

    suspend fun update(transform: (GameState) -> GameState) {
        context.gameDataStore.edit { prefs ->
            val current = prefs[KEY]
                ?.let { runCatching { json.decodeFromString<GameState>(it) }.getOrNull() }
                ?.let { validate(it) }
                ?: GameState()
            val next = transform(current)
            prefs[KEY] = json.encodeToString(GameState.serializer(), next)
        }
    }

    suspend fun reset() = update { GameState() }

    private fun validate(state: GameState): GameState {
        if (state.version != GameState.CURRENT_VERSION) return GameState()
        val maxPop = 8 + state.buildings.count { it.type == app.umaia.android.domain.model.BuildingId.YURT } * 3
        return state.copy(
            maxPopulation = maxPop,
            population    = state.population.coerceIn(2, maxPop),
            spirit        = state.spirit.coerceIn(0.0, 100.0),
            nur           = maxOf(0.0, state.nur)
        )
    }
}
