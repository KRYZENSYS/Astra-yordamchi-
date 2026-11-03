package com.example.data

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

@Entity(tableName = "mission_logs")
data class MissionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commandText: String,
    val detectedIntent: String,
    val voiceSummary: String,
    val architectReport: String,
    val strategistPlanJoined: String,
    val sentinelSecurityNote: String,
    val creativeSpark: String,
    val confidenceScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_blueprints")
data class CustomBlueprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val voicePipeline: String,
    val aiModelsJoined: String,
    val coreFeaturesJoined: String,
    val stepsJoined: String,
    val powerLevel: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface JarvisDao {
    @Query("SELECT * FROM mission_logs ORDER BY timestamp DESC")
    fun getAllMissions(): Flow<List<MissionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionLogEntity)

    @Query("DELETE FROM mission_logs WHERE id = :id")
    suspend fun deleteMissionById(id: Int)

    @Query("DELETE FROM mission_logs")
    suspend fun clearAllMissions()

    @Query("SELECT * FROM custom_blueprints ORDER BY timestamp DESC")
    fun getAllCustomBlueprints(): Flow<List<CustomBlueprintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomBlueprint(blueprint: CustomBlueprintEntity)

    @Query("DELETE FROM custom_blueprints WHERE id = :id")
    suspend fun deleteCustomBlueprintById(id: Int)
}

@Database(
    entities = [MissionLogEntity::class, CustomBlueprintEntity::class],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun jarvisDao(): JarvisDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_nexus.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class JarvisRepository(private val dao: JarvisDao) {
    val allMissions: Flow<List<MissionLogEntity>> = dao.getAllMissions()
    val allCustomBlueprints: Flow<List<CustomBlueprintEntity>> = dao.getAllCustomBlueprints()

    suspend fun saveMission(mission: MissionLogEntity) = dao.insertMission(mission)
    suspend fun deleteMission(id: Int) = dao.deleteMissionById(id)
    suspend fun clearMissions() = dao.clearAllMissions()

    suspend fun saveBlueprint(blueprint: CustomBlueprintEntity) = dao.insertCustomBlueprint(blueprint)
    suspend fun deleteBlueprint(id: Int) = dao.deleteCustomBlueprintById(id)
}
