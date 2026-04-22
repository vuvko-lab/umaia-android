package app.umaia.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.umaia.android.domain.model.BuildingId
import app.umaia.android.domain.model.GameState
import app.umaia.android.ui.theme.Gold
import java.util.Calendar
import kotlin.math.*

// ── Season ───────────────────────────────────────────────────────────────────

private enum class Season { SPRING, SUMMER, AUTUMN, WINTER }

private fun currentSeason(): Season {
    return when (Calendar.getInstance().get(Calendar.MONTH) + 1) {
        in 3..5 -> Season.SPRING
        in 6..8 -> Season.SUMMER
        in 9..11 -> Season.AUTUMN
        else -> Season.WINTER
    }
}

private data class SeasonColors(
    val skyTop: Color,
    val skyBottom: Color,
    val ground: Color,
    val groundRim: Color,
    val groundHighlight: Color,
    val stars: Color,
    val ambient: Color,
    val grassDark: Color,
    val grassLight: Color,
    val pathColor: Color
)

private fun seasonColors(season: Season) = when (season) {
    Season.SPRING -> SeasonColors(
        skyTop = Color(0.03f, 0.08f, 0.14f),
        skyBottom = Color(0.08f, 0.16f, 0.28f),
        ground = Color(0.23f, 0.42f, 0.19f),
        groundRim = Color(0.12f, 0.25f, 0.10f),
        groundHighlight = Color(0.30f, 0.50f, 0.25f),
        stars = Color(1f, 0.93f, 0.67f),
        ambient = Color(0.15f, 0.20f, 0.35f),
        grassDark = Color(0.18f, 0.35f, 0.14f),
        grassLight = Color(0.32f, 0.55f, 0.22f),
        pathColor = Color(0.35f, 0.30f, 0.20f)
    )
    Season.SUMMER -> SeasonColors(
        skyTop = Color(0.02f, 0.05f, 0.12f),
        skyBottom = Color(0.08f, 0.16f, 0.35f),
        ground = Color(0.29f, 0.55f, 0.19f),
        groundRim = Color(0.16f, 0.35f, 0.10f),
        groundHighlight = Color(0.36f, 0.62f, 0.26f),
        stars = Color(1f, 1f, 0.87f),
        ambient = Color(0.12f, 0.18f, 0.35f),
        grassDark = Color(0.22f, 0.45f, 0.14f),
        grassLight = Color(0.38f, 0.65f, 0.24f),
        pathColor = Color(0.38f, 0.32f, 0.22f)
    )
    Season.AUTUMN -> SeasonColors(
        skyTop = Color(0.08f, 0.04f, 0.12f),
        skyBottom = Color(0.14f, 0.11f, 0.18f),
        ground = Color(0.48f, 0.42f, 0.19f),
        groundRim = Color(0.30f, 0.25f, 0.10f),
        groundHighlight = Color(0.56f, 0.50f, 0.26f),
        stars = Color(1f, 0.87f, 0.67f),
        ambient = Color(0.18f, 0.14f, 0.20f),
        grassDark = Color(0.40f, 0.34f, 0.14f),
        grassLight = Color(0.52f, 0.46f, 0.22f),
        pathColor = Color(0.42f, 0.35f, 0.18f)
    )
    Season.WINTER -> SeasonColors(
        skyTop = Color(0.02f, 0.02f, 0.06f),
        skyBottom = Color(0.08f, 0.10f, 0.18f),
        ground = Color(0.75f, 0.78f, 0.82f),
        groundRim = Color(0.45f, 0.50f, 0.58f),
        groundHighlight = Color(0.85f, 0.88f, 0.92f),
        stars = Color(0.80f, 0.87f, 1f),
        ambient = Color(0.10f, 0.12f, 0.22f),
        grassDark = Color(0.65f, 0.68f, 0.72f),
        grassLight = Color(0.80f, 0.83f, 0.88f),
        pathColor = Color(0.55f, 0.52f, 0.50f)
    )
}

// ── Building slots ────────────────────────────────────────────────────────────

private data class BuildingSlot(val id: BuildingId, val cx: Float, val cy: Float, val scale: Float)

private val buildingSlots = listOf(
    BuildingSlot(BuildingId.TANNERY,  0.58f, 0.30f, 0.75f),
    BuildingSlot(BuildingId.WORKSHOP, 0.30f, 0.30f, 0.75f),
    BuildingSlot(BuildingId.PASTURE,  0.74f, 0.36f, 0.78f),
    BuildingSlot(BuildingId.WOODCAMP, 0.16f, 0.40f, 0.80f),
    BuildingSlot(BuildingId.YURT,     0.62f, 0.46f, 0.90f),
    BuildingSlot(BuildingId.HEARTH,   0.48f, 0.56f, 0.92f),
    BuildingSlot(BuildingId.WELL,     0.62f, 0.64f, 1.0f),
    BuildingSlot(BuildingId.LOOM,     0.24f, 0.64f, 1.0f),
    BuildingSlot(BuildingId.BREWERY,  0.78f, 0.62f, 1.0f),
    BuildingSlot(BuildingId.CIRCLE,   0.48f, 0.76f, 1.05f),
)

// ── VillageScene ──────────────────────────────────────────────────────────────

@Composable
fun VillageScene(
    state: GameState,
    onPopulationClick: () -> Unit,
    onSpiritClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val season = remember { currentSeason() }
    val colors = remember(season) { seasonColors(season) }

    val built = remember(state.buildings) { state.buildings.map { it.type }.toSet() }
    val counts = remember(state.buildings) {
        state.buildings.groupBy { it.type }.mapValues { it.value.size }
    }

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "village")
    val firePhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "fire"
    )
    val starPhase by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "stars"
    )
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )

    // Tilt state
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }

    // Particle data (stable random positions)
    val particles = remember {
        (0 until 12).map { i ->
            val rng = java.util.Random(i.toLong() * 1234567L)
            Triple(
                0.1f + rng.nextFloat() * 0.8f,  // x
                rng.nextFloat() * 2.5f,           // delay
                2f + rng.nextFloat() * 2f          // size
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .defaultMinSize(minHeight = 220.dp)
            .clip(RoundedCornerShape(16.dp))
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        tiltX = (tiltX + dragAmount.x / 150f).coerceIn(-1f, 1f)
                        tiltY = (tiltY + dragAmount.y / 150f).coerceIn(-1f, 1f)
                    },
                    onDragEnd = { tiltX = 0f; tiltY = 0f },
                    onDragCancel = { tiltX = 0f; tiltY = 0f }
                )
            }
    ) {
        val scale = 1f + breathe * 0.008f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationX = tiltY * 3f
                    rotationY = tiltX * 3f
                    translationX = tiltX * 2f
                    translationY = tiltY * 1.5f
                    cameraDistance = 8f * density
                }
        ) {
            drawBackground(colors, starPhase)
            drawGround(colors)
            drawGrassPatches(colors)
            drawPaths(colors, built)
            drawMainYurt(colors)

            // Draw buildings back-to-front by cy
            buildingSlots.sortedBy { it.cy }.forEach { slot ->
                if (built.contains(slot.id)) {
                    drawBuilding(slot, colors, firePhase)
                }
            }

            // Atmospheric depth overlays
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent),
                    startY = 0f, endY = size.height * 0.14f
                ),
                size = Size(size.width, size.height * 0.14f)
            )
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                    startY = size.height * 0.72f, endY = size.height
                ),
                topLeft = Offset(0f, size.height * 0.72f),
                size = Size(size.width, size.height * 0.28f)
            )

            // Fire glow overlay
            if (built.contains(BuildingId.HEARTH)) {
                val centerX = size.width * 0.48f
                val centerY = size.height * 0.58f
                val radius = size.width * 0.35f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(1f, 0.6f, 0.15f, 0.25f + firePhase * 0.15f),
                            Color(1f, 0.4f, 0.05f, 0.12f + firePhase * 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            }

            // Floating gold particles
            particles.forEachIndexed { i, (px, delay, sz) ->
                val particleX = px * size.width + sin(breathe * PI.toFloat() + i) * 4f
                val particleY = size.height * (0.4f + 0.5f * (1f - breathe)) - delay * 20f
                val alpha = (0.3f + breathe * 0.4f).coerceIn(0f, 0.7f)
                drawCircle(
                    color = Color(0.85f, 0.68f, 0.15f, alpha),
                    radius = sz / 2f,
                    center = Offset(particleX, particleY)
                )
            }
        }

        // Building count badges (overlay via Box)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val h = constraints.maxHeight.toFloat()
            counts.forEach { (type, count) ->
                if (count > 1) {
                    val slot = buildingSlots.find { it.id == type } ?: return@forEach
                    val badgeX = (slot.cx * w + 14f * slot.scale).dp
                    val badgeY = (slot.cy * h - 10f * slot.scale).dp
                    Box(
                        modifier = Modifier
                            .offset(x = badgeX - 12.dp, y = badgeY - 8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.55f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "×$count",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Gold border overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0.82f, 0.62f, 0.22f, 0.15f),
                        Color.Transparent,
                        Color(0.82f, 0.62f, 0.22f, 0.08f)
                    )
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1f)
            )
        }
    }
}

// ── Background ────────────────────────────────────────────────────────────────

private fun DrawScope.drawBackground(colors: SeasonColors, starPhase: Float) {
    // Sky gradient
    drawRect(
        brush = Brush.verticalGradient(
            listOf(colors.skyTop, colors.skyBottom, colors.ambient)
        )
    )

    // Moon glow
    val moonX = size.width * 0.82f
    val moonY = size.height * 0.10f
    val moonR = 14f
    drawCircle(
        color = Color(0.7f, 0.75f, 0.9f, 0.08f),
        radius = moonR * 2.5f,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = Color(0.8f, 0.85f, 0.95f, 0.12f),
        radius = moonR * 1.5f,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = Color(0.9f, 0.92f, 0.95f, 0.85f),
        radius = moonR,
        center = Offset(moonX, moonY)
    )

    // Stars
    val starPositions = listOf(
        Triple(0.10f, 0.08f, 1.8f), Triple(0.25f, 0.05f, 2.2f), Triple(0.45f, 0.10f, 1.5f),
        Triple(0.60f, 0.04f, 2.5f), Triple(0.75f, 0.09f, 1.2f), Triple(0.88f, 0.06f, 2.0f),
        Triple(0.15f, 0.18f, 1.3f), Triple(0.35f, 0.14f, 1.8f), Triple(0.55f, 0.18f, 1.4f),
        Triple(0.80f, 0.15f, 1.6f), Triple(0.92f, 0.20f, 1.1f), Triple(0.05f, 0.25f, 1.5f),
        Triple(0.70f, 0.22f, 1.7f), Triple(0.50f, 0.03f, 2.8f), Triple(0.95f, 0.12f, 1.3f),
        Triple(0.42f, 0.07f, 1.0f), Triple(0.68f, 0.16f, 0.9f), Triple(0.22f, 0.22f, 1.1f),
        Triple(0.78f, 0.02f, 1.4f)
    )
    starPositions.forEachIndexed { i, (sx, sy, r) ->
        val alpha = when (i % 3) {
            0 -> starPhase
            1 -> 1f - starPhase
            else -> 0.7f
        }
        drawCircle(
            color = colors.stars.copy(alpha = alpha * 0.8f),
            radius = r,
            center = Offset(sx * size.width, sy * size.height)
        )
    }

    drawMountains(colors)
}

private fun DrawScope.drawMountains(colors: SeasonColors) {
    val w = size.width
    val h = size.height

    // Far mountains
    val farPath = Path().apply {
        moveTo(0f, h * 0.45f)
        lineTo(w * 0.12f, h * 0.28f)
        lineTo(w * 0.22f, h * 0.34f)
        lineTo(w * 0.35f, h * 0.22f)
        lineTo(w * 0.48f, h * 0.30f)
        lineTo(w * 0.58f, h * 0.25f)
        lineTo(w * 0.72f, h * 0.32f)
        lineTo(w * 0.85f, h * 0.20f)
        lineTo(w * 0.95f, h * 0.30f)
        lineTo(w, h * 0.38f)
        lineTo(w, h * 0.45f)
        close()
    }
    drawPath(farPath, color = colors.ambient.copy(alpha = 0.35f))

    // Near mountains
    val nearPath = Path().apply {
        moveTo(0f, h * 0.45f)
        lineTo(w * 0.08f, h * 0.36f)
        lineTo(w * 0.18f, h * 0.40f)
        lineTo(w * 0.30f, h * 0.32f)
        lineTo(w * 0.42f, h * 0.38f)
        lineTo(w * 0.55f, h * 0.35f)
        lineTo(w * 0.68f, h * 0.40f)
        lineTo(w * 0.80f, h * 0.34f)
        lineTo(w * 0.92f, h * 0.39f)
        lineTo(w, h * 0.42f)
        lineTo(w, h * 0.45f)
        close()
    }
    drawPath(nearPath, color = colors.ambient.copy(alpha = 0.5f))
}

// ── Ground ────────────────────────────────────────────────────────────────────

private fun DrawScope.drawGround(colors: SeasonColors) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.74f
    val rx = size.width * 0.52f
    val ry = size.height * 0.34f

    drawOval(Color.Black.copy(alpha = 0.35f), topLeft = Offset(cx - rx - 8, cy - ry + 6), size = Size((rx + 8) * 2, (ry + 4) * 2))
    drawOval(colors.groundRim, topLeft = Offset(cx - rx - 3, cy - ry - 1), size = Size((rx + 3) * 2, (ry + 1) * 2))
    drawOval(colors.ground, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))

    val hlRx = rx * 0.7f
    val hlRy = ry * 0.5f
    drawOval(
        colors.groundHighlight.copy(alpha = 0.35f),
        topLeft = Offset(cx - hlRx - rx * 0.08f, cy - ry * 0.6f - hlRy * 0.4f),
        size = Size(hlRx * 2, hlRy * 2)
    )
}

private fun DrawScope.drawGrassPatches(colors: SeasonColors) {
    val patches = listOf(
        Triple(0.15f, 0.58f, 3f), Triple(0.82f, 0.55f, 2.5f), Triple(0.35f, 0.72f, 3.5f),
        Triple(0.68f, 0.78f, 2.8f), Triple(0.50f, 0.85f, 3.2f), Triple(0.22f, 0.48f, 2.2f),
        Triple(0.88f, 0.68f, 2.6f), Triple(0.42f, 0.62f, 2f), Triple(0.12f, 0.70f, 2.4f),
        Triple(0.75f, 0.48f, 2.1f), Triple(0.58f, 0.70f, 2.7f), Triple(0.30f, 0.55f, 1.8f),
        Triple(0.90f, 0.75f, 2.3f), Triple(0.20f, 0.82f, 3f), Triple(0.65f, 0.58f, 2f)
    )
    val paintColor = colors.grassLight.copy(alpha = 0.5f)
    patches.forEach { (px, py, h) ->
        val x = px * size.width
        val y = py * size.height
        listOf(-1.5f, 0f, 1.5f).forEach { offset ->
            drawLine(
                color = paintColor,
                start = Offset(x + offset, y),
                end = Offset(x + offset * 0.5f, y - h),
                strokeWidth = 0.8f
            )
        }
    }
}

private fun DrawScope.drawPaths(colors: SeasonColors, built: Set<BuildingId>) {
    val pathW = size.width * 0.02f
    val yurtX = size.width * 0.38f
    val yurtY = size.height * 0.64f
    val pathColor = colors.pathColor.copy(alpha = 0.35f)

    val mainPath = Path().apply {
        moveTo(yurtX + size.width * 0.06f, yurtY)
        quadraticBezierTo(
            size.width * 0.44f, size.height * 0.68f,
            size.width * 0.50f, size.height * 0.72f
        )
    }
    drawPath(mainPath, color = pathColor, style = Stroke(width = pathW))

    if (built.contains(BuildingId.WELL)) {
        val wellPath = Path().apply {
            moveTo(size.width * 0.50f, size.height * 0.72f)
            quadraticBezierTo(
                size.width * 0.56f, size.height * 0.67f,
                size.width * 0.62f, size.height * 0.64f
            )
        }
        drawPath(wellPath, color = colors.pathColor.copy(alpha = 0.30f), style = Stroke(width = pathW * 0.8f))
    }
}

// ── Main Yurt ─────────────────────────────────────────────────────────────────

private fun DrawScope.drawMainYurt(colors: SeasonColors) {
    val cx = size.width * 0.38f
    val cy = size.height * 0.60f
    val r = size.width * 0.09f

    val feltLight = Color(0.85f, 0.75f, 0.58f)
    val feltMid   = Color(0.72f, 0.60f, 0.42f)
    val feltDark  = Color(0.52f, 0.42f, 0.28f)
    val roofDark  = Color(0.48f, 0.38f, 0.24f)
    val roofLight = Color(0.62f, 0.52f, 0.36f)
    val ornamentGold = Color(0.82f, 0.62f, 0.22f)
    val ornamentRed  = Color(0.72f, 0.22f, 0.15f)
    val woodDark     = Color(0.35f, 0.25f, 0.14f)

    // Ground shadow
    drawOval(Color.Black.copy(alpha = 0.25f), topLeft = Offset(cx - r * 1.4f, cy + r * 0.1f), size = Size(r * 2.8f, r * 0.5f))

    // Base ellipse
    drawOval(feltMid.copy(alpha = 0.8f), topLeft = Offset(cx - r * 1.1f, cy - r * 0.3f), size = Size(r * 2.2f, r * 0.65f))

    // Left wall (darker/shadow side)
    drawRect(feltDark, topLeft = Offset(cx - r, cy - r), size = Size(r, r))
    // Right wall (lit side)
    drawRect(feltLight, topLeft = Offset(cx, cy - r), size = Size(r, r))

    // Kerege (lattice hint - diagonal lines on walls)
    val latticeColor = woodDark.copy(alpha = 0.12f)
    for (i in 0 until 8) {
        val lx = cx - r + i * (r * 2f / 8f)
        drawLine(
            color = latticeColor,
            start = Offset(lx, cy - r * 0.15f),
            end = Offset(lx + r * 0.1f, cy - r * 0.9f),
            strokeWidth = 0.6f
        )
    }

    // Ornament bands
    drawRect(ornamentGold.copy(alpha = 0.7f), topLeft = Offset(cx - r * 0.98f, cy - r * 0.62f), size = Size(r * 1.96f, 2.5f))
    drawRect(ornamentRed.copy(alpha = 0.5f), topLeft = Offset(cx - r * 0.98f, cy - r * 0.50f), size = Size(r * 1.96f, 2f))

    // Dome / roof
    val domePath = Path().apply {
        moveTo(cx - r, cy - r * 0.9f)
        cubicTo(
            cx - r * 0.6f, cy - r * 2.6f,
            cx + r * 0.6f, cy - r * 2.6f,
            cx + r, cy - r * 0.9f
        )
        close()
    }
    drawPath(domePath, color = roofDark)

    // Dome highlight (right side)
    val domeHL = Path().apply {
        moveTo(cx + r * 0.05f, cy - r * 0.9f)
        cubicTo(
            cx + r * 0.05f, cy - r * 2.3f,
            cx + r * 0.85f, cy - r * 2.25f,
            cx + r * 0.9f, cy - r * 0.9f
        )
        close()
    }
    drawPath(domeHL, color = roofLight.copy(alpha = 0.35f))

    // Ornament band at roof base
    drawRect(ornamentGold.copy(alpha = 0.5f), topLeft = Offset(cx - r * 0.98f, cy - r * 0.98f), size = Size(r * 1.96f, 2f))

    // Uyk poles (8 radial lines from shanyrak)
    val shY = cy - r * 2.35f
    for (i in 0 until 8) {
        val angle = (i.toFloat() / 8f) * PI.toFloat() + 0.1f
        val px1 = cx + cos(angle) * r * 0.12f
        val py1 = shY + sin(angle) * r * 0.08f
        val px2 = cx + cos(angle) * r * 0.85f
        val py2 = cy - r * 0.92f + sin(angle) * r * 0.05f
        drawLine(woodDark.copy(alpha = 0.2f), Offset(px1, py1), Offset(px2, py2), strokeWidth = 0.8f)
    }

    // Shanyrak (crown at apex)
    val shR = r * 0.22f
    drawOval(ornamentGold, topLeft = Offset(cx - shR, shY - shR * 0.5f), size = Size(shR * 2, shR * 0.9f))
    drawOval(Color.Transparent, topLeft = Offset(cx - shR, shY - shR * 0.5f), size = Size(shR * 2, shR * 0.9f),
        style = Stroke(width = 1.2f))
    // Shanyrak cross-hatch
    drawLine(woodDark, Offset(cx - shR * 0.6f, shY - shR * 0.1f), Offset(cx + shR * 0.6f, shY - shR * 0.1f), strokeWidth = 1f)
    drawLine(woodDark, Offset(cx, shY - shR * 0.45f), Offset(cx, shY + shR * 0.2f), strokeWidth = 1f)

    // Door (arched)
    val doorW = r * 0.32f
    val doorH = r * 0.75f
    val doorPath = Path().apply {
        moveTo(cx - doorW / 2, cy - r * 0.95f + doorH)
        lineTo(cx - doorW / 2, cy - r * 0.95f + doorH * 0.45f)
        cubicTo(
            cx - doorW / 2, cy - r * 0.95f,
            cx + doorW / 2, cy - r * 0.95f,
            cx + doorW / 2, cy - r * 0.95f + doorH * 0.45f
        )
        lineTo(cx + doorW / 2, cy - r * 0.95f + doorH)
        close()
    }
    drawPath(doorPath, color = woodDark.copy(alpha = 0.9f))
    // Door fill (very dark interior)
    val innerDoorPath = Path().apply {
        moveTo(cx - doorW / 2 + 1.5f, cy - r * 0.93f + doorH)
        lineTo(cx - doorW / 2 + 1.5f, cy - r * 0.93f + doorH * 0.45f)
        cubicTo(
            cx - doorW / 2 + 1.5f, cy - r * 0.93f + 3f,
            cx + doorW / 2 - 1.5f, cy - r * 0.93f + 3f,
            cx + doorW / 2 - 1.5f, cy - r * 0.93f + doorH * 0.45f
        )
        lineTo(cx + doorW / 2 - 1.5f, cy - r * 0.93f + doorH)
        close()
    }
    drawPath(innerDoorPath, color = Color(0.12f, 0.08f, 0.04f))
    // Warm glow from doorway
    drawRect(
        color = Color(0.8f, 0.5f, 0.15f, 0.12f),
        topLeft = Offset(cx - doorW / 2 + 2f, cy - r * 0.75f),
        size = Size(doorW - 4f, doorH * 0.35f)
    )
}

// ── Building dispatcher ───────────────────────────────────────────────────────

private fun DrawScope.drawBuilding(slot: BuildingSlot, colors: SeasonColors, flicker: Float) {
    val x = slot.cx * size.width
    val y = slot.cy * size.height
    val s = slot.scale
    when (slot.id) {
        BuildingId.PASTURE  -> drawPasture(x, y, s, colors)
        BuildingId.WELL     -> drawWell(x, y, s)
        BuildingId.HEARTH   -> drawHearth(flicker)
        BuildingId.LOOM     -> drawLoom(x, y, s)
        BuildingId.TANNERY  -> drawTannery(x, y, s)
        BuildingId.WOODCAMP -> drawWoodcamp(x, y, s, colors)
        BuildingId.BREWERY  -> drawBrewery(x, y, s)
        BuildingId.WORKSHOP -> drawWorkshop(x, y, s)
        BuildingId.CIRCLE   -> drawCircle(x, y, s, flicker)
        BuildingId.YURT     -> drawSmallYurt(x, y, s)
        else -> { /* other building types not rendered as canvas drawings */ }
    }
}

// ── Pasture ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawPasture(x: Float, y: Float, s: Float, colors: SeasonColors) {
    val w = size.width * 0.14f * s
    val h = size.height * 0.12f * s
    val fx = x - w / 2f
    val fy = y - h

    val fenceColor = Color(0.58f, 0.42f, 0.22f)
    val fenceLight = Color(0.72f, 0.56f, 0.32f)
    val horseBody  = Color(0.45f, 0.30f, 0.18f)
    val horseLight = Color(0.55f, 0.40f, 0.25f)
    val maneColor  = Color(0.28f, 0.18f, 0.10f)

    // Shadow
    drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(fx - 2f, y - 2f), size = Size(w + 4f, h * 0.15f))

    // Fence posts (5 posts)
    for (i in 0..4) {
        val px = fx + i * (w / 4f)
        drawRect(fenceColor, topLeft = Offset(px - 1.5f, fy), size = Size(3f, h + 2f))
    }
    // Rails (2 horizontal)
    drawRect(fenceLight, topLeft = Offset(fx - 1f, fy + h * 0.28f), size = Size(w + 2f, 2.5f))
    drawRect(fenceLight, topLeft = Offset(fx - 1f, fy + h * 0.62f), size = Size(w + 2f, 2.5f))

    // Horse 1 (left)
    val h1x = fx + w * 0.22f
    val h1y = y - h * 0.28f
    val br = size.width * 0.012f * s
    drawOval(horseBody, topLeft = Offset(h1x - br * 1.5f, h1y - br), size = Size(br * 3f, br * 2f))
    drawOval(horseLight, topLeft = Offset(h1x - br * 1.5f, h1y - br), size = Size(br * 1.5f, br * 2f))
    // Horse 1 head
    drawOval(horseBody, topLeft = Offset(h1x + br * 1.2f, h1y - br * 1.5f), size = Size(br * 1.2f, br))
    // Horse 1 legs
    for (legOff in listOf(-br * 0.8f, -br * 0.2f, br * 0.4f, br)) {
        drawLine(horseBody, Offset(h1x + legOff, h1y + br * 0.9f), Offset(h1x + legOff, h1y + br * 2.1f), strokeWidth = 1.5f)
    }
    // Mane
    drawLine(maneColor, Offset(h1x + br * 1.5f, h1y - br * 1.4f), Offset(h1x + br * 0.5f, h1y - br * 0.8f), strokeWidth = 1.5f)

    // Horse 2 (right, slightly smaller)
    val h2x = fx + w * 0.68f
    val h2y = y - h * 0.32f
    val br2 = br * 0.85f
    drawOval(horseBody, topLeft = Offset(h2x - br2 * 1.5f, h2y - br2), size = Size(br2 * 3f, br2 * 2f))
    drawOval(horseLight, topLeft = Offset(h2x - br2 * 1.5f, h2y - br2), size = Size(br2 * 1.5f, br2 * 2f))
    drawOval(horseBody, topLeft = Offset(h2x + br2 * 1.2f, h2y - br2 * 1.5f), size = Size(br2 * 1.2f, br2))
    for (legOff in listOf(-br2 * 0.8f, -br2 * 0.2f, br2 * 0.4f, br2)) {
        drawLine(horseBody, Offset(h2x + legOff, h2y + br2 * 0.9f), Offset(h2x + legOff, h2y + br2 * 2.1f), strokeWidth = 1.2f)
    }
    drawLine(maneColor, Offset(h2x + br2 * 1.5f, h2y - br2 * 1.4f), Offset(h2x + br2 * 0.5f, h2y - br2 * 0.8f), strokeWidth = 1.2f)
}

// ── Well ──────────────────────────────────────────────────────────────────────

private fun DrawScope.drawWell(x: Float, y: Float, s: Float) {
    val r = size.width * 0.04f * s
    val stoneColor = Color(0.48f, 0.44f, 0.38f)
    val stoneDark  = Color(0.32f, 0.28f, 0.24f)
    val stoneLight = Color(0.58f, 0.54f, 0.46f)
    val woodColor  = Color(0.55f, 0.38f, 0.20f)
    val roofColor  = Color(0.42f, 0.28f, 0.14f)

    // Shadow
    drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(x - r * 1.4f, y - r * 0.3f), size = Size(r * 2.8f, r * 0.55f))

    // Stone wall base
    val wallPath = Path().apply {
        addOval(Rect(x - r, y - r * 1.2f, x + r, y))
    }
    drawPath(wallPath, color = stoneColor)
    drawPath(wallPath, color = stoneDark, style = Stroke(width = 1.5f))

    // Stone courses
    for (i in 0 until 4) {
        val bandY = y - r * 0.25f - i * r * 0.22f
        drawOval(stoneLight.copy(alpha = 0.2f), topLeft = Offset(x - r * 0.9f, bandY), size = Size(r * 1.8f, r * 0.12f))
    }

    // Posts
    drawRect(woodColor, topLeft = Offset(x - r * 0.9f, y - r * 2.2f), size = Size(r * 0.22f, r * 1.2f))
    drawRect(woodColor, topLeft = Offset(x + r * 0.7f, y - r * 2.2f), size = Size(r * 0.22f, r * 1.2f))

    // Roof beam (crossbar)
    drawRect(woodColor, topLeft = Offset(x - r * 0.95f, y - r * 2.25f), size = Size(r * 1.9f, r * 0.18f))

    // Roof tiles
    val roofPath = Path().apply {
        moveTo(x - r * 1.1f, y - r * 2.05f)
        lineTo(x, y - r * 2.55f)
        lineTo(x + r * 1.1f, y - r * 2.05f)
        close()
    }
    drawPath(roofPath, color = roofColor)
    drawPath(roofPath, color = stoneLight.copy(alpha = 0.2f), style = Stroke(width = 1f))

    // Rope
    drawLine(Color(0.60f, 0.50f, 0.35f), Offset(x, y - r * 2.1f), Offset(x, y - r * 1.1f), strokeWidth = 1f)

    // Bucket
    val bucketY = y - r * 0.9f
    drawRect(Color(0.55f, 0.38f, 0.20f), topLeft = Offset(x - r * 0.14f, bucketY - r * 0.3f), size = Size(r * 0.28f, r * 0.3f))
    drawOval(Color(0.65f, 0.48f, 0.28f), topLeft = Offset(x - r * 0.16f, bucketY - r * 0.35f), size = Size(r * 0.32f, r * 0.1f))

    // Water shimmer
    drawOval(Color(0.38f, 0.55f, 0.72f, 0.5f), topLeft = Offset(x - r * 0.5f, y - r * 0.65f), size = Size(r, r * 0.25f))
}

// ── Hearth ────────────────────────────────────────────────────────────────────

private fun DrawScope.drawHearth(flicker: Float) {
    val cx = size.width * 0.48f
    val cy = size.height * 0.63f
    val r = size.width * 0.035f

    // Fire ground glow
    drawOval(
        Color(1f, 0.5f, 0.1f, 0.06f + flicker * 0.04f),
        topLeft = Offset(cx - r * 5, cy - r * 2.5f),
        size = Size(r * 10, r * 5)
    )

    // Stone ring shadow
    drawOval(Color.Black.copy(alpha = 0.25f), topLeft = Offset(cx - r * 1.5f, cy - r * 0.8f), size = Size(r * 3, r * 1.6f))

    // Stones
    val stonePositions = listOf(
        Pair(-1.1f, -0.5f), Pair(-0.5f, -0.7f), Pair(0.2f, -0.7f), Pair(0.8f, -0.5f),
        Pair(1.0f, 0.0f), Pair(0.7f, 0.4f), Pair(0.0f, 0.5f), Pair(-0.6f, 0.4f), Pair(-1.0f, 0.0f)
    )
    stonePositions.forEach { (ox, oy) ->
        val sx = cx + ox * r
        val sy = cy + oy * r
        drawOval(Color(0.38f, 0.36f, 0.33f), topLeft = Offset(sx - r * 0.3f, sy - r * 0.25f), size = Size(r * 0.6f, r * 0.5f))
        drawOval(Color(0.50f, 0.48f, 0.44f, 0.5f), topLeft = Offset(sx - r * 0.15f, sy - r * 0.2f), size = Size(r * 0.25f, r * 0.2f))
    }

    // Logs
    val logColor = Color(0.40f, 0.25f, 0.12f)
    drawLine(logColor, Offset(cx - r * 0.6f, cy + r * 0.1f), Offset(cx + r * 0.6f, cy - r * 0.2f), strokeWidth = 2.5f)
    drawLine(logColor, Offset(cx + r * 0.5f, cy + r * 0.15f), Offset(cx - r * 0.5f, cy - r * 0.15f), strokeWidth = 2.5f)

    // Fire outer
    val fireH = r * (2.2f + flicker * 0.6f)
    val firePath = Path().apply {
        moveTo(cx, cy - fireH)
        cubicTo(cx + r * 0.9f, cy - fireH * 0.35f, cx + r * 0.6f, cy, cx, cy - r * 0.1f)
        cubicTo(cx - r * 0.6f, cy, cx - r * 0.9f, cy - fireH * 0.35f, cx, cy - fireH)
        close()
    }
    drawPath(firePath, color = Color(1f, 0.38f, 0.08f, 0.9f))

    // Fire mid
    val midH = fireH * 0.72f
    val midPath = Path().apply {
        moveTo(cx, cy - midH)
        cubicTo(cx + r * 0.5f, cy - midH * 0.3f, cx + r * 0.4f, cy, cx, cy - r * 0.1f)
        cubicTo(cx - r * 0.4f, cy, cx - r * 0.5f, cy - midH * 0.3f, cx, cy - midH)
        close()
    }
    drawPath(midPath, color = Color(1f, 0.65f, 0f, 0.85f))

    // Fire inner
    val innerH = fireH * 0.42f
    val innerPath = Path().apply {
        moveTo(cx, cy - innerH)
        cubicTo(cx + r * 0.25f, cy - innerH * 0.25f, cx + r * 0.2f, cy - r * 0.2f, cx, cy - r * 0.2f)
        cubicTo(cx - r * 0.2f, cy - r * 0.2f, cx - r * 0.25f, cy - innerH * 0.25f, cx, cy - innerH)
        close()
    }
    drawPath(innerPath, color = Color(1f, 0.92f, 0.5f, 0.8f))

    // Sparks
    val sparkPositions = listOf(Pair(-0.3f, -2.8f), Pair(0.4f, -2.5f), Pair(-0.15f, -3.0f), Pair(0.25f, -3.2f))
    val sparkAlpha = if (flicker > 0.5f) 0.7f else 0.3f
    sparkPositions.forEach { (ox, oy) ->
        drawCircle(
            Color(1f, 0.8f, 0.3f, sparkAlpha),
            radius = 1f,
            center = Offset(cx + ox * r, cy + oy * r)
        )
    }
}

// ── Loom ──────────────────────────────────────────────────────────────────────

private fun DrawScope.drawLoom(x: Float, y: Float, s: Float) {
    val w = size.width * 0.06f * s
    val h = size.height * 0.08f * s
    val fx = x - w / 2f
    val fy = y - h

    val woodColor = Color(0.55f, 0.38f, 0.20f)
    val woodLight = Color(0.65f, 0.48f, 0.28f)

    drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(fx - 2f, y - 2f), size = Size(w + 4f, h * 0.2f))
    drawRect(woodColor, topLeft = Offset(fx, fy), size = Size(3f, h))
    drawRect(woodColor, topLeft = Offset(fx + w - 3f, fy), size = Size(3f, h))
    drawRect(woodLight, topLeft = Offset(fx - 2f, fy - 2f), size = Size(w + 4f, 3f))
    drawRect(woodLight, topLeft = Offset(fx - 1f, y - 2f), size = Size(w + 2f, 3f))

    val threadColors = listOf(
        Color(0.82f, 0.22f, 0.18f),
        Color(0.20f, 0.45f, 0.72f),
        Color(0.78f, 0.59f, 0.23f),
        Color(0.82f, 0.22f, 0.18f),
        Color(0.20f, 0.55f, 0.35f)
    )
    val threadSpacing = (w - 8f) / 4f
    for (i in 0 until 5) {
        val tx = fx + 4f + i * threadSpacing
        drawLine(threadColors[i].copy(alpha = 0.8f), Offset(tx, fy + 2f), Offset(tx, y - 3f), strokeWidth = 1f)
    }

    // Woven bands
    val weaveStart = fy + h * 0.4f
    val weaveEnd = y - 4f
    var bandY = weaveStart
    var colorIdx = 0
    while (bandY < weaveEnd) {
        drawRect(
            threadColors[colorIdx % 5].copy(alpha = 0.5f),
            topLeft = Offset(fx + 4f, bandY),
            size = Size(w - 8f, 2f)
        )
        bandY += 3f
        colorIdx++
    }
}

// ── Tannery ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawTannery(x: Float, y: Float, s: Float) {
    val w = size.width * 0.055f * s
    val h = size.height * 0.06f * s
    val fx = x - w / 2f
    val fy = y - h
    val woodColor = Color(0.50f, 0.35f, 0.18f)
    val hideColor = Color(0.72f, 0.58f, 0.40f)
    val hideDark  = Color(0.58f, 0.44f, 0.28f)

    drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(fx - 2f, y - 1f), size = Size(w + 4f, h * 0.2f))

    // A-frame posts
    drawLine(woodColor, Offset(fx + 2f, y), Offset(fx + w * 0.15f, fy), strokeWidth = 2.5f)
    drawLine(woodColor, Offset(fx + w - 2f, y), Offset(fx + w * 0.85f, fy), strokeWidth = 2.5f)

    // Crossbar
    drawRect(woodColor, topLeft = Offset(fx + w * 0.1f, fy), size = Size(w * 0.8f, 2.5f))

    // Hide
    val hidePath = Path().apply {
        moveTo(fx + w * 0.18f, fy + 3f)
        cubicTo(fx + w * 0.3f, fy + h * 0.7f, fx + w * 0.7f, fy + h * 0.7f, fx + w * 0.82f, fy + 3f)
        close()
    }
    drawPath(hidePath, color = hideColor)
    drawPath(hidePath, color = hideDark.copy(alpha = 0.3f))

    // Tool leaning
    drawLine(Color(0.40f, 0.30f, 0.18f), Offset(fx + w + 3f, y), Offset(fx + w - 2f, fy + h * 0.3f), strokeWidth = 1.5f)
}

// ── Woodcamp ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawWoodcamp(x: Float, y: Float, s: Float, colors: SeasonColors) {
    val w = size.width * 0.06f * s
    val fx = x - w / 2f
    val logColor = Color(0.50f, 0.35f, 0.18f)
    val logEnd   = Color(0.72f, 0.58f, 0.38f)
    val logDark  = Color(0.38f, 0.25f, 0.12f)

    drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(fx - 3f, y - 2f), size = Size(w + 6f, 6f))

    // Bottom row: 3 logs
    for (i in 0 until 3) {
        val lx = fx + 3f + i * (w * 0.3f)
        val ly = y - 6f
        val logR = w * 0.12f
        drawRect(logColor, topLeft = Offset(lx - logR, ly - logR * 1.5f), size = Size(logR * 2, logR * 3))
        drawOval(logEnd, topLeft = Offset(lx - logR, ly - logR), size = Size(logR * 2, logR * 2))
        drawOval(Color.Transparent, topLeft = Offset(lx - logR * 0.5f, ly - logR * 0.5f), size = Size(logR, logR),
            style = Stroke(width = 0.5f))
    }
    // Top row: 2 logs
    for (i in 0 until 2) {
        val lx = fx + 3f + w * 0.15f + i * (w * 0.3f)
        val ly = y - 6f - w * 0.22f
        val logR = w * 0.12f
        drawRect(logColor, topLeft = Offset(lx - logR, ly - logR * 1.5f), size = Size(logR * 2, logR * 3))
        drawOval(logEnd, topLeft = Offset(lx - logR, ly - logR), size = Size(logR * 2, logR * 2))
    }

    // Stump + axe
    val stumpX = fx + w + 5f
    val stumpY = y - 4f
    val stR = 4f
    drawRect(logColor, topLeft = Offset(stumpX - stR, stumpY - stR * 1.5f), size = Size(stR * 2, stR * 2))
    drawOval(logEnd, topLeft = Offset(stumpX - stR, stumpY - stR * 1.8f), size = Size(stR * 2, stR * 0.8f))
    drawLine(Color(0.45f, 0.32f, 0.16f), Offset(stumpX, stumpY - stR * 1.5f), Offset(stumpX + 5f, stumpY - stR * 4), strokeWidth = 1.5f)
    val axePath = Path().apply {
        moveTo(stumpX + 5f, stumpY - stR * 4)
        lineTo(stumpX + 8f, stumpY - stR * 4.5f)
        lineTo(stumpX + 8f, stumpY - stR * 3.5f)
        close()
    }
    drawPath(axePath, color = Color(0.55f, 0.55f, 0.58f))
}

// ── Brewery ───────────────────────────────────────────────────────────────────

private fun DrawScope.drawBrewery(x: Float, y: Float, s: Float) {
    val bW = size.width * 0.04f * s
    val bH = size.height * 0.055f * s
    val woodColor = Color(0.55f, 0.38f, 0.20f)
    val woodDark  = Color(0.40f, 0.25f, 0.12f)
    val bandColor = Color(0.45f, 0.42f, 0.38f)

    drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(x - bW - 4f, y - 2f), size = Size(bW * 2 + 8f, 5f))

    // Barrel body
    val barrelPath = Path().apply {
        moveTo(x - bW * 0.8f, y)
        cubicTo(x - bW * 1.1f, y - bH * 0.3f, x - bW * 1.1f, y - bH * 0.7f, x - bW * 0.8f, y - bH)
        lineTo(x + bW * 0.8f, y - bH)
        cubicTo(x + bW * 1.1f, y - bH * 0.7f, x + bW * 1.1f, y - bH * 0.3f, x + bW * 0.8f, y)
        close()
    }
    drawPath(barrelPath, color = woodColor)

    // Shadow side
    val shadowPath = Path().apply {
        moveTo(x - bW * 0.8f, y)
        cubicTo(x - bW * 1.1f, y - bH * 0.3f, x - bW * 1.1f, y - bH * 0.7f, x - bW * 0.8f, y - bH)
        lineTo(x, y - bH)
        lineTo(x, y)
        close()
    }
    drawPath(shadowPath, color = woodDark.copy(alpha = 0.35f))

    // Bands
    drawRect(bandColor, topLeft = Offset(x - bW * 1.05f, y - bH * 0.2f), size = Size(bW * 2.1f, 1.5f))
    drawRect(bandColor, topLeft = Offset(x - bW * 1.05f, y - bH * 0.8f), size = Size(bW * 2.1f, 1.5f))

    // Top
    drawOval(Color(0.62f, 0.45f, 0.25f), topLeft = Offset(x - bW * 0.85f, y - bH - 2f), size = Size(bW * 1.7f, 5f))

    // Small pot
    val potX = x + bW * 1.5f
    val potR = bW * 0.45f
    drawOval(Color(0.55f, 0.50f, 0.42f), topLeft = Offset(potX - potR, y - potR * 2.2f), size = Size(potR * 2, potR * 2.2f))
    drawOval(Color(0.62f, 0.56f, 0.48f), topLeft = Offset(potX - potR * 1.1f, y - potR * 2.4f), size = Size(potR * 2.2f, potR * 0.6f))
}

// ── Workshop ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawWorkshop(x: Float, y: Float, s: Float) {
    val w = size.width * 0.06f * s
    val h = size.height * 0.05f * s
    val fx = x - w / 2f
    val fy = y - h
    val woodColor = Color(0.52f, 0.38f, 0.22f)
    val woodLight = Color(0.62f, 0.48f, 0.30f)

    drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(fx - 2f, y - 1f), size = Size(w + 4f, 4f))
    drawRect(woodColor, topLeft = Offset(fx + 2f, fy + h * 0.35f), size = Size(2.5f, h * 0.65f))
    drawRect(woodColor, topLeft = Offset(fx + w - 4f, fy + h * 0.35f), size = Size(2.5f, h * 0.65f))
    drawRect(woodLight, topLeft = Offset(fx - 1f, fy + h * 0.25f), size = Size(w + 2f, h * 0.15f))
    drawRect(Color.White.copy(alpha = 0.12f), topLeft = Offset(fx - 1f, fy + h * 0.25f), size = Size(w + 2f, 1.5f))

    val coverPath = Path().apply {
        moveTo(fx - 3f, fy + h * 0.2f)
        lineTo(fx + w * 0.5f, fy - h * 0.3f)
        lineTo(fx + w + 3f, fy + h * 0.2f)
        close()
    }
    drawPath(coverPath, color = Color(0.60f, 0.42f, 0.22f))

    // Hammer handle
    drawLine(Color(0.45f, 0.32f, 0.18f), Offset(fx + w * 0.3f, fy + h * 0.15f), Offset(fx + w * 0.3f, fy - h * 0.1f), strokeWidth = 1.5f)
    drawRect(Color(0.55f, 0.55f, 0.58f), topLeft = Offset(fx + w * 0.22f, fy - h * 0.15f), size = Size(w * 0.16f, 3f))

    // Small crafted item
    drawOval(Color(0.78f, 0.59f, 0.23f), topLeft = Offset(fx + w * 0.55f, fy + h * 0.12f), size = Size(5f, 3f))
}

// ── Circle of Stories ─────────────────────────────────────────────────────────

private fun DrawScope.drawCircle(x: Float, y: Float, s: Float, flicker: Float) {
    val circleR = size.width * 0.04f * s

    // Glow
    drawOval(
        Color(0.6f, 0.5f, 0.8f, 0.06f + flicker * 0.03f),
        topLeft = Offset(x - circleR * 2, y - circleR * 1.5f),
        size = Size(circleR * 4, circleR * 3)
    )

    // 6 standing stones
    for (i in 0 until 6) {
        val angle = (i.toFloat() / 6f) * (2f * PI.toFloat()) - (PI.toFloat() / 2f)
        val sx = x + cos(angle) * circleR
        val sy = y + sin(angle) * circleR * 0.55f
        val stW = circleR * 0.3f
        val stH = circleR * 0.45f
        drawOval(Color(0.45f, 0.43f, 0.40f), topLeft = Offset(sx - stW / 2, sy - stH), size = Size(stW, stH))
        drawOval(Color(0.55f, 0.52f, 0.48f, 0.5f), topLeft = Offset(sx - stW * 0.3f, sy - stH * 0.8f), size = Size(stW * 0.4f, stH * 0.3f))
    }

    // Center totem
    val totemH = circleR * 0.8f
    drawRect(Color(0.50f, 0.35f, 0.18f), topLeft = Offset(x - 2f, y - totemH), size = Size(4f, totemH))
    drawOval(Color(0.78f, 0.59f, 0.23f), topLeft = Offset(x - 3.5f, y - totemH - 3f), size = Size(7f, 5f))

    // Note particles
    val noteAlpha = 0.3f + flicker * 0.3f
    drawOval(Color(0.7f, 0.6f, 0.9f, noteAlpha), topLeft = Offset(x - circleR * 0.5f, y - circleR * 1.2f), size = Size(3f, 2.5f))
    drawOval(Color(0.8f, 0.65f, 0.9f, noteAlpha * 0.7f), topLeft = Offset(x + circleR * 0.3f, y - circleR * 0.9f), size = Size(2.5f, 2f))
}

// ── Small Yurt ────────────────────────────────────────────────────────────────

private fun DrawScope.drawSmallYurt(x: Float, y: Float, s: Float) {
    val r = size.width * 0.055f * s
    val feltLight  = Color(0.82f, 0.72f, 0.55f)
    val feltMid    = Color(0.68f, 0.56f, 0.38f)
    val roofDark   = Color(0.46f, 0.36f, 0.22f)
    val roofLight  = Color(0.58f, 0.48f, 0.32f)
    val ornGold    = Color(0.82f, 0.62f, 0.22f)
    val ornRed     = Color(0.72f, 0.22f, 0.15f)
    val woodDark   = Color(0.35f, 0.25f, 0.14f)

    drawOval(Color.Black.copy(alpha = 0.2f), topLeft = Offset(x - r * 1.2f, y + r * 0.05f), size = Size(r * 2.4f, r * 0.4f))
    drawOval(feltMid.copy(alpha = 0.8f), topLeft = Offset(x - r * 0.95f, y - r * 0.25f), size = Size(r * 1.9f, r * 0.55f))

    drawRect(feltMid, topLeft = Offset(x - r * 0.9f, y - r * 0.95f), size = Size(r * 0.9f, r * 0.95f))
    drawRect(feltLight, topLeft = Offset(x, y - r * 0.95f), size = Size(r * 0.9f, r * 0.95f))

    for (i in 0 until 6) {
        val lx = x - r * 0.9f + i * (r * 1.8f / 6f)
        drawLine(woodDark.copy(alpha = 0.1f), Offset(lx, y - r * 0.15f), Offset(lx + r * 0.08f, y - r * 0.85f), strokeWidth = 0.5f)
    }

    drawRect(ornGold.copy(alpha = 0.6f), topLeft = Offset(x - r * 0.88f, y - r * 0.6f), size = Size(r * 1.76f, 2f))
    drawRect(ornRed.copy(alpha = 0.4f), topLeft = Offset(x - r * 0.88f, y - r * 0.48f), size = Size(r * 1.76f, 1.5f))

    val domePath = Path().apply {
        moveTo(x - r * 0.9f, y - r * 0.88f)
        cubicTo(x - r * 0.55f, y - r * 2.2f, x + r * 0.55f, y - r * 2.2f, x + r * 0.9f, y - r * 0.88f)
        close()
    }
    drawPath(domePath, color = roofDark)

    val domeHL = Path().apply {
        moveTo(x + r * 0.05f, y - r * 0.88f)
        cubicTo(x + r * 0.05f, y - r * 1.95f, x + r * 0.8f, y - r * 1.9f, x + r * 0.8f, y - r * 0.88f)
        close()
    }
    drawPath(domeHL, color = roofLight.copy(alpha = 0.35f))

    drawRect(ornGold.copy(alpha = 0.4f), topLeft = Offset(x - r * 0.88f, y - r * 0.95f), size = Size(r * 1.76f, 1.5f))

    val shY = y - r * 1.78f
    val shR = r * 0.2f
    drawOval(ornGold, topLeft = Offset(x - shR, shY - shR * 0.55f), size = Size(shR * 2, shR * 1.1f))

    val doorW = r * 0.3f
    val doorH = r * 0.6f
    drawRect(woodDark, topLeft = Offset(x - doorW / 2 - 1f, y - r * 0.82f), size = Size(doorW + 2, doorH + 1f))
    drawRect(Color(0.18f, 0.12f, 0.06f), topLeft = Offset(x - doorW / 2, y - r * 0.8f), size = Size(doorW, doorH))
    drawRect(Color(0.8f, 0.5f, 0.15f, 0.15f), topLeft = Offset(x - doorW / 2 + 1f, y - r * 0.7f), size = Size(doorW - 2f, doorH * 0.4f))
}
