package com.earlthekaiju.coach.feature.settings

import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.earlthekaiju.coach.data.db.ProgramEntity
import com.earlthekaiju.coach.data.repo.ProgramDraft
import com.earlthekaiju.coach.designsystem.component.ButtonVariant
import com.earlthekaiju.coach.designsystem.component.ConfirmDialog
import com.earlthekaiju.coach.designsystem.component.EmptyState
import com.earlthekaiju.coach.designsystem.component.HairlineDivider
import com.earlthekaiju.coach.designsystem.component.KaijuBottomSheet
import com.earlthekaiju.coach.designsystem.component.KaijuButton
import com.earlthekaiju.coach.designsystem.component.KaijuChip
import com.earlthekaiju.coach.designsystem.component.KaijuTextField
import com.earlthekaiju.coach.designsystem.component.KaijuTopBar
import com.earlthekaiju.coach.designsystem.component.StateAction
import com.earlthekaiju.coach.designsystem.component.programColorValue
import com.earlthekaiju.coach.designsystem.component.readableWidth
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace
import com.earlthekaiju.coach.di.AppContainer
import com.earlthekaiju.coach.di.kaijuViewModel
import com.earlthekaiju.coach.domain.model.Audience
import com.earlthekaiju.coach.domain.model.ProgramColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgramsViewModel(private val c: AppContainer) : ViewModel() {
    val programs: StateFlow<List<ProgramEntity>?> = c.programs.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(draft: ProgramDraft) = viewModelScope.launch { c.programs.save(draft) }
    fun delete(id: String) = viewModelScope.launch { c.programs.delete(id) }
}

@Composable
fun ProgramsDestination(onBack: () -> Unit) {
    val vm = kaijuViewModel { c, _ -> ProgramsViewModel(c) }
    val programs by vm.programs.collectAsStateWithLifecycle()
    ProgramsScreen(programs.orEmpty(), onBack, { vm.save(it) }, { vm.delete(it) })
}

@Composable
fun ProgramsScreen(programs: List<ProgramEntity>, onBack: () -> Unit, onSave: (ProgramDraft) -> Unit, onDelete: (String) -> Unit) {
    val colors = Kaiju.colors
    var editing by remember { mutableStateOf<ProgramDraft?>(null) }
    var deleting by remember { mutableStateOf<ProgramEntity?>(null) }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        KaijuTopBar(title = "Programs", eyebrow = "Settings", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = KaijuSpace.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.readableWidth().padding(top = KaijuSpace.m)) {
                if (programs.isEmpty()) {
                    EmptyState(
                        eyebrow = "Programs",
                        title = "No programs yet",
                        body = "A program is a group you teach regularly. Students enroll in programs, and classes use them to know who to expect.",
                        primary = StateAction("Add program") { editing = ProgramDraft() },
                    )
                }
                programs.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable(role = Role.Button, onClickLabel = "Edit program") {
                                editing = ProgramDraft(p.id, p.name, p.audience, p.color, p.notes)
                            }
                            .padding(vertical = KaijuSpace.s),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(width = 4.dp, height = 36.dp).background(programColorValue(p.color)))
                        Spacer(Modifier.width(KaijuSpace.m))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = Kaiju.type.titleSmall, color = colors.ink)
                            Text(p.audience.label, style = Kaiju.type.bodyS, color = colors.inkMuted)
                        }
                        Icon(KaijuIcons.Edit, null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                    }
                    HairlineDivider(Modifier.fillMaxWidth())
                }
                if (programs.isNotEmpty()) {
                    Spacer(Modifier.height(KaijuSpace.l))
                    KaijuButton("Add program", { editing = ProgramDraft() }, Modifier.fillMaxWidth(), icon = KaijuIcons.Add)
                }
                Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(KaijuSpace.xl))
            }
        }
    }
    editing?.let { draft ->
        ProgramSheet(
            initial = draft,
            onSave = { onSave(it); editing = null },
            onDelete = draft.id?.let { id -> { editing = null; deleting = programs.firstOrNull { it.id == id } } },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { p ->
        ConfirmDialog(
            title = "Delete ${p.name}?",
            body = "Students lose this enrollment and scheduled classes lose their program. Past attendance stays.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { onDelete(p.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ProgramSheet(initial: ProgramDraft, onSave: (ProgramDraft) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var d by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    KaijuBottomSheet(onDismiss = onDismiss, title = if (initial.id == null) "Add program" else "Edit program", eyebrow = "Programs") {
        KaijuTextField(
            d.name, { d = d.copy(name = it); error = null }, "Name",
            placeholder = "e.g. Adult Fundamentals",
            errorText = error,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        )
        Spacer(Modifier.height(KaijuSpace.m))
        Text("Audience", style = Kaiju.type.label, color = Kaiju.colors.ink)
        Spacer(Modifier.height(KaijuSpace.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            Audience.entries.forEach { a -> KaijuChip(a.label, d.audience == a, { d = d.copy(audience = a) }) }
        }
        Spacer(Modifier.height(KaijuSpace.m))
        Text("Planner accent", style = Kaiju.type.label, color = Kaiju.colors.ink)
        Spacer(Modifier.height(KaijuSpace.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.xs)) {
            ProgramColor.entries.forEach { c -> KaijuChip(c.label, d.color == c, { d = d.copy(color = c) }) }
        }
        Spacer(Modifier.height(KaijuSpace.l))
        KaijuButton("Save program", {
            if (d.name.isBlank()) error = "Name the program." else onSave(d)
        }, Modifier.fillMaxWidth(), icon = KaijuIcons.Check)
        if (onDelete != null) {
            Spacer(Modifier.height(KaijuSpace.s))
            KaijuButton("Delete program", onDelete, Modifier.fillMaxWidth(), variant = ButtonVariant.Danger, icon = KaijuIcons.Delete)
        }
    }
}
