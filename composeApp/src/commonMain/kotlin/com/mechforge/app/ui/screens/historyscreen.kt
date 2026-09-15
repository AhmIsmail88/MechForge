package com.mechforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies
import com.mechforge.app.data.Snapshots
import com.mechforge.app.ui.Screen
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.db.Calculation_history
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.mechforge.app.ui.i18n.LocalStrings

private val historyDateFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun HistoryScreen(deps: AppDependencies, onNavigate: (Screen) -> Unit) {
    val strings = LocalStrings.current
    val history by deps.history.all().collectAsState(initial = emptyList<Calculation_history>())
    var renameTarget by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(strings.historyTitle, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (history.isEmpty()) {
            Text(strings.historyEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(strings.historyEmptyHint, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            items(history, key = { it.id }) { row ->
                val calc = CalculatorRegistry.byId(row.calculator_id)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                calc?.let {
                                    val inputs = runCatching { Snapshots.decodeInputs(row.inputs_json) }.getOrNull()
                                    onNavigate(
                                        Screen.Calculator(
                                            calculatorId = row.calculator_id,
                                            restoreInputs = inputs,
                                            restoreTitle = row.title,
                                        )
                                    )
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${calc?.def?.name ?: row.calculator_id}  •  ${historyDateFormatter.format(Instant.ofEpochMilli(row.timestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = {
                            renameTarget = row.id
                            renameText = row.title
                        }) { Icon(Icons.Filled.Edit, "Rename") }
                        IconButton(onClick = { deps.history.duplicate(row.id, System.currentTimeMillis()) }) {
                            Text(strings.historyCopyName, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { deps.history.delete(row.id) }) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(strings.rename) },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    deps.history.rename(id, renameText.ifBlank { "Untitled" })
                    renameTarget = null
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text(strings.cancel) } },
        )
    }
}
