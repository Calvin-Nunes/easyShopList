package com.cnx.easyshoplist.data.db.dao

import androidx.room.*
import com.cnx.easyshoplist.data.db.entity.Item
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT * FROM item ORDER BY nome ASC")
    fun getAllFlow(): Flow<List<Item>>

    @Query("SELECT * FROM item ORDER BY nome ASC")
    suspend fun getAll(): List<Item>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun getById(id: Long): Item?


}

