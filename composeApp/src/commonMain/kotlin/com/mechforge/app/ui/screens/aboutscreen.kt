package com.mechforge.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("⚙ MechForge", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Mechanical Engineering Toolkit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text("Version 0.1.0 — Pilot (Desktop)", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))

        Text("Calculate. Check. Engineer.", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Text(
            "MechForge gives mechanical engineers one reliable, offline toolkit for the " +
                "calculations they use every day: hydraulics, HVAC, thermodynamics, mechanical " +
                "design and piping. Every calculator shows its formula, its steps and its " +
                "engineering reference — a result you can check, not just a number.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        Text("Local-first", style = MaterialTheme.typography.titleMedium)
        Text(
            "All data stays in a local SQLite database on this machine. No account, " +
                "no telemetry, no internet required.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        Text("References & licensing", style = MaterialTheme.typography.titleMedium)
        Text(
            "Formulas are implemented and cited; copyrighted standard tables are NOT embedded. " +
                "Results are engineering calculations, not code compliance.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
