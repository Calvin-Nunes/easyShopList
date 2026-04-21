package com.cnx.easyshoplist.data.db.dao

import androidx.room.*
import com.cnx.easyshoplist.data.db.entity.ListaItem
import kotlinx.coroutines.flow.Flow

data class ListaResumo(
    val idLista: Long,
    val totalItens: Int,
    val totalPreco: Double
)

@Dao
interface ListaItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(listaItem: ListaItem): Long

    @Update
    suspend fun update(listaItem: ListaItem)

    @Delete
    suspend fun delete(listaItem: ListaItem)

    @Query("SELECT * FROM lista_item WHERE idLista = :idLista")
    fun getByListaIdFlow(idLista: Long): Flow<List<ListaItem>>

    @Query("SELECT * FROM lista_item WHERE idLista = :idLista")
    suspend fun getByListaId(idLista: Long): List<ListaItem>

    @Query("DELETE FROM lista_item WHERE idLista = :idLista")
    suspend fun deleteByListaId(idLista: Long)

    @Query("SELECT * FROM lista_item WHERE id = :id")
    suspend fun getById(id: Long): ListaItem?

    @Query("SELECT idLista, COUNT(*) as totalItens, SUM(precoTotal) as totalPreco FROM lista_item GROUP BY idLista")
    fun getResumoFlow(): Flow<List<ListaResumo>>

    /**
     * Atualiza precoBase e precoTotal de todos os itens de listas ATIVAS (não finalizadas)
     * que referenciam o item do banco [idItem].
     * precoTotal é recalculado como novoPreco * quantidade.
     */
    @Query("""
        UPDATE lista_item
        SET precoBase = :novoPreco,
            precoTotal = :novoPreco * quantidade
        WHERE idItem = :idItem
          AND idLista IN (SELECT id FROM lista WHERE finalizada = 0)
    """)
    suspend fun atualizarPrecoNasListasAtivas(idItem: Long, novoPreco: Double)
}

