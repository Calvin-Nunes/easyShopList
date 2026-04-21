package com.cnx.easyshoplist.data.enums

enum class TipoMedida(val displayName: String, val allowsDecimal: Boolean) {
    KG("Kg", true),
    GRAMA("Grama", true),
    PACOTE("Pacote", false),
    CAIXA("Caixa", false),
    LITRO("Litro", true),
    GARRAFA("Garrafa", false),
    UNIDADE("Unidade", false),
    DUZIA("Dúzia", true),
    LATA("Lata", false),
    SACO("Saco", false)
}
