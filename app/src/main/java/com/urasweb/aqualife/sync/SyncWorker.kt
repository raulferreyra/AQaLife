package com.urasweb.aqualife.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.urasweb.aqualife.data.local.AquaDatabase
import com.urasweb.aqualife.data.local.ImcRecordEntity
import com.urasweb.aqualife.data.local.SyncStatus
import com.urasweb.aqualife.data.repository.AquaRepository
import kotlinx.coroutines.tasks.await

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return Result.success()
        val uid = currentUser.uid

        // Init local DB y repo (idempotente)
        AquaDatabase.init(applicationContext)
        val db = AquaDatabase.getInstance()
        AquaRepository.init(db)
        val imcDao = db.imcDao()

        val firestore = FirebaseFirestore.getInstance()
        val userImcCollection = firestore
            .collection("users")
            .document(uid)
            .collection("imc_history")

        try {
            // 1) Upload local DIRTY → Firestore
            val dirtyRecords = AquaRepository.getDirtyImc()
            for (record in dirtyRecords) {
                val data = mapOf(
                    "userId" to record.userId,
                    "pesoKg" to record.pesoKg,
                    "tallaM" to record.tallaM,
                    "perimetroAbdominalCm" to record.perimetroAbdominalCm,
                    "imc" to record.imc,
                    "clasificacionImc" to record.clasificacionImc,
                    "updatedAt" to record.updatedAt
                )

                try {
                    userImcCollection
                        .document(record.id)
                        .set(data)
                        .await()

                    imcDao.updateSyncStatus(record.id, SyncStatus.SYNCED)
                } catch (e: Exception) {
                    imcDao.updateSyncStatus(record.id, SyncStatus.ERROR)
                }
            }

            // 2) Pull de Firestore → local (solo nuevos o más recientes)
            val prefs = applicationContext
                .getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

            val lastSync = prefs.getLong("imc_last_sync", 0L)
            val now = System.currentTimeMillis()

            val baseQuery = if (lastSync > 0L) {
                userImcCollection.whereGreaterThan("updatedAt", lastSync)
            } else {
                userImcCollection
            }

            val snapshot = baseQuery.get().await()

            for (doc in snapshot.documents) {
                val id = doc.id
                val remoteUpdatedAt = doc.getLong("updatedAt") ?: 0L

                val local = imcDao.getById(id)

                val shouldApplyRemote =
                    local == null || remoteUpdatedAt > local.updatedAt

                if (shouldApplyRemote) {
                    val entity = ImcRecordEntity(
                        id = id,
                        userId = doc.getString("userId") ?: uid,
                        pesoKg = doc.getDouble("pesoKg") ?: 0.0,
                        tallaM = doc.getDouble("tallaM") ?: 0.0,
                        perimetroAbdominalCm = doc.getDouble("perimetroAbdominalCm") ?: 0.0,
                        imc = doc.getDouble("imc") ?: 0.0,
                        clasificacionImc = doc.getString("clasificacionImc") ?: "",
                        updatedAt = remoteUpdatedAt,
                        syncStatus = SyncStatus.SYNCED
                    )
                    imcDao.insert(entity)
                }
            }

            prefs.edit()
                .putLong("imc_last_sync", now)
                .apply()

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
