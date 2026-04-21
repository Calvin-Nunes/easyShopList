package com.cnx.easyshoplist.ui.screens.usar_lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.db.entity.ListaItem
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UsarItemDisplay(
    val listaItem: ListaItem,
    val item: Item,
    val setor: Setor?,
    val marcado: Boolean = false
)

data class UsarListaUiState(
    val lista: Lista? = null,
    val itensPorSetor: Map<Setor?, List<UsarItemDisplay>> = emptyMap(),
    val isLoading: Boolean = true
)

class UsarListaViewModel(
    private val listId: Long,
    private val listaRepository: ListaRepository,
    private val listaItemRepository: ListaItemRepository,
    private val itemRepository: ItemRepository,
    private val setorRepository: SetorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsarListaUiState())
    val uiState: StateFlow<UsarListaUiState> = _uiState.asStateFlow()

    private val _marcados = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    init {
        viewModelScope.launch {
            combine(
                listaRepository.getByIdFlow(listId),
                listaItemRepository.getByListaIdFlow(listId),
                setorRepository.allSetores
            ) { lista, listaItens, setores ->
                Triple(lista, listaItens, setores)
            }.combine(_marcados) { (lista, listaItens, setores), marcados ->
                val displays = listaItens.mapNotNull { li ->
                    val item = itemRepository.getById(li.idItem) ?: return@mapNotNull null
                    val setor = item.idSetor?.let { sid -> setores.firstOrNull { it.id == sid } }
                    UsarItemDisplay(li, item, setor, marcado = marcados[li.id] ?: false)
                }
                val agrupado: Map<Setor?, List<UsarItemDisplay>> = buildMap {
                    displays.filter { it.setor != null }
                        .groupBy { it.setor }
                        .toSortedMap(compareBy { it?.ordem })
                        .let { putAll(it) }
                    val semSetor = displays.filter { it.setor == null }
                    if (semSetor.isNotEmpty()) put(null, semSetor)
                }
                UsarListaUiState(lista = lista, itensPorSetor = agrupado, isLoading = false)
            }.collect { _uiState.value = it }
        }
    }

    fun toggleMarcado(listaItemId: Long) {
        _marcados.update { current ->
            current.toMutableMap().also { it[listaItemId] = !(it[listaItemId] ?: false) }
        }
    }

    fun concluirCompra(onConcluido: () -> Unit) {
        viewModelScope.launch {
            val lista = listaRepository.getById(listId) ?: return@launch
            listaRepository.update(lista.copy(finalizada = true))
            onConcluido()
        }
    }

    class Factory(
        private val listId: Long,
        private val listaRepository: ListaRepository,
        private val listaItemRepository: ListaItemRepository,
        private val itemRepository: ItemRepository,
        private val setorRepository: SetorRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return UsarListaViewModel(
                listId, listaRepository, listaItemRepository, itemRepository, setorRepository
            ) as T
        }
    }
}

