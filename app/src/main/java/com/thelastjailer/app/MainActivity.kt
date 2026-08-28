package com.thelastjailer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight

private data class Scene(val id: String, val title: String, val text: String, val choices: List<Choice>)
private data class Choice(val label: String, val next: String, val courage: Int = 0, val honour: Int = 0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { JailerApp() } }
}

@Composable
fun JailerApp() {
    val scenes = remember { demoScenes() }
    var sceneId by remember { mutableStateOf("prologue") }
    var courage by remember { mutableIntStateOf(0) }
    var honour by remember { mutableIntStateOf(0) }
    val scene = scenes.getValue(sceneId)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column { Text("THE LAST JAILER", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp)); Text(scene.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(20.dp)); Text(scene.text, style = MaterialTheme.typography.bodyLarge) }
                Column { Text("Courage $courage  •  Honour $honour", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(12.dp)); scene.choices.forEach { choice -> Button(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = { courage += choice.courage; honour += choice.honour; sceneId = choice.next }) { Text(choice.label) } } }
            }
        }
    }
}

private fun demoScenes() = mapOf(
    "prologue" to Scene("prologue", "The Road to Blackmere", "Three years ago, Kaelen Veyr wore the king's colours. Tonight he wears a broken sword and the shame of a knight who failed. At the edge of the old road, a black door stands where no door should be.", listOf(
        Choice("Approach the door.", "door", courage = 1), Choice("Keep walking. A disgraced knight has no business with it.", "road", honour = 1))),
    "door" to Scene("door", "The Door Without a Wall", "There is no wall. No hinges. No handle. Yet when Kaelen touches the iron, something on the other side knocks once.", listOf(
        Choice("Knock back.", "knock", courage = 1), Choice("Draw your sword.", "sword", courage = 1))),
    "road" to Scene("road", "The Weight of a Name", "Kaelen takes ten steps. Behind him, the knock comes again. He remembers the men who died when he obeyed an order he should have questioned.", listOf(
        Choice("Turn back.", "door", honour = 1), Choice("Leave the past behind.", "end", courage = 1))),
    "knock" to Scene("knock", "Something Answers", "The door opens onto darkness lit by a distant blue fire. A voice whispers: 'The last one has returned.'", listOf(Choice("Step through.", "end", courage = 1))),
    "sword" to Scene("sword", "Steel and Silence", "Kaelen raises his blade. The darkness does not attack. Instead, the sword begins to hum with a warmth he has not felt since the day he was disgraced.", listOf(Choice("Lower the sword.", "end", honour = 1), Choice("Enter with steel drawn.", "end", courage = 1))),
    "end" to Scene("end", "Chapter One — The First Threshold", "The road behind Kaelen is no longer the same. Somewhere beyond the door, something ancient is waiting. The choice was his. The consequences will be too.", listOf(Choice("Continue — Chapter One complete.", "prologue", honour = 0)))
)
