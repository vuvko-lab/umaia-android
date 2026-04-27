package app.umaia.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.domain.model.ResourceDef
import app.umaia.android.domain.model.ResourceMap
import app.umaia.android.ui.strings.LocalStrings
import app.umaia.android.ui.strings.localizedName
import app.umaia.android.ui.theme.Gold
import app.umaia.android.ui.theme.TC

@Composable
fun ResourceBar(
    resources: ResourceMap,
    defs: List<ResourceDef>,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        defs.forEach { def ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(def.icon, fontSize = 18.sp)
                Text(
                    text = "%.1f".format(resources[def.id]),
                    color = Gold,
                    fontSize = 11.sp
                )
                Text(def.localizedName(s.code), color = TC.text, fontSize = 9.sp, maxLines = 1)
            }
        }
    }
}
