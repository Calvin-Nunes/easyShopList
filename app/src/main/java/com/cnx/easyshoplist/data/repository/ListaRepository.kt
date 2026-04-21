package com.cnx.easyshoplist.data.repository

import com.cnx.easyshoplist.data.db.dao.ListaDao
import com.cnx.easyshoplist.data.db.entity.Lista
import kotlinx.coroutines.flow.Flow

class ListaRepository(private val listaDao: ListaDao) {

    val allListas: Flow<List<Lista>> = listaDao.getAllFlow()

    suspend fun insert(lista: Lista): Long = listaDao.insert(lista)
    suspend fun update(lista: Lista) = listaDao.update(lista)
    suspend fun delete(lista: Lista) = listaDao.delete(lista)
    suspend fun getById(id: Long): Lista? = listaDao.getById(id)
    fun getByIdFlow(id: Long): Flow<Lista?> = listaDao.getByIdFlow(id)
}

