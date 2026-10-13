package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val district: String,
    val category: String,
    val description: String,
    val objectiveSummary: String,
    val difficulty: Int,
    val xpReward: Int,
    val techTokensReward: Int,
    val targetWorldX: Float,
    val targetWorldY: Float,
    val isCompleted: Boolean = false,
    val bestScore: Int = 0,
    val bestTimeSeconds: Int = 0,
    val starsEarned: Int = 0
)

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val xp: Int = 0,
    val techTokens: Int = 60,
    val equippedSuitId: String = "classic_crimson",
    val unlockedSuitsCsv: String = "classic_crimson",
    val webSwingSpeedLevel: Int = 1,
    val combatDamageLevel: Int = 1,
    val webShooterCapacityLevel: Int = 1,
    val spiderSenseRangeLevel: Int = 1,
    val totalDistanceSwungMeters: Int = 0,
    val totalCrimesStopped: Int = 0,
    val collectiblesFoundCsv: String = ""
) {
    fun unlockedSuitsSet(): Set<String> =
        unlockedSuitsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun collectiblesFoundSet(): Set<String> =
        collectiblesFoundCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}

@Dao
interface SpiderDao {
    @Query("SELECT * FROM missions ORDER BY difficulty ASC, id ASC")
    fun observeMissions(): Flow<List<MissionEntity>>

    @Query("SELECT COUNT(*) FROM missions")
    suspend fun getMissionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<MissionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMission(mission: MissionEntity)

    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: PlayerProfileEntity)
}

@Database(
    entities = [MissionEntity::class, PlayerProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SpiderDatabase : RoomDatabase() {
    abstract fun spiderDao(): SpiderDao

    companion object {
        @Volatile
        private var INSTANCE: SpiderDatabase? = null

        fun getInstance(context: Context): SpiderDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpiderDatabase::class.java,
                    "spider_city_open_world.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
