package com.mechforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import com.mechforge.app.data.ImportResult
import com.mechforge.app.ui.util.DatasetFilePickerButton

/**
 * Engineering reference library (README v2 16/17/18).
 *
 * Shows the datasets with their source and licence metadata, lets the user import their
 * own CSV data, and previews the rows. The application itself ships only self-authored
 * generic values (see BuiltInDatasets); licensed tables are imported by the user.
 */
@Composable
fun ReferencesScreen(deps: AppDependencies) {
    val datasets by deps.references.datasets().collectAsState(initial = emptyList())
    var openId by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var showImport by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Reference library", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Engineering data with its source and licence. MechForge embeds no copyrighted table: " +
                "the built-in sets are generic engineering values, and any licensed data is imported by you.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showImport = true }) { Text("Import CSV...") }
            Spacer(Modifier.width(8.dp))
            Text(
                "CSV: key,value,unit,notes   |   JSON: [{ key, value, unit, notes }]",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(12.dp))

        if (datasets.isEmpty()) {
            Text("No datasets yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        for (dataset in datasets) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dataset.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${dataset.category}  |  ${dataset.rowCount} rows",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                "Source: ${dataset.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "Licence: ${dataset.licenseType} - ${dataset.licenseNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = { openId = if (openId == dataset.id) null else dataset.id },
                        ) { Text(if (openId == dataset.id) "Hide rows" else "Show rows") }
                        TextButton(onClick = {
                            deps.references.deleteDataset(dataset.id)
                            if (openId == dataset.id) openId = null
                            message = "Dataset deleted."
                        }) { Text("Delete") }
                    }

                    if (openId == dataset.id) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        for (row in deps.references.rows(dataset.id)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    row.key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${row.value} ${row.unit}".trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (row.notes.isNotBlank()) {
                                Text(
                                    row.notes,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showImport) {
        ImportDialog(
            onDismiss = { showImport = false },
            onImported = { result ->
                message = if (result.ok) {
                    "Imported '${result.name}' with ${result.rowsImported} rows."
                } else {
                    "Import problem: ${result.errors.joinToString("; ")}"
                }
                showImport = false
            },
            csvAction = { name, category, source, licence, csv ->
                deps.references.importCsv(name, category, source, licence, "Imported by the user (CSV)", csv, System.currentTimeMillis())
            },
            jsonAction = { name, category, source, licence, json ->
                deps.references.importJson(name, category, source, licence, json, System.currentTimeMillis())
            },
        )
    }
}

@Composable
private fun ImportDialog(
    onDismiss: () -> Unit,
    onImported: (ImportResult) -> Unit,
    csvAction: (String, String, String, String, String) -> ImportResult,
    jsonAction: (String, String, String, String, String) -> ImportResult,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Project data") }
    var source by remember { mutableStateOf("") }
    var licence by remember { mutableStateOf("User-licensed") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import a CSV dataset") },
        text = {
            Column {
                Text(
                    "Columns: key,value,unit,notes (header optional).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dataset name") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source (standard, handbook, vendor)") }, singleLine = true)
                OutlinedTextField(value = licence, onValueChange = { licence = it }, label = { Text("Licence type") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                DatasetFilePickerButton("Choose CSV file...", listOf("csv", "txt")) { text ->
                    if (name.isBlank()) name = "Imported dataset"
                    onImported(csvAction(name, category, source.ifBlank { "User import" }, licence, text))
                }
                Spacer(Modifier.height(6.dp))
                DatasetFilePickerButton("Choose JSON file...", listOf("json")) { text ->
                    if (name.isBlank()) name = "Imported dataset"
                    onImported(jsonAction(name, category, source.ifBlank { "User import" }, licence, text))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
