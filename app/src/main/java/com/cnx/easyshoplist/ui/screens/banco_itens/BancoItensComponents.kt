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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cnx.easyshoplist.R
import com.cnx.easyshoplist.data.db.entity.Setor
import com.cnx.easyshoplist.data.enums.TipoMedida
import com.cnx.easyshoplist.data.util.filterPreco
import java.text.NumberFormat

/** Linha de item no banco de itens. */
@Composable
fun BancoItemRow(
    display: ItemDisplay,
    priceFormat: NumberFormat,
    onEditar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(display.item.nome, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = buildString {
                    append(display.item.tipoMedida.displayName)
                    if (display.item.precoBase > 0) { append(" · "); append(priceFormat.format(display.item.precoBase)) }
                    display.setor?.let { append(" · ${it.nome}") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEditar) {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.action_edit))
        }
    }
    HorizontalDivider()
}

/** Dialog para criar um novo item no banco. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovoItemDialog(
    setores: List<Setor>,
    onDismiss: () -> Unit,
    onCreate: (nome: String, tipo: TipoMedida, preco: Double, idSetor: Long?) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoMedida.UNIDADE) }
    var preco by remember { mutableStateOf("") }
    var setorSel by remember { mutableStateOf<Setor?>(null) }
    var tipoDropdown by remember { mutableStateOf(false) }
    var setorDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_new_item_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text(stringResource(R.string.label_name_required)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    isError = nome.isBlank(), modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = tipoDropdown, onExpandedChange = { tipoDropdown = it }) {
                    OutlinedTextField(
                        value = tipo.displayName, onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(R.string.label_measure)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = tipoDropdown, onDismissRequest = { tipoDropdown = false }) {
                        TipoMedida.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.displayName) }, onClick = { tipo = t; tipoDropdown = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = preco, onValueChange = { preco = filterPreco(it) },
                    label = { Text(stringResource(R.string.label_base_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = setorDropdown, onExpandedChange = { setorDropdown = it }) {
                    OutlinedTextField(
                        value = setorSel?.nome ?: stringResource(R.string.label_none), onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(R.string.label_sector)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = setorDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = setorDropdown, onDismissRequest = { setorDropdown = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.label_none)) }, onClick = { setorSel = null; setorDropdown = false })
                        setores.forEach { s ->
                            DropdownMenuItem(text = { Text(s.nome) }, onClick = { setorSel = s; setorDropdown = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(nome, tipo, preco.replace(",", ".").toDoubleOrNull() ?: 0.0, setorSel?.id) },
                enabled = nome.isNotBlank()
            ) { Text(stringResource(R.string.action_create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/**
 * Dialog para gerenciar setores: criação com auto-ordem, edição de nome,
 * reordenação com setas ↑↓ e exclusão. Lista rolável.
 */
@Composable
fun SetorManagerDialog(
    setores: List<Setor>,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
    onUpdate: (Setor) -> Unit,
    onDelete: (Setor) -> Unit,
    onMoveUp: (Setor) -> Unit,
    onMoveDown: (Setor) -> Unit
) {
    var novoNome by remember { mutableStateOf("") }
    var setorEditando by remember { mutableStateOf<Setor?>(null) }
    var editNome by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_manage_sectors_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Criar novo setor
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = novoNome, onValueChange = { novoNome = it },
                        label = { Text(stringResource(R.string.label_new_sector)) }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { if (novoNome.isNotBlank()) { onCreate(novoNome); novoNome = "" } },
                        enabled = novoNome.isNotBlank()
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.action_add_sector))
                    }
                }
                HorizontalDivider()
                // Lista de setores (rolável)
                if (setores.isEmpty()) {
                    Text(stringResource(R.string.msg_no_sectors), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(setores, key = { it.id }) { setor ->
                            val isFirst = setores.first().id == setor.id
                            val isLast = setores.last().id == setor.id

                            if (setorEditando?.id == setor.id) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editNome, onValueChange = { editNome = it },
                                        singleLine = true, modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        if (editNome.isNotBlank()) { onUpdate(setor.copy(nome = editNome.trim())); setorEditando = null }
                                    }) {
                                        Icon(painterResource(R.drawable.ic_check), contentDescription = stringResource(R.string.action_save))
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(setor.nome, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    // Seta cima
                                    IconButton(onClick = { onMoveUp(setor) }, enabled = !isFirst, modifier = Modifier.size(32.dp)) {
                                        Icon(painterResource(R.drawable.ic_arrow_up), contentDescription = stringResource(R.string.action_move_up), modifier = Modifier.size(16.dp))
                                    }
                                    // Seta baixo
                                    IconButton(onClick = { onMoveDown(setor) }, enabled = !isLast, modifier = Modifier.size(32.dp)) {
                                        Icon(painterResource(R.drawable.ic_arrow_down), contentDescription = stringResource(R.string.action_move_down), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { setorEditando = setor; editNome = setor.nome }, modifier = Modifier.size(32.dp)) {
                                        Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.action_edit), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { onDelete(setor) }, modifier = Modifier.size(32.dp)) {
                                        Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}
