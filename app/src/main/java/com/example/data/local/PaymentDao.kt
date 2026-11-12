package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PaymentTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payments WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getPaymentsBetween(startTime: Long, endTime: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payments WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getPaymentsSince(startTime: Long): Flow<List<PaymentTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentTransaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentTransaction>)

    @Delete
    suspend fun delete(payment: PaymentTransaction)

    @Query("DELETE FROM payments")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM payments")
    suspend fun count(): Int

    @Query("SELECT SUM(amount) FROM payments WHERE status = 'SUCCESS'")
    suspend fun getTotalSuccessAmount(): Double?

    @Query("SELECT SUM(amount) FROM payments WHERE status = 'SUCCESS' AND timestamp >= :startOfDay")
    suspend fun getTodaySuccessAmount(startOfDay: Long): Double?
}
