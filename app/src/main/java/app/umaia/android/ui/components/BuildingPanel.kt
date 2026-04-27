package app.umaia.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.domain.model.*
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.strings.localizedDesc
import app.umaia.android.ui.strings.localizedName
import app.umaia.android.ui.theme.*

@Composable
fun BuildingPanel(
    def: BuildingDef,
    group: BuildingGroup?,
    resources: ResourceMap,
    onBuild: () -> Unit,
    onAssign: (delta: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val canAfford = def.cost.all { (rId, cost) -> resources[rId] >= cost }
    val hasWorkers = def.maxWorkers > 0

    Column(
        modifier = modifier
            .background(TC.cardAlt, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(def.icon, fontSize = 24.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(def.localizedName(s.code), color = TC.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(def.localizedDesc(s.code), color = TC.muted, fontSize = 11.sp, lineHeight = 14.sp)
            }
            if (group != null && group.count > 0) {
                Text("×${group.count}", color = GoldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Cost
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            def.cost.forEach { (rId, cost) ->
                val rDef = app.umaia.android.domain.model.resourceDef(rId)
                Text(
                    "${rDef.icon} ${"%.0f".format(cost)}",
                    color = if (resources[rId] >= cost) Gold else TerracottaRed,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Build button
            Button(
                onClick = onBuild,
                enabled = canAfford,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) Gold else TC.muted
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(s.tabBuild, color = NightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Worker controls (only for buildings that use workers)
            if (hasWorkers && group != null && group.count > 0) {
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onAssign(-1) }, modifier = Modifier.size(28.dp)) {
                        Text("−", color = TerracottaRed, fontSize = 18.sp)
                    }
                    Text(
                        "${group.totalWorkers}/${group.maxWorkers}",
                        color = GoldLight, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { onAssign(+1) }, modifier = Modifier.size(28.dp)) {
                        Text("+", color = SageGreen, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

// ── Buildings Panel with Tabs ────────────────────────────────────────────────

@Composable
fun BuildingsPanelWithTabs(
    state: GameState,
    onBuild: (BuildingId) -> Unit,
    onAssignWorker: (String, Int) -> Unit,
    onNavigateToOracle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val unlockedTier = maxUnlockedTier(state.buildings)
    val grouped = state.buildings.groupBy { it.type }
    val s = LocalStrings.current

    Column(modifier = modifier) {
        TabRow(selectedTabIndex = selectedTab, containerColor = TC.card) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(s.tabCamp, Modifier.padding(vertical = 12.dp), fontSize = 12.sp)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(s.tabBuild, Modifier.padding(vertical = 12.dp), fontSize = 12.sp)
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text(s.tabUmaia, Modifier.padding(vertical = 12.dp), fontSize = 12.sp)
            }
        }

        when (selectedTab) {
            0 -> CampTab(state, grouped, unlockedTier, onAssignWorker)
            1 -> BuildTab(state, grouped, unlockedTier, onBuild, onAssignWorker)
            2 -> UmaiaTab(state, onNavigateToOracle)
        }
    }
}

@Composable
private fun CampTab(
    state: GameState,
    grouped: Map<BuildingId, List<BuiltBuilding>>,
    unlockedTier: Int,
    onAssignWorker: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val s = LocalStrings.current
        Text(s.assignWorkers, color = TC.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

        BuildingId.entries.filter { buildingDef(it).tier <= unlockedTier && buildingDef(it).maxWorkers > 0 }
            .forEach { type ->
                val def = buildingDef(type)
                val instances = grouped[type] ?: emptyList()
                if (instances.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TC.cardAlt, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(def.localizedName(s.code), color = TC.text, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                        instances.forEachIndexed { idx, instance ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("${def.icon} #${idx + 1}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onAssignWorker(instance.instanceId, -1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("−", color = TerracottaRed, fontSize = 16.sp)
                                    }
                                    Text(
                                        "${instance.workers}/${def.maxWorkers}",
                                        color = GoldLight, fontSize = 11.sp,
                                        modifier = Modifier.width(36.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(
                                        onClick = { onAssignWorker(instance.instanceId, +1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", color = SageGreen, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun BuildTab(
    state: GameState,
    grouped: Map<BuildingId, List<BuiltBuilding>>,
    unlockedTier: Int,
    onBuild: (BuildingId) -> Unit,
    onAssignWorker: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BuildingId.entries.filter { buildingDef(it).tier <= unlockedTier }.forEach { type ->
            val def = buildingDef(type)
            val group = BuildingGroup(
                type = type,
                count = grouped[type]?.size ?: 0,
                totalWorkers = grouped[type]?.sumOf { it.workers } ?: 0,
                maxWorkers = (grouped[type]?.size ?: 0) * def.maxWorkers
            )
            BuildingPanel(
                def = def,
                group = group,
                resources = state.resources,
                onBuild = { onBuild(type) },
                onAssign = { delta ->
                    val instance = if (delta > 0) {
                        state.buildings.firstOrNull { it.type == type && it.workers < def.maxWorkers }
                    } else {
                        state.buildings.firstOrNull { it.type == type && it.workers > 0 }
                    }
                    if (instance != null) onAssignWorker(instance.instanceId, delta)
                }
            )
        }
    }
}

@Composable
private fun UmaiaTab(state: GameState, onNavigateToOracle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val s = LocalStrings.current

        // Game loop explanation
        Text(s.howGameWorks, color = TC.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

        val gameLoop = listOf(
            Triple("👣", s.gameLoopWalkTitle, s.gameLoopWalkDesc),
            Triple("✨", s.gameLoopNurTitle, s.gameLoopNurDesc),
            Triple("🏗️", s.gameLoopBuildTitle, s.gameLoopBuildDesc),
            Triple("👥", s.gameLoopGrowTitle, s.gameLoopGrowDesc)
        )

        gameLoop.forEach { (emoji, title, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TC.cardAlt, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(emoji, fontSize = 20.sp)
                Column {
                    Text(title, color = TC.text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(desc, color = TC.muted, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Active quest
        currentQuest(completedIds = state.completedQuests)?.let { quest ->
            Text(s.yourQuest, color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TC.card, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎯", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(quest.localizedName(s.code), color = TC.text, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(quest.localizedDesc(s.code), color = TC.muted, fontSize = 10.sp, lineHeight = 13.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun currentQuest(completedIds: List<Int>): QuestDef? =
    allQuests.firstOrNull { it.id !in completedIds }
