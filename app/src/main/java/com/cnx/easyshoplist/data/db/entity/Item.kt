package com.cnx.easyshoplist.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cnx.easyshoplist.data.enums.TipoMedida

@Entity(
    tableName = "item",
    foreignKeys = [
        ForeignKey(
            entity = Setor::class,
            parentColumns = ["id"],
            childColumns = ["idSetor"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("idSetor")]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val tipoMedida: TipoMedida = TipoMedida.UNIDADE,
    val precoBase: Double = 0.0,
    val idSetor: Long? = null
)

