package com.cnx.easyshoplist.data.repository

import com.cnx.easyshoplist.data.db.dao.ItemDao
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.util.capitalizeName
import com.cnx.easyshoplist.data.util.normalizeString
import kotlinx.coroutines.flow.Flow

class ItemRepository(private val itemDao: ItemDao) {

    val allItems: Flow<List<Item>> = itemDao.getAllFlow()

    suspend fun insert(item: Item): Long = itemDao.insert(item)
    suspend fun update(item: Item) = itemDao.update(item)
    suspend fun delete(item: Item) = itemDao.delete(item)
    suspend fun getAll(): List<Item> = itemDao.getAll()
    suspend fun getById(id: Long): Item? = itemDao.getById(id)

    /**
     * Upsert: busca item pelo nome normalizado (sem acentos, lowercase) em Kotlin —
     * evita a limitação do LOWER() do SQLite que não remove acentos.
     * - Se existir: atualiza tipoMedida e precoBase.
     * - Se não existir: insere novo.
     * Retorna o id do item.
     */
    suspend fun upsertByNome(
        nome: String,
        tipoMedida: TipoMedida,
        precoBase: Double,
        idSetor: Long? = null
    ): Long {
        val nomeNorm = normalizeString(nome)
        val existing = itemDao.getAll().firstOrNull { normalizeString(it.nome) == nomeNorm }
        return if (existing != null) {
            itemDao.update(existing.copy(tipoMedida = tipoMedida, precoBase = precoBase))
            existing.id
        } else {
            itemDao.insert(Item(nome = capitalizeName(nome), tipoMedida = tipoMedida, precoBase = precoBase, idSetor = idSetor))
        }
    }
}
