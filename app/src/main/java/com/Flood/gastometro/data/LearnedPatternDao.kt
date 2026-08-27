package com.Flood.gastometro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedPatternDao {

    @Insert
    suspend fun insert(pattern: LearnedPattern)

    @Query("SELECT * FROM learned_patterns ORDER BY dateMillis DESC, id DESC")
    fun observeAll(): Flow<List<LearnedPattern>>

    @Query("SELECT * FROM learned_patterns")
    suspend fun getAll(): List<LearnedPattern>

    @Query("DELETE FROM learned_patterns WHERE id = :id")
    suspend fun delete(id: Long)
}