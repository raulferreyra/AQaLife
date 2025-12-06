package com.urasweb.aqualife.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.urasweb.aqualife.data.repository.AquaRepository
import kotlinx.coroutines.tasks.await

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success()

        // Example: sync IMC records
        val dirtyImc = AquaRepository.getDirtyImc()

        for (record in dirtyImc) {
            val docRef = firestore.collection("users")
                .document(uid)
                .collection("imc_history")
                .document(record.id)

            // send to Firestore
            docRef.set(record).await()

            // TODO: mark as SYNCED in local DB
            // For now omit; lo completamos en el siguiente paso
        }

        return Result.success()
    }
}
