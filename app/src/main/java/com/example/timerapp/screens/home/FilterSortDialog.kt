package com.example.timerapp.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterSortDialog(
    currentSort: SortType,
    currentFilter: String?,
    categories: List<String>,
    onSortChange: (SortType) -> Unit,
    onFilterChange: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sortieren & Filtern") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Sortieren nach:", style = MaterialTheme.typography.titleSmall)
                SortType.values().forEach { sortType ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == sortType,
                            onClick = { onSortChange(sortType) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sortType.label)
                    }
                }

                HorizontalDivider()

                Text("Filtern nach Kategorie:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentFilter == null,
                        onClick = { onFilterChange(null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Alle Kategorien")
                }
                categories.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentFilter == category,
                            onClick = { onFilterChange(category) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        }
    )
}

enum class SortType(val label: String) {
    DATE("Datum"),
    NAME("Name"),
    CATEGORY("Kategorie")
}

enum class TimeFilter(val label: String) {
    ALL("Alle"),
    TODAY("Heute"),
    TOMORROW("Morgen"),
    THIS_WEEK("Diese Woche")
}
