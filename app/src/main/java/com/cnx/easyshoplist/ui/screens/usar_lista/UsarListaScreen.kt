package com.cnx.easyshoplist.ui.screens.usar_lista
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
/**
 * Ponto de entrada da tela de Usar Lista.
 * Responsabilidade: instanciar o ViewModel, coletar o estado e repassar ao [UsarListaContent].
 * Nao contém código visual — apenas ligacao entre ViewModel e template.
 */
@Composable
fun UsarListaScreen(
    listId: Long,
    listaRepository: ListaRepository,
    listaItemRepository: ListaItemRepository,
    itemRepository: ItemRepository,
    setorRepository: SetorRepository,
    onConcluir: () -> Unit
) {
    val vm: UsarListaViewModel = viewModel(
        key = "usar_lista_$listId",
        factory = UsarListaViewModel.Factory(
            listId, listaRepository, listaItemRepository, itemRepository, setorRepository
        )
    )
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    UsarListaContent(
        uiState         = uiState,
        onToggleMarcado = { vm.toggleMarcado(it) },
        onConcluir      = { vm.concluirCompra(onConcluir) }
    )
}