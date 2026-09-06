package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.GameState
import com.thelastjailer.app.Item
import com.thelastjailer.app.data.ItemCatalog

/** One item the Inventory screen's shop offers. [repeatable] items (draughts) can be bought more than once. */
private data class ShopEntry(val itemId: String, val price: Int, val repeatable: Boolean)

private val SHOP_ENTRIES = listOf(
    ShopEntry("healing_draught", price = 40, repeatable = true),
    ShopEntry("greater_healing_draught", price = 150, repeatable = true),
    ShopEntry("sturdy_buckler", price = 70, repeatable = false),
    ShopEntry("leather_armor", price = 70, repeatable = false),
    ShopEntry("proper_helmet", price = 160, repeatable = false),
    ShopEntry("upgraded_sword", price = 200, repeatable = false),
    ShopEntry("reforged_guard_clasp", price = 50, repeatable = false)
)

/** A short "+2 damage reduction"-style line describing what an item actually does, or null for pure flavor items. */
private fun statEffectLine(item: Item): String? {
    val parts = mutableListOf<String>()
    item.combatEffect?.let { effect ->
        if (effect.damageReduction > 0) parts += "+${effect.damageReduction} damage reduction"
        if (effect.attackBonus > 0) parts += "+${effect.attackBonus} attack"
    }
    when (item.id) {
        "healing_draught" -> parts += "+25 HP"
        "greater_healing_draught" -> parts += "+50 HP"
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
fun InventoryScreen(state: GameState, onPurchase: (itemId: String, price: Int) -> Unit = { _, _ -> }, modifier: Modifier = Modifier) {
    val ownedItems = ItemCatalog.resolve(state.inventory)
    Column(modifier = modifier.fillMaxSize().padding(14.dp)) {
        Text("INVENTORY", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                if (ownedItems.isEmpty()) {
                    OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                        Text("Kaelen carries nothing of note yet.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(ownedItems) { invItem ->
                OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                    Text(invItem.name, style = MaterialTheme.typography.titleMedium)
                    Text(invItem.description, style = MaterialTheme.typography.bodyMedium)
                    statEffectLine(invItem)?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = JailerColors.Gold)
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("SHOP", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
                Text("Gold: ${state.gold}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
            }
            items(SHOP_ENTRIES.filter { it.repeatable || it.itemId !in state.inventory }) { entry ->
                val shopItem = ItemCatalog.get(entry.itemId) ?: return@items
                OrnatePanel(modifier = Modifier.fillMaxWidth()) {
                    Text(shopItem.name, style = MaterialTheme.typography.titleMedium)
                    Text(shopItem.description, style = MaterialTheme.typography.bodyMedium)
                    statEffectLine(shopItem)?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = JailerColors.Gold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${entry.price} gold", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Button(
                            enabled = state.gold >= entry.price,
                            onClick = { onPurchase(entry.itemId, entry.price) }
                        ) {
                            Text("BUY")
                        }
                    }
                }
            }
        }
    }
}
