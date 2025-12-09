package com.urasweb.aqualife.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ImcDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ImcRecordEntity)

    @Query("SELECT * FROM imc_history WHERE syncStatus = :status")
    suspend fun getRecordsByStatus(status: SyncStatus): List<ImcRecordEntity>

    @Query("UPDATE imc_history SET syncStatus = :newStatus WHERE id = :recordId")
    suspend fun updateSyncStatus(recordId: String, newStatus: SyncStatus)

    @Query("SELECT * FROM imc_history WHERE id = :recordId LIMIT 1")
    suspend fun getById(recordId: String): ImcRecordEntity?

    @Query("SELECT * FROM imc_history WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getHistoryForUser(userId: String): List<ImcRecordEntity>

    // Sólo los últimos N registros
    @Query("SELECT * FROM imc_history WHERE userId = :userId ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getLastRecordsForUser(userId: String, limit: Int): List<ImcRecordEntity>
}
