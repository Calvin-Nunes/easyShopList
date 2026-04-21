package com.cnx.easyshoplist.data.util

import java.text.Normalizer
import java.util.Locale

/**
 * Remove acentos e converte para lowercase para comparação normalizada de nomes.
 * Exemplo: "Açúcar" → "acucar"
 */
fun normalizeString(input: String): String {
    val normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
    return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
        .lowercase()
        .trim()
}

/**
 * Força a primeira letra em maiúscula e o restante em minúscula (Capital Case).
 * Exemplo: "alho" -> "Alho", "ALHO" -> "Alho", "aLHo" -> "Alho"
 */
fun capitalizeName(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return trimmed
    return trimmed.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}
