package com.thelastjailer.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelastjailer.app.BuildConfig
import com.thelastjailer.app.Choice
import com.thelastjailer.app.GameState
import com.thelastjailer.app.StoryNode
import com.thelastjailer.app.data.EntitlementRepository
import com.thelastjailer.app.data.StoryRepository

@Composable
fun StoryScreen(
    state: GameState,
    entitlements: EntitlementRepository,
    onChoiceSelected: (Choice) -> Unit,
    onOpenJournal: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenCharacter: () -> Unit,
    onOpenMenu: () -> Unit,
    isExpandedWidth: Boolean = false,
    modifier: Modifier = Modifier
) {
    val node = StoryRepository.node(state.sceneId)
    val choices = StoryRepository.visibleChoices(node, state)
    val chapterUnlocked = entitlements.isChapterUnlocked(node.chapterId)

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
            LockedChapterScreen(node = node, entitlements = entitlements, modifier = Modifier.fillMaxSize())
            return@Column
        }

        ChapterThumbnailStrip(node)
        Spacer(Modifier.height(10.dp))

        if (isExpandedWidth) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NarrativePanel(node)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    ChoiceList(choices, onChoiceSelected)
                    Column {
                        StatsBar(state)
                        Spacer(Modifier.height(8.dp))
                        CharacterSummaryCard(state)
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                NarrativePanel(node)
                Column(modifier = Modifier.fillMaxWidth()) {
                    StatsBar(state)
                    Spacer(Modifier.height(8.dp))
                    ChoiceList(choices, onChoiceSelected)
                    Spacer(Modifier.height(8.dp))
                    CharacterSummaryCard(state)
                    Spacer(Modifier.height(8.dp))
                }
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
        TextButton(onClick = onOpenMenu) { Text("☰", color = JailerColors.Gold, fontSize = 22.sp) }
        TextButton(onClick = onOpenJournal) { Text("📖", fontSize = 20.sp) }
        Text(
            "THE LAST JAILER",
            color = JailerColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onOpenInventory) { Text("🎒", fontSize = 20.sp) }
        TextButton(onClick = onOpenTrophies) { Text("🏆 $trophyCount", color = JailerColors.Gold, fontSize = 14.sp) }
    }
}

@Composable
private fun ChapterThumbnailStrip(activeNode: StoryNode) {
    val nodes = StoryRepository.nodesInChapter(activeNode.chapterId)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(nodes) { n ->
            val active = n.id == activeNode.id
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .background(
                        if (active) JailerColors.GoldSoft.copy(alpha = .35f) else JailerColors.Panel,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(6.dp)
            ) {
                Text(
                    n.title,
                    color = if (active) JailerColors.Gold else JailerColors.TextPrimary.copy(alpha = .7f),
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun NarrativePanel(node: StoryNode) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SceneIllustration(node.illustrationId, Modifier.fillMaxWidth().height(220.dp))
        Spacer(Modifier.height(12.dp))
        Text(node.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(node.narrativeText, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ChoiceList(choices: List<Choice>, onChoiceSelected: (Choice) -> Unit) {
    choices.forEachIndexed { index, choice ->
        Button(
            onClick = { onChoiceSelected(choice) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            shape = RoundedCornerShape(5.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (index == 0) Color(0xFF211A0F) else Color(0xFF171B21),
                contentColor = JailerColors.TextPrimary
            ),
            border = BorderStroke(1.dp, if (index == 0) JailerColors.Gold else JailerColors.GoldSoft.copy(alpha = .55f))
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (index == 0) "◆" else "›", color = JailerColors.Gold, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(choice.label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LockedChapterScreen(node: StoryNode, entitlements: EntitlementRepository, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text("CHAPTER LOCKED", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "${node.title} is part of the full story. Unlock it with a one-time purchase to continue Kaelen's tale beyond Chapter III.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { entitlements.unlockFullStory() }) {
            Text("UNLOCK FULL STORY")
        }
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { entitlements.setDebugPurchaseSimulated(true) }) {
                Text("DEBUG: SIMULATE PURCHASE", color = JailerColors.Gold)
            }
        }
    }
}
