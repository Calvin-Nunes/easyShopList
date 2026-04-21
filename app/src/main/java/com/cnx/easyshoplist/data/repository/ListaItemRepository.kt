package com.cnx.easyshoplist.data.repository

import com.cnx.easyshoplist.data.db.dao.ListaItemDao
import com.cnx.easyshoplist.data.db.dao.ListaResumo
import com.cnx.easyshoplist.data.db.entity.ListaItem
import kotlinx.coroutines.flow.Flow

class ListaItemRepository(private val listaItemDao: ListaItemDao) {

    fun getByListaIdFlow(idLista: Long): Flow<List<ListaItem>> =
        listaItemDao.getByListaIdFlow(idLista)

    fun getResumoFlow(): Flow<List<ListaResumo>> = listaItemDao.getResumoFlow()

    suspend fun insert(listaItem: ListaItem): Long = listaItemDao.insert(listaItem)
    suspend fun update(listaItem: ListaItem) = listaItemDao.update(listaItem)
    suspend fun delete(listaItem: ListaItem) = listaItemDao.delete(listaItem)
    suspend fun deleteByListaId(idLista: Long) = listaItemDao.deleteByListaId(idLista)
    suspend fun getById(id: Long): ListaItem? = listaItemDao.getById(id)

    /** Propaga o novo preço base de um item do banco para todas as listas ativas. */
    suspend fun atualizarPrecoNasListasAtivas(idItem: Long, novoPreco: Double) =
        listaItemDao.atualizarPrecoNasListasAtivas(idItem, novoPreco)
}

