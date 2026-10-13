package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.data.local.MissionEntity
import com.example.domain.model.Building
import com.example.domain.model.EnemyState
import com.example.domain.model.EnemyType
import com.example.domain.model.MissionObjectiveTarget
import com.example.domain.model.ObjectiveTargetKind
import com.example.domain.model.RoofType
import com.example.domain.model.SpiderSuit
import com.example.domain.model.SpiderWorldCatalog
import com.example.domain.model.TimeOfDay
import com.example.ui.viewmodel.OpenWorldUiState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OpenWorldCityCanvas(
    worldState: OpenWorldUiState,
    equippedSuit: SpiderSuit,
    missions: List<MissionEntity>,
    collectedBackpacks: Set<String>,
    animationTick: Float,
    onCanvasSkySwingPress: (Float, Float) -> Unit,
    onCanvasSkyRelease: () -> Unit,
    onCanvasDoubleTapZip: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("open_world_city_canvas")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { tapOffset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val groundScreenY = h * 0.84f
                        val scale = (h / 740f).coerceIn(0.65f, 1.45f)
                        val cameraX = worldState.heroX
                        val tapWorldX = cameraX + (tapOffset.x - w * 0.5f) / scale
                        val tapWorldY = (groundScreenY - tapOffset.y) / scale
                        onCanvasSkySwingPress(tapWorldX, tapWorldY)
                        tryAwaitRelease()
                        onCanvasSkyRelease()
                    },
                    onDoubleTap = { tapOffset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)
                        val groundScreenY = h * 0.84f
                        val scale = (h / 740f).coerceIn(0.65f, 1.45f)
                        val cameraX = worldState.heroX
                        val tapWorldX = cameraX + (tapOffset.x - w * 0.5f) / scale
                        val tapWorldY = (groundScreenY - tapOffset.y) / scale
                        onCanvasDoubleTapZip(tapWorldX, tapWorldY)
                    }
                )
            }
    ) {
        val canvasW = size.width
        val canvasH = size.height
        val groundScreenY = canvasH * 0.84f
        val scale = (canvasH / 740f).coerceIn(0.65f, 1.45f)
        val cameraX = worldState.heroX

        fun worldToScreenX(wx: Float): Float = canvasW * 0.5f + (wx - cameraX) * scale
        fun worldToScreenY(wy: Float): Float = groundScreenY - wy * scale

        // 1. Sky Atmosphere Gradient
        val skyColors = when (worldState.timeOfDay) {
            TimeOfDay.TWILIGHT_DUSK -> listOf(
                Color(0xFF070B1E),
                Color(0xFF1A1F4A),
                Color(0xFF4A1E52),
                Color(0xFF8C2D4B)
            )
            TimeOfDay.NEON_MIDNIGHT -> listOf(
                Color(0xFF030611),
                Color(0xFF081026),
                Color(0xFF0F1E42),
                Color(0xFF162955)
            )
            TimeOfDay.GOLDEN_DAWN -> listOf(
                Color(0xFF0F1B3D),
                Color(0xFF283B6B),
                Color(0xFF7D4364),
                Color(0xFFD96B43)
            )
        }
        drawRect(
            brush = Brush.verticalGradient(skyColors),
            size = Size(canvasW, groundScreenY)
        )

        // 2. Parallax Distant Skyline
        drawParallaxSkyline(cameraX, groundScreenY, canvasW, scale)

        // 3. Queens Bridge Suspension Cables (when near 4500..6000)
        drawSuspensionBridgeCables(
            worldToScreenX = ::worldToScreenX,
            worldToScreenY = ::worldToScreenY,
            canvasW = canvasW
        )

        // 4. Foreground Interactive Buildings
        for (building in SpiderWorldCatalog.buildings) {
            val leftX = worldToScreenX(building.x)
            val rightX = worldToScreenX(building.x + building.width)
            if (rightX < -120f || leftX > canvasW + 120f) continue
            drawBuilding(
                building = building,
                leftX = leftX,
                topY = worldToScreenY(building.height),
                widthPx = (building.width * scale),
                heightPx = (building.height * scale),
                groundY = groundScreenY,
                scale = scale,
                labelPaint = labelPaint
            )
        }

        // 5. Street & East River Ground Plane
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B1022), Color(0xFF050712)),
                startY = groundScreenY,
                endY = canvasH
            ),
            topLeft = Offset(0f, groundScreenY),
            size = Size(canvasW, canvasH - groundScreenY)
        )
        drawLine(
            color = Color(0xFF00E5FF).copy(alpha = 0.45f),
            start = Offset(0f, groundScreenY),
            end = Offset(canvasW, groundScreenY),
            strokeWidth = 3f
        )

        // 6. Mission Beacons in Free Roam & Waypoint Column
        if (worldState.activeMission == null) {
            for (mission in missions) {
                val bx = worldToScreenX(mission.targetWorldX)
                if (bx < -80f || bx > canvasW + 80f) continue
                val by = worldToScreenY(mission.targetWorldY)
                val beaconColor = if (mission.isCompleted) Color(0xFF00E5FF) else Color(0xFFFFC400)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(beaconColor.copy(alpha = 0.0f), beaconColor.copy(alpha = 0.38f)),
                        startY = 0f,
                        endY = by
                    ),
                    topLeft = Offset(bx - 18f * scale, 0f),
                    size = Size(36f * scale, by)
                )
                drawCircle(
                    color = beaconColor,
                    radius = (12f + 3f * sin(animationTick * 5f)) * scale,
                    center = Offset(bx, by - 16f * scale),
                    style = Stroke(width = 3f * scale)
                )
            }
        }

        worldState.waypointX?.let { wx ->
            val sx = worldToScreenX(wx)
            if (sx in -60f..(canvasW + 60f)) {
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.55f),
                    start = Offset(sx, 0f),
                    end = Offset(sx, groundScreenY),
                    strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), animationTick * 40f)
                )
            }
        }

        // 7. Rooftop Backpacks (Collectibles)
        for (bp in SpiderWorldCatalog.backpacks) {
            if (bp.id in collectedBackpacks) continue
            val bpx = worldToScreenX(bp.worldX)
            if (bpx < -50f || bpx > canvasW + 50f) continue
            val bpy = worldToScreenY(bp.worldY)
            val pulse = 1f + 0.18f * sin(animationTick * 6f)
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = 0.28f),
                radius = 22f * scale * pulse,
                center = Offset(bpx, bpy - 12f * scale)
            )
            drawRoundRect(
                color = Color(0xFF00E676),
                topLeft = Offset(bpx - 9f * scale, bpy - 21f * scale),
                size = Size(18f * scale, 18f * scale),
                cornerRadius = CornerRadius(5f * scale, 5f * scale)
            )
        }

        // 8. Mission Objective Targets (Holo-Rings, Hostage Pods, Sonic Bombs, Mystic Rifts)
        for (target in worldState.objectiveTargets) {
            if (target.isCompleted) continue
            val tx = worldToScreenX(target.x)
            if (tx < -90f || tx > canvasW + 90f) continue
            val ty = worldToScreenY(target.y)
            drawMissionTarget(target, tx, ty, scale, animationTick)
        }

        // 9. Enemies & Bosses
        for (enemy in worldState.enemies) {
            val ex = worldToScreenX(enemy.x)
            if (ex < -120f || ex > canvasW + 120f) continue
            val ey = worldToScreenY(enemy.y)
            drawEnemy(enemy, ex, ey, scale, animationTick, labelPaint)
        }

        // 10. Web Projectiles
        for (proj in worldState.projectiles) {
            val px = worldToScreenX(proj.x)
            val py = worldToScreenY(proj.y)
            drawCircle(
                color = Color(proj.colorHex),
                radius = 7f * scale,
                center = Offset(px, py)
            )
            drawCircle(
                color = Color.White,
                radius = 12f * scale,
                center = Offset(px, py),
                style = Stroke(width = 2f)
            )
        }

        // 11. Active Web-Swing Line or Web-Zip Twin Line
        val heroScreenX = worldToScreenX(worldState.heroX)
        val heroScreenY = worldToScreenY(worldState.heroY)

        if (worldState.isSwinging) {
            val ax = worldToScreenX(worldState.anchorX)
            val ay = worldToScreenY(worldState.anchorY)
            drawLine(
                color = Color(equippedSuit.webColorHex).copy(alpha = 0.4f),
                start = Offset(heroScreenX, heroScreenY - 12f * scale),
                end = Offset(ax, ay),
                strokeWidth = 7f * scale,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(heroScreenX, heroScreenY - 12f * scale),
                end = Offset(ax, ay),
                strokeWidth = 3f * scale,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(equippedSuit.webColorHex),
                radius = 7f * scale,
                center = Offset(ax, ay)
            )
        }

        if (worldState.isZipping) {
            val zx = worldToScreenX(worldState.zipTargetX)
            val zy = worldToScreenY(worldState.zipTargetY)
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(heroScreenX, heroScreenY - 10f * scale),
                end = Offset(zx, zy),
                strokeWidth = 4f * scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 8f), 0f)
            )
        }

        // 12. Ultimate Ability Shockwave
        if (worldState.ultimateEffectTimer > 0f) {
            val progress = 1f - (worldState.ultimateEffectTimer / 1.4f).coerceIn(0f, 1f)
            val radius = (60f + progress * 480f) * scale
            drawCircle(
                color = Color(equippedSuit.eyeGlowHex).copy(alpha = (1f - progress) * 0.45f),
                radius = radius,
                center = Offset(heroScreenX, heroScreenY)
            )
            drawCircle(
                color = Color(equippedSuit.primaryColorHex).copy(alpha = (1f - progress) * 0.85f),
                radius = radius,
                center = Offset(heroScreenX, heroScreenY),
                style = Stroke(width = 8f * scale)
            )
        }

        // 13. Spider-Man Hero Character & Spider-Sense Waves
        drawSpiderHero(
            worldState = worldState,
            suit = equippedSuit,
            cx = heroScreenX,
            cy = heroScreenY,
            scale = scale,
            animationTick = animationTick
        )

        // 14. Floating Comic Action Callouts
        for (callout in worldState.callouts) {
            val cx = worldToScreenX(callout.x)
            val cy = worldToScreenY(callout.y)
            labelPaint.color = android.graphics.Color.BLACK
            labelPaint.textSize = 31f * scale
            drawContext.canvas.nativeCanvas.drawText(callout.text, cx + 3f, cy + 3f, labelPaint)
            labelPaint.color = callout.colorHex.toInt()
            drawContext.canvas.nativeCanvas.drawText(callout.text, cx, cy, labelPaint)
        }
    }
}

private fun DrawScope.drawParallaxSkyline(
    cameraX: Float,
    groundY: Float,
    canvasW: Float,
    scale: Float
) {
    val parallaxX = -(cameraX * 0.25f) % 320f
    var x = parallaxX - 320f
    var idx = 0
    while (x < canvasW + 320f) {
        val bW = (75f + (idx % 4) * 22f) * scale
        val bH = (190f + ((idx * 37) % 210)) * scale
        drawRect(
            color = Color(0xFF0D142E).copy(alpha = 0.72f),
            topLeft = Offset(x, groundY - bH),
            size = Size(bW - 6f, bH)
        )
        x += bW
        idx++
    }
}

private fun DrawScope.drawSuspensionBridgeCables(
    worldToScreenX: (Float) -> Float,
    worldToScreenY: (Float) -> Float,
    canvasW: Float
) {
    val westPylonX = worldToScreenX(4905f)
    val eastPylonX = worldToScreenX(5677f)
    if (eastPylonX < -200f || westPylonX > canvasW + 200f) return

    val pylonTopY = worldToScreenY(500f)
    val midSpanY = worldToScreenY(270f)
    val path = Path().apply {
        moveTo(westPylonX, pylonTopY)
        quadraticTo((westPylonX + eastPylonX) * 0.5f, midSpanY, eastPylonX, pylonTopY)
    }
    drawPath(
        path = path,
        color = Color(0xFFD500F9).copy(alpha = 0.55f),
        style = Stroke(width = 5f)
    )
}

private fun DrawScope.drawBuilding(
    building: Building,
    leftX: Float,
    topY: Float,
    widthPx: Float,
    heightPx: Float,
    groundY: Float,
    scale: Float,
    labelPaint: Paint
) {
    val bodyColor = Color(building.primaryColorHex)
    val winColor = Color(building.windowColorHex)

    // Main Skyscraper Body
    drawRect(
        color = bodyColor,
        topLeft = Offset(leftX, topY),
        size = Size(widthPx, heightPx)
    )
    drawRect(
        color = winColor.copy(alpha = 0.35f),
        topLeft = Offset(leftX, topY),
        size = Size(widthPx, heightPx),
        style = Stroke(width = 2f)
    )

    // Roof Feature
    when (building.roofType) {
        RoofType.SPIRE -> {
            val midX = leftX + widthPx * 0.5f
            drawLine(
                color = winColor,
                start = Offset(midX, topY),
                end = Offset(midX, topY - 68f * scale),
                strokeWidth = 4f * scale
            )
            drawCircle(
                color = winColor,
                radius = 5f * scale,
                center = Offset(midX, topY - 68f * scale)
            )
        }
        RoofType.CRANE -> {
            val mastX = leftX + widthPx * 0.7f
            drawLine(
                color = Color(0xFFFFB300),
                start = Offset(mastX, topY),
                end = Offset(mastX, topY - 52f * scale),
                strokeWidth = 5f * scale
            )
            drawLine(
                color = Color(0xFFFFB300),
                start = Offset(mastX - 35f * scale, topY - 48f * scale),
                end = Offset(mastX + 55f * scale, topY - 48f * scale),
                strokeWidth = 4f * scale
            )
        }
        RoofType.PAGODA -> {
            val roofPath = Path().apply {
                moveTo(leftX - 12f * scale, topY)
                quadraticTo(leftX + widthPx * 0.5f, topY - 28f * scale, leftX + widthPx + 12f * scale, topY)
                close()
            }
            drawPath(roofPath, color = Color(0xFFD50000))
        }
        RoofType.BRIDGE_TOWER -> {
            drawRect(
                color = winColor.copy(alpha = 0.5f),
                topLeft = Offset(leftX + widthPx * 0.2f, topY - 35f * scale),
                size = Size(widthPx * 0.6f, 35f * scale),
                style = Stroke(width = 4f * scale)
            )
        }
        RoofType.NEON_BILLBOARD, RoofType.FLAT -> {
            drawRect(
                color = winColor.copy(alpha = 0.8f),
                topLeft = Offset(leftX, topY),
                size = Size(widthPx, 5f * scale)
            )
        }
    }

    // Lit Window Grid
    val cols = 4
    val rows = (building.height / 36f).toInt().coerceIn(4, 12)
    val padX = widthPx * 0.14f
    val cellW = (widthPx - padX * 2) / cols
    val cellH = (heightPx - 30f * scale) / rows

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if ((building.id + r * 3 + c * 7) % 5 == 0) continue
            drawRect(
                color = winColor.copy(alpha = 0.42f),
                topLeft = Offset(
                    x = leftX + padX + c * cellW + 3f,
                    y = topY + 18f * scale + r * cellH
                ),
                size = Size((cellW - 7f).coerceAtLeast(3f), (cellH * 0.55f).coerceAtLeast(3f))
            )
        }
    }

    // Billboard Sign
    building.billboardText?.let { text ->
        val signW = widthPx * 0.88f
        val signH = 24f * scale
        val signX = leftX + (widthPx - signW) * 0.5f
        val signY = topY + 22f * scale
        drawRoundRect(
            color = Color(0xDD070B19),
            topLeft = Offset(signX, signY),
            size = Size(signW, signH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = winColor,
            topLeft = Offset(signX, signY),
            size = Size(signW, signH),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 2.5f)
        )
        labelPaint.color = building.windowColorHex.toInt()
        labelPaint.textSize = 13f * scale
        drawContext.canvas.nativeCanvas.drawText(
            text,
            leftX + widthPx * 0.5f,
            signY + signH * 0.72f,
            labelPaint
        )
    }
}

private fun DrawScope.drawMissionTarget(
    target: MissionObjectiveTarget,
    tx: Float,
    ty: Float,
    scale: Float,
    animationTick: Float
) {
    val pulse = 1f + 0.15f * sin(animationTick * 6f)
    when (target.kind) {
        ObjectiveTargetKind.SWING_RING -> {
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                radius = target.radius * scale * pulse,
                center = Offset(tx, ty)
            )
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = target.radius * scale,
                center = Offset(tx, ty),
                style = Stroke(width = 5f * scale)
            )
            drawCircle(
                color = Color(0xFFFFC400),
                radius = target.radius * 0.65f * scale,
                center = Offset(tx, ty),
                style = Stroke(width = 3f * scale)
            )
        }
        ObjectiveTargetKind.HOSTAGE_POD -> {
            drawRoundRect(
                color = Color(0xFFFFC400).copy(alpha = 0.3f),
                topLeft = Offset(tx - 24f * scale, ty - 32f * scale),
                size = Size(48f * scale, 52f * scale),
                cornerRadius = CornerRadius(10f * scale, 10f * scale)
            )
            drawRoundRect(
                color = Color(0xFFFFC400),
                topLeft = Offset(tx - 24f * scale, ty - 32f * scale),
                size = Size(48f * scale, 52f * scale),
                cornerRadius = CornerRadius(10f * scale, 10f * scale),
                style = Stroke(width = 3.5f * scale)
            )
        }
        ObjectiveTargetKind.SONIC_BOMB -> {
            drawCircle(
                color = Color(0xFFFF1744).copy(alpha = 0.3f),
                radius = 34f * scale * pulse,
                center = Offset(tx, ty - 12f * scale)
            )
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 16f * scale,
                center = Offset(tx, ty - 12f * scale)
            )
        }
        ObjectiveTargetKind.MYSTIC_RIFT -> {
            drawCircle(
                color = Color(0xFFD500F9).copy(alpha = 0.32f),
                radius = target.radius * scale * pulse,
                center = Offset(tx, ty)
            )
            drawCircle(
                color = Color(0xFFFFC400),
                radius = target.radius * 0.8f * scale,
                center = Offset(tx, ty),
                style = Stroke(
                    width = 4f * scale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), animationTick * 60f)
                )
            )
        }
    }
}

private fun DrawScope.drawEnemy(
    enemy: EnemyState,
    ex: Float,
    ey: Float,
    scale: Float,
    animationTick: Float,
    labelPaint: Paint
) {
    // Telegraphing Spider-Sense Danger Ring
    if (enemy.isTelegraphingAttack) {
        drawCircle(
            color = Color(0xFFFF1744).copy(alpha = 0.42f),
            radius = (34f + 8f * sin(animationTick * 15f)) * scale,
            center = Offset(ex, ey - 16f * scale)
        )
    }

    when (enemy.type) {
        EnemyType.DOC_CYBER_OCTO -> {
            // Draw 4 animated neon-green mechanical tentacles
            for (arm in 0 until 4) {
                val side = if (arm % 2 == 0) -1f else 1f
                val verticalSpread = if (arm < 2) -26f else -6f
                val wave = sin(animationTick * 5f + arm * 1.4f) * 18f * scale
                val path = Path().apply {
                    moveTo(ex, ey - 24f * scale)
                    quadraticTo(
                        ex + side * 44f * scale,
                        ey + verticalSpread * scale + wave,
                        ex + side * 78f * scale,
                        ey + (verticalSpread - 18f) * scale - wave
                    )
                }
                drawPath(
                    path = path,
                    color = Color(0xFF00E676),
                    style = Stroke(width = 6f * scale, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = Color(0xFF76FF03),
                    radius = 7f * scale,
                    center = Offset(ex + side * 78f * scale, ey + (verticalSpread - 18f) * scale - wave)
                )
            }
            // Armored Boss Torso & Glowing Plasma Core
            drawRoundRect(
                color = Color(0xFF263238),
                topLeft = Offset(ex - 20f * scale, ey - 46f * scale),
                size = Size(40f * scale, 46f * scale),
                cornerRadius = CornerRadius(10f * scale, 10f * scale)
            )
            drawCircle(
                color = Color(0xFFFF9100),
                radius = 9f * scale,
                center = Offset(ex, ey - 24f * scale)
            )
        }
        EnemyType.HOVER_DRONE -> {
            val bob = sin(animationTick * 7f + enemy.id) * 6f * scale
            drawRoundRect(
                color = Color(0xFF37474F),
                topLeft = Offset(ex - 22f * scale, ey - 12f * scale + bob),
                size = Size(44f * scale, 18f * scale),
                cornerRadius = CornerRadius(9f * scale, 9f * scale)
            )
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 6f * scale,
                center = Offset(ex, ey - 3f * scale + bob)
            )
        }
        EnemyType.RHINO_MECH -> {
            drawRoundRect(
                color = Color(0xFF546E7A),
                topLeft = Offset(ex - 24f * scale, ey - 48f * scale),
                size = Size(48f * scale, 48f * scale),
                cornerRadius = CornerRadius(12f * scale, 12f * scale)
            )
            drawCircle(
                color = Color(0xFFFF3D00),
                radius = 8f * scale,
                center = Offset(ex, ey - 34f * scale)
            )
        }
        else -> {
            val bodyColor = if (enemy.type == EnemyType.SHOCK_ENFORCER) Color(0xFFFFC400) else Color(0xFFEF5350)
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(ex - 13f * scale, ey - 32f * scale),
                size = Size(26f * scale, 32f * scale),
                cornerRadius = CornerRadius(7f * scale, 7f * scale)
            )
            drawCircle(
                color = Color(0xFF263238),
                radius = 9f * scale,
                center = Offset(ex, ey - 38f * scale)
            )
        }
    }

    // Webbed-Up Overlay
    if (enemy.webbedStacks > 0) {
        drawCircle(
            color = Color.White.copy(alpha = 0.28f * enemy.webbedStacks),
            radius = 24f * scale,
            center = Offset(ex, ey - 20f * scale)
        )
        if (enemy.webbedStacks >= 3) {
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 26f * scale,
                center = Offset(ex, ey - 20f * scale),
                style = Stroke(width = 3f * scale)
            )
        }
    }

    // Enemy Health Bar
    val hpRatio = (enemy.hp.toFloat() / enemy.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    val barW = if (enemy.type.isBoss) 84f * scale else 44f * scale
    val barY = ey - (if (enemy.type.isBoss) 68f else 54f) * scale
    drawRoundRect(
        color = Color(0xAA000000),
        topLeft = Offset(ex - barW * 0.5f, barY),
        size = Size(barW, 7f * scale),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = if (enemy.type.isBoss) Color(0xFF00E676) else Color(0xFFFF3D43),
        topLeft = Offset(ex - barW * 0.5f, barY),
        size = Size(barW * hpRatio, 7f * scale),
        cornerRadius = CornerRadius(4f, 4f)
    )
    if (enemy.type.isBoss) {
        labelPaint.color = android.graphics.Color.WHITE
        labelPaint.textSize = 14f * scale
        drawContext.canvas.nativeCanvas.drawText("DR. CYBER-STORM", ex, barY - 6f * scale, labelPaint)
    }
}

private fun DrawScope.drawSpiderHero(
    worldState: OpenWorldUiState,
    suit: SpiderSuit,
    cx: Float,
    cy: Float,
    scale: Float,
    animationTick: Float
) {
    val primary = Color(suit.primaryColorHex)
    val secondary = Color(suit.secondaryColorHex)
    val eyeGlow = Color(suit.eyeGlowHex)

    // Spider-Sense Tingling Lightning Waves around head!
    if (worldState.isSpiderSenseActive) {
        val waveRadius = (36f + 6f * sin(animationTick * 18f)) * scale
        drawCircle(
            color = Color(0xFFFFC400),
            radius = waveRadius,
            center = Offset(cx, cy - 22f * scale),
            style = Stroke(
                width = 3.5f * scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), animationTick * 90f)
            )
        )
        drawCircle(
            color = Color(0xFFFF3D00),
            radius = waveRadius * 1.25f,
            center = Offset(cx, cy - 22f * scale),
            style = Stroke(
                width = 2.5f * scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), -animationTick * 90f)
            )
        )
    }

    val rotationDegrees = when {
        worldState.isFlipping -> worldState.flipAngle
        worldState.isSwinging -> (worldState.heroVx * 0.06f).coerceIn(-32f, 32f)
        else -> 0f
    }

    rotate(degrees = rotationDegrees, pivot = Offset(cx, cy - 18f * scale)) {
        // Iron-Spider Golden Waldo Arms if equipped
        if (suit.id == "iron_spider_nano") {
            for (i in -1..1 step 2) {
                drawLine(
                    color = Color(0xFFFFC400),
                    start = Offset(cx, cy - 20f * scale),
                    end = Offset(cx + i * 28f * scale, cy - 42f * scale),
                    strokeWidth = 3.5f * scale,
                    cap = StrokeCap.Round
                )
            }
        }

        // Dynamic Cape/Aura Trail at high speed
        if (abs(worldState.heroVx) > 220f || worldState.isSwinging) {
            val trailDir = if (worldState.facingRight) -1f else 1f
            drawLine(
                color = Color(suit.webColorHex).copy(alpha = 0.45f),
                start = Offset(cx, cy - 18f * scale),
                end = Offset(cx + trailDir * 36f * scale, cy - 12f * scale),
                strokeWidth = 8f * scale,
                cap = StrokeCap.Round
            )
        }

        // Spider-Hero Limbs (Crouched / Swinging Dynamic Pose)
        drawLine(
            color = secondary,
            start = Offset(cx - 8f * scale, cy - 12f * scale),
            end = Offset(cx - 18f * scale, cy + 2f * scale),
            strokeWidth = 6.5f * scale,
            cap = StrokeCap.Round
        )
        drawLine(
            color = secondary,
            start = Offset(cx + 8f * scale, cy - 12f * scale),
            end = Offset(cx + 18f * scale, cy + 2f * scale),
            strokeWidth = 6.5f * scale,
            cap = StrokeCap.Round
        )

        // Torso with White Spider Emblem
        drawRoundRect(
            color = primary,
            topLeft = Offset(cx - 11f * scale, cy - 30f * scale),
            size = Size(22f * scale, 22f * scale),
            cornerRadius = CornerRadius(7f * scale, 7f * scale)
        )
        drawCircle(
            color = Color.White,
            radius = 3.5f * scale,
            center = Offset(cx, cy - 19f * scale)
        )

        // Masked Head & Angular Expressive Eyes
        drawCircle(
            color = primary,
            radius = 11.5f * scale,
            center = Offset(cx, cy - 39f * scale)
        )
        val eyeOffsetX = if (worldState.facingRight) 3.5f * scale else -3.5f * scale
        drawCircle(
            color = eyeGlow,
            radius = 3.6f * scale,
            center = Offset(cx + eyeOffsetX - 3f * scale, cy - 40f * scale)
        )
        drawCircle(
            color = eyeGlow,
            radius = 3.6f * scale,
            center = Offset(cx + eyeOffsetX + 4f * scale, cy - 40f * scale)
        )
    }
}
