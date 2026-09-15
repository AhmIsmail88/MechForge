package com.mechforge.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies
import com.mechforge.app.ui.i18n.LocalStrings
import com.mechforge.app.ui.i18n.UiStrings
import com.mechforge.app.ui.theme.Glass
import com.mechforge.app.ui.theme.GlassBackdrop
import com.mechforge.app.ui.theme.glassChrome
import com.mechforge.app.ui.theme.glassPanel
import com.mechforge.app.ui.screens.AboutScreen
import com.mechforge.app.ui.screens.CalculatorScreen
import com.mechforge.app.ui.screens.ConverterScreen
import com.mechforge.app.ui.screens.FavoritesScreen
import com.mechforge.app.ui.screens.HistoryScreen
import com.mechforge.app.ui.screens.HomeScreen
import com.mechforge.app.ui.screens.ProjectsScreen
import com.mechforge.app.ui.screens.ReferencesScreen
import com.mechforge.app.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

/** Simple state-based navigation (pilot: no external nav library). */
sealed interface Screen {
    data object Home : Screen
    data object Converter : Screen
    data object History : Screen
    data object Favorites : Screen
    data object Projects : Screen
    data object Settings : Screen
    data object References : Screen
    data object About : Screen
    data class Calculator(
        val calculatorId: String,
        val restoreInputs: Map<String, com.mechforge.core.engine.InputValue>? = null,
        val restoreTitle: String? = null,
    ) : Screen
}

private data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

private fun navItems(strings: UiStrings): List<NavItem> = listOf(
    NavItem(Screen.Home, strings.navHome, Icons.Filled.Home),
    NavItem(Screen.Converter, strings.navConverter, Icons.Filled.SwapHoriz),
    NavItem(Screen.History, strings.navHistory, Icons.Filled.History),
    NavItem(Screen.Favorites, strings.navFavorites, Icons.Filled.Star),
    NavItem(Screen.Projects, strings.navProjects, Icons.Filled.Folder),
    NavItem(Screen.References, strings.navReferences, Icons.Filled.List),
    NavItem(Screen.Settings, strings.navSettings, Icons.Filled.Settings),
    NavItem(Screen.About, strings.navAbout, Icons.Filled.Info),
)

/** Width below which the fixed sidebar is replaced by a drawer (phones / small windows). */
private val CompactWidth = 600.dp

private fun isSelected(current: Screen, target: Screen): Boolean =
    current == target || (current is Screen.Calculator && target == Screen.Home)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechForgeApp(deps: AppDependencies) {
    val strings = LocalStrings.current
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Glass pass: the backdrop is painted once here; every screen stays transparent
        // so the translucent surfaces above it read as frosted panes.
        GlassBackdrop()
        if (maxWidth < CompactWidth) {
            // Phone / narrow window: navigation drawer + top bar so the content gets the full width.
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        // Frosted pane with a dense fill: the menu stays readable and the
                        // backdrop only glows through it (a nearly transparent menu looked broken).
                        drawerContainerColor = Glass.fillStrong(),
                        // Keep the drawer off the far edge on phones: the Material default (360dp)
                        // covers almost the whole width on a ~370dp-wide screen.
                        modifier = Modifier.widthIn(max = 300.dp),
                    ) {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                strings.appName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                            )
                            Text(
                                strings.appTagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "By Ahmed Ismail",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            Spacer(Modifier.height(12.dp))
                            for (item in navItems(strings)) {
                                NavigationDrawerItem(
                                    label = { Text(item.label) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    selected = isSelected(screen, item.screen),
                                    onClick = {
                                        screen = item.screen
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                },
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
title = { Text(strings.appName) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = strings.openNavigation)
                                }
                            },
                        )
                    },
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        ScreenHost(screen, deps) { screen = it }
                    }
                }
            }
        } else {
            // Desktop / tablet: the engineering-workspace layout from README §28.
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(current = screen, onNavigate = { screen = it })
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    ScreenHost(screen, deps) { screen = it }
                }
            }
        }
    }
}

@Composable
private fun ScreenHost(screen: Screen, deps: AppDependencies, onNavigate: (Screen) -> Unit) {
    when (screen) {
        Screen.Home -> HomeScreen(deps, onNavigate)
        Screen.Converter -> ConverterScreen()
        Screen.History -> HistoryScreen(deps, onNavigate)
        Screen.Favorites -> FavoritesScreen(deps, onNavigate)
        Screen.Projects -> ProjectsScreen(deps)
        Screen.References -> ReferencesScreen(deps)
        Screen.Settings -> SettingsScreen(deps)
        Screen.About -> AboutScreen()
        is Screen.Calculator -> CalculatorScreen(
            deps = deps,
            calculatorId = screen.calculatorId,
            restoreInputs = screen.restoreInputs,
            restoreTitle = screen.restoreTitle,
            onBack = { onNavigate(Screen.Home) },
        )
    }
}

@Composable
private fun Sidebar(current: Screen, onNavigate: (Screen) -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .width(230.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            strings.appName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            strings.appTagline,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp),
        )
        for (item in navItems(strings)) {
            val selected = isSelected(current, item.screen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(item.screen) }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "v0.2.0 — Calculate. Check. Engineer.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
            Spacer(Modifier.height(6.dp))
            Text(
                "By Ahmed Ismail",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
    }
}
