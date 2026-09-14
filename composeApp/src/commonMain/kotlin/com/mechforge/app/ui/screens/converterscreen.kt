package com.mechforge.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechforge.core.units.UnitFamily
import com.mechforge.core.units.Units
import com.mechforge.core.util.Fmt

/** Global unit converter (README v2 §16): family tabs, from/to pickers, live conversion. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConverterScreen() {
    var family by remember { mutableStateOf(UnitFamily.PRESSURE) }
    var valueText by remember { mutableStateOf("1") }
    var fromUnitId by remember { mutableStateOf(Units.defaultUnit(UnitFamily.PRESSURE).id) }
    var toUnitId by remember { mutableStateOf(Units.byId("pa").id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Unit Converter", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        // Family selector — wraps on narrow screens instead of clipping
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (f in UnitFamily.entries) {
                OutlinedButton(
                    onClick = {
                        family = f
                        fromUnitId = Units.defaultUnit(f).id
                        toUnitId = Units.byFamily(f).first().id
                    },
                    colors = if (family == f) {
                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    },
                ) { Text(f.displayName, style = MaterialTheme.typography.labelSmall) }
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = valueText,
            onValueChange = { valueText = it },
            label = { Text("Value") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            UnitDropdown(family, null, fromUnitId) { fromUnitId = it }
            Text("→", style = MaterialTheme.typography.titleLarge)
            UnitDropdown(family, null, toUnitId) { toUnitId = it }
        }
        Spacer(Modifier.height(16.dp))

        val parsed = valueText.trim().toDoubleOrNull()
        if (parsed == null) {
            Text("Enter a number.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val result = runCatching { Units.convert(parsed, fromUnitId, toUnitId) }
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (result.isSuccess) {
                        Text(
                            "${Fmt.n(parsed, 6)} ${Units.byId(fromUnitId).symbol}  =  " +
                                "${Fmt.n(result.getOrThrow(), 6)} ${Units.byId(toUnitId).symbol}",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    } else {
                        Text(
                            "Incompatible units.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Full family table
            Text("All ${family.displayName} units", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    for (unit in Units.byFamily(family)) {
                        val converted = runCatching { Units.convert(parsed, fromUnitId, unit.id) }
                        if (converted.isSuccess) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Text(
                                    unit.symbol,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    Fmt.n(converted.getOrDefault(Double.NaN), 6),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
