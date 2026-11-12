package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.PaymentTransaction

@Database(entities = [PaymentTransaction::class], version = 1, exportSchema = false)
abstract class SoundboxDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: SoundboxDatabase? = null

        fun getDatabase(context: Context): SoundboxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SoundboxDatabase::class.java,
                    "live_voice_alert_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
