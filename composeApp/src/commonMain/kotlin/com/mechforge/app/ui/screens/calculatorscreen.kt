package com.mechforge.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies
import com.mechforge.app.ui.util.UiFormat
import com.mechforge.app.export.ReportLabels
import com.mechforge.app.export.ReportSheet
import com.mechforge.app.export.ReportWriter
import com.mechforge.app.data.LanguageMode
import com.mechforge.app.data.SettingsRepository
import com.mechforge.app.data.Snapshots
import com.mechforge.core.engine.CalcOutput
import com.mechforge.core.engine.InputValue
import com.mechforge.core.engine.ValidationException
import com.mechforge.core.units.Units
import com.mechforge.core.util.Fmt
import kotlinx.coroutines.launch

private data class InputUi(
    val specId: String,
    val text: String,
    val unitId: String,
)

@Composable
fun CalculatorScreen(
    deps: AppDependencies,
    calculatorId: String,
    restoreInputs: Map<String, InputValue>? = null,
    restoreTitle: String? = null,
    onBack: () -> Unit,
) {
    val calc = remember(calculatorId) {
        com.mechforge.core.engine.CalculatorRegistry.byIdOrThrow(calculatorId)
    }
    val def = calc.def

    var inputsUi by remember(calculatorId) {
        mutableStateOf(
            def.inputs.map { spec ->
                val restored = restoreInputs?.get(spec.id)
                if (restored != null) {
                    val unit = Units.byId(restored.displayUnitId)
                    InputUi(spec.id, UiFormat.n(unit.fromBase(restored.baseValue)), restored.displayUnitId)
                } else {
                    val unitId = spec.defaultUnitId ?: Units.defaultUnit(spec.family).id
                    InputUi(spec.id, "", unitId)
                }
            }
        )
    }
    var output by remember(calculatorId) { mutableStateOf<CalcOutput?>(null) }
    var lastInputs by remember(calculatorId) { mutableStateOf<Map<String, InputValue>?>(null) }
    var fieldErrors by remember(calculatorId) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var globalError by remember(calculatorId) { mutableStateOf<String?>(null) }
    var savedMessage by remember(calculatorId) { mutableStateOf<String?>(null) }
    var showSaveDialog by remember(calculatorId) { mutableStateOf(false) }
    val isFavorite = remember(calculatorId) { mutableStateOf(deps.favorites.isFavorite(calculatorId)) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Result display unit overrides (per result id) — README v2 §35
    val resultUnits = remember(calculatorId) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(modifier = Modifier.weight(1f)) {
                Text(def.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    restoreTitle ?: def.category.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                deps.favorites.toggle(calculatorId, System.currentTimeMillis())
                isFavorite.value = deps.favorites.isFavorite(calculatorId)
            }) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favorite",
                    tint = if (isFavorite.value) Color(0xFFF5B301) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(def.description, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        // ---- Inputs ----
        Text("Inputs", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        for (spec in def.inputs) {
            val ui = inputsUi.first { it.specId == spec.id }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = ui.text,
                    onValueChange = { text ->
                        inputsUi = inputsUi.map { if (it.specId == spec.id) it.copy(text = text) else it }
                        fieldErrors = fieldErrors - spec.id
                    },
                    label = { Text("${spec.symbol} — ${spec.label}${if (spec.required) "" else " (optional)"}") },
                    isError = fieldErrors.containsKey(spec.id),
                    supportingText = fieldErrors[spec.id]?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                UnitDropdown(
                    family = spec.family,
                    allowedUnitIds = spec.allowedUnitIds,
                    selectedUnitId = ui.unitId,
                    onSelected = { newUnitId ->
                        // Convert the typed value to the new unit (README: value converts on unit change)
                        val newText = ui.text.toDoubleOrNull()?.let { value ->
                            val base = Units.byId(ui.unitId).toBase(value)
                            UiFormat.n(Units.byId(newUnitId).fromBase(base))
                        } ?: ui.text
                        inputsUi = inputsUi.map {
                            if (it.specId == spec.id) it.copy(unitId = newUnitId, text = newText) else it
                        }
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                globalError = null
                savedMessage = null
                try {
                    val map = buildMap<String, InputValue> {
                        for (ui in inputsUi) {
                            val text = ui.text.trim()
                            if (text.isEmpty()) continue
                            val number = text.toDoubleOrNull()
                            if (number == null) {
                                fieldErrors = fieldErrors + (ui.specId to "Enter a valid number.")
                                continue
                            }
                            put(ui.specId, InputValue(ui.specId, Units.byId(ui.unitId).toBase(number), ui.unitId))
                        }
                    }
                    val result = calc.run(map)
                    output = result
                    lastInputs = map
                    fieldErrors = emptyMap()
                } catch (e: ValidationException) {
                    output = null
                    fieldErrors = e.errors.associate { it.inputId to it.message }
                } catch (e: NumberFormatException) {
                    output = null
                    globalError = "One or more inputs are not valid numbers."
                } catch (e: Exception) {
                    output = null
                    globalError = "Calculation error: ${e.message}"
                }
            }) { Text("Calculate") }
            OutlinedButton(onClick = {
                inputsUi = def.inputs.map { spec ->
                    InputUi(spec.id, "", spec.defaultUnitId ?: Units.defaultUnit(spec.family).id)
                }
                output = null
                lastInputs = null
                fieldErrors = emptyMap()
                globalError = null
                savedMessage = null
            }) { Text("Reset") }
        }

        globalError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        savedMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        // ---- Results ----
        output?.let { out ->
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Results", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    for (r in out.results) {
                        val displayUnitId = resultUnits.value[r.id] ?: r.unitId
                        val displayValue = runCatching {
                            Units.convert(r.value, r.unitId, displayUnitId)
                        }.getOrElse { r.value }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    r.label + when {
                                        r.isRecommended -> "  ★"
                                        r.isPrimary -> ""
                                        else -> ""
                                    },
                                    style = if (r.isPrimary || r.isRecommended) {
                                        MaterialTheme.typography.titleSmall
                                    } else {
                                        MaterialTheme.typography.labelMedium
                                    },
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    "${UiFormat.n(displayValue)} ${Units.byId(displayUnitId).symbol}",
                                    style = if (r.isPrimary || r.isRecommended) {
                                        MaterialTheme.typography.headlineSmall
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    },
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            UnitDropdown(
                                family = Units.byId(r.unitId).family,
                                allowedUnitIds = null,
                                selectedUnitId = displayUnitId,
                                onSelected = { newUnit ->
                                    resultUnits.value = resultUnits.value + (r.id to newUnit)
                                },
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Formula", style = MaterialTheme.typography.labelMedium)
                    Text(def.formulaDisplay, style = MaterialTheme.typography.bodyLarge)
                    if (out.steps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Step by step", style = MaterialTheme.typography.labelMedium)
                        for (step in out.steps) {
                            Text(step, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (out.warnings.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        for (w in out.warnings) {
                            Text("⚠ $w", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistChip(
                            onClick = { clipboard.setText(AnnotatedString(ReportWriter.build(calc, lastInputs ?: emptyMap(), out, restoreTitle))) },
                            label = { Text("Copy") },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                        )
                        AssistChip(
                            onClick = { showSaveDialog = true },
                            label = { Text("Save to history") },
                            leadingIcon = { Icon(Icons.Filled.Save, null) },
                        )
                        AssistChip(
                            onClick = {
                                scope.launch {
                                    val rtl = deps.settings.language.value == LanguageMode.ARABIC
                                    val labels = ReportLabels.of(rtl)
                                    val engineerName = deps.settings.reportValue(SettingsRepository.KEY_REPORT_ENGINEER)
                                    val meta = listOfNotNull(
                                        labels.project to deps.settings.reportValue(SettingsRepository.KEY_REPORT_PROJECT),
                                        labels.client to deps.settings.reportValue(SettingsRepository.KEY_REPORT_CLIENT),
                                        labels.engineer to engineerName,
                                        labels.location to deps.settings.reportValue(SettingsRepository.KEY_REPORT_LOCATION),
                                        labels.reportNo to deps.settings.reportValue(SettingsRepository.KEY_REPORT_NO),
                                        labels.revision to deps.settings.reportValue(SettingsRepository.KEY_REPORT_REV),
                                        labels.date to java.time.LocalDate.now().toString(),
                                    ).filter { it.second.isNotBlank() }
                                    val signature = listOf(
                                        labels.preparedBy to engineerName,
                                        labels.checkedBy to deps.settings.reportValue(SettingsRepository.KEY_REPORT_CHECKED),
                                        labels.approvedBy to "",
                                    )
                                    val blocks = ReportSheet.build(calc, lastInputs ?: emptyMap(), out, restoreTitle, labels, signature)
                                    val path = deps.exporter.savePdf(def.id, def.name, meta, blocks, deps.logoStore.load(), rtl)
                                    savedMessage = if (path != null) "PDF saved: " + path else "Export cancelled."
                                }
                            },
                            label = { Text("Export PDF") },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Reference: ${def.reference}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (def.notes.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text("Notes: ${def.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showSaveDialog) {
        SaveToHistoryDialog(
            defaultTitle = restoreTitle ?: def.name,
            onDismiss = { showSaveDialog = false },
            onSave = { title ->
                val out = output
                val inMap = lastInputs
                if (out != null && inMap != null) {
                    val inputsJson = Snapshots.encodeInputs(inMap)
                    val resultsJson = Snapshots.encodeResults(out)
                    deps.history.add(def.id, title, System.currentTimeMillis(), inputsJson, resultsJson)
                    savedMessage = "Saved to history."
                }
                showSaveDialog = false
            },
        )
    }
}

@Composable
private fun SaveToHistoryDialog(
    defaultTitle: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember { mutableStateOf(defaultTitle) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to history") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(title.ifBlank { "Untitled" }) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun UnitDropdown(
    family: com.mechforge.core.units.UnitFamily,
    allowedUnitIds: List<String>?,
    selectedUnitId: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val units = allowedUnitIds?.map { Units.byId(it) } ?: Units.byFamily(family)
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(Units.byId(selectedUnitId).symbol)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for (unit in units) {
                DropdownMenuItem(
                    text = { Text("${unit.symbol}") },
                    onClick = {
                        expanded = false
                        onSelected(unit.id)
                    },
                )
            }
        }
    }
}
