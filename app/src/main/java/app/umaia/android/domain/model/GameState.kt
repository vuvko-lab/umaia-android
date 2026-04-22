package app.umaia.android.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val version: Int = CURRENT_VERSION,
    val resources: ResourceMap = ResourceMap.STARTING,
    val buildings: List<BuiltBuilding> = emptyList(),
    val population: Int = 5,
    val maxPopulation: Int = 8,
    val spirit: Double = 50.0,
    val nur: Double = 0.0,
    val day: Int = 1,
    val completedQuests: List<Int> = emptyList(),
    val totalWorkers: Int = 0,
    val panelsOpened: List<String> = emptyList(),
    val stepsSynced: Boolean = false,
    val completedNutritionCategories: List<String> = emptyList(),
    val oracleCompleted: Boolean = false
) {
    val buildingGroups: List<BuildingGroup>
        get() {
            val map = mutableMapOf<BuildingId, BuildingGroup>()
            for (b in buildings) {
                val def = buildingDef(b.type)
                val g = map.getOrDefault(b.type, BuildingGroup(b.type, 0, 0, 0))
                map[b.type] = g.copy(
                    count = g.count + 1,
                    totalWorkers = g.totalWorkers + b.workers,
                    maxWorkers = g.maxWorkers + def.maxWorkers
                )
            }
            return map.values.toList()
        }

    fun countBuildings(type: BuildingId): Int = buildings.count { it.type == type }

    companion object {
        const val CURRENT_VERSION = 2
    }
}
