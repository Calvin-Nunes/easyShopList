package com.cnx.easyshoplist.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// PALETA PRINCIPAL — Verde Teal
// =============================================================================
val TealPrimary     = Color(0xFF00796B)   // Teal 700  — cor principal (botões, nav ativa, AppBar)
val TealDark        = Color(0xFF004D40)   // Teal 900  — contraste escuro / container texto
val TealLight       = Color(0xFFB2DFDB)   // Teal 100  — container claro
val TealPrimaryDark = Color(0xFF26A69A)   // Teal 400  — primary no modo escuro

// =============================================================================
// REALCE — Azul levemente escurecido (setores, destaques)
// =============================================================================
val SetorBlue          = Color(0xFF1565C0)   // Blue 800  — fundo dos cabeçalhos de setor
val SetorBlueContainer = Color(0xFFBBDEFB)   // Blue 100  — container azul claro

// =============================================================================
// FUNDO DO APP
// =============================================================================
/** Teal 50 — fundo muito suave (equivale a Teal 700 com ~10 % de opacidade sobre branco). */
val AppBackground   = Color(0xFFE0F2F1)
/** Quase preto — fonte padrão sobre fundos claros (background / surface). */
val AppOnBackground = Color(0xFF1C1B1F)

// =============================================================================
// UTILITÁRIOS — nunca usar Color.White / Color.Transparent direto nos composables
// =============================================================================
/** Branco puro — texto em fundos primários/secundários/terciários coloridos. */
val AppWhite        = Color(0xFFFFFFFF)
/** Transparente — para backgrounds de swipe-to-dismiss antes do gesto iniciar. */
val AppTransparent  = Color(0x00000000)
