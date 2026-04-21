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
    primary              = TealPrimary,       // Teal 700
    onPrimary            = AppWhite,
    primaryContainer     = TealLight,         // Teal 100
    onPrimaryContainer   = TealDark,          // Teal 900
    secondary            = SetorBlue,         // Blue 800 — realces e setores
    onSecondary          = AppWhite,
    secondaryContainer   = SetorBlueContainer,// Blue 100
    onSecondaryContainer = TealDark,
    tertiary             = TealDark,
    onTertiary           = AppWhite,
    background           = AppBackground,     // Teal 50 — fundo suave do app
    onBackground         = AppOnBackground,   // Quase preto — fonte padrão
    surface              = AppBackground,     // Mesma cor para Surface, Card, etc.
    onSurface            = AppOnBackground,
    surfaceVariant       = TealLight,
    onSurfaceVariant     = TealDark,
)

/**
 * Esquema escuro: variante Teal mais clara para modo noturno.
 */
private val DarkColorScheme = darkColorScheme(
    primary              = TealPrimaryDark,   // Teal 400
    onPrimary            = TealDark,          // Teal 900
    primaryContainer     = TealDark,
    onPrimaryContainer   = TealLight,
    secondary            = SetorBlueContainer,// Blue 100
    onSecondary          = TealDark,
)

@Composable
fun EasyShopListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}