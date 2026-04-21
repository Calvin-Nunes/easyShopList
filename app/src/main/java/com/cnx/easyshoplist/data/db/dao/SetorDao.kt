package com.cnx.easyshoplist.data.db.dao

import androidx.room.*
import com.cnx.easyshoplist.data.db.entity.Setor
import kotlinx.coroutines.flow.Flow

@Dao
interface SetorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setor: Setor): Long

    @Update
    suspend fun update(setor: Setor)

    @Delete
    suspend fun delete(setor: Setor)

    @Query("SELECT * FROM setor ORDER BY ordem ASC, nome ASC")
    fun getAllFlow(): Flow<List<Setor>>

    @Query("SELECT * FROM setor ORDER BY ordem ASC, nome ASC")
    suspend fun getAll(): List<Setor>

    @Query("SELECT * FROM setor WHERE id = :id")
    suspend fun getById(id: Long): Setor?
}

