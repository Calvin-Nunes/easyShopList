package com.cnx.easyshoplist.ui.screens.banco_itens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import com.cnx.easyshoplist.data.util.capitalizeName
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ItemDisplay(
    val item: Item,
    val setor: Setor?
)

data class BancoItensUiState(
    val items: List<ItemDisplay> = emptyList(),
    val setores: List<Setor> = emptyList(),
    val isLoading: Boolean = true
)

class BancoItensViewModel(
    private val itemRepository: ItemRepository,
    private val setorRepository: SetorRepository,
    private val listaItemRepository: ListaItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BancoItensUiState())
    val uiState: StateFlow<BancoItensUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                itemRepository.allItems,
                setorRepository.allSetores
            ) { items, setores ->
                val displays = items.map { item ->
                    val setor = item.idSetor?.let { sid -> setores.firstOrNull { it.id == sid } }
                    ItemDisplay(item, setor)
                }.sortedBy { it.item.nome.lowercase() }
                
                BancoItensUiState(items = displays, setores = setores, isLoading = false)
            }.collect { _uiState.value = it }
        }
    }

    fun createItem(nome: String, tipoMedida: TipoMedida, precoBase: Double, idSetor: Long?) {
        viewModelScope.launch {
            itemRepository.insert(
                Item(
                    nome = capitalizeName(nome),
                    tipoMedida = tipoMedida,
                    precoBase = precoBase,
                    idSetor = idSetor
                )
            )
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            val itemAtualizado = item.copy(nome = capitalizeName(item.nome))
            itemRepository.update(itemAtualizado)
            // Propaga o novo preço base para todos os ListaItem das listas ativas
            listaItemRepository.atualizarPrecoNasListasAtivas(itemAtualizado.id, itemAtualizado.precoBase)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch { itemRepository.delete(item) }
    }

    fun createSetor(nome: String) {
        viewModelScope.launch {
            val nextOrdem = (_uiState.value.setores.maxOfOrNull { it.ordem } ?: -1) + 1
            setorRepository.insert(Setor(nome = nome.trim(), ordem = nextOrdem))
        }
    }

    fun updateSetor(setor: Setor) {
        viewModelScope.launch { setorRepository.update(setor) }
    }

    fun deleteSetor(setor: Setor) {
        viewModelScope.launch { setorRepository.delete(setor) }
    }

    fun moveSetorUp(setor: Setor) {
        viewModelScope.launch {
            val sorted = _uiState.value.setores
            val idx = sorted.indexOfFirst { it.id == setor.id }
            if (idx <= 0) return@launch
            val above = sorted[idx - 1]
            setorRepository.update(setor.copy(ordem = above.ordem))
            setorRepository.update(above.copy(ordem = setor.ordem))
        }
    }

    fun moveSetorDown(setor: Setor) {
        viewModelScope.launch {
            val sorted = _uiState.value.setores
            val idx = sorted.indexOfFirst { it.id == setor.id }
            if (idx >= sorted.size - 1) return@launch
            val below = sorted[idx + 1]
            setorRepository.update(setor.copy(ordem = below.ordem))
            setorRepository.update(below.copy(ordem = setor.ordem))
        }
    }

    class Factory(
        private val itemRepository: ItemRepository,
        private val setorRepository: SetorRepository,
        private val listaItemRepository: ListaItemRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BancoItensViewModel(itemRepository, setorRepository, listaItemRepository) as T
        }
    }
}
