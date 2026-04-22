package app.umaia.android.domain.model

import kotlinx.serialization.Serializable

enum class ResourceId { FOOD, WATER, FUEL, FELT, LEATHER, WOOD, KUMIS, CRAFTS, SONGS }

data class ResourceDef(
    val id: ResourceId,
    val icon: String,
    val nameEn: String,
    val nameRu: String,
    val nameKk: String,
    val baseProduction: Double,
    val consumptionPerPop: Double
)

val allResources: List<ResourceDef> = listOf(
    ResourceDef(ResourceId.FOOD,    "🍖", "Food",    "Еда",     "Азық",    0.065, 0.020),
    ResourceDef(ResourceId.WATER,   "💧", "Water",   "Вода",    "Су",      0.050, 0.015),
    ResourceDef(ResourceId.FUEL,    "🔥", "Fuel",    "Топливо", "Отын",    0.025, 0.008),
    ResourceDef(ResourceId.FELT,    "🧶", "Felt",    "Войлок",  "Киіз",    0.008, 0.0),
    ResourceDef(ResourceId.LEATHER, "🪡", "Leather", "Кожа",    "Тері",    0.005, 0.0),
    ResourceDef(ResourceId.WOOD,    "🪵", "Wood",    "Дерево",  "Ағаш",    0.004, 0.0),
    ResourceDef(ResourceId.KUMIS,   "🥛", "Kumis",   "Кумыс",   "Қымыз",   0.003, 0.003),
    ResourceDef(ResourceId.CRAFTS,  "🏺", "Crafts",  "Ремёсла", "Қолөнер", 0.0,   0.0005),
    ResourceDef(ResourceId.SONGS,   "🎵", "Songs",   "Песни",   "Жырлар",  0.001, 0.001),
)

fun resourceDef(id: ResourceId): ResourceDef = allResources.first { it.id == id }

@Serializable
data class ResourceMap(
    val food: Double = 0.0,
    val water: Double = 0.0,
    val fuel: Double = 0.0,
    val felt: Double = 0.0,
    val leather: Double = 0.0,
    val wood: Double = 0.0,
    val kumis: Double = 0.0,
    val crafts: Double = 0.0,
    val songs: Double = 0.0
) {
    operator fun get(id: ResourceId): Double = when (id) {
        ResourceId.FOOD    -> food
        ResourceId.WATER   -> water
        ResourceId.FUEL    -> fuel
        ResourceId.FELT    -> felt
        ResourceId.LEATHER -> leather
        ResourceId.WOOD    -> wood
        ResourceId.KUMIS   -> kumis
        ResourceId.CRAFTS  -> crafts
        ResourceId.SONGS   -> songs
    }

    fun set(id: ResourceId, value: Double): ResourceMap = when (id) {
        ResourceId.FOOD    -> copy(food    = value)
        ResourceId.WATER   -> copy(water   = value)
        ResourceId.FUEL    -> copy(fuel    = value)
        ResourceId.FELT    -> copy(felt    = value)
        ResourceId.LEATHER -> copy(leather = value)
        ResourceId.WOOD    -> copy(wood    = value)
        ResourceId.KUMIS   -> copy(kumis   = value)
        ResourceId.CRAFTS  -> copy(crafts  = value)
        ResourceId.SONGS   -> copy(songs   = value)
    }

    fun add(id: ResourceId, delta: Double): ResourceMap =
        set(id, maxOf(0.0, this[id] + delta))

    companion object {
        val STARTING = ResourceMap(
            food = 30.0, water = 25.0, fuel = 15.0,
            felt = 8.0, leather = 4.0, wood = 6.0,
            kumis = 5.0, crafts = 0.0, songs = 3.0
        )
    }
}
