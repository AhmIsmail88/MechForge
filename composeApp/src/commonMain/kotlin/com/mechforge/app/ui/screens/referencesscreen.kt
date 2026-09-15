package com.mechforge.app.ui.screens

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
import com.mechforge.app.ui.i18n.LocalStrings
import com.mechforge.app.ui.theme.glassBorder
import com.mechforge.app.ui.util.DatasetFilePickerButton

/**
 * Engineering reference library (README v2 16/17/18).
 *
 * Shows the datasets with their source and licence metadata, lets the user import their
 * own CSV or JSON data, and previews the rows. The application itself ships only
 * self-authored generic values (see BuiltInDatasets); licensed tables are imported by
 * the user. Every string comes from UiStrings, so the screen follows the app language.
 */
@Composable
fun ReferencesScreen(deps: AppDependencies) {
    val strings = LocalStrings.current
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
        Text(strings.referencesTitle, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            strings.referencesIntro,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showImport = true }) { Text(strings.referencesImportCsv) }
            Spacer(Modifier.width(8.dp))
            Text(
                strings.referencesHint,
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
            Text(strings.referencesEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(strings.referencesEmptyHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        for (dataset in datasets) {
            Card(modifier = Modifier.fillMaxWidth().glassBorder(MaterialTheme.shapes.large).padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dataset.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                dataset.category + "  |  " + strings.referencesRows(dataset.rowCount.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                strings.referencesSourceLine + " " + dataset.source,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                strings.referencesLicenceLine + " " + dataset.licenseType + " - " + dataset.licenseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = { openId = if (openId == dataset.id) null else dataset.id },
                        ) {
                            Text(if (openId == dataset.id) strings.referencesHideRows else strings.referencesShowRows)
                        }
                        TextButton(onClick = {
                            deps.references.deleteDataset(dataset.id)
                            if (openId == dataset.id) openId = null
                            message = strings.referencesDeleted
                        }) { Text(strings.delete) }
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
                    strings.referencesImported(result.name, result.rowsImported)
                } else {
                    strings.referencesImportProblem + " " + result.errors.joinToString("; ")
                }
                showImport = false
            },
            csvAction = { name, category, source, licence, csv ->
                deps.references.importCsv(
                    name, category, source, licence, "Imported by the user (CSV)", csv, System.currentTimeMillis(),
                )
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
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Project data") }
    var source by remember { mutableStateOf("") }
    var licence by remember { mutableStateOf("User-licensed") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.referencesImportTitle) },
        text = {
            Column {
                Text(
                    strings.referencesColumnsHint,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(strings.referencesName) }, singleLine = true,
                )
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text(strings.referencesCategory) }, singleLine = true,
                )
                OutlinedTextField(
                    value = source, onValueChange = { source = it },
                    label = { Text(strings.referencesSource) }, singleLine = true,
                )
                OutlinedTextField(
                    value = licence, onValueChange = { licence = it },
                    label = { Text(strings.referencesLicence) }, singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                DatasetFilePickerButton(strings.referencesChooseCsv, listOf("csv", "txt")) { text ->
                    if (name.isBlank()) name = strings.referencesDefaultName
                    onImported(csvAction(name, category, source.ifBlank { "User import" }, licence, text))
                }
                Spacer(Modifier.height(6.dp))
                DatasetFilePickerButton(strings.referencesChooseJson, listOf("json")) { text ->
                    if (name.isBlank()) name = strings.referencesDefaultName
                    onImported(jsonAction(name, category, source.ifBlank { "User import" }, licence, text))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
    )
}
