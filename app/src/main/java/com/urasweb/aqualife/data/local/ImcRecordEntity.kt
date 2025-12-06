package com.urasweb.aqualife.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "imc_history")
data class ImcRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val pesoKg: Double,
    val tallaM: Double,
    val perimetroAbdominalCm: Double,
    val imc: Double,
    val clasificacionImc: String,
    val updatedAt: Long,
    val syncStatus: SyncStatus
)
