package com.cnx.easyshoplist.ui.screens.listas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cnx.easyshoplist.data.db.dao.ListaResumo
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListaComResumo(
    val lista: Lista,
    val totalItens: Int = 0,
    val totalPreco: Double = 0.0
)

class ListasViewModel(
    private val listaRepository: ListaRepository,
    private val listaItemRepository: ListaItemRepository
) : ViewModel() {

    val listas: StateFlow<List<ListaComResumo>> =
        combine(listaRepository.allListas, listaItemRepository.getResumoFlow()) { listas, resumos ->
            val resumoMap: Map<Long, ListaResumo> = resumos.associateBy { it.idLista }
            listas.map { lista ->
                val r = resumoMap[lista.id]
                ListaComResumo(lista, r?.totalItens ?: 0, r?.totalPreco ?: 0.0)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deletarLista(lista: Lista) {
        viewModelScope.launch { listaRepository.delete(lista) }
    }

    class Factory(
        private val listaRepository: ListaRepository,
        private val listaItemRepository: ListaItemRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ListasViewModel(listaRepository, listaItemRepository) as T
        }
    }
}
