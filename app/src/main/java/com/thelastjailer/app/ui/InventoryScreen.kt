package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.ItemCatalog

@Composable
fun InventoryScreen(state: GameState, modifier: Modifier = Modifier) {
    val items = ItemCatalog.resolve(state.inventory)
    Column(modifier = modifier.fillMaxSize().padding(14.dp)) {
        Text("INVENTORY", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                Text("Kaelen carries nothing of note yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text(item.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
