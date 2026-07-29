package com.cnx.easyshoplist.ui.screens.lista_detalhe
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.ListaItem
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.util.filterPreco
import com.cnx.easyshoplist.data.util.filterQuantidade
import com.cnx.easyshoplist.data.util.normalizeString
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
/**
 * Template visual da tela de Detalhe de Lista.
 * Recebe apenas dados e callbacks — nao conhece o ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaDetalheContent(
    uiState: ListaDetalheUiState,
    onNavigateUp: () -> Unit,
    onUsarLista: () -> Unit,
    onAdicionarItem: (nome: String, tipo: TipoMedida, preco: Double, qtd: Float) -> Unit,
    onRemoverItem: (ListaItem) -> Unit,
    onEditarQuantidade: (ListaItem, Float) -> Unit,
    onAtualizarNome: (String) -> Unit
) {
    // --- Estados locais de UI (formulario e dialogs) ---
    var nomeItem by remember { mutableStateOf("") }
    var tipoMedidaSel by remember { mutableStateOf(TipoMedida.UNIDADE) }
    var precoItem by remember { mutableStateOf("") }
    var qtdItem by remember { mutableStateOf("1") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showSugestoes by remember { mutableStateOf(false) }
    var showEditNomeDialog by remember { mutableStateOf(false) }
    var editNomeInput by remember { mutableStateOf("") }
    var itemParaEditar by remember { mutableStateOf<ListaItemDisplay?>(null) }
    var editQtdInput by remember { mutableStateOf("") }
    val sugestoes = remember(nomeItem, uiState.todosItens) {
        if (nomeItem.isBlank()) emptyList()
        else {
            val norm = normalizeString(nomeItem)
            uiState.todosItens
                .filter { normalizeString(it.nome).contains(norm) }
                .sortedWith(compareBy({ !normalizeString(it.nome).startsWith(norm) }, { it.nome.length }))
                .take(6)
        }
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR")) }
    val currencyFmt = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
    }
    val lista = uiState.lista
    val totalItens = uiState.itensPorSetor.values.flatten().size
    val valorTotal = uiState.itensPorSetor.values.flatten().sumOf { it.listaItem.precoTotal }
    val finalizada = lista?.finalizada == true
    // --- Layout principal ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.screen_list_detail_title))
                        if (valorTotal > 0) {
                            Text(
                                text = " · ${currencyFmt.format(valorTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        bottomBar = {
            if (totalItens > 0 && !finalizada) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = onUsarLista,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_shopping_cart), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_use_list))
                    }
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Banner: lista finalizada
            if (finalizada) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(stringResource(R.string.msg_purchase_completed), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            // Cabecalho: nome e data
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        val dataStr = lista?.dataCriacao?.let { dateFormat.format(Date(it)) } ?: ""
                        Text(
                            text = lista?.nome ?: dataStr.ifBlank { stringResource(R.string.screen_new_list_title) },
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                        )
                        if (lista?.nome != null) {
                            Text(dateFormat.format(Date(lista.dataCriacao)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (!finalizada) {
                        IconButton(onClick = { editNomeInput = lista?.nome ?: ""; showEditNomeDialog = true }) {
                            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.action_edit_name))
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            // Formulario: adicionar item
            if (!finalizada) {
                item {
                    Text(stringResource(R.string.section_add_item), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = nomeItem,
                            onValueChange = { nomeItem = it; showSugestoes = it.isNotBlank() },
                            label = { Text(stringResource(R.string.label_item_name)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = showSugestoes && sugestoes.isNotEmpty(),
                            onDismissRequest = { showSugestoes = false },
                            properties = PopupProperties(focusable = false),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            sugestoes.forEach { sugestao ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(sugestao.nome, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                buildString {
                                                    append(sugestao.tipoMedida.displayName)
                                                    if (sugestao.precoBase > 0) append(" · ${currencyFmt.format(sugestao.precoBase)}")
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        nomeItem = sugestao.nome
                                        tipoMedidaSel = sugestao.tipoMedida
                                        precoItem = if (sugestao.precoBase > 0)
                                            sugestao.precoBase.toBigDecimal().stripTrailingZeros().toPlainString()
                                        else ""
                                        if (!sugestao.tipoMedida.allowsDecimal)
                                            qtdItem = qtdItem.filter { it.isDigit() }.ifBlank { "1" }
                                        showSugestoes = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qtdItem,
                            onValueChange = { qtdItem = filterQuantidade(it, tipoMedidaSel.allowsDecimal) },
                            label = { Text(stringResource(R.string.label_quantity_short)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.8f), singleLine = true
                        )
                        ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }, modifier = Modifier.weight(1.4f)) {
                            OutlinedTextField(
                                value = tipoMedidaSel.displayName, onValueChange = {}, readOnly = true,
                                label = { Text(stringResource(R.string.label_measure)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                TipoMedida.entries.forEach { tipo ->
                                    DropdownMenuItem(
                                        text = { Text(tipo.displayName) },
                                        onClick = {
                                            tipoMedidaSel = tipo; dropdownExpanded = false
                                            if (!tipo.allowsDecimal)
                                                qtdItem = qtdItem.filter { it.isDigit() }.ifBlank { "1" }
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = precoItem,
                            onValueChange = { precoItem = filterPreco(it) },
                            label = { Text(stringResource(R.string.label_price)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f), singleLine = true
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (nomeItem.isNotBlank()) {
                                val preco = precoItem.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val qtd = qtdItem.replace(",", ".").toFloatOrNull()?.coerceAtLeast(0.001f) ?: 1f
                                onAdicionarItem(nomeItem.trim(), tipoMedidaSel, preco, qtd)
                                nomeItem = ""; precoItem = ""; qtdItem = "1"
                                tipoMedidaSel = TipoMedida.UNIDADE; showSugestoes = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), enabled = nomeItem.isNotBlank()
                    ) { Text(stringResource(R.string.action_add)) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            // Lista vazia
            if (uiState.itensPorSetor.isEmpty()) {
                item {
                    Text(stringResource(R.string.msg_no_items_added), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Itens agrupados por setor
            uiState.itensPorSetor.forEach { (setor, itensDoSetor) ->
                val totalSetor = itensDoSetor.sumOf { it.listaItem.precoTotal }
                item(key = "setor_${setor?.id ?: "geral"}") {
                    SetorHeader(setor = setor, totalPreco = totalSetor, currencyFormat = currencyFmt)
                }
                items(itensDoSetor, key = { "item_${it.listaItem.id}" }) { display ->
                    ListaItemRow(
                        display = display, currencyFormat = currencyFmt,
                        onRemover = if (!finalizada) ({ onRemoverItem(display.listaItem) }) else null,
                        onEditar = if (!finalizada) ({
                            itemParaEditar = display
                            editQtdInput = display.listaItem.quantidade.let {
                                if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString()
                            }
                        }) else null
                    )
                }
            }
            // Total geral
            if (uiState.itensPorSetor.isNotEmpty()) {
                item {
                    val totalGeral = uiState.itensPorSetor.values.flatten().sumOf { it.listaItem.precoTotal }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.label_total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(currencyFmt.format(totalGeral), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
    // --- Dialog: editar nome da lista ---
    if (showEditNomeDialog) {
        AlertDialog(
            onDismissRequest = { showEditNomeDialog = false },
            title = { Text(stringResource(R.string.dialog_list_name_title)) },
            text = {
                OutlinedTextField(
                    value = editNomeInput, onValueChange = { editNomeInput = it },
                    label = { Text(stringResource(R.string.hint_list_name)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { onAtualizarNome(editNomeInput); showEditNomeDialog = false }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNomeDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    // --- Dialog: editar quantidade de item ---
    itemParaEditar?.let { display ->
        AlertDialog(
            onDismissRequest = { itemParaEditar = null },
            title = { Text(stringResource(R.string.dialog_edit_quantity_title)) },
            text = {
                OutlinedTextField(
                    value = editQtdInput,
                    onValueChange = { editQtdInput = filterQuantidade(it, display.item.tipoMedida.allowsDecimal) },
                    label = { Text(stringResource(R.string.label_quantity_unit, display.item.tipoMedida.displayName)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val qtd = editQtdInput.replace(",", ".").toFloatOrNull()?.coerceAtLeast(0.001f) ?: 1f
                    onEditarQuantidade(display.listaItem, qtd)
                    itemParaEditar = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { itemParaEditar = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}