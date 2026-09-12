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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    onChoiceSelected: (Choice) -> Unit,
    onCombatResolved: (CombatEncounter, CombatOutcome) -> Unit,
    onOpenJournal: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenCharacter: () -> Unit,
    onOpenMenu: () -> Unit,
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

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        StoryHeader(
            node = node,
            onOpenMenu = onOpenMenu,
            onOpenJournal = onOpenJournal,
            onOpenInventory = onOpenInventory,
            onOpenTrophies = onOpenCharacter,
            trophyCount = state.trophies.size
        )
        Spacer(Modifier.height(8.dp))

        if (!chapterUnlocked) {
            LockedChapterScreen(
                node = node,
                entitlements = entitlements,
                onRequestUnlock = onRequestUnlock,
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
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    ActionArea(encounter, choices, onEngage = { inCombat = true }, onChoiceSelected)
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
        TextButton(onClick = onOpenMenu) { Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = JailerColors.Gold) }
        TextButton(onClick = onOpenJournal) { Icon(Icons.Filled.MenuBook, contentDescription = "Journal", tint = JailerColors.Gold) }
        Text(
            "THE LAST JAILER",
            color = JailerColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onOpenInventory) { Icon(Icons.Filled.Backpack, contentDescription = "Inventory", tint = JailerColors.Gold) }
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "SCENE ${activeIndex + 1} OF ${nodes.size}",
            color = JailerColors.TextPrimary.copy(alpha = .7f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items(nodes) { n ->
                val active = n.id == activeNode.id
                Box(
                    modifier = Modifier
                        .size(if (active) 9.dp else 6.dp)
                        .background(
                            if (active) JailerColors.Gold else JailerColors.TextPrimary.copy(alpha = .3f),
                            CircleShape
                        )
                )
            }
        }
    }
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
        SceneIllustration(node.illustrationId, Modifier.fillMaxWidth().height(220.dp))
        Spacer(Modifier.height(12.dp))
        Text(node.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(node.narrativeText, style = MaterialTheme.typography.bodyLarge)
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
    node: StoryNode,
    entitlements: EntitlementRepository,
    onRequestUnlock: () -> Unit,
    onDebugUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text("CHAPTER LOCKED", style = MaterialTheme.typography.labelLarge, color = JailerColors.Gold)
        Spacer(Modifier.height(12.dp))
        Text(
            "${node.title} is part of the full story. Unlock it with a one-time purchase to continue Kaelen's tale beyond Chapter V.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequestUnlock) {
            Text("UNLOCK FULL STORY")
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
