package com.cnx.easyshoplist.ui.screens.banco_itens
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.Item
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.util.filterPreco
import java.text.NumberFormat
import java.util.*
/**
 * Template visual da tela de Banco de Itens.
 * Recebe apenas dados e callbacks — não conhece o ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BancoItensContent(
    uiState: BancoItensUiState,
    onCreateItem: (nome: String, tipo: TipoMedida, preco: Double, idSetor: Long?) -> Unit,
    onUpdateItem: (Item) -> Unit,
    onDeleteItem: (Item) -> Unit,
    onCreateSetor: (String) -> Unit,
    onUpdateSetor: (Setor) -> Unit,
    onDeleteSetor: (Setor) -> Unit,
    onMoveSetorUp: (Setor) -> Unit,
    onMoveSetorDown: (Setor) -> Unit
) {
    val priceFmt = remember {
        NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR")).apply {
            minimumFractionDigits = 2; maximumFractionDigits = 2
        }
    }
    var itemEditando by remember { mutableStateOf<ItemDisplay?>(null) }
    var showSetorManager by remember { mutableStateOf(false) }
    var showNovoItemDialog by remember { mutableStateOf(false) }
    var itemParaExcluir by remember { mutableStateOf<ItemDisplay?>(null) }
    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = { showSetorManager = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        painterResource(R.drawable.ic_category),
                        contentDescription = stringResource(R.string.action_manage_sectors)
                    )
                }
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.action_new_item)) },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    onClick = { showNovoItemDialog = true }
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.items.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.msg_no_items_in_bank),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
                ) {
                    items(uiState.items, key = { it.item.id }) { display ->
                        BancoItemRow(
                            display = display,
                            priceFormat = priceFmt,
                            onEditar = { itemEditando = display }
                        )
                    }
                }
            }
        }
    }
    // --- Dialog: criar novo item ---
    if (showNovoItemDialog) {
        NovoItemDialog(
            setores = uiState.setores,
            onDismiss = { showNovoItemDialog = false },
            onCreate = { nome, tipo, preco, idSetor ->
                onCreateItem(nome, tipo, preco, idSetor)
                showNovoItemDialog = false
            }
        )
    }
    // --- Dialog: editar item ---
    itemEditando?.let { editDisplay ->
        var nomeEdit by remember(editDisplay.item.id) { mutableStateOf(editDisplay.item.nome) }
        var tipoEdit by remember(editDisplay.item.id) { mutableStateOf(editDisplay.item.tipoMedida) }
        var precoEdit by remember(editDisplay.item.id) {
            mutableStateOf(
                if (editDisplay.item.precoBase > 0)
                    editDisplay.item.precoBase.toBigDecimal().stripTrailingZeros().toPlainString()
                else ""
            )
        }
        var setorEdit by remember(editDisplay.item.id) { mutableStateOf(editDisplay.setor) }
        var tipoDropdown by remember { mutableStateOf(false) }
        var setorDropdown by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { itemEditando = null },
            title = { Text(stringResource(R.string.dialog_edit_item_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nomeEdit, onValueChange = { nomeEdit = it },
                        label = { Text(stringResource(R.string.label_name)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(expanded = tipoDropdown, onExpandedChange = { tipoDropdown = it }) {
                        OutlinedTextField(
                            value = tipoEdit.displayName, onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.label_measure)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = tipoDropdown, onDismissRequest = { tipoDropdown = false }) {
                            TipoMedida.entries.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text(tipo.displayName) },
                                    onClick = { tipoEdit = tipo; tipoDropdown = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = precoEdit, onValueChange = { precoEdit = filterPreco(it) },
                        label = { Text(stringResource(R.string.label_base_price)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(expanded = setorDropdown, onExpandedChange = { setorDropdown = it }) {
                        OutlinedTextField(
                            value = setorEdit?.nome ?: stringResource(R.string.label_none),
                            onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.label_sector)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = setorDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = setorDropdown, onDismissRequest = { setorDropdown = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.label_none)) },
                                onClick = { setorEdit = null; setorDropdown = false }
                            )
                            uiState.setores.forEach { setor ->
                                DropdownMenuItem(
                                    text = { Text(setor.nome) },
                                    onClick = { setorEdit = setor; setorDropdown = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val preco = precoEdit.replace(",", ".").toDoubleOrNull() ?: editDisplay.item.precoBase
                    onUpdateItem(
                        editDisplay.item.copy(
                            nome = nomeEdit.trim(),
                            tipoMedida = tipoEdit,
                            precoBase = preco,
                            idSetor = setorEdit?.id
                        )
                    )
                    itemEditando = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { itemParaExcluir = editDisplay; itemEditando = null }) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { itemEditando = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        )
    }
    // --- Dialog: gerenciar setores ---
    if (showSetorManager) {
        SetorManagerDialog(
            setores = uiState.setores,
            onDismiss = { showSetorManager = false },
            onCreate = onCreateSetor,
            onUpdate = onUpdateSetor,
            onDelete = onDeleteSetor,
            onMoveUp = onMoveSetorUp,
            onMoveDown = onMoveSetorDown
        )
    }
    // --- Dialog: confirmar exclusao de item ---
    itemParaExcluir?.let { display ->
        AlertDialog(
            onDismissRequest = { itemParaExcluir = null },
            title = { Text(stringResource(R.string.dialog_delete_item_title)) },
            text = { Text(stringResource(R.string.msg_delete_item_confirm, display.item.nome)) },
            confirmButton = {
                TextButton(onClick = { onDeleteItem(display.item); itemParaExcluir = null }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemParaExcluir = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}