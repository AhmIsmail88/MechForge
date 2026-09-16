package com.mechforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies
import com.mechforge.app.data.ProjectInfo
import com.mechforge.app.ui.i18n.LocalStrings
import com.mechforge.app.data.Snapshots
import com.mechforge.app.export.ProjectPackage
import com.mechforge.app.export.ProjectRegister
import com.mechforge.app.export.ReportLabels
import com.mechforge.app.ui.theme.glassBorder
import com.mechforge.core.engine.CalculatorRegistry
import kotlinx.coroutines.launch

/**
 * The project area: a list of projects, and the engineering record of each one.
 *
 * Following the engineering audit (sections 3, 6, 7) the project - not Settings - owns the
 * project number, the client/consultant/contractor, the location, the revision, the status,
 * the responsibility and the design basis, and a calculation report prints whatever the
 * active project holds.
 */
@Composable
fun ProjectsScreen(deps: AppDependencies) {
    val strings = LocalStrings.current
    val projects by deps.projects.all().collectAsState(initial = emptyList())
    val activeId by deps.projects.activeProjectIdFlow().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    var lastExport by remember { mutableStateOf("") }
    var showCreate by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }
    var infoTarget by remember { mutableStateOf<Long?>(null) }
    var infoDraft by remember { mutableStateOf<ProjectInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(strings.projectsTitle, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, strings.projectsNew)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (projects.isEmpty()) {
            Text(strings.projectsEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(strings.projectsEmptyHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (lastExport.isNotBlank()) {
            Text(lastExport, style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(projects, key = { it.id }) { project ->
                val info = deps.projects.info(project.id)
                Card(modifier = Modifier.fillMaxWidth().glassBorder()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(project.name, style = MaterialTheme.typography.titleSmall)
                                if (project.id == activeId) {
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(strings.projectActive, style = MaterialTheme.typography.labelSmall) },
                                    )
                                }
                            }
                            Text(
                                strings.savedCalculations(project.saved_count.toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val details = listOfNotNull(
                                info?.projectNumber?.takeIf { it.isNotBlank() }?.let { "No. $it" },
                                info?.client?.takeIf { it.isNotBlank() },
                                info?.revision?.takeIf { it.isNotBlank() }?.let { "Rev. $it" },
                                info?.status?.takeIf { it.isNotBlank() },
                            ).joinToString("  -  ")
                            val statusSummary = deps.history.countsByStatus(project.id)
                                .filter { it.second > 0L }
                                .joinToString("  -  ") { (status, count) ->
                                    "${status.ifBlank { "-" }} $count"
                                }
                            if (statusSummary.isNotBlank()) {
                                Text(
                                    statusSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (details.isNotBlank()) {
                                Text(
                                    details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (project.id != activeId) {
                            IconButton(onClick = { deps.projects.setActiveProject(project.id) }) {
                                Icon(Icons.Filled.Check, strings.projectSetActive)
                            }
                        }
                        IconButton(onClick = {
                            infoTarget = project.id
                            infoDraft = info
                        }) { Icon(Icons.Filled.Info, strings.projectInfoTitle) }
                        IconButton(onClick = {
                            scope.launch {
                                val record = deps.projects.info(project.id) ?: return@launch
                                val rtl = deps.settings.reportIsArabic()
                                val labels = ReportLabels.of(rtl)
                                val today = java.time.LocalDate.now().toString()
                                val records = deps.history.byProject(project.id).map { row ->
                                    ProjectPackage.Record(
                                        calculationNumber = row.calculation_number
                                            ?: row.id.toString().padStart(3, '0'),
                                        calculatorId = row.calculator_id,
                                        title = row.title,
                                        revision = row.revision.orEmpty(),
                                        status = row.status.orEmpty(),
                                        timestamp = row.timestamp,
                                        inputsJson = row.inputs_json,
                                        resultsJson = row.results_json,
                                    )
                                }
                                val blocks = ProjectPackage.build(
                                    project = record,
                                    records = records,
                                    labels = labels,
                                    date = today,
                                    signature = listOf(
                                        labels.preparedBy to record.preparedBy,
                                        labels.checkedBy to record.checkedBy,
                                    ),
                                    decodeInputs = { Snapshots.decodeInputs(it) },
                                    decodeOutput = { Snapshots.decodeResults(it) },
                                )
                                val path = deps.exporter.savePdf(
                                    defaultName = "package_" + (record.projectNumber.ifBlank { project.name }),
                                    title = labels.packageTitle,
                                    meta = emptyList(),
                                    blocks = blocks,
                                    logo = deps.logoStore.load(),
                                    rtl = rtl,
                                )
                                lastExport = path ?: ""
                            }
                        }) { Icon(Icons.Filled.Share, strings.projectsPackage) }
                        IconButton(onClick = {
                            scope.launch {
                                val info = deps.projects.info(project.id) ?: return@launch
                                val rtl = deps.settings.reportIsArabic()
                                val labels = ReportLabels.of(rtl)
                                val today = java.time.LocalDate.now().toString()
                                val entries = deps.history.byProject(project.id).map { row ->
                                    val name = runCatching {
                                        CalculatorRegistry.byIdOrThrow(row.calculator_id).def.name
                                    }.getOrElse { row.title }
                                    ProjectRegister.Entry(
                                        calculationNumber = row.calculation_number
                                            ?: row.id.toString().padStart(3, '0'),
                                        calculatorName = name,
                                        revision = row.revision.orEmpty(),
                                        status = row.status.orEmpty(),
                                        preparedBy = info.preparedBy,
                                        date = java.time.Instant.ofEpochMilli(row.timestamp)
                                            .atOffset(java.time.ZoneOffset.UTC)
                                            .toLocalDate()
                                            .toString(),
                                    )
                                }
                                val blocks = ProjectRegister.build(info, entries, labels, today)
                                val path = deps.exporter.savePdf(
                                    defaultName = "register_" + (info.projectNumber.ifBlank { project.name }),
                                    title = labels.registerTitle,
                                    meta = emptyList(),
                                    blocks = blocks,
                                    logo = deps.logoStore.load(),
                                    rtl = rtl,
                                )
                                lastExport = if (path != null) path else ""
                            }
                        }) { Icon(Icons.Filled.List, strings.projectsRegister) }
                        IconButton(onClick = {
                            renameTarget = project.id
                            renameText = project.name
                        }) { Icon(Icons.Filled.Edit, strings.projectsRenameTitle) }
                        IconButton(onClick = { deps.projects.delete(project.id) }) {
                            Icon(Icons.Filled.Delete, strings.projectsDelete)
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(strings.projectsNew) },
            text = {
                OutlinedTextField(value = createName, onValueChange = { createName = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (createName.isNotBlank()) {
                        deps.projects.create(createName.trim(), "", System.currentTimeMillis())
                    }
                    createName = ""
                    showCreate = false
                }) { Text(strings.create) }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text(strings.cancel) } },
        )
    }

    renameTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(strings.projectsRenameTitle) },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    deps.projects.rename(id, renameText.ifBlank { "Untitled" })
                    renameTarget = null
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text(strings.cancel) } },
        )
    }

    val draft = infoDraft
    val target = infoTarget
    if (draft != null && target != null) {
        AlertDialog(
            onDismissRequest = { infoTarget = null; infoDraft = null },
            title = { Text(strings.projectInfoTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ProjectField(strings.projectsNameLabel, draft.name) { infoDraft = draft.copy(name = it) }
                    ProjectField(strings.fieldProjectNumber, draft.projectNumber) { infoDraft = draft.copy(projectNumber = it) }
                    ProjectField(strings.fieldProjectClient, draft.client) { infoDraft = draft.copy(client = it) }
                    ProjectField(strings.fieldProjectConsultant, draft.consultant) { infoDraft = draft.copy(consultant = it) }
                    ProjectField(strings.fieldProjectContractor, draft.contractor) { infoDraft = draft.copy(contractor = it) }
                    ProjectField(strings.fieldProjectLocation, draft.location) { infoDraft = draft.copy(location = it) }
                    ProjectField(strings.fieldProjectRevision, draft.revision) { infoDraft = draft.copy(revision = it) }
                    ProjectField(strings.fieldProjectStatus, draft.status) { infoDraft = draft.copy(status = it) }
                    ProjectField(strings.fieldProjectPreparedBy, draft.preparedBy) { infoDraft = draft.copy(preparedBy = it) }
                    ProjectField(strings.fieldProjectCheckedBy, draft.checkedBy) { infoDraft = draft.copy(checkedBy = it) }
                    ProjectField(strings.fieldProjectCodes, draft.codes) { infoDraft = draft.copy(codes = it) }
                    ProjectField(strings.fieldProjectCodeEdition, draft.codeEdition) { infoDraft = draft.copy(codeEdition = it) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    deps.projects.saveInfo(target, draft)
                    infoTarget = null
                    infoDraft = null
                }) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { infoTarget = null; infoDraft = null }) { Text(strings.cancel) } },
        )
    }
}

@Composable
private fun ProjectField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}