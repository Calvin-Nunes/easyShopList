package com.cnx.easyshoplist.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cnx.easyshoplist.data.db.EasyShopDatabase
import com.cnx.easyshoplist.data.repository.ItemRepository
import com.cnx.easyshoplist.data.repository.ListaItemRepository
import com.cnx.easyshoplist.data.repository.ListaRepository
import com.cnx.easyshoplist.data.repository.SetorRepository
import com.cnx.easyshoplist.ui.screens.banco_itens.BancoItensScreen
import com.cnx.easyshoplist.ui.screens.lista_detalhe.ListaDetalheScreen
import com.cnx.easyshoplist.ui.screens.listas.ListasScreen
import com.cnx.easyshoplist.ui.screens.usar_lista.UsarListaScreen

object AppRoutes {
    const val HOME = "home"
    const val CREATE_LIST = "create_list"
    const val EDIT_LIST = "edit_list/{listId}"
    const val USE_LIST = "use_list/{listId}"
    const val ITEM_BANK = "item_bank"

    fun editList(listId: Long) = "edit_list/$listId"
    fun useList(listId: Long) = "use_list/$listId"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    db: EasyShopDatabase
) {
    val listaRepo = ListaRepository(db.listaDao())
    val listaItemRepo = ListaItemRepository(db.listaItemDao())
    val itemRepo = ItemRepository(db.itemDao())
    val setorRepo = SetorRepository(db.setorDao())

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            ListasScreen(
                listaRepository = listaRepo,
                listaItemRepository = listaItemRepo,
                onListaClick = { lista ->
                    navController.navigate(AppRoutes.editList(lista.id))
                }
            )
        }

        composable(AppRoutes.CREATE_LIST) {
            ListaDetalheScreen(
                listId = -1L,
                listaRepository = listaRepo,
                listaItemRepository = listaItemRepo,
                itemRepository = itemRepo,
                setorRepository = setorRepo,
                onNavigateUp = { navController.popBackStack() },
                onUsarLista = { listId ->
                    navController.navigate(AppRoutes.useList(listId))
                }
            )
        }

        composable(
            route = AppRoutes.EDIT_LIST,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: -1L
            ListaDetalheScreen(
                listId = listId,
                listaRepository = listaRepo,
                listaItemRepository = listaItemRepo,
                itemRepository = itemRepo,
                setorRepository = setorRepo,
                onNavigateUp = { navController.popBackStack() },
                onUsarLista = { id ->
                    navController.navigate(AppRoutes.useList(id))
                }
            )
        }

        composable(
            route = AppRoutes.USE_LIST,
            arguments = listOf(navArgument("listId") { type = NavType.LongType })
        ) { backStackEntry ->
            val listId = backStackEntry.arguments?.getLong("listId") ?: -1L
            UsarListaScreen(
                listId = listId,
                listaRepository = listaRepo,
                listaItemRepository = listaItemRepo,
                itemRepository = itemRepo,
                setorRepository = setorRepo,
                onConcluir = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.ITEM_BANK) {
            BancoItensScreen(
                itemRepository = itemRepo,
                setorRepository = setorRepo,
                listaItemRepository = listaItemRepo
            )
        }
    }
}

