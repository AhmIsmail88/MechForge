package com.mechforge.app

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.mechforge.app.db.DatabaseFactory
import com.mechforge.app.export.DesktopReportExporter
import com.mechforge.app.export.LogoStore
import com.mechforge.app.ui.MechForgeApp
import com.mechforge.app.ui.theme.MechForgeTheme

fun main() {
    val deps = AppDependencies(
        database = DatabaseFactory.create(),
        exporter = DesktopReportExporter(),
        logoStore = LogoStore(),
    )

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MechForge — Mechanical Engineering Toolkit",
            state = rememberWindowState(width = 1280.dp, height = 840.dp),
        ) {
            val themeMode by deps.settings.theme.collectAsState()
            val languageMode by deps.settings.language.collectAsState()
            MechForgeTheme(themeMode, languageMode) {
                MechForgeApp(deps)
            }
        }
    }
}
