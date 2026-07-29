package com.cnx.easyshoplist.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// PALETA PRINCIPAL — Verde Teal (Versão mais clara)
// =============================================================================
val TealPrimary     = Color(0xFF4DB6AC)   // Teal 300  — cor principal (mais clara)
val TealDark        = Color(0xFF00796B)   // Teal 700  — contraste
val TealLight       = Color(0xFFB2DFDB)   // Teal 100  — container claro
val TealPrimaryDark = Color(0xFF80CBC4)   // Teal 200  — primary no modo escuro

// =============================================================================
// REALCE — Azul (Versão mais clara)
// =============================================================================
val SetorBlue          = Color(0xFF64B5F6)   // Blue 300  — fundo dos cabeçalhos de setor
val SetorBlueContainer = Color(0xFFE3F2FD)   // Blue 50   — container azul muito claro

// =============================================================================
// FUNDO DO APP
// =============================================================================
/** Branco gelo / cinza muito claro para um visual limpo. */
val AppBackground   = Color(0xFFF5F7F7)
/** Quase preto — fonte padrão sobre fundos claros (background / surface). */
val AppOnBackground = Color(0xFF1C1B1F)

// =============================================================================
// UTILITÁRIOS — nunca usar Color.White / Color.Transparent direto nos composables
// =============================================================================
/** Branco puro — texto em fundos primários/secundários/terciários coloridos. */
val AppWhite        = Color(0xFFFFFFFF)
/** Transparente — para backgrounds de swipe-to-dismiss antes do gesto iniciar. */
val AppTransparent  = Color(0x00000000)
