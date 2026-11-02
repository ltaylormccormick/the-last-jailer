package com.thelastjailer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.thelastjailer.app.GameState
import com.thelastjailer.app.data.StoryRepository
import com.thelastjailer.app.ending.EndingTrophies

/**
 * What the Character screen shows of the nine ending trophies, kept free of Compose so plain JVM tests can cover every rule.
 *
 * The Endings section is **absent** until the player has completed the game, and from then on it shows **only what has been
 * earned**: never a locked name, a silhouette, a placeholder or a count of what remains. Nothing here reads or writes the
 * save; everything is derived from the [GameState] it is given.
 *
 * A save counts as completed when it holds any of the nine, or holds "The Watch Goes On" (earned by replaying a finished
 * game, including on a build that predates the ending trophies), or is standing on an ending right now. The last two cover
 * an older save that finished the game but has none of the nine yet; it still sees the section, with a short explanation
 * instead of rows, and gets its rows when another telling reaches an ending.
 */
internal object EndingsPanel {
    const val TITLE = "ENDINGS"
    const val EPIGRAPH = "Not every telling of this tale leaves the same mark."
    const val EMPTY_STATE = "Your earlier telling is remembered. New marks will appear when another telling reaches its end."
    const val COLLECTION_NOTE = "The original collection. Ending marks are kept in their own panel below."
    const val HINT_BUTTON = "Show a hint"
    const val HINT_BUTTON_AGAIN = "Show another hint"

    private const val WATCH_GOES_ON = "The Watch Goes On"

    class Entry(val id: String, val description: String)

    /** The nine, in the one fixed order they are ever shown: completion, the three legacies, the aid outcome, the fate outcome. */
    val catalogue: List<Entry> = listOf(
        Entry(EndingTrophies.TALE_TOLD, "You carried a telling to its end."),
        Entry(EndingTrophies.UNYIELDING, "Where the road offered a harder way, you took it."),
        Entry(EndingTrophies.HONOURBOUND, "You kept faith with what you owed, whatever it cost."),
        Entry(EndingTrophies.STEADFAST, "You stayed, and let others reach their own answers."),
        Entry(EndingTrophies.BORROWED_STRENGTH, "At Stonebeard's gate you opened your hand and took what was offered."),
        Entry(EndingTrophies.UNAIDED, "At Stonebeard's gate you kept your hand closed."),
        Entry(EndingTrophies.HER_CHAINS, "You bound her, and brought her back to face the Order."),
        Entry(EndingTrophies.HER_FREEDOM, "You let her go, on one condition."),
        Entry(EndingTrophies.HER_FATE_UNWRITTEN, "You took her weapon and left the rest for later.")
    )

    /**
     * The same three pointers for every player and every save. They name a chapter, never a choice or an outcome, and they do
     * not depend on what has or has not been earned, so pressing the button cannot reveal what is missing. Each chapter named
     * holds a decision every completion has already passed.
     */
    val hints: List<String> = listOf(
        "A turning point lies in Chapter ${romanNumeral(4)}.",
        "A turning point lies in Chapter ${romanNumeral(18)}.",
        "The closing chapters, ${romanNumeral(21)} to ${romanNumeral(30)}, leave their own mark."
    )

    fun hasCompleted(state: GameState): Boolean =
        EndingTrophies.all.any { it in state.trophies } ||
            WATCH_GOES_ON in state.trophies ||
            StoryRepository.nodeOrNull(state.sceneId)?.let { StoryRepository.isEnding(it) } == true

    /** Whether the Endings section is shown at all. */
    fun isVisible(state: GameState): Boolean = hasCompleted(state)

    /** The earned ending trophies, in the fixed order. Never an unearned one. */
    fun entries(state: GameState): List<Entry> = catalogue.filter { it.id in state.trophies }

    /** The explanation shown in place of rows: only when the section is visible and nothing has been recorded in it yet. */
    fun showEmptyState(state: GameState): Boolean = isVisible(state) && entries(state).isEmpty()

    /** The note under the original TROPHIES heading. Null while the Endings section is hidden, so it never hints that one exists. */
    fun collectionNote(state: GameState): String? = if (isVisible(state)) COLLECTION_NOTE else null

    /** The trophies for the original flat list: everything held except the nine, in the order the set gives them. */
    fun listedTrophies(trophies: Set<String>): List<String> = trophies.filterNot { it in EndingTrophies.all }

    /** The hint to show after [taps] presses of the button (1 is the first). Pure, so nothing about it needs saving. */
    fun hintFor(taps: Int): String? = if (taps < 1) null else hints[(taps - 1) % hints.size]

    /** Roman numerals for chapter numbers. */
    fun romanNumeral(n: Int): String {
        require(n in 1..39) { "chapter numbers are 1 to 39 here: $n" }
        var rest = n
        val out = StringBuilder()
        for ((value, symbol) in listOf(10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")) {
            while (rest >= value) { out.append(symbol); rest -= value }
        }
        return out.toString()
    }
}

/**
 * The Endings panel of the Character screen. Emits nothing before the first completion. The tap count behind the hint button
 * is plain `remember` state: it is not saved, it resets when the player leaves the tab, and this composable has no way to reach
 * the save at all.
 */
@Composable
fun EndingsSection(state: GameState, modifier: Modifier = Modifier) {
    if (!EndingsPanel.isVisible(state)) return
    val entries = EndingsPanel.entries(state)
    var taps by remember { mutableIntStateOf(0) }
    OrnatePanel(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                EndingsPanel.TITLE,
                style = MaterialTheme.typography.labelLarge,
                color = JailerColors.Gold,
                modifier = Modifier.semantics { heading() }
            )
            Text(EndingsPanel.EPIGRAPH, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            entries.forEach { entry ->
                Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                    Text(entry.id, style = MaterialTheme.typography.titleMedium)
                    Text(entry.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (EndingsPanel.showEmptyState(state)) {
                Text(EndingsPanel.EMPTY_STATE, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { taps += 1 }) {
                Text(if (taps == 0) EndingsPanel.HINT_BUTTON else EndingsPanel.HINT_BUTTON_AGAIN)
            }
            EndingsPanel.hintFor(taps)?.let { hint ->
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
        }
    }
}
