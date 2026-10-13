package com.example.domain.model

enum class RoofType {
    FLAT,
    SPIRE,
    CRANE,
    PAGODA,
    BRIDGE_TOWER,
    NEON_BILLBOARD
}

enum class MissionCategory(val label: String) {
    STORY("Story Op"),
    BOSS("Boss Threat"),
    CRIME_PATROL("Crime Patrol"),
    TIME_TRIAL("Time Trial"),
    STEALTH_RESCUE("Rescue Op")
}

enum class TimeOfDay(val label: String) {
    TWILIGHT_DUSK("Twilight Dusk"),
    NEON_MIDNIGHT("Neon Midnight"),
    GOLDEN_DAWN("Golden Dawn")
}

enum class EnemyType(
    val displayName: String,
    val baseHp: Int,
    val isAirborne: Boolean,
    val isBoss: Boolean
) {
    STREET_THUG("Armed Heist Thug", 60, false, false),
    SHOCK_ENFORCER("Shock-Gauntlet Enforcer", 110, false, false),
    HOVER_DRONE("Oscorp Hunter Drone", 75, true, false),
    RHINO_MECH("Armored Rhino-Mech", 240, false, false),
    DOC_CYBER_OCTO("Dr. Cyber-Storm (Boss)", 550, false, true)
}

enum class ObjectiveTargetKind {
    SWING_RING,
    HOSTAGE_POD,
    SONIC_BOMB,
    MYSTIC_RIFT
}

data class CityDistrict(
    val id: String,
    val name: String,
    val subtitle: String,
    val startX: Float,
    val endX: Float,
    val accentColorHex: Long,
    val landmarkName: String
)

data class Building(
    val id: Int,
    val districtId: String,
    val x: Float,
    val width: Float,
    val height: Float,
    val roofType: RoofType,
    val primaryColorHex: Long,
    val windowColorHex: Long,
    val billboardText: String? = null
)

data class RooftopBackpack(
    val id: String,
    val name: String,
    val loreHint: String,
    val districtId: String,
    val worldX: Float,
    val worldY: Float,
    val tokenReward: Int = 25
)

data class SpiderSuit(
    val id: String,
    val name: String,
    val tagline: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val eyeGlowHex: Long,
    val webColorHex: Long,
    val ultimateName: String,
    val ultimateDescription: String,
    val speedBonus: Float,
    val damageMultiplier: Float,
    val tokenCost: Int
)

data class EnemyState(
    val id: Int,
    val name: String,
    val type: EnemyType,
    val x: Float,
    val y: Float,
    val vx: Float = 0f,
    val vy: Float = 0f,
    val hp: Int,
    val maxHp: Int,
    val webbedStacks: Int = 0,
    val stunnedSeconds: Float = 0f,
    val attackCooldown: Float = 2.0f,
    val isTelegraphingAttack: Boolean = false,
    val telegraphTimer: Float = 0f,
    val patrolMinX: Float,
    val patrolMaxX: Float
)

data class MissionObjectiveTarget(
    val id: Int,
    val label: String,
    val x: Float,
    val y: Float,
    val radius: Float = 52f,
    val isCompleted: Boolean = false,
    val kind: ObjectiveTargetKind
)

data class WebProjectile(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val colorHex: Long
)

data class ComicCallout(
    val id: Int,
    val text: String,
    val x: Float,
    val y: Float,
    val colorHex: Long,
    val lifeRemaining: Float = 1.1f
)

data class ActiveMissionRuntime(
    val missionId: String,
    val title: String,
    val category: MissionCategory,
    val districtName: String,
    val currentInstruction: String,
    val timeRemainingSeconds: Float,
    val totalTimeSeconds: Float,
    val elapsedSeconds: Float = 0f,
    val enemiesDefeated: Int = 0,
    val totalEnemiesToDefeat: Int = 0,
    val targetsCompleted: Int = 0,
    val totalTargets: Int = 0,
    val score: Int = 0
)

data class MissionOutcomeSummary(
    val missionId: String,
    val missionTitle: String,
    val isVictory: Boolean,
    val score: Int,
    val timeTakenSeconds: Int,
    val starsEarned: Int,
    val xpEarned: Int,
    val techTokensEarned: Int,
    val failureReason: String? = null
)
