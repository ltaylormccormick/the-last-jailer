package com.thelastjailer.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.thelastjailer.app.data.EntitlementRepository
import com.thelastjailer.app.data.LocalEntitlementRepository
import com.thelastjailer.app.data.StoryRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JailerApp() }
    }
}

private val Night = Color(0xFF07090D)
private val Panel = Color(0xFF11151B)
private val Gold = Color(0xFFC79A4A)
private val GoldSoft = Color(0xFF8D6A32)
private val TextCream = Color(0xFFE8DFC9)

@Composable
fun JailerApp() {
    val context = LocalContext.current
    val store = remember(context) {
        SaveStore(context.getSharedPreferences("jailer_saves", Context.MODE_PRIVATE))
    }
    val entitlements = remember(context) {
        LocalEntitlementRepository(context.getSharedPreferences("jailer_entitlements", Context.MODE_PRIVATE))
    }
    var state by remember {
        val slot = store.currentActiveSlot() ?: 1
        mutableStateOf(store.load(slot) ?: GameState(activeSlot = slot))
    }
    var showSlots by remember { mutableStateOf(false) }
    val node = StoryRepository.node(state.sceneId)
    val choices = StoryRepository.visibleChoices(node, state)
    val chapterUnlocked = entitlements.isChapterUnlocked(node.chapterId)

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF151A21), Night))
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("☰", color = Gold, fontSize = 25.sp)
                Text("THE LAST JAILER", color = TextCream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showSlots = true }) { Text("SAVE", color = Gold) }
            }

            if (!chapterUnlocked) {
                LockedChapterScreen(
                    node = node,
                    entitlements = entitlements,
                    modifier = Modifier.fillMaxSize().padding(14.dp)
                )
            } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val chapter = StoryRepository.chapter(node.chapterId)
                    Text(
                        (chapter?.title ?: node.chapterId).uppercase(),
                        color = Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    SceneIllustration(node.illustrationId, Modifier.fillMaxWidth().height(220.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(node.title, color = TextCream, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(node.narrativeText, color = TextCream.copy(alpha = .92f), style = MaterialTheme.typography.bodyLarge, lineHeight = 23.sp)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    StatsBar(state)
                    Spacer(Modifier.height(8.dp))
                    choices.forEachIndexed { index, choice ->
                        Button(
                            onClick = {
                                state = state.applyChoice(choice)
                                store.save(state.activeSlot, state)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            shape = RoundedCornerShape(5.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (index == 0) Color(0xFF211A0F) else Color(0xFF171B21),
                                contentColor = TextCream
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (index == 0) Gold else GoldSoft.copy(alpha = .55f))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (index == 0) "◆" else "›", color = Gold, fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(choice.label, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = { showSlots = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("SAVE / LOAD", color = Gold)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            }
        }
    }

    if (showSlots) {
        SaveDialog(store, entitlements, state, { state = it }, { showSlots = false })
    }
}

@Composable
private fun LockedChapterScreen(
    node: StoryNode,
    entitlements: EntitlementRepository,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Text("CHAPTER LOCKED", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "${node.title} is part of the full story. Unlock it with a one-time purchase to continue Kaelen's tale beyond Chapter III.",
            color = TextCream.copy(alpha = .9f),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { entitlements.unlockFullStory() }) {
            Text("UNLOCK FULL STORY")
        }
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { entitlements.setDebugPurchaseSimulated(true) }) {
                Text("DEBUG: SIMULATE PURCHASE", color = Gold)
            }
        }
    }
}

@Composable
private fun SceneIllustration(illustrationId: String, modifier: Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF26303A), Color(0xFF090B10))))
            val tower = Color(0xFF111821)
            drawRect(tower, Offset(size.width * .08f, size.height * .45f), Size(size.width * .09f, size.height * .35f))
            drawRect(tower, Offset(size.width * .17f, size.height * .34f), Size(size.width * .07f, size.height * .46f))
            drawRect(tower, Offset(size.width * .72f, size.height * .39f), Size(size.width * .08f, size.height * .41f))
            val mountain = Path().apply {
                moveTo(0f, size.height * .68f)
                lineTo(size.width * .28f, size.height * .38f)
                lineTo(size.width * .46f, size.height * .67f)
                lineTo(size.width * .63f, size.height * .43f)
                lineTo(size.width, size.height * .69f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountain, Color(0xFF141A20))
            val tree = Color(0xFF0B0D10)
            drawCircle(tree, size.minDimension * .33f, Offset(size.width * .73f, size.height * .25f))
            drawCircle(tree, size.minDimension * .29f, Offset(size.width * .88f, size.height * .38f))
            drawRect(tree, Offset(size.width * .72f, size.height * .22f), Size(size.width * .12f, size.height * .78f))
            val doorLeft = size.width * .63f
            val doorTop = size.height * .30f
            val doorW = size.width * .20f
            val doorH = size.height * .49f
            val radius = CornerRadius(12f, 12f)
            drawRoundRect(color = Color(0xFF17120D), topLeft = Offset(doorLeft, doorTop), size = Size(doorW, doorH), cornerRadius = radius)
            drawRoundRect(color = GoldSoft.copy(alpha = .7f), topLeft = Offset(doorLeft, doorTop), size = Size(doorW, doorH), cornerRadius = radius, style = Stroke(3f))
            drawCircle(color = Gold, radius = 4f, center = Offset(doorLeft + doorW * .76f, doorTop + doorH * .54f))
            drawLine(GoldSoft, Offset(doorLeft + doorW * .12f, doorTop + doorH * .72f), Offset(doorLeft + doorW * .88f, doorTop + doorH * .72f), 2f)
            drawCircle(color = Gold.copy(alpha = .35f), radius = 28f, center = Offset(doorLeft + doorW * .52f, doorTop + doorH * .32f))
            drawCircle(color = Gold, radius = 5f, center = Offset(doorLeft + doorW * .52f, doorTop + doorH * .32f))
            val kx = size.width * .25f
            val ky = size.height * .57f
            drawCircle(color = Color(0xFF0A0B0D), radius = 18f, center = Offset(kx, ky - 47f))
            drawRoundRect(color = Color(0xFF0A0B0D), topLeft = Offset(kx - 25f, ky - 28f), size = Size(50f, 92f), cornerRadius = CornerRadius(16f, 16f))
            drawLine(Color(0xFF6C6B65), Offset(kx + 18f, ky + 12f), Offset(kx + 56f, ky + 54f), 5f, StrokeCap.Round)
            repeat(34) { i ->
                val x = (i * 47f) % size.width
                val y = (i * 71f) % size.height
                drawLine(Color.White.copy(alpha = .10f), Offset(x, y), Offset(x - 8f, y + 22f), 1f)
            }
        }
        Text(
            "PLACEHOLDER ART — $illustrationId",
            modifier = Modifier.padding(12.dp).align(Alignment.BottomStart),
            color = TextCream.copy(alpha = .78f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun StatsBar(state: GameState) {
    Column(modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(6.dp)).padding(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("❤ ${state.health}/${state.maxHealth}", color = Color(0xFFC25B5B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("⚔ COURAGE ${state.courage}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("⚖ HONOUR ${state.honour}", color = Color(0xFF8AA7C2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("LVL ${state.level} · XP ${state.xp}/${state.xpToNextLevel}", color = TextCream, fontSize = 12.sp)
            Text("⛁ ${state.gold}", color = Gold, fontSize = 12.sp)
            Text("🏆 ${state.trophies.size}", color = TextCream, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SaveDialog(
    store: SaveStore,
    entitlements: EntitlementRepository,
    state: GameState,
    onStateChange: (GameState) -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Save Slots") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                (1..entitlements.maxSaveSlots()).forEach { slot ->
                    val saved = store.load(slot)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Slot $slot", modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            store.save(slot, state.copy(activeSlot = slot))
                            store.setActiveSlot(slot)
                        }) { Text("SAVE") }
                        TextButton(enabled = saved != null, onClick = {
                            saved?.let {
                                store.setActiveSlot(slot)
                                onStateChange(it)
                                onClose()
                            }
                        }) { Text("LOAD") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("CLOSE") } }
    )
}
