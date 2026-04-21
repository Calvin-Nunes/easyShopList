package com.cnx.easyshoplist.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lista")
data class Lista(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String? = null,
    val dataCriacao: Long = System.currentTimeMillis(),
    val finalizada: Boolean = false
)

