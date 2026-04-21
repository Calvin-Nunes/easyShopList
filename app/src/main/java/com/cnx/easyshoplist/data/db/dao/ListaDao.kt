package com.cnx.easyshoplist.data.db.dao

import androidx.room.*
import com.cnx.easyshoplist.data.db.entity.Lista
import kotlinx.coroutines.flow.Flow

@Dao
interface ListaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lista: Lista): Long

    @Update
    suspend fun update(lista: Lista)

    @Delete
    suspend fun delete(lista: Lista)

    @Query("SELECT * FROM lista ORDER BY dataCriacao DESC")
    fun getAllFlow(): Flow<List<Lista>>

    @Query("SELECT * FROM lista WHERE id = :id")
    suspend fun getById(id: Long): Lista?

    @Query("SELECT * FROM lista WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<Lista?>
}

