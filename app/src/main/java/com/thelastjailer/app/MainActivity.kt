package com.thelastjailer.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
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
    var state by remember {
        val slot = store.currentActiveSlot() ?: 1
        mutableStateOf(store.load(slot) ?: GameState(activeSlot = slot))
    }
    var screen by remember { mutableStateOf(AppScreen.STORY) }

    Scaffold(
        containerColor = JailerColors.Night,
        bottomBar = {
            NavigationBar(containerColor = JailerColors.Panel) {
                bottomNavScreens.forEach { s ->
                    NavigationBarItem(
                        selected = screen == s,
                        onClick = { screen = s },
                        icon = { Text(s.glyph) },
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
                    onChoiceSelected = { choice ->
                        state = state.applyChoice(choice)
                        store.save(state.activeSlot, state)
                    },
                    onOpenJournal = { screen = AppScreen.JOURNAL },
                    onOpenInventory = { screen = AppScreen.INVENTORY },
                    onOpenCharacter = { screen = AppScreen.CHARACTER },
                    onOpenMenu = { /* menu drawer is future work */ },
                    isExpandedWidth = isExpandedWidth
                )
                AppScreen.CHARACTER -> CharacterScreen(state)
                AppScreen.INVENTORY -> InventoryScreen(state)
                AppScreen.MAP -> MapScreen(entitlements)
                AppScreen.SAVE -> SaveScreen(
                    store = store,
                    entitlements = entitlements,
                    state = state,
                    onStateChange = { state = it }
                )
                AppScreen.OPTIONS -> OptionsScreen(entitlements)
                AppScreen.JOURNAL -> JournalScreen(state)
            }
        }
    }
}
