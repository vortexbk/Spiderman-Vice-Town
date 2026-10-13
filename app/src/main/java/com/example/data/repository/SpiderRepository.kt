package com.example.data.repository

import com.example.data.local.MissionEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SpiderDao
import com.example.domain.model.SpiderWorldCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SpiderRepository(
    private val dao: SpiderDao
) {
    val missionsFlow: Flow<List<MissionEntity>> = dao.observeMissions()

    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map { entity ->
        entity ?: PlayerProfileEntity()
    }

    suspend fun ensureSeeded() {
        if (dao.getMissionCount() == 0) {
            dao.insertMissions(SpiderWorldCatalog.initialMissions)
        }
        if (dao.getProfileOnce() == null) {
            dao.saveProfile(PlayerProfileEntity())
        }
    }

    suspend fun recordMissionVictory(
        mission: MissionEntity,
        score: Int,
        timeSeconds: Int,
        stars: Int
    ) {
        val updatedMission = mission.copy(
            isCompleted = true,
            bestScore = maxOf(mission.bestScore, score),
            bestTimeSeconds = if (mission.bestTimeSeconds == 0) timeSeconds else minOf(mission.bestTimeSeconds, timeSeconds),
            starsEarned = maxOf(mission.starsEarned, stars)
        )
        dao.updateMission(updatedMission)

        val currentProfile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val repeatMultiplier = if (mission.isCompleted) 0.5f else 1.0f
        val earnedXp = (mission.xpReward * repeatMultiplier).toInt()
        val earnedTokens = (mission.techTokensReward * repeatMultiplier).toInt()

        val newTotalXp = currentProfile.xp + earnedXp
        val newLevel = 1 + (newTotalXp / 400)
        dao.saveProfile(
            currentProfile.copy(
                xp = newTotalXp,
                level = newLevel,
                techTokens = currentProfile.techTokens + earnedTokens,
                totalCrimesStopped = currentProfile.totalCrimesStopped + 1
            )
        )
    }

    suspend fun awardOpenWorldActivity(
        bonusXp: Int,
        bonusTokens: Int,
        distanceDeltaMeters: Int = 0,
        crimeStopped: Boolean = false
    ) {
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        val newTotalXp = current.xp + bonusXp
        val newLevel = 1 + (newTotalXp / 400)
        dao.saveProfile(
            current.copy(
                xp = newTotalXp,
                level = newLevel,
                techTokens = current.techTokens + bonusTokens,
                totalDistanceSwungMeters = current.totalDistanceSwungMeters + distanceDeltaMeters,
                totalCrimesStopped = current.totalCrimesStopped + if (crimeStopped) 1 else 0
            )
        )
    }

    suspend fun collectBackpack(backpackId: String, tokenReward: Int, xpReward: Int = 120): Boolean {
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        val existingSet = current.collectiblesFoundSet()
        if (backpackId in existingSet) return false

        val updatedSet = existingSet + backpackId
        val newTotalXp = current.xp + xpReward
        val newLevel = 1 + (newTotalXp / 400)
        dao.saveProfile(
            current.copy(
                xp = newTotalXp,
                level = newLevel,
                techTokens = current.techTokens + tokenReward,
                collectiblesFoundCsv = updatedSet.joinToString(",")
            )
        )
        return true
    }

    suspend fun unlockOrEquipSuit(suitId: String, cost: Int): Boolean {
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        val unlocked = current.unlockedSuitsSet()
        return if (suitId in unlocked) {
            dao.saveProfile(current.copy(equippedSuitId = suitId))
            true
        } else if (current.techTokens >= cost) {
            val newUnlocked = (unlocked + suitId).joinToString(",")
            dao.saveProfile(
                current.copy(
                    techTokens = current.techTokens - cost,
                    unlockedSuitsCsv = newUnlocked,
                    equippedSuitId = suitId
                )
            )
            true
        } else {
            false
        }
    }

    suspend fun upgradeTechSystem(upgradeKey: String, cost: Int): Boolean {
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        if (current.techTokens < cost) return false

        val updated = when (upgradeKey) {
            "swing_speed" -> {
                if (current.webSwingSpeedLevel >= 5) return false
                current.copy(
                    techTokens = current.techTokens - cost,
                    webSwingSpeedLevel = current.webSwingSpeedLevel + 1
                )
            }
            "combat_damage" -> {
                if (current.combatDamageLevel >= 5) return false
                current.copy(
                    techTokens = current.techTokens - cost,
                    combatDamageLevel = current.combatDamageLevel + 1
                )
            }
            "web_capacity" -> {
                if (current.webShooterCapacityLevel >= 5) return false
                current.copy(
                    techTokens = current.techTokens - cost,
                    webShooterCapacityLevel = current.webShooterCapacityLevel + 1
                )
            }
            "spider_sense" -> {
                if (current.spiderSenseRangeLevel >= 5) return false
                current.copy(
                    techTokens = current.techTokens - cost,
                    spiderSenseRangeLevel = current.spiderSenseRangeLevel + 1
                )
            }
            else -> return false
        }
        dao.saveProfile(updated)
        return true
    }
}
