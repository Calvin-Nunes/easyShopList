package com.cnx.easyshoplist.ui.screens.lista_detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.db.entity.ListaItem
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ListaItemDisplay(
    val listaItem: ListaItem,
    val item: Item,
    val setor: Setor?
)

data class ListaDetalheUiState(
    val lista: Lista? = null,
    val itensPorSetor: Map<Setor?, List<ListaItemDisplay>> = emptyMap(),
    val setoresDisponiveis: List<Setor> = emptyList(),
    val todosItens: List<Item> = emptyList(),
    val isLoading: Boolean = true
)

class ListaDetalheViewModel(
    private val listId: Long,
    private val listaRepository: ListaRepository,
    private val listaItemRepository: ListaItemRepository,
    private val itemRepository: ItemRepository,
    private val setorRepository: SetorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ListaDetalheUiState())
    val uiState: StateFlow<ListaDetalheUiState> = _uiState.asStateFlow()

    /**
     * ID da lista ativa.
     * Permanece -1L para novas listas enquanto nenhum item for adicionado (criação lazy).
     */
    private var currentListId: Long = listId

    /** Nome digitado pelo usuário antes de qualquer item ser criado (nova lista ainda não persistida). */
    private var pendingNome: String? = null

    /** Job do observador de dados atual; cancelado ao trocar de modo (static → full). */
    private var dataObserveJob: Job? = null

    init {
        if (listId == -1L) {
            // Nova lista: ainda não persiste no banco — aguarda o primeiro item.
            // Observa apenas setores e banco de itens (necessários para autocompletar).
            startStaticObserver()
        } else {
            startFullObserver()
        }
    }

    // -------------------------------------------------------------------------
    // Observadores
    // -------------------------------------------------------------------------

    /**
     * Observa setores e banco de itens para autocompletar enquanto a lista não existe no banco.
     */
    private fun startStaticObserver() {
        dataObserveJob?.cancel()
        dataObserveJob = viewModelScope.launch {
            combine(
                setorRepository.allSetores,
                itemRepository.allItems
            ) { setores, todosItens ->
                _uiState.value.copy(
                    setoresDisponiveis = setores,
                    todosItens = todosItens,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    /**
     * Observa todos os dados: lista, itens, setores e banco de itens.
     * Chamado após a lista ser criada no banco (ID definido em [currentListId]).
     */
    private fun startFullObserver() {
        dataObserveJob?.cancel()
        dataObserveJob = viewModelScope.launch {
            combine(
                listaRepository.getByIdFlow(currentListId),
                listaItemRepository.getByListaIdFlow(currentListId),
                setorRepository.allSetores,
                itemRepository.allItems
            ) { lista, listaItens, setores, todosItens ->
                val displays = listaItens.mapNotNull { li ->
                    val item = todosItens.firstOrNull { it.id == li.idItem }
                    if (item == null) {
                        viewModelScope.launch { listaItemRepository.delete(li) }
                        return@mapNotNull null
                    }
                    val setor = item.idSetor?.let { sid -> setores.firstOrNull { it.id == sid } }
                    ListaItemDisplay(li, item, setor)
                }
                val agrupado: Map<Setor?, List<ListaItemDisplay>> = buildMap {
                    displays.filter { it.setor != null }
                        .groupBy { it.setor }
                        .toSortedMap(compareBy { it?.ordem })
                        .let { putAll(it) }
                    val semSetor = displays.filter { it.setor == null }
                    if (semSetor.isNotEmpty()) put(null, semSetor)
                }
                ListaDetalheUiState(
                    lista = lista,
                    itensPorSetor = agrupado,
                    setoresDisponiveis = setores,
                    todosItens = todosItens,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    // -------------------------------------------------------------------------
    // Ações
    // -------------------------------------------------------------------------

    fun atualizarNomeLista(novoNome: String) {
        viewModelScope.launch {
            if (currentListId == -1L) {
                // Lista ainda não criada: armazena o nome para aplicar ao persistir
                pendingNome = novoNome.trim().ifBlank { null }
                // Reflete o nome no UI sem persistir no banco
                _uiState.value = _uiState.value.copy(
                    lista = Lista(nome = pendingNome)
                )
                return@launch
            }
            val lista = listaRepository.getById(currentListId) ?: return@launch
            listaRepository.update(lista.copy(nome = novoNome.trim().ifBlank { null }))
        }
    }

    fun adicionarItem(nome: String, tipoMedida: TipoMedida, preco: Double, quantidade: Float = 1f) {
        viewModelScope.launch {
            // Criação lazy: persiste a lista no banco somente ao adicionar o primeiro item.
            // Qualquer nome digitado antes deste momento é aplicado aqui via pendingNome.
            if (currentListId == -1L) {
                currentListId = listaRepository.insert(Lista(nome = pendingNome))
                startFullObserver()
            }
            val itemId = itemRepository.upsertByNome(nome, tipoMedida, preco)
            listaItemRepository.insert(
                ListaItem(
                    idLista = currentListId,
                    idItem = itemId,
                    quantidade = quantidade,
                    precoBase = preco,
                    precoTotal = preco * quantidade
                )
            )
        }
    }

    fun removerItem(listaItem: ListaItem) {
        viewModelScope.launch { listaItemRepository.delete(listaItem) }
    }

    fun editarQuantidadeItem(listaItem: ListaItem, novaQuantidade: Float) {
        viewModelScope.launch {
            listaItemRepository.update(
                listaItem.copy(
                    quantidade = novaQuantidade,
                    precoTotal = listaItem.precoBase * novaQuantidade
                )
            )
        }
    }

    /**
     * Remove a lista se estiver vazia (para listas existentes que possam ter ficado sem itens).
     * Para novas listas (listId == -1L) que ainda não foram persistidas, é no-op — a lista
     * simplesmente nunca existiu no banco.
     */
    fun deleteIfEmpty() {
        if (listId != -1L) return         // nunca apaga lista existente
        if (currentListId == -1L) return  // nova lista nunca foi persistida, nada a fazer
        viewModelScope.launch {
            val itens = _uiState.value.itensPorSetor.values.flatten()
            val lista = listaRepository.getById(currentListId) ?: return@launch
            if (itens.isEmpty() && lista.nome == null) {
                listaRepository.delete(lista)
            }
        }
    }

    fun getListId(): Long = currentListId

    class Factory(
        private val listId: Long,
        private val listaRepository: ListaRepository,
        private val listaItemRepository: ListaItemRepository,
        private val itemRepository: ItemRepository,
        private val setorRepository: SetorRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ListaDetalheViewModel(
                listId, listaRepository, listaItemRepository, itemRepository, setorRepository
            ) as T
        }
    }
}
