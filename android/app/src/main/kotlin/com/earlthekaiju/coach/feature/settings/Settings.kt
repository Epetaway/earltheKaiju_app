package com.earlthekaiju.coach.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.earlthekaiju.coach.BuildConfig
import com.earlthekaiju.coach.R
import com.earlthekaiju.coach.data.prefs.AppSettings
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ConfirmDialog
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuListRow
import com.earlthekaiju.coach.designsystem.component.KaijuSwitchRow
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.SyncStatusPanel
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.LocalAppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.ui.Format
import com.earlthekaiju.coach.ui.LocalSnackbarHost
import com.earlthekaiju.coach.ui.LocalUse24Hour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val programCount: Int = 0,
    val versionName: String = BuildConfig.VERSION_NAME,
    val showDebugTools: Boolean = BuildConfig.DEBUG,
)

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<SettingsUiState> = combine(c.settings.settings, c.programs.observeAll()) { s, p ->
        SettingsUiState(settings = s, programCount = p.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setSound(v: Boolean) = viewModelScope.launch { c.settings.setTimerSound(v) }
    fun setVibration(v: Boolean) = viewModelScope.launch { c.settings.setTimerVibration(v) }
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { c.settings.setKeepScreenOn(v) }
    fun loadSample(done: () -> Unit) = viewModelScope.launch { c.sampleData.load(); done() }
}

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onSound: (Boolean) -> Unit = {},
    val onVibration: (Boolean) -> Unit = {},
    val onKeepScreenOn: (Boolean) -> Unit = {},
    val onExport: () -> Unit = {},
    val onPrograms: () -> Unit = {},
    val onLoadSample: () -> Unit = {},
)

@Composable
fun SettingsDestination(onBack: () -> Unit, onPrograms: () -> Unit) {
    val vm = kaijuViewModel { c, _ -> SettingsViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { container.backup.writeTo(it) }
                        ?: error("Couldn't open the file")
                }
            }
            snackbar.showSnackbar(
                result.fold(
                    onSuccess = { "Backup saved · ${it.records} records" },
                    onFailure = { "Backup failed: ${it.message ?: "unknown error"}" },
                ),
            )
        }
    }
    SettingsScreen(
        state,
        SettingsActions(
            onBack = onBack,
            onSound = { vm.setSound(it) },
            onVibration = { vm.setVibration(it) },
            onKeepScreenOn = { vm.setKeepScreenOn(it) },
            onExport = { exportLauncher.launch("kaiju-coach-backup-${container.clock.today()}.json") },
            onPrograms = onPrograms,
            onLoadSample = { vm.loadSample { scope.launch { snackbar.showSnackbar("Sample data loaded") } } },
        ),
    )
}

@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions) {
    val colors = Kaiju.colors
    val use24h = LocalUse24Hour.current
    var confirmExport by remember { mutableStateOf(false) }
    val lastBackup = state.settings.lastBackupAt?.let {
        val dt = Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault())
        "${Format.shortDate(dt.toLocalDate())}, ${Format.time(dt.toLocalTime(), use24h)}"
    }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        KaijuTopBar(title = "Settings", eyebrow = "Kaiju Coach", onBack = actions.onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KaijuSpace.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.readableWidth()) {
                SectionHeader("Data & sync", index = 1, modifier = Modifier.padding(top = KaijuSpace.m))
                Spacer(Modifier.height(KaijuSpace.s))
                SyncStatusPanel(lastBackupLabel = lastBackup)
                Spacer(Modifier.height(KaijuSpace.s))
                KaijuButton("Export backup", { confirmExport = true }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = KaijuIcons.Export)
                Text(
                    "Saves everything to a JSON file you choose. Keep it somewhere private: it contains student names.",
                    style = Kaiju.type.caption,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(top = KaijuSpace.xxs),
                )

                SectionHeader("Timer", index = 2, modifier = Modifier.padding(top = KaijuSpace.xl))
                KaijuSwitchRow("Round sounds", state.settings.timerSound, actions.onSound, subtitle = "Bells and the 3-2-1 count-in, on the media volume", icon = KaijuIcons.SoundOn)
                HairlineDivider(Modifier.fillMaxWidth())
                KaijuSwitchRow("Vibration", state.settings.timerVibration, actions.onVibration, subtitle = "Buzz at round changes", icon = KaijuIcons.Vibrate)
                HairlineDivider(Modifier.fillMaxWidth())
                KaijuSwitchRow(
                    "Keep screen on",
                    state.settings.keepScreenOn,
                    actions.onKeepScreenOn,
                    subtitle = "While a timer runs and in Class Mode",
                    icon = KaijuIcons.Phone,
                )

                SectionHeader("Teaching", index = 3, modifier = Modifier.padding(top = KaijuSpace.xl))
                KaijuListRow(
                    "Programs",
                    subtitle = "Groups like Adult Fundamentals or Kids 6–12. They decide who's expected in class.",
                    icon = KaijuIcons.Program,
                    value = state.programCount.toString(),
                    onClick = actions.onPrograms,
                )

                SectionHeader("About", index = 4, modifier = Modifier.padding(top = KaijuSpace.xl))
                Spacer(Modifier.height(KaijuSpace.s))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.brand_crest), contentDescription = "Earl the Kaiju crest", modifier = Modifier.size(width = 82.dp, height = 64.dp))
                    Spacer(Modifier.width(KaijuSpace.m))
                    Column {
                        Text("KAIJU COACH", style = Kaiju.type.title, color = colors.ink)
                        Eyebrow("Earl the Kaiju · v${state.versionName}")
                    }
                }
                Spacer(Modifier.height(KaijuSpace.m))
                Text(
                    "A private teaching tool. Everything stays on this phone; nothing is sent anywhere. Android's cloud backup is turned off for this app.",
                    style = Kaiju.type.bodyS,
                    color = colors.inkMuted,
                )
                Spacer(Modifier.height(KaijuSpace.s))
                Text(
                    "Fonts: Barlow Condensed and Manrope, SIL Open Font License 1.1. Crest and brand marks © Earl Hickson Jr.",
                    style = Kaiju.type.caption,
                    color = colors.inkMuted,
                )
                Text("Theme: Shadow (the only theme in this version).", style = Kaiju.type.caption, color = colors.inkMuted, modifier = Modifier.padding(top = KaijuSpace.xxs))

                if (state.showDebugTools) {
                    SectionHeader("Debug build", index = 5, modifier = Modifier.padding(top = KaijuSpace.xl))
                    Spacer(Modifier.height(KaijuSpace.s))
                    KaijuButton("Load sample data", actions.onLoadSample, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
                    Text(
                        "Adds fictional programs, students, lessons and this week's classes for trying the app. Debug builds only.",
                        style = Kaiju.type.caption,
                        color = colors.inkMuted,
                        modifier = Modifier.padding(top = KaijuSpace.xxs),
                    )
                }
                Spacer(Modifier.height(KaijuSpace.xxl))
            }
        }
    }
    if (confirmExport) {
        ConfirmDialog(
            title = "Export a backup?",
            body = "The file includes student names, ranks, attendance and your notes. Save it somewhere only you can open.",
            confirmLabel = "Choose location",
            onConfirm = { confirmExport = false; actions.onExport() },
            onDismiss = { confirmExport = false },
        )
    }
}
