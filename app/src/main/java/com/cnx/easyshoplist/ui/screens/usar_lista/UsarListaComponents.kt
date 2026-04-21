package com.cnx.easyshoplist.ui.screens.usar_lista

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.Setor
import java.text.NumberFormat

/** Cabeçalho de setor na tela de uso da lista. */
@Composable
fun UsarSetorHeader(setor: Setor?, itens: List<UsarItemDisplay>, currencyFormat: NumberFormat) {
    val total = itens.sumOf { it.listaItem.precoTotal }
    val nomeSetor = setor?.nome ?: stringResource(R.string.label_sector_general)
    Surface(
        color        = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        shape        = MaterialTheme.shapes.small,
        modifier     = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(nomeSetor.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(currencyFormat.format(total), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Linha de item com checkbox na tela de uso da lista. */
@Composable
fun UsarItemRow(
    display: UsarItemDisplay,
    currencyFormat: NumberFormat,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = display.marcado, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            val qtdStr = display.listaItem.quantidade.let {
                if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString()
            }
            Text(
                text = display.item.nome,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (display.marcado) TextDecoration.LineThrough else null,
                color = if (display.marcado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$qtdStr ${display.item.tipoMedida.displayName} · ${currencyFormat.format(display.listaItem.precoTotal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
