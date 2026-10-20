package com.earlthekaiju.coach.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.R
import com.earlthekaiju.coach.designsystem.component.AttendanceRow
import com.earlthekaiju.coach.designsystem.component.BeltBar
import com.earlthekaiju.coach.designsystem.component.ButtonSize
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ClassCard
import com.earlthekaiju.coach.designsystem.component.ConceptBanner
import com.earlthekaiju.coach.designsystem.component.DiamondDivider
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.ErrorState
import com.earlthekaiju.coach.designsystem.component.Eyebrow
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuIconButton
import com.earlthekaiju.coach.designsystem.component.KaijuSwitchRow
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KeystoneRule
import com.earlthekaiju.coach.designsystem.component.LessonSegmentRow
import com.earlthekaiju.coach.designsystem.component.LoadingState
import com.earlthekaiju.coach.designsystem.component.NumberStepper
import com.earlthekaiju.coach.designsystem.component.PickerField
import com.earlthekaiju.coach.designsystem.component.SectionHeader
import com.earlthekaiju.coach.designsystem.component.SegmentProgressBar
import com.earlthekaiju.coach.designsystem.component.SegmentState
import com.earlthekaiju.coach.designsystem.component.SegmentedControl
import com.earlthekaiju.coach.designsystem.component.SessionStatusBadge
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.StudentRow
import com.earlthekaiju.coach.designsystem.component.SyncStatusPanel
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuPalette
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.domain.model.AttendanceStatus
import com.earlthekaiju.coach.domain.model.Belt
import com.earlthekaiju.coach.domain.model.SessionStatus
import com.earlthekaiju.coach.ui.toCardModel

/** Design-system reference sheets rendered from the real components (not mockups). */
@Composable
fun FoundationsSheet() {
    val colors = Kaiju.colors
    Column(Modifier.background(colors.background).padding(KaijuSpace.gutter)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.brand_crest), null, Modifier.size(width = 82.dp, height = 64.dp))
            Spacer(Modifier.width(KaijuSpace.m))
            Column {
                Eyebrow("Design system · Sprint 01")
                Text("KAIJU COACH", style = Kaiju.type.displayM, color = colors.ink)
            }
        }
        Spacer(Modifier.height(KaijuSpace.s))
        KeystoneRule()
        SectionHeader("Color roles", index = 1, modifier = Modifier.padding(top = KaijuSpace.l))
        val swatches = listOf(
            "Shadow · background" to KaijuPalette.Shadow,
            "Raised · surface" to KaijuPalette.ShadowRaised,
            "Facet · inset" to KaijuPalette.Facet,
            "Emerald · action" to KaijuPalette.Emerald,
            "Deep Emerald · pressed, work phase" to KaijuPalette.EmeraldDeep,
            "Accent · links, active (6.3:1)" to KaijuPalette.Accent,
            "Spectral · live, focus (9.7:1)" to KaijuPalette.SpectralEmerald,
            "Gold · eyebrows, rank (9.2:1)" to KaijuPalette.Gold,
            "Ivory · text (17.1:1)" to KaijuPalette.Ivory,
            "Muted · secondary (8.6:1)" to KaijuPalette.InkMuted,
            "Signal · errors (6.9:1)" to KaijuPalette.Signal,
        )
        swatches.forEach { (label, c) ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 56.dp, height = 28.dp).background(c).border(1.dp, colors.line))
                Spacer(Modifier.width(KaijuSpace.s))
                Text(label, style = Kaiju.type.bodyS, color = colors.ink)
            }
        }
        SectionHeader("Type", index = 2, modifier = Modifier.padding(top = KaijuSpace.l))
        Text("DISPLAY XL", style = Kaiju.type.displayXL, color = colors.ink)
        Text("DISPLAY L · 64", style = Kaiju.type.displayL, color = colors.ink)
        Text("Display M · 40", style = Kaiju.type.displayM, color = colors.ink)
        Text("Heading · 28", style = Kaiju.type.heading, color = colors.ink)
        Text("TITLE · BARLOW 22", style = Kaiju.type.title, color = colors.ink)
        Eyebrow("Eyebrow · 13 / +0.22em")
        Text("Body L · Manrope 18 — I teach the details that hold up under pressure.", style = Kaiju.type.bodyL, color = colors.ink)
        Text("Body · Manrope 16 — readable mid-class.", style = Kaiju.type.body, color = colors.ink)
        Text("Label · 14 semibold", style = Kaiju.type.label, color = colors.inkMuted)
        Text("05:00  12:34  1:05:59", style = Kaiju.type.numeralL, color = colors.ink)
        SectionHeader("Shapes & graphics", index = 3, modifier = Modifier.padding(top = KaijuSpace.l))
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).background(colors.surface, KaijuShapes.Card).border(1.dp, colors.line, KaijuShapes.Card))
            Box(Modifier.size(64.dp).background(colors.action, KaijuShapes.ActionHero))
            Box(Modifier.size(64.dp).background(colors.surfaceInset, KaijuShapes.Sheet))
            Image(painterResource(R.drawable.brand_crest_simplified), null, Modifier.size(width = 61.dp, height = 48.dp))
            Image(painterResource(R.drawable.brand_eyes_mark), null, Modifier.size(width = 30.dp, height = 24.dp))
        }
        Spacer(Modifier.height(KaijuSpace.s))
        DiamondDivider()
        SectionHeader("Icons", index = 4, modifier = Modifier.padding(top = KaijuSpace.l))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s), verticalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            listOf(
                KaijuIcons.Today, KaijuIcons.Planner, KaijuIcons.Timer, KaijuIcons.Students, KaijuIcons.Library,
                KaijuIcons.Lessons, KaijuIcons.Technique, KaijuIcons.Attendance, KaijuIcons.Play, KaijuIcons.Pause,
                KaijuIcons.SkipNext, KaijuIcons.Reset, KaijuIcons.Note, KaijuIcons.Flag, KaijuIcons.Sync,
                KaijuIcons.Export, KaijuIcons.Program, KaijuIcons.Shield, KaijuIcons.Warning, KaijuIcons.Settings,
            ).forEach { Icon(it, null, tint = colors.ink, modifier = Modifier.size(28.dp)) }
        }
        SectionHeader("Rank", index = 5, modifier = Modifier.padding(top = KaijuSpace.l))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s), verticalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            listOf(Belt.WHITE to 4, Belt.BLUE to 2, Belt.PURPLE to 1, Belt.BROWN to 3, Belt.BLACK to 2, Belt.GREY_WHITE to 1, Belt.YELLOW_BLACK to 0, Belt.GREEN to 3)
                .forEach { (b, s) -> BeltBar(b, s, width = 72.dp, height = 16.dp) }
        }
        Spacer(Modifier.height(KaijuSpace.l))
    }
}

@Composable
fun ComponentsSheet() {
    val colors = Kaiju.colors
    Column(Modifier.background(colors.background).padding(KaijuSpace.gutter), verticalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
        SectionHeader("Buttons", index = 1)
        KaijuButton("Start class", {}, Modifier.fillMaxWidth(), size = ButtonSize.Hero, icon = KaijuIcons.Play)
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s)) {
            KaijuButton("Primary", {}, Modifier.weight(1f))
            KaijuButton("Secondary", {}, Modifier.weight(1f), variant = ButtonVariant.Secondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s), verticalAlignment = Alignment.CenterVertically) {
            KaijuButton("Danger", {}, variant = ButtonVariant.Danger, icon = KaijuIcons.Delete)
            KaijuButton("Ghost link", {}, variant = ButtonVariant.Ghost, size = ButtonSize.Compact)
            KaijuButton("Disabled", {}, enabled = false, size = ButtonSize.Compact)
            KaijuIconButton(KaijuIcons.Add, "Add", {}, filled = true)
        }
        SectionHeader("Status", index = 2, modifier = Modifier.padding(top = KaijuSpace.s))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs), verticalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            SessionStatus.entries.forEach { SessionStatusBadge(it) }
        }
        SegmentProgressBar(listOf(10, 20, 15, 15), currentIndex = 1, currentFraction = 0.4f)
        SectionHeader("Inputs", index = 3, modifier = Modifier.padding(top = KaijuSpace.s))
        KaijuTextField("Guard Retention", {}, "Lesson title")
        KaijuTextField("", {}, "Class name", placeholder = "e.g. Fundamentals", errorText = "Give the lesson a title.")
        PickerField("Start", "6:30 PM", {}, icon = KaijuIcons.Clock)
        NumberStepper("Length", 60, {}, unitLabel = "minutes", step = 5)
        SegmentedControl(listOf("Schedule", "Lessons"), 0, {})
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            KaijuChip("Warm-up", true, {})
            KaijuChip("Drilling", false, {})
            KaijuChip("Sparring", false, {})
        }
        KaijuSwitchRow("Round sounds", true, {}, subtitle = "Bells and the 3-2-1 count-in", icon = KaijuIcons.SoundOn)
        SectionHeader("Rows & cards", index = 4, modifier = Modifier.padding(top = KaijuSpace.s))
        ClassCard(Fx.openMat.toCardModel(false), {})
        LessonSegmentRow(2, "Guard retention concepts", 20, kindLabel = "Instruction", techniques = listOf("Frames and hip escape", "Knee shield"), state = SegmentState.Current)
        StudentRow("Ana Ribeiro", Belt.BLUE, 2, "Adult Fundamentals · 9 classes in 30 days", {}, horizontalPadding = 0.dp)
        AttendanceRow("Marcus Hale", Belt.WHITE, 3, AttendanceStatus.PRESENT, {})
        SectionHeader("States", index = 5, modifier = Modifier.padding(top = KaijuSpace.s))
        LoadingState("Loading roster")
        ErrorState("Backup failed", "The file couldn't be written. Try another location.", retry = StateAction("Try again") {})
        ConceptBanner("Concept · planned", "Previews a later feature; nothing here is working software yet.")
        SyncStatusPanel(lastBackupLabel = null)
        EmptyState("No class on the schedule", "Schedule your next class.", eyebrow = "Nothing scheduled", primary = StateAction("Schedule class") {})
        Spacer(Modifier.height(KaijuSpace.l))
    }
}
