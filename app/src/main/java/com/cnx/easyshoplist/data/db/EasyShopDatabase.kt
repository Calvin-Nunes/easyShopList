package com.cnx.easyshoplist.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cnx.easyshoplist.data.db.converter.Converters
import com.cnx.easyshoplist.data.db.dao.ItemDao
import com.cnx.easyshoplist.data.db.dao.ListaDao
import com.cnx.easyshoplist.data.db.dao.ListaItemDao
import com.cnx.easyshoplist.data.db.dao.SetorDao
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.db.entity.ListaItem
import com.cnx.easyshoplist.data.db.entity.Setor

@Database(
    entities = [Lista::class, ListaItem::class, Item::class, Setor::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EasyShopDatabase : RoomDatabase() {

    abstract fun listaDao(): ListaDao
    abstract fun listaItemDao(): ListaItemDao
    abstract fun itemDao(): ItemDao
    abstract fun setorDao(): SetorDao

    companion object {
        @Volatile
        private var INSTANCE: EasyShopDatabase? = null

        private val fkCallback = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        fun getDatabase(context: Context): EasyShopDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    EasyShopDatabase::class.java,
                    "easy_shop_database"
                )
                    .addCallback(fkCallback)
                    .build().also { INSTANCE = it }
            }
        }
    }
}

