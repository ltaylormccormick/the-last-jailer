package com.thelastjailer.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelastjailer.app.BuildConfig
import com.thelastjailer.app.Choice
import com.thelastjailer.app.CombatEncounter
import com.thelastjailer.app.CombatOutcome
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.data.CombatRepository
import com.thelastjailer.app.data.EntitlementRepository
import com.thelastjailer.app.data.StoryRepository

@Composable
fun StoryScreen(
    state: GameState,
    entitlements: EntitlementRepository,
    purchaseCompletedTick: Int,
    onRequestUnlock: () -> Unit,
    onRestorePurchase: () -> Unit,
    onChoiceSelected: (Choice) -> Unit,
    onCombatResolved: (CombatEncounter, CombatOutcome) -> Unit,
    onOpenJournal: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenShop: () -> Unit,
    onCombatActiveChanged: (Boolean) -> Unit,
    onOpenCharacter: () -> Unit,
    isExpandedWidth: Boolean = false,
    modifier: Modifier = Modifier
) {
    val node = StoryRepository.node(state.sceneId)
    val choices = StoryRepository.visibleChoices(node, state)
    var chapterUnlocked by remember(node.chapterId) {
        mutableStateOf(entitlements.isChapterUnlocked(node.chapterId))
    }
    LaunchedEffect(purchaseCompletedTick, node.chapterId) {
        chapterUnlocked = entitlements.isChapterUnlocked(node.chapterId)
    }
    val encounter = node.combatEncounterId?.let { CombatRepository.encounter(it) }
    var inCombat by remember(node.id) { mutableStateOf(false) }

    DisposableEffect(inCombat) {
        onCombatActiveChanged(inCombat)
        onDispose { onCombatActiveChanged(false) }
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        StoryHeader(
            node = node,
            menuExpanded = menuExpanded,
            onOpenMenu = { menuExpanded = true },
            onDismissMenu = { menuExpanded = false },
            onOpenShop = { menuExpanded = false; onOpenShop() },
            shopEnabled = !inCombat && chapterUnlocked,
            onOpenJournal = onOpenJournal,
            onOpenInventory = onOpenInventory,
            onOpenTrophies = onOpenCharacter,
            trophyCount = state.trophies.size
        )
        Spacer(Modifier.height(8.dp))

        if (!chapterUnlocked) {
            LockedChapterScreen(
                entitlements = entitlements,
                onRequestUnlock = onRequestUnlock,
                onRestorePurchase = onRestorePurchase,
                onNotNow = { menuExpanded = true },
                onDebugUnlocked = { chapterUnlocked = true },
                modifier = Modifier.fillMaxSize()
            )
            return@Column
        }

        if (encounter != null && inCombat) {
            CombatScreen(
                encounter = encounter,
                playerState = state,
                onResolved = { outcome ->
                    onCombatResolved(encounter, outcome)
                    inCombat = false
                },
                modifier = Modifier.fillMaxSize()
            )
            return@Column
        }

        ChapterThumbnailStrip(node)
        Spacer(Modifier.height(10.dp))

        if (isExpandedWidth) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NarrativePanel(node, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (StoryRepository.isEnding(node)) CompletionPanel(state, onOpenCharacter)
                        ActionArea(encounter, choices, onEngage = { inCombat = true }, onChoiceSelected)
                    }
                    Column {
                        StatsBar(state)
                        Spacer(Modifier.height(8.dp))
                        CharacterSummaryCard(state)
                    }
                }
            }
        } else {
            val phoneScrollState = rememberScrollState()
            LaunchedEffect(node.id) { phoneScrollState.scrollTo(0) }
            // The whole scene (illustration, text, stats, choices, character card) scrolls as
            // one unit, rather than splitting the screen with weight() between the narrative
            // panel and the section below it: giving the narrative panel a fixed share of the
            // screen meant a tall stats/choices/character-card section could squeeze it down to
            // barely more than the illustration's height, leaving the title and body text
            // scrolled into a sliver too thin to notice, let alone read. Scrolling the full
            // column guarantees both the full narrative text and the choices below it render at
            // their natural size and stay reachable regardless of how long the scene is.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(phoneScrollState)
            ) {
                NarrativeContent(node)
                if (StoryRepository.isEnding(node)) {
                    Spacer(Modifier.height(12.dp))
                    CompletionPanel(state, onOpenCharacter)
                }
                Spacer(Modifier.height(8.dp))
                StatsBar(state)
                Spacer(Modifier.height(8.dp))
                ActionArea(encounter, choices, onEngage = { inCombat = true }, onChoiceSelected)
                Spacer(Modifier.height(8.dp))
                CharacterSummaryCard(state)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StoryHeader(
    node: StoryNode,
    onOpenMenu: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onOpenShop: () -> Unit,
    shopEnabled: Boolean,
    onOpenJournal: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenTrophies: () -> Unit,
    trophyCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box {
            TextButton(onClick = onOpenMenu) { Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = JailerColors.Gold) }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(text = { Text(if (shopEnabled) "Shop" else "Shop (unavailable here)") }, enabled = shopEnabled, onClick = onOpenShop)
                DropdownMenuItem(text = { Text("Inventory") }, enabled = shopEnabled, onClick = { onDismissMenu(); onOpenInventory() })
                DropdownMenuItem(text = { Text("Journal") }, onClick = { onDismissMenu(); onOpenJournal() })
                DropdownMenuItem(text = { Text("Character & trophies") }, onClick = { onDismissMenu(); onOpenTrophies() })
            }
        }
        TextButton(onClick = onOpenJournal) { Icon(Icons.Filled.MenuBook, contentDescription = "Journal", tint = JailerColors.Gold) }
        Text(
            "THE LAST JAILER",
            color = JailerColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onOpenInventory, enabled = shopEnabled) { Icon(Icons.Filled.Backpack, contentDescription = "Inventory", tint = JailerColors.Gold) }
        TextButton(onClick = onOpenTrophies) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = "Trophies", tint = JailerColors.Gold)
                Spacer(Modifier.width(4.dp))
                Text("$trophyCount", color = JailerColors.Gold, fontSize = 14.sp)
            }
        }
    }
}

/**
 * A non-spoiler progress cue for where the player is in the current chapter. Deliberately shows
 * no scene titles: [StoryRepository.nodesInChapter] returns every node belonging to the chapter
 * regardless of whether the player has actually reached it yet, so the previous title-card strip
 * revealed the names (and by extension the existence/order) of scenes the player hadn't visited.
 * A dot per scene, plus a plain "scene X of Y" count, carries none of that.
 */
@Composable
private fun ChapterThumbnailStrip(activeNode: StoryNode) {
    val nodes = StoryRepository.nodesInChapter(activeNode.chapterId)
    val activeIndex = nodes.indexOfFirst { it.id == activeNode.id }.coerceAtLeast(0)
    Text(
        "SCENE ${activeIndex + 1} / ${nodes.size}",
        color = JailerColors.TextPrimary.copy(alpha = .7f),
        style = MaterialTheme.typography.labelSmall
    )
}

/**
 * The tablet side-by-side layout gives this its own full-height column with no sibling to
 * compete with, so it can safely scroll within that space via [NarrativePanel]. The phone
 * layout instead scrolls this content as part of one larger column (see [StoryScreen]) and
 * uses [NarrativeContent] directly, unscrolled, to avoid splitting the screen's height between
 * this and the stats/choices/character-card section below it.
 */
@Composable
private fun NarrativePanel(node: StoryNode, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    // Without this, scrolling down on one scene would leave the next scene's narrative
    // panel starting mid-scroll instead of at the top, since ScrollState otherwise survives
    // recomposition across node changes.
    LaunchedEffect(node.id) {
        scrollState.scrollTo(0)
    }
    NarrativeContent(node, modifier = modifier.verticalScroll(scrollState))
}

@Composable
private fun NarrativeContent(node: StoryNode, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SceneIllustration(node.illustrationId, Modifier.fillMaxWidth(), naturalAspectRatio = true)
        Spacer(Modifier.height(12.dp))
        Text(node.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(readingText(node.narrativeText), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ActionArea(
    encounter: CombatEncounter?,
    choices: List<Choice>,
    onEngage: () -> Unit,
    onChoiceSelected: (Choice) -> Unit
) {
    if (encounter != null) {
        Button(
            onClick = onEngage,
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            shape = RoundedCornerShape(5.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF211A0F), contentColor = JailerColors.TextPrimary),
            border = BorderStroke(1.dp, JailerColors.Gold)
        ) {
            Text("⚔ ENGAGE", fontWeight = FontWeight.SemiBold)
        }
    } else {
        ChoiceList(choices, onChoiceSelected)
    }
}

/**
 * Every choice renders identically - order in the list carries no meaning (it's authoring order,
 * not a ranking), so styling one differently would visually nudge players toward whichever choice
 * happens to be listed first, with no connection to what it actually does. The one exception is
 * the accent stripe from [leadsToSignaledCombat]: that's not a ranking either, just an honest
 * heads-up that this path goes straight into a fight.
 */
@Composable
private fun ChoiceList(choices: List<Choice>, onChoiceSelected: (Choice) -> Unit) {
    choices.forEach { choice ->
        val signaled = leadsToSignaledCombat(choice.nextNodeId)
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Button(
                onClick = { onChoiceSelected(choice) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF171B21),
                    contentColor = JailerColors.TextPrimary
                ),
                border = BorderStroke(1.dp, JailerColors.GoldSoft.copy(alpha = .55f))
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("›", color = JailerColors.Gold, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(choice.label, fontWeight = FontWeight.SemiBold)
                }
            }
            if (signaled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 1.dp)
                        .width(3.dp)
                        .fillMaxHeight(0.62f)
                        .background(JailerColors.HealthRed, RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

/**
 * True when [nodeId] is a fight ([StoryNode.combatEncounterId] set) that hasn't opted out via
 * [CombatEncounter.isSurprise] — see that flag's doc for why a handful of fights opt out.
 */
private fun leadsToSignaledCombat(nodeId: String): Boolean {
    val encounterId = StoryRepository.node(nodeId).combatEncounterId ?: return false
    return !CombatRepository.encounter(encounterId).isSurprise
}

@Composable
private fun LockedChapterScreen(
    entitlements: EntitlementRepository,
    onRequestUnlock: () -> Unit,
    onRestorePurchase: () -> Unit,
    onNotNow: () -> Unit,
    onDebugUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(vertical = 24.dp), verticalArrangement = Arrangement.Center) {
        Text("THANK YOU FOR READING THIS FAR", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
        Spacer(Modifier.height(12.dp))
        Text(
            "I hope you’re enjoying The Last Jailer, my first project. There’s still much more of Kaelen’s story to discover.\n\nFor a one-time payment of £1.99, you can unlock the rest of the book — Chapters 10–30, with no further story purchases. Your support will help me gradually spend more time creating and developing projects like this.\n\nThank you for giving my first adventure a chance.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequestUnlock) {
            Text("UNLOCK THE REST OF THE STORY — £1.99")
        }
        TextButton(onClick = onRestorePurchase) { Text("Restore purchase") }
        TextButton(onClick = onNotNow) { Text("Not now") }
        if (BuildConfig.TESTER_UNLOCK_ENABLED) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {
                entitlements.unlockForTesting()
                onDebugUnlocked()
            }) {
                Text("TESTER: Unlock full story", color = JailerColors.Gold)
            }
            Text("Free access for this test build. No purchase is made.", style = MaterialTheme.typography.bodySmall)
        }
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = {
                entitlements.setDebugPurchaseSimulated(true)
                onDebugUnlocked()
            }) {
                Text("DEBUG: SIMULATE PURCHASE", color = JailerColors.Gold)
            }
        }
    }
}

@Composable
private fun CompletionPanel(state: GameState, onViewTrophies: () -> Unit) {
    val earned = state.trophies.intersect(StoryRepository.availableTrophies)
    OrnatePanel(Modifier.fillMaxWidth()) {
        Text("THANK YOU FOR PLAYING THE LAST JAILER", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Thank you for seeing Kaelen’s tale through. This is my first project, and I hope you’ve enjoyed the journey. Your time and support mean a great deal to me. — Lee", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Text("YOUR JOURNEY", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
        Text("Level ${state.level} · XP ${state.xp}/${state.xpToNextLevel}")
        Text("Courage ${state.courage} · Honour ${state.honour}")
        Text("Health ${state.health}/${state.maxHealth} · Gold ${state.gold}")
        Spacer(Modifier.height(8.dp))
        Text("TROPHIES · ${earned.size} / ${StoryRepository.availableTrophies.size}", color = JailerColors.Gold)
        Text("Different choices and victories reveal different trophies.", style = MaterialTheme.typography.bodySmall)
        earned.filter { it in setOf("Six, Not One", "The Whole, Undisguised", "The Last Gate") }.forEach {
            Text("🏆 $it", style = MaterialTheme.typography.titleMedium)
        }
        Button(onClick = onViewTrophies, modifier = Modifier.fillMaxWidth()) { Text("VIEW TROPHIES") }
        Spacer(Modifier.height(8.dp))
        Text("Begin the tale again using the choice below. Your current stats, equipment and trophies carry over.", style = MaterialTheme.typography.bodySmall)
        if ("The Watch Goes On" !in state.trophies) {
            Text("Beginning again earns ‘The Watch Goes On’.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
