package com.cnx.easyshoplist.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Esquema claro: Verde Teal principal · Azul escurecido secundário.
 * Fundo: Teal 50 (muito suave) · Texto padrão: quase preto.
 * Branco em todas as superfícies sobre cores vivas (primary/secondary/tertiary).
 */
private val LightColorScheme = lightColorScheme(
    primary              = TealPrimary,       // Teal 300
    onPrimary            = AppWhite,
    primaryContainer     = TealLight,         // Teal 100
    onPrimaryContainer   = TealDark,          // Teal 700
    secondary            = SetorBlue,         // Blue 300 — realces e setores
    onSecondary          = AppWhite,
    secondaryContainer   = SetorBlueContainer,// Blue 50
    onSecondaryContainer = TealDark,
    tertiary             = TealDark,
    onTertiary           = AppWhite,
    background           = AppBackground,     // Fundo claro
    onBackground         = AppOnBackground,   // Quase preto
    surface              = AppWhite,          // Surface branco puro para Cards
    onSurface            = AppOnBackground,
    surfaceVariant       = TealLight,
    onSurfaceVariant     = TealDark,
)

/**
 * Esquema escuro: (Opcional) Poderia ser diferente, mas para este app
 * manteremos tons claros mesmo que o sistema peça dark, conforme pedido do usuário.
 */
private val DarkColorScheme = darkColorScheme(
    primary              = TealPrimary,
    onPrimary            = AppWhite,
    primaryContainer     = TealLight,
    onPrimaryContainer   = TealDark,
    secondary            = SetorBlue,
    onSecondary          = AppWhite,
    secondaryContainer   = SetorBlueContainer,
    onSecondaryContainer = TealDark,
    tertiary             = TealDark,
    onTertiary           = AppWhite,
    background           = AppBackground,
    onBackground         = AppOnBackground,
    surface              = AppWhite,
    onSurface            = AppOnBackground,
    surfaceVariant       = TealLight,
    onSurfaceVariant     = TealDark,
)

@Composable
fun EasyShopListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Mantido para assinatura, mas ignorado
    content: @Composable () -> Unit
) {
    // Força sempre o esquema claro, conforme pedido do usuário
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // Força ícones da barra de status a serem escuros (pois o fundo é claro)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}