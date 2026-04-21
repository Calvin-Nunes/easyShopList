package com.cnx.easyshoplist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cnx.easyshoplist.data.db.EasyShopDatabase
import com.cnx.easyshoplist.ui.navigation.AppNavigation
import com.cnx.easyshoplist.ui.navigation.AppRoutes
import com.cnx.easyshoplist.ui.theme.EasyShopListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = EasyShopDatabase.getDatabase(this)
        setContent {
            EasyShopListTheme {
                EasyShopListApp(db = db)
            }
        }
    }
}

enum class AppNavDestination(
    val labelRes: Int,
    val iconRes: Int,
    val route: String
) {
    HOME(R.string.nav_lists, R.drawable.ic_home, AppRoutes.HOME),
    CREATE(R.string.nav_new_list, R.drawable.ic_add, AppRoutes.CREATE_LIST),
    ITEM_BANK(R.string.nav_products, R.drawable.ic_inventory, AppRoutes.ITEM_BANK),
}

@Composable
fun EasyShopListApp(db: EasyShopDatabase) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppNavDestination.entries.forEach { destination ->
                val selected = when (destination) {
                    AppNavDestination.HOME -> currentRoute == AppRoutes.HOME ||
                        currentRoute?.startsWith("edit_list") == true ||
                        currentRoute?.startsWith("use_list") == true
                    else -> currentRoute == destination.route
                }
                item(
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = stringResource(destination.labelRes)
                        )
                    },
                    label = { Text(stringResource(destination.labelRes)) },
                    selected = selected,
                    onClick = {
                        if (destination == AppNavDestination.HOME) {
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.HOME) { inclusive = true; saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } else if (destination == AppNavDestination.CREATE) {
                            // Sempre cria uma nova lista do zero — nunca restaura sessão anterior
                            navController.navigate(AppRoutes.CREATE_LIST) {
                                popUpTo(AppRoutes.CREATE_LIST) { inclusive = true }
                                launchSingleTop = false
                                restoreState = false
                            }
                        } else {
                            navController.navigate(destination.route) {
                                popUpTo(AppRoutes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) {
        AppNavigation(navController = navController, db = db)
    }
}