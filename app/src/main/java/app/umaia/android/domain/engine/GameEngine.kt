package app.umaia.android.domain.engine

import app.umaia.android.domain.model.*
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.random.Random

// ── Constants ─────────────────────────────────────────────────────────────────
const val TICK_MS: Long            = 4_000L
const val NUR_DECAY_PER_TICK       = 0.3
const val NUR_BOOST                = 1.15
const val BASE_MAX_POP             = 8
const val MIN_POP                  = 2
const val SPIRIT_DRIFT_RATE        = 0.05
const val SESSION_WARN_MINUTES     = 10
const val SESSION_DECAY_MINUTES    = 15
const val NUR_DECAY_PER_MINUTE     = 5.0
const val QUESTIONNAIRE_BONUS      = 50
const val NUR_PER_QUESTION         = 3

// ── Formula helpers ───────────────────────────────────────────────────────────

/** Nur earned from steps: 50 * (1 - e^(-steps / 6000)) */
fun stepsToNur(steps: Int): Int =
    (50.0 * (1.0 - exp(-steps / 6000.0))).roundToInt()

/** Nur lost to session overstay (>15 min). */
fun sessionDecayNur(sessionMinutes: Int): Double =
    if (sessionMinutes <= SESSION_DECAY_MINUTES) 0.0
    else (sessionMinutes - SESSION_DECAY_MINUTES).toDouble() * NUR_DECAY_PER_MINUTE

// ── Spirit calculation ────────────────────────────────────────────────────────

private fun calculateSpiritTarget(resources: ResourceMap, nur: Double): Double {
    var target = 50.0
    if (resources[ResourceId.FOOD] > 0 && resources[ResourceId.WATER] > 0) target += 12
    if (resources[ResourceId.FUEL] > 0) target += 4
    if (resources[ResourceId.KUMIS] > 0.5) target += 10
    if (resources[ResourceId.SONGS] > 0.5) target += 8
    if (resources[ResourceId.CRAFTS] > 0.5) target += 7
    if (nur > 0) target += 4
    if (resources[ResourceId.FOOD] <= 0) target -= 25
    if (resources[ResourceId.WATER] <= 0) target -= 20
    if (resources[ResourceId.KUMIS] <= 0.3
        && resources[ResourceId.SONGS] <= 0.3
        && resources[ResourceId.CRAFTS] <= 0.3) target -= 12
    return target.coerceIn(0.0, 100.0)
}

// ── Pure tick ─────────────────────────────────────────────────────────────────

/**
 * Pure tick function — no side effects. Returns new state.
 * Pass a seeded [rng] for deterministic testing.
 */
fun tick(state: GameState, rng: Random = Random.Default): GameState {
    var res = state.resources
    val boost = if (state.nur > 0) NUR_BOOST else 1.0

    // Base resource production
    for (rDef in allResources) {
        res = res.add(rDef.id, rDef.baseProduction * boost)
    }

    // Building production / consumption
    for (b in state.buildings) {
        val def = buildingDef(b.type)
        if (def.maxWorkers == 0) {
            for ((rId, amount) in def.production) res = res.add(rId, amount * boost)
        } else if (b.workers > 0) {
            val eff = b.workers.toDouble() / def.maxWorkers
            for ((rId, amount) in def.production)  res = res.add(rId,  amount * eff * boost)
            for ((rId, amount) in def.consumption) res = res.add(rId, -amount * eff)
        }
    }

    // Population consumption
    for (rDef in allResources.filter { it.consumptionPerPop > 0 }) {
        res = res.add(rDef.id, -rDef.consumptionPerPop * state.population)
    }

    val newNur = maxOf(0.0, state.nur - NUR_DECAY_PER_TICK)

    val spiritTarget = calculateSpiritTarget(res, newNur)
    val newSpirit = (state.spirit + (spiritTarget - state.spirit) * SPIRIT_DRIFT_RATE)
        .coerceIn(0.0, 100.0)

    val newMaxPop = BASE_MAX_POP + state.buildings.count { it.type == BuildingId.YURT } * 3

    var newPop = state.population
    if (newSpirit >= 80 && newPop < newMaxPop && rng.nextDouble() < 0.015) newPop++
    if (res[ResourceId.FOOD] <= 0 && res[ResourceId.WATER] <= 0) {
        if (rng.nextDouble() < 0.03 && newPop > MIN_POP) newPop--
    } else if (res[ResourceId.FOOD] <= 0 || res[ResourceId.WATER] <= 0) {
        if (rng.nextDouble() < 0.01 && newPop > MIN_POP) newPop--
    }

    val newTotalWorkers = state.buildings.sumOf { it.workers }
    val newDay = if (rng.nextDouble() < 0.04) state.day + 1 else state.day

    return state.copy(
        resources     = res,
        population    = newPop,
        maxPopulation = newMaxPop,
        spirit        = newSpirit,
        nur           = newNur,
        totalWorkers  = newTotalWorkers,
        day           = newDay
    )
}

// ── Build ─────────────────────────────────────────────────────────────────────

/** Attempt to construct a building. Returns null if resources insufficient. */
fun buildBuilding(state: GameState, type: BuildingId): GameState? {
    val def = buildingDef(type)
    if (def.cost.any { (rId, cost) -> state.resources[rId] < cost }) return null

    var res = state.resources
    for ((rId, cost) in def.cost) res = res.add(rId, -cost)

    val newBuilding = BuiltBuilding(
        instanceId = "${type.name.lowercase()}_${System.currentTimeMillis()}_${(1000..9999).random()}",
        type = type
    )
    return state.copy(resources = res, buildings = state.buildings + newBuilding)
}

// ── Assign worker ─────────────────────────────────────────────────────────────

/** Change worker count on a specific building instance by delta (+1 or -1). */
fun assignWorker(state: GameState, instanceId: String, delta: Int): GameState {
    val buildings = state.buildings.toMutableList()
    val idx = buildings.indexOfFirst { it.instanceId == instanceId }
    if (idx < 0) return state

    val b = buildings[idx]
    val def = buildingDef(b.type)
    val currentTotal = buildings.sumOf { it.workers }

    if (delta > 0 && b.workers >= def.maxWorkers) return state
    if (delta > 0 && currentTotal >= state.population) return state
    if (delta < 0 && b.workers <= 0) return state

    buildings[idx] = b.copy(workers = b.workers + delta)
    return state.copy(buildings = buildings, totalWorkers = buildings.sumOf { it.workers })
}

// ── Quest completion ──────────────────────────────────────────────────────────

fun isQuestComplete(state: GameState, quest: QuestDef): Boolean = when (quest.id) {
    1  -> state.countBuildings(BuildingId.PASTURE) > 0
    2  -> state.countBuildings(BuildingId.WELL) > 0
    3  -> state.countBuildings(BuildingId.HEARTH) > 0
    4  -> state.totalWorkers >= 3
    5  -> "spirit_panel" in state.panelsOpened && "population_panel" in state.panelsOpened
    6  -> state.stepsSynced
    7  -> state.countBuildings(BuildingId.WOODCAMP) > 0
    8  -> state.countBuildings(BuildingId.LOOM) > 0
    9  -> state.countBuildings(BuildingId.BREWERY) > 0
    10 -> state.countBuildings(BuildingId.YURT) > 0
    11 -> state.oracleCompleted
    12 -> state.completedNutritionCategories.isNotEmpty()
    else -> false
}
