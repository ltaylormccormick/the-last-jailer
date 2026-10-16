package com.thelastjailer.app

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.thelastjailer.app.checkpoint.ArrivalRecorder
import com.thelastjailer.app.checkpoint.CheckpointStore
import com.thelastjailer.app.checkpoint.PrefsCheckpointStorage
import com.thelastjailer.app.data.BillingRepository
import com.thelastjailer.app.data.LocalEntitlementRepository
import com.thelastjailer.app.ui.AppScreen
import com.thelastjailer.app.ui.CharacterScreen
import com.thelastjailer.app.ui.InventoryScreen
import com.thelastjailer.app.ui.JailerColors
import com.thelastjailer.app.ui.JailerTheme
import com.thelastjailer.app.ui.JournalScreen
import com.thelastjailer.app.ui.MapScreen
import com.thelastjailer.app.ui.OptionsScreen
import com.thelastjailer.app.ui.SaveScreen
import com.thelastjailer.app.ui.StoryScreen
import com.thelastjailer.app.ui.bottomNavScreens
import com.thelastjailer.app.ui.isNavigationEnabled

/**
 * Kill switch for chapter-arrival recording. Off: no checkpoint file is read or written, but the slot's
 * ledger is still maintained and invalidated (a stale ledger must never outlive the state it described).
 */
private const val ARRIVAL_RECORDING_ENABLED = true

/**
 * The one app-level [ArrivalRecorder] (and so the one [CheckpointStore]) for the process. Its memory scope
 * is the **application** context, which never changes while the process lives, so the per-file locks and
 * the "untrusted" flags survive Activity recreation. Construction failure yields null: gameplay and the
 * ordinary save are unaffected, and every slot's timeline is simply invalidated by the default save.
 */
private object ArrivalRecorderHolder {
    private var recorder: ArrivalRecorder? = null
    private var built = false

    @Synchronized
    fun get(context: Context): ArrivalRecorder? {
        if (!built) {
            built = true
            recorder = try {
                val app = context.applicationContext
                val storage = PrefsCheckpointStorage(app) { name -> app.getSharedPreferences(name, Context.MODE_PRIVATE) }
                ArrivalRecorder(
                    store = CheckpointStore(storage),
                    recordingEnabled = ARRIVAL_RECORDING_ENABLED,
                    log = { message, error -> android.util.Log.w("ArrivalRecorder", message, error) }
                )
            } catch (e: Exception) {
                android.util.Log.w("ArrivalRecorder", "checkpoint recording unavailable", e)
                null
            }
        }
        return recorder
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
            JailerTheme {
                JailerApp(isExpandedWidth = isExpandedWidth)
            }
        }
    }
}

@Composable
fun JailerApp(isExpandedWidth: Boolean = false) {
    val context = LocalContext.current
    val store = remember(context) {
        SaveStore(context.getSharedPreferences("jailer_saves", Context.MODE_PRIVATE))
    }
    val entitlements = remember(context) {
        LocalEntitlementRepository(context.getSharedPreferences("jailer_entitlements", Context.MODE_PRIVATE))
    }
    val recorder = remember(context) { ArrivalRecorderHolder.get(context) }
    val activity = context as Activity
    val billing = remember(context) { BillingRepository(context, entitlements) }
    DisposableEffect(billing) {
        billing.start()
        onDispose { billing.endConnection() }
    }
    val onRequestUnlock: () -> Unit = { billing.launchPurchaseFlow(activity) }
    var state by remember {
        val slot = store.currentActiveSlot() ?: 1
        mutableStateOf(store.load(slot) ?: GameState(activeSlot = slot))
    }
    var screen by remember { mutableStateOf(AppScreen.STORY) }
    var combatActive by remember { mutableStateOf(false) }
    var openShop by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = JailerColors.Night,
        bottomBar = {
            NavigationBar(containerColor = JailerColors.Panel) {
                bottomNavScreens.forEach { s ->
                    NavigationBarItem(
                        selected = screen == s,
                        enabled = isNavigationEnabled(s, combatActive),
                        onClick = { openShop = false; screen = s },
                        icon = {
                            when (s) {
                                AppScreen.STORY -> Icon(Icons.Filled.AutoStories, contentDescription = s.label, tint = JailerColors.Gold)
                                AppScreen.CHARACTER -> Icon(Icons.Filled.Shield, contentDescription = s.label, tint = JailerColors.Gold)
                                AppScreen.INVENTORY -> Icon(Icons.Filled.Backpack, contentDescription = s.label, tint = JailerColors.Gold)
                                AppScreen.MAP -> Icon(Icons.Filled.Map, contentDescription = s.label, tint = JailerColors.Gold)
                                AppScreen.SAVE -> Icon(Icons.Filled.Save, contentDescription = s.label, tint = JailerColors.Gold)
                                AppScreen.OPTIONS -> Icon(Icons.Filled.Settings, contentDescription = s.label, tint = JailerColors.Gold)
                                else -> Text(s.glyph)
                            }
                        },
                        label = { Text(s.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(JailerColors.NightGradientTop, JailerColors.Night)))
        ) {
            when (screen) {
                AppScreen.STORY -> StoryScreen(
                    state = state,
                    entitlements = entitlements,
                    purchaseCompletedTick = billing.purchaseCompletedTick,
                    onRequestUnlock = onRequestUnlock,
                    onRestorePurchase = { billing.restorePastPurchases(showFeedback = true) },
                    onChoiceSelected = { choice ->
                        // Keeps one exact pre-choice snapshot so "go back one step" restores stats,
                        // flags and items as well as the previous scene (see StateCommits.kt).
                        state = store.commitChoice(state, choice, recorder)
                    },
                    onCombatResolved = { encounter, outcome ->
                        // Combat is deliberately irreversible through the one-step story back action.
                        state = store.commitCombatResult(state, encounter, outcome, recorder)
                    },
                    onOpenJournal = { screen = AppScreen.JOURNAL },
                    onOpenInventory = { openShop = false; screen = AppScreen.INVENTORY },
                    onOpenShop = { if (!combatActive) { openShop = true; screen = AppScreen.INVENTORY } },
                    onCombatActiveChanged = { combatActive = it },
                    onOpenCharacter = { screen = AppScreen.CHARACTER },
                    isExpandedWidth = isExpandedWidth
                )
                AppScreen.CHARACTER -> CharacterScreen(state)
                AppScreen.INVENTORY -> InventoryScreen(
                    state = state,
                    openShop = openShop,
                    onPurchase = { itemId, price ->
                        // Clears the story undo so it cannot refund/duplicate a shop transaction.
                        state = store.commitPurchase(state, itemId, price, recorder)
                    }
                )
                AppScreen.MAP -> MapScreen(entitlements)
                AppScreen.SAVE -> SaveScreen(
                    store = store,
                    entitlements = entitlements,
                    state = state,
                    onStateChange = { state = it },
                    recorder = recorder
                )
                AppScreen.OPTIONS -> OptionsScreen(
                    entitlements = entitlements,
                    purchaseCompletedTick = billing.purchaseCompletedTick,
                    onRequestUnlock = onRequestUnlock
                )
                AppScreen.JOURNAL -> JournalScreen(state)
            }
        }
    }
}
