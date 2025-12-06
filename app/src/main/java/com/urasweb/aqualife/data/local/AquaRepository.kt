package com.urasweb.aqualife.data.repository

import com.urasweb.aqualife.data.local.AquaDatabase
import com.urasweb.aqualife.data.local.ImcRecordEntity
import com.urasweb.aqualife.data.local.SyncStatus
import java.util.UUID

object AquaRepository {

    private lateinit var db: AquaDatabase
    private var currentUserId: String? = null

    fun init(database: AquaDatabase) {
        db = database
    }

    fun setCurrentUser(uid: String) {
        currentUserId = uid
    }

    suspend fun saveImc(
        pesoKg: Double,
        tallaM: Double,
        perimetroAbdominalCm: Double,
        imc: Double,
        clasificacionImc: String
    ) {
        val uid = currentUserId
            ?: throw IllegalStateException("Current user not set in AquaRepository")

        val record = ImcRecordEntity(
            id = UUID.randomUUID().toString(),
            userId = uid,
            pesoKg = pesoKg,
            tallaM = tallaM,
            perimetroAbdominalCm = perimetroAbdominalCm,
            imc = imc,
            clasificacionImc = clasificacionImc,
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.DIRTY
        )

        db.imcDao().insert(record)
    }

    suspend fun getDirtyImc(): List<ImcRecordEntity> {
        return db.imcDao().getRecordsByStatus(SyncStatus.DIRTY)
    }
}
