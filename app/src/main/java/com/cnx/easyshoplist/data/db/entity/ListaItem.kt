package com.cnx.easyshoplist.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lista_item",
    foreignKeys = [
        ForeignKey(
            entity = Lista::class,
            parentColumns = ["id"],
            childColumns = ["idLista"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Item::class,
            parentColumns = ["id"],
            childColumns = ["idItem"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("idLista"), Index("idItem")]
)
data class ListaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val idLista: Long,
    val idItem: Long,
    val quantidade: Float = 1f,
    val precoBase: Double = 0.0,
    val precoTotal: Double = 0.0
)

