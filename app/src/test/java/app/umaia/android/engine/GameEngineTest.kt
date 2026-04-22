package app.umaia.android.engine

import app.umaia.android.domain.engine.*
import app.umaia.android.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private val seededRng = Random(42)

    // ── stepsToNur ────────────────────────────────────────────────────────────

    @Test fun stepsToNur_returnsZeroForZeroSteps() {
        assertEquals(0, stepsToNur(0))
    }

    @Test fun stepsToNur_returnsAbout15For2000Steps() {
        val nur = stepsToNur(2000)
        assertTrue("Expected 14-16, got $nur", nur in 14..16)
    }

    @Test fun stepsToNur_capsNear50ForLargeStepCount() {
        assertTrue(stepsToNur(100_000) <= 50)
    }

    // ── tick ──────────────────────────────────────────────────────────────────

    @Test fun tick_producesFood() {
        val before = GameState(population = 0)
        val after = tick(before, seededRng)
        assertTrue(after.resources[ResourceId.FOOD] > before.resources[ResourceId.FOOD])
    }

    @Test fun tick_decaysNur() {
        val state = GameState(nur = 10.0)
        val after = tick(state, seededRng)
        assertTrue(after.nur < 10.0)
    }

    @Test fun tick_keepsNurAboveZero() {
        val state = GameState(nur = 0.0)
        val after = tick(state, seededRng)
        assertTrue(after.nur >= 0.0)
    }

    @Test fun tick_movesSpiritTowardTarget() {
        val state = GameState(spirit = 0.0)
        val after = tick(state, seededRng)
        assertTrue(after.spirit > 0.0)
    }

    // ── buildBuilding ─────────────────────────────────────────────────────────

    @Test fun buildBuilding_deductsResources() {
        val state = GameState()
        val after = buildBuilding(state, BuildingId.HEARTH)
        assertNotNull(after)
        assertTrue(after!!.resources[ResourceId.FELT] < state.resources[ResourceId.FELT])
        assertTrue(after.resources[ResourceId.WOOD] < state.resources[ResourceId.WOOD])
    }

    @Test fun buildBuilding_failsWhenResourcesInsufficient() {
        val state = GameState(resources = ResourceMap())
        val result = buildBuilding(state, BuildingId.PASTURE)
        assertNull(result)
    }

    @Test fun buildBuilding_appendsBuildingToList() {
        val state = GameState()
        val after = buildBuilding(state, BuildingId.HEARTH)!!
        assertEquals(1, after.buildings.size)
        assertEquals(BuildingId.HEARTH, after.buildings.first().type)
    }

    // ── assignWorker ──────────────────────────────────────────────────────────

    @Test fun assignWorker_incrementsWorkers() {
        val state = GameState()
        val withBuilding = buildBuilding(state, BuildingId.PASTURE)!!
        val instanceId = withBuilding.buildings.first().instanceId
        val after = assignWorker(withBuilding, instanceId, +1)
        assertEquals(1, after.buildings.first().workers)
    }

    @Test fun assignWorker_cannotExceedMaxWorkers() {
        val state = GameState()
        val withBuilding = buildBuilding(state, BuildingId.PASTURE)!! // maxWorkers = 1
        val instanceId = withBuilding.buildings.first().instanceId
        val after1 = assignWorker(withBuilding, instanceId, +1)
        val after2 = assignWorker(after1, instanceId, +1) // should be clamped
        assertEquals(1, after2.buildings.first().workers)
    }

    @Test fun assignWorker_cannotExceedPopulation() {
        val state = GameState(population = 1)
        val withBuilding = buildBuilding(state, BuildingId.LOOM)!! // maxWorkers = 2
        val id = withBuilding.buildings.first().instanceId
        val after1 = assignWorker(withBuilding, id, +1)
        val after2 = assignWorker(after1, id, +1) // would exceed pop=1
        assertEquals(1, after2.buildings.first().workers)
    }

    // ── Quest completion ──────────────────────────────────────────────────────

    @Test fun quest1_completeWhenPastureBuilt() {
        val state = GameState()
        val withPasture = buildBuilding(state, BuildingId.PASTURE)!!
        val quest = allQuests.first { it.id == 1 }
        assertTrue(isQuestComplete(withPasture, quest))
    }

    @Test fun quest4_completeWhen3WorkersAssigned() {
        val resources = ResourceMap(wood = 20.0, leather = 20.0, felt = 20.0,
            food = 30.0, water = 25.0, fuel = 10.0, kumis = 0.0, crafts = 0.0, songs = 0.0)
        var state = GameState(resources = resources)
        state = buildBuilding(state, BuildingId.LOOM)!!
        state = buildBuilding(state, BuildingId.PASTURE)!!
        val loomId = state.buildings.first { it.type == BuildingId.LOOM }.instanceId
        val pastureId = state.buildings.first { it.type == BuildingId.PASTURE }.instanceId
        state = assignWorker(state, loomId, +1)
        state = assignWorker(state, loomId, +1)
        state = assignWorker(state, pastureId, +1)
        val quest = allQuests.first { it.id == 4 }
        assertTrue(isQuestComplete(state, quest))
    }
}
