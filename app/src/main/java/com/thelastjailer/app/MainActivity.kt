package com.thelastjailer.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Scene(
    val id: String,
    val title: String,
    val text: String,
    val choices: List<Choice>
)

data class Choice(
    val label: String,
    val next: String,
    val courage: Int = 0,
    val honour: Int = 0,
    val trophy: String? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                JailerApp()
            }
        }
    }
}

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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "THE LAST JAILER",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kaelen Veyr • Disgraced Knight",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = scene.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = scene.text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Courage ${state.courage}  •  Honour ${state.honour}",
                    style = MaterialTheme.typography.labelLarge
                )

                if (state.trophies.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Trophies: ${state.trophies.size}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                scene.choices.forEach { choice ->
                    Button(
                        onClick = {
                            val newTrophies = choice.trophy?.let { trophy ->
                                state.trophies + trophy
                            } ?: state.trophies

                            state = state.copy(
                                sceneId = choice.next,
                                courage = state.courage + choice.courage,
                                honour = state.honour + choice.honour,
                                trophies = newTrophies
                            )
                            store.save(1, state)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Text(text = choice.label)
                    }
                }

                OutlinedButton(
                    onClick = { showSlots = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                ) {
                    Text(text = "Save / Load")
                }
            }
        }
    }

    if (showSlots) {
        AlertDialog(
            onDismissRequest = { showSlots = false },
            title = { Text(text = "Save slots") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    (1..3).forEach { slot ->
                        val saved = store.load(slot)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Slot $slot${if (saved == null) " — Empty" else " — ${saved.sceneId}"}",
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { store.save(slot, state) }
                            ) {
                                Text(text = "Save")
                            }
                            TextButton(
                                enabled = saved != null,
                                onClick = {
                                    if (saved != null) {
                                        state = saved
                                        showSlots = false
                                    }
                                }
                            ) {
                                Text(text = "Load")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSlots = false }) {
                    Text(text = "Close")
                }
            }
        )
    }
}

private fun demoScenes(): Map<String, Scene> = mapOf(
    "prologue" to Scene(
        id = "prologue",
        title = "The Road to Blackmere",
        text = "Three years ago, Kaelen Veyr wore the king's colours. Tonight he wears a broken sword and the shame of a knight who failed. At the edge of the old road, a black door stands where no door should be.",
        choices = listOf(
            Choice("Approach the door.", "door", courage = 1),
            Choice("Keep walking. A disgraced knight has no business with it.", "road", honour = 1)
        )
    ),
    "door" to Scene(
        id = "door",
        title = "The Door Without a Wall",
        text = "There is no wall. No hinges. No handle. Yet when Kaelen touches the iron, something on the other side knocks once.",
        choices = listOf(
            Choice("Knock back.", "knock", courage = 1, trophy = "Answered the Door"),
            Choice("Draw your sword.", "sword", courage = 1)
        )
    ),
    "road" to Scene(
        id = "road",
        title = "The Weight of a Name",
        text = "Kaelen takes ten steps. Behind him, the knock comes again. He remembers the men who died when he obeyed an order he should have questioned.",
        choices = listOf(
            Choice("Turn back.", "door", honour = 1),
            Choice("Leave the past behind.", "end", courage = 1)
        )
    ),
    "knock" to Scene(
        id = "knock",
        title = "Something Answers",
        text = "The door opens onto darkness lit by a distant blue fire. A voice whispers: 'The last one has returned.'",
        choices = listOf(
            Choice("Step through.", "end", courage = 1)
        )
    ),
    "sword" to Scene(
        id = "sword",
        title = "Steel and Silence",
        text = "Kaelen raises his blade. The darkness does not attack. Instead, the sword begins to hum with a warmth he has not felt since the day he was disgraced.",
        choices = listOf(
            Choice("Lower the sword.", "end", honour = 1),
            Choice("Enter with steel drawn.", "end", courage = 1)
        )
    ),
    "end" to Scene(
        id = "end",
        title = "Chapter One — The First Threshold",
        text = "The road behind Kaelen is no longer the same. Somewhere beyond the door, something ancient is waiting. The choice was his. The consequences will be too.",
        choices = listOf(
            Choice("Return to the beginning.", "prologue")
        )
    )
)
