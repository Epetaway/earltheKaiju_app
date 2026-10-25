package com.earlthekaiju.coach.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.earlthekaiju.coach.R
import com.earlthekaiju.coach.designsystem.icon.KaijuIcons
import com.earlthekaiju.coach.designsystem.theme.Kaiju
import com.earlthekaiju.coach.designsystem.theme.KaijuShapes
import com.earlthekaiju.coach.designsystem.theme.KaijuSpace

data class StateAction(val label: String, val onClick: () -> Unit)

/**
 * Empty state: a quiet gold frame from the brand kit, a plain-language title, one line of help,
 * and the next useful action. No mascot, no illustration filler.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    primary: StateAction? = null,
    secondary: StateAction? = null,
    showFrame: Boolean = true,
) {
    val colors = Kaiju.colors
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, colors.line, KaijuShapes.CardFeature)
            .padding(KaijuSpace.l),
        horizontalAlignment = Alignment.Start,
    ) {
        if (showFrame) {
            Image(
                painterResource(R.drawable.brand_frame_triangle),
                contentDescription = null,
                modifier = Modifier.size(width = 56.dp, height = 44.dp),
            )
            Spacer(Modifier.height(KaijuSpace.m))
        }
        if (eyebrow != null) {
            Eyebrow(eyebrow)
            Spacer(Modifier.height(KaijuSpace.xs))
        }
        Text(title, style = Kaiju.type.heading, color = colors.ink, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(KaijuSpace.xs))
        Text(body, style = Kaiju.type.body, color = colors.inkMuted)
        if (primary != null || secondary != null) {
            Spacer(Modifier.height(KaijuSpace.l))
            Column(verticalArrangement = Arrangement.spacedBy(KaijuSpace.s), modifier = Modifier.fillMaxWidth()) {
                primary?.let { KaijuButton(it.label, it.onClick, Modifier.fillMaxWidth(), icon = KaijuIcons.Add) }
                secondary?.let { KaijuButton(it.label, it.onClick, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary) }
            }
        }
    }
}

@Composable
fun LoadingState(label: String, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = KaijuSpace.xl)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Eyebrow(label, color = colors.inkMuted)
        Spacer(Modifier.height(KaijuSpace.s))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = colors.accent,
            trackColor = colors.surfaceInset,
        )
    }
}

@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    retry: StateAction? = null,
) {
    val colors = Kaiju.colors
    Column(
        modifier
            .fillMaxWidth()
            .border(1.dp, colors.signal, KaijuShapes.Card)
            .padding(KaijuSpace.l)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(KaijuIcons.Warning, null, tint = colors.signal, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(KaijuSpace.s))
            Text(title, style = Kaiju.type.titleSmall, color = colors.ink)
        }
        Spacer(Modifier.height(KaijuSpace.xs))
        Text(message, style = Kaiju.type.bodyS, color = colors.inkMuted)
        if (retry != null) {
            Spacer(Modifier.height(KaijuSpace.m))
            KaijuButton(retry.label, retry.onClick, variant = ButtonVariant.Secondary, size = ButtonSize.Compact)
        }
    }
}

/**
 * Marks a screen region as a design concept, not working software (e.g. the media library in
 * Sprint 01), so previews are never mistaken for finished functionality.
 */
@Composable
fun ConceptBanner(title: String, body: String, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.surfaceInset, KaijuShapes.Small)
            .border(1.dp, colors.gold, KaijuShapes.Small)
            .padding(KaijuSpace.m),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(KaijuIcons.Info, null, tint = colors.gold, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(KaijuSpace.s))
        Column {
            Eyebrow(title)
            Spacer(Modifier.height(KaijuSpace.xxs))
            Text(body, style = Kaiju.type.bodyS, color = colors.ink)
        }
    }
}

/** Confirmation for destructive or hard-to-undo actions. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String = "Cancel",
) {
    val colors = Kaiju.colors
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surface, KaijuShapes.Dialog)
                .border(1.dp, colors.lineStrong, KaijuShapes.Dialog)
                .padding(KaijuSpace.l)
                .semantics { paneTitle = title },
        ) {
            Text(title, style = Kaiju.type.heading, color = colors.ink, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(KaijuSpace.s))
            Text(body, style = Kaiju.type.body, color = colors.inkMuted)
            Spacer(Modifier.height(KaijuSpace.l))
            Row(horizontalArrangement = Arrangement.spacedBy(KaijuSpace.s), modifier = Modifier.fillMaxWidth()) {
                KaijuButton(dismissLabel, onDismiss, Modifier.weight(1f), variant = ButtonVariant.Secondary)
                KaijuButton(
                    confirmLabel,
                    onConfirm,
                    Modifier.weight(1f),
                    variant = if (destructive) ButtonVariant.Danger else ButtonVariant.Primary,
                )
            }
        }
    }
}

/** Modal bottom sheet with the brand's single-chamfer top and a gold handle. */
@Composable
fun KaijuBottomSheet(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    eyebrow: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Kaiju.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = KaijuShapes.Sheet,
        containerColor = colors.surface,
        contentColor = colors.ink,
        scrimColor = colors.scrim,
        // The gold bar sits in a labeled 64×48dp target: the sheet wraps it with expand/dismiss
        // actions for screen readers, and the device accessibility checks require a label and size.
        dragHandle = {
            Box(
                Modifier.size(width = 64.dp, height = 48.dp).semantics { contentDescription = "Drag handle" },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(width = 36.dp, height = 3.dp).background(colors.gold))
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KaijuSpace.gutter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = KaijuSpace.l)
                .semantics { paneTitle = title },
        ) {
            if (eyebrow != null) Eyebrow(eyebrow)
            Text(title, style = Kaiju.type.heading, color = colors.ink, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(KaijuSpace.m))
            content()
        }
    }
}

/** Snackbar in brand style: Facet, hairline, Ivory text, accent action (e.g. Undo). */
@Composable
fun KaijuSnackbar(data: SnackbarData, modifier: Modifier = Modifier) {
    val colors = Kaiju.colors
    Row(
        modifier
            .padding(horizontal = KaijuSpace.m, vertical = KaijuSpace.xs)
            .fillMaxWidth()
            .background(colors.surfaceInset, KaijuShapes.Small)
            .border(1.dp, colors.lineStrong, KaijuShapes.Small)
            .padding(start = KaijuSpace.m, end = KaijuSpace.xxs, top = KaijuSpace.xxs, bottom = KaijuSpace.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            data.visuals.message,
            style = Kaiju.type.bodyS,
            color = colors.ink,
            modifier = Modifier.weight(1f).padding(vertical = KaijuSpace.s),
        )
        data.visuals.actionLabel?.let { label ->
            KaijuButton(label, onClick = { data.performAction() }, variant = ButtonVariant.Ghost, size = ButtonSize.Compact)
        }
    }
}

/** Centered message used inside empty lists that sit within a larger screen. */
@Composable
fun InlineNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = Kaiju.type.bodyS,
        color = Kaiju.colors.inkMuted,
        textAlign = TextAlign.Start,
        modifier = modifier.fillMaxWidth().padding(vertical = KaijuSpace.m),
    )
}
