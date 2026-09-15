package com.mechforge.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mechforge.app.AppDependencies
import com.mechforge.app.ui.Screen
import com.mechforge.app.ui.theme.glassBorder
import com.mechforge.core.engine.CalculatorRegistry
import com.mechforge.app.ui.i18n.LocalStrings

@Composable
fun FavoritesScreen(deps: AppDependencies, onNavigate: (Screen) -> Unit) {
    val strings = LocalStrings.current
    val favorites by deps.favorites.all().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(strings.favoritesTitle, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        val favoriteCalcs = favorites.mapNotNull { CalculatorRegistry.byId(it.calculator_id) }
        if (favoriteCalcs.isEmpty()) {
            Text(strings.favoritesEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(strings.favoritesEmptyHint, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            items(favoriteCalcs, key = { it.def.id }) { calc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassBorder()
                        .clickable { onNavigate(Screen.Calculator(calc.def.id)) },
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(calc.def.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            calc.def.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
