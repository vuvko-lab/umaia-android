package app.umaia.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.domain.model.BuildingDef
import app.umaia.android.domain.model.BuildingGroup
import app.umaia.android.domain.model.ResourceMap
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
                Text(def.nameEn, color = TC.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(def.descEn, color = TC.muted, fontSize = 11.sp, lineHeight = 14.sp)
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
                Text("Build", color = NightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
