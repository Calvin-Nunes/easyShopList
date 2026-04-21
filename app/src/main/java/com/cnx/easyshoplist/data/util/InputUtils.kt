package com.cnx.easyshoplist.data.util

/**
 * Filtra o input de preço: aceita apenas dígitos e um separador decimal (vírgula ou ponto)
 * com no máximo 2 casas decimais.
 */
fun filterPreco(input: String): String {
    val cleaned = input.filter { it.isDigit() || it == ',' || it == '.' }
    val sepIdx = cleaned.indexOfFirst { it == ',' || it == '.' }
    if (sepIdx == -1) return cleaned
    val intPart = cleaned.substring(0, sepIdx)
    val sep = cleaned[sepIdx]
    val decPart = cleaned.substring(sepIdx + 1).filter { it.isDigit() }.take(2)
    return "$intPart$sep$decPart"
}

/**
 * Filtra o input de quantidade:
 * - allowsDecimal = false → apenas inteiros
 * - allowsDecimal = true  → máximo de 1 casa decimal
 */
fun filterQuantidade(input: String, allowsDecimal: Boolean): String {
    val cleaned = input.filter { it.isDigit() || it == ',' || it == '.' }
    if (!allowsDecimal) return cleaned.filter { it.isDigit() }
    val sepIdx = cleaned.indexOfFirst { it == ',' || it == '.' }
    if (sepIdx == -1) return cleaned
    val intPart = cleaned.substring(0, sepIdx)
    val sep = cleaned[sepIdx]
    val decPart = cleaned.substring(sepIdx + 1).filter { it.isDigit() }.take(1)
    return "$intPart$sep$decPart"
}

