package com.cnx.easyshoplist.ui.screens.lista_detalhe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
/**
 * Ponto de entrada da tela de Detalhe de Lista.
 * Responsabilidade: instanciar o ViewModel, coletar o estado e repassar ao [ListaDetalheContent].
 * Nao contém código visual — apenas ligacao entre ViewModel e template.
 */
@Composable
fun ListaDetalheScreen(
    listId: Long,
    listaRepository: ListaRepository,
    listaItemRepository: ListaItemRepository,
    itemRepository: ItemRepository,
    setorRepository: SetorRepository,
    onNavigateUp: () -> Unit,
    onUsarLista: (Long) -> Unit
) {
    val vm: ListaDetalheViewModel = viewModel(
        key = "lista_detalhe_$listId",
        factory = ListaDetalheViewModel.Factory(
            listId, listaRepository, listaItemRepository, itemRepository, setorRepository
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    ListaDetalheContent(
        uiState          = uiState,
        onNavigateUp     = { vm.deleteIfEmpty(); onNavigateUp() },
        onUsarLista      = { onUsarLista(vm.getListId()) },
        onAdicionarItem  = { nome, tipo, preco, qtd -> vm.adicionarItem(nome, tipo, preco, qtd) },
        onRemoverItem    = { vm.removerItem(it) },
        onEditarQuantidade = { listaItem, qtd -> vm.editarQuantidadeItem(listaItem, qtd) },
        onAtualizarNome  = { vm.atualizarNomeLista(it) }
    )
}