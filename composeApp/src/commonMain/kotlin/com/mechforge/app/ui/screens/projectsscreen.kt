package com.mechforge.app.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies

@Composable
fun ProjectsScreen(deps: AppDependencies) {
    val projects by deps.projects.all().collectAsState(initial = emptyList())
    var showCreate by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Long?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Projects", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, "New project")
            }
        }
        Spacer(Modifier.height(12.dp))

        if (projects.isEmpty()) {
            Text(
                "No projects yet. Create one to group saved calculations.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            items(projects, key = { it.id }) { project ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(project.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${project.saved_count} saved calculation(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = {
                            renameTarget = project.id
                            renameText = project.name
                        }) { Icon(Icons.Filled.Edit, "Rename") }
                        IconButton(onClick = { deps.projects.delete(project.id) }) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New project") },
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
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }

    renameTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename project") },
            text = {
                OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    deps.projects.rename(id, renameText.ifBlank { "Untitled" })
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
        )
    }
}
