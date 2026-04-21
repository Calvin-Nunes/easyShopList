package com.cnx.easyshoplist.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setor")
data class Setor(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val ordem: Int = 0
)

