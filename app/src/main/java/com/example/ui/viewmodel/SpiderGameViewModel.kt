package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MissionEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SpiderDatabase
import com.example.data.repository.SpiderRepository
import com.example.domain.model.ActiveMissionRuntime
import com.example.domain.model.CityDistrict
import com.example.domain.model.ComicCallout
import com.example.domain.model.EnemyState
import com.example.domain.model.EnemyType
import com.example.domain.model.MissionCategory
import com.example.domain.model.MissionObjectiveTarget
import com.example.domain.model.MissionOutcomeSummary
import com.example.domain.model.ObjectiveTargetKind
import com.example.domain.model.RooftopBackpack
import com.example.domain.model.SpiderSuit
import com.example.domain.model.SpiderWorldCatalog
import com.example.domain.model.TimeOfDay
import com.example.domain.model.WebProjectile
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GameTab {
    OPEN_WORLD,
    MISSIONS,
    SUITS_TECH,
    CITY_MAP
}

data class OpenWorldUiState(
    val heroX: Float = 1470f,
    val heroY: Float = 475f,
    val heroVx: Float = 0f,
    val heroVy: Float = 0f,
    val facingRight: Boolean = true,
    val heroHealth: Int = 100,
    val maxHealth: Int = 100,
    val webFluid: Float = 100f,
    val focusMeter: Float = 45f,
    val comboCount: Int = 0,
    val comboTimer: Float = 0f,
    val isSwinging: Boolean = false,
    val anchorX: Float = 1580f,
    val anchorY: Float = 620f,
    val ropeLength: Float = 180f,
    val isZipping: Boolean = false,
    val zipTargetX: Float = 0f,
    val zipTargetY: Float = 0f,
    val zipTimer: Float = 0f,
    val isFlipping: Boolean = false,
    val flipAngle: Float = 0f,
    val isWallCrawling: Boolean = false,
    val isPerchedOnRoof: Boolean = true,
    val isSpiderSenseActive: Boolean = false,
    val ultimateEffectTimer: Float = 0f,
    val timeOfDay: TimeOfDay = TimeOfDay.TWILIGHT_DUSK,
    val isPaused: Boolean = false,
    val currentDistrict: CityDistrict = SpiderWorldCatalog.districts[1],
    val enemies: List<EnemyState> = emptyList(),
    val objectiveTargets: List<MissionObjectiveTarget> = emptyList(),
    val projectiles: List<WebProjectile> = emptyList(),
    val callouts: List<ComicCallout> = emptyList(),
    val activeMission: ActiveMissionRuntime? = null,
    val nearbyMissionBeacon: MissionEntity? = null,
    val nearbyBackpack: RooftopBackpack? = null,
    val waypointX: Float? = null,
    val waypointLabel: String? = null,
    val statusBannerText: String = "Patrolling Midtown Manhattan — Hold SWING or tap sky to web-swing!"
)

class SpiderGameViewModel(
    private val repository: SpiderRepository
) : ViewModel() {

    val missions: StateFlow<List<MissionEntity>> = repository.missionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SpiderWorldCatalog.initialMissions
        )

    val profile: StateFlow<PlayerProfileEntity> = repository.profileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerProfileEntity()
        )

    private val _selectedTab = MutableStateFlow(GameTab.OPEN_WORLD)
    val selectedTab: StateFlow<GameTab> = _selectedTab.asStateFlow()

    private val _missionFilter = MutableStateFlow("ALL")
    val missionFilter: StateFlow<String> = _missionFilter.asStateFlow()

    private val _missionOutcome = MutableStateFlow<MissionOutcomeSummary?>(null)
    val missionOutcome: StateFlow<MissionOutcomeSummary?> = _missionOutcome.asStateFlow()

    private val _worldState = MutableStateFlow(OpenWorldUiState())
    val worldState: StateFlow<OpenWorldUiState> = _worldState.asStateFlow()

    private var nextEntityId = 100
    private var accumulatedDistanceBuffer = 0f

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            spawnFreeRoamPatrolEncounter(districtX = 1470f)
        }
    }

    fun selectTab(tab: GameTab) {
        _selectedTab.value = tab
    }

    fun setMissionFilter(filter: String) {
        _missionFilter.value = filter
    }

    fun dismissMissionOutcome() {
        _missionOutcome.value = null
    }

    fun togglePause() {
        _worldState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun cycleTimeOfDay() {
        _worldState.update { state ->
            val next = when (state.timeOfDay) {
                TimeOfDay.TWILIGHT_DUSK -> TimeOfDay.NEON_MIDNIGHT
                TimeOfDay.NEON_MIDNIGHT -> TimeOfDay.GOLDEN_DAWN
                TimeOfDay.GOLDEN_DAWN -> TimeOfDay.TWILIGHT_DUSK
            }
            state.copy(
                timeOfDay = next,
                callouts = state.callouts + makeCallout(
                    text = next.label.uppercase(),
                    x = state.heroX,
                    y = state.heroY + 55f,
                    colorHex = 0xFF00E5FF
                )
            )
        }
    }

    fun equippedSuit(): SpiderSuit {
        val suitId = profile.value.equippedSuitId
        return SpiderWorldCatalog.suits.firstOrNull { it.id == suitId }
            ?: SpiderWorldCatalog.suits.first()
    }

    fun setMoveDirection(movingRight: Boolean, nudgeBoost: Boolean = true) {
        _worldState.update { state ->
            val speedMult = getSpeedMultiplier()
            val horizontalImpulse = if (nudgeBoost) (if (movingRight) 220f else -220f) * speedMult else state.heroVx
            state.copy(
                facingRight = movingRight,
                heroVx = if (nudgeBoost) horizontalImpulse else state.heroVx
            )
        }
    }

    fun startWebSwing(customSkyTargetX: Float? = null, customSkyTargetY: Float? = null) {
        _worldState.update { state ->
            if (state.isPaused) return@update state
            val dir = if (state.facingRight) 1f else -1f
            val speedMult = getSpeedMultiplier()

            val desiredAnchorX = customSkyTargetX ?: (state.heroX + dir * 165f)
            val clampedAnchorX = desiredAnchorX.coerceIn(60f, SpiderWorldCatalog.WORLD_WIDTH - 60f)

            val nearbyBuilding = SpiderWorldCatalog.buildings.minByOrNull { b ->
                abs((b.x + b.width * 0.5f) - clampedAnchorX)
            }
            val roofTopY = (nearbyBuilding?.height ?: 420f) + 95f
            val anchorY = (customSkyTargetY ?: maxOf(state.heroY + 130f, roofTopY))
                .coerceIn(260f, SpiderWorldCatalog.MAX_SKY_ALTITUDE)

            val dist = hypot(clampedAnchorX - state.heroX, anchorY - state.heroY)
                .coerceIn(95f, 270f)

            val initialVx = if (abs(state.heroVx) < 150f) dir * 270f * speedMult else state.heroVx * 1.12f
            val initialVy = maxOf(state.heroVy, 40f)

            state.copy(
                isSwinging = true,
                isPerchedOnRoof = false,
                isWallCrawling = false,
                anchorX = clampedAnchorX,
                anchorY = anchorY,
                ropeLength = dist,
                heroVx = initialVx,
                heroVy = initialVy,
                webFluid = (state.webFluid - 4f).coerceAtLeast(0f),
                callouts = state.callouts + makeCallout("THWIP!", state.heroX, state.heroY + 38f, 0xFF00E5FF)
            )
        }
    }

    fun releaseWebSwing() {
        _worldState.update { state ->
            if (!state.isSwinging) return@update state
            val dir = if (state.facingRight) 1f else -1f
            val speedMult = getSpeedMultiplier()
            state.copy(
                isSwinging = false,
                isFlipping = true,
                flipAngle = 0f,
                heroVx = (state.heroVx + dir * 85f * speedMult).coerceIn(-620f, 620f),
                heroVy = (state.heroVy + 155f).coerceIn(-300f, 440f),
                focusMeter = (state.focusMeter + 5f).coerceAtMost(100f)
            )
        }
    }

    fun triggerWebZip(explicitWorldX: Float? = null, explicitWorldY: Float? = null) {
        _worldState.update { state ->
            if (state.isPaused) return@update state
            val dir = if (state.facingRight) 1f else -1f
            val speedMult = getSpeedMultiplier()

            // Prioritize nearest uncompleted ring/pod or airborne enemy if within range
            val nearestTarget = state.objectiveTargets
                .filter { !it.isCompleted && hypot(it.x - state.heroX, it.y - state.heroY) < 420f }
                .minByOrNull { hypot(it.x - state.heroX, it.y - state.heroY) }

            val nearestEnemy = state.enemies
                .filter { hypot(it.x - state.heroX, it.y - state.heroY) < 380f }
                .minByOrNull { hypot(it.x - state.heroX, it.y - state.heroY) }

            val targetX: Float
            val targetY: Float
            when {
                explicitWorldX != null && explicitWorldY != null -> {
                    targetX = explicitWorldX.coerceIn(50f, SpiderWorldCatalog.WORLD_WIDTH - 50f)
                    targetY = explicitWorldY.coerceIn(90f, SpiderWorldCatalog.MAX_SKY_ALTITUDE - 30f)
                }
                nearestTarget != null -> {
                    targetX = nearestTarget.x
                    targetY = nearestTarget.y
                }
                nearestEnemy != null -> {
                    targetX = nearestEnemy.x
                    targetY = nearestEnemy.y + 15f
                }
                else -> {
                    targetX = (state.heroX + dir * 240f).coerceIn(50f, SpiderWorldCatalog.WORLD_WIDTH - 50f)
                    targetY = (state.heroY + 95f).coerceIn(140f, SpiderWorldCatalog.MAX_SKY_ALTITUDE - 40f)
                }
            }

            val dx = targetX - state.heroX
            val dy = targetY - state.heroY
            val angle = atan2(dy, dx)
            val zipSpeed = 580f * speedMult

            state.copy(
                isSwinging = false,
                isPerchedOnRoof = false,
                isZipping = true,
                zipTargetX = targetX,
                zipTargetY = targetY,
                zipTimer = 0.28f,
                facingRight = dx >= 0f,
                heroVx = cos(angle) * zipSpeed,
                heroVy = sin(angle) * zipSpeed,
                focusMeter = (state.focusMeter + 6f).coerceAtMost(100f),
                callouts = state.callouts + makeCallout("WEB-ZIP!", state.heroX, state.heroY + 42f, 0xFF80F2FF)
            )
        }
    }

    fun triggerWebStrike() {
        var stoppedCrimeInFreeRoam = false
        _worldState.update { state ->
            if (state.isPaused) return@update state
            val suit = equippedSuit()
            val combatLevel = profile.value.combatDamageLevel
            val baseDamage = (34f * suit.damageMultiplier * (1f + (combatLevel - 1) * 0.18f)).toInt()

            val targetEnemy = state.enemies
                .minByOrNull { hypot(it.x - state.heroX, it.y - state.heroY) }

            if (targetEnemy == null || hypot(targetEnemy.x - state.heroX, targetEnemy.y - state.heroY) > 370f) {
                // Air kick boost forward
                val dir = if (state.facingRight) 1f else -1f
                return@update state.copy(
                    heroVx = dir * 320f,
                    heroVy = maxOf(state.heroVy, 120f),
                    isFlipping = true,
                    flipAngle = 0f,
                    callouts = state.callouts + makeCallout("SWING KICK!", state.heroX, state.heroY + 35f, 0xFFFFC400)
                )
            }

            val bonusWebMultiplier = if (targetEnemy.webbedStacks >= 3) 1.5f else 1.0f
            val finalDamage = (baseDamage * bonusWebMultiplier).toInt()
            val newCombo = state.comboCount + 1
            val hitWords = listOf("POW!", "THWACK!", "WHAM!", "SPIDER-STRIKE!", "CRACK!")
            val word = hitWords[newCombo % hitWords.size]

            var defeatedCountDelta = 0
            val updatedEnemies = state.enemies.mapNotNull { enemy ->
                if (enemy.id == targetEnemy.id) {
                    val remainingHp = enemy.hp - finalDamage
                    if (remainingHp <= 0) {
                        defeatedCountDelta = 1
                        null
                    } else {
                        val pushDir = if (targetEnemy.x >= state.heroX) 1f else -1f
                        enemy.copy(
                            hp = remainingHp,
                            x = (enemy.x + pushDir * 28f).coerceIn(50f, SpiderWorldCatalog.WORLD_WIDTH - 50f),
                            stunnedSeconds = maxOf(enemy.stunnedSeconds, 0.9f),
                            isTelegraphingAttack = false
                        )
                    }
                } else {
                    enemy
                }
            }

            if (defeatedCountDelta > 0 && state.activeMission == null) {
                stoppedCrimeInFreeRoam = true
            }

            val updatedMission = state.activeMission?.let { m ->
                m.copy(
                    enemiesDefeated = m.enemiesDefeated + defeatedCountDelta,
                    score = m.score + finalDamage * 10 + (newCombo * 25)
                )
            }

            state.copy(
                heroX = targetEnemy.x + (if (targetEnemy.x >= state.heroX) -32f else 32f),
                heroY = targetEnemy.y + 18f,
                heroVx = 0f,
                heroVy = 160f,
                facingRight = targetEnemy.x >= state.heroX,
                isSwinging = false,
                comboCount = newCombo,
                comboTimer = 4.0f,
                focusMeter = (state.focusMeter + 14f).coerceAtMost(100f),
                enemies = updatedEnemies,
                activeMission = updatedMission,
                callouts = state.callouts + makeCallout(
                    text = "$word -$finalDamage",
                    x = targetEnemy.x,
                    y = targetEnemy.y + 48f,
                    colorHex = 0xFFFF3D43
                )
            )
        }

        if (stoppedCrimeInFreeRoam) {
            viewModelScope.launch {
                repository.awardOpenWorldActivity(bonusXp = 65, bonusTokens = 12, crimeStopped = true)
            }
        }
        checkActiveMissionCompletion()
    }

    fun triggerWebShot() {
        _worldState.update { state ->
            if (state.isPaused) return@update state
            if (state.webFluid < 8f) {
                return@update state.copy(
                    callouts = state.callouts + makeCallout("RECHARGING WEBS!", state.heroX, state.heroY + 40f, 0xFFFF9100)
                )
            }
            val suit = equippedSuit()
            val nearestEnemy = state.enemies
                .filter { hypot(it.x - state.heroX, it.y - state.heroY) < 520f }
                .minByOrNull { hypot(it.x - state.heroX, it.y - state.heroY) }

            val angle = if (nearestEnemy != null) {
                atan2(nearestEnemy.y - state.heroY, nearestEnemy.x - state.heroX)
            } else {
                if (state.facingRight) 0.08f else 3.06f
            }

            val speed = 780f
            val proj = WebProjectile(
                id = ++nextEntityId,
                x = state.heroX,
                y = state.heroY + 12f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                colorHex = suit.webColorHex
            )

            state.copy(
                facingRight = cos(angle) >= 0f,
                webFluid = (state.webFluid - 8f).coerceAtLeast(0f),
                projectiles = state.projectiles + proj,
                callouts = state.callouts + makeCallout("THWIP!", state.heroX, state.heroY + 34f, suit.webColorHex)
            )
        }
    }

    fun triggerDodgeOrTrick() {
        var awardedTrickXp = false
        _worldState.update { state ->
            if (state.isPaused) return@update state
            val dir = if (state.facingRight) -1f else 1f
            if (state.isSpiderSenseActive) {
                // Perfect Spider-Sense Counter Dodge!
                val updatedEnemies = state.enemies.map { e ->
                    if (e.isTelegraphingAttack) {
                        e.copy(
                            isTelegraphingAttack = false,
                            telegraphTimer = 0f,
                            stunnedSeconds = 2.5f,
                            webbedStacks = (e.webbedStacks + 2).coerceAtMost(3)
                        )
                    } else e
                }
                state.copy(
                    heroVx = dir * 280f,
                    heroVy = 220f,
                    isSwinging = false,
                    isFlipping = true,
                    flipAngle = 0f,
                    isSpiderSenseActive = false,
                    heroHealth = (state.heroHealth + 10).coerceAtMost(state.maxHealth),
                    focusMeter = (state.focusMeter + 26f).coerceAtMost(100f),
                    comboCount = state.comboCount + 2,
                    comboTimer = 4.5f,
                    enemies = updatedEnemies,
                    callouts = state.callouts + makeCallout(
                        "SPIDER-SENSE PERFECT DODGE!",
                        state.heroX,
                        state.heroY + 56f,
                        0xFFFFC400
                    )
                )
            } else {
                // Aerial Acrobatic Trick!
                awardedTrickXp = !state.isPerchedOnRoof && state.heroY > 220f
                val trickNames = listOf("SPIDER-SALTO!", "WEB HANDSPRING!", "MOONSAULT FLIP!", "BROOKLYN TWIST!")
                val trick = trickNames[(state.heroX.toInt().coerceAtLeast(0)) % trickNames.size]
                state.copy(
                    heroVy = maxOf(state.heroVy + 135f, 190f),
                    isFlipping = true,
                    flipAngle = 0f,
                    focusMeter = (state.focusMeter + 12f).coerceAtMost(100f),
                    callouts = state.callouts + makeCallout(
                        text = if (awardedTrickXp) "$trick +20 XP" else "ACROBATIC EVADE!",
                        x = state.heroX,
                        y = state.heroY + 46f,
                        colorHex = 0xFF00E5FF
                    )
                )
            }
        }
        if (awardedTrickXp) {
            viewModelScope.launch {
                repository.awardOpenWorldActivity(bonusXp = 20, bonusTokens = 2)
            }
        }
    }

    fun triggerSuitUltimate() {
        var defeatedCount = 0
        _worldState.update { state ->
            if (state.isPaused || state.focusMeter < 100f) return@update state
            val suit = equippedSuit()
            val ultDamage = (125f * suit.damageMultiplier).toInt()

            val updatedEnemies = state.enemies.mapNotNull { enemy ->
                val dist = hypot(enemy.x - state.heroX, enemy.y - state.heroY)
                if (dist <= 580f) {
                    val newHp = enemy.hp - ultDamage
                    if (newHp <= 0) {
                        defeatedCount++
                        null
                    } else {
                        enemy.copy(
                            hp = newHp,
                            webbedStacks = 3,
                            stunnedSeconds = 5.0f,
                            isTelegraphingAttack = false
                        )
                    }
                } else {
                    enemy
                }
            }

            val updatedMission = state.activeMission?.let { m ->
                m.copy(
                    enemiesDefeated = m.enemiesDefeated + defeatedCount,
                    score = m.score + defeatedCount * 350 + 500
                )
            }

            state.copy(
                focusMeter = 0f,
                heroHealth = (state.heroHealth + 25).coerceAtMost(state.maxHealth),
                ultimateEffectTimer = 1.4f,
                comboCount = state.comboCount + 5,
                comboTimer = 5.0f,
                enemies = updatedEnemies,
                activeMission = updatedMission,
                callouts = state.callouts + makeCallout(
                    text = "${suit.ultimateName.uppercase()}!",
                    x = state.heroX,
                    y = state.heroY + 65f,
                    colorHex = suit.eyeGlowHex
                )
            )
        }

        if (defeatedCount > 0) {
            viewModelScope.launch {
                repository.awardOpenWorldActivity(
                    bonusXp = defeatedCount * 50,
                    bonusTokens = defeatedCount * 10,
                    crimeStopped = true
                )
            }
        }
        checkActiveMissionCompletion()
    }

    fun startMissionById(missionId: String) {
        val mission = missions.value.firstOrNull { it.id == missionId } ?: return
        launchMission(mission)
    }

    fun launchMission(mission: MissionEntity) {
        val category = runCatching { MissionCategory.valueOf(mission.category) }
            .getOrDefault(MissionCategory.CRIME_PATROL)

        val spawnedEnemies = mutableListOf<EnemyState>()
        val spawnedTargets = mutableListOf<MissionObjectiveTarget>()
        var timeLimit = 90f
        var instruction = mission.objectiveSummary

        when (mission.id) {
            "m1_times_square_heist" -> {
                timeLimit = 85f
                instruction = "Defeat the 5 Times Square Heist Enforcers!"
                val xs = listOf(1340f, 1435f, 1495f, 1610f, 1790f)
                xs.forEachIndexed { idx, xPos ->
                    val bRoof = roofHeightAt(xPos)
                    val eType = if (idx >= 3) EnemyType.SHOCK_ENFORCER else EnemyType.STREET_THUG
                    spawnedEnemies += createEnemy(eType, xPos, bRoof + 18f)
                }
            }
            "m2_skyline_express" -> {
                timeLimit = 75f
                instruction = "Swing & Web-Zip through all 7 Aerial Holo-Rings!"
                val ringCoords = listOf(
                    1180f to 430f,
                    1420f to 520f,
                    1680f to 460f,
                    1920f to 580f,
                    2180f to 490f,
                    2450f to 440f,
                    2720f to 530f
                )
                ringCoords.forEachIndexed { idx, (rx, ry) ->
                    spawnedTargets += MissionObjectiveTarget(
                        id = ++nextEntityId,
                        label = "Ring #${idx + 1}",
                        x = rx,
                        y = ry,
                        radius = 58f,
                        kind = ObjectiveTargetKind.SWING_RING
                    )
                }
            }
            "m3_fisk_tower_raid" -> {
                timeLimit = 95f
                instruction = "Disarm 4 Sonic Bombs on rooftops & defeat 4 Fisk Enforcers!"
                val bombXs = listOf(270f, 620f, 800f, 970f)
                bombXs.forEachIndexed { idx, bx ->
                    val by = roofHeightAt(bx) + 22f
                    spawnedTargets += MissionObjectiveTarget(
                        id = ++nextEntityId,
                        label = "Sonic Bomb #${idx + 1}",
                        x = bx,
                        y = by,
                        radius = 54f,
                        kind = ObjectiveTargetKind.SONIC_BOMB
                    )
                    spawnedEnemies += createEnemy(EnemyType.SHOCK_ENFORCER, bx + 25f, by)
                }
            }
            "m4_chinatown_rescue" -> {
                timeLimit = 95f
                instruction = "Free 4 Civilian Pods & destroy 4 Oscorp Hunter-Drones!"
                val podXs = listOf(2495f, 2675f, 2870f, 3235f)
                podXs.forEachIndexed { idx, px ->
                    val py = roofHeightAt(px) + 20f
                    spawnedTargets += MissionObjectiveTarget(
                        id = ++nextEntityId,
                        label = "Hostage Pod #${idx + 1}",
                        x = px,
                        y = py,
                        radius = 55f,
                        kind = ObjectiveTargetKind.HOSTAGE_POD
                    )
                    spawnedEnemies += createEnemy(EnemyType.HOVER_DRONE, px, py + 95f)
                }
            }
            "m5_oscorp_drone_swarm" -> {
                timeLimit = 85f
                instruction = "Destroy all 6 Airborne Hunter-Drones over Oscorp Tower!"
                val dronePositions = listOf(
                    3700f to 490f,
                    3880f to 460f,
                    4060f to 585f,
                    4260f to 490f,
                    4450f to 530f,
                    4620f to 430f
                )
                dronePositions.forEach { (dx, dy) ->
                    spawnedEnemies += createEnemy(EnemyType.HOVER_DRONE, dx, dy)
                }
            }
            "m6_mystic_portal_rift" -> {
                timeLimit = 75f
                instruction = "Swing through 6 High-Altitude Mystic Rifts to seal them!"
                val riftPositions = listOf(
                    2500f to 450f,
                    2680f to 540f,
                    2870f to 590f,
                    3060f to 480f,
                    3240f to 550f,
                    3420f to 460f
                )
                riftPositions.forEachIndexed { idx, (rx, ry) ->
                    spawnedTargets += MissionObjectiveTarget(
                        id = ++nextEntityId,
                        label = "Mystic Rift #${idx + 1}",
                        x = rx,
                        y = ry,
                        radius = 60f,
                        kind = ObjectiveTargetKind.MYSTIC_RIFT
                    )
                }
            }
            "m7_midtown_rhino_convoy" -> {
                timeLimit = 100f
                instruction = "Defeat the Armored Rhino-Mech & 3 Shock Escorts in Midtown!"
                spawnedEnemies += createEnemy(EnemyType.RHINO_MECH, 1845f, roofHeightAt(1845f) + 20f)
                spawnedEnemies += createEnemy(EnemyType.SHOCK_ENFORCER, 1655f, roofHeightAt(1655f) + 18f)
                spawnedEnemies += createEnemy(EnemyType.SHOCK_ENFORCER, 1475f, roofHeightAt(1475f) + 18f)
                spawnedEnemies += createEnemy(EnemyType.HOVER_DRONE, 1845f, 610f)
            }
            else -> {
                // m8_boss_cyber_octo
                timeLimit = 120f
                instruction = "BOSS ALERT: Defeat Dr. Cyber-Storm & his 3 Reactor Drones on Queens Bridge!"
                spawnedEnemies += createEnemy(EnemyType.DOC_CYBER_OCTO, 5290f, roofHeightAt(5290f) + 24f)
                spawnedEnemies += createEnemy(EnemyType.HOVER_DRONE, 5095f, 390f)
                spawnedEnemies += createEnemy(EnemyType.HOVER_DRONE, 5290f, 470f)
                spawnedEnemies += createEnemy(EnemyType.HOVER_DRONE, 5485f, 390f)
            }
        }

        val startRoofY = roofHeightAt(mission.targetWorldX) + 25f
        _missionOutcome.value = null
        _selectedTab.value = GameTab.OPEN_WORLD

        _worldState.update { state ->
            state.copy(
                heroX = mission.targetWorldX - 80f,
                heroY = startRoofY + 40f,
                heroVx = 120f,
                heroVy = 80f,
                facingRight = true,
                heroHealth = state.maxHealth,
                webFluid = 100f,
                isSwinging = false,
                isPaused = false,
                enemies = spawnedEnemies,
                objectiveTargets = spawnedTargets,
                projectiles = emptyList(),
                waypointX = mission.targetWorldX,
                waypointLabel = mission.title,
                activeMission = ActiveMissionRuntime(
                    missionId = mission.id,
                    title = mission.title,
                    category = category,
                    districtName = mission.district,
                    currentInstruction = instruction,
                    timeRemainingSeconds = timeLimit,
                    totalTimeSeconds = timeLimit,
                    totalEnemiesToDefeat = spawnedEnemies.size,
                    totalTargets = spawnedTargets.size,
                    score = 200
                ),
                statusBannerText = "MISSION ACTIVE: ${mission.title}",
                callouts = state.callouts + makeCallout(
                    text = "MISSION START: ${mission.title.uppercase()}",
                    x = mission.targetWorldX,
                    y = startRoofY + 80f,
                    colorHex = 0xFFFFC400
                )
            )
        }
    }

    fun abortActiveMission() {
        _worldState.update { state ->
            state.copy(
                activeMission = null,
                enemies = emptyList(),
                objectiveTargets = emptyList(),
                waypointX = null,
                waypointLabel = null,
                statusBannerText = "Returned to Free Roam Patrol in ${state.currentDistrict.name}."
            )
        }
        spawnFreeRoamPatrolEncounter(_worldState.value.heroX)
    }

    fun fastTravelToDistrict(district: CityDistrict) {
        val midX = (district.startX + district.endX) * 0.5f
        val roofY = roofHeightAt(midX) + 45f
        _selectedTab.value = GameTab.OPEN_WORLD
        _worldState.update { state ->
            state.copy(
                heroX = midX,
                heroY = roofY,
                heroVx = 0f,
                heroVy = 0f,
                isSwinging = false,
                isPerchedOnRoof = true,
                currentDistrict = district,
                waypointX = midX,
                waypointLabel = district.landmarkName,
                statusBannerText = "Fast-traveled to ${district.name} (${district.landmarkName})",
                callouts = state.callouts + makeCallout(
                    text = "ARRIVED: ${district.name.uppercase()}",
                    x = midX,
                    y = roofY + 55f,
                    colorHex = district.accentColorHex
                )
            )
        }
        if (_worldState.value.activeMission == null) {
            spawnFreeRoamPatrolEncounter(midX)
        }
    }

    fun setWaypoint(worldX: Float, label: String) {
        _selectedTab.value = GameTab.OPEN_WORLD
        _worldState.update { state ->
            state.copy(
                waypointX = worldX,
                waypointLabel = label,
                statusBannerText = "Waypoint locked: $label (${abs(worldX - state.heroX).toInt()}m away)"
            )
        }
    }

    fun collectNearbyBackpack() {
        val bp = _worldState.value.nearbyBackpack ?: return
        viewModelScope.launch {
            val collected = repository.collectBackpack(bp.id, bp.tokenReward)
            if (collected) {
                _worldState.update { state ->
                    state.copy(
                        nearbyBackpack = null,
                        statusBannerText = "Collected Backpack: ${bp.name} (+${bp.tokenReward} Tech Tokens!)",
                        callouts = state.callouts + makeCallout(
                            text = "BACKPACK FOUND! +${bp.tokenReward} TOKENS",
                            x = bp.worldX,
                            y = bp.worldY + 45f,
                            colorHex = 0xFF00E676
                        )
                    )
                }
            }
        }
    }

    fun unlockOrEquipSuit(suit: SpiderSuit) {
        viewModelScope.launch {
            repository.unlockOrEquipSuit(suit.id, suit.tokenCost)
        }
    }

    fun upgradeTech(upgradeKey: String, cost: Int) {
        viewModelScope.launch {
            repository.upgradeTechSystem(upgradeKey, cost)
        }
    }

    fun stepPhysics(rawDt: Float) {
        val dt = rawDt.coerceIn(0.008f, 0.045f)
        val currentSnapshot = _worldState.value
        if (currentSnapshot.isPaused || _missionOutcome.value != null) return

        var newlyCollectedBackpack: RooftopBackpack? = null
        var missionTimedOut = false
        var defeatedEnemiesInStep = 0

        _worldState.update { state ->
            val speedMult = getSpeedMultiplier()
            var x = state.heroX
            var y = state.heroY
            var vx = state.heroVx
            var vy = state.heroVy
            var isPerched = false
            var isWallCrawling = false
            var isSwinging = state.isSwinging

            if (state.isZipping && state.zipTimer > 0f) {
                x += vx * dt
                y += vy * dt
            } else if (isSwinging) {
                val gravity = -480f
                val dir = if (state.facingRight) 1f else -1f
                // Pendulum swing physics around (anchorX, anchorY)
                vy += gravity * dt
                vx += dir * 320f * speedMult * dt

                x += vx * dt
                y += vy * dt

                val dx = x - state.anchorX
                val dy = y - state.anchorY
                val dist = hypot(dx, dy).coerceAtLeast(1f)
                if (dist > state.ropeLength) {
                    val nx = dx / dist
                    val ny = dy / dist
                    x = state.anchorX + nx * state.ropeLength
                    y = state.anchorY + ny * state.ropeLength
                    val radialVel = vx * nx + vy * ny
                    if (radialVel > 0f) {
                        vx -= radialVel * nx
                        vy -= radialVel * ny
                    }
                    // Add forward pendulum tangential boost
                    val tangentX = -ny * dir
                    val tangentY = nx * dir
                    vx += tangentX * 95f * speedMult * dt
                    vy += tangentY * 95f * speedMult * dt
                }

                // Auto-vault at forward apex for smooth continuous swinging
                val pastApex = (state.facingRight && x > state.anchorX + state.ropeLength * 0.72f) ||
                    (!state.facingRight && x < state.anchorX - state.ropeLength * 0.72f)
                if (pastApex && y >= state.anchorY - state.ropeLength * 0.55f) {
                    // Automatically re-shoot web ahead!
                    val nextAnchorX = (x + dir * 185f).coerceIn(80f, SpiderWorldCatalog.WORLD_WIDTH - 80f)
                    val nextAnchorY = maxOf(y + 115f, roofHeightAt(nextAnchorX) + 95f)
                        .coerceIn(280f, SpiderWorldCatalog.MAX_SKY_ALTITUDE)
                    return@update state.copy(
                        heroX = x.coerceIn(30f, SpiderWorldCatalog.WORLD_WIDTH - 30f),
                        heroY = y.coerceIn(45f, SpiderWorldCatalog.MAX_SKY_ALTITUDE),
                        heroVx = vx,
                        heroVy = maxOf(vy + 110f, 150f),
                        anchorX = nextAnchorX,
                        anchorY = nextAnchorY,
                        ropeLength = hypot(nextAnchorX - x, nextAnchorY - y).coerceIn(100f, 260f),
                        focusMeter = (state.focusMeter + 2.5f).coerceAtMost(100f)
                    )
                }
            } else {
                // Free flight / rooftop running / wall crawling
                val prevY = y
                vy -= 520f * dt
                vx *= (1f - 1.15f * dt)
                x += vx * dt
                y += vy * dt

                // Check building rooftop landing or wall-crawl
                val building = SpiderWorldCatalog.buildings.firstOrNull { b ->
                    x >= b.x - 10f && x <= b.x + b.width + 10f
                }
                if (building != null) {
                    val roofY = building.height
                    if (prevY >= roofY - 12f && y <= roofY + 6f && vy <= 0f) {
                        y = roofY
                        vy = 0f
                        isPerched = true
                    } else if (y < roofY - 12f && (abs(x - building.x) < 18f || abs(x - (building.x + building.width)) < 18f)) {
                        // Wall-crawl upward boost!
                        isWallCrawling = true
                        vy = 140f * speedMult
                    }
                }

                // Street floor safety bounce
                if (y < 42f) {
                    y = 42f
                    vy = 0f
                    isPerched = true
                }
            }

            x = x.coerceIn(30f, SpiderWorldCatalog.WORLD_WIDTH - 30f)
            y = y.coerceIn(42f, SpiderWorldCatalog.MAX_SKY_ALTITUDE - 15f)

            val stepDistMeters = (hypot(x - state.heroX, y - state.heroY) * 0.08f)
            accumulatedDistanceBuffer += stepDistMeters

            // Update projectiles & check enemy hits
            val activeProjectiles = mutableListOf<WebProjectile>()
            val hitEnemyIds = mutableMapOf<Int, Int>()
            for (p in state.projectiles) {
                val nx = p.x + p.vx * dt
                val ny = p.y + p.vy * dt
                if (abs(nx - x) > 750f || ny < 20f || ny > SpiderWorldCatalog.MAX_SKY_ALTITUDE) continue
                val hitEnemy = state.enemies.firstOrNull { e ->
                    hypot(e.x - nx, e.y - ny) < 42f
                }
                if (hitEnemy != null) {
                    hitEnemyIds[hitEnemy.id] = (hitEnemyIds[hitEnemy.id] ?: 0) + 1
                } else {
                    activeProjectiles += p.copy(x = nx, y = ny)
                }
            }

            // Update enemies AI & Spider-Sense telegraphing
            var anyTelegraphing = false
            var heroHp = state.heroHealth
            val newCallouts = state.callouts.mapNotNull { c ->
                val rem = c.lifeRemaining - dt
                if (rem > 0f) c.copy(y = c.y + 22f * dt, lifeRemaining = rem) else null
            }.toMutableList()

            val senseRangeBonus = 1f + (profile.value.spiderSenseRangeLevel - 1) * 0.15f
            val updatedEnemies = state.enemies.mapNotNull { enemy ->
                val webHits = hitEnemyIds[enemy.id] ?: 0
                val combatMult = 1f + (profile.value.combatDamageLevel - 1) * 0.15f
                val webDamage = (webHits * 22f * combatMult).toInt()
                val newHp = enemy.hp - webDamage
                if (newHp <= 0) {
                    defeatedEnemiesInStep++
                    newCallouts += makeCallout("WEBBED & KO!", enemy.x, enemy.y + 40f, 0xFF00E676)
                    return@mapNotNull null
                }

                val newWebStacks = (enemy.webbedStacks + webHits).coerceAtMost(3)
                if (webHits > 0 && newWebStacks == 3) {
                    newCallouts += makeCallout("WEBBED UP!", enemy.x, enemy.y + 36f, 0xFF00E5FF)
                }

                val stunned = (enemy.stunnedSeconds - dt).coerceAtLeast(0f)
                if (stunned > 0f || newWebStacks >= 3) {
                    val decayStacks = if (stunned <= 0.05f && newWebStacks >= 3) 0 else newWebStacks
                    return@mapNotNull enemy.copy(
                        hp = newHp,
                        webbedStacks = decayStacks,
                        stunnedSeconds = if (newWebStacks >= 3 && enemy.stunnedSeconds <= 0f) 3.2f else stunned,
                        isTelegraphingAttack = false
                    )
                }

                // Move enemy along rooftop/air patrol
                val dirToHero = if (x >= enemy.x) 1f else -1f
                val distToHero = hypot(x - enemy.x, y - enemy.y)
                val moveSpeed = if (enemy.type.isAirborne) 75f else 48f
                val nextEx = (enemy.x + dirToHero * moveSpeed * dt).coerceIn(enemy.patrolMinX, enemy.patrolMaxX)

                var cooldown = (enemy.attackCooldown - dt).coerceAtLeast(0f)
                var telegraphing = enemy.isTelegraphingAttack
                var tTimer = enemy.telegraphTimer

                if (distToHero < 280f * senseRangeBonus && cooldown <= 0f && !telegraphing) {
                    telegraphing = true
                    tTimer = 1.15f * senseRangeBonus
                }

                if (telegraphing) {
                    anyTelegraphing = true
                    tTimer -= dt
                    if (tTimer <= 0f) {
                        telegraphing = false
                        cooldown = if (enemy.type.isBoss) 2.2f else 3.2f
                        if (distToHero < 210f && !state.isFlipping) {
                            val dmg = if (enemy.type.isBoss) 14 else 8
                            heroHp = (heroHp - dmg).coerceAtLeast(15)
                            newCallouts += makeCallout("-$dmg HP! DODGE NEXT TIME!", x, y + 40f, 0xFFFF3D43)
                        }
                    }
                }

                enemy.copy(
                    x = nextEx,
                    hp = newHp,
                    webbedStacks = newWebStacks,
                    attackCooldown = cooldown,
                    isTelegraphingAttack = telegraphing,
                    telegraphTimer = tTimer
                )
            }

            // Check objective target collisions (Swing Rings, Hostage Pods, Sonic Bombs, Mystic Rifts)
            var newlyCompletedTargets = 0
            val updatedTargets = state.objectiveTargets.map { target ->
                if (!target.isCompleted && hypot(target.x - x, target.y - y) <= target.radius + 34f) {
                    newlyCompletedTargets++
                    val calloutLabel = when (target.kind) {
                        ObjectiveTargetKind.SWING_RING -> "RING CLEARED! +250"
                        ObjectiveTargetKind.HOSTAGE_POD -> "CIVILIAN RESCUED! +300"
                        ObjectiveTargetKind.SONIC_BOMB -> "BOMB DISARMED! +300"
                        ObjectiveTargetKind.MYSTIC_RIFT -> "RIFT SEALED! +350"
                    }
                    newCallouts += makeCallout(calloutLabel, target.x, target.y + 35f, 0xFF00E676)
                    target.copy(isCompleted = true)
                } else {
                    target
                }
            }

            // Check nearby uncollected backpack in open world
            val foundSet = profile.value.collectiblesFoundSet()
            val closeBackpack = SpiderWorldCatalog.backpacks.firstOrNull { bp ->
                bp.id !in foundSet && hypot(bp.worldX - x, bp.worldY - y) < 110f
            }
            if (closeBackpack != null && hypot(closeBackpack.worldX - x, closeBackpack.worldY - y) < 58f) {
                newlyCollectedBackpack = closeBackpack
            }

            // Check nearby mission beacon in Free Roam
            val closeMission = if (state.activeMission == null) {
                missions.value.firstOrNull { m ->
                    abs(m.targetWorldX - x) < 220f
                }
            } else null

            // Update active mission timer & progress
            val updatedActiveMission = state.activeMission?.let { m ->
                val remTime = (m.timeRemainingSeconds - dt).coerceAtLeast(0f)
                if (remTime <= 0f) {
                    missionTimedOut = true
                }
                m.copy(
                    timeRemainingSeconds = remTime,
                    elapsedSeconds = m.elapsedSeconds + dt,
                    enemiesDefeated = m.enemiesDefeated + defeatedEnemiesInStep,
                    targetsCompleted = m.targetsCompleted + newlyCompletedTargets,
                    score = m.score + defeatedEnemiesInStep * 200 + newlyCompletedTargets * 280
                )
            }

            val currentDistrict = SpiderWorldCatalog.districtAt(x)

            state.copy(
                heroX = x,
                heroY = y,
                heroVx = vx,
                heroVy = vy,
                heroHealth = heroHp,
                webFluid = (state.webFluid + 14f * dt).coerceAtMost(100f),
                focusMeter = (state.focusMeter + (if (isSwinging) 4f * dt else 1f * dt)).coerceAtMost(100f),
                comboTimer = (state.comboTimer - dt).coerceAtLeast(0f),
                comboCount = if (state.comboTimer - dt <= 0f) 0 else state.comboCount,
                isSwinging = isSwinging,
                isPerchedOnRoof = isPerched,
                isWallCrawling = isWallCrawling,
                isZipping = state.zipTimer - dt > 0f,
                zipTimer = (state.zipTimer - dt).coerceAtLeast(0f),
                isFlipping = if (state.isFlipping) state.flipAngle + 720f * dt < 360f else false,
                flipAngle = if (state.isFlipping) (state.flipAngle + 720f * dt) % 360f else 0f,
                isSpiderSenseActive = anyTelegraphing,
                ultimateEffectTimer = (state.ultimateEffectTimer - dt).coerceAtLeast(0f),
                currentDistrict = currentDistrict,
                enemies = updatedEnemies,
                objectiveTargets = updatedTargets,
                projectiles = activeProjectiles,
                callouts = newCallouts.takeLast(8),
                activeMission = updatedActiveMission,
                nearbyMissionBeacon = closeMission,
                nearbyBackpack = closeBackpack
            )
        }

        if (accumulatedDistanceBuffer >= 25f) {
            val delta = accumulatedDistanceBuffer.toInt()
            accumulatedDistanceBuffer = 0f
            viewModelScope.launch {
                repository.awardOpenWorldActivity(bonusXp = 2, bonusTokens = 0, distanceDeltaMeters = delta)
            }
        }

        if (newlyCollectedBackpack != null) {
            collectNearbyBackpack()
        }

        if (missionTimedOut) {
            val active = _worldState.value.activeMission
            if (active != null) {
                _missionOutcome.value = MissionOutcomeSummary(
                    missionId = active.missionId,
                    missionTitle = active.title,
                    isVictory = false,
                    score = active.score,
                    timeTakenSeconds = active.totalTimeSeconds.toInt(),
                    starsEarned = 0,
                    xpEarned = 0,
                    techTokensEarned = 0,
                    failureReason = "Time expired before all mission objectives were secured!"
                )
                _worldState.update { it.copy(activeMission = null) }
            }
        } else {
            checkActiveMissionCompletion()
        }

        // Respawn ambient street crimes in Free Roam if district is quiet
        val postState = _worldState.value
        if (postState.activeMission == null && postState.enemies.isEmpty()) {
            spawnFreeRoamPatrolEncounter(postState.heroX)
        }
    }

    private fun checkActiveMissionCompletion() {
        val state = _worldState.value
        val active = state.activeMission ?: return
        val allEnemiesCleared = state.enemies.isEmpty()
        val allTargetsCleared = state.objectiveTargets.all { it.isCompleted }

        if (allEnemiesCleared && allTargetsCleared) {
            val missionEntity = missions.value.firstOrNull { it.id == active.missionId } ?: return
            val timeTaken = active.elapsedSeconds.toInt().coerceAtLeast(5)
            val timeRatio = active.timeRemainingSeconds / active.totalTimeSeconds.coerceAtLeast(1f)
            val stars = when {
                timeRatio >= 0.45f -> 3
                timeRatio >= 0.20f -> 2
                else -> 1
            }
            val finalScore = active.score + (active.timeRemainingSeconds.toInt() * 15) + (stars * 250)

            _missionOutcome.value = MissionOutcomeSummary(
                missionId = missionEntity.id,
                missionTitle = missionEntity.title,
                isVictory = true,
                score = finalScore,
                timeTakenSeconds = timeTaken,
                starsEarned = stars,
                xpEarned = missionEntity.xpReward,
                techTokensEarned = missionEntity.techTokensReward
            )

            _worldState.update {
                it.copy(
                    activeMission = null,
                    statusBannerText = "VICTORY! ${missionEntity.title} Completed!",
                    callouts = it.callouts + makeCallout(
                        text = "MISSION ACCOMPLISHED! ★$stars",
                        x = it.heroX,
                        y = it.heroY + 60f,
                        colorHex = 0xFFFFC400
                    )
                )
            }

            viewModelScope.launch {
                repository.recordMissionVictory(
                    mission = missionEntity,
                    score = finalScore,
                    timeSeconds = timeTaken,
                    stars = stars
                )
            }
        }
    }

    private fun spawnFreeRoamPatrolEncounter(districtX: Float) {
        val district = SpiderWorldCatalog.districtAt(districtX)
        val midX = (district.startX + district.endX) * 0.5f
        val e1X = (midX - 180f).coerceIn(100f, SpiderWorldCatalog.WORLD_WIDTH - 100f)
        val e2X = (midX + 190f).coerceIn(100f, SpiderWorldCatalog.WORLD_WIDTH - 100f)
        val e1 = createEnemy(EnemyType.STREET_THUG, e1X, roofHeightAt(e1X) + 18f)
        val e2 = createEnemy(EnemyType.HOVER_DRONE, e2X, roofHeightAt(e2X) + 110f)
        _worldState.update { state ->
            state.copy(enemies = listOf(e1, e2))
        }
    }

    private fun createEnemy(type: EnemyType, x: Float, y: Float): EnemyState {
        val building = SpiderWorldCatalog.buildings.minByOrNull { b ->
            abs((b.x + b.width * 0.5f) - x)
        }
        val minX = building?.x ?: (x - 70f)
        val maxX = if (building != null) building.x + building.width else (x + 70f)
        return EnemyState(
            id = ++nextEntityId,
            name = type.displayName,
            type = type,
            x = x,
            y = y,
            hp = type.baseHp,
            maxHp = type.baseHp,
            patrolMinX = minX + 14f,
            patrolMaxX = maxX - 14f
        )
    }

    private fun roofHeightAt(worldX: Float): Float {
        val b = SpiderWorldCatalog.buildings.minByOrNull { building ->
            abs((building.x + building.width * 0.5f) - worldX)
        }
        return b?.height ?: 340f
    }

    private fun getSpeedMultiplier(): Float {
        val suit = equippedSuit()
        val swingLevel = profile.value.webSwingSpeedLevel
        return suit.speedBonus * (1f + (swingLevel - 1) * 0.12f)
    }

    private fun makeCallout(text: String, x: Float, y: Float, colorHex: Long): ComicCallout {
        return ComicCallout(
            id = ++nextEntityId,
            text = text,
            x = x,
            y = y,
            colorHex = colorHex
        )
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = SpiderDatabase.getInstance(context)
                    val repo = SpiderRepository(db.spiderDao())
                    return SpiderGameViewModel(repo) as T
                }
            }
    }
}
