package com.thelastjailer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class Scene(val id: String, val title: String, val text: String, val choices: List<Choice>)
private data class Choice(val label: String, val next: String, val courage: Int = 0, val honour: Int = 0, val trophy: String? = null)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JailerApp() }
    }
}

@Composable
fun JailerApp() {
    val scenes = remember { demoScenes() }
    val store = remember { SaveStore(androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("jailer_saves", 0)) }
    var state by remember { mutableStateOf(store.load(1) ?: GameState()) }
    var showSlots by remember { mutableStateOf(false) }
    val scene = scenes.getValue(state.sceneId)

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("THE LAST JAILER", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Kaelen Veyr • Disgraced Knight", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(18.dp))
                    Text(scene.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(14.dp))
                    Text(scene.text, style = MaterialTheme.typography.bodyLarge)
                }

                Column {
                    Text("Courage ${state.courage}  •  Honour ${state.honour}", style = MaterialTheme.typography.labelLarge)
                    if (state.trophies.isNotEmpty()) {
                        Text("Trophies: ${state.trophies.size}", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    scene.choices.forEach { choice ->
                        Button(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            onClick = {
                                val trophies = choice.trophy?.let { state.trophies + it } ?: state.trophies
                                state = state.copy(
                                    sceneId = choice.next,
                                    courage = state.courage + choice.courage,
                                    honour = state.honour + choice.honour,
                                    trophies = trophies
                                )
                                store.save(1, state)
                            }
                        ) { Text(choice.label) }
                    }
                    OutlinedButton(
                        Modifier.fillMaxWidth().padding(top = 5.dp),
                        onClick = { showSlots = true }
                    ) { Text("Save / Load") }
                }
            }
        }
    }

    if (showSlots) {
        AlertDialog(
            onDismissRequest = { showSlots = false },
            title = { Text("Save slots") },
            text = {
                Column {
                    (1..3).forEach { slot ->
                        val saved = store.load(slot)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Slot $slot${if (saved == null) " — Empty" else " — ${saved.sceneId}"}")
                            Row {
                                TextButton(onClick = { store.save(slot, state) }) { Text("Save") }
                                TextButton(enabled = saved != null, onClick = { state = saved ?: state; showSlots = false }) { Text("Load") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSlots = false }) { Text("Close") } }
        )
    }
}

private fun demoScenes() = mapOf(
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
