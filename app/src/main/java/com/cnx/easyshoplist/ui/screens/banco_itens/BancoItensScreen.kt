package com.cnx.easyshoplist.ui.screens.banco_itens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
/**
 * Ponto de entrada da tela de Banco de Itens.
 * Responsabilidade: instanciar o ViewModel, coletar o estado e repassar ao [BancoItensContent].
 * Não contém código visual — apenas ligação entre ViewModel e template.
 */
@Composable
fun BancoItensScreen(
    itemRepository: ItemRepository,
    setorRepository: SetorRepository,
    listaItemRepository: ListaItemRepository
) {
    val vm: BancoItensViewModel = viewModel(
        factory = BancoItensViewModel.Factory(itemRepository, setorRepository, listaItemRepository)
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    BancoItensContent(
        uiState         = uiState,
        onCreateItem    = { nome, tipo, preco, idSetor -> vm.createItem(nome, tipo, preco, idSetor) },
        onUpdateItem    = { vm.updateItem(it) },
        onDeleteItem    = { vm.deleteItem(it) },
        onCreateSetor   = { vm.createSetor(it) },
        onUpdateSetor   = { vm.updateSetor(it) },
        onDeleteSetor   = { vm.deleteSetor(it) },
        onMoveSetorUp   = { vm.moveSetorUp(it) },
        onMoveSetorDown = { vm.moveSetorDown(it) }
    )
}