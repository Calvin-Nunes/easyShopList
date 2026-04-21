package com.cnx.easyshoplist.ui.screens.listas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.Lista
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/** Card de uma lista na tela de listagem. */
@Composable
fun ListaCardItem(listaResumo: ListaComResumo, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }
    val currencyFmt = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
    }
    val lista = listaResumo.lista
    val dataFormatada = remember(lista.dataCriacao) { dateFormat.format(Date(lista.dataCriacao)) }
    val nomeDisplay = lista.nome ?: dataFormatada

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(nomeDisplay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (lista.nome != null) {
                    Text(dataFormatada, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (listaResumo.totalItens > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(pluralStringResource(R.plurals.items_count, listaResumo.totalItens, listaResumo.totalItens))
                            append(" · ")
                            append(currencyFmt.format(listaResumo.totalPreco))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (lista.finalizada) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(R.string.label_finalized),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
