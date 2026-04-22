package app.umaia.android.domain.model

import kotlinx.serialization.Serializable

enum class BuildingId {
    PASTURE, WELL, HEARTH, LOOM, TANNERY, WOODCAMP, BREWERY, WORKSHOP, CIRCLE, YURT
}

data class BuildingDef(
    val id: BuildingId,
    val tier: Int,
    val icon: String,
    val nameEn: String,
    val nameRu: String,
    val nameKk: String,
    val cost: Map<ResourceId, Double>,
    val production: Map<ResourceId, Double>,
    val consumption: Map<ResourceId, Double>,
    val maxWorkers: Int,
    val popBonus: Int,
    val descEn: String,
    val descRu: String,
    val descKk: String
)

val allBuildings: List<BuildingDef> = listOf(
    BuildingDef(
        BuildingId.PASTURE, tier = 0, "🐎", "Pasture", "Пастбище", "Жайылым",
        cost = mapOf(ResourceId.FELT to 3.0, ResourceId.WOOD to 2.0),
        production = mapOf(ResourceId.FOOD to 0.10, ResourceId.LEATHER to 0.01),
        consumption = emptyMap(), maxWorkers = 1, popBonus = 0,
        descEn = "Horses graze and provide food and leather",
        descRu = "Лошади пасутся и дают еду и кожу",
        descKk = "Жылқылар жайылып, азық пен тері береді"
    ),
    BuildingDef(
        BuildingId.WELL, tier = 0, "⛲", "Well", "Колодец", "Құдық",
        cost = mapOf(ResourceId.WOOD to 3.0, ResourceId.LEATHER to 2.0),
        production = mapOf(ResourceId.WATER to 0.12),
        consumption = emptyMap(), maxWorkers = 1, popBonus = 0,
        descEn = "A deep well provides clean water",
        descRu = "Глубокий колодец даёт чистую воду",
        descKk = "Терең құдық таза су береді"
    ),
    BuildingDef(
        BuildingId.HEARTH, tier = 0, "🏕️", "Hearth", "Очаг", "Ошақ",
        cost = mapOf(ResourceId.FELT to 2.0, ResourceId.WOOD to 1.0),
        production = mapOf(ResourceId.FUEL to 0.08, ResourceId.KUMIS to 0.015),
        consumption = emptyMap(), maxWorkers = 1, popBonus = 0,
        descEn = "The sacred fire warms and produces kumis",
        descRu = "Священный огонь греет и даёт кумыс",
        descKk = "Қасиетті от жылытып, қымыз береді"
    ),
    BuildingDef(
        BuildingId.LOOM, tier = 1, "🧵", "Loom", "Ткацкий станок", "Тоқыма станогы",
        cost = mapOf(ResourceId.WOOD to 5.0, ResourceId.LEATHER to 3.0),
        production = mapOf(ResourceId.FELT to 0.06),
        consumption = emptyMap(), maxWorkers = 2, popBonus = 0,
        descEn = "Weaves wool into felt for buildings",
        descRu = "Ткёт шерсть в войлок для построек",
        descKk = "Жүннен құрылысқа арналған киіз тоқиды"
    ),
    BuildingDef(
        BuildingId.TANNERY, tier = 1, "🔨", "Tannery", "Дубильня", "Тері зауыты",
        cost = mapOf(ResourceId.WOOD to 4.0, ResourceId.FUEL to 4.0),
        production = mapOf(ResourceId.LEATHER to 0.05),
        consumption = emptyMap(), maxWorkers = 1, popBonus = 0,
        descEn = "Tans hides into usable leather",
        descRu = "Дубит шкуры в кожу",
        descKk = "Терілерді өңделген теріге айналдырады"
    ),
    BuildingDef(
        BuildingId.WOODCAMP, tier = 1, "🪓", "Woodcamp", "Лесоруб", "Ағаш кесу лагері",
        cost = mapOf(ResourceId.FELT to 4.0, ResourceId.LEATHER to 2.0),
        production = mapOf(ResourceId.WOOD to 0.03),
        consumption = emptyMap(), maxWorkers = 1, popBonus = 0,
        descEn = "Gathers scarce wood from the steppe",
        descRu = "Собирает редкую древесину со степи",
        descKk = "Даладан тапшы ағашты жинайды"
    ),
    BuildingDef(
        BuildingId.BREWERY, tier = 2, "🥛", "Kumys House", "Кумысхана", "Қымызхана",
        cost = mapOf(ResourceId.WOOD to 4.0, ResourceId.FELT to 3.0),
        production = mapOf(ResourceId.KUMIS to 0.03),
        consumption = mapOf(ResourceId.FOOD to 0.005),
        maxWorkers = 1, popBonus = 0,
        descEn = "Converts food into kumis for Spirit",
        descRu = "Превращает еду в кумыс для Духа",
        descKk = "Азықты Рух үшін қымызға айналдырады"
    ),
    BuildingDef(
        BuildingId.WORKSHOP, tier = 2, "⚒️", "Workshop", "Мастерская", "Шеберхана",
        cost = mapOf(ResourceId.WOOD to 8.0, ResourceId.LEATHER to 5.0, ResourceId.FELT to 3.0),
        production = mapOf(ResourceId.CRAFTS to 0.04),
        consumption = mapOf(ResourceId.LEATHER to 0.005),
        maxWorkers = 2, popBonus = 0,
        descEn = "Crafts goods from leather for Spirit",
        descRu = "Делает ремёсла из кожи для Духа",
        descKk = "Теріден Рух үшін қолөнер жасайды"
    ),
    BuildingDef(
        BuildingId.CIRCLE, tier = 2, "🎶", "Circle of Stories", "Круг Историй", "Жырлар Шеңбері",
        cost = mapOf(ResourceId.FELT to 6.0, ResourceId.KUMIS to 3.0),
        production = mapOf(ResourceId.SONGS to 0.02),
        consumption = emptyMap(), maxWorkers = 0, popBonus = 0,
        descEn = "Stories and songs uplift the Spirit",
        descRu = "Истории и песни поднимают Дух",
        descKk = "Әңгімелер мен жырлар Рухты көтереді"
    ),
    BuildingDef(
        BuildingId.YURT, tier = 2, "⛺", "New Yurt", "Новая Юрта", "Жаңа Киіз үй",
        cost = mapOf(ResourceId.FELT to 4.0, ResourceId.WOOD to 2.0, ResourceId.LEATHER to 1.0),
        production = emptyMap(),
        consumption = emptyMap(), maxWorkers = 0, popBonus = 3,
        descEn = "Houses 3 more people in your tribe",
        descRu = "Вмещает ещё 3 человека в племени",
        descKk = "Руыңа тағы 3 адам сыйғызады"
    ),
)

fun buildingDef(id: BuildingId): BuildingDef = allBuildings.first { it.id == id }

fun maxUnlockedTier(buildings: List<BuiltBuilding>): Int {
    val builtTiers = buildings.map { buildingDef(it.type).tier }.toSet()
    return when {
        builtTiers.contains(1) -> 2
        builtTiers.contains(0) -> 1
        else -> 0
    }
}

fun isTierUnlocked(tier: Int, buildings: List<BuiltBuilding>): Boolean =
    tier <= maxUnlockedTier(buildings)

@Serializable
data class BuiltBuilding(
    val instanceId: String,
    val type: BuildingId,
    val workers: Int = 0
)

data class BuildingGroup(
    val type: BuildingId,
    val count: Int,
    val totalWorkers: Int,
    val maxWorkers: Int
)
