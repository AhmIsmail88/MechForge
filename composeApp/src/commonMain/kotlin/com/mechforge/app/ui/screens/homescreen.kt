package com.mechforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies
import com.mechforge.app.ui.i18n.LocalStrings
import com.mechforge.app.ui.Screen
import com.mechforge.core.engine.Calculator
import com.mechforge.core.engine.CalculatorCategory
import com.mechforge.core.engine.CalculatorRegistry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(deps: AppDependencies, onNavigate: (Screen) -> Unit) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<CalculatorCategory?>(null) }

    val favorites by deps.favorites.all().collectAsState(initial = emptyList())
    val favoriteIds = favorites.map { it.calculator_id }.toSet()

    Column(modifier = Modifier.fillMaxSize()) {
        // Fixed header: the search box stays put instead of being sliced by the top bar while scrolling.
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text(strings.homeTitle, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(strings.homeSearchLabel) },
                placeholder = { Text(strings.homeSearchPlaceholder) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            val results = if (query.isBlank()) {
                emptyList()
            } else {
                CalculatorRegistry.search(query)
            }
            if (results.isNotEmpty()) {
                Card {
                    Column(modifier = Modifier.padding(8.dp)) {
                        for (calc in results) {
                            Text(
                                "${calc.def.name}  —  ${calc.def.category.displayName}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = ""
                                        onNavigate(Screen.Calculator(calc.def.id))
                                    }
                                    .padding(10.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Favorites
            val favoriteCalcs = CalculatorRegistry.all.filter { it.def.id in favoriteIds }
            if (favoriteCalcs.isNotEmpty()) {
                Text(strings.homeSectionFavorites, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    favoriteCalcs.forEach { calc ->
                        FilterChip(
                            selected = false,
                            onClick = { onNavigate(Screen.Calculator(calc.def.id)) },
                            label = { Text(calc.def.name) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Recently used
            val recents = deps.history.recentCalculators(6)
            if (recents.isNotEmpty()) {
                Text(strings.homeSectionRecent, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    recents.forEach { recent ->
                        CalculatorRegistry.byId(recent.calculatorId)?.let { calc ->
                            FilterChip(
                                selected = false,
                                onClick = { onNavigate(Screen.Calculator(calc.def.id)) },
                                label = { Text(calc.def.name) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Category filter
            Text(strings.homeSectionCategories, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text(strings.all) },
                )
                for (category in CalculatorCategory.entries) {
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.displayName) },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // Calculator cards.
            // Wrapping rows (not a fixed-height lazy grid): every card is always laid out and
            // reachable by scrolling. The previous fixed-height grid clipped the tail of the
            // list on phones, where the grid collapses to a single column.
            val calculators = CalculatorRegistry.all
                .filter { selectedCategory == null || it.def.category == selectedCategory }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val minCardWidth = 230.dp
                val columns = maxOf(1, (maxWidth / minCardWidth).toInt())

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (chunk in calculators.chunked(columns)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            chunk.forEach { calc ->
                                CalculatorCard(
                                    calc = calc,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigate(Screen.Calculator(calc.def.id)) },
                                )
                            }
                            repeat(columns - chunk.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CalculatorCard(calc: Calculator, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(calc.def.name, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                calc.def.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                calc.def.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
        }
    }
}
