package com.urasweb.aqualife.data.repository

import com.urasweb.aqualife.data.local.AquaDatabase
import com.urasweb.aqualife.data.local.ImcRecordEntity
import com.urasweb.aqualife.data.local.SyncStatus
import java.util.UUID
import kotlin.math.round

object AquaRepository {

    private lateinit var db: AquaDatabase
    private var currentUserId: String? = null

    fun init(database: AquaDatabase) {
        db = database
    }

    fun setCurrentUser(uid: String) {
        currentUserId = uid
    }

    /**
     * Guardar un registro IMC genérico (se puede usar desde cualquier parte).
     */
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

    /**
     * Usado específicamente en el Setup:
     * recibe altura en cm y peso en kg, calcula IMC y lo guarda.
     */
    suspend fun saveInitialSetupImc(
        alturaCm: Float,
        pesoKg: Float,
        perimetroAbdominalCm: Double? = null
    ) {
        val alturaM = alturaCm / 100.0
        val peso = pesoKg.toDouble()

        val imc = peso / (alturaM * alturaM)
        val imcRedondeado = (round(imc * 100.0) / 100.0)
        val clasificacion = clasificarImc(imcRedondeado)

        saveImc(
            pesoKg = peso,
            tallaM = alturaM,
            perimetroAbdominalCm = perimetroAbdominalCm ?: 0.0,
            imc = imcRedondeado,
            clasificacionImc = clasificacion
        )
    }

    suspend fun getDirtyImc(): List<ImcRecordEntity> {
        return db.imcDao().getRecordsByStatus(SyncStatus.DIRTY)
    }

    suspend fun getImcHistoryForCurrentUser(): List<ImcRecordEntity> {
        val uid = currentUserId
            ?: throw IllegalStateException("Current user not set in AquaRepository")
        return db.imcDao().getHistoryForUser(uid)
    }


    private fun clasificarImc(imc: Double): String {
        return when {
            imc < 18.5 -> "Bajo peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            else -> "Obesidad"
        }
    }
}
