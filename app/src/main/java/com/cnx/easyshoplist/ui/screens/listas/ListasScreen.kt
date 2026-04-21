package com.cnx.easyshoplist.ui.screens.listas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cnx.easyshoplist.data.db.entity.Lista
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
/**
 * Ponto de entrada da tela de Listas.
 * Responsabilidade: instanciar o ViewModel, coletar o estado e repassar ao [ListasContent].
 * Nao contém código visual — apenas ligacao entre ViewModel e template.
 */
@Composable
fun ListasScreen(
    listaRepository: ListaRepository,
    listaItemRepository: ListaItemRepository,
    onListaClick: (Lista) -> Unit
) {
    val vm: ListasViewModel = viewModel(
        factory = ListasViewModel.Factory(listaRepository, listaItemRepository)
    )
    val listas by vm.listas.collectAsStateWithLifecycle()
    ListasContent(
        listas       = listas,
        onListaClick = onListaClick,
        onDeletar    = { vm.deletarLista(it) }
    )
}