package com.urasweb.aqualife.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ImcRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AquaDatabase : RoomDatabase() {

    abstract fun imcDao(): ImcDao

    companion object {
        @Volatile
        private var INSTANCE: AquaDatabase? = null

        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            AquaDatabase::class.java,
                            "aqualife_local.db"
                        ).fallbackToDestructiveMigration()
                            .build()
                    }
                }
            }
        }

        fun getInstance(): AquaDatabase {
            return INSTANCE
                ?: throw IllegalStateException("AquaDatabase not initialized. Call AquaDatabase.init(context) first.")
        }
    }
}
