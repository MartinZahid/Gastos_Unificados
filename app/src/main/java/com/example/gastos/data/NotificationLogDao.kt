package com.example.gastos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {

    @Insert
    suspend fun insert(log: NotificationLog)

    @Query("SELECT * FROM notification_log ORDER BY dateMillis DESC, id DESC LIMIT 200")
    fun observeAll(): Flow<List<NotificationLog>>

    @Query("DELETE FROM notification_log WHERE id NOT IN (SELECT id FROM notification_log ORDER BY dateMillis DESC, id DESC LIMIT 200)")
    suspend fun prune()

    @Query("DELETE FROM notification_log")
    suspend fun clear()

    @Query("UPDATE notification_log SET type = :type WHERE id = :id")
    suspend fun setType(id: Long, type: String)
}