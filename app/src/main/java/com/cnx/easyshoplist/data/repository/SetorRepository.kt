package com.cnx.easyshoplist.data.repository

import com.cnx.easyshoplist.data.db.dao.SetorDao
import com.cnx.easyshoplist.data.db.entity.Setor
import kotlinx.coroutines.flow.Flow

class SetorRepository(private val setorDao: SetorDao) {

    val allSetores: Flow<List<Setor>> = setorDao.getAllFlow()

    suspend fun insert(setor: Setor): Long = setorDao.insert(setor)
    suspend fun update(setor: Setor) = setorDao.update(setor)
    suspend fun delete(setor: Setor) = setorDao.delete(setor)
    suspend fun getAll(): List<Setor> = setorDao.getAll()
    suspend fun getById(id: Long): Setor? = setorDao.getById(id)
}

