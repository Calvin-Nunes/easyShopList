package com.cnx.easyshoplist.ui.screens.lista_detalhe

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.Setor
import java.text.NumberFormat

/** Cabeçalho de setor com fundo azul vivo e texto em maiúsculas. */
@Composable
fun SetorHeader(setor: Setor?, totalPreco: Double, currencyFormat: NumberFormat) {
    val nomeSetor = setor?.nome ?: stringResource(R.string.label_sector_general)
    Surface(
        // Usa a cor secondary do tema (SetorBlue) — muda automaticamente com o tema
        color        = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        shape        = MaterialTheme.shapes.small,
        modifier     = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = nomeSetor.uppercase(),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
                // color herdada de contentColor (onSecondary = AppWhite)
            )
            Text(
                text  = currencyFormat.format(totalPreco),
                style = MaterialTheme.typography.labelLarge
                // color herdada de contentColor
            )
        }
    }
}

/** Linha de item dentro da lista com botões de editar e remover. */
@Composable
fun ListaItemRow(
    display: ListaItemDisplay,
    currencyFormat: NumberFormat,
    onRemover: (() -> Unit)?,
    onEditar: (() -> Unit)?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(display.item.nome, style = MaterialTheme.typography.bodyLarge)
                val qtdStr = display.listaItem.quantidade.let {
                    if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString()
                }
                Text(
                    text = "$qtdStr ${display.item.tipoMedida.displayName} · ${currencyFormat.format(display.listaItem.precoTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onEditar != null) {
                IconButton(onClick = onEditar) {
                    Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.action_edit))
                }
            }
            if (onRemover != null) {
                IconButton(onClick = onRemover) {
                    Icon(
                        painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.action_remove),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
