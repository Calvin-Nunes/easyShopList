package com.cnx.easyshoplist.ui.screens.usar_lista
import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    // Mantém a tela ligada enquanto o usuário estiver nesta tela (mercado)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

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
        onConcluir      = { vm.concluirCompra(onConcluir) },
        onExportar      = { texto ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, texto)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        }
    )
}