package com.cnx.easyshoplist.data.db.converter

import androidx.room.TypeConverter
import com.cnx.easyshoplist.data.enums.TipoMedida

class Converters {
    @TypeConverter
    fun fromTipoMedida(value: TipoMedida): String = value.name

    @TypeConverter
    fun toTipoMedida(value: String): TipoMedida = TipoMedida.valueOf(value)
}

