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
    val scenes = remember { demoScenes() }
    val store = remember(context) {
        SaveStore(context.getSharedPreferences("jailer_saves", Context.MODE_PRIVATE))
    }
    var state by remember { mutableStateOf(store.load(1) ?: GameState()) }
    var showSlots by remember { mutableStateOf(false) }
    val scene = scenes[state.sceneId] ?: scenes.getValue("prologue")

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

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("CHAPTER I — THE FALLEN KNIGHT", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SceneIllustration(Modifier.fillMaxWidth().height(220.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(scene.title, color = TextCream, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(scene.text, color = TextCream.copy(alpha = .92f), style = MaterialTheme.typography.bodyLarge, lineHeight = 23.sp)
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    StatsBar(state)
                    Spacer(Modifier.height(8.dp))
                    scene.choices.forEachIndexed { index, choice ->
                        Button(
                            onClick = {
                                val trophies = choice.trophy?.let { t ->
                                    if (t in state.trophies) state.trophies else state.trophies + t
                                } ?: state.trophies
                                state = state.copy(
                                    sceneId = choice.next,
                                    courage = state.courage + choice.courage,
                                    honour = state.honour + choice.honour,
                                    trophies = trophies
                                )
                                store.save(1, state)
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

    if (showSlots) {
        SaveDialog(store, state, { state = it }, { showSlots = false })
    }
}

@Composable
private fun SceneIllustration(modifier: Modifier) {
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
        Text("THE ROAD TO BLACKMERE", modifier = Modifier.padding(12.dp).align(Alignment.BottomStart), color = TextCream.copy(alpha = .78f), fontSize = 10.sp)
    }
}

@Composable
private fun StatsBar(state: GameState) {
    Row(modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(6.dp)).padding(10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Text("⚔ COURAGE ${state.courage}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("⚖ HONOUR ${state.honour}", color = Color(0xFF8AA7C2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text("🏆 ${state.trophies.size}", color = TextCream, fontSize = 12.sp)
    }
}

@Composable
private fun SaveDialog(store: SaveStore, state: GameState, onStateChange: (GameState) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Save Slots") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                (1..3).forEach { slot ->
                    val saved = store.load(slot)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Slot $slot", modifier = Modifier.weight(1f))
                        TextButton(onClick = { store.save(slot, state) }) { Text("SAVE") }
                        TextButton(enabled = saved != null, onClick = { saved?.let { onStateChange(it); onClose() } }) { Text("LOAD") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("CLOSE") } }
    )
}

private fun demoScenes(): Map<String, Scene> = mapOf(
    "prologue" to Scene("prologue", "The Road to Blackmere", "Three years ago, Kaelen Veyr wore the king's colours. Tonight he wears a broken sword and the shame of a knight who failed. At the edge of the old road, a black door stands where no door should be.", listOf(
        Choice("Approach the door.", "door", courage = 1),
        Choice("Keep walking. A disgraced knight has no business with it.", "road", honour = 1)
    )),
    "door" to Scene("door", "The Door Without a Wall", "There is no wall. No hinges. No handle. Yet when Kaelen touches the iron, something on the other side knocks once.", listOf(
        Choice("Knock back.", "knock", courage = 1, trophy = "Answered the Door"),
        Choice("Draw your sword.", "sword", courage = 1)
    )),
    "road" to Scene("road", "The Weight of a Name", "Kaelen takes ten steps. Behind him, the knock comes again. He remembers the men who died when he obeyed an order he should have questioned.", listOf(
        Choice("Turn back.", "door", honour = 1),
        Choice("Leave the past behind.", "end", courage = 1)
    )),
    "knock" to Scene("knock", "Something Answers", "The door opens onto darkness lit by a distant blue fire. A voice whispers: 'The last one has returned.'", listOf(
        Choice("Step through.", "end", courage = 1)
    )),
    "sword" to Scene("sword", "Steel and Silence", "Kaelen raises his blade. The darkness does not attack. Instead, the sword begins to hum with a warmth he has not felt since the day he was disgraced.", listOf(
        Choice("Lower the sword.", "end", honour = 1),
        Choice("Enter with steel drawn.", "end", courage = 1)
    )),
    "end" to Scene("end", "Chapter One — The First Threshold", "The road behind Kaelen is no longer the same. Somewhere beyond the door, something ancient is waiting. The choice was his. The consequences will be too.", listOf(
        Choice("Return to the beginning.", "prologue")
    ))
)
