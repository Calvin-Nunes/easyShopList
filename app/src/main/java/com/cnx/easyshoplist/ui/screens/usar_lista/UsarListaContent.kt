package com.cnx.easyshoplist.ui.screens.usar_lista
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cnx.easyshoplist.R
import java.text.NumberFormat
import java.util.*
/**
 * Template visual da tela de Usar Lista.
 * Recebe apenas dados e callbacks — nao conhece o ViewModel.
 */
@Composable
fun UsarListaContent(
    uiState: UsarListaUiState,
    onToggleMarcado: (Long) -> Unit,
    onConcluir: () -> Unit,
    onExportar: (String) -> Unit
) {
    val currencyFmt = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    // Total acumulado dos itens marcados
    val totalMarcado = remember(uiState.itensPorSetor) {
        uiState.itensPorSetor.values.flatten()
            .filter { it.marcado }
            .sumOf { it.listaItem.precoTotal }
    }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Linha do total marcado — sempre visível
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_total_marked),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currencyFmt.format(totalMarcado),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    val labelSetorGeral = stringResource(R.string.label_sector_general)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val texto = buildString {
                                    uiState.itensPorSetor.forEach { (setor, itensDoSetor) ->
                                        val nomeSetor = setor?.nome ?: labelSetorGeral
                                        append("*$nomeSetor*\n")
                                        itensDoSetor.forEach { display ->
                                            val qtdStr = display.listaItem.quantidade.let {
                                                if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString()
                                            }
                                            val medidaStr = when (display.item.tipoMedida) {
                                                com.cnx.easyshoplist.data.enums.TipoMedida.KG -> "kg"
                                                com.cnx.easyshoplist.data.enums.TipoMedida.GRAMA -> "Gr."
                                                com.cnx.easyshoplist.data.enums.TipoMedida.LITRO -> "Litro"
                                                com.cnx.easyshoplist.data.enums.TipoMedida.DUZIA -> "Dúzia"
                                                else -> ""
                                            }
                                            append("* $qtdStr$medidaStr ${display.item.nome} - ${currencyFmt.format(display.listaItem.precoTotal)}\n")
                                        }
                                        append("\n")
                                    }
                                }.trim()
                                onExportar(texto)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_export))
                        }
                        Button(
                            onClick = onConcluir,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.action_conclude_purchase))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val valorTotal = uiState.itensPorSetor.values.flatten().sumOf { it.listaItem.precoTotal }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                val nomeLista = uiState.lista?.nome ?: stringResource(R.string.screen_list_detail_title)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nomeLista,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (valorTotal > 0) {
                        Text(
                            text = currencyFmt.format(valorTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            uiState.itensPorSetor.forEach { (setor, itens) ->
                item(key = "setor_usar_${setor?.id ?: "geral"}") {
                    UsarSetorHeader(setor = setor, itens = itens, currencyFormat = currencyFmt)
                }
                items(itens, key = { "usar_item_${it.listaItem.id}" }) { display ->
                    UsarItemRow(
                        display = display,
                        currencyFormat = currencyFmt,
                        onToggle = { onToggleMarcado(display.listaItem.id) }
                    )
                }
            }
        }
    }
}